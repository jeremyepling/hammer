package net.j40climb.florafauna_hammer.common.item.abilities;

import net.j40climb.florafauna_hammer.common.item.abilities.data.MultiToolAbilityData;
import net.j40climb.florafauna_hammer.common.item.abilities.multiblock.MultiBlockPatterns;
import net.minecraft.core.BlockPos;
import net.fabricmc.fabric.api.registry.StrippableBlockRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Applies the platform's strip, path and till interactions to the active mining pattern. */
final class ToolModifications {
    private ToolModifications() {}

    static boolean apply(Player player, InteractionHand hand, BlockPos pos, BlockHitResult hitResult, MultiToolAbilityData multiTool, boolean requireTarget) {
        if (!(player instanceof ServerPlayer)) return false;
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
        var level = player.level();
        var state = level.getBlockState(pos);
        var hit = new BlockHitResult(Vec3.atCenterOf(pos), originalHit.getDirection(), pos, originalHit.isInside());
        var context = new UseOnContext(player, hand, hit);
        if (multiTool.strip()) {
            var stripped = StrippableBlockRegistry.getStrippedBlockState(state);
            if (stripped != null && level.setBlock(pos, stripped, 11)) {
                level.playSound(null, pos, SoundEvents.AXE_STRIP, SoundSource.BLOCKS, 1, 1);
                return true;
            }
        }
        // Shovels can also extinguish a campfire. Path mode only flattens blocks.
        if (multiTool.flatten() && !(state.getBlock() instanceof CampfireBlock)
                && Items.IRON_SHOVEL.useOn(context).consumesAction()) return true;
        return multiTool.till() && Items.IRON_HOE.useOn(context).consumesAction();
    }
}
