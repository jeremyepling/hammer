package net.j40climb.florafauna_hammer.playtest;

import com.mojang.blaze3d.platform.InputConstants;
import net.j40climb.florafauna_hammer.common.item.abilities.data.HammerAction;
import net.j40climb.florafauna_hammer.common.item.abilities.networking.ChargedThrowPayload;
import net.j40climb.florafauna_hammer.common.item.hammer.HammerItem;
import net.j40climb.florafauna_hammer.common.item.hammer.HammerStage;
import net.j40climb.florafauna_hammer.setup.ClientSetup;
import net.j40climb.florafauna_hammer.setup.HammerKey;
import net.j40climb.florafauna_hammer.setup.HammerRegistry;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

final class ChargedHammerPlaytest {
    private static final BlockPos RIGHT_CLICK_ORE = new BlockPos(2, 202, 4);

    static void rightClickSteps(Playtest t) {
        var ore = RIGHT_CLICK_ORE;
        t.server("Build a grounded ore lane and equip a Tempest Hammer", p -> {
            var level = p.level();
            for (var pos : BlockPos.betweenClosed(ore.offset(-3, -2, -6), ore.offset(3, 3, 1))) {
                var block = pos.getY() == ore.getY() - 2 || pos.getZ() == ore.getZ() + 1
                        ? Blocks.SMOOTH_STONE : Blocks.AIR;
                level.setBlockAndUpdate(pos, block.defaultBlockState());
            }
            level.setBlockAndUpdate(ore, Blocks.CHEST.defaultBlockState());
            level.setBlockAndUpdate(ore.east(), Blocks.DIAMOND_ORE.defaultBlockState());
            level.setBlockAndUpdate(ore.south(), Blocks.IRON_ORE.defaultBlockState());
            p.setGameMode(GameType.SURVIVAL);
            p.getInventory().clearContent();
            p.getInventory().setSelectedSlot(0);
            var hammer = HammerStage.TEMPEST.createStack();
            hammer.set(HammerRegistry.HAMMER_ACTION.get(), HammerAction.AUTO);
            p.setItemInHand(InteractionHand.MAIN_HAND, hammer);
            p.getInventory().setItem(1, new ItemStack(Items.STICK));
            p.teleportTo(level, ore.getX() + 0.5, ore.getY() - 1, ore.getZ() - 3.5, Set.of(), 0f, 0f, true);
        });
        t.client("Frame the Hammer in first person", mc -> {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            Playtest.showHud(mc);
            Playtest.require(HammerKey.USE_HAMMER.mapping().isUnbound(), "Use Hammer must start unbound");
            bindHammerUse();
        });
        t.awaitClient("Face the chest while grounded with the Hammer selected", mc -> mc.player.onGround()
                && mc.player.getMainHandItem().is(HammerRegistry.HAMMER.get())
                && mc.hitResult instanceof BlockHitResult hit && hit.getBlockPos().equals(ore));
        t.client("Use the optional Hammer binding with Auto selected", mc -> press(HammerKey.USE_HAMMER.mapping()));
        t.awaitClient("Open the chest while keeping the Hammer", mc -> Playtest.screen(mc) instanceof ContainerScreen
                && mc.player.getMainHandItem().is(HammerRegistry.HAMMER.get()));
        t.client("Release the optional binding and close the chest", mc -> {
            HammerKey.USE_HAMMER.mapping().setDown(false);
            mc.player.closeContainer();
        });
        t.server("Expose the ore and select Throw", p -> {
            p.level().setBlockAndUpdate(ore, Blocks.DIAMOND_ORE.defaultBlockState());
            p.getMainHandItem().set(HammerRegistry.HAMMER_ACTION.get(), HammerAction.THROW);
        });
        t.awaitClient("See the exposed ore with Throw selected", mc -> mc.level.getBlockState(ore).is(Blocks.DIAMOND_ORE)
                && mc.player.getMainHandItem().get(HammerRegistry.HAMMER_ACTION.get()) == HammerAction.THROW);
        t.screenshot("throw-input-idle");

        t.client("Press the mapped Use button", mc -> press(mc.options.keyUse));
        t.client("Release Use on the next client tick", mc -> mc.options.keyUse.setDown(false));
        t.awaitServer("Observe the quick throw and intact ore", p -> {
            var projectiles = p.level().getEntities(HammerRegistry.THROWN_ITEM.get(), e -> e.getOwner() == p);
            if (projectiles.isEmpty()) return false;
            Playtest.require(projectiles.size() == 1 && p.getMainHandItem().isEmpty(), "Quick release did not launch exactly one Hammer");
            Playtest.require(HammerItem.charge(projectiles.getFirst().getThrownItem()) == HammerStage.TEMPEST.capacity(),
                    "Quick throw spent charge");
            return p.level().getBlockState(ore).is(Blocks.DIAMOND_ORE);
        });
        t.awaitClient("Catch the quick throw with both ores still intact", mc ->
                mc.player.getMainHandItem().is(HammerRegistry.HAMMER.get())
                        && mc.level.getBlockState(ore).is(Blocks.DIAMOND_ORE)
                        && mc.level.getBlockState(ore.east()).is(Blocks.DIAMOND_ORE));

        t.client("Start a second throw hold", mc -> press(mc.options.keyUse));
        t.awaitServer("Observe a new held Hammer before switching slots", p -> stillHolding(p, 0));
        t.client("Switch to the stick while holding Use", mc -> mc.player.getInventory().setSelectedSlot(1));
        t.awaitServer("Cancel the hold and keep the Hammer in the original slot", p -> p.getMainHandItem().is(Items.STICK)
                && HammerRegistry.throwHold(p) == null && p.getInventory().getItem(0).is(HammerRegistry.HAMMER.get()));
        t.client("Release Use, then reselect the Hammer", mc -> {
            mc.options.keyUse.setDown(false);
            mc.player.getInventory().setSelectedSlot(0);
        });
        t.awaitServer("Keep one Hammer and no flight after cancelling", p ->
                p.getMainHandItem().is(HammerRegistry.HAMMER.get()) && p.getInventory().countItem(HammerRegistry.HAMMER.get()) == 1
                        && p.level().getEntities(HammerRegistry.THROWN_ITEM.get(), e -> e.getOwner() == p).isEmpty());

        t.client("Hold the optional Hammer Use key for an ore strike", mc -> press(HammerKey.USE_HAMMER.mapping()));
        t.awaitServer("Hold the same Hammer for five server ticks", p -> stillHolding(p, 5));
        t.screenshot("throw-input-charging-early");
        t.awaitServer("Keep the Hammer in hand for twelve server ticks", p -> stillHolding(p, 12));
        t.screenshot("throw-input-charging-late");
        t.awaitServer("Reach the server's ore-strike threshold", p -> stillHolding(p, ChargedThrowPayload.HOLD_TICKS));
        t.screenshot("throw-input-ready");
        t.client("Release the optional key to launch the powered Hammer", mc -> HammerKey.USE_HAMMER.mapping().setDown(false));
        t.awaitServer("Mine only the two diamond ores and debit the projectile", p -> {
            if (!p.level().isEmptyBlock(ore) || !p.level().isEmptyBlock(ore.east())) return false;
            Playtest.require(p.level().getBlockState(ore.south()).is(Blocks.IRON_ORE), "Powered throw mined a different ore");
            var projectiles = p.level().getEntities(HammerRegistry.THROWN_ITEM.get(), e -> e.getOwner() == p);
            return projectiles.size() == 1
                    && HammerItem.charge(projectiles.getFirst().getThrownItem()) == HammerStage.TEMPEST.capacity() - 2;
        });
        t.awaitClient("Catch one Hammer and see the two mined blocks", mc ->
                mc.player.getMainHandItem().is(HammerRegistry.HAMMER.get())
                        && mc.player.getInventory().countItem(HammerRegistry.HAMMER.get()) == 1
                        && mc.level.isEmptyBlock(ore) && mc.level.isEmptyBlock(ore.east()));
    }

