package net.j40climb.florafauna_hammer.common.item.abilities;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

final class SurfaceTeleport {
    private SurfaceTeleport() {}

    static void teleportToSurface(ServerPlayer player) {
        Level level = player.level();

        BlockPos playerPos = player.blockPosition();
        BlockPos.MutableBlockPos scanPos = playerPos.mutable();

        // Scan upward for the surface. Start by looking at the block above the player's head (i.e., +2)
        for (int y = playerPos.getY() + 2; y <= level.getMaxY() - 2; y++) {
            scanPos.setY(y);
            if (level.getBlockState(scanPos).isAir() || !level.getFluidState(scanPos).isEmpty()) continue;
            BlockPos feet = scanPos.above();
            if (!level.getBlockState(feet).isAir() || !level.getBlockState(feet.above()).isAir()) continue;

            Vec3 landing = DismountHelper.findSafeDismountLocation(player.getType(), level, feet, true);
            if (landing == null) continue;

            player.teleportTo(landing.x, landing.y, landing.z);
            player.resetFallDistance();
            player.sendOverlayMessage(Component.translatable("ability.hammer.teleport_surface.success"));
            return;
        }

        // No surface found
        player.sendOverlayMessage(Component.translatable("ability.hammer.teleport_surface.no_surface"));
    }
}
