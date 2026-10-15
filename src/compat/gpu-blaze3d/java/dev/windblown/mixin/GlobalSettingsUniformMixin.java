package dev.windblown.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.GpuDevice;
import dev.windblown.client.WindblownClient;
import dev.windblown.client.ShaderPatches;
import dev.windblown.client.WindFrame;
import net.minecraft.client.renderer.GlobalSettingsUniform;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

@Mixin(value = GlobalSettingsUniform.class, priority = 1500)
abstract class GlobalSettingsUniformMixin {
    @Shadow
    @Final
    private GpuBuffer buffer;

    @WrapOperation(method = "<init>", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/GpuDevice;createBuffer(Ljava/util/function/Supplier;IJ)Lcom/mojang/blaze3d/buffers/GpuBuffer;"))
    private GpuBuffer windblown$size(GpuDevice device, Supplier<String> label, int usage, long size, Operation<GpuBuffer> original) {
        return original.call(device, label, usage, Math.max(size, offset() + WindFrame.BYTES));
    }

    @Inject(method = "update", at = @At("TAIL"))
    private void windblown$update(CallbackInfo ci, @Local(argsOnly = true) Vec3 cameraPos) {
        try {
            WindFrame.update(cameraPos);
            if (!ShaderPatches.applied(ShaderPatches.VANILLA_GLOBALS)) {
                return;
            }
            RenderSystem.getDevice().createCommandEncoder()
                    .writeToBuffer(buffer.slice(offset(), WindFrame.BYTES), WindFrame.bytes());
        } catch (RuntimeException e) {
            WindblownClient.fail(e);
        }
    }

    private static int offset() {
        return ((GlobalSettingsUniform.UBO_SIZE + 15) & ~15) + WindFrame.FIRELIGHT_BYTES;
    }
}
