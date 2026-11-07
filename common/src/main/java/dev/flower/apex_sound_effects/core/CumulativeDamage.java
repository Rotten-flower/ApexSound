package dev.flower.apex_sound_effects.core;

import java.util.LinkedHashMap;
import java.util.Map;

/** 短时累计本地伤害显示，不修改原伤害事件。 / Short lived, local damage totals; never changes the individual damage event. */
public final class CumulativeDamage {
    public static final long WINDOW_NANOS=1_000_000_000L;
    private static final int MAX_TARGETS=64;
    private record Entry(double total,long lastHit) {}
    private final Map<Integer,Entry> targets=new LinkedHashMap<>();

    public double add(int target,float damage,long now,boolean terminal) {
        if(!Float.isFinite(damage) || damage<=0)return 0;
        targets.entrySet().removeIf(e->now-e.getValue().lastHit()>=WINDOW_NANOS);
        Entry previous=targets.remove(target);
        double total=(previous==null?0:previous.total())+damage;
        if(!terminal) {
            if(targets.size()>=MAX_TARGETS)targets.remove(targets.keySet().iterator().next());
            targets.put(target,new Entry(total,now));
        }
        return total;
    }
    public static String format(double total) {
        return Double.isFinite(total) && total>0?Long.toString(Math.round(total)):"0";
    }
    public void clear(){targets.clear();}
}
