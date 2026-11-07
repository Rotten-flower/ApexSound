package dev.flower.apex_sound_effects;
import net.minecraft.core.particles.*;
import net.minecraftforge.registries.*;
public final class ArmorBreakParticles {
    public static final DeferredRegister<ParticleType<?>> REGISTRY=DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES,ApexSound.MOD_ID);
    public static final RegistryObject<SimpleParticleType> SHARD=REGISTRY.register("shield_shard",()->new SimpleParticleType(false));
}
