package net.j40climb.florafauna_hammer.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.j40climb.florafauna_hammer.client.KeyInputEvents;
import net.j40climb.florafauna_hammer.setup.HammerKey;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
abstract class HammerUseMixin {
    @WrapOperation(method = "handleKeybinds", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/KeyMapping;consumeClick()Z"))
    private boolean hammer$consumeUseClick(KeyMapping mapping, Operation<Boolean> original) {
        boolean clicked = original.call(mapping);
        var mc = (Minecraft) (Object) this;
        if (mapping != mc.options.keyUse || !KeyInputEvents.canUseHammerBinding(mc.player)) return clicked;
        var alternate = HammerKey.USE_HAMMER.mapping();
        return (!alternate.isUnbound() && alternate.consumeClick()) || clicked;
    }

    @WrapOperation(method = "handleKeybinds", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/KeyMapping;isDown()Z"))
    private boolean hammer$isUseDown(KeyMapping mapping, Operation<Boolean> original) {
        var mc = (Minecraft) (Object) this;
        return original.call(mapping) || (mapping == mc.options.keyUse && KeyInputEvents.hammerBindingDown(mc.player));
    }

    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void hammer$skipHeldUseRepeat(CallbackInfo ci) {
        if (KeyInputEvents.suppressRepeatedUse()) ci.cancel();
    }
}
