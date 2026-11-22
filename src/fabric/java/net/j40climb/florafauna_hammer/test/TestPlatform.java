package net.j40climb.florafauna_hammer.test;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.UUID;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.impl.networking.RegistrationPayload;
import net.j40climb.florafauna_hammer.HammerMod;
import net.j40climb.florafauna_hammer.common.item.abilities.GroundSlam;
import net.j40climb.florafauna_hammer.common.item.abilities.networking.SeismicScanPayload;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

final class TestPlatform {
    private static final Map<Player, Predicate<BlockPos>> protectedBlocks = new IdentityHashMap<>();
    private static final Map<Player, BiPredicate<BlockPos, Block>> protectedPlacements = new IdentityHashMap<>();

    static {
        PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, entity) -> {
            var denied = protectedBlocks.get(player);
            return denied == null || !denied.test(pos);
        });
        var phase = Identifier.fromNamespaceAndPath(HammerMod.MOD_ID, "test_protection");
        UseBlockCallback.EVENT.addPhaseOrdering(phase, Event.DEFAULT_PHASE);
        UseBlockCallback.EVENT.register(phase, (player, level, hand, hit) -> {
            var denied = protectedPlacements.get(player);
            if (denied == null) return InteractionResult.PASS;
            // Treads target the empty cell. Torches target the solid wall face.
            boolean stair = hit.getDirection() == Direction.UP;
            var pos = stair ? hit.getBlockPos() : hit.getBlockPos().relative(hit.getDirection());
            return denied.test(pos, stair ? Blocks.COBBLESTONE_STAIRS : Blocks.WALL_TORCH)
                    ? InteractionResult.FAIL : InteractionResult.PASS;
        });
    }

    static ServerPlayer player(ServerLevel level, String name) {
        return FakePlayer.get(level, new GameProfile(UUID.randomUUID(), name));
    }

    static Runnable vetoBreak(ServerPlayer player, Predicate<BlockPos> denied) {
        protectedBlocks.put(player, denied);
        return () -> protectedBlocks.remove(player);
    }

    static Runnable vetoPlacement(ServerPlayer player, BiPredicate<BlockPos, Block> denied) {
        protectedPlacements.put(player, denied);
        return () -> protectedPlacements.remove(player);
    }

    static boolean clickBlock(ServerPlayer player, InteractionHand hand, BlockHitResult hit) {
        return UseBlockCallback.EVENT.invoker().interact(player, player.level(), hand, hit).consumesAction();
    }

    static boolean clickAir(ServerPlayer player, InteractionHand hand) {
        return UseItemCallback.EVENT.invoker().interact(player, player.level(), hand).consumesAction();
    }

    static float destroySpeed(ServerPlayer player, BlockState state, BlockPos pos) {
        return player.getDestroySpeed(state);
    }

    static void tick(ServerPlayer player) {
        GroundSlam.tick(player);
    }

    static double fallDistance(ServerPlayer player, double distance) {
        return GroundSlam.fallDistance(player, distance);
    }

    static ServerPlayer scanPlayer(GameTestHelper helper, ChannelOutboundHandlerAdapter observer) {
        var level = helper.getLevel();
        var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "scan-test"), false);
        var player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation());
        var connection = new Connection(PacketFlow.SERVERBOUND);
        var channel = new EmbeddedChannel(connection);
        level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        // Simulate the receiving client's REGISTER packet on the GameTest's in-memory connection.
        var register = new RegistrationPayload(RegistrationPayload.REGISTER, List.of(SeismicScanPayload.TYPE.id()));
        player.connection.handleCustomPayload(new ServerboundCustomPayloadPacket(register));
        channel.pipeline().addLast(observer);
        return player;
    }
}
