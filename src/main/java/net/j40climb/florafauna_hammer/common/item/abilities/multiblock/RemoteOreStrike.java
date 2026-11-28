package net.j40climb.florafauna_hammer.common.item.abilities.multiblock;

import net.j40climb.florafauna_hammer.common.item.abilities.data.MiningModeData;
import net.j40climb.florafauna_hammer.common.item.abilities.data.MiningShape;
import net.j40climb.florafauna_hammer.common.item.hammer.HammerItem;
import net.j40climb.florafauna_hammer.common.item.hammer.HammerStage;
import net.j40climb.florafauna_hammer.common.item.abilities.data.ToolConfig;
import net.j40climb.florafauna_hammer.setup.HammerRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;
import net.j40climb.florafauna_hammer.common.item.abilities.OreTags;
import org.jspecify.annotations.Nullable;

/** A synchronous player break sees the projectile's tool without moving or replacing any inventory slot. */
public final class RemoteOreStrike {
    private record Strike(ServerPlayer owner, ItemStack hammer, BlockPos pos, @Nullable List<ItemStack> drops) {}
    private static final ThreadLocal<Strike> ACTIVE = new ThreadLocal<>();

    private RemoteOreStrike() {}

    public static @Nullable ItemStack toolFor(Object entity) {
        Strike strike = ACTIVE.get();
        return strike != null && strike.owner == entity ? strike.hammer : null;
    }

    /** Called only for newly generated block loot, after the loader's drop rules. */
    public static boolean collectDrop(Level level, BlockPos pos, ItemStack stack) {
        Strike strike = ACTIVE.get();
        if (strike == null || strike.drops == null || strike.owner.level() != level || !strike.pos.equals(pos)) return false;
        if (!stack.isEmpty()) strike.drops.add(stack.copy());
        return true;
    }

    public static boolean breakThrownBlock(ServerLevel level, ServerPlayer owner, ItemStack tool, BlockPos pos,
                                           @Nullable List<ItemStack> drops) {
        boolean hammer = tool.is(HammerRegistry.HAMMER.get());
        int charge = hammer ? HammerItem.charge(tool) : 0;
        if (hammer && charge == 0) return false;
        if (owner.level() != level || !owner.isAlive() || owner.isSpectator() || tool.isEmpty()
                || !MultiBlockPatterns.canInteract(level, owner, pos)
                || owner.blockActionRestricted(level, pos, owner.gameMode.getGameModeForPlayer())) return false;

        var before = level.getBlockState(pos);
        if (before.isAir() || before.getDestroySpeed(level, pos) < 0) return false;

        var previous = ACTIVE.get();
        ACTIVE.set(new Strike(owner, tool, pos, hammer ? drops : null));
        try {
            // The player break invokes the loader's protection and harvest hooks.
            // A reported success can still leave the original block in place.
            boolean removed = owner.gameMode.destroyBlock(pos) && !level.getBlockState(pos).is(before.getBlock());
            if (removed && hammer) tool.set(HammerRegistry.HAMMER_CHARGE.get(), charge - 1);
            return removed;
        } finally {
            if (previous == null) ACTIVE.remove();
            else ACTIVE.set(previous);
        }
    }

    public static void hit(ServerPlayer owner, ItemStack hammer, BlockPos origin) {
        hit(owner, hammer, origin, null);
    }

    public static void hit(ServerPlayer owner, ItemStack hammer, BlockPos origin, @Nullable List<ItemStack> drops) {
        var level = owner.level();
        int charge = HammerItem.charge(hammer);
        if (!owner.isAlive() || owner.isSpectator() || !hammer.is(HammerRegistry.HAMMER.get()) || HammerStage.of(hammer) == HammerStage.FORGED || charge == 0
                || !MultiBlockPatterns.canInteract(level, owner, origin) || !level.getBlockState(origin).is(OreTags.ORES)) return;
        var settings = hammer.getOrDefault(HammerRegistry.MULTI_BLOCK_MINING.get(), MiningModeData.DEFAULT);
        var vein = new MiningModeData(MiningShape.SHAPELESS, MiningShape.SHAPELESS.getRadius(),
                settings.maxBlocksToBreak(), settings.ignoreToolRestrictions());
        if (!MultiBlockPatterns.isMineable(level, owner, hammer, vein, origin)) return;
        var originState = level.getBlockState(origin);
        var matches = MultiBlockPatterns.veinMatcher(originState, hammer.getOrDefault(HammerRegistry.TOOL_CONFIG.get(), ToolConfig.DEFAULT).exactVein());
        var positions = MultiBlockPatterns.vein(owner, hammer, vein, origin);
        for (BlockPos pos : positions) {
            if (!MultiBlockPatterns.canInteract(level, owner, pos)) continue;
            var before = level.getBlockState(pos);
            boolean mined = matches.test(before) && before.is(OreTags.ORES)
                    && MultiBlockPatterns.isMineable(level, owner, hammer, vein, pos)
                    && breakThrownBlock(level, owner, hammer, pos, drops);
            if (mined) {
                if (HammerItem.charge(hammer) == 0) break;
            } else if (pos.equals(origin)) {
                break;
            }
        }
    }
}
