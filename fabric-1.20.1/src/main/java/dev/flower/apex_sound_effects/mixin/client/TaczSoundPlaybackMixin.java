package dev.flower.apex_sound_effects.mixin.client;

import dev.flower.apex_sound_effects.client.TaczSoundFilter;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 拦截 Fabric 声音播放，仅取消已启用替换的可选反馈音效。 / Fabric 1.20.1/1.21.1 play method, cancelling only an active optional feedback sound. */
@Mixin(SoundManager.class)
public abstract class TaczSoundPlaybackMixin {
    @Inject(method="play(Lnet/minecraft/client/resources/sounds/SoundInstance;)V",at=@At("HEAD"),cancellable=true)
    private void apexSoundEffects$replaceGunFeedback(SoundInstance sound,CallbackInfo ci) {
        if(TaczSoundFilter.replaces(sound))ci.cancel();
    }
}
