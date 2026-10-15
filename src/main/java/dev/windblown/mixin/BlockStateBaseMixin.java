package dev.windblown.mixin;

import dev.windblown.client.FoliageInfo;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(BlockBehaviour.BlockStateBase.class)
abstract class BlockStateBaseMixin implements FoliageInfo {
    @Unique
    private volatile int windblown$info = -1;

    @Override
    public int windblown$info() {
        return windblown$info;
    }

    @Override
    public void windblown$setInfo(int info) {
        windblown$info = info;
    }
}
