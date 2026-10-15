package dev.windblown.selftest.mixin;

import dev.windblown.selftest.WindblownSelfTest;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
abstract class GameRendererHandMixin {
    @Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
    private void windblownSelftest$hideHand(CallbackInfo ci) {
        if (WindblownSelfTest.hideHud) {
            ci.cancel();
        }
    }
}
