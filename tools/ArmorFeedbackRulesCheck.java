import dev.flower.apex_sound_effects.core.ArmorFeedbackRules;
import static dev.flower.apex_sound_effects.core.ArmorFeedbackRules.Kind.*;
public final class ArmorFeedbackRulesCheck {
    static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
    public static void main(String[] args) {
        check(ArmorFeedbackRules.classify(20,18,20,.5)==ARMOR,"shield hit");
        check(ArmorFeedbackRules.classify(11,10,20,.5)==BREAK,"exact threshold");
        check(ArmorFeedbackRules.classify(11,0,20,.5)==BREAK,"lethal crossing");
        check(ArmorFeedbackRules.classify(10,9,20,.5)==FLESH,"already broken");
        check(ArmorFeedbackRules.classify(9,12,20,.5)==NONE,"healing does not hit");
        check(ArmorFeedbackRules.classify(12,9,20,.5)==BREAK,"healing re-arms");
        check(ArmorFeedbackRules.classify(200,180,200,.5)==ARMOR,"custom max health");
        check(ArmorFeedbackRules.classify(20,20,20,.5)==NONE,"canceled damage");
        check(ArmorFeedbackRules.classify(20,19,0,.5)==NONE,"invalid max");
        check(ArmorFeedbackRules.classify(Float.NaN,10,20,.5)==NONE,"nonfinite");
        check(ArmorFeedbackRules.merge(BREAK,FLESH)==BREAK,"split projectile preserves break");
        check(ArmorFeedbackRules.formatDamage(2).equals("2"),"actual HP without scaling");
        var w=ArmorFeedbackRules.ShieldColor.WHITE;var b=ArmorFeedbackRules.ShieldColor.BLUE;
        var p=ArmorFeedbackRules.ShieldColor.PURPLE;var g=ArmorFeedbackRules.ShieldColor.GOLD;var r=ArmorFeedbackRules.ShieldColor.RED;
        check(ArmorFeedbackRules.colorFor(40,40,80,120,160)==w,"white upper bound inclusive");
        check(ArmorFeedbackRules.colorFor(40.1f,40,80,120,160)==b,"blue lower bound exclusive");
        check(ArmorFeedbackRules.colorFor(80,40,80,120,160)==b,"blue upper bound");
        check(ArmorFeedbackRules.colorFor(80.1f,40,80,120,160)==p,"purple lower bound");
        check(ArmorFeedbackRules.colorFor(120,40,80,120,160)==p,"purple upper bound");
        check(ArmorFeedbackRules.colorFor(120.1f,40,80,120,160)==g,"gold lower bound");
        check(ArmorFeedbackRules.colorFor(160,40,80,120,160)==g,"gold upper bound");
        check(ArmorFeedbackRules.colorFor(160.1f,40,80,120,160)==r,"red lower bound");
        check(ArmorFeedbackRules.colorFor(200,40,80,120,160)==r,"dragon benchmark");
        check(ArmorFeedbackRules.colorFor(40,10,20,30,50)==g,"custom bands");
        check(!ArmorFeedbackRules.validBands(20,20,80,160),"duplicate boundaries rejected");
        check(!ArmorFeedbackRules.validBands(20,40,80,Double.NaN),"nonfinite boundary rejected");
        check(ArmorFeedbackRules.colorFor(80,50,20,80,160)==b,"invalid bands use defaults");
        check(ArmorFeedbackRules.formatDamage(.2f).equals("0"),"fractional damage rounds down");
        check(ArmorFeedbackRules.formatDamage(5.856f).equals("6"),"actual damage rounds up");
        check(ArmorFeedbackRules.formatDamage(.5f).equals("1"),"half rounds up");
        check(ArmorFeedbackRules.formatDamage(Float.NaN).equals("0"),"invalid damage text");
        System.out.println("PASS: 29 shield boundary, health-color and actual HP display checks");
    }
}
