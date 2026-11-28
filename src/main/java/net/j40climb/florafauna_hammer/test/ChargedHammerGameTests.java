package net.j40climb.florafauna_hammer.test;

import net.j40climb.florafauna_hammer.HammerMod;
import net.j40climb.florafauna_hammer.common.item.abilities.data.MiningModeData;
import net.j40climb.florafauna_hammer.common.item.abilities.data.MiningShape;
import net.j40climb.florafauna_hammer.common.item.abilities.data.MiningSpeed;
import net.j40climb.florafauna_hammer.common.item.abilities.data.ToolConfig;
import net.j40climb.florafauna_hammer.common.item.abilities.multiblock.RemoteOreStrike;
import net.j40climb.florafauna_hammer.common.item.abilities.networking.ChargedThrowPayload;
import net.j40climb.florafauna_hammer.common.item.abilities.networking.UpdateToolConfigPayload;
import net.j40climb.florafauna_hammer.common.item.hammer.HammerItem;
import net.j40climb.florafauna_hammer.common.item.hammer.HammerStage;
import net.j40climb.florafauna_hammer.setup.HammerRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.phys.Vec3;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;


final class ChargedHammerGameTests {
    static void register(HammerGameTests.Registrar tests) {
        var mine = Identifier.fromNamespaceAndPath(HammerMod.MOD_ID, "test_hammer_mine");
        HammerGameTests.registerStructureTest(tests, "hammer_charged_ore_rules", mine, 100, ChargedHammerGameTests::oreRules);
        HammerGameTests.registerStructureTest(tests, "hammer_charged_first_surface", mine, 100, ChargedHammerGameTests::firstSurface);
        HammerGameTests.registerStructureTest(tests, "hammer_charged_permissions", mine, 100, ChargedHammerGameTests::permissions);
        HammerGameTests.registerStructureTest(tests, "hammer_charged_survival_reload", mine, 100, h -> flight(h, false));
        HammerGameTests.registerStructureTest(tests, "hammer_charged_creative_reload", mine, 100, h -> flight(h, true));
        HammerGameTests.registerStructureTest(tests, "hammer_charged_tap_and_recharge", mine, 100, ChargedHammerGameTests::tap);
    }

    private static ServerPlayer player(GameTestHelper h, boolean creative) {
        var p = TestPlatform.player(h.getLevel(), "charge-test");
        p.setGameMode(creative ? GameType.CREATIVE : GameType.SURVIVAL);
        p.setItemInHand(InteractionHand.MAIN_HAND, HammerStage.RESONANT.createStack());
        return p;
    }

    private static void rock(ServerPlayer p, BlockPos pos, Block block) {
        p.level().setBlock(pos, block.defaultBlockState(), Block.UPDATE_CLIENTS);
    }

    private static void oreRules(GameTestHelper h) {
        for (boolean exact : new boolean[]{false, true}) {
            var p = player(h, false);
            var hammer = p.getMainHandItem();
            UpdateToolConfigPayload.applyToHeldItem(p, new ToolConfig(false, true, MiningSpeed.STANDARD, exact));
            hammer.set(HammerRegistry.MULTI_BLOCK_MINING.get(), new MiningModeData(MiningShape.SINGLE, 0, 2, true));
            var selected = new ItemStack(Items.STICK, 5);
            p.setItemInHand(InteractionHand.MAIN_HAND, selected);
            var center = h.absolutePos(new BlockPos(4, 6, exact ? 14 : 4));
            rock(p, center, Blocks.DIAMOND_ORE);
            rock(p, center.east(), Blocks.DEEPSLATE_DIAMOND_ORE);
            rock(p, center.east(2), Blocks.DIAMOND_ORE);
            rock(p, center.north(), Blocks.IRON_ORE);
            RemoteOreStrike.hit(p, hammer, center);
            int count = exact ? 1 : 2;
            h.assertTrue(HammerItem.charge(hammer) == HammerStage.RESONANT.capacity() - count, "Only successful blocks should cost charge");
            h.assertTrue(p.getMainHandItem() == selected, "Hotbar stack was replaced");
            h.assertTrue(p.level().isEmptyBlock(center) && p.level().isEmptyBlock(center.east()) != exact
                    && p.level().getBlockState(center.east(2)).is(Blocks.DIAMOND_ORE)
                    && p.level().getBlockState(center.north()).is(Blocks.IRON_ORE), "Wrong remote vein or cap");
            var drops = p.level().getEntitiesOfClass(ItemEntity.class, new AABB(center).inflate(3));
            int silk = drops.stream().filter(e -> e.getItem().is(Items.DIAMOND_ORE) || e.getItem().is(Items.DEEPSLATE_DIAMOND_ORE))
                    .mapToInt(e -> e.getItem().getCount()).sum();
            h.assertTrue(silk == count, "Expected " + count + " silk ore blocks, got " + drops.stream().map(ItemEntity::getItem).toList());
        }
        h.succeed();
    }

