package net.j40climb.florafauna_hammer.test;

import net.j40climb.florafauna_hammer.common.item.abilities.data.MiningShape;
import net.j40climb.florafauna_hammer.common.item.abilities.data.MiningSpeed;
import net.j40climb.florafauna_hammer.common.item.abilities.data.ToolConfig;
import net.j40climb.florafauna_hammer.setup.HammerRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.function.Consumer;

import static net.j40climb.florafauna_hammer.test.HammerMiningGameTests.*;

final class HammerLightingGameTests {
    static void register(HammerGameTests.Registrar tests, Identifier mine) {
        for (MiningShape shape : new MiningShape[]{MiningShape.WALKABLE_TUNNEL, MiningShape.LARGE_TUNNEL,
                MiningShape.TUNNEL_UP, MiningShape.TUNNEL_DOWN}) {
            register(tests, mine, shape.getSerializedName(), h -> supported(h, shape));
        }
        for (String condition : new String[]{"disabled", "empty", "left_only", "bright", "cancel_center", "cancel_place",
                "bore", "vertical", "single"}) {
            register(tests, mine, condition, h -> withheld(h, condition));
        }
        register(tests, mine, "unloaded_wall", HammerLightingGameTests::unloadedWall);
        register(tests, mine, "separate_dark_passage", HammerLightingGameTests::separatePassage);
        register(tests, mine, "spacing", HammerLightingGameTests::spacing);
    }

    private static void register(HammerGameTests.Registrar tests, Identifier mine,
                                 String name, Consumer<GameTestHelper> test) {
        HammerGameTests.registerStructureTest(tests, "hammer_lighting_" + name, mine, 160, test);
    }

    private static ServerPlayer equipped(GameTestHelper h, MiningShape shape) {
        ServerPlayer player = player(h, shape);
        player.setGameMode(GameType.SURVIVAL);
        player.getMainHandItem().set(HammerRegistry.TOOL_CONFIG.get(),
                ToolConfig.DEFAULT.withMiningSpeed(MiningSpeed.INSTABREAK).withToggledTunnelLighting());
        player.getInventory().setItem(12, new ItemStack(Items.TORCH, 3));
        return player;
    }

    private static BlockPos rock(GameTestHelper h, ServerPlayer player) {
        BlockPos c = h.absolutePos(new BlockPos(8, 12, 8));
        fill(player, c.offset(-7, -7, -7), c.offset(7, 8, 7), Blocks.STONE);
        return c;
    }

