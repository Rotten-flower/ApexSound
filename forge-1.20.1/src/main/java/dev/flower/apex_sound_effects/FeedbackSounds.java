package dev.flower.apex_sound_effects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.*;
/** 注册命中与终结音效。 / Registers hit and finish sound events. */
public final class FeedbackSounds {
    public static final DeferredRegister<SoundEvent> REGISTRY=DeferredRegister.create(ForgeRegistries.SOUND_EVENTS,ApexSound.MOD_ID);
    public static final RegistryObject<SoundEvent> ARMOR=add("armor_hit"), BREAK=add("shield_break"), FLESH=add("flesh_hit");
    public static final RegistryObject<SoundEvent> CRACK=add("shield_crack_hit"), ARROW_ARMOR=add("arrow_armor_hit"), ARROW_FLESH=add("arrow_flesh_hit");
    public static final RegistryObject<SoundEvent> FINISH_LIGHT=add("finish_light"), FINISH_HEAVY=add("finish_heavy"), FINISH_SHOTGUN=add("finish_shotgun"), FINISH_ARROW=add("finish_arrow");
    private static RegistryObject<SoundEvent> add(String id) {
        return REGISTRY.register(id,()->SoundEvent.createVariableRangeEvent(new ResourceLocation(ApexSound.MOD_ID,id)));
    }
}
