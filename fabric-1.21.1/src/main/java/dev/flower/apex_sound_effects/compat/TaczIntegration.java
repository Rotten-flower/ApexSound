package dev.flower.apex_sound_effects.compat;

import dev.flower.apex_sound_effects.core.TaczIntegrationRules;
import org.slf4j.LoggerFactory;

/** 确认可选 TaCZ API 后再加载适配器。 / Loads optional TaCZ adapters after checking its API. */
public final class TaczIntegration {
    private TaczIntegration() {}

    private static boolean canEnable(boolean client) {
        String version = net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer("tacz")
                .map(mod -> mod.getMetadata().getVersion().getFriendlyString()).orElse(null);
        return TaczIntegrationRules.canEnable("fabric-1.21.1", version, client,
                TaczIntegration.class.getClassLoader(),
                net.minecraft.resources.ResourceLocation.class, net.minecraft.world.entity.Entity.class,
                net.minecraft.world.entity.LivingEntity.class,
                message -> LoggerFactory.getLogger("apex_sound_effects").warn(message));
    }

    public static void registerServer() {
        if (canEnable(false)) TaczHitHandler.register();
    }

    public static void registerClient() {
        if (canEnable(true)) TaczSounds.register();
    }
}
