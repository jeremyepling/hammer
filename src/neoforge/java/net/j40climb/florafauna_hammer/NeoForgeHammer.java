package net.j40climb.florafauna_hammer;

import net.j40climb.florafauna_hammer.common.HammerCommands;
import net.j40climb.florafauna_hammer.common.item.abilities.networking.*;
import net.j40climb.florafauna_hammer.common.item.hammer.HammerStage;
import net.j40climb.florafauna_hammer.setup.ClientSetup;
import net.j40climb.florafauna_hammer.setup.HammerRegistry;
import net.j40climb.florafauna_hammer.test.NeoForgeHammerGameTests;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.Arrays;
import java.util.function.BiConsumer;

@Mod(HammerMod.MOD_ID)
public final class NeoForgeHammer {

    public NeoForgeHammer(IEventBus bus) {
        HammerRegistry.initialize();
        HammerRegistry.register(bus);
        NeoForge.EVENT_BUS.addListener((PlayerTickEvent.Post event) -> {
            if (event.getEntity() instanceof ServerPlayer player) ChargedThrowPayload.tick(player);
        });
        bus.addListener(NeoForgeHammer::networking);
        bus.addListener(NeoForgeHammer::creativeTab);
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> HammerCommands.register(event.getDispatcher()));
        if (FMLLoader.getCurrent().getDist().isClient()) ClientSetup.init(bus);
        var namespaces = System.getProperty("neoforge.enabledGameTestNamespaces", "").split(",");
        if (Arrays.asList(namespaces).contains(HammerMod.MOD_ID)) bus.addListener(NeoForgeHammerGameTests::registerTests);
    }

    private static void creativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            for (HammerStage stage : HammerStage.values()) event.accept(stage.createStack());
        } else if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(HammerRegistry.RESONANCE_CORE.get());
            event.accept(HammerRegistry.TEMPEST_CORE.get());
        }
    }

    private static void networking(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("2");
        HammerPackets.register(new HammerPackets.Registrar() {
            @Override
            public <T extends CustomPacketPayload> void clientbound(Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
                registrar.playToClient(type, codec);
            }

            @Override
            public <T extends CustomPacketPayload> void serverbound(String version, Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
                                                                     BiConsumer<T, ServerPlayer> handler) {
                registrar.versioned(version).playToServer(type, codec, (payload, context) -> handler.accept(payload, (ServerPlayer) context.player()));
            }
        });
    }
}
