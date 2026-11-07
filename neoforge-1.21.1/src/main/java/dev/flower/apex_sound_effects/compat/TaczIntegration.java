package dev.flower.apex_sound_effects.compat;

import dev.flower.apex_sound_effects.core.TaczIntegrationRules;
import org.slf4j.LoggerFactory;

/** 确认可选 TaCZ API 后再加载适配器。 / Loads optional TaCZ adapters after checking its API. */
public final class TaczIntegration {
    private TaczIntegration() {}

    private static boolean canEnable(boolean client) {
        String version = net.neoforged.fml.ModList.get().getModContainerById("tacz")
                .map(mod -> mod.getModInfo().getVersion().toString()).orElse(null);
        return TaczIntegrationRules.canEnable("neoforge-1.21.1", version, client,
                TaczIntegration.class.getClassLoader(),
                net.minecraft.resources.ResourceLocation.class, net.minecraft.world.entity.Entity.class,
                net.minecraft.world.entity.LivingEntity.class,
                message -> LoggerFactory.getLogger("apex_sound_effects").warn(message));
    }

    public static void registerServer() {
        if (canEnable(false)) net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(TaczHitHandler.class);
    }

    public static void registerClient() {
        if (canEnable(true)) net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(TaczSounds.class);
    }
}
