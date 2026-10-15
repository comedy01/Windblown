package dev.windblown.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.windblown.client.Foliage;
import dev.windblown.client.WindblownClient;
import dev.windblown.core.WindCode;
import net.fabricmc.fabric.impl.client.indigo.renderer.mesh.MutableQuadViewImpl;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.BlockRenderInfo;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.fabricmc.fabric.impl.client.indigo.renderer.render.AbstractQuadRenderer", remap = false)
abstract class IndigoQuadMixin {
    @Shadow(remap = false)
    @Final
    protected BlockRenderInfo blockInfo;

    @Inject(method = "renderQuad", at = @At("HEAD"), remap = false, require = 0)
    private void windblown$tag(CallbackInfo ci, @Local(argsOnly = true) MutableQuadViewImpl quad) {
        if (!Foliage.compiling() || blockInfo == null || blockInfo.blockState == null) {
            return;
        }
        try {
            Foliage.Sway sway = Foliage.begin(blockInfo.blockState, blockInfo.blockView, blockInfo.blockPos);
            if (sway == null) {
                return;
            }
            for (int i = 0; i < 4; i++) {
                quad.spriteColor(i, 0, WindCode.withAlpha(quad.spriteColor(i, 0), Foliage.alpha(sway, quad.y(i))));
            }
        } catch (RuntimeException e) {
            WindblownClient.fail(e);
        }
    }
}
