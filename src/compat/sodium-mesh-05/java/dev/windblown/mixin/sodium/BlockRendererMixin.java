package dev.windblown.mixin.sodium;

import com.llamalad7.mixinextras.sugar.Local;
import dev.windblown.client.Foliage;
import dev.windblown.client.WindblownClient;
import dev.windblown.core.WindCode;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderContext;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(value = BlockRenderer.class, remap = false)
abstract class BlockRendererMixin {
    @Shadow
    @Final
    private ChunkVertexEncoder.Vertex[] vertices;

    @Unique
    private final Foliage.Sway windblown$sway = new Foliage.Sway();

    @Unique
    private boolean windblown$swaying;

    @Inject(method = "renderModel", at = @At("HEAD"), remap = false, require = 0)
    private void windblown$block(CallbackInfo ci, @Local(argsOnly = true) BlockRenderContext ctx) {
        windblown$swaying = false;
        try {
            Foliage.Sway sway = Foliage.begin(ctx.state(), ctx.world(), ctx.pos());
            if (sway != null) {
                windblown$sway.kind = sway.kind;
                windblown$sway.base = sway.base;
                windblown$sway.slope = sway.slope;
                windblown$swaying = true;
            }
        } catch (RuntimeException e) {
            WindblownClient.fail(e);
        }
    }

    @Inject(method = "writeGeometry", at = @At(value = "INVOKE",
            target = "Lme/jellysquid/mods/sodium/client/render/chunk/vertex/builder/ChunkMeshBufferBuilder;push"),
            remap = false, require = 0)
    private void windblown$tag(CallbackInfo ci, @Local(argsOnly = true) BlockRenderContext ctx) {
        if (!Foliage.tagging()) {
            return;
        }
        float origin = ctx.origin().y();
        for (ChunkVertexEncoder.Vertex vertex : vertices) {
            int alpha = windblown$swaying ? Foliage.alpha(windblown$sway, vertex.y - origin) : WindCode.NONE;
            vertex.color = WindCode.foldShade(vertex.color, alpha);
        }
    }
}
