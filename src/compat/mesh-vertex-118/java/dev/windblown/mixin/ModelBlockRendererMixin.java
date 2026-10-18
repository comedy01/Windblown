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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ModelBlockRenderer.class)
abstract class ModelBlockRendererMixin {
    @Inject(method = "tesselateBlock*", at = @At("HEAD"))
    private void windblown$enter(CallbackInfoReturnable<Boolean> ci, @Local(argsOnly = true) BlockAndTintGetter level,
                                 @Local(argsOnly = true) BlockState state, @Local(argsOnly = true) BlockPos pos) {
        try {
            Foliage.enter(state, level, pos);
        } catch (RuntimeException e) {
            Foliage.exit();
            WindblownClient.fail(e);
        }
    }

    @Inject(method = "tesselateBlock*", at = @At("RETURN"))
    private void windblown$exit(CallbackInfoReturnable<Boolean> ci) {
        Foliage.exit();
    }

    @Inject(method = {"tesselateBlock(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/client/resources/model/BakedModel;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;ZLjava/util/Random;JILnet/minecraftforge/client/model/data/IModelData;)Z"}, remap = false, require = 0, at = @At("HEAD"))
    private void windblown$enterForge(CallbackInfoReturnable<Boolean> ci, @Local(argsOnly = true) BlockAndTintGetter level,
                                      @Local(argsOnly = true) BlockState state, @Local(argsOnly = true) BlockPos pos) {
        try {
            Foliage.enter(state, level, pos);
        } catch (RuntimeException e) {
            Foliage.exit();
            WindblownClient.fail(e);
        }
    }

    @Inject(method = {"tesselateBlock(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/client/resources/model/BakedModel;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;ZLjava/util/Random;JILnet/minecraftforge/client/model/data/IModelData;)Z"}, remap = false, require = 0, at = @At("RETURN"))
    private void windblown$exitForge(CallbackInfoReturnable<Boolean> ci) {
        Foliage.exit();
    }
}
