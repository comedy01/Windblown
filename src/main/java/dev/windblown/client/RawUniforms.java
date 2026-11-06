package dev.windblown.client;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.nio.FloatBuffer;

public final class RawUniforms {
    private static final Int2ObjectOpenHashMap<int[]> PROGRAMS = new Int2ObjectOpenHashMap<>();
    // Sodium/Embeddium GlProgram extends GlObject, which has "public final int handle()" in every version.
    private static final ClassValue<MethodHandle> HANDLES = new ClassValue<MethodHandle>() {
        @Override
        protected MethodHandle computeValue(Class<?> type) {
            try {
                Method method = type.getMethod("handle");
                if (method.getReturnType() != int.class) {
                    return null;
                }
                return MethodHandles.publicLookup().unreflect(method).asType(MethodType.methodType(int.class, Object.class));
            } catch (ReflectiveOperationException | RuntimeException e) {
                return null;
            }
        }
    };
    private static volatile boolean changed;
    private static volatile boolean any;
    private static int frame;
    private static int updatedFrame = -1;
    private static FloatBuffer info;
    private static FloatBuffer camera;
    private static FloatBuffer wind;
    private static long uploads;
    private static long programQueries;

    private RawUniforms() {
    }

    public static long uploads() {
        return uploads;
    }

    public static long programQueries() {
        return programQueries;
    }

    public static void shadersChanged() {
        changed = true;
        any = true;
    }

    public static void forgetPrograms() {
        changed = true;
    }

    public static void newFrame() {
        frame++;
    }

    /**
     * Called after a Sodium GlProgram is bound. Reads the program id from the Java object instead of asking the
     * driver with glGetInteger(GL_CURRENT_PROGRAM): that query makes the render thread wait for the driver thread
     * when NVIDIA "Threaded optimization" is on, and Sodium binds programs many times per frame.
     */
    public static void onBind(Object glProgram) {
        if (!any) {
            return;
        }
        MethodHandle handle = HANDLES.get(glProgram.getClass());
        if (handle != null) {
            int program;
            try {
                program = (int) handle.invokeExact(glProgram);
            } catch (Throwable e) {
                program = 0;
            }
            if (program != 0) {
                onUse(program);
                return;
            }
        }
        programQueries++;
        onUse(GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM));
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
