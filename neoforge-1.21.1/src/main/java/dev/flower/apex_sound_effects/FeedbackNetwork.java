package dev.flower.apex_sound_effects;

import dev.flower.apex_sound_effects.core.ArmorFeedbackRules.Kind;
import dev.flower.apex_sound_effects.core.AmmoKind;
import dev.flower.apex_sound_effects.client.ClientFeedback;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** 将命中反馈和规则发送至客户端。 / Sends hit feedback and rules to clients. */
public final class FeedbackNetwork {
    public record Hit(int target, Kind kind, float damage, int color, boolean headshot,
                      double x, double y, double z, AmmoKind ammo, boolean terminal)
            implements CustomPacketPayload {
        public static final Type<Hit> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ApexSound.MOD_ID, "feedback"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Hit> STREAM_CODEC = new StreamCodec<>() {
            @Override public Hit decode(RegistryFriendlyByteBuf b) {
                return new Hit(b.readVarInt(), b.readEnum(Kind.class), b.readFloat(), b.readInt(), b.readBoolean(),
                        b.readDouble(), b.readDouble(), b.readDouble(), b.readEnum(AmmoKind.class), b.readBoolean());
            }
            @Override public void encode(RegistryFriendlyByteBuf b, Hit h) {
                b.writeVarInt(h.target); b.writeEnum(h.kind); b.writeFloat(h.damage); b.writeInt(h.color); b.writeBoolean(h.headshot);
                b.writeDouble(h.x); b.writeDouble(h.y); b.writeDouble(h.z); b.writeEnum(h.ammo); b.writeBoolean(h.terminal);
            }
        };
        @Override public Type<Hit> type() { return TYPE; }
    }

    @EventBusSubscriber(modid = ApexSound.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent
        public static void register(RegisterPayloadHandlersEvent event) {
            event.registrar("1").playToClient(Hit.TYPE, Hit.STREAM_CODEC,
                    (hit, context) -> context.enqueueWork(() -> ClientFeedback.accept(hit)));
        }
    }

    public static void send(ServerPlayer player, Hit hit) {
        PacketDistributor.sendToPlayer(player, hit);
    }

    private FeedbackNetwork() {}
}
