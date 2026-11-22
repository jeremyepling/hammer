package net.j40climb.florafauna_hammer.playtest;

import net.j40climb.florafauna_hammer.common.item.abilities.data.MiningShape;
import net.j40climb.florafauna_hammer.common.item.abilities.multiblock.MultiBlockPatterns;
import net.j40climb.florafauna_hammer.setup.ClientSetup;
import net.j40climb.florafauna_hammer.setup.HammerRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;

import java.util.List;
import net.minecraft.client.KeyMapping;

final class HammerSinglePlaytest {
    static void steps(Playtest t, BlockPos wall) {
        t.client("Double-tap C", mc -> {
            KeyMapping.click(ClientSetup.boundKey(ClientSetup.CYCLE_MINING_MODE_KEY.get()));
            KeyMapping.click(ClientSetup.boundKey(ClientSetup.CYCLE_MINING_MODE_KEY.get()));
        });
        t.awaitClient("Receive Single before striking", mc ->
                mc.player.getMainHandItem().get(HammerRegistry.MULTI_BLOCK_MINING.get()).shape() == MiningShape.SINGLE);
        t.client("Strike the center in Single mode", mc -> {
            Playtest.require(mc.gameMode.startDestroyBlock(wall, Direction.NORTH), "Single strike refused");
        });
        t.awaitServer("Verify Single broke only the center and stayed selected", p -> {
            if (!p.level().getBlockState(wall).isAir()) return false;
            Playtest.require(p.getMainHandItem().get(HammerRegistry.MULTI_BLOCK_MINING.get()).shape() == MiningShape.SINGLE,
                    "Single did not persist");
            for (BlockPos pos : BlockPos.betweenClosed(wall.offset(-1, -1, 0), wall.offset(1, 1, 0))) {
                Playtest.require(pos.equals(wall) || p.level().getBlockState(pos).is(Blocks.STONE), "Single mined " + pos);
            }
            return true;
        });
        t.server("Replace the center for the restored pattern", p -> p.level().setBlockAndUpdate(wall, Blocks.STONE.defaultBlockState()));
        t.awaitClient("Preview the single target after key release", mc -> mc.level.getBlockState(wall).is(Blocks.STONE)
                && MultiBlockPatterns.plan(mc.player, mc.player.getMainHandItem(), wall, Direction.NORTH).blocks().equals(List.of(wall)));
        t.client("Tap C to select the next shape", mc -> KeyMapping.click(ClientSetup.boundKey(ClientSetup.CYCLE_MINING_MODE_KEY.get())));
        t.awaitClient("Receive 3x3", mc -> mc.player.getMainHandItem().get(HammerRegistry.MULTI_BLOCK_MINING.get()).shape() == MiningShape.FLAT_3X3);
        t.client("Attack using 3x3", mc -> {
            Playtest.require(mc.gameMode.startDestroyBlock(wall, Direction.NORTH), "3×3 strike refused after cycling");
        });
        t.awaitServer("Restore the full nine-block pattern after cycling", p -> {
            for (BlockPos pos : BlockPos.betweenClosed(wall.offset(-1, -1, 0), wall.offset(1, 1, 0))) {
                if (!p.level().isEmptyBlock(pos)) return false;
            }
            return true;
        });
        t.server("Rebuild the wall for the outline and perimeter checks", p -> {
            for (BlockPos pos : BlockPos.betweenClosed(wall.offset(-1, -1, 0), wall.offset(1, 1, 0))) {
                p.level().setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
            }
        });
    }
}
