package dev.flower.apex_sound_effects.compat;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.event.common.EntityHurtByGunEvent;
import com.tacz.guns.api.event.common.EntityKillByGunEvent;
import com.tacz.guns.sound.SoundManager;
import com.tacz.guns.client.sound.GunSoundInstance;
import dev.flower.apex_sound_effects.FeedbackConfig;
import dev.flower.apex_sound_effects.AttackWeaponFilter;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.*;
import net.neoforged.fml.LogicalSide;

/** 只替换 TaCZ 命中音效，保留射击、换弹和画面。 / Replaces TaCZ hit audio while preserving firing, reload and visuals. */
public final class TaczSounds {
    private static ResourceLocation flesh,head,kill;
    private static boolean active(EntityHurtByGunEvent.Post e) {
        return e.getLogicalSide()==LogicalSide.CLIENT && e.getAttacker()==Minecraft.getInstance().player
                && FeedbackConfig.REPLACE_TACZ.get() && FeedbackConfig.ENABLED.get() && AttackWeaponFilter.allowsGun(e.getGunId(),e.getHurtEntity());
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void before(EntityHurtByGunEvent.Post e) {
        if(!active(e))return;
        TimelessAPI.getGunDisplay(e.getGunDisplayId(),e.getGunId()).ifPresent(display->{
            flesh=display.getSounds(SoundManager.FLESH_HIT_SOUND);
            head=display.getSounds(SoundManager.HEAD_HIT_SOUND);
        });
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void after(EntityHurtByGunEvent.Post e) {
        if(!active(e))return;
        flesh=head=null;
    }
    @SubscribeEvent public static void sound(PlaySoundEvent e) {
        if(e.getSound()==null)return;
        // TaCZ shares a registered sound event across gun audio; the instance carries
        // the actual file ID separately. Match that ID so firing/reloading remain intact.
        var resource=e.getSound() instanceof GunSoundInstance gunSound?gunSound.getRegistryName():e.getSound().getLocation();
        if(resource!=null && (resource.equals(flesh)||resource.equals(head)||resource.equals(kill)))e.setSound(null);
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void beforeKill(EntityKillByGunEvent e) {
        if(e.getLogicalSide()!=LogicalSide.CLIENT || e.getAttacker()!=Minecraft.getInstance().player
                || !FeedbackConfig.REPLACE_TACZ.get() || !FeedbackConfig.ENABLED.get() || !AttackWeaponFilter.allowsGun(e.getGunId(),e.getKilledEntity()))return;
        TimelessAPI.getGunDisplay(e.getGunDisplayId(),e.getGunId()).ifPresent(display->kill=display.getSounds(SoundManager.KILL_SOUND));
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void afterKill(EntityKillByGunEvent e) {
        if(e.getLogicalSide()!=LogicalSide.CLIENT || e.getAttacker()!=Minecraft.getInstance().player)return;
        kill=null;
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post e) {
        flesh=head=kill=null;
    }
}
