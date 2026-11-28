package net.j40climb.florafauna_hammer.test;

import net.j40climb.florafauna_hammer.common.item.abilities.GroundSlam;
import net.j40climb.florafauna_hammer.common.item.abilities.SeismicScanner;
import net.j40climb.florafauna_hammer.common.item.abilities.data.HammerAction;
import net.j40climb.florafauna_hammer.common.item.abilities.data.MiningShape;
import net.j40climb.florafauna_hammer.common.item.abilities.data.MiningSpeed;
import net.j40climb.florafauna_hammer.common.item.abilities.data.ToolConfig;
import net.j40climb.florafauna_hammer.common.item.abilities.networking.ChargedThrowPayload;
import net.j40climb.florafauna_hammer.common.item.abilities.networking.CycleMiningModePayload;
import net.j40climb.florafauna_hammer.common.item.abilities.networking.UpdateToolConfigPayload;
import net.j40climb.florafauna_hammer.common.item.hammer.HammerItem;
import net.j40climb.florafauna_hammer.common.item.hammer.HammerStage;
import net.j40climb.florafauna_hammer.setup.HammerRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

import java.util.EnumSet;

final class HammerProgressionGameTests {
    private static final InteractionHand HAND = InteractionHand.MAIN_HAND;

    static void register(HammerGameTests.Registrar tests) {
        HammerGameTests.registerTest(tests, "hammer_progression_recipes", HammerProgressionGameTests::recipes);
        HammerGameTests.registerTest(tests, "hammer_progression_locks", HammerProgressionGameTests::locks);
        HammerGameTests.registerTest(tests, "hammer_progression_charge_meter", HammerProgressionGameTests::chargeMeter);
        HammerGameTests.registerTest(tests, "hammer_progression_forged_throw", HammerProgressionGameTests::forgedThrow);
    }

    private static void recipes(GameTestHelper h) {
        var p = h.makeMockServerPlayerInLevel();
        try {
            p.setGameMode(GameType.SURVIVAL);
            p.getInventory().clearContent();
            HammerSurvivalFixture.forge(p);
            HammerSurvivalFixture.reject(p, HammerRegistry.TEMPEST_CORE.get());
            HammerSurvivalFixture.upgrade(p, HammerStage.RESONANT);
            HammerSurvivalFixture.reject(p, HammerRegistry.RESONANCE_CORE.get());
            p.getMainHandItem().set(HammerRegistry.HAMMER_CHARGE.get(), 23);
            HammerSurvivalFixture.upgrade(p, HammerStage.TEMPEST);
            HammerSurvivalFixture.reject(p, HammerRegistry.RESONANCE_CORE.get());
            HammerSurvivalFixture.reject(p, HammerRegistry.TEMPEST_CORE.get());
        } finally {
            h.getLevel().getServer().getPlayerList().remove(p);
        }
        h.succeed();
    }

    private static void locks(GameTestHelper h) {
        var p = HammerMiningGameTests.player(h, MiningShape.SINGLE);
        p.setGameMode(GameType.SURVIVAL);
        for (HammerStage stage : HammerStage.values()) {
            var hammer = stage.createStack();
            p.setItemInHand(HAND, hammer);
            var allowed = EnumSet.allOf(MiningShape.class);
            if (stage == HammerStage.FORGED) allowed.removeAll(EnumSet.of(
                    MiningShape.FLAT_5X5, MiningShape.FLAT_7X7, MiningShape.SHAPELESS, MiningShape.LARGE_TUNNEL));
            var visited = EnumSet.noneOf(MiningShape.class);
            for (int i = 0; i < allowed.size(); i++) {
                CycleMiningModePayload.cycleMiningMode(p, hammer);
                visited.add(hammer.get(HammerRegistry.MULTI_BLOCK_MINING.get()).shape());
            }
            h.assertTrue(visited.equals(allowed), "Unexpected " + stage + " cycle " + visited);
            int expectedCapacity = switch (stage) { case FORGED -> 0; case RESONANT -> 64; case TEMPEST -> 96; };
            hammer.set(HammerRegistry.HAMMER_CHARGE.get(), 1000);
            p.tickCount = 20;
            hammer.getItem().inventoryTick(hammer, p.level(), p, EquipmentSlot.MAINHAND);
            h.assertTrue(HammerItem.charge(hammer) == expectedCapacity, "Overcharged " + stage);
            if (expectedCapacity > 0) {
                hammer.set(HammerRegistry.HAMMER_CHARGE.get(), expectedCapacity - 1);
                p.tickCount = 40;
                hammer.getItem().inventoryTick(hammer, p.level(), p, EquipmentSlot.MAINHAND);
                p.tickCount = 60;
                hammer.getItem().inventoryTick(hammer, p.level(), p, EquipmentSlot.MAINHAND);
                h.assertTrue(HammerItem.charge(hammer) == expectedCapacity, "Incorrect " + stage + " recharge cap");
            }
            UpdateToolConfigPayload.applyToHeldItem(p, ToolConfig.DEFAULT.withMiningSpeed(MiningSpeed.INSTABREAK));
            h.assertTrue((hammer.get(HammerRegistry.TOOL_CONFIG.get()).miningSpeed() == MiningSpeed.INSTABREAK) == (stage == HammerStage.TEMPEST),
                    "Instabreak tier bypass on " + stage);
            h.assertTrue(HammerAction.SCAN.availableOn(hammer) == (stage != HammerStage.FORGED)
                    && HammerAction.LIGHTNING.availableOn(hammer) == (stage == HammerStage.TEMPEST)
                    && HammerAction.TELEPORT.availableOn(hammer) == (stage == HammerStage.TEMPEST), "Wrong action availability on " + stage);
            if (stage == HammerStage.FORGED) {
                p.setOnGround(true);
                h.assertTrue(!SeismicScanner.activate(p, HAND) && HammerItem.charge(hammer) == 0, "Forged activated Scan");
            }
            if (stage != HammerStage.TEMPEST) {
                p.setOnGround(false);
                h.assertTrue(!GroundSlam.activate(p, HAND) && HammerRegistry.pendingSlam(p) == null
                        && HammerItem.charge(hammer) == expectedCapacity, "Locked slam activated or spent charge on " + stage);
            }
        }
        h.succeed();
    }

