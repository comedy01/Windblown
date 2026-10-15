package dev.windblown.client;

import net.minecraft.client.Minecraft;

public final class Rebuilds {
    private Rebuilds() {
    }

    public static void all(Minecraft mc) {
        mc.levelExtractor.allChanged();
    }
}
