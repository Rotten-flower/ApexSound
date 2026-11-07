package dev.flower.apex_sound_effects.client;

import dev.flower.apex_sound_effects.FeedbackConfig;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.screens.SoundOptionsScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 独立调节本地反馈音量，同时受主音量控制。 / Logical feedback channel: local UI audio with its own controls, under master volume. */
@Mod.EventBusSubscriber(modid="apex_sound_effects",value=Dist.CLIENT)
public final class ApexVolumeControl {
    private static boolean dirty;
    private static OptionInstance<Integer> slider(String key,ForgeConfigSpec.DoubleValue setting) {
        return new OptionInstance<>(key,OptionInstance.noTooltip(),
                (caption,value)->Component.translatable("audio.apex_sound_effects.percent",caption,value),
                new OptionInstance.IntRange(0,100),(int)Math.round(setting.get()*100),value->{
                    setting.set(value/100.0);dirty=true;
                });
    }
    @SubscribeEvent public static void init(ScreenEvent.Init.Post e) {
        if(!(e.getScreen() instanceof SoundOptionsScreen))return;
        for(var child:e.getListenersList())if(child instanceof OptionsList list) {
            list.addBig(slider("audio.apex_sound_effects.channel",FeedbackConfig.VOLUME));
            break;
        }
    }
    @SubscribeEvent public static void close(ScreenEvent.Closing e) {
        if(e.getScreen() instanceof SoundOptionsScreen && dirty){FeedbackConfig.CLIENT.save();dirty=false;}
    }
}