    private static void permissions(GameTestHelper h) {
        var p = player(h, false);
        var hammer = p.getMainHandItem();
        hammer.set(HammerRegistry.HAMMER_CHARGE.get(), 1);
        var center = h.absolutePos(new BlockPos(6, 8, 8));
        rock(p, center, Blocks.DIAMOND_ORE);
        rock(p, center.east(), Blocks.DIAMOND_ORE);
        rock(p, center.west(), Blocks.DIAMOND_ORE);
        var held = new ItemStack(Items.STONE);
        p.setItemInHand(InteractionHand.MAIN_HAND, held);
        Runnable releaseCenter = TestPlatform.vetoBreak(p, position -> {
            h.assertTrue(p.getMainHandItem() == hammer, "Break hook saw the wrong tool");
            return true;
        });
        try {
            RemoteOreStrike.hit(p, hammer, center);
        } finally {
            releaseCenter.run();
        }
        h.assertTrue(p.getMainHandItem() == held && HammerItem.charge(hammer) == 1 && !p.level().isEmptyBlock(center)
                && !p.level().isEmptyBlock(center.east()) && !p.level().isEmptyBlock(center.west()), "Denied center should do nothing");
        // A denied neighbor must leave charge available for another target.
        hammer.set(HammerRegistry.HAMMER_CHARGE.get(), 2);
        rock(p, center.east(2), Blocks.DIAMOND_ORE);
        Runnable releaseNeighbor = TestPlatform.vetoBreak(p, center.west()::equals);
        try {
            RemoteOreStrike.hit(p, hammer, center);
        } finally {
            releaseNeighbor.run();
        }
        h.assertTrue(HammerItem.charge(hammer) == 0 && p.level().isEmptyBlock(center) && p.level().isEmptyBlock(center.east())
                && !p.level().isEmptyBlock(center.west()) && !p.level().isEmptyBlock(center.east(2)), "Protected block or charge limit ignored");
        RemoteOreStrike.hit(p, hammer, center.west());
        h.assertTrue(!p.level().isEmptyBlock(center.west()), "An empty Hammer mined ore");
        h.succeed();
    }

