package net.j40climb.florafauna_hammer.playtest;

import net.j40climb.florafauna_hammer.common.item.abilities.data.HammerAction;
import net.j40climb.florafauna_hammer.common.item.hammer.HammerItem;
import net.j40climb.florafauna_hammer.common.item.hammer.HammerStage;
import net.j40climb.florafauna_hammer.setup.HammerRegistry;
import net.j40climb.florafauna_hammer.setup.ClientSetup;
import net.j40climb.florafauna_hammer.setup.HammerKey;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
//? if >=26.3 {
/*import net.minecraft.world.entity.EntityTypes;*/
//?} else {
import net.minecraft.world.entity.EntityType;
//?}
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

/** A buried deposit behind a monochrome wall makes its private HUD echo measurable in the framebuffer. */
final class HammerPowerPlaytest {
    private static final BlockPos CHEST = new BlockPos(0, 202, 3);
    private static final BlockPos ORE = new BlockPos(2, 203, 8);
    private static final BlockPos LIGHTNING_TARGET = new BlockPos(8, 202, 0);
    private static final BlockPos TELEPORT_ROOF = new BlockPos(0, 205, 0);

    //? if >=26.3 {
    /*private static final Block SCANNER_WALL = Blocks.CONCRETE.gray();*/
    //?} else {
    private static final Block SCANNER_WALL = Blocks.GRAY_CONCRETE;
    //?}

