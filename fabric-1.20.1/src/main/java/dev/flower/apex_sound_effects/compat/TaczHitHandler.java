package dev.flower.apex_sound_effects.compat;

import cn.sh1rocu.tacz.api.LogicalSide;
import com.tacz.guns.api.event.common.EntityHurtByGunEvent;
import com.tacz.guns.api.event.common.EntityKillByGunEvent;
import com.tacz.guns.entity.EntityKineticBullet;
import dev.flower.apex_sound_effects.*;
import dev.flower.apex_sound_effects.core.AmmoKind;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import java.util.*;

/** Refabricated's events enclose both damage components, including a lethal bullet. */
public final class TaczHitHandler {
    private static final ResourceLocation AFTER = ResourceLocation.tryParse("apex_sound_effects:gun_snapshot");
    private record Snapshot(Entity bullet,float health,AmmoKind ammo,boolean allowed) {}
    private static final Map<LivingEntity,Snapshot> ACTIVE=new IdentityHashMap<>();
    public static void register() {
        EntityHurtByGunEvent.PRE.addPhaseOrdering(Event.DEFAULT_PHASE,AFTER);
        EntityHurtByGunEvent.POST.addPhaseOrdering(Event.DEFAULT_PHASE,AFTER);
        EntityKillByGunEvent.CALLBACK.addPhaseOrdering(Event.DEFAULT_PHASE,AFTER);
        EntityHurtByGunEvent.PRE.register(AFTER,TaczHitHandler::pre);
        EntityHurtByGunEvent.POST.register(AFTER,TaczHitHandler::post);
        EntityKillByGunEvent.CALLBACK.register(AFTER,TaczHitHandler::kill);
        ServerTickEvents.END_SERVER_TICK.register(server->ACTIVE.clear());
        ServerLifecycleEvents.SERVER_STOPPED.register(server->ACTIVE.clear());
    }
    private static void pre(EntityHurtByGunEvent.Pre event) {
        if(event.getLogicalSide()!=LogicalSide.SERVER || event.isCanceled()
                || !(event.getAttacker() instanceof ServerPlayer) || !(event.getHurtEntity() instanceof LivingEntity target)
                || ACTIVE.size()>=512)return;
        AmmoKind ammo=event.getBullet() instanceof EntityKineticBullet bullet
                ?FeedbackConfig.taczSettings().ammoKind(bullet.getAmmoId().toString()):AmmoKind.GENERIC;
        ACTIVE.put(target,new Snapshot(event.getBullet(),target.getHealth(),ammo,AttackWeaponFilter.allowsGun(event.getGunId(),target)));
        HitFeedback.enterGunHit(target);
    }
    private static void post(EntityHurtByGunEvent.Post event) {
        if(event.getLogicalSide()==LogicalSide.SERVER && event.getAttacker() instanceof ServerPlayer player
                && event.getHurtEntity() instanceof LivingEntity target)finish(player,target,event.getBullet(),event.isHeadShot());
    }
    private static void kill(EntityKillByGunEvent event) {
        if(event.getLogicalSide()==LogicalSide.SERVER && event.getAttacker() instanceof ServerPlayer player)
            finish(player,event.getKilledEntity(),event.getBullet(),event.isHeadShot());
    }
    private static void finish(ServerPlayer player,LivingEntity target,Entity bullet,boolean headshot) {
        if(target==null)return;
        Snapshot snapshot=ACTIVE.remove(target);
        HitFeedback.leaveGunHit(target);
        if(snapshot!=null && snapshot.allowed && snapshot.bullet==bullet)
            HitFeedback.completed(player,target,snapshot.health,target.getHealth(),headshot,snapshot.ammo);
    }
}
