package net.j40climb.florafauna_hammer.common.item.abilities;

import net.fabricmc.fabric.api.registry.StrippableBlockRegistry;
import net.j40climb.florafauna_hammer.common.item.abilities.data.MultiToolAbilityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.CampfireBlock;

final class BlockToolActions {
    static boolean apply(UseOnContext context, MultiToolAbilityData multiTool) {
        var level = context.getLevel();
        var pos = context.getClickedPos();
        var state = level.getBlockState(pos);

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
