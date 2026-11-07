package dev.flower.apex_sound_effects;

import dev.flower.apex_sound_effects.core.GunFeedbackPolicy;
import dev.flower.apex_sound_effects.core.WeaponFeedbackRules;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;

/** 入服时同步武器规则，保留本地音量。 / Syncs weapon rules on join while preserving local volume. */
public final class GunPolicyNetwork {
    public static final ResourceLocation CHANNEL=ResourceLocation.tryParse("apex_sound_effects:gun_policy");
    public static void registerServer() {
        ServerPlayConnectionEvents.JOIN.register((handler,sender,server)->{
            if(!ServerPlayNetworking.canSend(handler.player,CHANNEL))return;
            var buffer=new FriendlyByteBuf(Unpooled.buffer());
            var settings=FeedbackConfig.settings();
            var policy=GunFeedbackPolicy.from(settings);
            if(settings.enabled() && !policy.enabled())org.slf4j.LoggerFactory.getLogger(ApexSound.MOD_ID)
                    .warn("weapons.whitelist exceeds the client gun audio policy budget (256 entries, 2048 characters each, 60000 bytes); retaining original TaCZ audio for this connection");
            write(buffer,policy);
            ServerPlayNetworking.send(handler.player,CHANNEL,buffer);
        });
    }
    public static void write(FriendlyByteBuf buffer,GunFeedbackPolicy policy) {
        buffer.writeBoolean(policy.enabled());buffer.writeBoolean(policy.players());
        buffer.writeBoolean(policy.monsters());buffer.writeBoolean(policy.animals());buffer.writeEnum(policy.mode());
        buffer.writeVarInt(policy.whitelist().size());
        for(String entry:policy.whitelist())buffer.writeUtf(entry,2048);
    }
    public static GunFeedbackPolicy read(FriendlyByteBuf buffer) {
        boolean enabled=buffer.readBoolean(),players=buffer.readBoolean(),monsters=buffer.readBoolean(),animals=buffer.readBoolean();
        var mode=buffer.readEnum(WeaponFeedbackRules.Mode.class);
        int count=buffer.readVarInt();if(count<0 || count>256)throw new IllegalArgumentException("Invalid gun policy size");
        var list=new ArrayList<String>(count);
        for(int i=0;i<count;i++)list.add(buffer.readUtf(2048));
        return new GunFeedbackPolicy(enabled,players,monsters,animals,mode,list);
    }
}
