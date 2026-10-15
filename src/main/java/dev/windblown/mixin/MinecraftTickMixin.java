package dev.windblown.mixin;

import dev.windblown.client.WindblownClient;
import dev.windblown.client.RawUniforms;
import dev.windblown.client.Rustle;
import dev.windblown.client.WindState;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
abstract class MinecraftTickMixin {
    @Inject(method = "runTick", at = @At("HEAD"))
    private void windblown$frame(boolean advanceGameTime, CallbackInfo ci) {
        RawUniforms.newFrame();
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void windblown$tick(CallbackInfo ci) {
        try {
            WindState.tick((Minecraft) (Object) this);
            Rustle.tick((Minecraft) (Object) this);
        } catch (RuntimeException e) {
            WindblownClient.fail(e);
        }
    }
}
