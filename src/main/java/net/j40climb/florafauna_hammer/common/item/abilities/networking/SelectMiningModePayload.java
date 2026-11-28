package net.j40climb.florafauna_hammer.common.item.abilities.networking;

import io.netty.buffer.ByteBuf;
import net.j40climb.florafauna_hammer.HammerMod;
import net.j40climb.florafauna_hammer.common.item.abilities.data.MiningShape;
import net.j40climb.florafauna_hammer.common.item.hammer.HammerStage;
import net.j40climb.florafauna_hammer.setup.HammerRegistry;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;

public record SelectMiningModePayload(int slot, MiningShape shape, boolean toggleFavorite) implements CustomPacketPayload {
    public static final Type<SelectMiningModePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(HammerMod.MOD_ID, "select_mining_mode"));
    public static final StreamCodec<ByteBuf, SelectMiningModePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SelectMiningModePayload::slot,
            MiningShape.STREAM_CODEC, SelectMiningModePayload::shape,
            ByteBufCodecs.BOOL, SelectMiningModePayload::toggleFavorite, SelectMiningModePayload::new);

    public static void onServerReceived(SelectMiningModePayload payload, ServerPlayer player) {
        if (player.getInventory().getSelectedSlot() != payload.slot) return;
        var stack = player.getMainHandItem();
        var current = stack.get(HammerRegistry.MULTI_BLOCK_MINING.get());
        if (current == null || !HammerStage.of(stack).allows(payload.shape)) return;
        if (payload.toggleFavorite) {
            stack.set(HammerRegistry.MULTI_BLOCK_MINING.get(), current.toggleFavorite(payload.shape));
        } else if (current.shape() != payload.shape) {
            var selected = current.withShape(payload.shape);
            stack.set(HammerRegistry.MULTI_BLOCK_MINING.get(), selected);
            player.sendOverlayMessage(selected.modeDescription());
            if (stack.is(HammerRegistry.HAMMER.get())) HammerCorePayload.send(player, InteractionHand.MAIN_HAND, HammerCorePayload.Cue.MODE);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