    private static void supported(GameTestHelper h, MiningShape shape) {
        ServerPlayer player = equipped(h, shape);
        var hammer = player.getMainHandItem(); // The stock Hammer's right-click action is Throw.
        BlockPos c = rock(h, player);
        Direction travel;
        Direction face;
        BlockPos expected;
        if (shape == MiningShape.LARGE_TUNNEL) {
            travel = Direction.WEST;
            face = Direction.EAST;
            expected = c.north();
            player.getInventory().setItem(12, ItemStack.EMPTY);
            player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.TORCH, 3));
        } else if (shape == MiningShape.TUNNEL_UP) {
            travel = Direction.SOUTH;
            face = Direction.NORTH;
            expected = c.above();
        } else if (shape == MiningShape.TUNNEL_DOWN) {
            travel = Direction.NORTH;
            face = Direction.UP; // A floor hit follows the player's heading.
            expected = c.above(2);
        } else {
            travel = Direction.EAST;
            face = Direction.WEST;
            expected = c.above();
            // No free slots, and the only torch stack is in the backpack.
            for (int i = 1; i < 36; i++) if (i != 12) player.getInventory().setItem(i, new ItemStack(Items.COBBLESTONE, 64));
        }
        h.runAfterDelay(8, () -> {
            swing(player, c, face, face == Direction.UP ? travel : travel.getOpposite());
            h.succeedWhen(() -> {
                check(h, expected, Blocks.WALL_TORCH, "right wall torch");
                var state = player.level().getBlockState(expected);
                h.assertTrue(state.getValue(WallTorchBlock.FACING) == travel.getCounterClockWise(), "Incorrect torch attachment");
                h.assertTrue(count(player) == 2 && player.getMainHandItem() == hammer, "Torch consumption or held Hammer changed");
            });
        });
    }

    private static void withheld(GameTestHelper h, String condition) {
        MiningShape shape = switch (condition) {
            case "bore" -> MiningShape.BORE;
            case "single" -> MiningShape.SINGLE;
            default -> MiningShape.WALKABLE_TUNNEL;
        };
        ServerPlayer player = equipped(h, shape);
        BlockPos c = rock(h, player);
        switch (condition) {
            case "disabled" -> player.getMainHandItem().set(HammerRegistry.TOOL_CONFIG.get(), ToolConfig.DEFAULT.withMiningSpeed(MiningSpeed.INSTABREAK));
            case "empty" -> player.getInventory().setItem(12, ItemStack.EMPTY);
            case "left_only" -> fill(player, c.south(), c.south().east(4).above(2), Blocks.AIR);
            case "bright" -> fill(player, c.north().above(), c.east(4).north().above(), Blocks.GLOWSTONE);
        }
        int supply = count(player);
        boolean[] attempted = {false};
        h.runAfterDelay(8, () -> {
            Runnable releaseCenter = condition.equals("cancel_center") ? TestPlatform.vetoBreak(player, c::equals) : () -> {};
            Runnable releasePlace = condition.equals("cancel_place") ? TestPlatform.vetoPlacement(player, (pos, block) -> {
                if (block != Blocks.WALL_TORCH) return false;
                attempted[0] = true;
                return true;
            }) : () -> {};
            try {
                swing(player, c, condition.equals("vertical") ? Direction.UP : Direction.WEST, Direction.EAST);
            } finally {
                releaseCenter.run();
            }
            h.runAfterDelay(25, () -> {
                releasePlace.run();
                h.assertTrue(!condition.equals("cancel_place") || attempted[0], "No cancellable torch placement was attempted");
                check(h, c, condition.equals("cancel_center") ? Blocks.STONE : Blocks.AIR, "center break");
                h.assertTrue(count(player) == supply, "Unexpected torch consumption with " + condition);
                for (BlockPos pos : BlockPos.betweenClosed(c.offset(-1, -5, -1), c.offset(6, 6, 1))) {
                    h.assertTrue(!player.level().getBlockState(pos).is(Blocks.WALL_TORCH), "Unexpected torch with " + condition);
                }
                h.succeed();
            });
        });
    }

    private static void unloadedWall(GameTestHelper h) {
        ServerPlayer player = equipped(h, MiningShape.WALKABLE_TUNNEL);
        var level = player.level();
        BlockPos origin = h.absolutePos(new BlockPos(8, 12, 8));
        ChunkPos chunk = ChunkPos.containing(origin);
        int x = chunk.x();
        while (x < chunk.x() + 64 && level.hasChunk(x + 1, chunk.z())) x++;
        h.assertTrue(!level.hasChunk(x + 1, chunk.z()), "Could not find a loaded chunk with an unloaded east neighbor");
        BlockPos center = new BlockPos(SectionPos.sectionToBlockCoord(x) + 15, origin.getY(), chunk.getMinBlockZ() + 8);
        // This tiny fixture lies outside the test structure, so restore all of its cells.
        var before = new HashMap<BlockPos, BlockState>();
        for (BlockPos pos : BlockPos.betweenClosed(center.north(4), center.above(2))) {
            before.put(pos.immutable(), level.getBlockState(pos));
            set(player, pos, Blocks.AIR);
        }
        try {
            set(player, center, Blocks.STONE);
            swing(player, center, Direction.SOUTH, Direction.NORTH); // Travel north, right wall lies across the boundary.
        } finally {
            before.forEach((pos, state) -> level.setBlock(pos, state, Block.UPDATE_CLIENTS));
        }
        int absentX = x + 1;
        h.runAfterDelay(25, () -> {
            h.assertTrue(!level.hasChunk(absentX, chunk.z()), "Lighting loaded a neighboring chunk");
            h.assertTrue(count(player) == 3, "Unloaded wall spent a torch");
            h.succeed();
        });
    }

    private static void separatePassage(GameTestHelper h) {
        ServerPlayer player = equipped(h, MiningShape.WALKABLE_TUNNEL);
        BlockPos c = rock(h, player);
        // Two parallel passages have a solid wall between them.
        BlockPos other = c.above().south(2);
        player.level().setBlockAndUpdate(other, Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.NORTH));
        h.runAfterDelay(8, () -> {
            swing(player, c, Direction.WEST, Direction.NORTH);
            h.succeedWhen(() -> {
                check(h, c.above(), Blocks.WALL_TORCH, "dark passage behind the solid wall");
                check(h, other, Blocks.WALL_TORCH, "separate light source");
                h.assertTrue(count(player) == 2, "Incorrect torch consumption");
            });
        });
    }

    private static void spacing(GameTestHelper h) {
        ServerPlayer player = equipped(h, MiningShape.WALKABLE_TUNNEL);
        player.setGameMode(GameType.CREATIVE);
        BlockPos c = h.absolutePos(new BlockPos(2, 12, 6));
        fill(player, c.offset(-1, -1, -2), c.offset(11, 4, 2), Blocks.STONE);
        h.runAfterDelay(8, () -> {
            swing(player, c, Direction.WEST, Direction.NORTH);
            swing(player, c.east(5), Direction.WEST, Direction.NORTH);
            h.succeedWhen(() -> {
                check(h, c.above(), Blocks.WALL_TORCH, "first lit passage");
                check(h, c.east(7).above(), Blocks.WALL_TORCH, "next dark cell");
                for (int x = 1; x < 7; x++) check(h, c.east(x).above(), Blocks.AIR, "recently lit cell");
                h.assertTrue(count(player) == 3, "Creative placement consumed a torch");
            });
        });
    }

    private static int count(ServerPlayer player) {
        return player.getInventory().countItem(Items.TORCH);
    }
}
