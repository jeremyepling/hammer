package net.j40climb.florafauna_hammer.mixin.client;

import net.j40climb.florafauna_hammer.client.KeyInputEvents;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
abstract class HammerKeyboardMixin {
    @Inject(method = "keyPress", at = @At("RETURN"))
    private void hammer$controlInput(long window, int action, KeyEvent event, CallbackInfo ci) {
        KeyInputEvents.onControlKey(event, action);
    }
}
