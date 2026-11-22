package net.j40climb.florafauna_hammer.common.item.abilities.networking;

import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public final class HammerNetwork {
    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        if (player.connection != null && ServerPlayNetworking.canSend(player, payload.type())) ServerPlayNetworking.send(player, payload);
    }

    public static void sendToTrackingAndSelf(ServerPlayer player, CustomPacketPayload payload) {
        sendToPlayer(player, payload);
        for (var observer : PlayerLookup.tracking(player)) {
            if (observer != player) sendToPlayer(observer, payload);
        }
    }

    private HammerNetwork() {}
}
