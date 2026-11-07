package dev.flower.apex_sound_effects.client;

import net.minecraft.client.resources.sounds.SoundInstance;
import java.util.function.Predicate;
import dev.flower.apex_sound_effects.core.GunFeedbackPolicy;
import dev.flower.apex_sound_effects.core.WeaponFeedbackRules;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Keeps the always-present sound mixin independent of optional TaCZ classes. */
public final class TaczSoundFilter {
    private static Predicate<SoundInstance> filter=ignored->false;
    private static GunFeedbackPolicy policy;
    private static WeaponFeedbackRules rules;
    public static void setServerPolicy(GunFeedbackPolicy value) {
        policy=value;
        rules=value==null?null:new WeaponFeedbackRules(value.whitelist(),ignored->{});
    }
    public static boolean allowsGun(ResourceLocation gun,Entity target) {
        var current=policy;
        if(current==null || !current.enabled() || target==null)return false;
        boolean allowed=target instanceof Player?current.players()
                :target instanceof Enemy || target.getType().getCategory()==MobCategory.MONSTER?current.monsters():current.animals();
        if(!allowed)return false;
        var stack=new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.tryParse("tacz:modern_kinetic_gun")));
        return rules.allows(current.mode(),"tacz:modern_kinetic_gun",gun==null?null:gun.toString(),tag->{
            var id=ResourceLocation.tryParse(tag);
            return id!=null && stack.is(TagKey.create(Registries.ITEM,id));
        });
    }
    public static void register(Predicate<SoundInstance> replacement){filter=replacement;}
    public static boolean replaces(SoundInstance sound){return filter.test(sound);}
    private TaczSoundFilter() {}
}
