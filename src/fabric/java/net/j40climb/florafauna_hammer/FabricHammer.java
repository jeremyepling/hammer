package net.j40climb.florafauna_hammer;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.j40climb.florafauna_hammer.common.HammerCommands;
import net.j40climb.florafauna_hammer.common.item.abilities.FabricHammerEvents;
import net.j40climb.florafauna_hammer.common.item.abilities.networking.HammerPackets;
import net.j40climb.florafauna_hammer.common.item.hammer.HammerStage;
import net.j40climb.florafauna_hammer.setup.HammerRegistry;
import net.j40climb.florafauna_hammer.test.FabricHammerGameTests;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTabs;

import java.util.function.BiConsumer;

public final class FabricHammer implements ModInitializer {
    @Override
    public void onInitialize() {
        HammerRegistry.initialize();
        FabricHammerEvents.register();
        CommandRegistrationCallback.EVENT.register((dispatcher, registries, environment) -> HammerCommands.register(dispatcher));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output -> {
            for (HammerStage stage : HammerStage.values()) output.accept(stage.createStack());
        });
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(output -> {
            output.accept(HammerRegistry.RESONANCE_CORE.get());
            output.accept(HammerRegistry.TEMPEST_CORE.get());
        });
        if (System.getProperty("fabric-api.gametest") != null) FabricHammerGameTests.initialize();
        HammerPackets.register(new HammerPackets.Registrar() {
            @Override
            public <T extends CustomPacketPayload> void clientbound(Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
                PayloadTypeRegistry.clientboundPlay().register(type, codec);
            }

            @Override
            public <T extends CustomPacketPayload> void serverbound(String version, Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
                                                                     BiConsumer<T, ServerPlayer> handler) {
                PayloadTypeRegistry.serverboundPlay().register(type, codec);
                ServerPlayNetworking.registerGlobalReceiver(type, (payload, ctx) -> handler.accept(payload, ctx.player()));
            }
        });
    }
}
