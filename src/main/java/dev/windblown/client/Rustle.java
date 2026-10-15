package dev.windblown.client;

import dev.windblown.config.WindblownConfig;
import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.Random;

public final class Rustle {
    public static final SoundEvent GRASS = SoundIds.event("rustle.grass");
    public static final SoundEvent LEAVES = SoundIds.event("rustle.leaves");
    public static final SoundEvent CROPS = SoundIds.event("rustle.crops");
    private static final double RANGE = 24.0;

    private static final Int2FloatOpenHashMap TRAVEL = new Int2FloatOpenHashMap();
    private static final Int2FloatOpenHashMap NEXT = new Int2FloatOpenHashMap();
    private static final BlockPos.MutableBlockPos SCAN = new BlockPos.MutableBlockPos();
    private static final Random RANDOM = new Random();
    private static long played;

    private Rustle() {
    }

    public static long played() {
        return played;
    }

    public static void tick(Minecraft mc) {
        ClientLevel level = mc.level;
        WindblownConfig config = WindblownClient.config();
        if (level == null || !WindblownClient.active() || !config.rustle() || config.rustleVolume() <= 0f || mc.player == null) {
            TRAVEL.clear();
            NEXT.clear();
            return;
        }
        if (TRAVEL.size() > 512) {
            TRAVEL.clear();
            NEXT.clear();
        }
        double range = RANGE * RANGE;
        for (Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living) || living.isSpectator() || entity.distanceToSqr(mc.player) > range) {
                continue;
            }
            double dx = entity.getX() - entity.xo;
            double dy = entity.getY() - entity.yo;
            double dz = entity.getZ() - entity.zo;
            double moved = Math.sqrt(dx * dx + dz * dz + dy * dy * 0.5);
            int id = entity.getId();
            if (moved < 0.02 || moved > 2.0) {
                continue;
            }
            int category = touching(level, entity.getBoundingBox());
            if (category == Foliage.NONE) {
                continue;
            }
            float travelled = TRAVEL.get(id) + (float) moved;
            float next = NEXT.containsKey(id) ? NEXT.get(id) : stride();
            if (travelled < next) {
                TRAVEL.put(id, travelled);
                NEXT.put(id, next);
                continue;
            }
            TRAVEL.put(id, 0f);
            NEXT.put(id, stride());
            float size = Mth.clamp(entity.getBbWidth() * entity.getBbHeight() / 1.08f, 0.25f, 1.6f);
            float pace = Mth.clamp(0.45f + (float) moved * 2.2f, 0.45f, 1.2f);
            float volume = config.rustleVolume() * pace * (float) Math.sqrt(size) * (entity == mc.player ? 0.18f : 0.14f);
            float pitch = (0.88f + RANDOM.nextFloat() * 0.24f) / (float) Math.sqrt(Math.max(0.5f, size));
            SoundEvent sound = category == Foliage.LEAVES ? LEAVES : category == Foliage.CROPS ? CROPS : GRASS;
            level.playLocalSound(entity.getX(), entity.getY() + entity.getBbHeight() * 0.4, entity.getZ(), sound,
                    entity == mc.player ? SoundSource.PLAYERS : SoundSource.NEUTRAL, volume, pitch, false);
            played++;
        }
    }

    private static float stride() {
        return 0.9f + RANDOM.nextFloat() * 0.6f;
    }

    private static int touching(ClientLevel level, AABB box) {
        int x0 = Mth.floor(box.minX + 0.05);
        int x1 = Mth.floor(box.maxX - 0.05);
        int y0 = Mth.floor(box.minY + 0.05);
        int y1 = Mth.floor(box.maxY - 0.05);
        int z0 = Mth.floor(box.minZ + 0.05);
        int z1 = Mth.floor(box.maxZ - 0.05);
        int found = Foliage.NONE;
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                for (int z = z0; z <= z1; z++) {
                    int category = Foliage.category(Foliage.info(level.getBlockState(SCAN.set(x, y, z))));
                    if (category == Foliage.LEAVES) {
                        return category;
                    }
                    if (category == Foliage.GRASS || category == Foliage.CROPS) {
                        found = category;
                    }
                }
            }
        }
        return found;
    }
}
