package dev.windblown.mixin.sodium;

import com.llamalad7.mixinextras.sugar.Local;
import dev.windblown.client.Foliage;
import dev.windblown.client.WindblownClient;
import dev.windblown.core.WindCode;
import me.jellysquid.mods.sodium.client.render.pipeline.BlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(value = BlockRenderer.class, remap = false)
abstract class BlockRendererMixin {
    @Unique
    private final Foliage.Sway windblown$sway = new Foliage.Sway();

    @Unique
    private boolean windblown$swaying;

    @Inject(method = "renderModel", at = @At("HEAD"), remap = false, require = 0)
    private void windblown$block(CallbackInfoReturnable<Boolean> cir, @Local(argsOnly = true) BlockAndTintGetter level,
                                 @Local(argsOnly = true) BlockState state, @Local(ordinal = 0, argsOnly = true) BlockPos pos) {
        windblown$swaying = false;
        try {
            Foliage.Sway sway = Foliage.begin(state, level, pos);
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

    @ModifyArg(method = "renderQuad", at = @At(value = "INVOKE",
            target = "Lme/jellysquid/mods/sodium/client/render/chunk/format/ModelVertexSink;writeVertex"),
            index = 4, remap = false, require = 0)
    private int windblown$tag(Vec3i origin, float x, float y, float z, int color, float u, float v, int light, int chunk) {
        return windblown$swaying ? WindCode.withAlpha(color, Foliage.alpha(windblown$sway, y)) : color;
    }
}
