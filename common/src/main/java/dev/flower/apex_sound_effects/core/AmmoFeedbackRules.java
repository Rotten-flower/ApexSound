package dev.flower.apex_sound_effects.core;

import java.util.*;

public final class AmmoFeedbackRules {
    public static final List<String> LIGHT=List.of("tacz:9mm","tacz:45acp","tacz:46x30","tacz:57x28","tacz:22wmr","tacz:357mag","tacz:762x25","tacz:556x45","tacz:545x39","tacz:58x42");
    public static final List<String> HEAVY=List.of("tacz:762x39","tacz:762x54","tacz:308","tacz:30_06","tacz:338","tacz:50bmg","tacz:68x51fury","tacz:792x57","tacz:45_70","tacz:500mag","tacz:50ae");
    public static final List<String> SHOTGUN=List.of("tacz:12g");
    public static final List<String> ARROW=List.of("minecraft:arrow","tacz:arrow","tacz:bolt");
    public static Map<String,AmmoKind> mapping(List<? extends String> light,List<? extends String> heavy,List<? extends String> shotgun,List<? extends String> arrow) {
        Map<String,AmmoKind> result=new HashMap<>();
        add(result,light,AmmoKind.LIGHT);add(result,heavy,AmmoKind.HEAVY);add(result,shotgun,AmmoKind.SHOTGUN);add(result,arrow,AmmoKind.ARROW);
        return Map.copyOf(result);
    }
    private static void add(Map<String,AmmoKind> result,List<? extends String> ids,AmmoKind kind) {
        for(String id:ids)result.putIfAbsent(id,kind);
    }
    public static AmmoKind classify(String ammoId,Map<String,AmmoKind> mapping) {
        return ammoId==null?AmmoKind.GENERIC:mapping.getOrDefault(ammoId,AmmoKind.GENERIC);
    }
    private AmmoFeedbackRules() {}
}
