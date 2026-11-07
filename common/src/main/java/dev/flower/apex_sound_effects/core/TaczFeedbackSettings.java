package dev.flower.apex_sound_effects.core;

import java.util.*;
import java.util.function.Consumer;

/** 可选枪械反馈配置，保留未知项和错误项原文。 / Optional gun feedback settings; unknown or invalid values remain in the properties file. */
public record TaczFeedbackSettings(boolean replaceHitSounds, Map<String, AmmoKind> ammoMapping) {
    public TaczFeedbackSettings { ammoMapping = Map.copyOf(ammoMapping); }
    public AmmoKind ammoKind(String id) { return AmmoFeedbackRules.classify(id, ammoMapping); }
    public static TaczFeedbackSettings parse(Properties values, Consumer<String> warning) {
        String raw = values.getProperty("replaceTaczHitFeedback", "true").trim();
        boolean replace = true;
        if (raw.equalsIgnoreCase("false")) replace = false;
        else if (!raw.equalsIgnoreCase("true")) warning.accept("replaceTaczHitFeedback must be true or false; using true and preserving the value");
        var seen = new HashSet<String>();
        var light = ids(values, "ammo.light", AmmoFeedbackRules.LIGHT, seen, warning);
        var heavy = ids(values, "ammo.heavy", AmmoFeedbackRules.HEAVY, seen, warning);
        var shotgun = ids(values, "ammo.shotgun", AmmoFeedbackRules.SHOTGUN, seen, warning);
        var arrow = ids(values, "ammo.arrow", AmmoFeedbackRules.ARROW, seen, warning);
        return new TaczFeedbackSettings(replace, AmmoFeedbackRules.mapping(light, heavy, shotgun, arrow));
    }
    private static List<String> ids(Properties values, String key, List<String> defaults,
                                    Set<String> seen, Consumer<String> warning) {
        String raw = values.getProperty(key);
        var result = new ArrayList<String>();
        for (String value : raw == null ? defaults : Arrays.asList(raw.split(",", -1))) {
            String id = value.trim();
            if (id.isEmpty()) continue;
            if (!WeaponFeedbackRules.isValidId(id)) { warning.accept(key + " contains invalid ID '" + id + "'; ignored and preserved"); continue; }
            if (!seen.add(id)) warning.accept(key + " repeats ID '" + id + "'; using its first family and preserving the value");
            result.add(id);
        }
        return result;
    }
}
