package net.j40climb.florafauna_hammer.playtest;

import net.j40climb.florafauna_hammer.setup.ClientSetup;
import net.j40climb.florafauna_hammer.common.item.abilities.data.MiningModeData;
import net.j40climb.florafauna_hammer.common.item.abilities.data.MiningShape;
import net.j40climb.florafauna_hammer.common.item.abilities.data.MiningSpeed;
import net.j40climb.florafauna_hammer.common.item.abilities.data.ToolConfig;
import net.j40climb.florafauna_hammer.common.item.abilities.multiblock.MultiBlockOutlineRenderer;
import net.j40climb.florafauna_hammer.setup.HammerRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

final class HammerStairOutlinePlaytest {
    private static final BlockPos WALL = new BlockPos(0, 202, 3);

    static void steps(Playtest t) {
        List<BlockPos> drawn = new ArrayList<>();
        var expected = IntStream.range(0, 5).mapToObj(i -> WALL.south(i).below(i + 2)).toList();
        t.server("Prepare a slow survival break with both north and top faces visible", p -> {
            p.setGameMode(GameType.SURVIVAL);
            p.removeAllEffects();
            for (BlockPos pos : BlockPos.betweenClosed(WALL.offset(-1, -7, -3), WALL.offset(1, 3, 4))) {
                p.level().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            }
            p.level().setBlockAndUpdate(WALL.north(3).below(), Blocks.STONE.defaultBlockState());
            p.level().setBlockAndUpdate(WALL, Blocks.DEEPSLATE.defaultBlockState());
            var hammer = new ItemStack(HammerRegistry.HAMMER.get());
            hammer.set(HammerRegistry.MULTI_BLOCK_MINING.get(), new MiningModeData(MiningShape.TUNNEL_DOWN, 0, 64, true));
            hammer.set(HammerRegistry.TOOL_CONFIG.get(), new ToolConfig(true, false, MiningSpeed.STANDARD));
            p.getInventory().setSelectedSlot(0);
            p.getInventory().setItem(0, hammer);
            p.teleportTo(p.level(), 0.5, 202, 0.5, Set.of(), 0f, 20f, true);
            p.inventoryMenu.broadcastChanges();
        });
        t.awaitClient("Aim at the north face with the descending Hammer", mc -> {
            var mode = mc.player.getMainHandItem().get(HammerRegistry.MULTI_BLOCK_MINING.get());
            return mode != null && mode.shape() == MiningShape.TUNNEL_DOWN && aimedAt(mc, Direction.NORTH);
        });
        t.client("Begin the north-face swing using the attack key", mc -> {
            KeyMapping.click(ClientSetup.boundKey(mc.options.keyAttack));
            mc.options.keyAttack.setDown(true);
        });
        t.awaitClient("Aim up at the top face while still mining the same block", mc -> {
            if (!mc.gameMode.isDestroying()) return false;
            mc.player.setXRot(11.7f);
            return true;
        });
        t.awaitClient("Compare top-face hover outline with original wall treads", mc -> {
            if (!aimedAt(mc, Direction.UP)) return false;
            Playtest.require(mc.gameMode.isDestroying(), "Lost the multi-tick swing");
            var hit = (BlockHitResult) mc.hitResult;
            var preview = MultiBlockOutlineRenderer.previewPlan(mc.player, hit.getBlockPos(), hit.getDirection());
            Playtest.require(preview.stairs().equals(expected), "Outline moved after the face changed: " + preview.stairs());
            drawn.addAll(preview.stairs());
            return true;
        });
        t.awaitServer("Finish the original swing and verify all five previewed stairs", p -> {
            if (!p.level().isEmptyBlock(WALL)) return false;
            for (BlockPos pos : drawn) {
                var state = p.level().getBlockState(pos);
                Playtest.require(state.is(Blocks.COBBLESTONE_STAIRS) && state.getValue(StairBlock.FACING) == Direction.NORTH,
                        "Server disagreed with the outline at " + pos + ": " + state);
            }
            return true;
        });
        t.client("Release attack", mc -> mc.options.keyAttack.setDown(false));
    }

    private static boolean aimedAt(Minecraft mc, Direction face) {
        return mc.hitResult instanceof BlockHitResult hit && hit.getBlockPos().equals(WALL) && hit.getDirection() == face;
    }
}
