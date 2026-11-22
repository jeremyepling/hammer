package net.j40climb.florafauna_hammer.common.item.abilities.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.EventHooks;

final class StairPlacement {
    static void placeStair(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState stair) {
        if (level.captureBlockSnapshots) {
            // The enclosing NeoForge placement owns the event and subsequent notifications.
            level.setBlock(pos, stair, Block.UPDATE_ALL);
            return;
        }
        var snapshot = BlockSnapshot.create(level.dimension(), level, pos, Block.UPDATE_ALL);
        var chunk = level.getChunkAt(pos);
        // The place event reads the stair from the world. Defer observers, clients and
        // shape updates until the event accepts it, including on paths that restore air.
        if (chunk.setBlockState(pos, stair, Block.UPDATE_SKIP_ALL_SIDEEFFECTS) == null) return;
        boolean accepted = false;
        try {
            accepted = !EventHooks.onBlockPlace(player, snapshot, Direction.UP);
        } finally {
            if (!accepted) chunk.setBlockState(pos, snapshot.getState(), Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
        }
        if (accepted && level.getBlockState(pos) == stair) {
            stair.onPlace(level, pos, snapshot.getState(), false);
            level.markAndNotifyBlock(pos, chunk, snapshot.getState(), stair, Block.UPDATE_ALL, Block.UPDATE_LIMIT);
        }
    }
}
