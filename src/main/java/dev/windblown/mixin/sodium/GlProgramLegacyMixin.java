package dev.windblown.mixin.sodium;

import dev.windblown.client.RawUniforms;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "me.jellysquid.mods.sodium.client.gl.shader.GlProgram", remap = false, priority = 1500)
abstract class GlProgramLegacyMixin {
    @Inject(method = "bind", at = @At("TAIL"), remap = false, require = 0)
    private void windblown$bind(CallbackInfo ci) {
        RawUniforms.onCurrentProgram();
    }
}
