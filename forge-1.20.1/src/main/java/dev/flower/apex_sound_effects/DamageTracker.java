package dev.flower.apex_sound_effects;

import dev.flower.apex_sound_effects.core.ArmorFeedbackRules;
import dev.flower.apex_sound_effects.core.ArmorFeedbackRules.Kind;
import dev.flower.apex_sound_effects.core.AmmoKind;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.*;
import java.util.*;

/** 记录已完成伤害并合并同次攻击的反馈。 / Tracks completed damage and combines feedback for one attack. */
public final class DamageTracker {
    private record Key(UUID attacker,UUID target,AmmoKind ammo) {}
    private static final Map<Key,Pending> PENDING=new LinkedHashMap<>();
    private static final int BUDGET=512;
    private static final Set<LivingEntity> GUN_HITS=Collections.newSetFromMap(new IdentityHashMap<>());
    public static void enterGunHit(LivingEntity t){GUN_HITS.add(t);}
    public static void leaveGunHit(LivingEntity t){GUN_HITS.remove(t);}
    private static final class Pending {
        final ServerPlayer attacker; final LivingEntity target; final float max;
        final AmmoKind ammo;
        final List<float[]> segments=new ArrayList<>(2);
        boolean headshot;
        Pending(ServerPlayer a,LivingEntity t,AmmoKind ammo){attacker=a;target=t;max=t.getMaxHealth();this.ammo=ammo;}
    }
    public static void completed(ServerPlayer a,LivingEntity target,float before,float after,boolean headshot) {
        completed(a,target,before,after,headshot,AmmoKind.GENERIC);
    }
    public static void completed(ServerPlayer a,LivingEntity target,float before,float after,boolean headshot,AmmoKind ammo) {
        queue(a,target,before,after,headshot,true,ammo);
    }
    private static void queue(ServerPlayer a,LivingEntity t,float before,float after,boolean head,boolean completed,AmmoKind ammo) {
        if (!FeedbackConfig.ENABLED.get() || !AttackWeaponFilter.targetAllowed(t) || a==t || before<=after || !Float.isFinite(before) || !Float.isFinite(after))return;
        Key key=new Key(a.getUUID(),t.getUUID(),ammo);
        if(!PENDING.containsKey(key)&&PENDING.size()>=BUDGET)return;
        Pending p=PENDING.computeIfAbsent(key,k->new Pending(a,t,ammo));
        if(p.segments.size()<64)p.segments.add(new float[]{before,after,completed?1:0});
        p.headshot|=head;
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void damaged(LivingDamageEvent e) {
        if(e.getEntity().level().isClientSide || e.getAmount()<=0)return;
        if(!(e.getSource().getEntity() instanceof ServerPlayer attacker))return;
        // TaCZ Pre/Post encloses both damage components; do not count them again.
        if(GUN_HITS.contains(e.getEntity()))return;
        if(FeedbackConfig.WEAPON_MODE.get()!=dev.flower.apex_sound_effects.core.WeaponFeedbackRules.Mode.ALL
                && !AttackWeaponFilter.allows(AttackWeaponFilter.source(e.getSource().getDirectEntity(),attacker),null))return;
        float before=e.getEntity().getHealth();
        AmmoKind ammo=e.getSource().getDirectEntity() instanceof net.minecraft.world.entity.projectile.AbstractArrow?AmmoKind.ARROW:AmmoKind.GENERIC;
        queue(attacker,e.getEntity(),before,Math.max(0,before-e.getAmount()),false,false,ammo);
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e) {
        if(e.phase!=TickEvent.Phase.END)return;
        double ratio=FeedbackConfig.FLESH_RATIO.get();
        int sent=0;
        for(Pending p:PENDING.values()) {
            Kind kind=Kind.NONE;float loss=0;boolean terminal=false;
            for(float[] s:p.segments) {
                float after=s[2]==1?s[1]:Math.max(s[1],p.target.getHealth());
                Kind part=ArmorFeedbackRules.classify(s[0],after,p.max,ratio);
                kind=ArmorFeedbackRules.merge(kind,part);
                if(part!=Kind.NONE){loss+=s[0]-after;terminal|=after<=0;}
            }
            if(kind!=Kind.NONE && p.attacker.connection!=null && !p.attacker.hasDisconnected()) {
                FeedbackNetwork.send(p.attacker,new FeedbackNetwork.Hit(p.target.getId(),kind,loss,FeedbackConfig.armorColorForMaximumHealth(p.max).rgb,p.headshot,
                        p.target.getX(),p.target.getY()+p.target.getBbHeight()*0.65,p.target.getZ(),p.ammo,terminal));
                sent++;
            }
        }
        PENDING.clear();
        GUN_HITS.clear();
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent e){PENDING.clear();GUN_HITS.clear();}
}
