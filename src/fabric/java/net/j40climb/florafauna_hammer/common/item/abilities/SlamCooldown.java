package net.j40climb.florafauna_hammer.common.item.abilities;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.j40climb.florafauna_hammer.HammerMod;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

final class SlamCooldown {
    private static final AttachmentType<Long> READY_AT = AttachmentRegistry.createPersistent(
            Identifier.fromNamespaceAndPath(HammerMod.MOD_ID, "slam_ready_at"), Codec.LONG);

    static void initialize() {
        // Register the persistent attachment before player data is loaded.
    }

    static long readyAt(ServerPlayer player) {
        return player.getAttachedOrElse(READY_AT, 0L);
    }

    static void setReadyAt(ServerPlayer player, long tick) {
        player.setAttached(READY_AT, tick);
    }
}
