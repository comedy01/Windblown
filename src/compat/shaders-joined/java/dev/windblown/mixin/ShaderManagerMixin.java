package dev.windblown.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.windblown.client.ShaderPatches;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = ShaderManager.class, priority = 500)
abstract class ShaderManagerMixin {
    @ModifyExpressionValue(method = "loadShader", at = @At(value = "INVOKE",
            target = "Ljava/lang/String;join(Ljava/lang/CharSequence;Ljava/lang/Iterable;)Ljava/lang/String;"))
    private static String windblown$shader(String contents, @Local(argsOnly = true) Identifier location) {
        return ShaderPatches.patch(location.toString(), contents);
    }
}
