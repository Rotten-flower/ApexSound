package dev.flower.apex_sound_effects.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;

/** Fabric 共用的配置模型，独立校验且不依赖加载器。 / Validated, loader-independent settings shared by the Fabric adapters. */
public record FeedbackSettings(
        boolean enabled,
        double fleshRatio,
        double whiteMax,
        double blueMax,
        double purpleMax,
        double goldMax,
        boolean targetPlayers,
        boolean targetMonsters,
        boolean targetAnimals,
        WeaponFeedbackRules.Mode weaponMode,
        List<String> weaponWhitelist,
        double volume) {

    public static final FeedbackSettings DEFAULT = new FeedbackSettings(
            true, 0.5, 40, 80, 120, 160,
            true, true, true, WeaponFeedbackRules.Mode.ALL, List.of(),
            1.0);

    public FeedbackSettings {
        weaponWhitelist = List.copyOf(weaponWhitelist);
    }

    public FeedbackSettings withVolume(double value) {
        return new FeedbackSettings(enabled, fleshRatio, whiteMax, blueMax, purpleMax, goldMax,
                targetPlayers, targetMonsters, targetAnimals, weaponMode, weaponWhitelist,
                clamp(value, 0, 1));
    }

    public static FeedbackSettings parse(Properties values, Consumer<String> warning) {
        FeedbackSettings d = DEFAULT;
        String schemaVersion = values.getProperty("schemaVersion");
        if (schemaVersion != null && !schemaVersion.trim().equals("1")) {
            warning.accept("schemaVersion must be 1; using safe defaults and preserving the configured file");
            return d;
        }
        var result = new FeedbackSettings(
                bool(values, "enabled", d.enabled, warning),
                decimal(values, "fleshRatio", d.fleshRatio, 0.05, 0.95, warning),
                decimal(values, "whiteMax", d.whiteMax, 0.1, 1_000_000, warning),
                decimal(values, "blueMax", d.blueMax, 0.1, 1_000_000, warning),
                decimal(values, "purpleMax", d.purpleMax, 0.1, 1_000_000, warning),
                decimal(values, "goldMax", d.goldMax, 0.1, 1_000_000, warning),
                bool(values, "targets.players", d.targetPlayers, warning),
                bool(values, "targets.monsters", d.targetMonsters, warning),
                bool(values, "targets.animals", d.targetAnimals, warning),
                mode(values, warning),
                ids(values, "weapons.whitelist", d.weaponWhitelist, true, warning),
                decimal(values, "volume", d.volume, 0, 1, warning));
        if (!ArmorFeedbackRules.validBands(result.whiteMax, result.blueMax, result.purpleMax, result.goldMax)) {
            warning.accept("health bands must increase from white to gold; default colors will be used and configured values were preserved");
        }
        return result;
    }

    private static boolean bool(Properties values, String key, boolean fallback, Consumer<String> warning) {
        String raw = values.getProperty(key);
        if (raw == null) return fallback;
        if (raw.equalsIgnoreCase("true")) return true;
        if (raw.equalsIgnoreCase("false")) return false;
        warning.accept(key + " must be true or false; using the default");
        return fallback;
    }

    private static double decimal(Properties values, String key, double fallback, double min, double max,
                                  Consumer<String> warning) {
        String raw = values.getProperty(key);
        if (raw == null) return fallback;
        try {
            double parsed = Double.parseDouble(raw.trim());
            if (Double.isFinite(parsed) && parsed >= min && parsed <= max) return parsed;
        } catch (NumberFormatException ignored) {
            // Invalid input is reported below and left untouched in the properties file.
        }
        warning.accept(key + " must be a number from " + min + " to " + max + "; using the default");
        return fallback;
    }

    private static WeaponFeedbackRules.Mode mode(Properties values, Consumer<String> warning) {
        String raw = values.getProperty("weapons.mode");
        if (raw == null) return DEFAULT.weaponMode;
        try {
            return WeaponFeedbackRules.Mode.valueOf(raw.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            warning.accept("weapons.mode is invalid; using ALL");
            return DEFAULT.weaponMode;
        }
    }

    private static List<String> ids(Properties values, String key, List<String> fallback, boolean allowTags,
                                    Consumer<String> warning) {
        String raw = values.getProperty(key);
        if (raw == null) return fallback;
        var result = new ArrayList<String>();
        var seen = new HashSet<String>();
        for (String piece : raw.split(",", -1)) {
            String value = piece.trim();
            if (value.isEmpty()) continue;
            boolean tag = value.startsWith("#");
            String id = tag ? value.substring(1) : value;
            if ((tag && !allowTags) || !WeaponFeedbackRules.isValidId(id)) {
                warning.accept(key + " contains invalid entry '" + value + "'; that entry was ignored and preserved");
                continue;
            }
            if (seen.add(value)) result.add(value);
        }
        return List.copyOf(result);
    }

    private static double clamp(double value, double min, double max) {
        return Double.isFinite(value) ? Math.max(min, Math.min(max, value)) : min;
    }
}
