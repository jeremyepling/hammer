package net.j40climb.florafauna_hammer.common.item.abilities;

import net.j40climb.florafauna_hammer.common.item.abilities.data.MultiToolAbilityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbilities;

final class BlockToolActions {
    static boolean apply(UseOnContext context, MultiToolAbilityData multiTool) {
        var level = context.getLevel();
        var pos = context.getClickedPos();
        var state = level.getBlockState(pos);

        // Try each enabled tool action in order of priority
        if (multiTool.strip()) {
            BlockState modified = state.getToolModifiedState(context, ItemAbilities.AXE_STRIP, false);
            if (modified != null && modified != state) {
                level.setBlock(pos, modified, 11);
                level.playSound(null, pos, SoundEvents.AXE_STRIP, SoundSource.BLOCKS, 1.0f, 1.0f);
                return true;
            }
        }

        if (multiTool.flatten()) {
            BlockState modified = state.getToolModifiedState(context, ItemAbilities.SHOVEL_FLATTEN, false);
            if (modified != null && modified != state) {
                level.setBlock(pos, modified, 11);
                level.playSound(null, pos, SoundEvents.SHOVEL_FLATTEN, SoundSource.BLOCKS, 1.0f, 1.0f);
                return true;
            }
        }

        if (multiTool.till()) {
            BlockState modified = state.getToolModifiedState(context, ItemAbilities.HOE_TILL, false);
            if (modified != null && modified != state) {
                level.setBlock(pos, modified, 11);
                level.playSound(null, pos, SoundEvents.HOE_TILL, SoundSource.BLOCKS, 1.0f, 1.0f);
                return true;
            }
        }

        return false;
    }

    // Let the player open a chest or use a workbench even if a neighboring block is eligible.
    static boolean canModifyTarget(UseOnContext context, MultiToolAbilityData actions) {
        if (!actions.hasAnyEnabled()) return false;
        var ability = actions.strip() ? ItemAbilities.AXE_STRIP : actions.flatten() ? ItemAbilities.SHOVEL_FLATTEN : ItemAbilities.HOE_TILL;
        BlockState state = context.getLevel().getBlockState(context.getClickedPos());
        var changed = state.getToolModifiedState(context, ability, true);
        return changed != null && changed != state;
    }
}
