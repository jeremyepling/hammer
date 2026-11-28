package net.j40climb.florafauna_hammer.test;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.j40climb.florafauna_hammer.HammerMod;
import net.j40climb.florafauna_hammer.common.item.abilities.data.HammerAction;
import net.j40climb.florafauna_hammer.common.item.abilities.data.MiningModeData;
import net.j40climb.florafauna_hammer.common.item.abilities.data.MiningShape;
import net.j40climb.florafauna_hammer.common.item.abilities.data.MiningSpeed;
import net.j40climb.florafauna_hammer.common.item.abilities.data.ThrowableAbilityData;
import net.j40climb.florafauna_hammer.common.item.abilities.data.ToolConfig;
import net.j40climb.florafauna_hammer.common.item.abilities.networking.ThrowItemPayload;
import net.j40climb.florafauna_hammer.common.item.abilities.networking.UpdateToolConfigPayload;
import net.j40climb.florafauna_hammer.common.item.projectile.ThrownItemEntity;
import net.j40climb.florafauna_hammer.common.item.hammer.HammerStage;
import net.j40climb.florafauna_hammer.setup.HammerRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;


final class HammerAbilityGameTests {
    static void register(HammerGameTests.Registrar tests) {
        HammerGameTests.registerTest(tests, "hammer_namespaced_saved_stack", HammerAbilityGameTests::namespacedSavedStack);
        HammerGameTests.registerTest(tests, "hammer_fresh_and_configured_loot", HammerAbilityGameTests::checkConfigurations);
        HammerGameTests.registerTest(tests, "hammer_block_action_dispatch", HammerAbilityGameTests::checkBlockActions);
        HammerGameTests.registerTest(tests, "hammer_throw_single_item_and_reload", HammerAbilityGameTests::checkThrow);
        HammerGameTests.registerTest(tests, "hammer_throw_straight_flight", HammerAbilityGameTests::straightFlight);
        var mine = Identifier.fromNamespaceAndPath(HammerMod.MOD_ID, "test_hammer_mine");
        HammerGameTests.registerStructureTest(tests, "hammer_throw_impact_protected", mine, 100, h -> checkThrowImpact(h, true));
        HammerGameTests.registerStructureTest(tests, "hammer_throw_impact_silk_touch", mine, 100, h -> checkThrowImpact(h, false));
        HammerGameTests.registerTest(tests, "hammer_throw_rejected_or_inventory_full", HammerAbilityGameTests::checkThrowFailure);
        //? if neoforge {
        HammerGameTests.registerTest(tests, "hammer_throw_recovery_toss_veto", HammerAbilityGameTests::checkRecoveryTossVeto);
        //?}
        HammerGameTests.registerTest(tests, "hammer_throw_recovery_cross_dimension", HammerAbilityGameTests::checkRecoveryCrossDimension);
    }

    private static ServerPlayer player(GameTestHelper helper) {
        ServerPlayer player = TestPlatform.player(helper.getLevel(), "hammer-test");
        player.setGameMode(GameType.SURVIVAL);
        player.setPos(Vec3.atCenterOf(helper.absolutePos(new BlockPos(1, 2, 1))));
        player.setOnGround(true);
        return player;
    }

