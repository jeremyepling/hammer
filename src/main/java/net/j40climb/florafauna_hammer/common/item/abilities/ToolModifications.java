package net.j40climb.florafauna_hammer.common.item.abilities;

import net.j40climb.florafauna_hammer.common.item.abilities.data.MultiToolAbilityData;
import net.j40climb.florafauna_hammer.common.item.abilities.multiblock.MultiBlockPatterns;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Applies the selected tool actions to the Hammer's current mining pattern. */
final class ToolModifications {
    private ToolModifications() {}

    static boolean apply(Player player, InteractionHand hand, BlockPos center, BlockHitResult hit,
                         MultiToolAbilityData actions, boolean requireTarget) {
        //? if <26.3 {
        if (!(player instanceof ServerPlayer)) return false;
        //?}
        //? if neoforge && <26.3 {
        // The NeoForge hook can simulate the original click before planning. Real edits run in pattern order.
        if (requireTarget && !BlockToolActions.canModifyTarget(new UseOnContext(player, hand, hit), actions)) return false;
        boolean centerFirst = false;
        //?} else {
        /*boolean centerFirst = requireTarget;
        *///?}
        var positions = MultiBlockPatterns.plan(player, player.getItemInHand(hand), center, hit.getDirection()).blocks();
        boolean modified = false;
        if (centerFirst) {
            if (!positions.contains(center) || !modifyAt(player, hand, center, hit, actions)) return false;
            modified = true;
        }
        for (BlockPos pos : positions) {
            if (pos.equals(center)) {
                if (centerFirst) continue;
            } else if (!RightClickActionHandler.allowsNeighborUse(player, hand, hit, pos)) {
                continue;
            }
            if (modifyAt(player, hand, pos, hit, actions)) modified = true;
        }
        return modified;
    }

    private static boolean modifyAt(Player player, InteractionHand hand, BlockPos pos, BlockHitResult original,
                                    MultiToolAbilityData actions) {
        var hit = new BlockHitResult(Vec3.atCenterOf(pos), original.getDirection(), pos, original.isInside());
        return BlockToolActions.apply(new UseOnContext(player, hand, hit), actions);
    }
}
