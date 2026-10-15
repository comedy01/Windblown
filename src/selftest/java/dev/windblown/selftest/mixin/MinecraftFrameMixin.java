package dev.windblown.selftest.mixin;

import dev.windblown.selftest.WindblownSelfTest;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
abstract class MinecraftFrameMixin {
    @Inject(method = "runTick", at = @At("HEAD"))
    private void windblownSelftest$frame(boolean advanceGameTime, CallbackInfo ci) {
        WindblownSelfTest.frame();
    }
}
