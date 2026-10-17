package dev.windblown.client;

import dev.windblown.core.Wind;
import net.minecraft.world.phys.Vec3;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class WindFrame {
    public static final int BYTES = Wind.FLOATS * 4;
    public static final int FIRELIGHT_BYTES = 1040;
    private static final long START = System.nanoTime();

    private static final float[] FRAME = new float[Wind.FLOATS];
    private static final ByteBuffer BYTES_BUFFER = ByteBuffer.allocateDirect(BYTES).order(ByteOrder.nativeOrder());

    private WindFrame() {
    }

    public static double seconds() {
        return (System.nanoTime() - START) / 1.0E9;
    }

    public static void update(Vec3 camera) {
        float[] frame = FRAME;
        double seconds = seconds();
        boolean on = WindblownClient.active() && camera != null && !Compat.shaderPackInUse();
        double angle = Wind.angle(seconds);
        frame[0] = (float) (seconds % Wind.PERIOD);
        frame[1] = on ? WindState.strength() : 0f;
        frame[2] = on ? Wind.MARK : 0f;
        frame[3] = on ? WindState.current() : 0f;
        if (camera != null) {
            frame[4] = (float) Wind.wrap(camera.x);
            frame[5] = (float) camera.y;
            frame[6] = (float) Wind.wrap(camera.z);
        }
        frame[7] = 0f;
        frame[8] = (float) Math.cos(angle);
        frame[9] = (float) Math.sin(angle);
        frame[10] = Wind.gust(seconds);
        frame[11] = on ? WindblownClient.config().gusts() : 0f;
        ByteBuffer out = BYTES_BUFFER;
        out.clear();
        for (int i = 0; i < Wind.FLOATS; i++) {
            out.putFloat(i * 4, frame[i]);
        }
    }

    public static ByteBuffer bytes() {
        BYTES_BUFFER.clear();
        return BYTES_BUFFER;
    }

    public static void writeTo(ByteBuffer target, int offset) {
        ByteBuffer out = target.duplicate().order(ByteOrder.nativeOrder());
        for (int i = 0; i < Wind.FLOATS; i++) {
            out.putFloat(offset + i * 4, BYTES_BUFFER.getFloat(i * 4));
        }
    }

    public static float strength() {
        return FRAME[1];
    }

    public static boolean sending() {
        return FRAME[2] == Wind.MARK;
    }
}
