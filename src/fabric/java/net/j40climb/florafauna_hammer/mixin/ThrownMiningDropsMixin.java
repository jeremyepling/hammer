package net.j40climb.florafauna_hammer.mixin;

import net.j40climb.florafauna_hammer.common.item.abilities.multiblock.RemoteOreStrike;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.gamerules.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Block.class)
abstract class ThrownMiningDropsMixin {
    @Inject(method = "popResource(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;)V",
            at = @At("HEAD"), cancellable = true)
    private static void hammer$collectResource(Level level, BlockPos pos, ItemStack stack, CallbackInfo ci) {
        if (hammer$collect(level, pos, stack)) ci.cancel();
    }

    @Inject(method = "popResourceFromFace", at = @At("HEAD"), cancellable = true)
    private static void hammer$collectFaceResource(Level level, BlockPos pos, Direction face, ItemStack stack, CallbackInfo ci) {
        if (hammer$collect(level, pos, stack)) ci.cancel();
    }

    private static boolean hammer$collect(Level level, BlockPos pos, ItemStack stack) {
        return level instanceof ServerLevel server && server.getGameRules().get(GameRules.BLOCK_DROPS)
                && RemoteOreStrike.collectDrop(level, pos, stack);
    }
}
