package net.j40climb.florafauna_hammer.mixin.client;

import net.j40climb.florafauna_hammer.client.KeyInputEvents;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
abstract class HammerUseMixin {
    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void hammer$skipHeldUseRepeat(CallbackInfo ci) {
        if (KeyInputEvents.suppressRepeatedUse()) ci.cancel();
    }
}
