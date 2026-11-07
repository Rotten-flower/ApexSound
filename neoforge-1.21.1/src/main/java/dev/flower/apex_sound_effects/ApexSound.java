package dev.flower.apex_sound_effects;
import net.neoforged.fml.common.Mod;
import dev.flower.apex_sound_effects.core.ModIdentity;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
/** 注册本端音效、粒子和事件。 / Registers this platform’s sounds, particles and events. */
@Mod(ApexSound.MOD_ID)
public final class ApexSound {
    public static final String MOD_ID = "apex_sound_effects";
    public ApexSound() {
        if (!MOD_ID.equals(ModIdentity.ID)) throw new IllegalStateException("Platform ID differs from common ID");
        var bus=ModLoadingContext.get().getActiveContainer().getEventBus();
        ModLoadingContext.get().getActiveContainer().registerConfig(ModConfig.Type.SERVER,FeedbackConfig.SERVER);
        ModLoadingContext.get().getActiveContainer().registerConfig(ModConfig.Type.CLIENT,FeedbackConfig.CLIENT);
        FeedbackSounds.REGISTRY.register(bus);
        ArmorBreakParticles.REGISTRY.register(bus);
        NeoForge.EVENT_BUS.register(DamageTracker.class);
        NeoForge.EVENT_BUS.register(AttackWeaponFilter.class);
        dev.flower.apex_sound_effects.compat.TaczIntegration.registerServer();
    }
}
