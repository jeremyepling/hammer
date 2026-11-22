package net.j40climb.florafauna_hammer.test;

import net.j40climb.florafauna_hammer.common.item.abilities.GroundSlam;
import net.j40climb.florafauna_hammer.common.item.hammer.HammerItem;
import net.j40climb.florafauna_hammer.common.item.hammer.HammerStage;
import net.j40climb.florafauna_hammer.setup.HammerRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
//? if >=26.3 {
/*import net.minecraft.world.entity.EntityTypes;*/
//?} else {
import net.minecraft.world.entity.EntityType;
//?}
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

final class HammerSlamLifecycleGameTests {
    private static final InteractionHand HAND = InteractionHand.MAIN_HAND;

    static void register(HammerGameTests.Registrar tests) {
        var mine = Identifier.fromNamespaceAndPath("florafauna", "test_hammer_mine");
        HammerGameTests.registerStructureTest(tests, "hammer_slam_lifecycle", mine, 100, HammerSlamLifecycleGameTests::lifecycle);
        HammerGameTests.registerStructureTest(tests, "hammer_slam_timeout", mine, 220, HammerSlamLifecycleGameTests::timeout);
        HammerGameTests.registerStructureTest(tests, "hammer_slam_players_and_falls", mine, 100, HammerSlamLifecycleGameTests::playersAndFalls);
    }

    private static ServerPlayer start(GameTestHelper h) {
        var player = HammerPowerGameTests.airborne(h);
        h.assertTrue(GroundSlam.activate(player, HAND), "Valid airborne Hammer failed to start the fixture");
        return player;
    }

    private static void assertCanceled(GameTestHelper h, ServerPlayer player, String reason) {
        TestPlatform.tick(player);
        h.assertTrue(HammerRegistry.pendingSlam(player) == null, "Slam survived " + reason);
    }

    private static void lifecycle(GameTestHelper h) {
        var dead = start(h);
        dead.setHealth(0);
        assertCanceled(h, dead, "death");

        var swimmer = start(h);
        swimmer.setSwimming(true);
        assertCanceled(h, swimmer, "entering an invalid movement state");

        var traveler = start(h);
        var destination = h.getLevel().getServer().getLevel(Level.NETHER);
        h.assertTrue(destination != null, "Missing destination dimension");
        traveler.setServerLevel(destination);
        assertCanceled(h, traveler, "dimension change");

        var leaving = start(h);
        h.getLevel().addNewPlayer(leaving);
        h.getLevel().removePlayerImmediately(leaving, Entity.RemovalReason.UNLOADED_WITH_PLAYER);
        h.assertTrue(HammerRegistry.pendingSlam(leaving) == null, "Disconnect retained a pending slam");

        var spectator = HammerPowerGameTests.airborne(h);
        spectator.setGameMode(GameType.SPECTATOR);
        h.assertTrue(spectator.isSpectator() && !GroundSlam.activate(spectator, HAND), "Spectator could slam");

        var climber = HammerPowerGameTests.airborne(h);
        var ladder = climber.blockPosition();
        h.getLevel().setBlockAndUpdate(ladder, Blocks.SCAFFOLDING.defaultBlockState());
        h.assertTrue(climber.onClimbable() && !GroundSlam.activate(climber, HAND), "Climber could slam");
        h.getLevel().setBlockAndUpdate(ladder, Blocks.AIR.defaultBlockState());

        var rider = h.makeMockServerPlayerInLevel();
        try {
            rider.setItemInHand(HAND, climber.getMainHandItem().copy());
            rider.getAbilities().flying = false;
            rider.setPos(climber.position());
            rider.setOnGround(false);
            //? if >=26.3 {
            /*var pig = h.spawn(EntityTypes.PIG, new BlockPos(8, 11, 8));*/
            //?} else {
            var pig = h.spawn(EntityType.PIG, new BlockPos(8, 11, 8));
            //?}
            h.assertTrue(rider.startRiding(pig, true, false) && rider.isPassenger(), "Failed to mount the rider");
            h.assertTrue(!GroundSlam.activate(rider, HAND) && HammerItem.charge(rider.getMainHandItem()) == HammerStage.TEMPEST.capacity(),
                    "Riding consumed Hammer charge");
            rider.stopRiding();
        } finally {
            h.getLevel().getServer().getPlayerList().remove(rider);
        }
        h.succeed();
    }

    private static void timeout(GameTestHelper h) {
        var stalled = start(h);
        h.runAfterDelay(201, () -> {
            assertCanceled(h, stalled, "a stalled descent");
            h.succeed();
        });
    }

    private static void playersAndFalls(GameTestHelper h) {
        var attacker = start(h);
        var shortFall = TestPlatform.fallDistance(attacker, 1.25);
        var longFall = TestPlatform.fallDistance(attacker, 40);
        h.assertTrue(shortFall == 0 && longFall > 30, "Fall cushion lost short/high fall distinction");
        var witness = HammerPowerGameTests.airborne(h);
        //? if >=26.3 {
        /*witness.setPermanentlyInvulnerable(false);*/
        //?} else {
        witness.setInvulnerable(false);
        //?}
        var floor = h.absolutePos(new BlockPos(8, 8, 8));
        witness.setPos(Vec3.atBottomCenterOf(floor.above().east()));
        h.getLevel().addNewPlayer(witness);
        try {
            h.assertTrue(h.getLevel().getEntitiesOfClass(ServerPlayer.class, witness.getBoundingBox()).contains(witness),
                    "Second player must be present in the impact area");
            //? if >=26.3 {
            /*var hostile = h.spawn(EntityTypes.HUSK, new BlockPos(8, 9, 7));*/
            //?} else {
            var hostile = h.spawn(EntityType.HUSK, new BlockPos(8, 9, 7));
            //?}
            float enemyHealth = hostile.getHealth(), witnessHealth = witness.getHealth();
            var witnessMotion = witness.getDeltaMovement();
            attacker.setPos(Vec3.atBottomCenterOf(floor.above()));
            attacker.setOnGround(true);
            TestPlatform.tick(attacker);
            h.assertTrue(hostile.getHealth() < enemyHealth && witness.getHealth() == witnessHealth
                    && witnessMotion.equals(witness.getDeltaMovement()), "Impact damaged or moved the neighboring player");
        } finally {
            h.getLevel().removePlayerImmediately(witness, Entity.RemovalReason.UNLOADED_WITH_PLAYER);
        }
        h.succeed();
    }
}
