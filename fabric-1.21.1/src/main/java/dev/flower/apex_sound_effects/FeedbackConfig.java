package dev.flower.apex_sound_effects;

import dev.flower.apex_sound_effects.core.FeedbackSettings;
import dev.flower.apex_sound_effects.core.FeedbackSettingsStore;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.LoggerFactory;

/** 管理服务端规则和客户端音量配置。 / Manages server rules and local sound settings. */
public final class FeedbackConfig {
    private static volatile FeedbackSettingsStore store;

    public static synchronized void initialize() {
        if (store != null) return;
        var log = LoggerFactory.getLogger(ApexSound.MOD_ID);
        var path = FabricLoader.getInstance().getConfigDir().resolve(ApexSound.MOD_ID + ".properties");
        store = FeedbackSettingsStore.open(path, message -> log.warn("{}", message));
        AttackWeaponFilter.configure(store.settings());
    }

    public static FeedbackSettings settings() {
        if(store==null) initialize();
        return store.settings();
    }

    public static dev.flower.apex_sound_effects.core.TaczFeedbackSettings taczSettings() {
        initialize();return store.taczSettings();
    }

    public static void setVolume(double value) {
        initialize();
        store.setVolume(value);
    }

    public static void save() {
        initialize();
        store.save();
    }

    private FeedbackConfig() {}
}