    private static void chargeMeter(GameTestHelper h) {
        var forged = HammerStage.FORGED.createStack();
        h.assertTrue(!forged.getItem().isBarVisible(forged) && forged.getItem().getBarWidth(forged) == 0,
                "The Forged Hammer has no charge meter");
        for (var stage : EnumSet.of(HammerStage.RESONANT, HammerStage.TEMPEST)) {
            var hammer = stage.createStack();
            var item = hammer.getItem();
            h.assertTrue(item.isBarVisible(hammer) && item.getBarWidth(hammer) == 13,
                    "A fully charged Hammer must show all 13 pixels");
            hammer.set(HammerRegistry.HAMMER_CHARGE.get(), stage.capacity() / 2);
            h.assertTrue(item.isBarVisible(hammer) && item.getBarWidth(hammer) == 7,
                    "A half-charged Hammer must show a half-full meter");
            hammer.set(HammerRegistry.HAMMER_CHARGE.get(), 1);
            h.assertTrue(item.isBarVisible(hammer) && item.getBarWidth(hammer) == 1,
                    "A Hammer with one charge must show at least one pixel");
            hammer.set(HammerRegistry.HAMMER_CHARGE.get(), 0);
            h.assertTrue(item.isBarVisible(hammer) && item.getBarWidth(hammer) == 0,
                    "An empty powered Hammer must still show the empty meter");
        }
        h.succeed();
    }

    private static void forgedThrow(GameTestHelper h) {
        var p = HammerMiningGameTests.player(h, MiningShape.SINGLE);
        p.setGameMode(GameType.SURVIVAL);
        p.setItemInHand(HAND, HammerStage.FORGED.createStack());
        var ore = h.absolutePos(new BlockPos(0, 3, 0));
        p.level().setBlockAndUpdate(ore, Blocks.DIAMOND_ORE.defaultBlockState());
        p.setPos(Vec3.atCenterOf(ore.north(4)));
        ChargedThrowPayload.begin(p, InteractionHand.MAIN_HAND);
        h.runAfterDelay(ChargedThrowPayload.HOLD_TICKS + 1, () -> {
            ChargedThrowPayload.key(p, ChargedThrowPayload.RELEASE);
            var entities = p.level().getEntities(HammerRegistry.THROWN_ITEM.get(), e -> e.getOwner() == p);
            h.assertTrue(entities.size() == 1 && p.getMainHandItem().isEmpty(), "Long T did not transfer exactly one ordinary Forged Hammer");
            var thrown = entities.getFirst();
            thrown.setPos(Vec3.atCenterOf(ore.north(2)));
            thrown.setDeltaMovement(0, 0, 2.5);
            thrown.tick();
            h.assertTrue(thrown.isReturning() && p.level().getBlockState(ore).is(Blocks.DIAMOND_ORE)
                    && HammerItem.charge(thrown.getThrownItem()) == 0, "Forged long throw mined ore or spent charge");
            thrown.discard();
            h.succeed();
        });
    }
}
