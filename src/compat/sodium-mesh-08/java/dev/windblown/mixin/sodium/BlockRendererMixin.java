package dev.windblown.mixin.sodium;

import com.llamalad7.mixinextras.sugar.Local;
import dev.windblown.client.Foliage;
import dev.windblown.client.WindblownClient;
import dev.windblown.core.WindCode;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import net.caffeinemc.mods.sodium.client.render.model.AbstractBlockRenderContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;
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
abstract class BlockRendererMixin extends AbstractBlockRenderContext {
    @Shadow
    @Final
    private ChunkVertexEncoder.Vertex[] vertices;

    @Shadow
    @Final
    private Vector3f posOffset;

    @Unique
    private final Foliage.Sway windblown$sway = new Foliage.Sway();

    @Unique
    private boolean windblown$swaying;

    @Inject(method = "renderModel", at = @At("HEAD"), remap = false, require = 0)
    private void windblown$block(CallbackInfo ci, @Local(argsOnly = true) BlockState state,
                                 @Local(ordinal = 0, argsOnly = true) BlockPos pos) {
        windblown$swaying = false;
        try {
            Foliage.Sway sway = Foliage.begin(state, slice, pos);
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

    @Inject(method = "bufferQuad", at = @At(value = "INVOKE",
            target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/vertex/builder/ChunkMeshBufferBuilder;push([Lnet/caffeinemc/mods/sodium/client/render/chunk/vertex/format/ChunkVertexEncoder$Vertex;I)V"),
            remap = false, require = 0)
    private void windblown$tag(CallbackInfo ci) {
        if (!windblown$swaying) {
            return;
        }
        for (ChunkVertexEncoder.Vertex vertex : vertices) {
            vertex.color = WindCode.withAlpha(vertex.color, Foliage.alpha(windblown$sway, vertex.y - posOffset.y));
        }
    }
}
