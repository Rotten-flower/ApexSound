package dev.flower.apex_sound_effects.core;

import java.util.List;

/** 入服时同步服务端适用规则，不同步本地音量和配置文件。 / Server-owned eligibility mirrored once on joining, without client volume or file contents. */
public record GunFeedbackPolicy(boolean enabled, boolean players, boolean monsters, boolean animals,
                                WeaponFeedbackRules.Mode mode, List<String> whitelist) {
    public GunFeedbackPolicy { whitelist=List.copyOf(whitelist); }
    public static GunFeedbackPolicy from(FeedbackSettings settings) {
        // Keep the join packet bounded. Oversized policies retain the original TaCZ audio.
        long bytes=settings.weaponWhitelist().stream().mapToLong(s->s.getBytes(java.nio.charset.StandardCharsets.UTF_8).length+5).sum();
        if(settings.weaponWhitelist().size()>256 || bytes>60000 || settings.weaponWhitelist().stream().anyMatch(s->s.length()>2048))
            return new GunFeedbackPolicy(false,false,false,false,WeaponFeedbackRules.Mode.ALL,List.of());
        return new GunFeedbackPolicy(settings.enabled(),settings.targetPlayers(),settings.targetMonsters(),settings.targetAnimals(),settings.weaponMode(),settings.weaponWhitelist());
    }
}
