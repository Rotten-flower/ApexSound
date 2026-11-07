import dev.flower.apex_sound_effects.core.AmmoFeedbackRules;
import dev.flower.apex_sound_effects.core.AmmoKind;
import dev.flower.apex_sound_effects.core.ArmorFeedbackRules;
import dev.flower.apex_sound_effects.core.FeedbackSettings;
import dev.flower.apex_sound_effects.core.FeedbackSettingsStore;
import dev.flower.apex_sound_effects.core.WeaponFeedbackRules;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

public final class FeedbackSettingsCheck {
    public static void main(String[] args) throws Exception {
        var warnings = new ArrayList<String>();
        var defaults = FeedbackSettings.parse(new Properties(), warnings::add);
        require(defaults.enabled() && defaults.targetPlayers() && defaults.targetMonsters() && defaults.targetAnimals(), "default switches");
        require(defaults.weaponMode() == WeaponFeedbackRules.Mode.ALL, "default weapon mode");
        require(defaults.volume() == 1.0 && defaults.fleshRatio() == 0.5, "default values");

        var values = new Properties();
        values.setProperty("schemaVersion", "1");
        values.setProperty("fleshRatio", "NaN");
        values.setProperty("volume", "2.5");
        values.setProperty("weapons.mode", "WHITELIST");
        values.setProperty("weapons.whitelist", "minecraft:bow, #minecraft:arrows, malformed");
        values.setProperty("whiteMax", "40");
        values.setProperty("blueMax", "20");
        values.setProperty("purpleMax", "120");
        values.setProperty("goldMax", "160");
        var parsed = FeedbackSettings.parse(values, warnings::add);
        require(parsed.fleshRatio() == 0.5 && parsed.volume() == 1.0, "invalid numbers use defaults");
        require(parsed.weaponMode() == WeaponFeedbackRules.Mode.WHITELIST, "mode parsed");
        require(parsed.weaponWhitelist().equals(java.util.List.of("minecraft:bow", "#minecraft:arrows")), "valid whitelist entries retained");
        require(!ArmorFeedbackRules.validBands(parsed.whiteMax(), parsed.blueMax(), parsed.purpleMax(), parsed.goldMax()), "invalid band sequence retained for correction");
        require(ArmorFeedbackRules.colorFor(100, parsed.whiteMax(), parsed.blueMax(), parsed.purpleMax(), parsed.goldMax()) == ArmorFeedbackRules.ShieldColor.PURPLE, "invalid bands use safe defaults for colors");
        require(warnings.size() >= 4, "configuration issues diagnosed");

        var ammo = AmmoFeedbackRules.mapping(java.util.List.of("tacz:shared"), java.util.List.of("tacz:shared"), java.util.List.of(), java.util.List.of());
        require(AmmoFeedbackRules.classify("tacz:shared", ammo) == AmmoKind.LIGHT, "first configured ammo family wins");

        var directory = Files.createTempDirectory("apex-config-check-");
        var config = directory.resolve("apex_sound_effects.properties");
        Files.writeString(config, "schemaVersion=1\nvolume=0.37\nfleshRatio=NaN\nweapons.whitelist=minecraft:bow,bad-entry\n", StandardCharsets.UTF_8);
        var store = FeedbackSettingsStore.open(config, warnings::add);
        require(store.settings().volume() == 0.37 && store.settings().fleshRatio() == 0.5, "file values parsed independently");
        store.setVolume(0.75);
        store.save();
        var saved = new Properties();
        try (var reader = Files.newBufferedReader(config, StandardCharsets.UTF_8)) { saved.load(reader); }
        require("NaN".equals(saved.getProperty("fleshRatio")) && "minecraft:bow,bad-entry".equals(saved.getProperty("weapons.whitelist")), "invalid and valid file entries preserved on volume save");
        require("0.75".equals(saved.getProperty("volume")), "volume saved");

        var malformed = directory.resolve("broken.properties");
        byte[] brokenBytes = "schemaVersion=\\uQQQQ\nvolume=0.2\n".getBytes(StandardCharsets.UTF_8);
        Files.write(malformed, brokenBytes);
        var brokenStore = FeedbackSettingsStore.open(malformed, warnings::add);
        brokenStore.setVolume(0.9);
        brokenStore.save();
        require(java.util.Arrays.equals(brokenBytes, Files.readAllBytes(malformed)), "unreadable file remains untouched");
        var future = directory.resolve("future.properties");
        byte[] futureBytes = "schemaVersion=2\nvolume=0.2\n".getBytes(StandardCharsets.UTF_8);
        Files.write(future, futureBytes);
        var futureStore = FeedbackSettingsStore.open(future, warnings::add);
        futureStore.setVolume(0.8); futureStore.save();
        require(java.util.Arrays.equals(futureBytes, Files.readAllBytes(future)), "unknown schema remains untouched");
        require(!Files.exists(config.resolveSibling("apex_sound_effects.properties.tmp")), "temporary save file consumed");
        Files.delete(config); Files.delete(malformed); Files.delete(future); Files.delete(directory);
        System.out.println("FeedbackSettingsCheck: PASS (defaults, validation, safe bands, ammo precedence, persistence and preservation)");
    }

    private static void require(boolean value, String name) {
        if (!value) throw new AssertionError(name);
    }
}
