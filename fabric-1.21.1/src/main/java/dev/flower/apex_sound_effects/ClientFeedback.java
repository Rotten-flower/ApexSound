package dev.flower.apex_sound_effects;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.flower.apex_sound_effects.client.ArmorShardParticle;
import dev.flower.apex_sound_effects.client.TargetLabelProjection;
import dev.flower.apex_sound_effects.core.AmmoKind;
import dev.flower.apex_sound_effects.core.ArmorFeedbackRules;
import dev.flower.apex_sound_effects.core.CumulativeDamage;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.screens.options.SoundOptionsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** 播放命中音效并绘制随目标移动的伤害数字。 / Plays hit sounds and renders damage labels beside the target. */
public final class ClientFeedback implements ClientModInitializer {
    private static final ResourceLocation SHIELD = ResourceLocation.fromNamespaceAndPath(ApexSound.MOD_ID, "textures/gui/shield.png");
    private static final ResourceLocation BROKEN = ResourceLocation.fromNamespaceAndPath(ApexSound.MOD_ID, "textures/gui/shield_broken.png");
    private static final Style DAMAGE_STYLE = Style.EMPTY.withFont(ResourceLocation.fromNamespaceAndPath(ApexSound.MOD_ID, "damage"));
    private static final long HUD_LIFETIME = 1_000_000_000L;
    private static final int MAX_LABELS = 32;
    private static final Map<Integer, Label> LABELS = new LinkedHashMap<>();
    private static final CumulativeDamage DAMAGE_STACK = new CumulativeDamage();
    private static final Matrix4f WORLD_TO_CLIP = new Matrix4f();
    private static final Vector4f CLIP = new Vector4f();
    private static net.minecraft.client.multiplayer.ClientLevel feedbackLevel;
    private static long projectionTime;
    private static long lastArmorSound, lastFleshSound, lastBreakSound;
    private static final Deque<SimpleSoundInstance> SOUNDS = new ArrayDeque<>();
    private static final Map<SimpleSoundInstance, ArmorFeedbackRules.Kind> SOUND_KINDS = new java.util.IdentityHashMap<>();
    private static final Deque<Finish> FINISHES = new ArrayDeque<>();

    private record Finish(long due, AmmoKind ammo) {}
    private static final class Label {
        final HitFeedback.Hit hit;
        final String text;
        final UUID entityUuid;
        final Vector2f screen = new Vector2f();
        double x, y, z;
        float bodyWidth = 0.6f, bodyHeight = 1.8f;
        boolean visible;
        final long time;
        final float textWidth;
        Label(HitFeedback.Hit hit, String text, long time, Entity entity, Label previous) {
            this.hit = hit; this.text = text; this.time = time;
            entityUuid = entity != null ? entity.getUUID() : previous != null ? previous.entityUuid : null;
            if (entity != null) {
                bodyWidth = entity.getBbWidth(); bodyHeight = entity.getBbHeight();
                x = entity.getX(); y = entity.getY() + bodyHeight; z = entity.getZ();
            } else if (previous != null) {
                x = previous.x; y = previous.y; z = previous.z; bodyWidth = previous.bodyWidth; bodyHeight = previous.bodyHeight;
            } else { x = hit.x(); y = hit.y() + 0.75; z = hit.z(); }
            textWidth = Minecraft.getInstance().font.width(Component.literal(text).withStyle(DAMAGE_STYLE)) * 1.35f;
        }
    }

    @Override public void onInitializeClient() {
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->dev.flower.apex_sound_effects.client.TaczSoundFilter.setServerPolicy(null));
        ClientPlayNetworking.registerGlobalReceiver(GunPolicyNetwork.PolicyPacket.TYPE,(packet,context)->context.client().execute(()->
            dev.flower.apex_sound_effects.client.TaczSoundFilter.setServerPolicy(packet.policy())));

