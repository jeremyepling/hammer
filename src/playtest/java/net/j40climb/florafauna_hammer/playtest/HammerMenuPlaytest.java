package net.j40climb.florafauna_hammer.playtest;

import net.j40climb.florafauna_hammer.common.item.abilities.data.HammerAction;
import net.j40climb.florafauna_hammer.common.item.abilities.data.MiningShape;
import net.j40climb.florafauna_hammer.common.item.abilities.data.MultiToolAbilityData;
import net.j40climb.florafauna_hammer.setup.HammerRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;

final class HammerMenuPlaytest {
    static void steps(Playtest t, BlockPos target) {
        t.server("Prepare ordinary dirt with both till and path enabled", p -> {
            p.level().setBlockAndUpdate(target, Blocks.DIRT.defaultBlockState());
            p.level().setBlockAndUpdate(target.above(), Blocks.AIR.defaultBlockState());
            p.getMainHandItem().set(HammerRegistry.MULTI_TOOL_ABILITY.get(), MultiToolAbilityData.DEFAULT);
            p.inventoryMenu.broadcastChanges();
        });
        choose(t, HammerAction.TILL);
        t.awaitClient("Aim at the dirt with Till selected", mc -> mc.level.getBlockState(target).is(Blocks.DIRT)
                && mc.hitResult instanceof BlockHitResult hit && hit.getBlockPos().equals(target));
        t.client("Use the selected Till action", mc -> mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, (BlockHitResult) mc.hitResult));
        t.awaitServer("Till won over Path and kept the selected mining shape", p -> p.level().getBlockState(target).is(Blocks.FARMLAND)
                && p.getMainHandItem().get(HammerRegistry.MULTI_BLOCK_MINING.get()).shape() == MiningShape.FLAT_5X5);
        choose(t, HammerAction.AUTO);
    }

    static void choose(Playtest t, HammerAction action) {
        HammerPlaytest.openMenu(t);
        if (action == HammerAction.TILL) t.screenshot("hammer-wheels-abilities");
        t.client("Select " + action.getSerializedName(), mc -> {
            for (var available : HammerAction.values()) HammerPlaytest.button(mc, HammerRadialPlaytest.actionLabel(available));
            HammerPlaytest.click(mc, HammerPlaytest.button(mc, HammerRadialPlaytest.actionLabel(action)));
            HammerPlaytest.releaseWheel(mc);
        });
        t.awaitServer("Persist " + action.getSerializedName() + " on the server's Hammer", p -> p.getMainHandItem().get(HammerRegistry.HAMMER_ACTION.get()) == action);
        t.awaitClient("Sync " + action.getSerializedName() + " back to the held tool", mc -> Playtest.screen(mc) == null
                && mc.player.getMainHandItem().get(HammerRegistry.HAMMER_ACTION.get()) == action);
    }
}
