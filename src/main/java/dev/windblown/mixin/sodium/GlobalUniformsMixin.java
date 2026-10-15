package dev.windblown.mixin.sodium;

import dev.windblown.client.SodiumHook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.ByteBuffer;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.UniformBufferManager$GlobalUniforms", remap = false, priority = 1500)
abstract class GlobalUniformsMixin {
    @Inject(method = "write", at = @At("HEAD"), remap = false, require = 0)
    private void windblown$write(ByteBuffer buffer, CallbackInfo ci) {
        SodiumHook.write(buffer);
    }
}
