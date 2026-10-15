package dev.windblown.client;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import java.nio.FloatBuffer;

public final class RawUniforms {
    private static final Int2ObjectOpenHashMap<int[]> PROGRAMS = new Int2ObjectOpenHashMap<>();
    private static volatile boolean changed;
    private static volatile boolean any;
    private static int frame;
    private static int updatedFrame = -1;
    private static FloatBuffer info;
    private static FloatBuffer camera;
    private static FloatBuffer wind;
    private static long uploads;

    private RawUniforms() {
    }

    public static long uploads() {
        return uploads;
    }

    public static void shadersChanged() {
        changed = true;
        any = true;
    }

    public static void newFrame() {
        frame++;
    }

    public static void onCurrentProgram() {
        if (any) {
            onUse(GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM));
        }
    }

    public static void onUse(int program) {
        if (!any || program == 0) {
            return;
        }
        try {
            if (changed) {
                PROGRAMS.clear();
                changed = false;
            }
            int[] entry = PROGRAMS.get(program);
            if (entry == null) {
                entry = new int[]{GL20.glGetUniformLocation(program, "WindblownInfo"),
                        GL20.glGetUniformLocation(program, "WindblownCamera"),
                        GL20.glGetUniformLocation(program, "WindblownWind"), -1};
                PROGRAMS.put(program, entry);
            }
            if (entry[0] < 0 || entry[3] == frame) {
                return;
            }
            if (updatedFrame != frame) {
                updatedFrame = frame;
                WindFrame.update(Cameras.position(Minecraft.getInstance()));
                FloatBuffer all = WindFrame.bytes().asFloatBuffer();
                info = all.duplicate().position(0).limit(4).slice();
                camera = all.duplicate().position(4).limit(8).slice();
                wind = all.duplicate().position(8).limit(12).slice();
            }
            GL20.glUniform4fv(entry[0], info);
            if (entry[1] >= 0) {
                GL20.glUniform4fv(entry[1], camera);
            }
            if (entry[2] >= 0) {
                GL20.glUniform4fv(entry[2], wind);
            }
            entry[3] = frame;
            uploads++;
        } catch (RuntimeException e) {
            WindblownClient.fail(e);
        }
    }
}
