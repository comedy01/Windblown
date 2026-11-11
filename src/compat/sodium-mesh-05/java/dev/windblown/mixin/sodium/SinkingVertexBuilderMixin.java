package dev.windblown.mixin.sodium;

import com.llamalad7.mixinextras.sugar.Local;
import dev.windblown.client.Foliage;
import dev.windblown.client.ShaderPatches;
import dev.windblown.client.sodium.SwayState;
import dev.windblown.core.WindCode;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Embeddium buffers FRAPI models (Continuity's connected textures, etc.) and custom block
 * renderers in a SinkingVertexBuilder and pushes them in {@code flush}, skipping
 * BlockRenderer.writeGeometry. Tag those vertices here with the sway of the block being
 * rendered. Each stored vertex y is relative to the block, flush adds offY to it.
 */
@Pseudo
@Mixin(targets = "me.jellysquid.mods.sodium.client.compat.ccl.SinkingVertexBuilder", remap = false)
abstract class SinkingVertexBuilderMixin {
    @ModifyArg(method = "flush(Lme/jellysquid/mods/sodium/client/render/chunk/compile/buffers/ChunkModelBuilder;Lme/jellysquid/mods/sodium/client/render/chunk/terrain/material/Material;FFF)Z",
            at = @At(value = "INVOKE",
                    target = "Lme/jellysquid/mods/sodium/client/render/chunk/vertex/builder/ChunkMeshBufferBuilder;push"),
            index = 0, remap = false, require = 0)
    private ChunkVertexEncoder.Vertex[] windblown$tag(ChunkVertexEncoder.Vertex[] vertices,
                                                     @Local(argsOnly = true, ordinal = 1) float offY) {
        if (!Foliage.tagging() || !ShaderPatches.applied(ShaderPatches.SODIUM_TERRAIN)) {
            return vertices;
        }
        SwayState state = SwayState.get();
        for (ChunkVertexEncoder.Vertex vertex : vertices) {
            int alpha = state.swaying ? Foliage.alpha(state.sway, vertex.y - offY) : WindCode.NONE;
            vertex.color = WindCode.foldShade(vertex.color, alpha);
        }
        return vertices;
    }
}
