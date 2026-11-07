package dev.flower.apex_sound_effects.core;

import java.util.function.Consumer;

/** 在注册回调前检查可选枪械接口。 / Checks optional gun APIs before registering callbacks. */
public final class TaczIntegrationRules {
    private TaczIntegrationRules() {}

    public static boolean supportsVersion(String platform, String version) {
        if (version == null) return false;
        return switch (platform) {
            case "forge-1.20.1" -> version.equals("1.1.8-hotfix");
            case "neoforge-1.21.1" -> version.equals("1.1.8-hotfix-r7");
            case "fabric-1.20.1", "fabric-1.21.1" -> version.equals("0.7.1-forge1.1.8-hotfix2");
            default -> false;
        };
    }

    public static boolean canEnable(String platform, String version, boolean client,
                                    ClassLoader loader, Class<?> resourceId, Class<?> entity, Class<?> livingEntity,
                                    Consumer<String> warning) {
        if (version == null) return false;
        String side = client ? "client" : "server";
        if (!supportsVersion(platform, version)) {
            warning.accept("Optional TaCZ " + side + " integration skipped: unsupported version " + version
                    + " on " + platform + ". Basic attack feedback remains available.");
            return false;
        }
        try {
            verifyCommonApi(platform, loader, resourceId, entity, livingEntity);
            if (client) verifySoundApi(loader, resourceId);
            // Resolve callback signatures without initializing or registering the optional adapter.
            String adapter = "dev.flower.apex_sound_effects.compat."
                    + (client ? "TaczSounds" : "TaczHitHandler");
            Class.forName(adapter, false, loader).getDeclaredMethods();
            return true;
        } catch (ReflectiveOperationException | LinkageError failure) {
            warning.accept("Optional TaCZ " + side + " integration skipped: incompatible API on " + platform
                    + " (" + failure.getClass().getSimpleName() + "). Basic attack feedback remains available.");
            return false;
        }
    }

    private static void verifyCommonApi(String platform, ClassLoader loader, Class<?> resourceId, Class<?> entity, Class<?> livingEntity)
            throws ReflectiveOperationException {
        Class<?> pre = Class.forName("com.tacz.guns.api.event.common.EntityHurtByGunEvent$Pre", false, loader);
        Class<?> post = Class.forName("com.tacz.guns.api.event.common.EntityHurtByGunEvent$Post", false, loader);
        Class<?> kill = Class.forName("com.tacz.guns.api.event.common.EntityKillByGunEvent", false, loader);
        String logicalSide = platform.startsWith("fabric-") ? "cn.sh1rocu.tacz.api.LogicalSide"
                : platform.startsWith("neoforge-") ? "net.neoforged.fml.LogicalSide" : "net.minecraftforge.fml.LogicalSide";
        for (Class<?> event : new Class<?>[]{pre, post, kill}) {
            requireMethod(event, "getLogicalSide", logicalSide);
            requireMethod(event, "getAttacker", livingEntity.getName());
            requireMethod(event, "getBullet", entity.getName());
            requireMethod(event, "getGunId", resourceId.getName());
        }
        requireMethod(pre, "getHurtEntity", entity.getName());
        requireMethod(post, "getHurtEntity", entity.getName());
        requireMethod(post, "getGunDisplayId", resourceId.getName());
        requireMethod(post, "isHeadShot", "boolean");
        requireMethod(kill, "getKilledEntity", livingEntity.getName());
        requireMethod(kill, "getGunDisplayId", resourceId.getName());
        requireMethod(kill, "isHeadShot", "boolean");
        requireMethod(Class.forName("com.tacz.guns.entity.EntityKineticBullet", false, loader), "getAmmoId", resourceId.getName());
        if (platform.startsWith("fabric-")) {
            requireMethod(pre, "isCanceled", "boolean");
            Class<?> hurt = Class.forName("com.tacz.guns.api.event.common.EntityHurtByGunEvent", false, loader);
            for (String field : new String[]{"PRE", "POST"}) requireField(hurt, field, "net.fabricmc.fabric.api.event.Event");
            requireField(kill, "CALLBACK", "net.fabricmc.fabric.api.event.Event");
        }
    }

    private static void verifySoundApi(ClassLoader loader, Class<?> resourceId) throws ReflectiveOperationException {
        Class<?> api = Class.forName("com.tacz.guns.api.TimelessAPI", false, loader);
        requireMethod(api, "getGunDisplay", "java.util.Optional", resourceId, resourceId);
        Class<?> display = Class.forName("com.tacz.guns.client.resource.GunDisplayInstance", false, loader);
        requireMethod(display, "getSounds", resourceId.getName(), String.class);
        requireMethod(Class.forName("com.tacz.guns.client.sound.GunSoundInstance", false, loader), "getRegistryName", resourceId.getName());
        Class<?> sounds = Class.forName("com.tacz.guns.sound.SoundManager", false, loader);
        for (String field : new String[]{"FLESH_HIT_SOUND", "HEAD_HIT_SOUND", "KILL_SOUND"}) requireField(sounds, field, "java.lang.String");
    }

    private static void requireMethod(Class<?> owner, String name, String result, Class<?>... parameters)
            throws NoSuchMethodException {
        if (!owner.getMethod(name, parameters).getReturnType().getName().equals(result))
            throw new NoSuchMethodException(owner.getName() + "." + name + " has an incompatible return type");
    }

    private static void requireField(Class<?> owner, String name, String type) throws NoSuchFieldException {
        if (!owner.getField(name).getType().getName().equals(type))
            throw new NoSuchFieldException(owner.getName() + "." + name + " has an incompatible field type");
    }
}
