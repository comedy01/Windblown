package dev.windblown.mixin.sodium;

import dev.windblown.client.ShaderPatches;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "me.jellysquid.mods.sodium.client.gl.shader.ShaderLoader", remap = false, priority = 500)
abstract class ShaderLoaderLegacyMixin {
    @Inject(method = "getShaderSource", at = @At("RETURN"), cancellable = true, remap = false, require = 0)
    private static void windblown$source(CallbackInfoReturnable<String> cir) {
        cir.setReturnValue(ShaderPatches.patch("sodium", cir.getReturnValue()));
    }
}
