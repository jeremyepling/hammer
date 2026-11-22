package net.j40climb.florafauna_hammer.mixin.client;

import net.j40climb.florafauna_hammer.client.KeyInputEvents;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
abstract class HammerMouseMixin {
    @Inject(method = "onButton", at = @At("RETURN"))
    private void hammer$controlInput(long window, MouseButtonInfo button, int action, CallbackInfo ci) {
        KeyInputEvents.onControlMouse(button.button(), action);
    }
}
