package dev.windblown.client;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.ByteBuffer;

public final class SodiumHook {
    public static final int OFFSET = 192 + WindFrame.FIRELIGHT_BYTES;
    private static final Logger LOGGER = LogManager.getLogger(WindblownClient.MOD_ID);
    private static volatile boolean sized;
    private static volatile boolean ran;

    private SodiumHook() {
    }

    public static int blockSize(int size) {
        int needed = (OFFSET + WindFrame.BYTES + 255) / 256 * 256;
        sized = true;
        return Math.max(size, needed);
    }

    public static void write(ByteBuffer buffer) {
        if (!sized || !ShaderPatches.applied(ShaderPatches.SODIUM_GLOBALS)) {
            return;
        }
        int offset = buffer.position() + OFFSET;
        if (offset + WindFrame.BYTES > buffer.limit() || Compat.shaderPackInUse()) {
            return;
        }
        WindFrame.writeTo(buffer, offset);
        if (!ran) {
            ran = true;
            LOGGER.info("Sodium found: wind data sent to its terrain shader");
        }
    }

    public static boolean ran() {
        return ran;
    }
}
