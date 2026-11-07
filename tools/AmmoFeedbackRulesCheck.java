import dev.flower.apex_sound_effects.core.*;
import java.util.*;

public final class AmmoFeedbackRulesCheck {
    public static void main(String[] args) {
        var map=AmmoFeedbackRules.mapping(AmmoFeedbackRules.LIGHT,AmmoFeedbackRules.HEAVY,AmmoFeedbackRules.SHOTGUN,AmmoFeedbackRules.ARROW);
        check(AmmoFeedbackRules.classify("tacz:556x45",map)==AmmoKind.LIGHT,"5.56 light");
        check(AmmoFeedbackRules.classify("tacz:762x39",map)==AmmoKind.HEAVY,"7.62 heavy");
        check(AmmoFeedbackRules.classify("tacz:12g",map)==AmmoKind.SHOTGUN,"12 gauge shotgun");
        check(AmmoFeedbackRules.classify("minecraft:arrow",map)==AmmoKind.ARROW,"arrow");
        check(AmmoFeedbackRules.classify("pack:556x45",map)==AmmoKind.GENERIC,"unknown namespace is not guessed");
        check(AmmoFeedbackRules.classify(null,map)==AmmoKind.GENERIC,"missing ammo");
        var custom=AmmoFeedbackRules.mapping(List.of("pack:round"),List.of("pack:round","pack:heavy"),List.of(),List.of("pack:bolt"));
        check(AmmoFeedbackRules.classify("pack:round",custom)==AmmoKind.LIGHT,"duplicate first family wins");
        check(AmmoFeedbackRules.classify("pack:heavy",custom)==AmmoKind.HEAVY,"pack override");
        check(AmmoFeedbackRules.classify("pack:bolt",custom)==AmmoKind.ARROW,"custom arrow");
        check(AmmoFeedbackRules.classify("tacz:12g",custom)==AmmoKind.GENERIC,"removed mapping falls back");
        System.out.println("PASS: 10 exact ammo mapping and fallback checks");
    }
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
