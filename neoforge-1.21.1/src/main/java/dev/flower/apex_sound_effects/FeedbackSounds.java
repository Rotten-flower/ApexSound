package dev.flower.apex_sound_effects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.minecraft.core.registries.BuiltInRegistries;
/** 注册命中与终结音效。 / Registers hit and finish sound events. */
public final class FeedbackSounds {
    public static final DeferredRegister<SoundEvent> REGISTRY=DeferredRegister.create(BuiltInRegistries.SOUND_EVENT,ApexSound.MOD_ID);
    public static final DeferredHolder<SoundEvent,SoundEvent> ARMOR=add("armor_hit"), BREAK=add("shield_break"), FLESH=add("flesh_hit");
    public static final DeferredHolder<SoundEvent,SoundEvent> CRACK=add("shield_crack_hit"), ARROW_ARMOR=add("arrow_armor_hit"), ARROW_FLESH=add("arrow_flesh_hit");
    public static final DeferredHolder<SoundEvent,SoundEvent> FINISH_LIGHT=add("finish_light"), FINISH_HEAVY=add("finish_heavy"), FINISH_SHOTGUN=add("finish_shotgun"), FINISH_ARROW=add("finish_arrow");
    private static DeferredHolder<SoundEvent,SoundEvent> add(String id) {
        return REGISTRY.register(id,()->SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ApexSound.MOD_ID,id)));
    }
}
