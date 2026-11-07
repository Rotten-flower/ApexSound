package dev.flower.apex_sound_effects.core;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.AtomicMoveNotSupportedException;
import java.util.Properties;
import java.util.function.Consumer;

/** 读写 UTF-8 配置，错误项使用默认值并保留原文以便修正。 / Small UTF-8 properties store. Invalid entries use defaults and remain on disk for correction. */
public final class FeedbackSettingsStore {
    private final Path path;
    private final Consumer<String> warning;
    private final Properties values;
    private final boolean writable;
    private volatile FeedbackSettings settings;
    private final TaczFeedbackSettings taczSettings;
    private volatile boolean dirty;

    private FeedbackSettingsStore(Path path, Consumer<String> warning, Properties values, boolean writable) {
        this.path = path;
        this.warning = warning;
        this.values = values;
        this.writable = writable;
        this.settings = FeedbackSettings.parse(values, warning);
        this.taczSettings = TaczFeedbackSettings.parse(writable ? values : new Properties(), warning);
    }

    public static FeedbackSettingsStore open(Path path, Consumer<String> warning) {
        Properties values = new Properties();
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                values.load(reader);
            } catch (IOException | IllegalArgumentException error) {
                warning.accept(path + ": could not read settings; using defaults and leaving the file untouched (" + error.getMessage() + ")");
                return new FeedbackSettingsStore(path, warning, new Properties(), false);
            }
            String schema = values.getProperty("schemaVersion", "1").trim();
            if (!schema.equals("1")) warning.accept(path + ": unsupported schemaVersion " + schema + "; using defaults and leaving the file untouched");
            return new FeedbackSettingsStore(path, warning, values, schema.equals("1"));
        } else {
            values = defaults();
            var store = new FeedbackSettingsStore(path, warning, values, true);
            store.dirty = true;
            store.save();
            return store;
        }
    }

    public FeedbackSettings settings() { return settings; }
    public TaczFeedbackSettings taczSettings() { return taczSettings; }

    public void setVolume(double value) {
        var updated = settings.withVolume(value);
        if (updated.volume() != settings.volume()) {
            settings = updated;
            dirty = true;
        }
    }

    /** Save only the user-edited client volume, preserving all other and unknown properties. */
    public synchronized void save() {
        if (!writable || !dirty) return;
        values.setProperty("volume", Double.toString(settings.volume()));
        Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                values.store(writer, null);
            }
            try {
                Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
            dirty = false;
        } catch (IOException error) {
            warning.accept(path + ": could not save settings (" + error.getMessage() + ")");
        }
    }

    private static Properties defaults() {
        var p = new Properties();
        p.setProperty("schemaVersion", "1");
        p.setProperty("enabled", "true");
        p.setProperty("fleshRatio", "0.5");
        p.setProperty("whiteMax", "40.0");
        p.setProperty("blueMax", "80.0");
        p.setProperty("purpleMax", "120.0");
        p.setProperty("goldMax", "160.0");
        p.setProperty("targets.players", "true");
        p.setProperty("targets.monsters", "true");
        p.setProperty("targets.animals", "true");
        p.setProperty("weapons.mode", "ALL");
        p.setProperty("weapons.whitelist", "");
        p.setProperty("volume", "1.0");
        p.setProperty("replaceTaczHitFeedback", "true");
        p.setProperty("ammo.light", String.join(",", AmmoFeedbackRules.LIGHT));
        p.setProperty("ammo.heavy", String.join(",", AmmoFeedbackRules.HEAVY));
        p.setProperty("ammo.shotgun", String.join(",", AmmoFeedbackRules.SHOTGUN));
        p.setProperty("ammo.arrow", String.join(",", AmmoFeedbackRules.ARROW));
        return p;
    }
}
