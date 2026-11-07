package dev.flower.apex_sound_effects.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;

/** Short lived, emissive pieces of the shield hexagon; created on the attacker's client only. */
public final class ArmorShardParticle extends TextureSheetParticle {
    private ArmorShardParticle(ClientLevel level,double x,double y,double z,double dx,double dy,double dz,SpriteSet sprites) {
        super(level,x,y,z,dx,dy,dz);
        xd=dx;yd=dy;zd=dz;lifetime=9+random.nextInt(5);quadSize=0.065f+random.nextFloat()*.045f;
        hasPhysics=false;friction=.87f;gravity=.08f;roll=random.nextFloat()*6.283f;oRoll=roll;
        pickSprite(sprites);
    }
    @Override public void tick(){super.tick();oRoll=roll;roll+=.12f;alpha=Math.max(0,1-age/(float)lifetime);}
    @Override public int getLightColor(float partial){return 15728880;}
    @Override public ParticleRenderType getRenderType(){return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;}
    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        public Provider(SpriteSet sprites){this.sprites=sprites;}
        @Override public Particle createParticle(SimpleParticleType type,ClientLevel level,double x,double y,double z,double dx,double dy,double dz) {
            return new ArmorShardParticle(level,x,y,z,dx,dy,dz,sprites);
        }
    }
}
