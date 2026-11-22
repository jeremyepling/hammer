package net.j40climb.florafauna_hammer.common.item.abilities.networking;

import io.netty.buffer.ByteBuf;
import net.j40climb.florafauna_hammer.HammerMod;
import net.j40climb.florafauna_hammer.common.item.abilities.data.HammerAction;
import net.j40climb.florafauna_hammer.setup.HammerRegistry;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public record SelectHammerActionPayload(int slot, HammerAction action) implements CustomPacketPayload {
    public static final Type<SelectHammerActionPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(HammerMod.CONTENT_NAMESPACE, "select_hammer_action"));
    public static final StreamCodec<ByteBuf, SelectHammerActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SelectHammerActionPayload::slot,
            HammerAction.STREAM_CODEC, SelectHammerActionPayload::action, SelectHammerActionPayload::new);

    public static void onServerReceived(SelectHammerActionPayload payload, ServerPlayer player) {
        var stack = player.getMainHandItem();
        if (player.getInventory().getSelectedSlot() == payload.slot && stack.has(HammerRegistry.HAMMER_ACTION.get())
                && payload.action.availableOn(stack)) {
            stack.set(HammerRegistry.HAMMER_ACTION.get(), payload.action);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
