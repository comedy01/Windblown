package dev.windblown.client;

import net.minecraft.client.multiplayer.ClientLevel;

public final class Levels {
    private Levels() {
    }

    public static boolean hasSkyLight(ClientLevel level) {
        return level.dimensionType().hasSkyLight();
    }
}
