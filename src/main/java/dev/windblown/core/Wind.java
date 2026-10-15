package dev.windblown.core;

public final class Wind {
    public static final float MARK = 4321.5f;
    public static final int FLOATS = 12;
    public static final double PERIOD = 3600.0;
    public static final double WRAP = 4096.0;

    private Wind() {
    }

    public static double angle(double seconds) {
        return 0.6 + 0.9 * Math.sin(seconds / 173.0) + 0.35 * Math.sin(seconds / 61.0 + 1.3);
    }

    public static float gust(double seconds) {
        double slow = 0.5 + 0.5 * Math.sin(seconds / 9.7) * Math.sin(seconds / 3.1 + 0.7);
        double swell = 0.5 + 0.5 * Math.sin(seconds / 23.0 + 2.1);
        return (float) Math.max(0.0, Math.min(1.0, 0.25 + 0.5 * slow + 0.25 * swell));
    }

    public static float weather(float rain, float thunder) {
        return 1f + 0.7f * clamp01(rain) + 0.9f * clamp01(thunder);
    }

    public static float ease(float current, float target, float seconds, float timeConstant) {
        float step = 1f - (float) Math.exp(-seconds / timeConstant);
        return current + (target - current) * step;
    }

    public static double wrap(double value) {
        return value - Math.floor(value / WRAP) * WRAP;
    }

    private static float clamp01(float value) {
        return Float.isNaN(value) ? 0f : Math.max(0f, Math.min(1f, value));
    }
}
