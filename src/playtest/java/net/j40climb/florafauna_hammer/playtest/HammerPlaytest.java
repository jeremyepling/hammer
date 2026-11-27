package net.j40climb.florafauna_hammer.playtest;

import net.j40climb.florafauna_hammer.HammerMod;
import net.j40climb.florafauna_hammer.common.item.abilities.data.MultiToolAbilityData;
import net.j40climb.florafauna_hammer.common.item.abilities.data.RightClickAction;
import net.j40climb.florafauna_hammer.common.item.abilities.data.MiningShape;
import net.j40climb.florafauna_hammer.common.item.abilities.data.MiningSpeed;
import net.j40climb.florafauna_hammer.common.item.abilities.menu.HammerMenuScreen;
import net.j40climb.florafauna_hammer.common.item.abilities.networking.UpdateToolConfigPayload;
import net.j40climb.florafauna_hammer.setup.ClientSetup;
import net.j40climb.florafauna_hammer.setup.HammerKey;
import net.j40climb.florafauna_hammer.common.item.hammer.HammerStage;
import net.j40climb.florafauna_hammer.setup.HammerRegistry;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.HashSet;
import java.util.Set;

public final class HammerPlaytest {
    private static final BlockPos WALL = new BlockPos(0, 202, 3);
    private static final BlockPos FLOOR = new BlockPos(0, 200, 0);
    private static BlockPos lastCustomOutline;
    private static boolean observingCracks;
    private static final Set<BlockPos> cracks = new HashSet<>();

    // 26.3 consumes these states when it submits the breaking animation for rendering.
    static void captureCracks(net.minecraft.client.renderer.state.level.LevelRenderState state) {
        if (observingCracks) {
            cracks.clear();
            state.blockBreakingRenderStates.forEach(crack -> cracks.add(crack.blockPos()));
        }
    }

    static void captureOutline(BlockPos hammerOutline) {
        lastCustomOutline = hammerOutline;
    }

