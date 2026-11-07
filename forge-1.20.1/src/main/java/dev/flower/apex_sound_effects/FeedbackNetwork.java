package dev.flower.apex_sound_effects;

import dev.flower.apex_sound_effects.core.ArmorFeedbackRules.Kind;
import dev.flower.apex_sound_effects.core.AmmoKind;
import dev.flower.apex_sound_effects.client.ClientFeedback;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
import java.util.Optional;

/** 将命中反馈和规则发送至客户端。 / Sends hit feedback and rules to clients. */
public final class FeedbackNetwork {
    private static final String VERSION="1";
    public static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(new ResourceLocation(ApexSound.MOD_ID,"feedback"),()->VERSION,VERSION::equals,VERSION::equals);
    public record Hit(int target, Kind kind, float damage, int color, boolean headshot, double x,double y,double z,AmmoKind ammo,boolean terminal) {
        public static void encode(Hit h,FriendlyByteBuf b) {
            b.writeVarInt(h.target); b.writeEnum(h.kind); b.writeFloat(h.damage); b.writeInt(h.color); b.writeBoolean(h.headshot);
            b.writeDouble(h.x);b.writeDouble(h.y);b.writeDouble(h.z);
            b.writeEnum(h.ammo);b.writeBoolean(h.terminal);
        }
        public static Hit decode(FriendlyByteBuf b) {
            return new Hit(b.readVarInt(),b.readEnum(Kind.class),b.readFloat(),b.readInt(),b.readBoolean(),b.readDouble(),b.readDouble(),b.readDouble(),b.readEnum(AmmoKind.class),b.readBoolean());
        }
    }
    public static void register() {
        CHANNEL.registerMessage(0,Hit.class,Hit::encode,Hit::decode,(hit,ctx)->{
            ctx.get().enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->ClientFeedback.accept(hit)));
            ctx.get().setPacketHandled(true);
        },Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }
    public static void send(ServerPlayer player,Hit hit) {
        if(player.connection!=null && player.connection.connection!=null && player.connection.connection.isConnected()
                && CHANNEL.isRemotePresent(player.connection.connection))
            CHANNEL.send(PacketDistributor.PLAYER.with(()->player),hit);
    }
}
