package dev.windblown.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.BufferBuilder;
import dev.windblown.client.Foliage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(BufferBuilder.class)
abstract class BufferBuilderMixin {
    @ModifyVariable(method = "vertex(FFFFFFFFFIIFFF)V", at = @At("HEAD"), argsOnly = true, ordinal = 6)
    private float windblown$alpha(float alpha, @Local(ordinal = 1, argsOnly = true) float y) {
        return Foliage.tagAlpha(alpha, y);
    }
}
