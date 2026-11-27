package net.j40climb.florafauna_hammer.test;

import net.j40climb.florafauna_hammer.HammerMod;
import net.j40climb.florafauna_hammer.common.item.abilities.data.MiningModeData;
import net.j40climb.florafauna_hammer.common.item.abilities.data.MiningShape;
import net.j40climb.florafauna_hammer.common.item.abilities.data.MiningSpeed;
import net.j40climb.florafauna_hammer.common.item.abilities.data.ToolConfig;
import net.j40climb.florafauna_hammer.common.item.abilities.multiblock.LavaExposure;
import net.j40climb.florafauna_hammer.common.item.abilities.multiblock.MultiBlockPatterns;
import net.j40climb.florafauna_hammer.common.item.abilities.networking.UpdateToolConfigPayload;
import net.j40climb.florafauna_hammer.common.item.hammer.HammerStage;
import net.j40climb.florafauna_hammer.setup.HammerRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.ObserverBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class HammerMiningGameTests {
    private static final Identifier MINE = Identifier.fromNamespaceAndPath(HammerMod.MOD_ID, "test_hammer_mine");

    static void register(HammerGameTests.Registrar tests) {
        for (MiningShape mode : MiningShape.values()) {
            String name = "hammer_" + mode.getSerializedName() + "_all_faces";
            HammerGameTests.registerStructureTest(tests, name, MINE, 100, h -> allFaces(h, mode));
        }
        HammerGameTests.registerStructureTest(tests, "hammer_ore_drops", MINE, 100, HammerMiningGameTests::oreDrops);
        HammerGameTests.registerStructureTest(tests, "hammer_repeated_excavation", MINE, 100, HammerMiningGameTests::repeat);
        HammerGameTests.registerStructureTest(tests, "hammer_vein_limit_and_connectivity", MINE, 100, HammerMiningGameTests::vein);
        HammerGameTests.registerStructureTest(tests, "hammer_vein_material_pairs", MINE, 100, HammerMiningGameTests::veinMaterialPairs);
        HammerGameTests.registerStructureTest(tests, "hammer_vein_exact_and_boundaries", MINE, 100, HammerMiningGameTests::veinBoundaries);
        HammerLightingGameTests.register(tests, MINE);
        HammerGameTests.registerStructureTest(tests, "hammer_tunnel_gaps", MINE, 100, HammerMiningGameTests::gaps);
        HammerGameTests.registerStructureTest(tests, "hammer_stair_player_collision", MINE, 100, HammerMiningGameTests::stairPlayerCollision);
        HammerGameTests.registerStructureTest(tests, "hammer_stair_canceled_placement", MINE, 100, HammerMiningGameTests::stairCanceledPlacement);
        HammerGameTests.registerStructureTest(tests, "hammer_lava_exposure", MINE, 100, HammerMiningGameTests::lavaExposure);
        HammerGameTests.registerStructureTest(tests, "hammer_dimension_floor", MINE, 100, HammerMiningGameTests::buildFloor);
        HammerGameTests.registerStructureTest(tests, "hammer_single_is_per_tool", MINE, 100, HammerMiningGameTests::perToolMode);
        HammerGameTests.registerStructureTest(tests, "hammer_permission_and_tool_rules", MINE, 100, HammerMiningGameTests::eligibility);
    }

    private static void lavaExposure(GameTestHelper h) {
        var player = player(h, MiningShape.BORE);
        var center = h.absolutePos(new BlockPos(6, 12, 10));
        fill(player, center.offset(-1, -2, -2), center.offset(9, 2, 2), Blocks.STONE);
        set(player, center.below(), Blocks.LAVA);
        set(player, center.east(3).south(), Blocks.LAVA);
        player.level().setBlock(center.east(5).above(), Blocks.LAVA.defaultBlockState().setValue(LiquidBlock.LEVEL, 3), Block.UPDATE_CLIENTS);
        set(player, center.east(7), Blocks.LAVA);
        set(player, center.east(4).north().above(), Blocks.LAVA); // Only touches an edge.
        set(player, center.east(4).south(), Blocks.WATER);
        set(player, center.east(), Blocks.AIR);
        set(player, center.east().above(), Blocks.LAVA);
        set(player, center.east(2), Blocks.BEDROCK);
        set(player, center.east(2).north(), Blocks.LAVA);

        var expected = Set.of(new LavaExposure(center, Direction.DOWN), new LavaExposure(center.east(3), Direction.SOUTH),
                new LavaExposure(center.east(5), Direction.UP), new LavaExposure(center.east(6), Direction.EAST));
        var plan = MultiBlockPatterns.plan(player, player.getMainHandItem(), center, Direction.WEST);
        h.assertTrue(Set.copyOf(LavaExposure.find(player.level(), plan)).equals(expected), "Wrong exposed faces along the bore");
        configure(player, MiningShape.SINGLE);
        var precise = MultiBlockPatterns.plan(player, player.getMainHandItem(), center, Direction.WEST);
        h.assertTrue(LavaExposure.find(player.level(), precise).equals(List.of(new LavaExposure(center, Direction.DOWN))),
                "Single preview marked lava outside the single block");
        check(h, center.east(6), Blocks.STONE, "Preview changed the wall");
        configure(player, MiningShape.BORE);
        swing(player, center, Direction.WEST, Direction.EAST);
        check(h, center.east(6), Blocks.AIR, "Lava warning blocked the cut");
        check(h, center.east(7), Blocks.LAVA, "Lava warning consumed lava");
        h.succeed();
    }

    private static void perToolMode(GameTestHelper h) {
        var precise = player(h, MiningShape.SINGLE);
        var other = player(h, MiningShape.TUNNEL_DOWN);
        var center = h.absolutePos(new BlockPos(11, 14, 11));
        var originalMode = precise.getMainHandItem().get(HammerRegistry.MULTI_BLOCK_MINING.get());
        fill(precise, center.offset(0, -6, -4), center.above(2), Blocks.STONE);
        swing(precise, center, Direction.SOUTH, Direction.NORTH);
        check(h, center, Blocks.AIR, "Single break");
        check(h, center.below(2), Blocks.STONE, "Single placed an unwanted stair");
        check(h, center.north(), Blocks.STONE, "Single mined a neighbor");
        set(precise, center, Blocks.STONE);
        swing(other, center, Direction.SOUTH, Direction.NORTH);
        stair(h, center.below(2), Direction.SOUTH);
        h.assertTrue(originalMode.equals(precise.getMainHandItem().get(HammerRegistry.MULTI_BLOCK_MINING.get())),
                "Another player rewrote the selected Single mode");
        h.succeed();
    }

    private static void allFaces(GameTestHelper h, MiningShape mode) {
        BlockPos center = h.absolutePos(new BlockPos(11, 14, 11));
        ServerPlayer player = player(h, mode);
        player.setGameMode(GameType.SURVIVAL);
        player.setOnGround(true);
        for (Direction face : Direction.values()) {
            for (Direction heading : Direction.Plane.HORIZONTAL) {
                // Check both player axes on vertical hits. Side hits depend only on the face.
                if (face.getAxis().isHorizontal() && heading != Direction.NORTH) continue;
                fill(player, center.offset(-7, -8, -7), center.offset(7, 9, 7), Blocks.STONE);
                Map<BlockPos, Block> expected = new HashMap<>();
                Direction stairsFace = Direction.NORTH;
                switch (mode) {
                    case SINGLE -> expected.put(center, Blocks.AIR);
                    case FLAT_3X3, FLAT_5X5, FLAT_7X7 -> {
                        int radius = switch (mode) {
                            case FLAT_3X3 -> 1;
                            case FLAT_5X5 -> 2;
                            default -> 3;
                        };
                        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius,-radius,-radius), center.offset(radius,radius,radius))) {
                            if (face.getAxis().choose(pos.getX()-center.getX(), pos.getY()-center.getY(), pos.getZ()-center.getZ()) == 0) {
                                expected.put(pos.immutable(), Blocks.AIR);
                            }
                        }
                    }
                    case SHAPELESS -> {
                        for (int i = 0; i < 4; i++) {
                            var ore = center.relative(face.getOpposite(), i);
                            set(player, ore, Blocks.IRON_ORE);
                            expected.put(ore, Blocks.AIR);
                        }
                    }
                    case WALKABLE_TUNNEL, LARGE_TUNNEL, BORE -> {
                        Direction endDirection = face.getOpposite();
                        int length = mode == MiningShape.BORE ? 7 : 5;
                        BlockPos end = center.relative(endDirection, length - 1);
                        BlockPos first = center;
                        BlockPos last = end;
                        if (mode == MiningShape.LARGE_TUNNEL) {
                            // One block of padding along each axis perpendicular to the depth.
                            int x = face.getAxis() == Direction.Axis.X ? 0 : 1;
                            int y = face.getAxis() == Direction.Axis.Y ? 0 : 1;
                            int z = face.getAxis() == Direction.Axis.Z ? 0 : 1;
                            first = center.offset(-x, -y, -z);
                            last = end.offset(x, y, z);
                        } else if (mode == MiningShape.WALKABLE_TUNNEL) {
                            if (face.getAxis().isHorizontal()) {
                                last = end.above(2);
                            } else {
                                first = center.relative(heading.getOpposite());
                                last = end.relative(heading);
                            }
                        }
                        for (BlockPos pos : BlockPos.betweenClosed(first, last)) expected.put(pos.immutable(), Blocks.AIR);
                        h.assertTrue(expected.size() == switch (mode) {
                            case WALKABLE_TUNNEL -> 15;
                            case LARGE_TUNNEL -> 45;
                            default -> 7;
                        }, "Incorrect test footprint for " + mode);
                        // Air and denied cells consume depth. Blocks beyond the gaps still break.
                        set(player, center.relative(endDirection), Blocks.AIR);
                        BlockPos protectedBlock = center.relative(endDirection, 2);
                        set(player, protectedBlock, Blocks.BEDROCK);
                        expected.put(protectedBlock, Blocks.BEDROCK);
                    }
                    case TUNNEL_UP, TUNNEL_DOWN -> {
                        boolean up = mode == MiningShape.TUNNEL_UP;
                        if (face == Direction.DOWN || (up && face == Direction.UP)) {
                            expected.put(center, Blocks.AIR);
                        } else {
                            Direction travel = face == Direction.UP ? heading : face.getOpposite();
                            stairsFace = up ? travel : travel.getOpposite();
                            BlockPos tread = face == Direction.UP ? center : center.below(up ? 1 : 2);
                            for (int step = 0; step < 5; step++) {
                                expected.put(tread, Blocks.COBBLESTONE_STAIRS);
                                for (int y = 1; y <= 4; y++) expected.put(tread.above(y), Blocks.AIR);
                                tread = tread.relative(travel).above(up ? 1 : -1);
                            }
                        }
                    }
                }
                swing(player, center, face, heading);
                for (BlockPos pos : BlockPos.betweenClosed(center.offset(-7, -8, -7), center.offset(7, 9, 7))) {
                    Block block = expected.getOrDefault(pos, Blocks.STONE);
                    check(h, pos, block, mode + " " + face + " " + heading);
                    if (block == Blocks.COBBLESTONE_STAIRS) stair(h, pos, stairsFace);
                }
            }
        }
        h.succeed();
    }

    private static void oreDrops(GameTestHelper h) {
        ServerPlayer player = player(h, MiningShape.FLAT_3X3);
        player.setGameMode(GameType.SURVIVAL);
        player.setOnGround(true);
        for (boolean silk : new boolean[]{false, true}) {
            var center = h.absolutePos(new BlockPos(silk ? 14 : 5, 10, 5));
            UpdateToolConfigPayload.applyToHeldItem(player, new ToolConfig(!silk, silk, MiningSpeed.INSTABREAK));
            set(player, center, Blocks.DIAMOND_ORE);
            set(player, center.east(), Blocks.DIAMOND_ORE);
            swing(player, center, Direction.UP, Direction.SOUTH);
            check(h, center, Blocks.AIR, "ore center");
            check(h, center.east(), Blocks.AIR, "ore neighbor");
            var bounds = new AABB(Vec3.atCenterOf(center), Vec3.atCenterOf(center.east())).inflate(2);
            var drops = h.getLevel().getEntitiesOfClass(ItemEntity.class, bounds);
            int count = 0;
            for (var drop : drops) {
                var stack = drop.getItem();
                h.assertTrue(stack.is(silk ? Items.DIAMOND_ORE : Items.DIAMOND), "Unexpected ore drop: " + stack);
                count += stack.getCount();
            }
            h.assertTrue(silk ? count == 2 : count >= 2 && count <= 8, "Wrong total ore loot: " + count);
            boolean hasExperience = !h.getLevel().getEntitiesOfClass(ExperienceOrb.class, bounds).isEmpty();
            h.assertTrue(hasExperience == !silk, "Wrong experience outcome for " + (silk ? "Silk Touch" : "Fortune"));
        }
        h.succeed();
    }

    private static void repeat(GameTestHelper h) {
        var player = player(h, MiningShape.FLAT_3X3);
        var start = h.absolutePos(new BlockPos(6, 22, 5));
        for (Direction travel : new Direction[]{Direction.SOUTH, Direction.DOWN}) {
            var end = start.relative(travel, 4);
            if (travel == Direction.SOUTH) fill(player, start.offset(-2, -2, -2), end.offset(2, 2, 2), Blocks.STONE);
            // Explicit box for the descending lane (the end is below the start).
            if (travel == Direction.DOWN) fill(player, end.offset(-2, -1, -2), start.offset(2, 1, 2), Blocks.STONE);
            for (int i = 0; i < 4; i++) {
                BlockPos center = start.relative(travel, i);
                swing(player, center, travel.getOpposite(), Direction.SOUTH);
                for (int x = -2; x <= 2; x++) {
                    for (int b = -2; b <= 2; b++) {
                        var pos = travel == Direction.DOWN ? center.offset(x, 0,b) : center.offset(x,b, 0);
                        check(h, pos, Math.abs(x) <= 1 && Math.abs(b) <= 1 ? Blocks.AIR : Blocks.STONE, "pass " + i);
                    }
                }
                check(h, center.relative(travel), Blocks.STONE, "backing");
            }
        }
        configure(player, MiningShape.TUNNEL_DOWN);
        start = h.absolutePos(new BlockPos(4, 24, 15));
        fill(player, start.offset(-1, -16, -1), start.offset(16, 5, 1), Blocks.STONE);
        for (int flight = 0; flight < 3; flight++) {
            var landing = start.offset(flight * 5, -flight * 5, 0);
            swing(player, landing, Direction.UP, Direction.EAST);
        }
        for (int step = 0; step < 15; step++) {
            var tread = start.offset(step, -step, 0);
            stair(h, tread, Direction.WEST);
            for (int y = 1; y <= 4; y++) check(h, tread.above(y), Blocks.AIR, "headroom");
            check(h, tread.above(5), Blocks.STONE, "roof");
            check(h, tread.below(), Blocks.STONE, "floor");
            check(h, tread.north(), Blocks.STONE, "side wall");
        }
        h.succeed();
    }

    private static void veinMaterialPairs(GameTestHelper h) {
        var player = player(h, MiningShape.SHAPELESS);
        BlockPos center = h.absolutePos(new BlockPos(5, 12, 5));
        Map<Block, Block> pairs = Map.of(
                Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE,
                Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE,
                Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE,
                Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE,
                Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE,
                Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE,
                Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE,
                Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE);
        pairs.forEach((stone, deep) -> {
            checkMixedVein(h, player, center, stone, deep);
            checkMixedVein(h, player, center, deep, stone);
        });
        h.succeed();
    }

    private static void checkMixedVein(GameTestHelper h, ServerPlayer player, BlockPos center, Block start, Block variant) {
        BlockPos neighbor = center.offset(1, 1, 0);
        BlockPos end = neighbor.offset(1, 1, 1);
        BlockPos island = end.east(2);
        Block other = start == Blocks.IRON_ORE || variant == Blocks.IRON_ORE ? Blocks.COPPER_ORE : Blocks.IRON_ORE;
        set(player, center, start);
        set(player, neighbor, variant);
        set(player, end, start);
        set(player, island, variant);
        set(player, center.north(), other);
        set(player, center.south(), Blocks.NETHER_GOLD_ORE);

        var plan = MultiBlockPatterns.plan(player, player.getMainHandItem(), center, Direction.UP);
        if (!plan.blocks().equals(List.of(center, neighbor, end))) throw h.assertionException("Incorrect mixed vein preview for " + start);
        swing(player, center, Direction.UP, Direction.NORTH);
        for (BlockPos pos : plan.blocks()) check(h, pos, Blocks.AIR, "mixed vein swing");
        check(h, island, variant, "disconnected island");
        check(h, center.north(), other, "touching different material");
        check(h, center.south(), Blocks.NETHER_GOLD_ORE, "touching Nether gold");
    }

    private static void veinBoundaries(GameTestHelper h) {
        var player = player(h, MiningShape.SHAPELESS);
        BlockPos center = h.absolutePos(new BlockPos(5, 12, 5));
        UpdateToolConfigPayload.applyToHeldItem(player, ToolConfig.DEFAULT.withToggledVeinMatching());
        set(player, center, Blocks.IRON_ORE);
        set(player, center.west(), Blocks.IRON_ORE);
        set(player, center.east(), Blocks.DEEPSLATE_IRON_ORE);
        set(player, center.east(2), Blocks.IRON_ORE);
        swing(player, center, Direction.UP, Direction.NORTH);
        check(h, center, Blocks.AIR, "exact center");
        check(h, center.west(), Blocks.AIR, "exact neighbor");
        check(h, center.east(), Blocks.DEEPSLATE_IRON_ORE, "exact mode rejects variant");
        check(h, center.east(2), Blocks.IRON_ORE, "variant cannot bridge exact blocks");

        UpdateToolConfigPayload.applyToHeldItem(player, ToolConfig.DEFAULT);
        player.getMainHandItem().set(HammerRegistry.MULTI_BLOCK_MINING.get(), new MiningModeData(MiningShape.SHAPELESS, 1, 2, true));
        set(player, center, Blocks.IRON_ORE);
        swing(player, center, Direction.UP, Direction.NORTH);
        check(h, center, Blocks.AIR, "capped mixed vein center");
        check(h, center.east(), Blocks.AIR, "variant consumes one slot");
        check(h, center.east(2), Blocks.IRON_ORE, "mixed vein respects two-block cap");

        configure(player, MiningShape.SHAPELESS);
        //? if >=26.3 {
        /*for (var entry : Map.of(Blocks.NETHER_GOLD_ORE, Blocks.GOLD_ORE, Blocks.WOOL.white(), Blocks.WOOL.black()).entrySet()) {*/
        //?} else {
        for (var entry : Map.of(Blocks.NETHER_GOLD_ORE, Blocks.GOLD_ORE, Blocks.WHITE_WOOL, Blocks.BLACK_WOOL).entrySet()) {
        //?}
            set(player, center, entry.getKey());
            set(player, center.west(), entry.getKey());
            set(player, center.east(), entry.getValue());
            swing(player, center, Direction.UP, Direction.NORTH);
            check(h, center, Blocks.AIR, "exact block center");
            check(h, center.west(), Blocks.AIR, "identical neighbor");
            check(h, center.east(), entry.getValue(), "unpaired block stays exact");
        }
        h.succeed();
    }

    private static void vein(GameTestHelper h) {
        var player = player(h, MiningShape.SHAPELESS);
        var center = h.absolutePos(new BlockPos(5, 12, 5));
        var last = center.offset(6, 0, 0);
        for (BlockPos pos : new BlockPos[]{center, center.east(), center.offset(2, 1, 0), center.offset(3, 2, 1), last}) {
            set(player, pos, Blocks.IRON_ORE);
        }
        set(player, center.north(), Blocks.COPPER_ORE);
        swing(player, center, Direction.NORTH, Direction.EAST);
        for (BlockPos pos : new BlockPos[]{center, center.east(), center.offset(2, 1, 0), center.offset(3, 2, 1)}) {
            check(h, pos, Blocks.AIR, "face, edge and corner neighbors");
        }
        check(h, last, Blocks.IRON_ORE, "disconnected island");
        check(h, center.north(), Blocks.COPPER_ORE, "different ore");
        // 4x4x4 plus one ore reachable only through the last column. Exactly 64 may break.
        var cap = center.above(5);
        fill(player, cap, cap.offset(3, 3, 3), Blocks.IRON_ORE);
        set(player, cap.offset(4, 3, 3), Blocks.IRON_ORE);
        swing(player, cap, Direction.UP, Direction.EAST);
        for (BlockPos pos : BlockPos.betweenClosed(cap, cap.offset(3, 3, 3))) check(h, pos, Blocks.AIR, "64-block cap");
        check(h, cap.offset(4, 3, 3), Blocks.IRON_ORE, "65th block");

        // Custom components can request a smaller cap. Malformed legacy values still have bounded behavior.
        var bounded = new MiningModeData(MiningShape.SHAPELESS, Integer.MAX_VALUE, Integer.MAX_VALUE, true);
        if (bounded.radius() != 3 || bounded.maxBlocksToBreak() != 64) throw h.assertionException("Unbounded component");
        player.getMainHandItem().set(HammerRegistry.MULTI_BLOCK_MINING.get(), new MiningModeData(MiningShape.SHAPELESS, 1, 2, true));
        fill(player, cap, cap.east(2), Blocks.IRON_ORE);
        swing(player, cap, Direction.UP, Direction.NORTH);
        check(h, cap.east(), Blocks.AIR, "custom second block");
        check(h, cap.east(2), Blocks.IRON_ORE, "custom cap");
        h.succeed();
    }

    private static void buildFloor(GameTestHelper h) {
        var player = player(h, MiningShape.TUNNEL_DOWN);
        var area = h.absolutePos(new BlockPos(5, 0, 5));
        var level = h.getLevel();
        var center = new BlockPos(area.getX(), level.getMinY() + 2, area.getZ());
        // Stock GameTest structures are placed above the floor. Restore this small outlying fixture.
        Map<BlockPos, BlockState> original = new HashMap<>();
        for (BlockPos pos : BlockPos.betweenClosed(center.below(2), center.offset(4, 4, 0))) {
            original.put(pos.immutable(), level.getBlockState(pos));
            set(player, pos, Blocks.STONE);
        }
        try {
            swing(player, center, Direction.UP, Direction.EAST);
            for (int step = 0; step <= 2; step++) stair(h, center.east(step).below(step), Direction.WEST);
            check(h, center.east(3).below(2), Blocks.AIR, "headroom at minimum Y");
            check(h, center.east(3).below(3), Blocks.VOID_AIR, "below build height");
        } finally {
            original.forEach((pos, state) -> level.setBlock(pos, state, Block.UPDATE_CLIENTS));
        }
        h.succeed();
    }

    private static void gaps(GameTestHelper h) {
        var player = player(h, MiningShape.TUNNEL_UP);
        player.setGameMode(GameType.SURVIVAL);
        player.setOnGround(true);
        for (var mode : new MiningShape[]{MiningShape.TUNNEL_UP, MiningShape.TUNNEL_DOWN}) {
            configure(player, mode);
            boolean up = mode == MiningShape.TUNNEL_UP;
            var center = h.absolutePos(new BlockPos(5, 17, up ? 5 : 12));
            fill(player, center.offset(-1, -7, -1), center.offset(5, 8, 1), Blocks.STONE);
            // A two-column cave with air beneath the future stairs and solid rock on the far side.
            fill(player, center.offset(1, -7, -1), center.offset(2, 8, 1), Blocks.AIR);
            var firstTread = center.below(up ? 1 : 2);
            var bedrock = firstTread.east(3).above(up ? 3 : -3);
            set(player, bedrock, Blocks.BEDROCK);
            swing(player, center, Direction.WEST, Direction.EAST);
            for (int step = 0; step < 5; step++) {
                var tread = firstTread.east(step).above(up ? step : -step);
                if (step == 3) check(h, tread, Blocks.BEDROCK, "unbreakable tread");
                else stair(h, tread, up ? Direction.EAST : Direction.WEST);
                for (int y = 1; y <= 4; y++) check(h, tread.above(y), Blocks.AIR, "headroom");
                if (step == 1 || step == 2) {
                    check(h, tread.below(), Blocks.AIR, "empty space under the bridge");
                    check(h, tread.north(), Blocks.AIR, "empty space beside the bridge");
                }
            }
            check(h, center.east(5), Blocks.STONE, "swing ends at its normal reach");
        }
        h.succeed();
    }

    private static BlockPos prepareDescendingGap(GameTestHelper h, ServerPlayer miner) {
        BlockPos center = h.absolutePos(new BlockPos(5, 17, 12));
        fill(miner, center.offset(-1, -7, -1), center.offset(5, 4, 1), Blocks.AIR);
        set(miner, center, Blocks.STONE);
        return center;
    }

    private static void stairPlayerCollision(GameTestHelper h) {
        var miner = player(h, MiningShape.TUNNEL_DOWN);
        BlockPos center = prepareDescendingGap(h, miner);
        BlockPos occupied = center.east().below(3);
        var occupant = TestPlatform.player(h.getLevel(), "stair-occupant");
        occupant.setPos(Vec3.atBottomCenterOf(occupied));
        h.getLevel().addNewPlayer(occupant);
        try {
            h.assertTrue(h.getLevel().getEntitiesOfClass(ServerPlayer.class, new AABB(occupied)).contains(occupant),
                    "Second player must occupy the planned tread");
            swing(miner, center, Direction.WEST, Direction.EAST);
            check(h, occupied, Blocks.AIR, "stair inside another player");
            stair(h, occupied.east().below(), Direction.WEST);
        } finally {
            h.getLevel().removePlayerImmediately(occupant, Entity.RemovalReason.DISCARDED);
        }
        h.succeed();
    }

    private static void stairCanceledPlacement(GameTestHelper h) {
        var miner = player(h, MiningShape.TUNNEL_DOWN);
        BlockPos center = prepareDescendingGap(h, miner);
        BlockPos denied = center.east().below(3);
        BlockPos allowed = denied.east().below();
        var level = h.getLevel();
        var observer = Blocks.OBSERVER.defaultBlockState().setValue(ObserverBlock.FACING, Direction.SOUTH);
        level.setBlock(denied.north(), observer, Block.UPDATE_ALL);
        level.setBlock(allowed.north(), observer, Block.UPDATE_ALL);
        boolean[] attempted = {false};
        Runnable release = TestPlatform.vetoPlacement(miner, (position, block) -> {
            if (!position.equals(denied) || block != Blocks.COBBLESTONE_STAIRS) return false;
            attempted[0] = true;
            return true;
        });
        try {
            swing(miner, center, Direction.WEST, Direction.EAST);
        } finally {
            release.run();
        }
        h.assertTrue(attempted[0], "Placement hook must see the proposed gap stair");
        check(h, denied, Blocks.AIR, "vetoed tread");
        stair(h, allowed, Direction.WEST);
        h.assertTrue(!level.getBlockTicks().hasScheduledTick(denied.north(), Blocks.OBSERVER),
                "Vetoed stair scheduled an observer pulse");
        h.assertTrue(level.getBlockTicks().hasScheduledTick(allowed.north(), Blocks.OBSERVER),
                "Accepted stair should notify the observer watching it");
        h.runAfterDelay(2, () -> {
            h.assertTrue(!level.getBlockState(denied.north()).getValue(ObserverBlock.POWERED),
                    "Observer powered after a vetoed stair");
            h.succeed();
        });
    }

    private static void eligibility(GameTestHelper h) {
        var player = player(h, MiningShape.FLAT_3X3);
        var center = h.absolutePos(new BlockPos(8, 12, 8));
        fill(player, center.offset(-1, -1, -1), center.offset(1, 1, 1), Blocks.STONE);
        Runnable releaseCenter = TestPlatform.vetoBreak(player, center::equals);
        try {
            swing(player, center, Direction.UP, Direction.NORTH);
        } finally {
            releaseCenter.run();
        }
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-1, 0, -1), center.offset(1, 0, 1))) {
            check(h, pos, Blocks.STONE, "canceled center");
        }

        var protectedPos = center.west();
        Runnable releaseNeighbor = TestPlatform.vetoBreak(player, protectedPos::equals);
        var border = player.level().getWorldBorder();
        double x = border.getCenterX(), z = border.getCenterZ(), size = border.getSize();
        try {
            border.setCenter(center.getX() - 4.0, center.getZ() + 0.5);
            border.setSize(10.0); // east edge at center.x + 1
            swing(player, center, Direction.UP, Direction.NORTH);
        } finally {
            border.setCenter(x, z);
            border.setSize(size);
            releaseNeighbor.run();
        }
        check(h, protectedPos, Blocks.STONE, "canceled extra");
        check(h, center.east(), Blocks.STONE, "outside world border");
        check(h, center, Blocks.AIR, "allowed center");
        check(h, center.north(), Blocks.AIR, "allowed neighbor");

        player.setGameMode(GameType.SURVIVAL);
        var pick = new ItemStack(Items.DIAMOND_PICKAXE);
        player.setItemInHand(InteractionHand.MAIN_HAND, pick);
        for (boolean ignore : new boolean[]{false, true}) {
            pick.set(HammerRegistry.MULTI_BLOCK_MINING.get(), new MiningModeData(MiningShape.FLAT_3X3, 1, 64, ignore));
            fill(player, center.offset(-1, 0, -1), center.offset(1, 0, 1), Blocks.STONE);
            set(player, center.west(), Blocks.OAK_LOG);
            set(player, center.south(), Blocks.BEDROCK);
            swing(player, center, Direction.UP, Direction.SOUTH);
            check(h, center, Blocks.AIR, "survival center");
            check(h, center.east(), Blocks.AIR, "pickaxe ore eligibility");
            check(h, center.west(), ignore ? Blocks.AIR : Blocks.OAK_LOG, "respect tool restrictions = " + !ignore);
            check(h, center.south(), Blocks.BEDROCK, "unbreakable block");
        }
        h.succeed();
    }

    static ServerPlayer player(GameTestHelper h, MiningShape mode) {
        var player = TestPlatform.player(h.getLevel(), "mining-test");
        player.setGameMode(GameType.CREATIVE);
        player.setItemInHand(InteractionHand.MAIN_HAND, HammerStage.TEMPEST.createStack());
        configure(player, mode);
        return player;
    }

    static void configure(ServerPlayer player, MiningShape mode) {
        player.getMainHandItem().set(HammerRegistry.MULTI_BLOCK_MINING.get(), new MiningModeData(mode, mode.getRadius(), 64, true));
    }

    static void swing(ServerPlayer player, BlockPos target, Direction face, Direction heading) {
        Vec3 stand = Vec3.atCenterOf(target.relative(face, 2));
        player.setPos(stand.x, stand.y - player.getEyeHeight(), stand.z);
        player.setYRot(heading.toYRot());
        player.gameMode.handleBlockBreakAction(target, ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                face, player.level().getMaxY(), 0);
        if (!player.getAbilities().instabuild) {
            for (int tick = 0; tick < 20; tick++) player.gameMode.tick();
            player.gameMode.handleBlockBreakAction(target, ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK,
                    face, player.level().getMaxY(), 1);
        }
    }

    static void set(ServerPlayer player, BlockPos pos, Block block) {
        player.level().setBlock(pos, block.defaultBlockState(), Block.UPDATE_CLIENTS);
    }

    static void fill(ServerPlayer player, BlockPos min, BlockPos max, Block block) {
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) set(player, pos, block);
    }

    static void check(GameTestHelper h, BlockPos pos, Block expected, String detail) {
        var state = h.getLevel().getBlockState(pos);
        if (!state.is(expected)) throw h.assertionException(detail + " at " + pos + ": expected " + expected + ", got " + state);
    }

    private static void stair(GameTestHelper h, BlockPos pos, Direction expected) {
        check(h, pos, Blocks.COBBLESTONE_STAIRS, "stair");
        var state = h.getLevel().getBlockState(pos);
        if (state.getValue(StairBlock.FACING) != expected || state.getValue(StairBlock.HALF) != Half.BOTTOM) {
            throw h.assertionException("Wrong stair at " + pos + ": expected bottom facing " + expected + ", got " + state);
        }
    }
}
