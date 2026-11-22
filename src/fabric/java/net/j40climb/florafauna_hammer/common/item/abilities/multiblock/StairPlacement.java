package net.j40climb.florafauna_hammer.common.item.abilities.multiblock;

import net.j40climb.florafauna_hammer.common.item.abilities.FabricHammerEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

final class StairPlacement {
    static void placeStair(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState stair) {
        var hit = new BlockHitResult(Vec3.atBottomCenterOf(pos), Direction.UP, pos, false);
        if (!FabricHammerEvents.allowsBlockUse(player, InteractionHand.MAIN_HAND, hit)) return;
        var stack = new ItemStack(stair.getBlock());
        // The landing angle follows the original mining face, including when looking down.
        var context = new BlockPlaceContext(player, InteractionHand.MAIN_HAND, stack, hit) {
            @Override public Direction getHorizontalDirection() { return stair.getValue(StairBlock.FACING); }
        };
        if (context.getClickedPos().equals(pos)) ((BlockItem) stack.getItem()).place(context);
    }
}
