package net.j40climb.florafauna_hammer.common.item.abilities.networking;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public final class HammerNetwork {
    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }

    public static void sendToTrackingAndSelf(ServerPlayer player, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, payload);
    }

    private HammerNetwork() {}
}
