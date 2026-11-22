package net.j40climb.florafauna_hammer.playtest;

import net.j40climb.florafauna_hammer.common.item.abilities.data.HammerAction;
import net.j40climb.florafauna_hammer.common.item.abilities.data.MiningShape;
import net.j40climb.florafauna_hammer.common.item.abilities.networking.SelectHammerActionPayload;
import net.j40climb.florafauna_hammer.common.item.abilities.networking.SelectMiningModePayload;
import net.j40climb.florafauna_hammer.common.item.abilities.networking.SpawnLightningPayload;
import net.j40climb.florafauna_hammer.common.item.abilities.networking.TeleportToSurfacePayload;
import net.j40climb.florafauna_hammer.common.item.hammer.HammerItem;
import net.j40climb.florafauna_hammer.common.item.hammer.HammerStage;
import net.j40climb.florafauna_hammer.setup.ClientSetup;
import net.j40climb.florafauna_hammer.setup.HammerKey;
import net.j40climb.florafauna_hammer.setup.HammerRegistry;
import net.j40climb.florafauna_hammer.test.HammerSurvivalFixture;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.world.InteractionHand;
//? if >=26.3 {
/*import net.minecraft.world.entity.EntityTypes;*/
//?} else {
import net.minecraft.world.entity.EntityType;
//?}
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;
import java.util.Set;

final class HammerProgressionPlaytest {
    private static final BlockPos TARGET = new BlockPos(0, 202, 4);

    static void steps(Playtest t, int phase) {
        if (phase == 2) {
            t.server("Reopen the named upgraded Hammer with its choices and favorite wedges", p ->
                    HammerSurvivalFixture.checkTool(p, HammerStage.TEMPEST));
            t.awaitClient("Rejoin with Tempest and both saved favorite shapes", mc -> {
                var stack = mc.player.getMainHandItem();
                var mode = stack.get(HammerRegistry.MULTI_BLOCK_MINING.get());
                return stack.is(HammerRegistry.HAMMER.get()) && HammerStage.of(stack) == HammerStage.TEMPEST
                        && mode.isFavorite(MiningShape.FLAT_3X3) && mode.isFavorite(MiningShape.BORE);
            });
            t.client("Find three charged stage variants in Tools and both cores in Ingredients", mc -> {
                CreativeModeTabs.tryRebuildTabContents(mc.level.enabledFeatures(), false, mc.level.registryAccess());
                var tabs = BuiltInRegistries.CREATIVE_MODE_TAB;
                var hammers = tabs.getValueOrThrow(CreativeModeTabs.TOOLS_AND_UTILITIES).getDisplayItems()
                        .stream().filter(stack -> stack.is(HammerRegistry.HAMMER.get())).toList();
                Playtest.require(hammers.stream().map(HammerStage::of).toList().equals(List.of(HammerStage.values())),
                        "Creative tools did not contain exactly one of each stage: " + hammers);
                for (var stack : hammers) {
                    Playtest.require(stack.getCount() == 1 && HammerItem.charge(stack) == HammerStage.of(stack).capacity(),
                            "Incorrect Creative Hammer stack count or charge: " + stack);
                }
                var ingredients = tabs.getValueOrThrow(CreativeModeTabs.INGREDIENTS).getDisplayItems();
                Playtest.require(ingredients.stream().anyMatch(s -> s.is(HammerRegistry.RESONANCE_CORE.get()))
                        && ingredients.stream().anyMatch(s -> s.is(HammerRegistry.TEMPEST_CORE.get())), "Missing Creative upgrade core");
            });
            return;
        }

        t.server("Build a lit Survival gallery and craft the initial Hammer at a crafting table", p -> {
            var level = p.level();
            for (var pos : BlockPos.betweenClosed(-6, 200, -6, 6, 207, 6)) {
                var block = pos.getY() == 200 ? Blocks.SMOOTH_STONE : Blocks.AIR;
                if (pos.getZ() == 5) block = Blocks.POLISHED_TUFF;
                level.setBlockAndUpdate(pos, block.defaultBlockState());
            }
            level.setBlockAndUpdate(TARGET, Blocks.OBSIDIAN.defaultBlockState());
            level.setBlockAndUpdate(TARGET.below().east(2), Blocks.CRAFTING_TABLE.defaultBlockState());
            level.setBlockAndUpdate(TARGET.below().west(2), Blocks.SMITHING_TABLE.defaultBlockState());
            for (int x : new int[]{-3, 3}) level.setBlockAndUpdate(TARGET.offset(x, 1, 1), Blocks.SEA_LANTERN.defaultBlockState());
            p.setGameMode(GameType.SURVIVAL);
            p.getInventory().clearContent();
            p.getInventory().setSelectedSlot(0);
            p.teleportTo(level, 0.5, 201, 0.5, Set.of(), 0f, 0f, true);
            HammerSurvivalFixture.forge(p);
        });
        for (HammerStage stage : HammerStage.values()) {
            if (stage != HammerStage.FORGED) t.server("Craft and smith " + stage, p -> HammerSurvivalFixture.upgrade(p, stage));
            t.awaitClient("Receive " + stage, mc -> mc.player.getMainHandItem().is(HammerRegistry.HAMMER.get())
                    && HammerStage.of(mc.player.getMainHandItem()) == stage && mc.player.onGround());
            gallery(t, stage);
        }
    }