        dev.flower.apex_sound_effects.compat.TaczIntegration.registerClient();
        FeedbackConfig.initialize();
        ParticleFactoryRegistry.getInstance().register(ApexSound.SHARD, ArmorShardParticle.Provider::new);
        ClientPlayNetworking.registerGlobalReceiver(HitFeedback.Hit.TYPE, (hit, context) -> context.client().execute(() -> accept(hit)));
        HudRenderCallback.EVENT.register((graphics, tickDelta) -> render(graphics));
        WorldRenderEvents.AFTER_ENTITIES.register(ClientFeedback::worldFrame);
        ClientTickEvents.END_CLIENT_TICK.register(client -> tick());
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (screen instanceof SoundOptionsScreen) ScreenEvents.remove(screen).register(removed -> FeedbackConfig.save());
        });
    }

    private static void accept(HitFeedback.Hit hit) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || hit.kind() == ArmorFeedbackRules.Kind.NONE || !Float.isFinite(hit.damage()) || hit.damage() <= 0) return;
        if (feedbackLevel != mc.level) {
            for (var sound : SOUNDS) mc.getSoundManager().stop(sound);
            SOUNDS.clear(); SOUND_KINDS.clear(); FINISHES.clear();
            lastArmorSound = lastFleshSound = lastBreakSound = 0;
            DAMAGE_STACK.clear(); LABELS.clear(); projectionTime = 0; feedbackLevel = mc.level;
        }
        long now = System.nanoTime();
        String total = CumulativeDamage.format(DAMAGE_STACK.add(hit.target(), hit.damage(), now, hit.terminal()));
        LABELS.values().removeIf(label -> now - label.time >= HUD_LIFETIME);
        Label previous = LABELS.remove(hit.target());
        if (LABELS.size() >= MAX_LABELS) LABELS.remove(LABELS.keySet().iterator().next());
        LABELS.put(hit.target(), new Label(hit, total, now, mc.level.getEntity(hit.target()), previous));
        playHit(hit, now);
        if (hit.terminal()) {
            if (FINISHES.size() >= 8) FINISHES.removeFirst();
            FINISHES.addLast(new Finish(now + (hit.kind() == ArmorFeedbackRules.Kind.BREAK ? 220_000_000L : 0), hit.ammo()));
        }
        spawnParticles(hit);
    }

    private static void playHit(HitFeedback.Hit hit, long now) {
        var kind = hit.kind();
        if (kind != ArmorFeedbackRules.Kind.BREAK && now - lastBreakSound < 180_000_000L) return;
        if (kind == ArmorFeedbackRules.Kind.ARMOR) {
            if (now - lastArmorSound < 55_000_000L) return;
            lastArmorSound = now;
        } else if (kind == ArmorFeedbackRules.Kind.FLESH) {
            if (now - lastFleshSound < 35_000_000L) return;
            lastFleshSound = now;
        } else lastBreakSound = now;
        float volume = (float) FeedbackConfig.settings().volume();
        if (volume <= 0 || hit.terminal() && kind != ArmorFeedbackRules.Kind.BREAK) return;
        SoundEvent sound = switch (kind) {
            case ARMOR -> hit.ammo() == AmmoKind.ARROW ? ApexSound.ARROW_ARMOR : ApexSound.ARMOR;
            case BREAK -> ApexSound.BREAK;
            default -> hit.ammo() == AmmoKind.ARROW ? ApexSound.ARROW_FLESH : ApexSound.FLESH;
        };
        Minecraft mc = Minecraft.getInstance();
        play(sound, kind, volume);
        if (kind == ArmorFeedbackRules.Kind.BREAK && hit.ammo() != AmmoKind.ARROW)
            play(ApexSound.CRACK, ArmorFeedbackRules.Kind.BREAK, volume);
    }

    private static void playFinish(AmmoKind ammo) {
        float volume = (float) FeedbackConfig.settings().volume();
        if (volume <= 0) return;
        SoundEvent sound = switch (ammo) {
            case HEAVY -> ApexSound.FINISH_HEAVY;
            case SHOTGUN -> ApexSound.FINISH_SHOTGUN;
            case ARROW -> ApexSound.FINISH_ARROW;
            default -> ApexSound.FINISH_LIGHT;
        };
        play(sound, ArmorFeedbackRules.Kind.NONE, volume);
    }

    private static void play(SoundEvent event, ArmorFeedbackRules.Kind kind, float volume) {
        var manager = Minecraft.getInstance().getSoundManager();
        if (kind == ArmorFeedbackRules.Kind.BREAK || kind == ArmorFeedbackRules.Kind.NONE) {
            for (var iterator = SOUNDS.iterator(); iterator.hasNext();) {
                var old = iterator.next();
                if (SOUND_KINDS.get(old) == ArmorFeedbackRules.Kind.ARMOR || SOUND_KINDS.get(old) == ArmorFeedbackRules.Kind.FLESH) {
                    manager.stop(old); SOUND_KINDS.remove(old); iterator.remove();
                }
            }
        }
        int maximum = kind == ArmorFeedbackRules.Kind.BREAK ? 2 : 8;
        while (SOUNDS.size() >= maximum) {
            var old = SOUNDS.removeFirst(); SOUND_KINDS.remove(old); manager.stop(old);
        }
        var instance = SimpleSoundInstance.forUI(event, 1.0f, volume);
        SOUNDS.addLast(instance); SOUND_KINDS.put(instance, kind); manager.play(instance);
    }

    private static void spawnParticles(HitFeedback.Hit hit) {
        if (hit.kind() == ArmorFeedbackRules.Kind.FLESH) return;
        Minecraft mc = Minecraft.getInstance();
        int color = hit.color();
        var tint = new Vector3f(((color >> 16) & 255) / 255f, ((color >> 8) & 255) / 255f, (color & 255) / 255f);
        int count = hit.kind() == ArmorFeedbackRules.Kind.BREAK ? 24 : 5;
        for (int i = 0; i < count; i++) {
            double angle = i * Math.PI * 2 / count;
            double radius = hit.kind() == ArmorFeedbackRules.Kind.BREAK ? 0.36 : 0.12;
            var particle = mc.particleEngine.createParticle(ApexSound.SHARD,
                    hit.x() + Math.cos(angle) * radius, hit.y() + (mc.level.random.nextFloat() - 0.5) * 0.6,
                    hit.z() + Math.sin(angle) * radius, Math.cos(angle) * 0.08, 0.04, Math.sin(angle) * 0.08);
            if (particle != null) {
                particle.setColor(tint.x, tint.y, tint.z);
                particle.setLifetime(hit.kind() == ArmorFeedbackRules.Kind.BREAK ? 12 : 5);
                particle.setParticleSpeed(Math.cos(angle) * 0.09, 0.025, Math.sin(angle) * 0.09);
            }
        }
    }

    private static void worldFrame(WorldRenderContext context) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.level != feedbackLevel || LABELS.isEmpty()) return;
        WORLD_TO_CLIP.set(context.projectionMatrix()).mul(context.positionMatrix());
        var camera = context.camera().getPosition();
        float partial = context.tickCounter().getGameTimeDeltaPartialTick(false);
        projectionTime = System.nanoTime();
        for (Label label : LABELS.values()) {
            Entity entity = mc.level.getEntity(label.hit.target());
            if (entity != null && !entity.isRemoved() && entity.getUUID().equals(label.entityUuid)
                    && (!(entity instanceof LivingEntity living) || living.isAlive())) {
                label.bodyWidth = entity.getBbWidth(); label.bodyHeight = entity.getBbHeight();
                label.x = Mth.lerp(partial, entity.xo, entity.getX());
                label.y = Mth.lerp(partial, entity.yo, entity.getY()) + label.bodyHeight;
                label.z = Mth.lerp(partial, entity.zo, entity.getZ());
            }
            label.visible = TargetLabelProjection.upperLeft(WORLD_TO_CLIP, label.x - camera.x, label.y - camera.y,
                    label.z - camera.z, label.bodyWidth, label.bodyHeight, mc.getWindow().getGuiScaledWidth(),
                    mc.getWindow().getGuiScaledHeight(), CLIP, label.screen);
        }
    }

    private static void render(GuiGraphics graphics) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui || mc.player.isSpectator() || LABELS.isEmpty()) return;
        long now = System.nanoTime();
        if (now - projectionTime >= 250_000_000L) return;
        float scale = (float) (Math.max(480, mc.getWindow().getHeight()) / 720.0 * 1.5 / mc.getWindow().getGuiScale());
        RenderSystem.enableBlend();
        for (Label label : LABELS.values()) if (label.visible && now - label.time < HUD_LIFETIME) renderLabel(graphics, label, scale, now);
        graphics.setColor(1, 1, 1, 1);
        RenderSystem.disableBlend();
    }

    private static void renderLabel(GuiGraphics graphics, Label label, float scale, long now) {
        double age = (now - label.time) / 1_000_000_000.0;
        float alpha = (float) Math.min(1, (1 - age) / 0.18);
        if (alpha < 0.02f) return;
        int color = label.hit.headshot() ? 0xFFD45A : label.hit.kind() == ArmorFeedbackRules.Kind.FLESH ? 0xFF4A4A : label.hit.color();
        int argb = ((int) (alpha * 255) << 24) | color;
        float width = label.textWidth + (label.hit.kind() == ArmorFeedbackRules.Kind.FLESH ? 0 : 15);
        graphics.pose().pushPose();
        graphics.pose().translate(label.screen.x, label.screen.y, 0);
        graphics.pose().scale(scale, scale, 1);
        graphics.pose().translate(-width - 6, -12 - Math.min(age, 0.18) * 22, 0);
        if (label.hit.kind() != ArmorFeedbackRules.Kind.FLESH) {
            graphics.setColor(((color >> 16) & 255) / 255f, ((color >> 8) & 255) / 255f, (color & 255) / 255f, alpha);
            int shift = label.hit.kind() == ArmorFeedbackRules.Kind.BREAK ? (int) (Math.sin(age * 70) * Math.max(0, 3 - age * 12)) : 0;
            graphics.blit(label.hit.kind() == ArmorFeedbackRules.Kind.BREAK ? BROKEN : SHIELD,
                    (int) label.textWidth + 3 + shift, -3, 12, 14, 0, 0, 64, 64, 64, 64);
        }
        graphics.setColor(1, 1, 1, 1);
        graphics.pose().pushPose(); graphics.pose().scale(1.35f, 1.35f, 1);
        graphics.drawString(Minecraft.getInstance().font, Component.literal(label.text).withStyle(DAMAGE_STYLE), 0, 0, argb, true);
        graphics.pose().popPose(); graphics.pose().popPose();
    }

    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        for (var iterator = SOUNDS.iterator(); iterator.hasNext();) {
            var sound = iterator.next();
            if (!mc.getSoundManager().isActive(sound)) { iterator.remove(); SOUND_KINDS.remove(sound); }
        }
        if (mc.level != feedbackLevel) {
            for (var sound : SOUNDS) mc.getSoundManager().stop(sound);
            SOUNDS.clear(); SOUND_KINDS.clear(); FINISHES.clear();
            DAMAGE_STACK.clear(); LABELS.clear(); projectionTime = 0; feedbackLevel = mc.level;
            lastArmorSound = lastFleshSound = lastBreakSound = 0;
        }
        if (mc.level == null) { LABELS.clear(); DAMAGE_STACK.clear(); FINISHES.clear(); lastArmorSound = lastFleshSound = lastBreakSound = 0; return; }
        long now = System.nanoTime();
        LABELS.values().removeIf(label -> now - label.time >= HUD_LIFETIME);
        for (var iterator = FINISHES.iterator(); iterator.hasNext();) {
            var pending = iterator.next();
            if (now >= pending.due()) { iterator.remove(); playFinish(pending.ammo()); }
        }
    }
}
