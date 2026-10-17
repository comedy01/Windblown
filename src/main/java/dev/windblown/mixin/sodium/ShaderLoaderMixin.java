package dev.windblown.mixin.sodium;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.windblown.client.ShaderPatches;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.gl.shader.ShaderLoader", remap = false, priority = 1500)
abstract class ShaderLoaderMixin {
    @ModifyExpressionValue(method = "loadShader", at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/gl/shader/ShaderLoader;getShaderSource", remap = false), remap = false, require = 0)
    private static String windblown$source(String source) {
        return ShaderPatches.patch("sodium", source);
    }
}
