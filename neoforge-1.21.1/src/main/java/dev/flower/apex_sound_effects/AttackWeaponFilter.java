package dev.flower.apex_sound_effects;

import dev.flower.apex_sound_effects.core.WeaponFeedbackRules;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.*;

/** Launch snapshots live with their projectile, including when its chunk is saved and loaded. */
public final class AttackWeaponFilter {
    private static final String SOURCE="apex_sound_effects:source_item";
    private record Compiled(WeaponFeedbackRules rules,Map<String,TagKey<Item>> tags) {}
    private static volatile Compiled compiled=new Compiled(new WeaponFeedbackRules(List.of(),s->{}),Map.of());
    public static void configure(List<? extends String> entries,java.util.function.Consumer<String> invalid) {
        var parsedTags=new HashMap<String,TagKey<Item>>();
        var rules=new WeaponFeedbackRules(entries,invalid);
        for(String entry:entries)if(entry.trim().startsWith("#")) {
            var id=ResourceLocation.tryParse(entry.trim().substring(1));
            if(id!=null)parsedTags.put(id.toString(),TagKey.create(Registries.ITEM,id));
        }
        compiled=new Compiled(rules,Map.copyOf(parsedTags));
    }
    public static boolean targetAllowed(Entity target) {
        if(target instanceof Player)return FeedbackConfig.TARGET_PLAYERS.get();
        if(target instanceof Enemy || target.getType().getCategory()==MobCategory.MONSTER)return FeedbackConfig.TARGET_MONSTERS.get();
        return FeedbackConfig.TARGET_ANIMALS.get();
    }
    public static boolean allows(ItemStack stack,ResourceLocation gunId) {
        var mode=FeedbackConfig.WEAPON_MODE.get();
        if(mode==WeaponFeedbackRules.Mode.ALL)return true;
        if(mode==WeaponFeedbackRules.Mode.TACZ_ONLY)return gunId!=null;
        var view=compiled;
        String item=stack.isEmpty()?"minecraft:air":Objects.requireNonNull(BuiltInRegistries.ITEM.getKey(stack.getItem())).toString();
        return view.rules.allows(mode,item,gunId==null?null:gunId.toString(),tag->stack.is(view.tags.get(tag)));
    }
    public static boolean allowsGun(ResourceLocation gunId,Entity target) {
        if(target==null || !targetAllowed(target))return false;
        var item=BuiltInRegistries.ITEM.get(ResourceLocation.parse("tacz:modern_kinetic_gun"));
        return allows(new ItemStack(item),gunId);
    }
    public static ItemStack source(Entity direct,LivingEntity attacker) {
        if(direct instanceof Projectile) {
            var id=ResourceLocation.tryParse(direct.getPersistentData().getString(SOURCE));
            var item=id==null?null:BuiltInRegistries.ITEM.get(id);
            return item==null?ItemStack.EMPTY:new ItemStack(item);
        }
        return attacker.getMainHandItem();
    }
    @SubscribeEvent public static void joined(EntityJoinLevelEvent e) {
        if(e.getLevel().isClientSide || !(e.getEntity() instanceof Projectile projectile)
                || projectile.getPersistentData().contains(SOURCE))return;
        ItemStack stack=ItemStack.EMPTY;
        if(projectile instanceof ThrowableItemProjectile thrown)stack=thrown.getItem();
        else if(projectile instanceof ThrownTrident)stack=new ItemStack(Items.TRIDENT);
        else if(projectile.getOwner() instanceof LivingEntity shooter) {
            if(projectile instanceof AbstractArrow arrow) {
                // Bow release and crossbow shooting still have the launching stack in a hand.
                var used=shooter.getUseItem();
                if(shooter.isUsingItem() && (used.getItem() instanceof BowItem || used.getItem() instanceof CrossbowItem))stack=used;
                for(var hand:net.minecraft.world.InteractionHand.values()) {
                    if(!stack.isEmpty())break;
                    var held=shooter.getItemInHand(hand);
                    if(arrow.shotFromCrossbow()?held.getItem() instanceof CrossbowItem:held.getItem() instanceof BowItem) {stack=held;break;}
                }
            } else stack=shooter.isUsingItem()?shooter.getUseItem():shooter.getMainHandItem();
        }
        var id=BuiltInRegistries.ITEM.getKey(stack.getItem());
        if(id!=null)projectile.getPersistentData().putString(SOURCE,id.toString());
    }
    private AttackWeaponFilter() {}
}
