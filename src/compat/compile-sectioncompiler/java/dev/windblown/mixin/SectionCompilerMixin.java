package dev.windblown.mixin;

import dev.windblown.client.Foliage;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SectionCompiler.class)
abstract class SectionCompilerMixin {
    @Inject(method = "compile*", at = @At("HEAD"))
    private void windblown$enter(CallbackInfoReturnable<SectionCompiler.Results> cir) {
        Foliage.setCompiling(true);
    }

    @Inject(method = "compile*", at = @At("RETURN"))
    private void windblown$exit(CallbackInfoReturnable<SectionCompiler.Results> cir) {
        Foliage.setCompiling(false);
    }
}