    private static void namespacedSavedStack(GameTestHelper helper) {
        var saved = JsonParser.parseString("""
                {"id":"hammer:hammer", "count":1, "components":{
                  "hammer:hammer_charge":17, "hammer:hammer_action":"strip",
                  "hammer:multi_block_mining":{"shape":"stairs_down","radius":0,"maxBlocksToBreak":24},
                  "hammer:tool_config":{"fortune":false,"silkTouch":true,"miningSpeed":"standard"}
                }}
                """);
        var ops = helper.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE);
        ItemStack stack = ItemStack.CODEC.parse(ops, saved).getOrThrow();
        helper.assertTrue(stack.is(HammerRegistry.HAMMER.get()) && stack.get(HammerRegistry.HAMMER_CHARGE.get()) == 17
                && stack.get(HammerRegistry.HAMMER_ACTION.get()) == HammerAction.STRIP, "Saved Hammer lost its identity, charge or action");
        var mode = stack.get(HammerRegistry.MULTI_BLOCK_MINING.get());
        helper.assertTrue(mode.shape() == MiningShape.TUNNEL_DOWN && mode.maxBlocksToBreak() == 24
                && stack.get(HammerRegistry.TOOL_CONFIG.get()).equals(new ToolConfig(false, true, MiningSpeed.STANDARD)),
                "Saved mining settings did not decode with their original defaults");
        helper.assertTrue(stack.is(ItemTags.AXES) && stack.is(ItemTags.HOES) && stack.is(ItemTags.SHOVELS)
                && stack.is(ItemTags.PICKAXES), "The installed Hammer must belong to all four tool tags");
        helper.succeed();
    }

    private static void checkBlockActions(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        ItemStack hammer = new ItemStack(HammerRegistry.HAMMER.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, hammer);
        BlockPos target = helper.absolutePos(new BlockPos(1, 1, 1));
        var neighbor = target.east();
        var outside = target.east(2);
        var protectedNeighbor = target.west();
        var level = helper.getLevel();
        var hit = new BlockHitResult(Vec3.atCenterOf(target), Direction.UP, target, false);
        hammer.set(HammerRegistry.MULTI_BLOCK_MINING.get(), new MiningModeData(MiningShape.FLAT_3X3, 1, 64, true));
        for (var mode : java.util.List.of(HammerAction.AUTO, HammerAction.STRIP, HammerAction.PATH, HammerAction.TILL, HammerAction.THROW)) {
            hammer.set(HammerRegistry.HAMMER_ACTION.get(), mode);
            var original = mode == HammerAction.STRIP || mode == HammerAction.THROW ? Blocks.OAK_LOG : Blocks.DIRT;
            for (var pos : java.util.List.of(target, neighbor, outside, protectedNeighbor)) {
                level.setBlockAndUpdate(pos, original.defaultBlockState());
                level.setBlockAndUpdate(pos.above(), Blocks.AIR.defaultBlockState());
            }
            boolean use;
            //? if fabric {
            /*var release = TestPlatform.vetoPlacement(player, (pos, block) -> pos.equals(protectedNeighbor));
            try {
                use = TestPlatform.clickBlock(player, InteractionHand.MAIN_HAND, hit);
                helper.assertTrue(level.getBlockState(protectedNeighbor).is(original), mode + " changed a vetoed neighbor");
            } finally {
                release.run();
            }
            *///?} else {
            use = TestPlatform.clickBlock(player, InteractionHand.MAIN_HAND, hit);
            //?}
            var expected = switch (mode) {
                case AUTO, PATH -> Blocks.DIRT_PATH;
                case STRIP -> Blocks.STRIPPED_OAK_LOG;
                case TILL -> Blocks.FARMLAND;
                case THROW -> Blocks.OAK_LOG;
                default -> throw new AssertionError(mode);
            };
            helper.assertTrue(use && level.getBlockState(target).is(expected), "Wrong center effect for " + mode);
            helper.assertTrue(level.getBlockState(neighbor).is(expected), "Wrong 3×3 neighbor effect for " + mode);
            helper.assertTrue(level.getBlockState(outside).is(original), mode + " changed an eligible block outside the 3×3");
            if (mode == HammerAction.THROW) {
                var thrown = helper.getLevel().getEntities(HammerRegistry.THROWN_ITEM.get(), e -> e.getOwner() == player);
                helper.assertTrue(hammer.isEmpty() && thrown.size() == 1 &&
                        thrown.getFirst().getThrownItem().get(HammerRegistry.HAMMER_ACTION.get()) == mode, "Throw lost the selected action or the tool");
                thrown.getFirst().discard();
            } else if (mode != HammerAction.AUTO) {
                level.setBlockAndUpdate(target, Blocks.CRAFTING_TABLE.defaultBlockState());
                level.setBlockAndUpdate(neighbor, original.defaultBlockState());
                boolean table = TestPlatform.clickBlock(player, InteractionHand.MAIN_HAND, hit);
                boolean air = TestPlatform.clickAir(player, InteractionHand.MAIN_HAND);
                helper.assertTrue(!table && !air && !hammer.isEmpty()
                                && level.getBlockState(neighbor).is(original),
                        "An inapplicable explicit action consumed vanilla interaction or threw the tool");
            }
        }
        helper.succeed();
    }

    private static void checkConfigurations(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        ItemStack hammer = HammerStage.TEMPEST.createStack();
        player.setItemInHand(InteractionHand.MAIN_HAND, hammer);
        var fortune = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE);
        var silk = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH);
        helper.assertTrue(hammer.getEnchantments().getLevel(fortune) == 3, "Fresh Hammer must already have Fortune III");
        var stone = Blocks.STONE.defaultBlockState();
        var pos = player.blockPosition();
        var speedWithoutConfig = hammer.copy();
        speedWithoutConfig.remove(HammerRegistry.TOOL_CONFIG.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, speedWithoutConfig);
        float originalSpeed = TestPlatform.destroySpeed(player, stone, pos);
        player.setItemInHand(InteractionHand.MAIN_HAND, hammer);
        for (MiningShape shape : MiningShape.values()) {
            var mode = new MiningModeData(shape, shape.getRadius(), 64, true);
            hammer.set(HammerRegistry.MULTI_BLOCK_MINING.get(), mode);
            for (MiningSpeed speed : MiningSpeed.values()) {
                for (boolean useFortune : new boolean[]{false, true}) {
                    ToolConfig config = new ToolConfig(useFortune, !useFortune, speed);
                    UpdateToolConfigPayload.applyToHeldItem(player, config);
                    String description = shape + " / " + speed + " / " + config.getEnchantmentType();
                    helper.assertTrue(config.equals(hammer.get(HammerRegistry.TOOL_CONFIG.get())), description);
                    helper.assertTrue(mode.equals(hammer.get(HammerRegistry.MULTI_BLOCK_MINING.get())), "Config changed shape: " + description);
                    helper.assertTrue(hammer.getEnchantments().getLevel(fortune) == (useFortune ? 3 : 0)
                            && hammer.getEnchantments().getLevel(silk) == (useFortune ? 0 : 1), description);
                    float expected = switch (speed) {
                        case STANDARD -> originalSpeed;
                        case EFFICIENCY -> 35;
                        case INSTABREAK -> 100;
                    };
                    helper.assertTrue(TestPlatform.destroySpeed(player, stone, pos) == expected, "Effective speed: " + description);
                }
            }
        }
        helper.succeed();
    }

    private static void checkThrowFailure(GameTestHelper helper) {
        var player = player(helper);
        var hammer = new ItemStack(HammerRegistry.HAMMER.get());
        hammer.set(DataComponents.CUSTOM_NAME, Component.literal("Quarry Hammer"));
        var expected = hammer.copy();
        player.setItemInHand(InteractionHand.MAIN_HAND, hammer);
        //? if neoforge {
        TestPlatform.withRejectedThrow(player, () -> ThrowItemPayload.throwItem(player, InteractionHand.MAIN_HAND));
        helper.assertTrue(ItemStack.matches(expected, player.getMainHandItem()), "Rejected throw consumed the Hammer");
        //?}

        ThrowItemPayload.throwItem(player, InteractionHand.MAIN_HAND);
        var entities = helper.getLevel().getEntities(HammerRegistry.THROWN_ITEM.get(), e -> e.getOwner() == player);
        helper.assertTrue(entities.size() == 1 && player.getMainHandItem().isEmpty(), "Expected a single accepted throw");
        for (int slot = 0; slot < player.getInventory().getNonEquipmentItems().size(); slot++) {
            player.getInventory().setItem(slot, new ItemStack(Items.STONE, 64));
        }
        var entity = entities.getFirst();
        entity.setPos(player.position().add(40, 0, 0));
        entity.tick();
        entity.setPos(player.position());
        entity.tick();
        var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(3),
                e -> ItemStack.matches(expected, e.getItem()));
        helper.assertTrue(entity.isRemoved() && drops.size() == 1, "A full inventory should leave one recoverable Hammer");
        helper.succeed();
    }

    //? if neoforge {
    private static void checkRecoveryTossVeto(GameTestHelper helper) {
        var player = player(helper);
        var expected = namedRecoveryHammer("Toss recovery Hammer");
        var entity = beginRecoveryThrow(helper, player, expected);
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getNonEquipmentItems().size(); slot++) {
            inventory.setItem(slot, new ItemStack(Items.STONE, 64));
        }
        var catchArea = player.getBoundingBox().inflate(3);

        int vetoes = TestPlatform.withRejectedRecoveryToss(player, entity::tick);
        helper.assertTrue(vetoes == 1, "Expected the full inventory to attempt one item toss");
        helper.assertTrue(!entity.isRemoved() && ItemStack.matches(expected, entity.getThrownItem())
                        && inventory.countItem(expected.getItem()) == 0 && recoveryDrops(helper, catchArea, expected).isEmpty(),
                "Toss veto should leave one unchanged Hammer in the returning projectile");

        int freeSlot = 5;
        inventory.setItem(freeSlot, ItemStack.EMPTY);
        for (int tick = 0; tick < 40 && !entity.isRemoved(); tick++) entity.tick();
        helper.assertTrue(entity.isRemoved() && ItemStack.matches(expected, inventory.getItem(freeSlot))
                        && inventory.countItem(expected.getItem()) == 1 && recoveryDrops(helper, catchArea, expected).isEmpty(),
                "The recovery retry should return one unchanged Hammer to the newly freed slot");
        helper.succeed();
    }
    //?}

    private static void checkRecoveryCrossDimension(GameTestHelper helper) {
        var player = player(helper);
        var expected = namedRecoveryHammer("Dimension recovery Hammer");
        var entity = beginRecoveryThrow(helper, player, expected);
        var originLevel = helper.getLevel();
        var destination = originLevel.getServer().getLevel(Level.NETHER);
        helper.assertTrue(destination != null && destination != originLevel, "Missing destination dimension");
        var recoveryArea = entity.getBoundingBox().inflate(3);
        player.setServerLevel(destination);
        try {
            //? if neoforge {
            int vetoes = TestPlatform.withRejectedRecoveryDrop(originLevel,
                    drop -> ItemStack.matches(expected, drop.getItem()), entity::tick);
            helper.assertTrue(vetoes == 1, "Expected one fallback spawn in the Hammer's dimension");
            helper.assertTrue(!entity.isRemoved() && ItemStack.matches(expected, entity.getThrownItem())
                            && player.getInventory().countItem(expected.getItem()) == 0
                            && recoveryDrops(helper, recoveryArea, expected).isEmpty(),
                    "Spawn veto should leave the unchanged Hammer in the projectile");
            //?}
            for (int tick = 0; tick < 40 && !entity.isRemoved(); tick++) entity.tick();
            var drops = recoveryDrops(helper, recoveryArea, expected);
            helper.assertTrue(entity.isRemoved() && drops.size() == 1 && ItemStack.matches(expected, drops.getFirst().getItem())
                            && player.getInventory().countItem(expected.getItem()) == 0,
                    "Recovery should leave one unchanged Hammer in the original dimension");
        } finally {
            player.setServerLevel(originLevel);
        }
        helper.succeed();
    }

    private static ItemStack namedRecoveryHammer(String name) {
        var stack = HammerStage.FORGED.createStack();
        stack.set(HammerRegistry.HAMMER_ACTION.get(), HammerAction.THROW);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        return stack;
    }

    private static ThrownItemEntity beginRecoveryThrow(GameTestHelper helper, ServerPlayer player, ItemStack expected) {
        player.setItemInHand(InteractionHand.MAIN_HAND, expected.copy());
        helper.assertTrue(TestPlatform.clickAir(player, InteractionHand.MAIN_HAND), "Throw was not accepted");
        var entities = helper.getLevel().getEntities(HammerRegistry.THROWN_ITEM.get(), projectile -> projectile.getOwner() == player);
        helper.assertTrue(entities.size() == 1 && player.getMainHandItem().isEmpty(), "Expected one Hammer in flight");
        var entity = entities.getFirst();
        entity.setPos(player.position().add(40, 0, 0));
        entity.tick();
        helper.assertTrue(entity.isReturning(), "Hammer did not start returning");
        entity.setPos(player.position());
        return entity;
    }

    private static List<ItemEntity> recoveryDrops(GameTestHelper helper, AABB area, ItemStack expected) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, area, drop -> drop.getItem().is(expected.getItem()));
    }

    private static void straightFlight(GameTestHelper helper) {
        var player = player(helper);
        player.setPos(player.position().add(0, 12, 0));
        player.setYRot(0);
        player.setXRot(0);
        var hammer = HammerStage.FORGED.createStack();
        hammer.set(HammerRegistry.HAMMER_ACTION.get(), HammerAction.THROW);
        var expected = hammer.copy();
        player.setItemInHand(InteractionHand.MAIN_HAND, hammer);
        var level = helper.getLevel();
        var target = BlockPos.containing(player.getEyePosition()).south(9);
        var previousBlock = level.getBlockState(target);
        level.setBlockAndUpdate(target, Blocks.STONE.defaultBlockState());
        try {
            helper.assertTrue(TestPlatform.clickAir(player, InteractionHand.MAIN_HAND), "Throw action was not accepted");
            var projectiles = level.getEntities(HammerRegistry.THROWN_ITEM.get(), e -> e.getOwner() == player);
            helper.assertTrue(projectiles.size() == 1 && player.getMainHandItem().isEmpty(), "Expected one Hammer in flight");
            var projectile = projectiles.getFirst();
            var start = projectile.position();
            var catchPoint = player.getEyePosition();
            boolean reachedTarget = false;
            boolean observedReturn = false;
            for (int tick = 0; tick < 40 && !projectile.isRemoved(); tick++) {
                Vec3 before = projectile.position();
                projectile.tick();
                if (projectile.isRemoved()) break;
                Vec3 after = projectile.position();
                if (reachedTarget) {
                    Vec3 toOwner = catchPoint.subtract(before);
                    Vec3 step = after.subtract(before);
                    helper.assertTrue(step.dot(toOwner) > 0 && step.cross(toOwner.normalize()).length() < 0.001,
                            "Hammer did not move straight back on return tick " + tick + ": " + step);
                    observedReturn = true;
                } else {
                    helper.assertTrue(Math.abs(after.x - start.x) < 0.001 && Math.abs(after.y - start.y) < 0.001
                                    && after.z > before.z && after.z <= target.getZ() + 0.001,
                            "Level throw deviated from its aim line or passed the target on tick " + tick + ": " + after);
                    if (projectile.isReturning()) {
                        helper.assertTrue(Math.abs(after.z - target.getZ()) < 0.1, "Hammer turned before touching stone");
                        reachedTarget = true;
                    }
                }
            }
            helper.assertTrue(reachedTarget && observedReturn && projectile.isRemoved()
                            && ItemStack.matches(expected, player.getMainHandItem()),
                    "Hammer must reach the target, fly back, and be caught");
        } finally {
            level.setBlockAndUpdate(target, previousBlock);
        }
        helper.succeed();
    }

    private static void checkThrowImpact(GameTestHelper helper, boolean protectedImpact) {
        var player = player(helper);
        var level = helper.getLevel();
        var launch = helper.absolutePos(new BlockPos(8, 10, 4));
        player.setPos(Vec3.atCenterOf(launch));
        player.setYRot(0);
        player.setXRot(0);
        var target = BlockPos.containing(player.getEyePosition()).south(4);
        var neighbor = target.east();
        level.setBlockAndUpdate(target, Blocks.DIAMOND_ORE.defaultBlockState());
        level.setBlockAndUpdate(neighbor, Blocks.DIAMOND_ORE.defaultBlockState());

        var hammer = HammerStage.RESONANT.createStack();
        hammer.set(DataComponents.CUSTOM_NAME, Component.literal("Quarry Throw Hammer"));
        hammer.set(HammerRegistry.HAMMER_ACTION.get(), HammerAction.THROW);
        hammer.set(HammerRegistry.HAMMER_CHARGE.get(), 16);
        hammer.set(HammerRegistry.THROWABLE_ABILITY.get(), ThrowableAbilityData.DEFAULT.withBreakBlocks(true));
        hammer.set(HammerRegistry.MULTI_BLOCK_MINING.get(), new MiningModeData(MiningShape.SHAPELESS, 1, 64, true));
        player.setItemInHand(InteractionHand.MAIN_HAND, hammer);
        UpdateToolConfigPayload.applyToHeldItem(player, new ToolConfig(false, true, MiningSpeed.STANDARD));
        var expected = hammer.copy();
        helper.assertTrue(TestPlatform.clickAir(player, InteractionHand.MAIN_HAND), "Throw action was not accepted");
        var projectiles = level.getEntities(HammerRegistry.THROWN_ITEM.get(), e -> e.getOwner() == player);
        helper.assertTrue(projectiles.size() == 1 && player.getMainHandItem().isEmpty(), "Expected one Hammer in flight");
        var projectile = projectiles.getFirst();

        var selected = new ItemStack(Items.STICK, 5);
        player.setItemInHand(InteractionHand.MAIN_HAND, selected);
        var inventory = player.getInventory();
        int selectedSlot = inventory.getSelectedSlot();
        var attempts = new AtomicInteger();
        Runnable release = TestPlatform.vetoBreak(player, pos -> {
            attempts.incrementAndGet();
            helper.assertTrue(pos.equals(target), "Throw attempted to break a block outside the impact");
            helper.assertTrue(ItemStack.matches(expected, player.getMainHandItem()), "Break hook saw the wrong tool");
            helper.assertTrue(inventory.getItem(selectedSlot) == selected, "Break hook saw a replaced hotbar slot");
            return protectedImpact;
        });
        try {
            for (int tick = 0; tick < 4 && attempts.get() == 0; tick++) projectile.tick();
            helper.assertTrue(attempts.get() == 1, "Expected exactly one break permission check at impact");
            helper.assertTrue(level.getBlockState(neighbor).is(Blocks.DIAMOND_ORE), "Ordinary throw mined the neighboring ore");
            var drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(target).inflate(2));
            if (protectedImpact) {
                helper.assertTrue(level.getBlockState(target).is(Blocks.DIAMOND_ORE) && drops.isEmpty(),
                        "Protected impact must preserve the ore and produce no drops");
                helper.assertTrue(projectile.isReturning(), "Hammer must begin returning from the protected block");
            } else {
                helper.assertTrue(level.isEmptyBlock(target) && drops.size() == 1
                                && ItemStack.matches(drops.getFirst().getItem(), new ItemStack(Items.DIAMOND_ORE)),
                        "Allowed impact must drop one Silk Touch ore block");
                helper.assertTrue(!projectile.isReturning(), "Hammer must continue flying after breaking the block");
            }
            var inFlight = level.getEntities(HammerRegistry.THROWN_ITEM.get(), e -> e.getOwner() == player);
            helper.assertTrue(inFlight.size() == 1 && inFlight.getFirst() == projectile
                            && ItemStack.matches(expected, projectile.getThrownItem())
                            && inventory.countItem(expected.getItem()) == 0,
                    "Impact must leave exactly one unchanged Hammer in the projectile");
            helper.assertTrue(player.getMainHandItem() == selected && selected.getCount() == 5,
                    "Impact changed the selected hotbar stack");
        } finally {
            release.run();
            projectile.discard();
        }
        helper.succeed();
    }

    private static void checkThrow(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        ItemStack stack = new ItemStack(Items.STICK, 16);
        stack.set(HammerRegistry.THROWABLE_ABILITY.get(), ThrowableAbilityData.DEFAULT);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("Returning stick"));
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        ThrowItemPayload.throwItem(player, InteractionHand.MAIN_HAND);
        var projectiles = helper.getLevel().getEntities(HammerRegistry.THROWN_ITEM.get(), e -> e.getOwner() == player);
        helper.assertTrue(projectiles.size() == 1 && stack.getCount() == 15, "Exactly one item must leave the hand");
        ThrownItemEntity thrown = projectiles.getFirst();
        var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
        thrown.saveWithoutId(output);
        thrown.discard();
        var restored = HammerRegistry.THROWN_ITEM.get().create(helper.getLevel(), EntitySpawnReason.LOAD);
        restored.load(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), output.buildResult()));
        helper.assertTrue(restored.getThrownItem().getCount() == 1
                && ItemStack.isSameItemSameComponents(stack, restored.getThrownItem()), "Reload lost item count or components");
        restored.setOwner(player);
        restored.setPos(player.position().add(40, 0, 0));
        restored.tick();
        helper.assertTrue(restored.isReturning() && restored.isNoPhysics(), "Range alone must start homing");
        restored.setPos(player.position());
        restored.tick();
        helper.assertTrue(restored.isRemoved() && stack.getCount() == 16, "Return must conserve the stack");
        helper.succeed();
    }
}
