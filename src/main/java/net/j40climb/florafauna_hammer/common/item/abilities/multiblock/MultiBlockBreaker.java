package net.j40climb.florafauna_hammer.common.item.abilities.multiblock;

import net.j40climb.florafauna_hammer.common.item.abilities.data.MiningModeData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;

import java.util.HashSet;
import java.util.Set;

/** Completes an accepted center break. Every neighbor goes through the normal player mining pipeline. */
public final class MultiBlockBreaker {
    private MultiBlockBreaker() {}

    public static void finish(ServerPlayer player, ItemStack tool, MiningModeData mode,
                              BlockPos center, MultiBlockPatterns.Plan plan) {
        ServerLevel level = player.level();
        Set<BlockPos> mined = new HashSet<>();
        mined.add(center);
        for (BlockPos pos : plan.blocks()) {
            if (player.getMainHandItem() != tool || tool.isEmpty()) break;
            if (pos.equals(center) || !MultiBlockPatterns.isMineable(level, player, tool, mode, pos)) continue;
            var before = level.getBlockState(pos);
            if (player.gameMode.destroyBlock(pos) && !level.getBlockState(pos).is(before.getBlock())) mined.add(pos);
        }
        for (BlockPos pos : plan.stairs()) {
            if (plan.blocks().contains(pos) && !mined.contains(pos)) continue;
            if (!MultiBlockPatterns.canInteract(level, player, pos) || !level.isEmptyBlock(pos)) continue;
            BlockState stair = plan.stairState();
            if (level.isUnobstructed(stair, pos, CollisionContext.placementContext(player))) {
                StairPlacement.placeStair(level, player, pos, stair);
            }
        }
        if (plan.passage() != null) TunnelLighting.afterMining(player, tool, plan.passage(), mined);
    }
}
