package dev.flower.apex_sound_effects;
import net.minecraftforge.fml.common.Mod;
import dev.flower.apex_sound_effects.core.ModIdentity;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.common.MinecraftForge;
/** 注册本端音效、粒子和事件。 / Registers this platform’s sounds, particles and events. */
@Mod(ApexSound.MOD_ID)
public final class ApexSound {
    public static final String MOD_ID = "apex_sound_effects";
    public ApexSound() {
        if (!MOD_ID.equals(ModIdentity.ID)) throw new IllegalStateException("Platform ID differs from common ID");
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER,FeedbackConfig.SERVER);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT,FeedbackConfig.CLIENT);
        FeedbackSounds.REGISTRY.register(FMLJavaModLoadingContext.get().getModEventBus());
        ArmorBreakParticles.REGISTRY.register(FMLJavaModLoadingContext.get().getModEventBus());
        FeedbackNetwork.register();
        MinecraftForge.EVENT_BUS.register(DamageTracker.class);
        MinecraftForge.EVENT_BUS.register(AttackWeaponFilter.class);
        dev.flower.apex_sound_effects.compat.TaczIntegration.registerServer();
    }
}
