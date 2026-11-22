package net.j40climb.florafauna_hammer.common.item.abilities;

import net.j40climb.florafauna_hammer.common.item.abilities.data.MultiToolAbilityData;
import net.minecraft.core.Holder;
import net.minecraft.core.component.BlockTransformer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.component.BlockTransformers;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;

final class BlockToolActions {
    static boolean apply(UseOnContext context, MultiToolAbilityData multiTool) {
        BlockState state = context.getLevel().getBlockState(context.getClickedPos());

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
