package net.j40climb.florafauna_hammer.common.item.abilities;

import net.minecraft.server.level.ServerPlayer;

final class SlamCooldown {
    private static final String READY_AT = "florafauna_hammer.slam_ready_at";

    static long readyAt(ServerPlayer player) {
        return player.getPersistentData().getLongOr(READY_AT, 0L);
    }

    static void setReadyAt(ServerPlayer player, long tick) {
        player.getPersistentData().putLong(READY_AT, tick);
    }
}
