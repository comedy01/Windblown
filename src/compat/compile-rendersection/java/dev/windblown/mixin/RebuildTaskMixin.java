package dev.windblown.mixin;

import dev.windblown.client.Foliage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.client.renderer.chunk.SectionRenderDispatcher$RenderSection$RebuildTask")
abstract class RebuildTaskMixin {
    @Inject(method = "compile", at = @At("HEAD"))
    private void windblown$enter(CallbackInfoReturnable<?> cir) {
        Foliage.setCompiling(true);
    }

    @Inject(method = "compile", at = @At("RETURN"))
    private void windblown$exit(CallbackInfoReturnable<?> cir) {
        Foliage.setCompiling(false);
    }
}