    static void steps(Playtest t) {
        t.server("Build the scanner wall, chest and hidden diamond deposit", p -> {
            var level = p.level();
            for (var pos : BlockPos.betweenClosed(-5, 200, -4, 5, 206, 9)) {
                var block = pos.getY() == 200 || pos.getZ() == 3 ? SCANNER_WALL : Blocks.AIR;
                level.setBlockAndUpdate(pos, block.defaultBlockState());
            }
            level.setBlockAndUpdate(CHEST, Blocks.CHEST.defaultBlockState());
            level.setBlockAndUpdate(CHEST.above(), Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(ORE, Blocks.DIAMOND_ORE.defaultBlockState());
            p.setGameMode(GameType.SURVIVAL);
            p.getInventory().clearContent();
            p.getInventory().setSelectedSlot(0);
            p.setItemInHand(InteractionHand.MAIN_HAND, HammerStage.RESONANT.createStack());
            p.teleportTo(level, 0.5, 201, 0.5, Set.of(), 0f, 0f, true);
        });
        t.awaitClient("Hold a fresh Hammer facing the scanner wall", mc ->
                mc.player.getMainHandItem().is(HammerRegistry.HAMMER.get()) && mc.player.onGround()
                        && mc.hitResult instanceof BlockHitResult hit && hit.getBlockPos().equals(CHEST));
        t.client("Set the first-person view", mc -> {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            Playtest.showHud(mc);
        });
        HammerMenuPlaytest.choose(t, HammerAction.SCAN);
        t.client("Use a chest with Seismic Scan selected", mc ->
                mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, (BlockHitResult) mc.hitResult));
        t.awaitClient("Open the chest and preserve all charge", mc -> Playtest.screen(mc) instanceof ContainerScreen
                && HammerItem.charge(mc.player.getMainHandItem()) == 64);
        t.client("Close the chest and aim at the wall", mc -> {
            mc.player.closeContainer();
            mc.player.setYRot(-23);
        });
        t.awaitClient("Target plain concrete", mc -> mc.hitResult instanceof BlockHitResult hit
                && mc.level.getBlockState(hit.getBlockPos()).is(SCANNER_WALL));
        long[] fired = {0};
        t.client("Scan the wall", mc -> {
            fired[0] = mc.level.getGameTime();
            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, (BlockHitResult) mc.hitResult);
        });
        t.awaitServer("Spend 16 scanner charge", p -> HammerItem.charge(p.getMainHandItem()) == 48
                || HammerItem.charge(p.getMainHandItem()) == 49);
        t.screenshot("resonant-seismic-pulse-first");
        t.awaitClient("Allow the pulse to cross the wall", mc -> mc.level.getGameTime() >= fired[0] + 38);
        t.screenshot("seismic-echo");
        t.awaitClient("Allow the deposit echo to expire", mc -> mc.level.getGameTime() >= fired[0] + 155);
        t.screenshot("seismic-expired");
        t.client("Compare the echo through the wall before and after expiry", mc -> {
            var projected = mc.gameRenderer.projectPointToScreen(new Vec3(3, 203, 9));
            var dir = mc.gameDirectory.toPath().resolve("screenshots");
            int visible = echoPixels(dir.resolve("seismic-echo.png"), projected);
            int expired = echoPixels(dir.resolve("seismic-expired.png"), projected);
            Playtest.require(visible >= 4 && expired == 0, "Unexpected violet marker pixel counts " + visible + " / " + expired);
        });
        Husk[] target = new Husk[1];
        t.server("Prepare a hostile target beside the landing position", p -> {
            //? if >=26.3 {
            /*target[0] = EntityTypes.HUSK.create(p.level(), EntitySpawnReason.TRIGGERED);*/
            //?} else {
            target[0] = EntityType.HUSK.create(p.level(), EntitySpawnReason.TRIGGERED);
            //?}
            target[0].setPos(p.position().add(2, 0, 0));
            target[0].setNoAi(true);
            p.level().addFreshEntity(target[0]);
            var tempest = HammerStage.TEMPEST.createStack();
            tempest.set(HammerRegistry.HAMMER_CHARGE.get(), 64);
            tempest.set(HammerRegistry.HAMMER_ACTION.get(), HammerAction.SCAN);
            p.setItemInHand(InteractionHand.MAIN_HAND, tempest);
        });
        t.client("Frame the approaching shockwave and jump from the scanner platform", mc -> {
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            mc.player.setXRot(24);
            mc.options.keyJump.setDown(true);
        });
        t.awaitClient("Use the selected scanner while jumping", mc -> {
            if (mc.player.onGround() || mc.player.getY() < 201.5) return false;
            mc.options.keyJump.setDown(false);
            mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
            return true;
        });
        t.awaitServer("Land the slam and damage the nearby hostile", p -> target[0].getHealth() < target[0].getMaxHealth()
                && HammerRegistry.pendingSlam(p) == null && HammerItem.charge(p.getMainHandItem()) >= 52
                && HammerItem.charge(p.getMainHandItem()) <= 54 && p.level().getBlockState(p.getOnPos()).is(SCANNER_WALL));
        t.screenshot("kinetic-impact");
        t.server("Build a ranged lightning target away from the player", p -> {
            p.level().setBlockAndUpdate(LIGHTNING_TARGET, Blocks.OBSIDIAN.defaultBlockState());
            p.teleportTo(p.level(), 0.5, 201, 0.5, Set.of(), -90f, 0f, true);
        });
        t.client("Restore first person for the lightning discharge", mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
        t.awaitClient("Aim down the lightning lane", mc -> mc.player.getYRot() == -90f
                && mc.level.getBlockState(LIGHTNING_TARGET).is(Blocks.OBSIDIAN));
        HammerMenuPlaytest.choose(t, HammerAction.LIGHTNING);
        t.client("Activate the selected Lightning action with vanilla Use", mc ->
                KeyMapping.click(ClientSetup.boundKey(mc.options.keyUse)));
        t.awaitClient("Observe a real bolt at the lightning target", mc -> !mc.level.getEntitiesOfClass(
                LightningBolt.class, mc.player.getBoundingBox().inflate(12),
                bolt -> bolt.blockPosition().equals(LIGHTNING_TARGET)).isEmpty());
        t.screenshot("tempest-lightning-discharge-first");
        t.server("Build a roof with a clear landing above the player", p -> {
            var level = p.level();
            for (var pos : BlockPos.betweenClosed(TELEPORT_ROOF.below(3), TELEPORT_ROOF.above(2))) {
                level.setBlockAndUpdate(pos, (pos.equals(TELEPORT_ROOF) ? Blocks.STONE : Blocks.AIR).defaultBlockState());
            }
        });
        t.awaitClient("Stand under the roof with the Hammer", mc -> mc.player.onGround()
                && mc.player.blockPosition().equals(TELEPORT_ROOF.below(4))
                && mc.level.getBlockState(TELEPORT_ROOF).is(Blocks.STONE));
        HammerMenuPlaytest.choose(t, HammerAction.TELEPORT);
        t.client("Activate the selected Surface Teleport with the optional Use Hammer binding", mc -> {
            Playtest.require(HammerKey.USE_HAMMER.mapping().isUnbound(), "Use Hammer must start unassigned");
            ChargedHammerPlaytest.bindHammerUse();
            KeyMapping.click(ClientSetup.boundKey(HammerKey.USE_HAMMER.mapping()));
        });
        t.awaitServer("Land safely on top of the roof with the same action selected", p -> p.onGround()
                && p.blockPosition().equals(TELEPORT_ROOF.above())
                && p.getMainHandItem().get(HammerRegistry.HAMMER_ACTION.get()) == HammerAction.TELEPORT);
        t.awaitClient("See the Hammer and the surface landing after teleport", mc ->
                mc.player.blockPosition().equals(TELEPORT_ROOF.above())
                        && mc.player.getMainHandItem().is(HammerRegistry.HAMMER.get()));
        t.server("Prepare a full Tempest Hammer for a charged ore strike", p ->
                p.getMainHandItem().set(HammerRegistry.HAMMER_CHARGE.get(), HammerStage.TEMPEST.capacity()));
        ChargedHammerPlaytest.steps(t);
    }

    private static int echoPixels(Path path, Vec3 projected) {
        try {
            var image = ImageIO.read(path.toFile());
            int cx = (int)((projected.x + 1) * image.getWidth() / 2);
            int cy = (int)((1 - projected.y) * image.getHeight() / 2);
            int count = 0;
            for (int y = Math.max(0, cy - 25); y < Math.min(cy + 26, image.getHeight()); y++) {
                for (int x = Math.max(0, cx - 25); x < Math.min(cx + 26, image.getWidth()); x++) {
                    int rgb = image.getRGB(x, y);
                    int r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255;
                    if (b > r + 10 && r > g + 8 && b > 90) count++;
                }
            }
            return count;
        } catch (IOException e) {
            throw new AssertionError("Cannot read scanner frame " + path, e);
        }
    }
}
