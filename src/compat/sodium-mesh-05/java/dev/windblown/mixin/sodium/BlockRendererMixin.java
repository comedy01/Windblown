package dev.windblown.mixin.sodium;

import com.llamalad7.mixinextras.sugar.Local;
import dev.windblown.client.Foliage;
import dev.windblown.client.ShaderPatches;
import dev.windblown.client.WindblownClient;
import dev.windblown.client.sodium.LegacyVertices;
import dev.windblown.core.WindCode;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderContext;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(value = BlockRenderer.class, remap = false)
abstract class BlockRendererMixin {
    @Unique
    private final Foliage.Sway windblown$sway = new Foliage.Sway();

    @Unique
    private boolean windblown$swaying;

    @Unique
    private static boolean windblown$legacy;

    @ModifyVariable(method = "renderModel", at = @At("HEAD"), argsOnly = true, remap = false, require = 0)
    private BlockRenderContext windblown$block(BlockRenderContext ctx) {
        windblown$swaying = false;
        try {
            BlockGetter world;
            if (windblown$legacy) {
                world = LegacyVertices.world(ctx);
            } else {
                try {
                    world = ctx.world();
                } catch (NoSuchMethodError e) {
                    windblown$legacy = true;
                    world = LegacyVertices.world(ctx);
                }
            }
            windblown$begin(ctx, world);
        } catch (ReflectiveOperationException | RuntimeException e) {
            WindblownClient.fail(e);
        }
        return ctx;
    }

    @Unique
    private void windblown$begin(BlockRenderContext ctx, BlockGetter world) {
        windblown$swaying = false;
        try {
            Foliage.Sway sway = Foliage.begin(ctx.state(), world, ctx.pos());
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

    @ModifyArg(method = "writeGeometry", at = @At(value = "INVOKE",
            target = "Lme/jellysquid/mods/sodium/client/render/chunk/vertex/builder/ChunkMeshBufferBuilder;push"),
            index = 0, remap = false, require = 0)
    private ChunkVertexEncoder.Vertex[] windblown$tag(ChunkVertexEncoder.Vertex[] vertices, @Local(argsOnly = true) BlockRenderContext ctx) {
        if (!Foliage.tagging() || !ShaderPatches.applied(ShaderPatches.SODIUM_TERRAIN)) {
            return vertices;
        }
        float origin = ctx.origin().y();
        for (ChunkVertexEncoder.Vertex vertex : vertices) {
            int alpha = windblown$swaying ? Foliage.alpha(windblown$sway, vertex.y - origin) : WindCode.NONE;
            vertex.color = WindCode.foldShade(vertex.color, alpha);
        }
        return vertices;
    }

    @Inject(method = "writeGeometry", at = @At(value = "INVOKE",
            target = "Lme/jellysquid/mods/sodium/client/render/vertex/type/ChunkVertexBufferBuilder;push"),
            remap = false, require = 0)
    private void windblown$tagLegacy(CallbackInfo ci, @Local(argsOnly = true) BlockRenderContext ctx) {
        if (!Foliage.tagging() || !ShaderPatches.applied(ShaderPatches.SODIUM_TERRAIN)) {
            return;
        }
        LegacyVertices.tag(this, windblown$swaying ? windblown$sway : null, ctx.origin().y());
    }
}
