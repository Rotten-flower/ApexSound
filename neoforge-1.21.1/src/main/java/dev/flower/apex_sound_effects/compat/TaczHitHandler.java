package dev.flower.apex_sound_effects.compat;

import com.tacz.guns.api.event.common.*;
import dev.flower.apex_sound_effects.DamageTracker;
import dev.flower.apex_sound_effects.FeedbackConfig;
import dev.flower.apex_sound_effects.AttackWeaponFilter;
import dev.flower.apex_sound_effects.core.AmmoKind;
import com.tacz.guns.entity.EntityKineticBullet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.bus.api.*;
import net.neoforged.fml.LogicalSide;
import java.util.*;

/** 仅在 TaCZ 可用时加载，用前后事件测量实际伤害。 / Loaded only when TaCZ is present; Pre/Post measures both damage components together. */
public final class TaczHitHandler {
    private record Snapshot(Entity bullet,LivingEntity target,float health,AmmoKind ammo,boolean allowed) {}
    private static final Map<LivingEntity,Snapshot> ACTIVE=new IdentityHashMap<>();
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void pre(EntityHurtByGunEvent.Pre e) {
        if(e.getLogicalSide()!=LogicalSide.SERVER || !(e.getAttacker() instanceof ServerPlayer)
                || !(e.getHurtEntity() instanceof LivingEntity target) || ACTIVE.size()>=512)return;
        AmmoKind ammo=e.getBullet() instanceof EntityKineticBullet bullet?FeedbackConfig.ammoKind(bullet.getAmmoId()):AmmoKind.GENERIC;
        ACTIVE.put(target,new Snapshot(e.getBullet(),target,target.getHealth(),ammo,AttackWeaponFilter.allowsGun(e.getGunId(),target)));
        DamageTracker.enterGunHit(target);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void post(EntityHurtByGunEvent.Post e) {
        if(e.getLogicalSide()!=LogicalSide.SERVER || !(e.getAttacker() instanceof ServerPlayer a)
                || !(e.getHurtEntity() instanceof LivingEntity t))return;
        finish(a,t,e.getBullet(),e.isHeadShot());
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void kill(EntityKillByGunEvent e) {
        if(e.getLogicalSide()==LogicalSide.SERVER && e.getAttacker() instanceof ServerPlayer a)
            finish(a,e.getKilledEntity(),e.getBullet(),e.isHeadShot());
    }
    private static void finish(ServerPlayer a,LivingEntity t,Entity bullet,boolean headshot) {
        Snapshot s=ACTIVE.remove(t);
        DamageTracker.leaveGunHit(t);
        if(t!=null && s!=null && s.allowed && s.bullet==bullet)DamageTracker.completed(a,t,s.health,t.getHealth(),headshot,s.ammo);
    }
    @SubscribeEvent public static void tick(ServerTickEvent.Post e) {
        ACTIVE.clear();
    }
}
