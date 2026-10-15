package dev.windblown.client;

import dev.windblown.config.WindblownConfig;
import dev.windblown.core.WindCode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

public final class Foliage {
    public static final int NONE = 0;
    public static final int GRASS = 1;
    public static final int CROPS = 2;
    public static final int LEAVES = 3;
    public static final int VINES = 4;
    public static final int WATER = 5;

    public static final int SINGLE = 0;
    public static final int LOWER = 1;
    public static final int UPPER = 2;
    public static final int STACK_UP = 3;
    public static final int STACK_DOWN = 4;
    public static final int HANG = 5;
    public static final int WALL = 6;
    public static final int FULL = 7;

    private static final int STACK_REACH = 5;
    private static final ThreadLocal<Sway> SWAY = ThreadLocal.withInitial(Sway::new);
    private static final ThreadLocal<boolean[]> COMPILING = ThreadLocal.withInitial(() -> new boolean[1]);
    private static final ThreadLocal<BlockPos.MutableBlockPos> SCAN = ThreadLocal.withInitial(BlockPos.MutableBlockPos::new);
    private static final ThreadLocal<Sway> ACTIVE = new ThreadLocal<>();

    private static volatile boolean tagging = true;
    private static volatile int allowed = mask(new WindblownConfig());

    private Foliage() {
    }

    public static final class Sway {
        public int kind;
        public float base;
        public float slope;
        public float originY;
    }

    public static int info(int category, int shape) {
        return category | shape << 4;
    }

    public static int category(int info) {
        return info & 15;
    }

    public static int shape(int info) {
        return info >> 4 & 15;
    }

    public static int info(BlockState state) {
        if (state instanceof FoliageInfo cached) {
            int info = cached.windblown$info();
            if (info < 0) {
                info = Plants.info(state);
                cached.windblown$setInfo(info);
            }
            return info;
        }
        return Plants.info(state);
    }

    public static void configure(boolean on, WindblownConfig config) {
        tagging = on;
        allowed = mask(config);
    }

    public static void setCompiling(boolean on) {
        COMPILING.get()[0] = on;
        ACTIVE.remove();
    }

    public static void enter(BlockState state, BlockGetter level, BlockPos pos) {
        Sway sway = compiling() ? begin(state, level, pos) : null;
        if (sway != null) {
            sway.originY = pos.getY() & 15;
        }
        ACTIVE.set(sway);
    }

    public static void exit() {
        ACTIVE.remove();
    }

    public static int tagColor(int color, float y) {
        Sway sway = ACTIVE.get();
        return sway == null ? color : WindCode.withAlpha(color, alpha(sway, y - sway.originY));
    }

    public static float tagAlpha(float alpha, float y) {
        Sway sway = ACTIVE.get();
        return sway == null ? alpha : (alpha(sway, y - sway.originY) + 0.5f) / 255f;
    }

    public static boolean compiling() {
        return COMPILING.get()[0];
    }

    public static boolean tagging() {
        return tagging;
    }

    public static int mask(WindblownConfig config) {
        int mask = 0;
        if (config.grass()) {
            mask |= 1 << GRASS;
        }
        if (config.crops()) {
            mask |= 1 << CROPS;
        }
        if (config.leaves()) {
            mask |= 1 << LEAVES;
        }
        if (config.vines()) {
            mask |= 1 << VINES;
        }
        if (config.waterPlants()) {
            mask |= 1 << WATER;
        }
        return mask;
    }

    public static Sway begin(BlockState state, BlockGetter level, BlockPos pos) {
        if (!tagging) {
            return null;
        }
        int info = info(state);
        int category = category(info);
        if (category == NONE || (allowed & 1 << category) == 0) {
            return null;
        }
        Sway sway = SWAY.get();
        int ground = category == WATER ? WindCode.WATER : WindCode.PLANT;
        float stiffness = category == CROPS ? 0.75f : 1f;
        switch (shape(info)) {
            case FULL -> set(sway, WindCode.LEAVES, 1f, 0f);
            case WALL -> set(sway, WindCode.LEAVES, 0.6f, 0f);
            case HANG -> set(sway, WindCode.HANGING, 1f, -1f);
            case LOWER -> set(sway, ground, 0f, 0.5f * stiffness);
            case UPPER -> set(sway, ground, 0.5f * stiffness, 0.5f * stiffness);
            case STACK_UP -> {
                int below = run(level, pos, info, -1);
                set(sway, ground, below / (float) STACK_REACH, 1f / STACK_REACH);
            }
            case STACK_DOWN -> {
                int above = run(level, pos, info, 1);
                set(sway, WindCode.HANGING, (above + 1) / (float) STACK_REACH, -1f / STACK_REACH);
            }
            default -> set(sway, ground, 0f, stiffness);
        }
        return sway;
    }

    public static int alpha(Sway sway, float localY) {
        float y = Math.max(0f, Math.min(1f, localY));
        return WindCode.alpha(sway.kind, sway.base + sway.slope * y);
    }

    private static void set(Sway sway, int kind, float base, float slope) {
        sway.kind = kind;
        sway.base = base;
        sway.slope = slope;
    }

    private static int run(BlockGetter level, BlockPos pos, int info, int step) {
        if (level == null) {
            return 0;
        }
        BlockPos.MutableBlockPos scan = SCAN.get();
        int count = 0;
        while (count < STACK_REACH) {
            scan.set(pos.getX(), pos.getY() + step * (count + 1), pos.getZ());
            if (info(level.getBlockState(scan)) != info) {
                break;
            }
            count++;
        }
        return count;
    }
}
