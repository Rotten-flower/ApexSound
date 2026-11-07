package dev.flower.apex_sound_effects;

import dev.flower.apex_sound_effects.core.ArmorFeedbackRules.ShieldColor;
import net.neoforged.neoforge.common.ModConfigSpec;
import dev.flower.apex_sound_effects.core.ArmorFeedbackRules;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.slf4j.LoggerFactory;
import dev.flower.apex_sound_effects.core.AmmoKind;
import dev.flower.apex_sound_effects.core.AmmoFeedbackRules;
import dev.flower.apex_sound_effects.core.WeaponFeedbackRules.Mode;
import net.minecraft.resources.ResourceLocation;
import java.util.*;

@EventBusSubscriber(modid="apex_sound_effects",bus=EventBusSubscriber.Bus.MOD)
/** 管理服务端规则和客户端音量配置。 / Manages server rules and local sound settings. */
public final class FeedbackConfig {
    public static final ModConfigSpec SERVER, CLIENT;
    public static final ModConfigSpec.DoubleValue FLESH_RATIO, VOLUME;
    public static final ModConfigSpec.DoubleValue WHITE_MAX, BLUE_MAX, PURPLE_MAX, GOLD_MAX;
    public static final ModConfigSpec.BooleanValue ENABLED, REPLACE_TACZ;
    public static final ModConfigSpec.BooleanValue TARGET_PLAYERS,TARGET_MONSTERS,TARGET_ANIMALS;
    public static final ModConfigSpec.EnumValue<Mode> WEAPON_MODE;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> WEAPON_WHITELIST;
    private static final ModConfigSpec.IntValue SERVER_SCHEMA, CLIENT_SCHEMA;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> LIGHT_AMMO, HEAVY_AMMO, SHOTGUN_AMMO, ARROW_AMMO;
    private static volatile Map<String,AmmoKind> ammoMapping=AmmoFeedbackRules.mapping(AmmoFeedbackRules.LIGHT,AmmoFeedbackRules.HEAVY,AmmoFeedbackRules.SHOTGUN,AmmoFeedbackRules.ARROW);
    static {
        var s=new ModConfigSpec.Builder();
        SERVER_SCHEMA=s.comment("Configuration format version.").defineInRange("schemaVersion",1,1,1);
        ENABLED=s.translation("config.apex_sound_effects.enabled").define("enabled",true);
        FLESH_RATIO=s.comment("Health at or below this fraction counts as flesh. Above it counts as armor.")
                .translation("config.apex_sound_effects.flesh_ratio").defineInRange("fleshRatio",0.5,0.05,0.95);
        s.comment("Maximum health in HP (2 HP = 1 heart). Upper bounds are inclusive and must increase.")
                .push("healthBands");
        WHITE_MAX=s.translation("config.apex_sound_effects.white_max").defineInRange("whiteMax",40.0,0.1,1000000.0);
        BLUE_MAX=s.translation("config.apex_sound_effects.blue_max").defineInRange("blueMax",80.0,0.1,1000000.0);
        PURPLE_MAX=s.translation("config.apex_sound_effects.purple_max").defineInRange("purpleMax",120.0,0.1,1000000.0);
        GOLD_MAX=s.translation("config.apex_sound_effects.gold_max").defineInRange("goldMax",160.0,0.1,1000000.0);
        s.pop();
        s.push("targets");
        TARGET_PLAYERS=s.translation("config.apex_sound_effects.target_players").define("players",true);
        TARGET_MONSTERS=s.translation("config.apex_sound_effects.target_monsters").define("monsters",true);
        TARGET_ANIMALS=s.comment("Animals and other non-hostile living targets, including fish, bats and villagers.")
                .translation("config.apex_sound_effects.target_animals").define("animals",true);
        s.pop();
        s.push("weapons");
        WEAPON_MODE=s.comment("ALL: all player attacks; TACZ_ONLY: TaCZ bullets; WHITELIST: matching items, gun IDs or item tags.")
                .translation("config.apex_sound_effects.weapon_mode").defineEnum("mode",Mode.ALL);
        WEAPON_WHITELIST=s.comment("Used only in WHITELIST mode. Exact item IDs, exact TaCZ gun IDs, or #item_tag. Example: minecraft:bow, tacz:m4a1, #forge:tools/bows. Empty list matches nothing.")
                .translation("config.apex_sound_effects.weapon_whitelist").defineListAllowEmpty(List.of("whitelist"),List.<String>of(),v->v instanceof String);
        s.pop();
        s.comment("Exact TaCZ ammo IDs. Unknown ammo uses generic bullet feedback. Duplicate IDs use the first family: light, heavy, shotgun, arrow.").push("ammoSounds");
        LIGHT_AMMO=s.translation("config.apex_sound_effects.light_ammo").defineListAllowEmpty(List.of("lightAmmo"),AmmoFeedbackRules.LIGHT,FeedbackConfig::validAmmoId);
        HEAVY_AMMO=s.translation("config.apex_sound_effects.heavy_ammo").defineListAllowEmpty(List.of("heavyAmmo"),AmmoFeedbackRules.HEAVY,FeedbackConfig::validAmmoId);
        SHOTGUN_AMMO=s.translation("config.apex_sound_effects.shotgun_ammo").defineListAllowEmpty(List.of("shotgunAmmo"),AmmoFeedbackRules.SHOTGUN,FeedbackConfig::validAmmoId);
        ARROW_AMMO=s.translation("config.apex_sound_effects.arrow_ammo").defineListAllowEmpty(List.of("arrowAmmo"),AmmoFeedbackRules.ARROW,FeedbackConfig::validAmmoId);
        s.pop();
        SERVER=s.build();
        var c=new ModConfigSpec.Builder();
        CLIENT_SCHEMA=c.defineInRange("schemaVersion",1,1,1);
        VOLUME=c.translation("config.apex_sound_effects.volume").defineInRange("volume",1.0,0,1);
        REPLACE_TACZ=c.translation("config.apex_sound_effects.replace_tacz").define("replaceTaczHitFeedback",true);
        CLIENT=c.build();
    }
    public static ShieldColor armorColorForMaximumHealth(float maximum) {
        return ArmorFeedbackRules.colorFor(maximum,WHITE_MAX.get(),BLUE_MAX.get(),PURPLE_MAX.get(),GOLD_MAX.get());
    }
    private static boolean validAmmoId(Object value) {return value instanceof String id && ResourceLocation.tryParse(id)!=null;}
    public static AmmoKind ammoKind(ResourceLocation id) {return AmmoFeedbackRules.classify(id==null?null:id.toString(),ammoMapping);}
    @SubscribeEvent public static void loaded(ModConfigEvent.Loading e){validate(e);}
    @SubscribeEvent public static void reloaded(ModConfigEvent.Reloading e){validate(e);}
    private static void validate(ModConfigEvent e) {
        if(!e.getConfig().getModId().equals("apex_sound_effects"))return;
        if(e.getConfig().getSpec()==SERVER) {
            if(!ArmorFeedbackRules.validBands(WHITE_MAX.get(),BLUE_MAX.get(),PURPLE_MAX.get(),GOLD_MAX.get()))
                LoggerFactory.getLogger("apex_sound_effects").warn("{}: healthBands must satisfy whiteMax < blueMax < purpleMax < goldMax. Using 40/80/120/160 for colors; configured values are preserved.",e.getConfig().getFileName());
            var all=new HashSet<String>();
            for(var list:List.of(LIGHT_AMMO.get(),HEAVY_AMMO.get(),SHOTGUN_AMMO.get(),ARROW_AMMO.get()))
                for(String id:list)if(!all.add(id))LoggerFactory.getLogger("apex_sound_effects").warn("{}: ammoSounds contains duplicate ID {}; using its first family, configured lists preserved",e.getConfig().getFileName(),id);
            ammoMapping=AmmoFeedbackRules.mapping(LIGHT_AMMO.get(),HEAVY_AMMO.get(),SHOTGUN_AMMO.get(),ARROW_AMMO.get());
            AttackWeaponFilter.configure(WEAPON_WHITELIST.get(),entry->LoggerFactory.getLogger("apex_sound_effects").warn("{}: weapons.whitelist invalid entry '{}'; ignored while valid entries and configured data are preserved",e.getConfig().getFileName(),entry));
        }
    }
    private FeedbackConfig() {}
}
