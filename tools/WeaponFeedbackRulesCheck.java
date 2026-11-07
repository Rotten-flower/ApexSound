import dev.flower.apex_sound_effects.core.WeaponFeedbackRules;
import static dev.flower.apex_sound_effects.core.WeaponFeedbackRules.Mode.*;
import java.util.*;
public final class WeaponFeedbackRulesCheck {
    static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
    public static void main(String[] args) {
        var invalid=new ArrayList<String>();
        var r=new WeaponFeedbackRules(List.of("minecraft:diamond_sword","tacz:m4a1","#forge:tools/bows","bad entry","#"," minecraft:bow "),invalid::add);
        check(invalid.size()==2,"bad entries individually reported");
        check(r.allows(ALL,"minecraft:air",null,t->false),"default includes fists");
        check(r.allows(ALL,null,null,t->false),"default includes unclassified sources");
        check(r.allows(WHITELIST,"minecraft:diamond_sword",null,t->false),"exact item ID");
        check(!r.allows(WHITELIST,"minecraft:iron_sword",null,t->false),"other item excluded");
        check(r.allows(WHITELIST,"tacz:modern_kinetic_gun","tacz:m4a1",t->false),"gun ID independent of base item");
        check(!r.allows(WHITELIST,"tacz:modern_kinetic_gun","tacz:ak47",t->false),"different gun excluded");
        check(r.allows(WHITELIST,"other:bow",null,t->t.equals("forge:tools/bows")),"modded item through tag");
        check(!r.allows(WHITELIST,"other:bow",null,t->false),"removed tag membership resolved dynamically");
        check(r.allows(WHITELIST,"minecraft:bow",null,t->false),"trim entry");
        check(!r.allows(TACZ_ONLY,"minecraft:bow",null,t->true),"TaCZ mode rejects vanilla arrows");
        check(r.allows(TACZ_ONLY,"tacz:modern_kinetic_gun","pack:gun",t->false),"TaCZ mode includes gun packs");
        check(!new WeaponFeedbackRules(List.of(),invalid::add).allows(WHITELIST,"minecraft:bow",null,t->true),"empty whitelist matches nothing");
        System.out.println("PASS: 13 weapon ID, gun ID, tag, invalid-entry and mode checks");
    }
}
