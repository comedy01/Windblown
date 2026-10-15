package dev.windblown.core;

public final class WindCode {
    public static final int PLANT = 0;
    public static final int LEAVES = 1;
    public static final int HANGING = 2;
    public static final int WATER = 3;
    public static final int LEVELS = 15;
    public static final int NONE = 255;

    private WindCode() {
    }

    public static int alpha(int kind, float weight) {
        float clamped = Float.isNaN(weight) ? 0f : Math.max(0f, Math.min(1f, weight));
        return 254 - (kind * 16 + Math.round(clamped * LEVELS));
    }

    public static int kind(int alpha) {
        int code = 254 - alpha;
        return code < 0 || code >= 64 ? -1 : code / 16;
    }

    public static float weight(int alpha) {
        int code = 254 - alpha;
        return code < 0 || code >= 64 ? 0f : (code % 16) / (float) LEVELS;
    }

    public static int withAlpha(int argb, int alpha) {
        return argb & 0x00FFFFFF | alpha << 24;
    }

    public static int foldShade(int color, int alpha) {
        int shade = color >>> 24;
        int c0 = ((color & 0xFF) * shade + 127) / 255;
        int c1 = ((color >> 8 & 0xFF) * shade + 127) / 255;
        int c2 = ((color >> 16 & 0xFF) * shade + 127) / 255;
        return alpha << 24 | c2 << 16 | c1 << 8 | c0;
    }
}
