package net.j40climb.florafauna_hammer.common.item.abilities.networking;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.BiConsumer;

public final class HammerPackets {
    public interface Registrar {
        <T extends CustomPacketPayload> void clientbound(Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec);
        <T extends CustomPacketPayload> void serverbound(String version, Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
                                                         BiConsumer<T, ServerPlayer> handler);
    }

    public static void register(Registrar registrar) {
        registrar.clientbound(HammerCorePayload.TYPE, HammerCorePayload.STREAM_CODEC);
        registrar.clientbound(SeismicScanPayload.TYPE, SeismicScanPayload.STREAM_CODEC);
        registrar.serverbound("2", SpawnLightningPayload.TYPE, SpawnLightningPayload.STREAM_CODEC, SpawnLightningPayload::onServerReceived);
        registrar.serverbound("2", TeleportToSurfacePayload.TYPE, TeleportToSurfacePayload.STREAM_CODEC, TeleportToSurfacePayload::onServerReceived);
        registrar.serverbound("3", UpdateToolConfigPayload.TYPE, UpdateToolConfigPayload.STREAM_CODEC, UpdateToolConfigPayload::onServerReceived);
        registrar.serverbound("2", ChargedThrowPayload.TYPE, ChargedThrowPayload.STREAM_CODEC, ChargedThrowPayload::handle);
        registrar.serverbound("2", ThrowItemPayload.TYPE, ThrowItemPayload.STREAM_CODEC, ThrowItemPayload::onServerReceived);
        registrar.serverbound("2", CycleMiningModePayload.TYPE, CycleMiningModePayload.STREAM_CODEC, CycleMiningModePayload::onServerReceived);
        registrar.serverbound("2", SelectMiningModePayload.TYPE, SelectMiningModePayload.STREAM_CODEC, SelectMiningModePayload::onServerReceived);
        registrar.serverbound("2", SelectHammerActionPayload.TYPE, SelectHammerActionPayload.STREAM_CODEC, SelectHammerActionPayload::onServerReceived);
    }

    private HammerPackets() {}
}
