package dev.windblown.mixin;

import dev.windblown.client.Foliage;
import dev.windblown.client.WindblownClient;
import dev.windblown.core.WindCode;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.fabricmc.fabric.impl.client.indigo.renderer.render.AltModelBlockRendererImpl", remap = false)
abstract class IndigoBlockRendererMixin {
    @Shadow(remap = false)
    private BlockAndTintGetter level;

    @Shadow(remap = false)
    private BlockPos pos;

    @Shadow(remap = false)
    private BlockState blockState;

    @Shadow(remap = false)
    @Final
    private Vector3f offset;

    @Inject(method = "transform", at = @At("RETURN"), remap = false, require = 0)
    private void windblown$tag(MutableQuadView quad, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() || !Foliage.compiling() || blockState == null) {
            return;
        }
        try {
            Foliage.Sway sway = Foliage.begin(blockState, level, pos);
            if (sway == null) {
                return;
            }
            for (int i = 0; i < 4; i++) {
                quad.color(i, WindCode.withAlpha(quad.color(i), Foliage.alpha(sway, quad.y(i) - offset.y)));
            }
        } catch (RuntimeException e) {
            WindblownClient.fail(e);
        }
    }
}
