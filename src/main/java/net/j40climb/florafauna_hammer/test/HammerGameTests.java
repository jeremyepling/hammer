package net.j40climb.florafauna_hammer.test;

import net.j40climb.florafauna_hammer.HammerMod;
import net.j40climb.florafauna_hammer.common.item.abilities.data.MiningModeData;
import net.j40climb.florafauna_hammer.common.item.abilities.data.MiningShape;
import net.j40climb.florafauna_hammer.common.item.abilities.SeismicTerrainGameTests;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;

import java.util.function.Consumer;

public final class HammerGameTests {
    private static final Identifier EMPTY_STRUCTURE = Identifier.withDefaultNamespace("empty");

    @FunctionalInterface
    interface Registrar {
        void register(Identifier id, Identifier structure, int maxTicks, int setupTicks, Consumer<GameTestHelper> run);
    }

    static void registerTests(Registrar tests) {
        registerMiningModeTests(tests);
        HammerProgressionGameTests.register(tests);
        HammerAbilityGameTests.register(tests);
        HammerPowerGameTests.register(tests);
        HammerSlamLifecycleGameTests.register(tests);
        registerTest(tests, "hammer_seismic_loaded_terrain", SeismicTerrainGameTests::checkLoadedTerrain);
        ChargedHammerGameTests.register(tests);
        HammerMiningGameTests.register(tests);
    }

    static void registerStructureTest(
            Registrar tests,
            String name,
            Identifier structure,
            int maxTicks,
            Consumer<GameTestHelper> testFunction
    ) {
        Identifier testId = Identifier.fromNamespaceAndPath(HammerMod.MOD_ID, name);

        tests.register(testId, structure, maxTicks, 20, testFunction);
    }

    /**
     * Register a test with the default empty structure and settings.
     */
    static void registerTest(
            Registrar tests,
            String name,
            Consumer<GameTestHelper> testFunction
    ) {
        Identifier testId = Identifier.fromNamespaceAndPath(HammerMod.MOD_ID, name);

        tests.register(testId, EMPTY_STRUCTURE, 100, 0, testFunction);
    }

    private static void registerMiningModeTests(Registrar tests) {
        registerTest(tests, "mining_mode_initial_state", HammerGameTests::testMiningModeInitialState);
        registerTest(tests, "mining_mode_cycling", HammerGameTests::testMiningModeCycling);
        registerTest(tests, "mining_mode_wrap_around", HammerGameTests::testMiningModeWrapAround);
        registerTest(tests, "mining_mode_radius_per_shape", HammerGameTests::testMiningModeRadiusPerShape);
    }

    private static void testMiningModeInitialState(GameTestHelper helper) {
        MiningModeData data = MiningModeData.DEFAULT;

        if (data.shape() != MiningShape.SINGLE) {
            throw helper.assertionException("Default shape should be SINGLE, got: " + data.shape());
        }
        if (data.radius() != 0) {
            throw helper.assertionException("Default radius should be 0, got: " + data.radius());
        }
        if (data.maxBlocksToBreak() != 64) {
            throw helper.assertionException("Default maxBlocksToBreak should be 64, got: " + data.maxBlocksToBreak());
        }

        helper.succeed();
    }

    private static void testMiningModeCycling(GameTestHelper helper) {
        MiningModeData data = MiningModeData.DEFAULT;

        // Existing shapes keep their order; new patterns follow the original staircase modes.
        MiningShape[] expectedOrder = {
            MiningShape.FLAT_3X3,
            MiningShape.FLAT_5X5,
            MiningShape.FLAT_7X7,
            MiningShape.SHAPELESS,
            MiningShape.TUNNEL_UP,
            MiningShape.TUNNEL_DOWN,
            MiningShape.WALKABLE_TUNNEL,
            MiningShape.LARGE_TUNNEL,
            MiningShape.BORE
        };

        for (MiningShape expected : expectedOrder) {
            data = data.getNextMode();
            if (data.shape() != expected) {
                throw helper.assertionException("Expected " + expected + ", got: " + data.shape());
            }
        }

        helper.succeed();
    }

    private static void testMiningModeWrapAround(GameTestHelper helper) {
        // Start from the last configured shape.
        MiningModeData data = new MiningModeData(MiningShape.BORE, 0, 64, true);

        // Next mode should wrap to SINGLE
        data = data.getNextMode();

        if (data.shape() != MiningShape.SINGLE) {
            throw helper.assertionException("After BORE should wrap to SINGLE, got: " + data.shape());
        }

        helper.succeed();
    }

    private static void testMiningModeRadiusPerShape(GameTestHelper helper) {
        // Only flat and shapeless mining use a configurable radius.
        int[] expectedRadii = {0, 1, 2, 3, 1, 0, 0, 0, 0, 0};
        MiningShape[] shapes = MiningShape.values();

        for (int i = 0; i < shapes.length; i++) {
            int actualRadius = shapes[i].getRadius();
            if (actualRadius != expectedRadii[i]) {
                throw helper.assertionException(shapes[i] + " should have radius " + expectedRadii[i] + ", got: " + actualRadius);
            }
        }

        helper.succeed();
    }
}
