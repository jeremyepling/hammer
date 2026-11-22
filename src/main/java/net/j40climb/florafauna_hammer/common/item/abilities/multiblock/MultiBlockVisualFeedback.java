package net.j40climb.florafauna_hammer.common.item.abilities.multiblock;

import net.j40climb.florafauna_hammer.HammerMod;
import net.j40climb.florafauna_hammer.common.item.abilities.data.MiningModeData;
import net.j40climb.florafauna_hammer.setup.HammerRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/** The local player's additional block cracks, cleared using the positions actually drawn. */
public final class MultiBlockVisualFeedback {
    private static BlockPos target = BlockPos.ZERO;
    private static Direction hitFace = Direction.UP;
    private static int ticksMining;
    private static List<BlockPos> cracks = List.of();
    private static MiningModeData crackMode;

    private MultiBlockVisualFeedback() {}

    public static void start(Player player, BlockPos pos, Direction face) {
        clear(player);
        target = pos.immutable();
        hitFace = face;
    }

    public static void hold(Player player, BlockPos pos) {
        if (!pos.equals(target) || !player.getMainHandItem().has(HammerRegistry.MULTI_BLOCK_MINING.get())) {
            clear(player);
            return;
        }
        var mode = MiningModeData.effective(player.getMainHandItem());
        if (ticksMining == 0 || !mode.equals(crackMode)) {
            draw(player, -1);
            crackMode = mode;
            cracks = MultiBlockPatterns.plan(player, player.getMainHandItem(), target, hitFace).blocks().stream()
                    .filter(neighbor -> !neighbor.equals(target)).toList();
        }
        ticksMining++;
        var state = player.level().getBlockState(target);
        float progress = state.getDestroyProgress(player, player.level(), target) * ticksMining;
        draw(player, Math.min(9, (int) (progress * 10)));
    }

    public static void onClientTick() {
        if (cracks.isEmpty()) return;
        var client = Minecraft.getInstance();
        if (client.player == null || client.gameMode == null) {
            cracks = List.of();
        } else if (!client.gameMode.isDestroying()) {
            clear(client.player);
        }
    }

    static Direction previewFace(BlockPos pos, Direction hoveredFace) {
        var gameMode = Minecraft.getInstance().gameMode;
        return gameMode != null && gameMode.isDestroying() && pos.equals(target) ? hitFace : hoveredFace;
    }

    public static void clear(Player player) {
        draw(player, -1);
        cracks = List.of();
        ticksMining = 0;
    }

    private static void draw(Player player, int stage) {
        // Local overlays use negative IDs. Distinct vein positions can share BlockPos.hashCode().
        for (int i = 0; i < cracks.size(); i++) {
            player.level().destroyBlockProgress(-1 - i, cracks.get(i), stage);
        }
    }
}
