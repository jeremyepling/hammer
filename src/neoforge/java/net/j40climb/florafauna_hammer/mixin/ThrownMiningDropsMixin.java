package net.j40climb.florafauna_hammer.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.j40climb.florafauna_hammer.common.item.abilities.multiblock.RemoteOreStrike;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.neoforge.common.CommonHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(CommonHooks.class)
abstract class ThrownMiningDropsMixin {
    // NeoForge has already applied BlockDropsEvent cancellation and drop edits here.
    @WrapOperation(method = "handleBlockDrops", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private static boolean hammer$collectResource(ServerLevel level, Entity entity, Operation<Boolean> original,
                                                  @Local(argsOnly = true) BlockPos pos) {
        if (entity instanceof ItemEntity drop && RemoteOreStrike.collectDrop(level, pos, drop.getItem())) return true;
        return original.call(level, entity);
    }
}
