package net.j40climb.florafauna_hammer.common.item.abilities.networking;

import net.j40climb.florafauna_hammer.HammerMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;

/** One-shot sounds and particles. Keep the original cue ordinals for existing clients. */
public record HammerCorePayload(int playerId, InteractionHand hand, Cue cue) implements CustomPacketPayload {
    public enum Cue { SCAN, DESCENT, IMPACT, LIGHTNING, WINDUP, READY, RELEASE, STOP_THROW, STOP_SLAM, MODE, CHARGE }

    public static final Type<HammerCorePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(HammerMod.MOD_ID, "hammer_core"));
    private static final StreamCodec<FriendlyByteBuf, Cue> CUE_CODEC =
            StreamCodec.of((buf, cue) -> buf.writeEnum(cue), buf -> buf.readEnum(Cue.class));
    public static final StreamCodec<FriendlyByteBuf, HammerCorePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, HammerCorePayload::playerId, InteractionHand.STREAM_CODEC, HammerCorePayload::hand,
            CUE_CODEC, HammerCorePayload::cue, HammerCorePayload::new);

    public static void send(ServerPlayer player, InteractionHand hand, Cue cue) {
        HammerNetwork.sendToTrackingAndSelf(player, new HammerCorePayload(player.getId(), hand, cue));
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
