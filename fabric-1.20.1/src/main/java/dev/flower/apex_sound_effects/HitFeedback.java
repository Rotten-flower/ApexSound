package dev.flower.apex_sound_effects;

import dev.flower.apex_sound_effects.core.AmmoKind;
import dev.flower.apex_sound_effects.core.ArmorFeedbackRules;
import dev.flower.apex_sound_effects.core.WeaponFeedbackRules;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 记录服务端伤害并向攻击者发送反馈。 / Records server damage and sends feedback to the attacker. */
public final class HitFeedback {
    public static final ResourceLocation CHANNEL = new ResourceLocation(ApexSound.MOD_ID, "feedback");
    private static final Map<Key, Pending> PENDING = new LinkedHashMap<>();
    private static final int MAX_PENDING = 512;
    private static final java.util.Set<LivingEntity> GUN_HITS=java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
    public static void enterGunHit(LivingEntity target){GUN_HITS.add(target);}
    public static void leaveGunHit(LivingEntity target){GUN_HITS.remove(target);}

    public record Hit(int target, ArmorFeedbackRules.Kind kind, float damage, int color, boolean headshot,
                      double x, double y, double z, AmmoKind ammo, boolean terminal) {
        static Hit read(FriendlyByteBuf b) {
            return new Hit(b.readVarInt(), b.readEnum(ArmorFeedbackRules.Kind.class), b.readFloat(), b.readInt(),
                    b.readBoolean(), b.readDouble(), b.readDouble(), b.readDouble(), b.readEnum(AmmoKind.class), b.readBoolean());
        }
        void write(FriendlyByteBuf b) {
            b.writeVarInt(target); b.writeEnum(kind); b.writeFloat(damage); b.writeInt(color); b.writeBoolean(headshot);
            b.writeDouble(x); b.writeDouble(y); b.writeDouble(z); b.writeEnum(ammo); b.writeBoolean(terminal);
        }
    }

    private record Key(UUID attacker, UUID target, AmmoKind ammo) {}
    private record Segment(float before, float after) {}
    private static final class Pending {
        final ServerPlayer attacker;
        final LivingEntity target;
        final float maximum;
        final AmmoKind ammo;
        final List<Segment> segments = new ArrayList<>(2);
        boolean headshot;
        Pending(ServerPlayer attacker, LivingEntity target, AmmoKind ammo) {
            this.attacker = attacker; this.target = target; this.maximum = target.getMaxHealth(); this.ammo = ammo;
        }
    }

    static void initializeServer() { FeedbackConfig.initialize(); }

    /** Called at the head of LivingEntity.hurt; the mixin passes this value back after the call. */
    public static float captureBefore(LivingEntity target) {
        return target.getHealth();
    }

    public static void captureAfter(LivingEntity target, DamageSource source, boolean applied, float before) {
        if (!applied || GUN_HITS.contains(target) || target.level().isClientSide || !(source.getEntity() instanceof ServerPlayer attacker)) return;
        float after = target.getHealth();
        if (!Float.isFinite(before) || !Float.isFinite(after) || after >= before || attacker == target) return;
        if (!FeedbackConfig.settings().enabled() || !AttackWeaponFilter.targetAllowed(target)) return;
        if (FeedbackConfig.settings().weaponMode() != WeaponFeedbackRules.Mode.ALL
                && !AttackWeaponFilter.allows(AttackWeaponFilter.sourceStack(source.getDirectEntity(), attacker))) return;
        AmmoKind ammo = source.getDirectEntity() instanceof AbstractArrow ? AmmoKind.ARROW : AmmoKind.GENERIC;
        completed(attacker,target,before,after,false,ammo);
    }

    public static void completed(ServerPlayer attacker,LivingEntity target,float before,float after,boolean headshot,AmmoKind ammo) {
        if(target.level().isClientSide || attacker==target || !Float.isFinite(before) || !Float.isFinite(after)
                || before<=after || !FeedbackConfig.settings().enabled() || !AttackWeaponFilter.targetAllowed(target))return;
        Key key = new Key(attacker.getUUID(), target.getUUID(), ammo);
        if (!PENDING.containsKey(key) && PENDING.size() >= MAX_PENDING) return;
        Pending pending = PENDING.computeIfAbsent(key, ignored -> new Pending(attacker, target, ammo));
        if (pending.segments.size() < 64) pending.segments.add(new Segment(before, after));
        pending.headshot|=headshot;
    }

    public static void flush(MinecraftServer server) {
        var settings = FeedbackConfig.settings();
        if (settings.enabled()) {
            for (Pending pending : PENDING.values()) {
                ArmorFeedbackRules.Kind kind = ArmorFeedbackRules.Kind.NONE;
                float damage = 0;
                boolean terminal = false;
                for (Segment segment : pending.segments) {
                    var part = ArmorFeedbackRules.classify(segment.before(), segment.after(), pending.maximum, settings.fleshRatio());
                    kind = ArmorFeedbackRules.merge(kind, part);
                    if (part != ArmorFeedbackRules.Kind.NONE) {
                        damage += segment.before() - segment.after();
                        terminal |= segment.after() <= 0;
                    }
                }
                if (kind == ArmorFeedbackRules.Kind.NONE
                        || pending.attacker.connection == null || pending.attacker.hasDisconnected()) continue;
                var color = ArmorFeedbackRules.colorFor(pending.maximum, settings.whiteMax(), settings.blueMax(),
                        settings.purpleMax(), settings.goldMax()).rgb;
                Hit hit = new Hit(pending.target.getId(), kind, damage, color, pending.headshot,
                        pending.target.getX(), pending.target.getY() + pending.target.getBbHeight() * 0.65,
                        pending.target.getZ(), pending.ammo, terminal || !pending.target.isAlive());
                FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
                hit.write(buffer);
                if(ServerPlayNetworking.canSend(pending.attacker, CHANNEL)) ServerPlayNetworking.send(pending.attacker, CHANNEL, buffer);
            }
        }
        PENDING.clear();GUN_HITS.clear();
    }

    public static void clear() { PENDING.clear();GUN_HITS.clear(); }

    private HitFeedback() {}
}
