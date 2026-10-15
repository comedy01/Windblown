package dev.windblown.client;

import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public final class SoundIds {
    private SoundIds() {
    }

    public static SoundEvent event(String path) {
        return SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(WindblownClient.MOD_ID, path));
    }
}