    private static void gallery(Playtest t, HammerStage stage) {
        String stem = stage.getSerializedName();
        t.client("Inspect " + stage + " in first person", mc -> {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            Playtest.showHud(mc);
            mc.player.setXRot(0);
            mc.player.setYRot(0);
        });
        if (stage != HammerStage.FORGED) setCharge(t, stage.capacity());
        waitTicks(t, 16); // Finish the vanilla hand-equip transition before sampling the item.
        t.screenshot(stem + "-first-idle");
        if (stage != HammerStage.FORGED) {
            waitTicks(t, 8);
            t.screenshot(stem + "-first-idle-later");
            for (int charge : new int[]{stage.capacity() / 2, 3, 0}) {
                setCharge(t, charge);
                t.screenshot(stem + "-first-charge-" + charge);
            }
            setCharge(t, stage.capacity());
        }
        t.client("Use the front-facing third-person camera", mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
        t.screenshot(stem + "-third-front");
        t.client("Inspect the inventory slot", mc -> {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            Playtest.setScreen(mc, new InventoryScreen(mc.player));
        });
        t.screenshot(stem + "-inventory");
        t.client("Close inventory", mc -> mc.player.closeContainer());

        HammerPlaytest.openMenu(t);
        t.client("Inspect ability and mining locks on " + stage, mc -> {
            boolean advanced = stage != HammerStage.FORGED, late = stage == HammerStage.TEMPEST;
            var five = HammerPlaytest.button(mc, Component.translatable("gui.florafauna.hammer_menu.shape.flat_5x5"));
            var scan = HammerPlaytest.button(mc, HammerRadialPlaytest.actionLabel(HammerAction.SCAN));
            var lightning = HammerPlaytest.button(mc, HammerRadialPlaytest.actionLabel(HammerAction.LIGHTNING));
            Playtest.require(five.active == advanced && scan.active == advanced && lightning.active == late, "Incorrect " + stage + " wheel locks");
            var hovered = advanced ? lightning : five;
            pointAt(mc, hovered.getX() + hovered.getWidth() / 2.0, hovered.getY() + hovered.getHeight() / 2.0);
            //? if >=26.3 {
            /*mc.gui.toastManager().clear();*/
            //?} else {
            mc.getToastManager().clear();
            //?}
        });
        t.screenshot(stem + "-wheels");
        t.client("Release the wheel", HammerPlaytest::releaseWheel);
        if (stage == HammerStage.FORGED) verifyLockedPackets(t);

        t.awaitClient("Aim at the mining test surface", mc -> mc.hitResult instanceof BlockHitResult hit && hit.getBlockPos().equals(TARGET));
        t.client("Start mining " + stage, mc -> {
            var hit = (BlockHitResult) mc.hitResult;
            Playtest.require(mc.gameMode.startDestroyBlock(TARGET, hit.getDirection()), "Client refused the mining swing");
            //? if >=26.3 {
            /*mc.player.swing(InteractionHand.MAIN_HAND, mc.player.getMainHandItem().getAttackAnimation(), false);*/
            //?} else {
            mc.player.swing(InteractionHand.MAIN_HAND);
            //?}
        });
        //? if >=26.3 {
        /*t.awaitClient("Observe a mining swing with the equipped Hammer", mc -> mc.player.isSwinging());*/
        //?} else {
        t.awaitClient("Observe a mining swing with the equipped Hammer", mc -> mc.player.swinging);
        //?}
        t.screenshot(stem + "-mining-first");
        t.client("Stop mining", mc -> mc.gameMode.stopDestroyBlock());

        ItemEntity[] drop = new ItemEntity[1];
        t.server("Drop this Hammer on the gallery floor", p -> {
            var entity = new ItemEntity(p.level(), 0.5, 201.1, 2.5, p.getMainHandItem(), 0, 0, 0);
            entity.setNeverPickUp();
            Playtest.require(p.level().addFreshEntity(entity), "Drop rejected");
            p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            drop[0] = entity;
        });
        t.awaitClient("See the dropped " + stage + " in front of the player", mc ->
                mc.player.getMainHandItem().isEmpty() && !mc.level.getEntitiesOfClass(ItemEntity.class,
                        mc.player.getBoundingBox().inflate(4), e -> e.getItem().is(HammerRegistry.HAMMER.get())
                                && HammerStage.of(e.getItem()) == stage).isEmpty());
        t.client("Look down at the dropped item", mc -> mc.player.setXRot(32));
        t.screenshot(stem + "-dropped");
        t.server("Recover the same Hammer", p -> {
            var recovered = drop[0].getItem();
            drop[0].discard();
            p.setItemInHand(InteractionHand.MAIN_HAND, recovered);
            HammerSurvivalFixture.checkTool(p, stage);
        });
        t.awaitClient("See the recovered Hammer", mc -> mc.player.getMainHandItem().is(HammerRegistry.HAMMER.get()));
        t.client("Restore the level view", mc -> mc.player.setXRot(0));
        if (stage == HammerStage.TEMPEST) {
            t.client("Use minimal particles", mc -> mc.options.particles().set(ParticleStatus.MINIMAL));
            t.screenshot("tempest-first-minimal-particles");
        }
    }

    /** Deliver a client-local mouse event. Screen rendering reads MouseHandler coordinates each frame. */
    private static void pointAt(Minecraft mc, double guiX, double guiY) {
        var window = mc.getWindow();
        double x = guiX * window.getScreenWidth() / window.getGuiScaledWidth();
        double y = guiY * window.getScreenHeight() / window.getGuiScaledHeight();
        //? if >=26.3 {
        /*mc.mouseHandler.onMove(window.handle(), x, y, 0, 0);*/
        //?} else {
        try {
            var move = MouseHandler.class.getDeclaredMethod("onMove", long.class, double.class, double.class);
            move.setAccessible(true);
            move.invoke(mc.mouseHandler, window.handle(), x, y);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Cannot dispatch the playtest mouse movement", e);
        }
        //?}
        Playtest.screen(mc).mouseMoved(guiX, guiY);
    }

    private static void waitTicks(Playtest t, int ticks) {
        long[] start = new long[1];
        t.client("Start observing the held Hammer", mc -> start[0] = mc.level.getGameTime());
        t.awaitClient("Render another " + ticks + " ticks of the same held stack", mc -> mc.level.getGameTime() >= start[0] + ticks);
    }

    private static void setCharge(Playtest t, int charge) {
        // Leave ample time before the next ordinary regeneration tick to observe the exact requested charge.
        t.awaitServer("Set charge to " + charge + " just after regeneration", p -> {
            if (p.tickCount % 20 > 3) return false;
            p.getMainHandItem().set(HammerRegistry.HAMMER_CHARGE.get(), charge);
            p.inventoryMenu.broadcastChanges();
            return true;
        });
        t.awaitClient("Sync exact charge " + charge, mc -> HammerItem.charge(mc.player.getMainHandItem()) == charge);
    }

    private static void verifyLockedPackets(Playtest t) {
        BlockPos feet = new BlockPos(0, 201, 0);
        t.server("Prepare a reachable surface above the Forged Hammer", p ->
                p.level().setBlockAndUpdate(feet.above(4), Blocks.STONE.defaultBlockState()));
        t.client("Send locked shape and favorite selections to the authoritative server", mc -> {
            int slot = mc.player.getInventory().getSelectedSlot();
            ClientSetup.sendToServer(new SelectMiningModePayload(slot, MiningShape.FLAT_7X7, true));
            ClientSetup.sendToServer(new SelectMiningModePayload(slot, MiningShape.FLAT_7X7, false));
            // A later allowed ability selection acknowledges processing of both mining requests.
            ClientSetup.sendToServer(new SelectHammerActionPayload(slot, HammerAction.STRIP));
        });
        t.awaitServer("Reject direct 7x7 selection and favoriting", p -> {
            var tool = p.getMainHandItem();
            if (tool.get(HammerRegistry.HAMMER_ACTION.get()) != HammerAction.STRIP) return false;
            var mode = tool.get(HammerRegistry.MULTI_BLOCK_MINING.get());
            Playtest.require(mode.shape() == MiningShape.BORE && !mode.isFavorite(MiningShape.FLAT_7X7),
                    "Accepted locked mining mode or favorite");
            return true;
        });
        t.client("Send the lightning and teleport packets directly", mc -> {
            int slot = mc.player.getInventory().getSelectedSlot();
            ClientSetup.sendToServer(new SelectHammerActionPayload(slot, HammerAction.LIGHTNING));
            ClientSetup.sendToServer(new SpawnLightningPayload(SpawnLightningPayload.targetFromView(mc.player)));
            ClientSetup.sendToServer(TeleportToSurfacePayload.INSTANCE);
            ClientSetup.sendToServer(new SelectMiningModePayload(slot, MiningShape.FLAT_3X3, false));
        });
        t.awaitServer("Locked favorites and abilities remain rejected on the server", p -> {
            var tool = p.getMainHandItem();
            var mode = tool.get(HammerRegistry.MULTI_BLOCK_MINING.get());
            if (mode.shape() != MiningShape.FLAT_3X3) return false;
            Playtest.require(tool.get(HammerRegistry.HAMMER_ACTION.get()) == HammerAction.STRIP && p.blockPosition().equals(feet)
                    //? if >=26.3 {
                    /*&& p.level().getEntities(EntityTypes.LIGHTNING_BOLT, bolt -> bolt.distanceToSqr(p) < 144).isEmpty(),*/
                    //?} else {
                    && p.level().getEntities(EntityType.LIGHTNING_BOLT, bolt -> bolt.distanceToSqr(p) < 144).isEmpty(),
                    //?}
                    "A locked ability was selected or activated");
            p.level().setBlockAndUpdate(feet.above(4), Blocks.AIR.defaultBlockState());
            return true;
        });
        t.client("Double-tap C with only Bore and 3x3 favorited", mc -> {
            var key = ClientSetup.boundKey(HammerKey.CYCLE_MINING_MODE.mapping());
            KeyMapping.click(key);
            KeyMapping.click(key);
        });
        t.awaitClient("Select Single despite the favorites filter", mc ->
                mc.player.getMainHandItem().get(HammerRegistry.MULTI_BLOCK_MINING.get()).shape() == MiningShape.SINGLE);
        t.client("Restore Till and Bore", mc -> {
            int slot = mc.player.getInventory().getSelectedSlot();
            ClientSetup.sendToServer(new SelectHammerActionPayload(slot, HammerAction.TILL));
            ClientSetup.sendToServer(new SelectMiningModePayload(slot, MiningShape.BORE, false));
        });
        t.awaitServer("Server accepts Bore", p -> p.getMainHandItem().get(HammerRegistry.MULTI_BLOCK_MINING.get()).shape() == MiningShape.BORE);
    }
}
