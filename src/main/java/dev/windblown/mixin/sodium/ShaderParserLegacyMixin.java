package dev.windblown.mixin.sodium;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.windblown.client.ShaderPatches;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "me.jellysquid.mods.sodium.client.gl.shader.ShaderParser", remap = false, priority = 1500)
abstract class ShaderParserLegacyMixin {
    @ModifyExpressionValue(method = "resolveImport", at = @At(value = "INVOKE", target = "Lme/jellysquid/mods/sodium/client/gl/shader/ShaderLoader;getShaderSource", remap = false), remap = false, require = 0)
    private static String windblown$include(String source) {
        return ShaderPatches.patch("sodium", source);
    }

    @ModifyExpressionValue(method = "processImport", at = @At(value = "INVOKE", target = "Lme/jellysquid/mods/sodium/client/gl/shader/ShaderLoader;getShaderSource", remap = false), remap = false, require = 0)
    private String windblown$import(String source) {
        return ShaderPatches.patch("sodium", source);
    }
}