    private static boolean stillHolding(ServerPlayer p, int ticks) {
        var hold = HammerRegistry.throwHold(p);
        if (hold == null) return false;
        Playtest.require(hold.hammer() == p.getMainHandItem()
                        && p.level().getEntities(HammerRegistry.THROWN_ITEM.get(), e -> e.getOwner() == p).isEmpty(),
                "The Hammer launched before Use was released");
        return p.level().getGameTime() - hold.start() >= ticks;
    }

    private static void press(KeyMapping mapping) {
        mapping.setDown(true);
        KeyMapping.click(ClientSetup.boundKey(mapping));
    }

    static void bindHammerUse() {
        HammerKey.USE_HAMMER.mapping().setKey(InputConstants.getKey("key.keyboard.v"));
        KeyMapping.resetMapping();
    }

    static void steps(Playtest t) {
        BlockPos ore = new BlockPos(24, 161, 24);
        t.server("Expose a diamond vein at the end of the throw lane", p -> {
            for (BlockPos pos : BlockPos.betweenClosed(ore.offset(-1, -1, -8), ore.offset(1, 1, -1))) {
                p.level().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            }
            for (BlockPos pos : BlockPos.betweenClosed(ore.offset(-2, -2, -8), ore.offset(2, -2, 0))) {
                p.level().setBlockAndUpdate(pos, Blocks.SMOOTH_STONE.defaultBlockState());
            }
            p.level().setBlockAndUpdate(ore, Blocks.DIAMOND_ORE.defaultBlockState());
            p.level().setBlockAndUpdate(ore.east(), Blocks.DIAMOND_ORE.defaultBlockState());
            p.level().setBlockAndUpdate(ore.south(), Blocks.IRON_ORE.defaultBlockState());
            p.getMainHandItem().set(HammerRegistry.HAMMER_ACTION.get(), HammerAction.THROW);
            p.teleportTo(p.level(), ore.getX() + 0.5, ore.getY() - 1, ore.getZ() - 6.5, Set.of(), 0f, 0f, true);
        });
        t.awaitClient("See the charged-throw lane", mc -> mc.player.position().distanceToSqr(Vec3.atCenterOf(ore.north(7))) < 6
                && mc.level.getBlockState(ore).is(Blocks.DIAMOND_ORE) && mc.player.getMainHandItem().is(HammerRegistry.HAMMER.get()));
        t.client("Hold the optional Hammer Use key", mc -> {
            bindHammerUse();
            press(HammerKey.USE_HAMMER.mapping());
        });
        t.awaitServer("Reach the ore-strike threshold with the Hammer still in hand", p -> {
            var hold = HammerRegistry.throwHold(p);
            return hold != null && p.level().getGameTime() - hold.start() >= ChargedThrowPayload.HOLD_TICKS
                    && p.getMainHandItem().is(HammerRegistry.HAMMER.get());
        });
        t.screenshot("tempest-throw-ready-first");
        t.client("Release the optional Hammer Use key", mc -> HammerKey.USE_HAMMER.mapping().setDown(false));
        t.awaitServer("Strike only the diamond vein and debit the flying Hammer", p -> {
            if (!p.level().isEmptyBlock(ore) || !p.level().isEmptyBlock(ore.east())) return false;
            Playtest.require(p.level().getBlockState(ore.south()).is(Blocks.IRON_ORE), "The Hammer dug into another ore");
            return p.level().getEntities(HammerRegistry.THROWN_ITEM.get(), e -> e.getOwner() == p
                    && HammerItem.charge(e.getThrownItem()) == HammerStage.TEMPEST.capacity() - 2).size() == 1;
        });
        t.screenshot("tempest-charged-ore-strike");
        t.awaitClient("Receive the returning Hammer in the hotbar", mc ->
                mc.player.getMainHandItem().is(HammerRegistry.HAMMER.get())
                        && mc.level.isEmptyBlock(ore) && mc.level.isEmptyBlock(ore.east()));
    }
}
