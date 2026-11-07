import dev.flower.apex_sound_effects.core.*;
import java.util.*;
import java.nio.file.*;

public final class TaczSettingsCheck {
    static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception {
        var warnings=new ArrayList<String>();var p=new Properties();
        var defaults=TaczFeedbackSettings.parse(p,warnings::add);
        check(defaults.replaceHitSounds() && defaults.ammoKind("tacz:9mm")==AmmoKind.LIGHT,"defaults");
        check(defaults.ammoKind("unknown:ammo")==AmmoKind.GENERIC,"unmapped fallback");
        p.setProperty("replaceTaczHitFeedback","false");
        p.setProperty("ammo.light","test:ammo,Invalid:Id");p.setProperty("ammo.heavy","test:ammo");
        p.setProperty("ammo.shotgun","");p.setProperty("ammo.arrow","test:arrow");
        var parsed=TaczFeedbackSettings.parse(p,warnings::add);
        check(!parsed.replaceHitSounds() && parsed.ammoKind("test:ammo")==AmmoKind.LIGHT,"switch/first group");
        check(parsed.ammoKind("tacz:12g")==AmmoKind.GENERIC && parsed.ammoKind("test:arrow")==AmmoKind.ARROW,"empty/custom groups");
        check(warnings.size()==2,"invalid and duplicate warnings");
        Path dir=Files.createTempDirectory("apex-tacz-check");Path file=dir.resolve("settings.properties");
        try {
            Files.writeString(file,"schemaVersion=1\nreplaceTaczHitFeedback=false\nammo.light=test:ammo,Invalid:Id\ncustom.setting=retained\n");
            var store=FeedbackSettingsStore.open(file,warnings::add);store.setVolume(.37);store.save();
            var saved=Files.readString(file);
            check(saved.contains("ammo.light=test\\:ammo,Invalid\\:Id") && saved.contains("custom.setting=retained"),"save preserves invalid/unknown");
            check(!FeedbackSettingsStore.open(file,warnings::add).taczSettings().replaceHitSounds(),"reopen switch");
            Files.writeString(file,"schemaVersion=99\nreplaceTaczHitFeedback=false\n");
            var future=FeedbackSettingsStore.open(file,warnings::add);future.setVolume(.2);future.save();
            check(future.taczSettings().replaceHitSounds() && Files.readString(file).contains("schemaVersion=99"),"unknown schema preserved");
        }finally{Files.deleteIfExists(file);Files.delete(dir);}
        var allowed=GunFeedbackPolicy.from(FeedbackSettings.DEFAULT);
        check(allowed.enabled() && allowed.mode()==WeaponFeedbackRules.Mode.ALL,"default policy");
        var big=new FeedbackSettings(true,.5,40,80,120,160,true,true,true,WeaponFeedbackRules.Mode.WHITELIST,Collections.nCopies(257,"test:gun"),.37);
        check(!GunFeedbackPolicy.from(big).enabled(),"bounded join policy");
        var longItem=new FeedbackSettings(true,.5,40,80,120,160,true,true,true,WeaponFeedbackRules.Mode.WHITELIST,List.of("test:"+"a".repeat(2049)),1);
        check(!GunFeedbackPolicy.from(longItem).enabled(),"bounded policy entry");
        var longList=new FeedbackSettings(true,.5,40,80,120,160,true,true,true,WeaponFeedbackRules.Mode.WHITELIST,Collections.nCopies(64,"test:"+"a".repeat(1000)),1);
        check(!GunFeedbackPolicy.from(longList).enabled(),"bounded policy byte budget");
        System.out.println("PASS TaCZ config defaults, switch, mapping validation, empty/custom lists, save/reopen, future schema and policy bound");
    }
}
