package dev.windblown.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public final class SoundIds {
    private SoundIds() {
    }

    public static SoundEvent event(String path) {
        return SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(WindblownClient.MOD_ID, path));
    }
}
