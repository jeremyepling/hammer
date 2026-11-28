package net.j40climb.florafauna_hammer.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.j40climb.florafauna_hammer.common.item.abilities.data.MiningShape;
import net.j40climb.florafauna_hammer.common.item.abilities.multiblock.MultiBlockBreaker;
import net.j40climb.florafauna_hammer.common.item.abilities.multiblock.RemoteOreStrike;
import net.j40climb.florafauna_hammer.common.item.abilities.multiblock.MultiBlockPatterns;
import net.j40climb.florafauna_hammer.setup.HammerRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import org.jspecify.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

@Mixin(ServerPlayerGameMode.class)
abstract class HammerMiningMixin {
    @Shadow @Final protected ServerPlayer player;
    @Unique private Direction hammer$activeFace = Direction.UP;
    @Unique private Direction hammer$delayedFace = Direction.UP;
    @Unique private @Nullable BlockPos hammer$patternTarget;
    @Unique private Direction hammer$patternFace = Direction.UP;

    // Capture only actions vanilla has accepted into its active and delayed break slots.
    @Inject(method = "handleBlockBreakAction", at = @At(value = "FIELD",
            target = "Lnet/minecraft/server/level/ServerPlayerGameMode;destroyPos:Lnet/minecraft/core/BlockPos;",
            opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER))
    private void hammer$rememberActiveFace(BlockPos pos, ServerboundPlayerActionPacket.Action action, Direction face,
                                           int maxY, int sequence, CallbackInfo ci) {
        hammer$activeFace = face;
    }

    @Inject(method = "handleBlockBreakAction", at = @At(value = "FIELD",
            target = "Lnet/minecraft/server/level/ServerPlayerGameMode;delayedDestroyPos:Lnet/minecraft/core/BlockPos;",
            opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER))
    private void hammer$rememberDelayedFace(CallbackInfo ci) {
        hammer$delayedFace = hammer$activeFace;
    }

    @WrapOperation(method = "handleBlockBreakAction", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayerGameMode;destroyAndAck(Lnet/minecraft/core/BlockPos;ILjava/lang/String;)V"))
    private void hammer$finishAcknowledgedHit(ServerPlayerGameMode gameMode, BlockPos pos, int sequence, String reason,
                                              Operation<Void> original,
                                              @Local(argsOnly = true) ServerboundPlayerActionPacket.Action action,
                                              @Local(argsOnly = true) Direction face) {
        Direction hitFace = action == ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK ? face : hammer$activeFace;
        hammer$withPatternHit(pos, hitFace, () -> original.call(gameMode, pos, sequence, reason));
    }

    @WrapOperation(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayerGameMode;destroyBlock(Lnet/minecraft/core/BlockPos;)Z"))
    private boolean hammer$finishDelayedHit(ServerPlayerGameMode gameMode, BlockPos pos, Operation<Boolean> original) {
        return hammer$withPatternHit(pos, hammer$delayedFace, () -> original.call(gameMode, pos));
    }

    @Unique
    private <T> T hammer$withPatternHit(BlockPos pos, Direction face, Supplier<T> action) {
        BlockPos previousTarget = hammer$patternTarget;
        Direction previousFace = hammer$patternFace;
        hammer$patternTarget = pos;
        hammer$patternFace = face;
        try {
            return action.get();
        } finally {
            hammer$patternTarget = previousTarget;
            hammer$patternFace = previousFace;
        }
    }

    @WrapMethod(method = "destroyBlock")
    private boolean hammer$minePattern(BlockPos pos, Operation<Boolean> original) {
        if (RemoteOreStrike.toolFor(player) != null) return original.call(pos);
        if (!pos.equals(hammer$patternTarget)) return original.call(pos);
        // Consume this player's hit once. Recursive neighbor breaks have no pending hit.
        hammer$patternTarget = null;
        var tool = player.getMainHandItem();
        var mode = tool.get(HammerRegistry.MULTI_BLOCK_MINING.get());
        if (mode == null || mode.shape() == MiningShape.SINGLE) return original.call(pos);
        var level = player.level();
        var before = level.getBlockState(pos);
        var plan = MultiBlockPatterns.plan(player, tool, pos, hammer$patternFace);
        boolean accepted = original.call(pos);
        // In 26.1 destroyBlock can return true even if onDestroyedByPlayer kept the block.
        if (accepted && !level.getBlockState(pos).is(before.getBlock()) && !plan.blocks().isEmpty()) {
            MultiBlockBreaker.finish(player, tool, mode, pos, plan);
        }
        return accepted;
    }
}
