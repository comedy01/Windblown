package dev.windblown.mixin.sodium;

import dev.windblown.client.Foliage;
import dev.windblown.client.ShaderPatches;
import dev.windblown.client.WindblownClient;
import dev.windblown.client.sodium.LegacyVertices;
import dev.windblown.client.sodium.SwayState;
import dev.windblown.core.WindCode;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderContext;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * With Indium installed, Sodium 0.5 hands FRAPI models (Continuity's connected textures, etc.)
 * to Indium's TerrainRenderContext instead of BlockRenderer.renderModel, so BlockRendererMixin
 * never sees them. Set the block's sway for {@code tessellateBlock} and tag the vertices Indium
 * pushes in {@code bufferQuad}. Those vertex positions are chunk-relative: subtract the origin.
 */
@Pseudo
@Mixin(targets = "link.infra.indium.renderer.render.TerrainRenderContext", remap = false)
abstract class IndiumTerrainContextMixin {
    @Inject(method = "tessellateBlock", at = @At("HEAD"), remap = false, require = 0)
    private void windblown$begin(BlockRenderContext ctx, CallbackInfo ci) {
        SwayState state = SwayState.get();
        state.swaying = false;
        try {
            BlockGetter world;
            try {
                world = ctx.world();
            } catch (NoSuchMethodError e) {
                world = LegacyVertices.world(ctx);
            }
            Foliage.Sway sway = Foliage.begin(ctx.state(), world, ctx.pos());
            if (sway != null) {
                state.sway.kind = sway.kind;
                state.sway.base = sway.base;
                state.sway.slope = sway.slope;
                state.originY = ctx.origin().y();
                state.swaying = true;
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            WindblownClient.fail(e);
        }
    }

    @Inject(method = "tessellateBlock", at = @At("RETURN"), remap = false, require = 0)
    private void windblown$end(BlockRenderContext ctx, CallbackInfo ci) {
        SwayState.get().swaying = false;
    }

    @ModifyArg(method = "bufferQuad", at = @At(value = "INVOKE",
            target = "Lme/jellysquid/mods/sodium/client/render/chunk/vertex/builder/ChunkMeshBufferBuilder;push"),
            index = 0, remap = false, require = 0)
    private ChunkVertexEncoder.Vertex[] windblown$tag(ChunkVertexEncoder.Vertex[] vertices) {
        if (!Foliage.tagging() || !ShaderPatches.applied(ShaderPatches.SODIUM_TERRAIN)) {
            return vertices;
        }
        SwayState state = SwayState.get();
        for (ChunkVertexEncoder.Vertex vertex : vertices) {
            int alpha = state.swaying ? Foliage.alpha(state.sway, vertex.y - state.originY) : WindCode.NONE;
            vertex.color = WindCode.foldShade(vertex.color, alpha);
        }
        return vertices;
    }
}
