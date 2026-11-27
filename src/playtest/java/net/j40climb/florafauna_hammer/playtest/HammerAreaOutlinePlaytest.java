package net.j40climb.florafauna_hammer.playtest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

final class HammerAreaOutlinePlaytest {
    static void steps(Playtest t, BlockPos wall) {
        t.server("Position the player far enough back to see the full 3×3 wall", p -> {
            double x = wall.getX() + 0.5;
            double y = wall.getY() - 1;
            double z = wall.getZ() - 3.5;
            p.level().setBlockAndUpdate(BlockPos.containing(x, y - 1, z), Blocks.STONE.defaultBlockState());
            p.teleportTo(p.level(), x, y, z, Set.of(), 0, 0, true);
        });
        t.awaitClient("Aim at the center of the 3×3 wall without mining", mc ->
                mc.player.onGround() && !mc.gameMode.isDestroying()
                        && mc.hitResult instanceof BlockHitResult hit
                        && hit.getBlockPos().equals(wall) && hit.getDirection() == Direction.NORTH);
        t.screenshot("hammer-3x3-before-mining");
        t.awaitClient("See the eight neighboring outlines and the seams around the center", mc -> {
            BufferedImage image;
            try {
                image = ImageIO.read(mc.gameDirectory.toPath().resolve("screenshots/hammer-3x3-before-mining.png").toFile());
            } catch (IOException e) {
                return false; // The screenshot writer may still be encoding the frame.
            }
            if (image == null) return false;
            for (Vec3 edge : frontEdges(wall)) {
                Vec3 projected = mc.gameRenderer.projectPointToScreen(edge);
                Playtest.require(hasVioletNear(image, projected), "Missing 3×3 edge before mining at " + edge);
            }
            return true;
        });
    }

    private static List<Vec3> frontEdges(BlockPos wall) {
        double x = wall.getX();
        double y = wall.getY();
        double z = wall.getZ();
        List<Vec3> edges = new ArrayList<>();
        for (int row = -1; row <= 1; row++) {
            edges.add(new Vec3(x - 1, y + row + 0.5, z));
            edges.add(new Vec3(x + 2, y + row + 0.5, z));
        }
        edges.add(new Vec3(x + 0.5, y - 1, z));
        edges.add(new Vec3(x + 0.5, y + 2, z));
        // Checking the interior prevents a single outline around the whole square passing.
        edges.add(new Vec3(x, y + 0.5, z));
        edges.add(new Vec3(x + 1, y + 0.5, z));
        edges.add(new Vec3(x + 0.5, y, z));
        edges.add(new Vec3(x + 0.5, y + 1, z));
        return edges;
    }

    private static boolean hasVioletNear(BufferedImage image, Vec3 projected) {
        int cx = (int) Math.round((projected.x + 1) * image.getWidth() / 2);
        int cy = (int) Math.round((1 - projected.y) * image.getHeight() / 2);
        int pixels = 0;
        for (int y = Math.max(0, cy - 4); y < Math.min(image.getHeight(), cy + 5); y++) {
            for (int x = Math.max(0, cx - 4); x < Math.min(image.getWidth(), cx + 5); x++) {
                int rgb = image.getRGB(x, y);
                int r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255;
                if (r >= g + 5 && b >= r + 10 && ++pixels >= 2) return true;
            }
        }
        return false;
    }

    private HammerAreaOutlinePlaytest() {}
}
