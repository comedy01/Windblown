package dev.windblown.mixin;

import com.mojang.blaze3d.vertex.QuadInstance;
import dev.windblown.client.Foliage;
import dev.windblown.client.WindblownClient;
import dev.windblown.core.WindCode;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelBlockRenderer.class)
abstract class ModelBlockRendererMixin {
    @Shadow
    @Final
    private QuadInstance quadInstance;

    @Inject(method = "putQuadWithTint", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/block/BlockQuadOutput;put(FFFLnet/minecraft/client/resources/model/geometry/BakedQuad;Lcom/mojang/blaze3d/vertex/QuadInstance;)V"))
    private void windblown$tag(BlockQuadOutput output, float x, float y, float z, BlockAndTintGetter level, BlockState state,
                               BlockPos pos, BakedQuad quad, CallbackInfo ci) {
        if (!Foliage.compiling()) {
            return;
        }
        try {
            Foliage.Sway sway = Foliage.begin(state, level, pos);
            if (sway == null) {
                return;
            }
            for (int i = 0; i < 4; i++) {
                quadInstance.setColor(i, WindCode.withAlpha(quadInstance.getColor(i), Foliage.alpha(sway, quad.position(i).y())));
            }
        } catch (RuntimeException e) {
            WindblownClient.fail(e);
        }
    }
}
