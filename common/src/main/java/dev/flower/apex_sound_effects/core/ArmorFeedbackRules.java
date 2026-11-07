package dev.flower.apex_sound_effects.core;

/** 按血量判定反馈，不保存额外护盾状态或改变伤害。 / Health alone determines feedback. No extra shield state is saved or damage changed. */
public final class ArmorFeedbackRules {
    public enum Kind { NONE, ARMOR, BREAK, FLESH }
    private ArmorFeedbackRules() {}
    public static Kind classify(float before, float after, float max, double fleshRatio) {
        if (!Float.isFinite(before) || !Float.isFinite(after) || !Float.isFinite(max)
                || max <= 0 || before <= 0 || after >= before || !Double.isFinite(fleshRatio)
                || fleshRatio <= 0 || fleshRatio >= 1) return Kind.NONE;
        double threshold = max * fleshRatio;
        if (before <= threshold + 0.00001) return Kind.FLESH;
        return after <= threshold + 0.00001 ? Kind.BREAK : Kind.ARMOR;
    }
    public static String formatDamage(float loss) {
        if(!Float.isFinite(loss) || loss<=0)return "0";
        return Long.toString(Math.round((double)loss));
    }
    public static Kind merge(Kind a, Kind b) {
        if (a == Kind.BREAK || b == Kind.BREAK) return Kind.BREAK;
        if (a == Kind.ARMOR || b == Kind.ARMOR) return Kind.ARMOR;
        return b == Kind.NONE ? a : b;
    }
    public static boolean validBands(double white,double blue,double purple,double gold) {
        return Double.isFinite(white) && Double.isFinite(blue) && Double.isFinite(purple)
                && Double.isFinite(gold) && white>0 && white<blue && blue<purple && purple<gold;
    }
    /** Bands use maximum health, so losing health never downgrades the shield color. */
    public static ShieldColor colorFor(float maximum,double white,double blue,double purple,double gold) {
        if(!validBands(white,blue,purple,gold)){white=40;blue=80;purple=120;gold=160;}
        if(!Float.isFinite(maximum) || maximum<=white)return ShieldColor.WHITE;
        if(maximum<=blue)return ShieldColor.BLUE;
        if(maximum<=purple)return ShieldColor.PURPLE;
        if(maximum<=gold)return ShieldColor.GOLD;
        return ShieldColor.RED;
    }
    public enum ShieldColor {
        WHITE(0xE8E8E8), BLUE(0x4AAFFF), PURPLE(0xB370FF), GOLD(0xFFD35A), RED(0xFF4B4B);
        public final int rgb;
        ShieldColor(int rgb) { this.rgb=rgb; }
    }
}
