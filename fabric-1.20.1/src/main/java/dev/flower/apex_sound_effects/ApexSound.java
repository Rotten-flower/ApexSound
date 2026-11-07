package dev.flower.apex_sound_effects;

import dev.flower.apex_sound_effects.core.ModIdentity;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.core.particles.SimpleParticleType;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;

/** 注册本端音效、粒子和事件。 / Registers this platform’s sounds, particles and events. */
public final class ApexSound implements ModInitializer {
    public static final String MOD_ID = ModIdentity.ID;
    public static final SoundEvent ARMOR = sound("armor_hit");
    public static final SoundEvent BREAK = sound("shield_break");
    public static final SoundEvent FLESH = sound("flesh_hit");
    public static final SoundEvent CRACK = sound("shield_crack_hit");
    public static final SoundEvent ARROW_ARMOR = sound("arrow_armor_hit");
    public static final SoundEvent ARROW_FLESH = sound("arrow_flesh_hit");
    public static final SoundEvent FINISH_LIGHT = sound("finish_light");
    public static final SoundEvent FINISH_HEAVY = sound("finish_heavy");
    public static final SoundEvent FINISH_SHOTGUN = sound("finish_shotgun");
    public static final SoundEvent FINISH_ARROW = sound("finish_arrow");
    public static final SimpleParticleType SHARD = Registry.register(BuiltInRegistries.PARTICLE_TYPE,
            new ResourceLocation(MOD_ID, "shield_shard"), FabricParticleTypes.simple());
    private static SoundEvent sound(String path) {
        ResourceLocation id = new ResourceLocation(MOD_ID, path);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }
    @Override public void onInitialize() {
        if (!MOD_ID.equals(ModIdentity.ID)) throw new IllegalStateException("Platform ID differs from common ID");
        HitFeedback.initializeServer();
        GunPolicyNetwork.registerServer();
        dev.flower.apex_sound_effects.compat.TaczIntegration.registerServer();
        AttackWeaponFilter.registerProjectileCapture();
        ServerTickEvents.END_SERVER_TICK.register(HitFeedback::flush);
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server->HitFeedback.clear());
    }
}
