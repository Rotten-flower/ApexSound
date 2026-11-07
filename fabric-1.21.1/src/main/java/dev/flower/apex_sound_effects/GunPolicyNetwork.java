package dev.flower.apex_sound_effects;

import dev.flower.apex_sound_effects.core.GunFeedbackPolicy;
import dev.flower.apex_sound_effects.core.WeaponFeedbackRules;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.ArrayList;

/** 入服时同步武器规则，保留本地音量。 / Syncs weapon rules on join while preserving local volume. */
public final class GunPolicyNetwork {
    public record PolicyPacket(GunFeedbackPolicy policy) implements CustomPacketPayload {
        public static final Type<PolicyPacket> TYPE=new Type<>(ResourceLocation.parse("apex_sound_effects:gun_policy"));
        public static final StreamCodec<RegistryFriendlyByteBuf,PolicyPacket> CODEC=StreamCodec.of(
                (buffer,packet)->write(buffer,packet.policy()),buffer->new PolicyPacket(read(buffer)));
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public static void registerServer() {
        PayloadTypeRegistry.playS2C().register(PolicyPacket.TYPE,PolicyPacket.CODEC);
        ServerPlayConnectionEvents.JOIN.register((handler,sender,server)->{
            if(!ServerPlayNetworking.canSend(handler.player,PolicyPacket.TYPE))return;
            var settings=FeedbackConfig.settings();
            var policy=GunFeedbackPolicy.from(settings);
            if(settings.enabled() && !policy.enabled())org.slf4j.LoggerFactory.getLogger(ApexSound.MOD_ID)
                    .warn("weapons.whitelist exceeds the client gun audio policy budget (256 entries, 2048 characters each, 60000 bytes); retaining original TaCZ audio for this connection");
            ServerPlayNetworking.send(handler.player,new PolicyPacket(policy));
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
