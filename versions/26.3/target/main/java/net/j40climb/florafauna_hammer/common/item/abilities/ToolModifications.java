package net.j40climb.florafauna_hammer.common.item.abilities;

import net.j40climb.florafauna_hammer.common.item.abilities.data.MultiToolAbilityData;
import net.j40climb.florafauna_hammer.common.item.abilities.multiblock.MultiBlockPatterns;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.BlockTransformer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.BlockTransformers;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Applies the platform's strip, path and till interactions to the active mining pattern. */
final class ToolModifications {
    private ToolModifications() {}

    static boolean apply(Player player, InteractionHand hand, BlockPos pos, BlockHitResult hitResult, MultiToolAbilityData multiTool, boolean requireTarget) {
        var blocksToModify = MultiBlockPatterns.plan(player, player.getItemInHand(hand), pos, hitResult.getDirection()).blocks();

        boolean anyModified = false;
        if (requireTarget) {
            // Explicit actions leave normal block use alone when the target cannot transform.
            if (!blocksToModify.contains(pos) || !tryModifySingleBlock(player, hand, pos, hitResult, multiTool)) return false;
            anyModified = true;
        }
        for (BlockPos targetPos : blocksToModify) {
            if (requireTarget && targetPos.equals(pos)) continue;
            if (!targetPos.equals(pos) && !RightClickActionHandler.allowsNeighborUse(player, hand, hitResult, targetPos)) continue;
            if (tryModifySingleBlock(player, hand, targetPos, hitResult, multiTool)) {
                anyModified = true;
            }
        }
        return anyModified;
    }

    private static boolean tryModifySingleBlock(Player player, InteractionHand hand, BlockPos pos, BlockHitResult originalHit, MultiToolAbilityData multiTool) {
        BlockState state = player.level().getBlockState(pos);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), originalHit.getDirection(), pos, originalHit.isInside());
        UseOnContext context = new UseOnContext(player, hand, hit);

        // The vanilla axe transformer also scrapes copper. This tool's strip setting is for logs and bamboo.
        if (multiTool.strip() && (state.is(BlockTags.LOGS) || state.is(BlockTags.BAMBOO_BLOCKS))
                && transformBlock(context, BlockTransformers.AXE)) {
            return true;
        }
        if (multiTool.flatten() && transformBlock(context, BlockTransformers.SHOVEL)) {
            return true;
        }
        return multiTool.till() && transformBlock(context, BlockTransformers.HOE);
    }

    private static boolean transformBlock(UseOnContext context, ResourceKey<BlockTransformer> key) {
        Holder<BlockTransformer> transformer = context.getLevel().registryAccess().getOrThrow(key);
        return ToolTransformer.apply(context, transformer);
    }
}
