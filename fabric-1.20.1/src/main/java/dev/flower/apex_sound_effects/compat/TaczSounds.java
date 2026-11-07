package dev.flower.apex_sound_effects.compat;

import cn.sh1rocu.tacz.api.LogicalSide;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.event.common.EntityHurtByGunEvent;
import com.tacz.guns.api.event.common.EntityKillByGunEvent;
import com.tacz.guns.client.sound.GunSoundInstance;
import com.tacz.guns.sound.SoundManager;
import dev.flower.apex_sound_effects.*;
import dev.flower.apex_sound_effects.client.TaczSoundFilter;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.*;

/** 只替换 TaCZ 命中音效，保留射击、换弹和画面。 / Replaces TaCZ hit audio while preserving firing, reload and visuals. */
public final class TaczSounds {
    private static final ResourceLocation BEFORE=ResourceLocation.tryParse("apex_sound_effects:before_gun_audio");
    private static final ResourceLocation AFTER=ResourceLocation.tryParse("apex_sound_effects:after_gun_audio");
    private static ResourceLocation flesh,head,kill;
    public static void register() {
        EntityHurtByGunEvent.POST.addPhaseOrdering(BEFORE,Event.DEFAULT_PHASE);
        EntityHurtByGunEvent.POST.addPhaseOrdering(Event.DEFAULT_PHASE,AFTER);
        EntityKillByGunEvent.CALLBACK.addPhaseOrdering(BEFORE,Event.DEFAULT_PHASE);
        EntityKillByGunEvent.CALLBACK.addPhaseOrdering(Event.DEFAULT_PHASE,AFTER);
        EntityHurtByGunEvent.POST.register(BEFORE,event->{
            if(!active(event.getLogicalSide(),event.getAttacker(),event.getGunId(),event.getHurtEntity()))return;
            flesh=head=null;
            TimelessAPI.getGunDisplay(event.getGunDisplayId(),event.getGunId()).ifPresent(display->{
                flesh=display.getSounds(SoundManager.FLESH_HIT_SOUND);head=display.getSounds(SoundManager.HEAD_HIT_SOUND);
            });
        });
        EntityHurtByGunEvent.POST.register(AFTER,event->{if(event.getLogicalSide()==LogicalSide.CLIENT)flesh=head=null;});
        EntityKillByGunEvent.CALLBACK.register(BEFORE,event->{
            if(!active(event.getLogicalSide(),event.getAttacker(),event.getGunId(),event.getKilledEntity()))return;
            kill=null;
            TimelessAPI.getGunDisplay(event.getGunDisplayId(),event.getGunId()).ifPresent(display->kill=display.getSounds(SoundManager.KILL_SOUND));
        });
        EntityKillByGunEvent.CALLBACK.register(AFTER,event->{if(event.getLogicalSide()==LogicalSide.CLIENT)kill=null;});
        ClientTickEvents.END_CLIENT_TICK.register(client->{flesh=head=kill=null;});
        TaczSoundFilter.register(TaczSounds::replaces);
    }
    private static boolean active(LogicalSide side,LivingEntity attacker,ResourceLocation gun,Entity target) {
        return side==LogicalSide.CLIENT && attacker==Minecraft.getInstance().player
                && FeedbackConfig.taczSettings().replaceHitSounds() && TaczSoundFilter.allowsGun(gun,target);
    }
    public static boolean replaces(SoundInstance sound) {
        if(!(sound instanceof GunSoundInstance gun))return false;
        ResourceLocation id=gun.getRegistryName();
        return id!=null && (id.equals(flesh)||id.equals(head)||id.equals(kill));
    }
}
