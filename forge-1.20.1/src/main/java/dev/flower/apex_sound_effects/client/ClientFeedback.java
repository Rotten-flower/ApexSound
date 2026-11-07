package dev.flower.apex_sound_effects.client;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.flower.apex_sound_effects.*;
import dev.flower.apex_sound_effects.core.ArmorFeedbackRules.Kind;
import dev.flower.apex_sound_effects.core.AmmoKind;
import dev.flower.apex_sound_effects.core.CumulativeDamage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;
import org.joml.Vector2f;
import org.joml.Vector4f;
import org.joml.Matrix4f;
import net.minecraft.util.Mth;
import java.util.*;

@Mod.EventBusSubscriber(modid=ApexSound.MOD_ID,value=Dist.CLIENT)
/** 播放命中音效并绘制随目标移动的伤害数字。 / Plays hit sounds and renders damage labels beside the target. */
public final class ClientFeedback {
    private static final ResourceLocation SHIELD=new ResourceLocation(ApexSound.MOD_ID,"textures/gui/shield.png");
    private static final ResourceLocation BROKEN=new ResourceLocation(ApexSound.MOD_ID,"textures/gui/shield_broken.png");
    private static final Style DAMAGE_STYLE=Style.EMPTY.withFont(new ResourceLocation(ApexSound.MOD_ID,"damage"));
    private static FeedbackNetwork.Hit latest;
    private static long hitTime;
    private static long lastArmorSound;
    private static long lastFleshSound;
    private static long lastBreakSound;
    private static String damageText="0";
    private static final CumulativeDamage DAMAGE_STACK=new CumulativeDamage();
    private static final long HUD_LIFETIME=1_000_000_000L;
    private static final int MAX_LABELS=32;
    private static final Map<Integer,Label> LABELS=new LinkedHashMap<>();
    private static final Matrix4f WORLD_TO_CLIP=new Matrix4f();
    private static final Vector4f CLIP=new Vector4f();
    private static long projectionTime;
    private static final class Label {
        final FeedbackNetwork.Hit hit;
        final String text;
        final Component styledText;
        final float textWidth;
        final long time;
        final UUID entityUuid;
        double x,y,z;
        float bodyWidth=.6f,bodyHeight=1.8f;
        final Vector2f screen=new Vector2f();
        boolean visible;
        Label(FeedbackNetwork.Hit hit,String text,long time,net.minecraft.world.entity.Entity entity,Label previous) {
            this.hit=hit;this.text=text;this.time=time;
            styledText=Component.literal(text).withStyle(DAMAGE_STYLE);
            textWidth=Minecraft.getInstance().font.width(styledText)*1.35f;
            entityUuid=entity!=null?entity.getUUID():previous!=null?previous.entityUuid:null;
            if(entity!=null) {bodyWidth=entity.getBbWidth();bodyHeight=entity.getBbHeight();x=entity.getX();y=entity.getY()+bodyHeight;z=entity.getZ();}
            else if(previous!=null) {x=previous.x;y=previous.y;z=previous.z;bodyWidth=previous.bodyWidth;bodyHeight=previous.bodyHeight;}
            else {x=hit.x();y=hit.y()+.75;z=hit.z();}
        }
    }
    private static net.minecraft.client.multiplayer.ClientLevel feedbackLevel;
    private static final Deque<SimpleSoundInstance> SOUNDS=new ArrayDeque<>();
    private static final Map<SimpleSoundInstance,Kind> SOUND_KINDS=new IdentityHashMap<>();
    private record Finish(long due,AmmoKind ammo) {}
    private static final Deque<Finish> FINISHES=new ArrayDeque<>();
    public static void accept(FeedbackNetwork.Hit h) {
        var mc=Minecraft.getInstance();
        if(mc.player==null || mc.level==null || h.kind()==Kind.NONE || !Float.isFinite(h.damage()) || h.damage()<=0)return;
        if(feedbackLevel!=mc.level){DAMAGE_STACK.clear();LABELS.clear();projectionTime=0;feedbackLevel=mc.level;latest=null;}
        long now=System.nanoTime();
        damageText=CumulativeDamage.format(DAMAGE_STACK.add(h.target(),h.damage(),now,h.terminal()));
        latest=h;hitTime=now;
        LABELS.values().removeIf(label->now-label.time>=HUD_LIFETIME);
        var previous=LABELS.remove(h.target());
        if(LABELS.size()>=MAX_LABELS)LABELS.remove(LABELS.keySet().iterator().next());
        LABELS.put(h.target(),new Label(h,damageText,now,mc.level.getEntity(h.target()),previous));
        var event=switch(h.kind()) {
            case ARMOR -> h.ammo()==AmmoKind.ARROW?FeedbackSounds.ARROW_ARMOR.get():FeedbackSounds.ARMOR.get();
            case BREAK -> FeedbackSounds.BREAK.get();
            default -> h.ammo()==AmmoKind.ARROW?FeedbackSounds.ARROW_FLESH.get():FeedbackSounds.FLESH.get();
        };
        // UI feedback is local and unattenuated; a bystander cannot hear it.
        float volume=FeedbackConfig.VOLUME.get().floatValue();
        if(volume>0 && (!h.terminal() || h.kind()==Kind.BREAK) && shouldPlaySound(h.kind(),now)) {
            if(h.kind()==Kind.BREAK)lastBreakSound=now;
            var sound=SimpleSoundInstance.forUI(event,1.0F,volume);
            if(h.kind()==Kind.BREAK) {
                // A break is a priority event. Do not let a rifle's preceding armor pings
                // mask it, while retaining at most two overlapping break sounds.
                for(var it=SOUNDS.iterator();it.hasNext();) {
                    var old=it.next();
                    if(SOUND_KINDS.get(old)!=Kind.BREAK) { mc.getSoundManager().stop(old); SOUND_KINDS.remove(old); it.remove(); }
                }
                while(SOUNDS.size()>=2) { var old=SOUNDS.removeFirst(); SOUND_KINDS.remove(old); mc.getSoundManager().stop(old); }
            } else {
                while(SOUNDS.size()>=8) {
                    SimpleSoundInstance old=null;
                    for(var candidate:SOUNDS) if(SOUND_KINDS.get(candidate)!=Kind.BREAK){old=candidate;break;}
                    if(old==null)break;
                    SOUNDS.remove(old); SOUND_KINDS.remove(old); mc.getSoundManager().stop(old);
                }
            }
            SOUNDS.addLast(sound);SOUND_KINDS.put(sound,h.kind());mc.getSoundManager().play(sound);
            if(h.kind()==Kind.BREAK && h.ammo()!=AmmoKind.ARROW) {
                var crack=SimpleSoundInstance.forUI(FeedbackSounds.CRACK.get(),1.0F,volume);
                SOUNDS.addLast(crack);SOUND_KINDS.put(crack,Kind.BREAK);mc.getSoundManager().play(crack);
            }
        }
        if(h.terminal()) {
            if(FINISHES.size()>=8)FINISHES.removeFirst();
            // Start after the shield transient without changing the real death timing.
            FINISHES.addLast(new Finish(now+(h.kind()==Kind.BREAK?220_000_000L:0),h.ammo()));
        }
        if(h.kind()!=Kind.FLESH) {
            int c=h.color(); var tint=new Vector3f(((c>>16)&255)/255f,((c>>8)&255)/255f,(c&255)/255f);
            int count=h.kind()==Kind.BREAK?24:5;
            for(int i=0;i<count;i++) {
                double angle=i*Math.PI*2/count;
                double radius=h.kind()==Kind.BREAK?0.36:0.12;
                var particle=mc.particleEngine.createParticle(ArmorBreakParticles.SHARD.get(),
                        h.x()+Math.cos(angle)*radius,h.y()+(mc.level.random.nextFloat()-.5)*0.6,h.z()+Math.sin(angle)*radius,
                        Math.cos(angle)*0.08,0.04,Math.sin(angle)*0.08);
                if(particle!=null){particle.setColor(tint.x,tint.y,tint.z);particle.setLifetime(h.kind()==Kind.BREAK?12:5);particle.setParticleSpeed(Math.cos(angle)*.09,.025,Math.sin(angle)*.09);}
            }
        }
    }
    private static boolean shouldPlaySound(Kind kind,long now) {
        if(kind!=Kind.BREAK && now-lastBreakSound<180_000_000L)return false;
        if(kind==Kind.ARMOR) {
            if(now-lastArmorSound<55_000_000L)return false;
            lastArmorSound=now;
        } else if(kind==Kind.FLESH) {
            if(now-lastFleshSound<35_000_000L)return false;
            lastFleshSound=now;
        }
        return true;
    }
    private static void finish(Finish pending) {
        var mc=Minecraft.getInstance();
        float volume=FeedbackConfig.VOLUME.get().floatValue();
        if(volume<=0)return;
        var event=switch(pending.ammo()) {
            case HEAVY -> FeedbackSounds.FINISH_HEAVY.get();
            case SHOTGUN -> FeedbackSounds.FINISH_SHOTGUN.get();
            case ARROW -> FeedbackSounds.FINISH_ARROW.get();
            default -> FeedbackSounds.FINISH_LIGHT.get();
        };
        for(var it=SOUNDS.iterator();it.hasNext();) {
            var old=it.next();
            if(SOUND_KINDS.get(old)==Kind.ARMOR || SOUND_KINDS.get(old)==Kind.FLESH) {
                it.remove();SOUND_KINDS.remove(old);mc.getSoundManager().stop(old);
            }
        }
        while(SOUNDS.size()>=8) {var old=SOUNDS.removeFirst();SOUND_KINDS.remove(old);mc.getSoundManager().stop(old);}
        var sound=SimpleSoundInstance.forUI(event,1.0F,volume);
        SOUNDS.addLast(sound);SOUND_KINDS.put(sound,Kind.NONE);mc.getSoundManager().play(sound);
    }
    @SubscribeEvent public static void worldFrame(RenderLevelStageEvent e) {
        // AFTER_LEVEL receives a projection pose in 1.20.1; AFTER_PARTICLES receives the world view pose.
        if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES)return;
        var mc=Minecraft.getInstance();
        if(mc.level==null || mc.level!=feedbackLevel)return;
        WORLD_TO_CLIP.set(e.getProjectionMatrix()).mul(e.getPoseStack().last().pose());
        var camera=e.getCamera().getPosition();
        float partial=e.getPartialTick();
        projectionTime=System.nanoTime();
        for(var label:LABELS.values()) {
            var entity=mc.level.getEntity(label.hit.target());
            if(entity!=null && !entity.isRemoved() && entity.getUUID().equals(label.entityUuid)
                    && (!(entity instanceof net.minecraft.world.entity.LivingEntity living) || living.isAlive())) {
                label.bodyWidth=entity.getBbWidth();label.bodyHeight=entity.getBbHeight();
                label.x=Mth.lerp(partial,entity.xo,entity.getX());
                label.y=Mth.lerp(partial,entity.yo,entity.getY())+label.bodyHeight;
                label.z=Mth.lerp(partial,entity.zo,entity.getZ());
            }
            label.visible=TargetLabelProjection.upperLeft(WORLD_TO_CLIP,label.x-camera.x,label.y-camera.y,label.z-camera.z,label.bodyWidth,label.bodyHeight,
                    mc.getWindow().getGuiScaledWidth(),mc.getWindow().getGuiScaledHeight(),CLIP,label.screen);
        }
    }
    public static void render(GuiGraphics g,int w,int h) {
        var mc=Minecraft.getInstance();
        if(mc.player==null || mc.level==null || mc.options.hideGui || mc.player.isSpectator() || LABELS.isEmpty())return;
        long now=System.nanoTime();
        if(now-projectionTime>=250_000_000L)return;
        float scale=(float)(Math.max(480,mc.getWindow().getHeight())/720.0*1.5/mc.getWindow().getGuiScale());
        RenderSystem.enableBlend();
        for(var label:LABELS.values())if(label.visible && now-label.time<HUD_LIFETIME)renderLabel(g,label,scale,now);
        g.setColor(1,1,1,1);RenderSystem.disableBlend();
    }
    private static void renderLabel(GuiGraphics g,Label label,float scale,long now) {
        var mc=Minecraft.getInstance();
        var hit=label.hit;
        double age=(now-label.time)/1_000_000_000.0;
        float alpha=(float)Math.min(1,(1.0-age)/.18);
        if(alpha<.02f)return;
        int color=hit.headshot()?0xFFD45A:hit.kind()==Kind.FLESH?0xFF4A4A:hit.color();
        int argb=((int)(alpha*255)<<24)|color;
        var text=label.styledText;
        float textWidth=label.textWidth;
        float width=textWidth+(hit.kind()==Kind.FLESH?0:15);
        g.pose().pushPose();
        g.pose().translate(label.screen.x,label.screen.y,0);g.pose().scale(scale,scale,1);
        // The whole group sits outside the projected body's upper-left corner.
        g.pose().translate(-width-6,-12-Math.min(age,.18)*22,0);
        if(hit.kind()!=Kind.FLESH) {
            g.setColor(((color>>16)&255)/255f,((color>>8)&255)/255f,(color&255)/255f,alpha);
            int shift=hit.kind()==Kind.BREAK?(int)(Math.sin(age*70)*Math.max(0,3-age*12)):0;
            g.blit(hit.kind()==Kind.BREAK?BROKEN:SHIELD,(int)textWidth+3+shift,-3,12,14,0f,0f,64,64,64,64);
        }
        g.setColor(1,1,1,1);
        g.pose().pushPose();g.pose().scale(1.35f,1.35f,1);
        g.drawString(mc.font,text,0,0,argb,true);g.pose().popPose();g.pose().popPose();
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e) {
        if(e.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();
        for(var it=SOUNDS.iterator();it.hasNext();) {
            var sound=it.next();if(!mc.getSoundManager().isActive(sound)){it.remove();SOUND_KINDS.remove(sound);}
        }
        if(mc.level!=feedbackLevel){DAMAGE_STACK.clear();LABELS.clear();projectionTime=0;feedbackLevel=mc.level;latest=null;damageText="0";}
        if(mc.level==null){latest=null;damageText="0";DAMAGE_STACK.clear();SOUNDS.clear();SOUND_KINDS.clear();FINISHES.clear();lastArmorSound=0;lastFleshSound=0;lastBreakSound=0;return;}
        long now=System.nanoTime();
        LABELS.values().removeIf(label->now-label.time>=HUD_LIFETIME);
        // Short delay is bounded and checked locally; an unrelated shot cannot erase it.
        for(var it=FINISHES.iterator();it.hasNext();) {var pending=it.next();if(now>=pending.due()){it.remove();finish(pending);}}
    }
    @Mod.EventBusSubscriber(modid=ApexSound.MOD_ID,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
    public static final class Overlay {
        @SubscribeEvent public static void particles(RegisterParticleProvidersEvent e) {
            e.registerSpriteSet(ArmorBreakParticles.SHARD.get(),ArmorShardParticle.Provider::new);
        }
        @SubscribeEvent public static void setup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent e) {
            e.enqueueWork(()->{
                dev.flower.apex_sound_effects.compat.TaczIntegration.registerClient();
            });
        }
        @SubscribeEvent public static void register(RegisterGuiOverlaysEvent e) {
            e.registerAboveAll("hit_feedback",(gui,g,partial,w,h)->render(g,w,h));
        }
    }
}
