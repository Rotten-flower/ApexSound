package dev.flower.apex_sound_effects.mixin.client;

import dev.flower.apex_sound_effects.FeedbackConfig;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.screens.options.SoundOptionsScreen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.Options;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundOptionsScreen.class)
abstract class SoundOptionsScreenMixin extends OptionsSubScreen {
    protected SoundOptionsScreenMixin(Screen parent, Options options, Component title) { super(parent, options, title); }

    @Inject(method = "addOptions", at = @At("TAIL"))
    private void apexSoundEffects$addVolumeControl(CallbackInfo callback) {
        int initial = (int) Math.round(FeedbackConfig.settings().volume() * 100);
        OptionInstance<Integer> volume = new OptionInstance<>(
                "audio.apex_sound_effects.channel", OptionInstance.noTooltip(),
                (caption, value) -> Component.translatable("audio.apex_sound_effects.percent", caption, value),
                new OptionInstance.IntRange(0, 100), initial,
                value -> FeedbackConfig.setVolume(value / 100.0));
        list.addBig(volume);
    }
}
