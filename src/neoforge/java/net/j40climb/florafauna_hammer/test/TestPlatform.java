package net.j40climb.florafauna_hammer.test;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.UUID;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import net.j40climb.florafauna_hammer.common.item.abilities.RightClickActionHandler;
import net.j40climb.florafauna_hammer.common.item.projectile.ThrownItemEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import java.util.function.Consumer;
//? if >=26.3 {
/*import net.neoforged.neoforge.event.level.block.BreakBlockEvent;*/
//?}

final class TestPlatform {
    static ServerPlayer player(ServerLevel level, String name) {
        return new FakePlayer(level, new GameProfile(UUID.randomUUID(), name));
    }

    static Runnable vetoBreak(ServerPlayer player, Predicate<BlockPos> denied) {
        //? if >=26.3 {
        /*Consumer<BreakBlockEvent> veto = event -> {*/
        //?} else {
        Consumer<BlockEvent.BreakEvent> veto = event -> {
        //?}
            if (event.getPlayer() == player && denied.test(event.getPos())) event.setCanceled(true);
        };
        //? if >=26.3 {
        /*NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, BreakBlockEvent.class, veto);*/
        //?} else {
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, BlockEvent.BreakEvent.class, veto);
        //?}
        return () -> NeoForge.EVENT_BUS.unregister(veto);
    }

    static Runnable vetoPlacement(ServerPlayer player, BiPredicate<BlockPos, Block> denied) {
        Consumer<BlockEvent.EntityPlaceEvent> veto = e -> {
            if (e.getEntity() == player && denied.test(e.getPos(), e.getPlacedBlock().getBlock())) e.setCanceled(true);
        };
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, BlockEvent.EntityPlaceEvent.class, veto);
        return () -> NeoForge.EVENT_BUS.unregister(veto);
    }

    static void withRejectedThrow(ServerPlayer player, Runnable attempt) {
        Consumer<EntityJoinLevelEvent> veto = e -> {
            if (e.getEntity() instanceof ThrownItemEntity projectile && projectile.getOwner() == player) e.setCanceled(true);
        };
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, EntityJoinLevelEvent.class, veto);
        try {
            attempt.run();
        } finally {
            NeoForge.EVENT_BUS.unregister(veto);
        }
    }

    static boolean clickBlock(ServerPlayer player, InteractionHand hand, BlockHitResult hit) {
        var use = new PlayerInteractEvent.RightClickBlock(player, hand, hit.getBlockPos(), hit);
        RightClickActionHandler.onRightClickBlock(use);
        return use.isCanceled();
    }

    static boolean clickAir(ServerPlayer player, InteractionHand hand) {
        var use = new PlayerInteractEvent.RightClickItem(player, hand);
        RightClickActionHandler.onRightClickItem(use);
        return use.isCanceled();
    }

    static float destroySpeed(ServerPlayer player, BlockState state, BlockPos pos) {
        return player.getDestroySpeed(state, pos);
    }

    static void tick(ServerPlayer player) {
        NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(player));
    }

    @SuppressWarnings("removal")
    static ServerPlayer scanPlayer(GameTestHelper helper, ChannelOutboundHandlerAdapter observer) {
        var player = helper.makeMockServerPlayerInLevel();
        var connection = player.connection.getConnection();
        NetworkRegistry.configureMockConnection(connection);
        connection.channel().pipeline().addLast(observer);
        return player;
    }
}
