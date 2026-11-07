package dev.flower.apex_sound_effects.core;

import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/** 独立于游戏注册表的不可变规则，使用时再解析标签。 / Immutable rules independent of game registries. Tags are resolved at the point of use. */
public final class WeaponFeedbackRules {
    public enum Mode { ALL, TACZ_ONLY, WHITELIST }
    private static final Pattern ID=Pattern.compile("[a-z0-9_.-]+:[a-z0-9/._-]+");
    private final Set<String> ids;
    private final List<String> tags;
    public WeaponFeedbackRules(Collection<? extends String> entries,java.util.function.Consumer<String> invalid) {
        var parsedIds=new HashSet<String>();var parsedTags=new LinkedHashSet<String>();
        for(String entry:entries) {
            String value=entry.trim();boolean tag=value.startsWith("#");
            String id=tag?value.substring(1):value;
            if(!ID.matcher(id).matches()){invalid.accept(entry);continue;}
            if(tag)parsedTags.add(id);else parsedIds.add(id);
        }
        ids=Set.copyOf(parsedIds);tags=List.copyOf(parsedTags);
    }
    public static boolean isValidId(String value) { return value != null && ID.matcher(value).matches(); }
    public boolean allows(Mode mode,String itemId,String gunId,Predicate<String> hasTag) {
        if(mode==Mode.ALL)return true;
        if(mode==Mode.TACZ_ONLY)return gunId!=null;
        if((itemId!=null && ids.contains(itemId)) || (gunId!=null && ids.contains(gunId)))return true;
        for(String tag:tags)if(hasTag.test(tag))return true;
        return false;
    }
}
