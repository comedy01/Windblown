package dev.windblown.client.sodium;

import dev.windblown.client.Foliage;
import dev.windblown.client.WindblownClient;
import dev.windblown.core.WindCode;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.world.level.BlockGetter;

public final class LegacyVertices {
    private static Field array;
    private static Field y;
    private static Field color;
    private static boolean broken;
    private static Method world;

    private LegacyVertices() {
    }

    public static BlockGetter world(Object context) throws ReflectiveOperationException {
        if (world == null) {
            world = context.getClass().getMethod("world");
        }
        return (BlockGetter) world.invoke(context);
    }

    public static void tag(Object renderer, Foliage.Sway sway, float origin) {
        if (broken) {
            return;
        }
        try {
            if (array == null) {
                Field field = renderer.getClass().getDeclaredField("vertices");
                field.setAccessible(true);
                Class<?> vertex = field.getType().getComponentType();
                y = vertex.getField("y");
                color = vertex.getField("color");
                array = field;
            }
            for (Object vertex : (Object[]) array.get(renderer)) {
                int alpha = sway != null ? Foliage.alpha(sway, y.getFloat(vertex) - origin) : WindCode.NONE;
                color.setInt(vertex, WindCode.foldShade(color.getInt(vertex), alpha));
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            broken = true;
            WindblownClient.fail(e);
        }
    }
}
