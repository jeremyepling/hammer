package net.j40climb.florafauna_hammer.mixin.client;

import net.j40climb.florafauna_hammer.common.item.abilities.multiblock.MultiBlockVisualFeedback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
abstract class HammerClientMiningMixin {
    @Inject(method = "startDestroyBlock", at = @At("HEAD"))
    private void hammer$start(BlockPos pos, Direction face, CallbackInfoReturnable<Boolean> ci) {
        MultiBlockVisualFeedback.start(Minecraft.getInstance().player, pos, face);
    }

    @Inject(method = "continueDestroyBlock", at = @At("HEAD"))
    private void hammer$hold(BlockPos pos, Direction face, CallbackInfoReturnable<Boolean> ci) {
        MultiBlockVisualFeedback.hold(Minecraft.getInstance().player, pos);
    }
}
