package net.j40climb.florafauna_hammer.common.item.abilities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.phys.Vec3;

final class SurfaceTeleport {
    private SurfaceTeleport() {}

    static void teleportToSurface(ServerPlayer player) {
        var level = player.level();

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

            Vec3 departure = player.position();
            level.playSound(null, departure.x, departure.y, departure.z,
                    SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.7F, 0.85F);
            level.sendParticles(ParticleTypes.PORTAL, departure.x, departure.y + 0.8, departure.z,
                    16, 0.32, 0.55, 0.32, 0.3);

            player.teleportTo(landing.x, landing.y, landing.z);
            player.resetFallDistance();
            level.playSound(null, landing.x, landing.y, landing.z,
                    SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 0.85F, 1.15F);
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, landing.x, landing.y + 0.8, landing.z,
                    20, 0.35, 0.55, 0.35, 0.035);
            player.sendOverlayMessage(Component.translatable("ability.hammer.teleport_surface.success"));
            return;
        }

        // No surface found
        player.sendOverlayMessage(Component.translatable("ability.hammer.teleport_surface.no_surface"));
    }
}