    private static void flight(GameTestHelper h, boolean creative) {
        var p = player(h, creative);
        var level = h.getLevel();
        var center = h.absolutePos(new BlockPos(8, 12, 8));
        rock(p, center, Blocks.DIAMOND_ORE);
        rock(p, center.east(), Blocks.DIAMOND_ORE);
        p.setPos(Vec3.atCenterOf(center.north(4)));
        p.setYRot(0);
        p.setXRot(0);
        var hammer = p.getMainHandItem();
        hammer.set(DataComponents.CUSTOM_NAME, Component.literal("Returning Vein Hammer"));
        UpdateToolConfigPayload.applyToHeldItem(p, new ToolConfig(false, true, MiningSpeed.STANDARD));
        var expected = hammer.copy();
        expected.set(HammerRegistry.HAMMER_CHARGE.get(), HammerStage.RESONANT.capacity() - 2);
        var catchArea = p.getBoundingBox().inflate(3);
        var flightArea = catchArea.minmax(new AABB(center).inflate(2));
        ChargedThrowPayload.key(p, ChargedThrowPayload.PRESS);
        h.runAfterDelay(ChargedThrowPayload.HOLD_TICKS + 1, () -> {
            ChargedThrowPayload.key(p, ChargedThrowPayload.RELEASE);
            ChargedThrowPayload.key(p, ChargedThrowPayload.RELEASE);
            var entities = level.getEntities(HammerRegistry.THROWN_ITEM.get(), e -> e.getOwner() == p);
            h.assertTrue(entities.size() == 1 && p.getMainHandItem().isEmpty(), "The charged throw must transfer exactly one Hammer");
            var projectile = entities.getFirst();
            var save = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, p.level().registryAccess());
            projectile.saveWithoutId(save);
            projectile.discard();
            var restored = HammerRegistry.THROWN_ITEM.get().create(p.level(), EntitySpawnReason.LOAD);
            restored.load(TagValueInput.create(ProblemReporter.DISCARDING, p.level().registryAccess(), save.buildResult()));
            restored.setOwner(p);
            restored.setPos(Vec3.atCenterOf(center.north(2)));
            restored.setDeltaMovement(0, 0, 2.5);
            var held = new ItemStack(Items.STICK, 4);
            p.setItemInHand(InteractionHand.MAIN_HAND, held);
            restored.tick();
            h.assertTrue(level.isEmptyBlock(center) && level.isEmptyBlock(center.east()), "Reloaded charged throw did not strike the vein");
            h.assertTrue(restored.isReturning() && ItemStack.matches(expected, restored.getThrownItem()),
                    "Mined projectile lost its Hammer components or the two-block debit");
            h.assertTrue(level.getEntitiesOfClass(ItemEntity.class, flightArea).isEmpty(),
                    "Charged impact spawned ground items before the catch");
            h.assertTrue(p.getInventory().countItem(Items.DIAMOND_ORE) == 0, "Charged impact delivered ore before the catch");

            var minedSave = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
            restored.saveWithoutId(minedSave);
            restored.discard();
            var returning = HammerRegistry.THROWN_ITEM.get().create(level, EntitySpawnReason.LOAD);
            returning.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), minedSave.buildResult()));
            returning.setOwner(p);
            h.assertTrue(returning.isReturning() && ItemStack.matches(expected, returning.getThrownItem()),
                    "Reload after mining lost the return state or the debited Hammer");

            var inventory = p.getInventory();
            if (!creative) {
                // Leave space for one Hammer and one more ore in the existing stack.
                int hammerSlot = 5;
                int oreSlot = 6;
                for (int slot = 0; slot < inventory.getNonEquipmentItems().size(); slot++) {
                    if (slot != inventory.getSelectedSlot() && slot != hammerSlot && slot != oreSlot) {
                        inventory.setItem(slot, new ItemStack(Items.STONE, 64));
                    }
                }
                inventory.setItem(oreSlot, new ItemStack(Items.DIAMOND_ORE, 63));
                //? if neoforge {
                int vetoes = TestPlatform.withRejectedRecoveryDrop(level, drop -> drop.getItem().is(Items.DIAMOND_ORE), () -> {
                    for (int tick = 0; tick < 5 && inventory.countItem(Items.DIAMOND_ORE) == 63 && !returning.isRemoved(); tick++) {
                        returning.tick();
                    }
                });
                h.assertTrue(vetoes == 1 && !returning.isRemoved()
                                && ItemStack.matches(expected, inventory.getItem(hammerSlot))
                                && inventory.countItem(Items.DIAMOND_ORE) == 64
                                && level.getEntitiesOfClass(ItemEntity.class, flightArea).isEmpty(),
                        "A vetoed overflow drop must leave only the rejected remainder in flight");
                //?}
            }
            for (int tick = 0; tick < 40 && !returning.isRemoved(); tick++) returning.tick();
            h.assertTrue(returning.isRemoved() && p.getMainHandItem() == held && held.getCount() == 4,
                    "The catch or overflow retry failed or displaced the selected stack");
            h.assertTrue(inventory.countItem(expected.getItem()) == 1
                            && inventory.getNonEquipmentItems().stream().anyMatch(stack -> ItemStack.matches(expected, stack)),
                    "Expected exactly one returned Hammer with the same name, charge and components");
            var drops = level.getEntitiesOfClass(ItemEntity.class, flightArea);
            if (creative) {
                h.assertTrue(inventory.countItem(Items.DIAMOND_ORE) == 0 && drops.isEmpty(),
                        "Creative mining should return the Hammer without creating ore");
            } else {
                h.assertTrue(inventory.countItem(Items.DIAMOND_ORE) == 64 && drops.size() == 1
                                && ItemStack.matches(new ItemStack(Items.DIAMOND_ORE), drops.getFirst().getItem())
                                && catchArea.intersects(drops.getFirst().getBoundingBox()),
                        "Catch must insert one ore and leave exactly one overflow ore at the owner");
            }
            h.succeed();
        });
    }

    private static void firstSurface(GameTestHelper h) {
        var p = player(h, false);
        var center = h.absolutePos(new BlockPos(8, 12, 8));
        rock(p, center, Blocks.STONE);
        rock(p, center.south(), Blocks.DIAMOND_ORE);
        p.setPos(Vec3.atCenterOf(center.north(4)));
        var original = p.getMainHandItem();
        ChargedThrowPayload.key(p, ChargedThrowPayload.PRESS);
        h.runAfterDelay(ChargedThrowPayload.HOLD_TICKS + 1, () -> {
            // NeoForge's spawn veto is a separate loader-specific regression.
            //? if neoforge {
            TestPlatform.withRejectedThrow(p, () -> ChargedThrowPayload.key(p, ChargedThrowPayload.RELEASE));
            h.assertTrue(p.getMainHandItem() == original && original.getCount() == 1 && HammerItem.charge(original) == 64,
                    "Rejected charged projectile consumed the Hammer or its charge");
            //?}
            ChargedThrowPayload.key(p, ChargedThrowPayload.PRESS);
            h.runAfterDelay(ChargedThrowPayload.HOLD_TICKS + 1, () -> {
                ChargedThrowPayload.key(p, ChargedThrowPayload.RELEASE);
                var thrown = p.level().getEntities(HammerRegistry.THROWN_ITEM.get(), e -> e.getOwner() == p).getFirst();
                thrown.setPos(Vec3.atCenterOf(center.north(2)));
                thrown.setDeltaMovement(0, 0, 3.5);
                thrown.tick();
                h.assertTrue(thrown.isReturning() && HammerItem.charge(thrown.getThrownItem()) == 64
                        && p.level().getBlockState(center).is(Blocks.STONE) && p.level().getBlockState(center.south()).is(Blocks.DIAMOND_ORE),
                        "First non-ore surface must return the Hammer without spending charge");
                h.succeed();
            });
        });
    }

    private static void tap(GameTestHelper h) {
        var p = player(h, false);
        var center = h.absolutePos(new BlockPos(8, 12, 8));
        rock(p, center, Blocks.DIAMOND_ORE);
        p.setPos(Vec3.atCenterOf(center.north(4)));
        var hammer = p.getMainHandItem();
        hammer.set(HammerRegistry.HAMMER_CHARGE.get(), 40);
        for (int tick = 1; tick <= 20; tick++) {
            p.tickCount = tick;
            hammer.getItem().inventoryTick(hammer, p.level(), p, EquipmentSlot.MAINHAND);
            hammer.getItem().inventoryTick(hammer, p.level(), p, null);
        }
        h.assertTrue(HammerItem.charge(hammer) == 41, "Held recharge must add one per second");
        ChargedThrowPayload.key(p, ChargedThrowPayload.PRESS);
        p.setItemInHand(InteractionHand.MAIN_HAND, HammerStage.RESONANT.createStack());
        ChargedThrowPayload.key(p, ChargedThrowPayload.RELEASE);
        h.assertTrue(!p.getMainHandItem().isEmpty(), "Switching Hammers must cancel the windup");
        p.setItemInHand(InteractionHand.MAIN_HAND, hammer);
        ChargedThrowPayload.key(p, ChargedThrowPayload.PRESS);
        ChargedThrowPayload.key(p, ChargedThrowPayload.RELEASE);
        var thrown = p.level().getEntities(HammerRegistry.THROWN_ITEM.get(), e -> e.getOwner() == p).getFirst();
        thrown.setPos(Vec3.atCenterOf(center.north(2)));
        thrown.setDeltaMovement(0, 0, 2.5);
        thrown.tick();
        h.assertTrue(!p.level().isEmptyBlock(center) && HammerItem.charge(thrown.getThrownItem()) == 41, "A tap must be a normal throw");
        h.succeed();
    }
}
