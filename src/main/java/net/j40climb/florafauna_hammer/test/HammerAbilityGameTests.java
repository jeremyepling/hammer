package net.j40climb.florafauna_hammer.test;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;


final class HammerAbilityGameTests {
    static void register(HammerGameTests.Registrar tests) {
        HammerGameTests.registerTest(tests, "hammer_legacy_saved_stack", HammerAbilityGameTests::legacySavedStack);
        HammerGameTests.registerTest(tests, "hammer_fresh_and_configured_loot", HammerAbilityGameTests::checkConfigurations);
        HammerGameTests.registerTest(tests, "hammer_block_action_dispatch", HammerAbilityGameTests::checkBlockActions);
        HammerGameTests.registerTest(tests, "hammer_throw_single_item_and_reload", HammerAbilityGameTests::checkThrow);
        HammerGameTests.registerTest(tests, "hammer_throw_rejected_or_inventory_full", HammerAbilityGameTests::checkThrowFailure);
    }

    private static ServerPlayer player(GameTestHelper helper) {
        ServerPlayer player = TestPlatform.player(helper.getLevel(), "hammer-test");
        player.setGameMode(GameType.SURVIVAL);
        player.setPos(Vec3.atCenterOf(helper.absolutePos(new BlockPos(1, 2, 1))));
        player.setOnGround(true);
        return player;
    }

    private static void legacySavedStack(GameTestHelper helper) {
        // This represents an inventory stack saved before Hammer became its own mod.
        var saved = JsonParser.parseString("""
                {"id":"florafauna:hammer", "count":1, "components":{
                  "florafauna:hammer_charge":17, "florafauna:hammer_action":"strip",
                  "florafauna:multi_block_mining":{"shape":"stairs_down","radius":0,"maxBlocksToBreak":24},
                  "florafauna:tool_config":{"fortune":false,"silkTouch":true,"miningSpeed":"standard"}
                }}
                """);
        var ops = helper.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE);
        ItemStack stack = ItemStack.CODEC.parse(ops, saved).getOrThrow();
        helper.assertTrue(stack.is(HammerRegistry.HAMMER.get()) && stack.get(HammerRegistry.HAMMER_CHARGE.get()) == 17
                && stack.get(HammerRegistry.HAMMER_ACTION.get()) == HammerAction.STRIP, "Saved Hammer lost its identity, charge or action");
        var mode = stack.get(HammerRegistry.MULTI_BLOCK_MINING.get());
        helper.assertTrue(mode.shape() == MiningShape.TUNNEL_DOWN && mode.maxBlocksToBreak() == 24
                && stack.get(HammerRegistry.TOOL_CONFIG.get()).equals(new ToolConfig(false, true, MiningSpeed.STANDARD)),
                "Legacy mining settings did not decode with their original defaults");
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