    static void steps(Playtest t, int phase) {
        if (phase == 2) {
            HammerQuarryPlaytest.resume(t);
            ChargedHammerPlaytest.steps(t);
            return;
        }
        if (phase != 1) {
            throw new IllegalArgumentException("Unknown Hammer phase " + phase);
        }
        t.server("Give the survival player a hammer and build the wall and floor", HammerPlaytest::prepare);
        t.awaitClient("Receive the hammer in the local player's hotbar",
                mc -> mc.player.getInventory().getItem(0).is(HammerRegistry.HAMMER.get()));
        t.client("Select the hammer and press its configured menu key", mc -> {
            mc.player.getInventory().setSelectedSlot(0);
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            Playtest.showHud(mc);
        });
        openSettings(t);
        t.client("Click the speed control to switch from Efficiency to Instabreak", mc -> {
            clickConfigButton(mc, "speed.efficiency");
            releaseWheel(mc);
        });
        t.awaitServer("Receive the speed update on the server", p ->
                p.getMainHandItem().get(HammerRegistry.TOOL_CONFIG.get()).miningSpeed() == MiningSpeed.INSTABREAK);
        t.awaitClient("Synchronize the server's tool configuration back to the client", mc ->
                mc.player.getMainHandItem().get(HammerRegistry.TOOL_CONFIG.get()).miningSpeed() == MiningSpeed.INSTABREAK);
        openSettings(t);
        t.client("Verify the persisted speed label and release the menu key", mc -> {
            configButton(mc, "speed.instabreak");
            releaseWheel(mc);
        });
        t.client("Tap the mapped mining key", mc -> KeyMapping.click(ClientSetup.boundKey(HammerKey.CYCLE_MINING_MODE.mapping())));
        t.awaitServer("Select flat 3×3 mining on the server", p ->
                p.getMainHandItem().get(HammerRegistry.MULTI_BLOCK_MINING.get()).shape() == MiningShape.FLAT_3X3);

        setVeinPreference(t, true);
        setVeinPreference(t, false);
        setLighting(t, true);
        setLighting(t, false);
        interruptSwing(t);
        HammerSinglePlaytest.steps(t, WALL);
        minePlane(t, WALL, Direction.NORTH, "wall");
        minePlane(t, FLOOR, Direction.UP, "floor");

        t.server("Set an oak log in front of the player", p -> {
            p.level().setBlockAndUpdate(FLOOR, Blocks.STONE.defaultBlockState());
            p.level().setBlockAndUpdate(WALL, Blocks.OAK_LOG.defaultBlockState());
            p.teleportTo(p.level(), 0.5, 201, 0.5, Set.of(), 0f, 0f, true);
        });
        t.awaitClient("Aim at the oak log", mc -> aimedAt(mc, WALL, Direction.NORTH)
                && mc.level.getBlockState(WALL).is(Blocks.OAK_LOG));
        t.client("Use the held hammer on the log", mc ->
                mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, (BlockHitResult) mc.hitResult));
        t.awaitServer("Verify oak was stripped and the hammer stayed in hand", p ->
                p.level().getBlockState(WALL).is(Blocks.STRIPPED_OAK_LOG)
                        && p.getMainHandItem().is(HammerRegistry.HAMMER.get()));
        var logX = Blocks.BIRCH_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
        var strippedLogX = Blocks.STRIPPED_BIRCH_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
        var woodZ = Blocks.BIRCH_WOOD.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z);
        var strippedWoodZ = Blocks.STRIPPED_BIRCH_WOOD.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z);
        t.server("Configure mining-mode cycling as the fallback right-click action", p -> {
            p.getMainHandItem().set(HammerRegistry.RIGHT_CLICK_ACTION.get(), new RightClickAction(BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(HammerRegistry.MULTI_BLOCK_MINING.get())));
            p.inventoryMenu.broadcastChanges();
        });
        // With stripping disabled, cycling from 3x3 to 5x5 acknowledges that the server handled the click.
        checkTransform(t, "Disabled stripping leaves the birch intact and runs the fallback", logX,
                MultiToolAbilityData.DEFAULT.withStrip(false), logX);
        checkTransform(t, "Strip birch log on its X axis", logX, MultiToolAbilityData.DEFAULT, strippedLogX);
        checkTransform(t, "Strip birch wood on its Z axis", woodZ, MultiToolAbilityData.DEFAULT, strippedWoodZ);
        checkTransform(t, "Flatten dirt before tilling when both modes are enabled", Blocks.DIRT.defaultBlockState(),
                MultiToolAbilityData.DEFAULT, Blocks.DIRT_PATH.defaultBlockState());
        checkTransform(t, "Till dirt when flattening is disabled", Blocks.DIRT.defaultBlockState(),
                MultiToolAbilityData.DEFAULT.withFlatten(false), Blocks.FARMLAND.defaultBlockState());
        HammerMenuPlaytest.steps(t, WALL);
        HammerQuarryPlaytest.start(t);
    }

    private static void setVeinPreference(Playtest t, boolean exact) {
        String current = exact ? "vein.same_ore" : "vein.exact";
        String next = exact ? "vein.exact" : "vein.same_ore";
        openSettings(t);
        t.client("Click the vein matching control to select " + next, mc -> {
            clickConfigButton(mc, current);
            configButton(mc, next);
            releaseWheel(mc);
        });
        t.awaitServer("Save vein matching " + next + " on the held hammer", p ->
                p.getMainHandItem().get(HammerRegistry.TOOL_CONFIG.get()).exactVein() == exact);
        t.awaitClient("Synchronize vein matching " + next + " to the client", mc ->
                mc.player.getMainHandItem().get(HammerRegistry.TOOL_CONFIG.get()).exactVein() == exact);
    }

    private static void setLighting(Playtest t, boolean enable) {
        String current = enable ? "lighting.off" : "lighting.on";
        String next = enable ? "lighting.on" : "lighting.off";
        openSettings(t);
        t.client("Click the auto torches control to select " + next, mc -> {
            clickConfigButton(mc, current);
            configButton(mc, next);
            releaseWheel(mc);
        });
        t.awaitServer("Save auto torches " + next + " on the held Hammer", p ->
                p.getMainHandItem().get(HammerRegistry.TOOL_CONFIG.get()).tunnelLighting() == enable);
        t.awaitClient("Sync auto torches " + next + " to the client", mc ->
                mc.player.getMainHandItem().get(HammerRegistry.TOOL_CONFIG.get()).tunnelLighting() == enable);
    }

    private static void interruptSwing(Playtest t) {
        t.server("Prepare a Standard-speed swing on the 3×3 stone wall", p -> setSpeed(p, MiningSpeed.STANDARD));
        t.awaitClient("Aim at the wall with Standard speed selected", mc -> aimedAt(mc, WALL, Direction.NORTH)
                && mc.player.getMainHandItem().get(HammerRegistry.TOOL_CONFIG.get()).miningSpeed() == MiningSpeed.STANDARD);
        t.client("Hold attack to begin mining", mc -> {
            observingCracks = true;
            KeyMapping.click(ClientSetup.boundKey(mc.options.keyAttack));
            mc.options.keyAttack.setDown(true);
        });
        Set<BlockPos> expected = new HashSet<>();
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) expected.add(WALL.offset(x, y, 0));
        }
        t.awaitClient("Render cracks at all nine target positions", mc -> cracks.equals(expected));
        t.client("Interrupt the swing", mc -> mc.options.keyAttack.setDown(false));
        t.awaitClient("Clear all cracks when the swing is interrupted", mc -> !mc.gameMode.isDestroying() && cracks.isEmpty());
        t.client("Finish observing the interrupted swing", mc -> observingCracks = false);
        t.server("Verify every block survived the interrupted swing", p -> {
            for (BlockPos pos : expected) {
                Playtest.require(p.level().getBlockState(pos).is(Blocks.STONE), "Interrupted swing broke " + pos);
            }
            setSpeed(p, MiningSpeed.INSTABREAK);
        });
        t.awaitClient("Receive Instabreak before the completed wall strike", mc ->
                mc.player.getMainHandItem().get(HammerRegistry.TOOL_CONFIG.get()).miningSpeed() == MiningSpeed.INSTABREAK);
    }

    private static void setSpeed(ServerPlayer player, MiningSpeed speed) {
        var config = player.getMainHandItem().get(HammerRegistry.TOOL_CONFIG.get());
        UpdateToolConfigPayload.applyToHeldItem(player, config.withMiningSpeed(speed));
        player.inventoryMenu.broadcastChanges();
    }

    private static void checkTransform(Playtest t, String label, BlockState before, MultiToolAbilityData modes, BlockState after) {
        t.server("Set up: " + label, p -> {
            p.level().setBlockAndUpdate(WALL.above(), Blocks.AIR.defaultBlockState());
            p.level().setBlockAndUpdate(WALL, before);
            p.getMainHandItem().set(HammerRegistry.MULTI_TOOL_ABILITY.get(), modes);
            p.inventoryMenu.broadcastChanges();
        });
        t.awaitClient("Receive the target and hammer modes: " + label, mc -> aimedAt(mc, WALL, Direction.NORTH)
                && mc.level.getBlockState(WALL).equals(before)
                && modes.equals(mc.player.getMainHandItem().get(HammerRegistry.MULTI_TOOL_ABILITY.get())));
        t.client("Use the hammer: " + label, mc ->
                mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, (BlockHitResult) mc.hitResult));
        t.awaitServer(label, p -> p.level().getBlockState(WALL).equals(after) && p.getMainHandItem().is(HammerRegistry.HAMMER.get())
                && p.getMainHandItem().get(HammerRegistry.MULTI_BLOCK_MINING.get()).shape() == MiningShape.FLAT_5X5);
        t.awaitClient("See the resulting block and mining mode: " + label, mc -> mc.level.getBlockState(WALL).equals(after)
                && mc.player.getMainHandItem().get(HammerRegistry.MULTI_BLOCK_MINING.get()).shape() == MiningShape.FLAT_5X5);
    }

    static void prepare(ServerPlayer player) {
        ServerLevel level = player.level().getServer().overworld();
        var origin = new BlockPos(-2, 199, -5);
        //? if >=26.3 {
        /*var plots = level.getStructureTemplateManager();*/
        //?} else {
        var plots = level.getStructureManager();
        //?}
        var fixture = plots.get(Identifier.fromNamespaceAndPath(HammerMod.MOD_ID, "plots/hammer")).orElseThrow();
        var placement = new StructurePlaceSettings().setIgnoreEntities(true);
        placement.setBoundingBox(fixture.getBoundingBox(placement, origin));
        Playtest.require(fixture.placeInWorld(level, origin, origin, placement, level.getRandom(), Block.UPDATE_CLIENTS),
                "Could not place Hammer plot");
        player.setGameMode(GameType.SURVIVAL);
        player.removeAllEffects();
        player.getInventory().clearContent();
        player.getInventory().setItem(0, HammerStage.TEMPEST.createStack());
        player.getInventory().setSelectedSlot(0);
        player.teleportTo(level, 0.5, 201, 0.5, Set.of(), 0f, 0f, true);
        player.inventoryMenu.broadcastChanges();
    }

    static void openMenu(Playtest t) {
        t.client("Hold the mapped Hammer control to open both wheels", mc -> {
            var key = ClientSetup.boundKey(HammerKey.CYCLE_MINING_MODE.mapping());
            KeyMapping.set(key, true);
            KeyMapping.click(key);
        });
        t.awaitClient("Show both Hammer wheels", mc -> Playtest.screen(mc) instanceof HammerMenuScreen);
    }

    static void releaseWheel(Minecraft mc) {
        var mapped = ClientSetup.boundKey(HammerKey.CYCLE_MINING_MODE.mapping());
        Playtest.require(Playtest.screen(mc) instanceof HammerMenuScreen, "Hammer wheel closed before releasing the bound key");
        // A screen receives the physical key-up even when opening it cleared the game's key mappings.
        Playtest.require(Playtest.screen(mc).keyReleased(new KeyEvent(mapped.getValue(), 0, 0)), "Wheel ignored the mapped release");
        Playtest.require(Playtest.screen(mc) == null, "Mapped release left the wheel open");
    }

    private static void openSettings(Playtest t) {
        openMenu(t);
        t.client("Open Settings from the right wheel", mc -> HammerRadialPlaytest.select(mc, "settings"));
    }

    private static void clickConfigButton(Minecraft mc, String setting) {
        click(mc, configButton(mc, setting));
    }

    static void click(Minecraft mc, Button button) {
        var click = new MouseButtonEvent(button.getX() + button.getWidth() / 2.0,
                button.getY() + button.getHeight() / 2.0, new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0));
        Playtest.require(Playtest.screen(mc).mouseClicked(click, false), "Ignored button " + button.getMessage().getString());
        if (Playtest.screen(mc) != null) Playtest.screen(mc).mouseReleased(click);
    }

    private static Button configButton(Minecraft mc, String setting) {
        return button(mc, Component.translatable("gui.hammer.tool_config." + setting));
    }

    static Button button(Minecraft mc, Component label) {
        String expected = label.getString();
        return Playtest.screen(mc).children().stream()
                .filter(Button.class::isInstance).map(Button.class::cast)
                .filter(button -> button.getMessage().getString().equals(expected))
                .findFirst().orElseThrow(() -> new AssertionError("Missing Hammer menu choice: " + expected));
    }

    private static boolean aimedAt(Minecraft mc, BlockPos center, Direction face) {
        return mc.hitResult instanceof BlockHitResult hit && hit.getBlockPos().equals(center)
                && hit.getDirection() == face && mc.player.onGround();
    }

    private static void minePlane(Playtest t, BlockPos center, Direction face, String label) {
        t.server("Aim at the " + label, p ->
                p.teleportTo(p.level(), 0.5, 201, 0.5, Set.of(), 0f, face == Direction.UP ? 90f : 0f, true));
        t.awaitClient("Target the " + label + " center on the client", mc -> aimedAt(mc, center, face));
        t.awaitClient("Extract the custom mining outline for the " + label,
                mc -> aimedAt(mc, center, face) && center.equals(lastCustomOutline));
        t.screenshot("hammer-" + label);
        t.client("Attack the center of the " + label, mc ->
                Playtest.require(mc.gameMode.startDestroyBlock(center, face), "Client refused to mine " + center));
        t.awaitServer("Mine the " + label + " center", p -> p.level().getBlockState(center).isAir());
        t.server("Verify all nine " + label + " blocks, the stone perimeter and the backing layer", p -> {
            for (int a = -2; a <= 2; a++) {
                for (int b = -2; b <= 2; b++) {
                    BlockPos pos = face == Direction.UP ? center.offset(a, 0, b) : center.offset(a, b, 0);
                    var state = p.level().getBlockState(pos);
                    boolean inside = Math.abs(a) <= 1 && Math.abs(b) <= 1;
                    Playtest.require(inside ? state.isAir() : state.is(Blocks.STONE),
                            "Wrong block at " + pos + ": expected " + (inside ? "air" : "stone") + ", got " + state);
                    BlockPos backing = pos.relative(face.getOpposite());
                    Playtest.require(p.level().getBlockState(backing).is(Blocks.STONE),
                            "Mining broke the backing layer at " + backing);
                }
            }
        });
    }
}
