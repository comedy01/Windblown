package dev.windblown.client;

import dev.windblown.config.WindblownConfig;
import dev.windblown.core.Wind;
import net.minecraft.client.Minecraft;

public final class WindState {
    private static final float TICK = 0.05f;

    private static float weather = 1f;
    private static float air = 1f;
    private static int mask = -1;

    private WindState() {
    }

    public static void tick(Minecraft mc) {
        WindblownConfig config = WindblownClient.config();
        boolean tagging = WindblownClient.active() && !Compat.shaderPackInUse();
        int wanted = Foliage.mask(config);
        if (tagging != Foliage.tagging() || wanted != mask) {
            Foliage.configure(tagging, config);
            mask = wanted;
            if (mc.level != null && mc.levelRenderer != null) {
                Rebuilds.all(mc);
            }
        }
        if (mc.level == null) {
            return;
        }
        float target = config.weather() ? Wind.weather(mc.level.getRainLevel(1f), mc.level.getThunderLevel(1f)) : 1f;
        weather = Wind.ease(weather, target, TICK, 4f);
        air = Wind.ease(air, Levels.hasSkyLight(mc.level) ? 1f : 0.3f, TICK, 1f);
    }

    public static float strength() {
        return WindblownClient.config().strength() * weather * air;
    }

    public static float current() {
        return WindblownClient.config().strength();
    }

    public static float weather() {
        return weather;
    }
}
