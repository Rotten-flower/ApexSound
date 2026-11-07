package dev.flower.apex_sound_effects;
import net.minecraft.core.particles.*;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.minecraft.core.registries.BuiltInRegistries;
public final class ArmorBreakParticles {
    public static final DeferredRegister<ParticleType<?>> REGISTRY=DeferredRegister.create(BuiltInRegistries.PARTICLE_TYPE,ApexSound.MOD_ID);
    public static final DeferredHolder<ParticleType<?>,SimpleParticleType> SHARD=REGISTRY.register("shield_shard",()->new SimpleParticleType(false));
}
