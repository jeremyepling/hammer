package net.j40climb.florafauna_hammer.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.j40climb.florafauna_hammer.common.item.abilities.GroundSlam;
import net.j40climb.florafauna_hammer.common.item.abilities.ToolAbilityEventHandlers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Player.class)
abstract class FabricHammerPlayerMixin {
    @ModifyReturnValue(method = "getDestroySpeed", at = @At("RETURN"))
    private float florafauna$hammerBreakSpeed(float speed) {
        return ToolAbilityEventHandlers.breakSpeed(((Player)(Object)this).getMainHandItem(), speed);
    }

    @ModifyVariable(method = "causeFallDamage", at = @At("HEAD"), argsOnly = true)
    private double florafauna$cushionSlam(double distance) {
        return (Object)this instanceof ServerPlayer player ? GroundSlam.fallDistance(player, distance) : distance;
    }
}
