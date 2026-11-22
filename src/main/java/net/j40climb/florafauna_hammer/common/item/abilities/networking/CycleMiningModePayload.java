package net.j40climb.florafauna_hammer.common.item.abilities.networking;

import io.netty.buffer.ByteBuf;
import net.j40climb.florafauna_hammer.HammerMod;
import net.j40climb.florafauna_hammer.common.item.abilities.data.MiningModeData;
import net.j40climb.florafauna_hammer.common.item.hammer.HammerStage;
import net.j40climb.florafauna_hammer.setup.HammerRegistry;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;

/**
 * Payload sent from client to server when the cycle mining mode key is pressed.
 * Cycles the mining mode on the held item if it has the MULTI_BLOCK_MINING component.
 */
public enum CycleMiningModePayload implements CustomPacketPayload {
    INSTANCE;

    public static final Type<CycleMiningModePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(HammerMod.CONTENT_NAMESPACE, "cycle_mining_mode"));

    public static final StreamCodec<ByteBuf, CycleMiningModePayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    public static void onServerReceived(final CycleMiningModePayload data, final ServerPlayer player) {
        cycleMiningMode(player, player.getMainHandItem());
    }

    /**
     * Cycles the mining mode on the interacting stack.
     * Can be called directly from items or via network payload.
     */
    public static void cycleMiningMode(ServerPlayer player, ItemStack stack) {

        MiningModeData previous = stack.get(HammerRegistry.MULTI_BLOCK_MINING.get());
        if (previous == null) return;
        MiningModeData selected = previous.getNextMode(HammerStage.of(stack));
        stack.set(HammerRegistry.MULTI_BLOCK_MINING.get(), selected);
        player.sendOverlayMessage(selected.modeDescription());
        if (stack.is(HammerRegistry.HAMMER.get()) && selected.shape() != previous.shape()) {
            var hand = stack == player.getMainHandItem() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            HammerCorePayload.send(player, hand, HammerCorePayload.Cue.MODE);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
