package dev.flower.apex_sound_effects;

import dev.flower.apex_sound_effects.core.ArmorFeedbackRules;
import dev.flower.apex_sound_effects.core.ArmorFeedbackRules.Kind;
import dev.flower.apex_sound_effects.core.AmmoKind;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.bus.api.*;
import java.util.*;

/** 记录已完成伤害并合并同次攻击的反馈。 / Tracks completed damage and combines feedback for one attack. */
public final class DamageTracker {
    private record Key(UUID attacker,UUID target,AmmoKind ammo) {}
    private record Segment(float before,float after) {}
    private static final Map<Key,Pending> PENDING=new LinkedHashMap<>();
    private static final Map<LivingEntity,Deque<Float>> BEFORE=new IdentityHashMap<>();
    private static final int BUDGET=512;
    private static final Set<LivingEntity> GUN_HITS=Collections.newSetFromMap(new IdentityHashMap<>());
    public static void enterGunHit(LivingEntity target){GUN_HITS.add(target);}
    public static void leaveGunHit(LivingEntity target){GUN_HITS.remove(target);}
    private static final class Pending {
        final ServerPlayer attacker; final LivingEntity target; final float max;
        final AmmoKind ammo;
        final List<Segment> segments=new ArrayList<>(2);
        boolean headshot;
        Pending(ServerPlayer a,LivingEntity t,AmmoKind ammo){attacker=a;target=t;max=t.getMaxHealth();this.ammo=ammo;}
    }
    private static void queue(ServerPlayer a,LivingEntity t,float before,float after,AmmoKind ammo) {
        if (!FeedbackConfig.ENABLED.get() || !AttackWeaponFilter.targetAllowed(t) || a==t || before<=after || !Float.isFinite(before) || !Float.isFinite(after))return;
        Key key=new Key(a.getUUID(),t.getUUID(),ammo);
        if(!PENDING.containsKey(key)&&PENDING.size()>=BUDGET)return;
        Pending p=PENDING.computeIfAbsent(key,k->new Pending(a,t,ammo));
        if(p.segments.size()<64)p.segments.add(new Segment(before,after));
    }
    public static void completed(ServerPlayer attacker,LivingEntity target,float before,float after,boolean headshot,AmmoKind ammo) {
        queue(attacker,target,before,after,ammo);
        var pending=PENDING.get(new Key(attacker.getUUID(),target.getUUID(),ammo));
        if(pending!=null)pending.headshot|=headshot;
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void beforeDamage(LivingDamageEvent.Pre e) {
        if(e.getEntity().level().isClientSide || GUN_HITS.contains(e.getEntity()) || !(e.getSource().getEntity() instanceof ServerPlayer))return;
        BEFORE.computeIfAbsent(e.getEntity(),ignored->new ArrayDeque<>()).push(e.getEntity().getHealth());
    }
    @SubscribeEvent
    public static void damaged(LivingDamageEvent.Post e) {
        if(e.getEntity().level().isClientSide || GUN_HITS.contains(e.getEntity()))return;
        var stack=BEFORE.get(e.getEntity());
        if(stack==null || stack.isEmpty())return;
        float before=stack.pop();
        if(stack.isEmpty())BEFORE.remove(e.getEntity());
        if(!(e.getSource().getEntity() instanceof ServerPlayer attacker))return;
        if(FeedbackConfig.WEAPON_MODE.get()!=dev.flower.apex_sound_effects.core.WeaponFeedbackRules.Mode.ALL
                && !AttackWeaponFilter.allows(AttackWeaponFilter.source(e.getSource().getDirectEntity(),attacker),null))return;
        float after=e.getEntity().getHealth();
        AmmoKind ammo=e.getSource().getDirectEntity() instanceof net.minecraft.world.entity.projectile.AbstractArrow?AmmoKind.ARROW:AmmoKind.GENERIC;
        queue(attacker,e.getEntity(),before,after,ammo);
    }
    @SubscribeEvent public static void tick(ServerTickEvent.Post e) {
        double ratio=FeedbackConfig.FLESH_RATIO.get();
        for(Pending p:PENDING.values()) {
            Kind kind=Kind.NONE;float loss=0;boolean terminal=false;
            for(Segment segment:p.segments) {
                float after=segment.after();
                Kind part=ArmorFeedbackRules.classify(segment.before(),after,p.max,ratio);
                kind=ArmorFeedbackRules.merge(kind,part);
                if(part!=Kind.NONE){loss+=segment.before()-after;terminal|=after<=0;}
            }
            if(kind!=Kind.NONE && p.attacker.connection!=null && !p.attacker.hasDisconnected()) {
                FeedbackNetwork.send(p.attacker,new FeedbackNetwork.Hit(p.target.getId(),kind,loss,FeedbackConfig.armorColorForMaximumHealth(p.max).rgb,p.headshot,
                        p.target.getX(),p.target.getY()+p.target.getBbHeight()*0.65,p.target.getZ(),p.ammo,terminal));
            }
        }
        PENDING.clear();
        BEFORE.clear();
        GUN_HITS.clear();
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent e){PENDING.clear();BEFORE.clear();GUN_HITS.clear();}
}
