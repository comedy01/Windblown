package dev.windblown.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.windblown.client.Foliage;
import dev.windblown.client.WindblownClient;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelBlockRenderer.class)
abstract class ModelBlockRendererMixin {
    @Inject(method = "tesselateBlock*", at = @At("HEAD"))
    private void windblown$enter(CallbackInfo ci, @Local(argsOnly = true) BlockAndTintGetter level,
                                 @Local(argsOnly = true) BlockState state, @Local(argsOnly = true) BlockPos pos) {
        try {
            Foliage.enter(state, level, pos);
        } catch (RuntimeException e) {
            Foliage.exit();
            WindblownClient.fail(e);
        }
    }

    @Inject(method = "tesselateBlock*", at = @At("RETURN"))
    private void windblown$exit(CallbackInfo ci) {
        Foliage.exit();
    }
}
