package dev.flower.apex_sound_effects;

import dev.flower.apex_sound_effects.core.FeedbackSettings;
import dev.flower.apex_sound_effects.core.WeaponFeedbackRules;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Resolves allowed attackers and records the item used to launch projectiles. */
public final class AttackWeaponFilter {
    private static final String SOURCE_PREFIX = "apex_sound_effects_source_item=";
    private static volatile WeaponFeedbackRules rules = new WeaponFeedbackRules(List.of(), ignored -> {});
    private static volatile Map<String, TagKey<Item>> tags = Map.of();

    public static void configure(FeedbackSettings settings) {
        rules = new WeaponFeedbackRules(settings.weaponWhitelist(), entry ->
                org.slf4j.LoggerFactory.getLogger(ApexSound.MOD_ID).warn("Invalid weapon whitelist entry '{}'; it was ignored and preserved", entry));
        var compiled = new HashMap<String, TagKey<Item>>();
        for (String entry : settings.weaponWhitelist()) {
            String value = entry.trim();
            if (value.startsWith("#")) {
                ResourceLocation id = ResourceLocation.tryParse(value.substring(1));
                if (id != null) compiled.put(id.toString(), TagKey.create(Registries.ITEM, id));
            }
        }
        tags = Map.copyOf(compiled);
    }

    public static boolean targetAllowed(Entity target) {
        var settings = FeedbackConfig.settings();
        if (target instanceof Player) return settings.targetPlayers();
        if (target instanceof Enemy || target.getType().getCategory() == MobCategory.MONSTER) return settings.targetMonsters();
        return settings.targetAnimals();
    }

    public static boolean allows(ItemStack stack) { return allows(stack,null); }

    public static boolean allows(ItemStack stack,ResourceLocation gunId) {
        var settings = FeedbackConfig.settings();
        if (settings.weaponMode() == WeaponFeedbackRules.Mode.ALL) return true;
        if (settings.weaponMode() == WeaponFeedbackRules.Mode.TACZ_ONLY) return gunId!=null;
        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        return rules.allows(settings.weaponMode(), itemId, gunId==null?null:gunId.toString(), tag -> {
            TagKey<Item> key = tags.get(tag);
            return key != null && stack.is(key);
        });
    }

    public static boolean allowsGun(ResourceLocation gunId,Entity target) {
        if(target==null || !targetAllowed(target))return false;
        var item=BuiltInRegistries.ITEM.get(ResourceLocation.tryParse("tacz:modern_kinetic_gun"));
        return allows(new ItemStack(item),gunId);
    }

    public static ItemStack sourceStack(Entity direct, Player attacker) {
        if (direct instanceof Projectile projectile) {
            for (String tag : projectile.getTags()) {
                if (!tag.startsWith(SOURCE_PREFIX)) continue;
                ResourceLocation id = ResourceLocation.tryParse(tag.substring(SOURCE_PREFIX.length()));
                if (id != null) {
                    Item item = BuiltInRegistries.ITEM.get(id);
                    if (item != Items.AIR) return new ItemStack(item);
                }
            }
            if (direct instanceof ThrowableItemProjectile thrown) return thrown.getItem();
            if (direct instanceof ThrownTrident) return new ItemStack(Items.TRIDENT);
            return ItemStack.EMPTY;
        }
        return attacker.getMainHandItem();
    }

    public static void registerProjectileCapture() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (!(entity instanceof Projectile projectile) || hasSourceTag(projectile)) return;
            if (!(projectile.getOwner() instanceof LivingEntity shooter)) return;
            ItemStack source = ItemStack.EMPTY;
            if (projectile instanceof ThrowableItemProjectile thrown) source = thrown.getItem();
            else if (projectile instanceof ThrownTrident) source = new ItemStack(Items.TRIDENT);
            else if (projectile instanceof AbstractArrow arrow) {
                ItemStack used = shooter.getUseItem();
                if (shooter.isUsingItem() && (used.getItem() instanceof BowItem || used.getItem() instanceof CrossbowItem)) source = used;
                if (source.isEmpty()) {
                    for (var hand : net.minecraft.world.InteractionHand.values()) {
                        ItemStack held = shooter.getItemInHand(hand);
                        if (arrow.shotFromCrossbow() ? held.getItem() instanceof CrossbowItem : held.getItem() instanceof BowItem) {
                            source = held;
                            break;
                        }
                    }
                }
            }
            if (source.isEmpty()) source = shooter.getMainHandItem();
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(source.getItem());
            if (id != null) projectile.addTag(SOURCE_PREFIX + id);
        });
    }

    private static boolean hasSourceTag(Projectile projectile) {
        return projectile.getTags().stream().anyMatch(tag -> tag.startsWith(SOURCE_PREFIX));
    }

    private AttackWeaponFilter() {}
}
