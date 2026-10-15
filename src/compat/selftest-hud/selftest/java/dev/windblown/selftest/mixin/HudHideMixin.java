package dev.windblown.selftest.mixin;

import dev.windblown.selftest.WindblownSelfTest;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
abstract class HudHideMixin {
    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void windblownSelftest$hide(GuiGraphicsExtractor graphics, DeltaTracker delta, CallbackInfo ci) {
        if (WindblownSelfTest.hideHud) {
            ci.cancel();
        }
    }
}
