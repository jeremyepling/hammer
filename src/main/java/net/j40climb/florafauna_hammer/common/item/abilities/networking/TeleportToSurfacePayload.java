package net.j40climb.florafauna_hammer.common.item.abilities.networking;

import io.netty.buffer.ByteBuf;
import net.j40climb.florafauna_hammer.HammerMod;
import net.j40climb.florafauna_hammer.common.item.abilities.data.HammerAction;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public enum TeleportToSurfacePayload implements CustomPacketPayload {
    INSTANCE;

    public static final Type<TeleportToSurfacePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(HammerMod.MOD_ID, "teleport_to_surface_payload"));
    public static final StreamCodec<ByteBuf, TeleportToSurfacePayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    public static void onServerReceived(final TeleportToSurfacePayload data, final ServerPlayer player) {
        if (HammerAction.TELEPORT.availableOn(player.getMainHandItem())) {
            teleportToSurface(player);
        }
    }

    public static void teleportToSurface(Player player) {
        Level level = player.level();
        if (level.isClientSide()) return;

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

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
