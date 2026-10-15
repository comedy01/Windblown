package dev.windblown.selftest;

import com.mojang.blaze3d.platform.NativeImage;
import dev.windblown.client.Backend;
import dev.windblown.client.Compat;
import dev.windblown.client.Foliage;
import dev.windblown.client.RawUniforms;
import dev.windblown.client.Rustle;
import dev.windblown.client.ShaderPatches;
import dev.windblown.client.SodiumHook;
import dev.windblown.client.WindFrame;
import dev.windblown.client.WindState;
import dev.windblown.client.WindblownClient;
import dev.windblown.client.gui.WindblownSettingsScreen;
import dev.windblown.config.WindblownConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.client.tutorial.TutorialSteps;
import org.apache.logging.log4j.LogManager;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;

public final class WindblownSelfTest {
    private static final String WORLD = "windblown-selftest";
    private static final int TIMEOUT = 2400;

    private static volatile int frames;
    public static volatile boolean hideHud;

    private final List<Step> steps = new ArrayList<>();
    private final List<String> shots = new ArrayList<>();
    private final Set<String> existingShots = new HashSet<>();
    private final List<int[]> captures = new CopyOnWriteArrayList<>();
    private boolean started;
    private boolean finished;
    private int idle;
    private int index;
    private int delay;
    private int waited;
    private int seenFrames;
    private int patience;
    private double windMotion;
    private long rustles;
    private boolean shaders;

    private record Step(int delay, BooleanSupplier ready, Runnable action) {
    }

    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (finished) {
            return;
        }
        if (!started) {
            if (mc.level == null && Screens.overlay(mc) == null && Screens.current(mc) != null && ++idle > 40) {
                started = true;
                mc.options.pauseOnLostFocus = false;
                mc.options.tutorialStep = TutorialSteps.NONE;
                ServerCompat.hideChat(mc);
                rememberShots(mc);
                plan(mc);
                delay = steps.get(0).delay();
                log("creating world");
                deleteWorld(mc, WORLD);
                Worlds.create(mc, WORLD, true, 0L);
            }
            return;
        }
        int drawn = frames;
        if (drawn == seenFrames) {
            return;
        }
        seenFrames = drawn;
        if (delay > 0) {
            delay--;
            return;
        }
        Step step = steps.get(index);
        if (!step.ready().getAsBoolean()) {
            if (++waited > TIMEOUT) {
                finish(mc, new AssertionError("timed out at step " + index));
            }
            return;
        }
        waited = 0;
        index++;
        try {
            step.action().run();
        } catch (Throwable e) {
            finish(mc, e);
            return;
        }
        if (index >= steps.size()) {
            finish(mc, null);
        } else {
            delay = steps.get(index).delay();
        }
    }

    private static final long FILM_STEP = 1_000_000_000L / 30;
    private static volatile Film film;
    private static volatile float[] pan;

    private static final class Film {
        final String name;
        final java.util.concurrent.BlockingQueue<Object[]> queue = new java.util.concurrent.ArrayBlockingQueue<>(6);
        final Thread writer;
        final long start = System.nanoTime();
        Process process;
        long next = start;
        int written;
        int dropped;
        int carry;

        Film(File out, String name) {
            this.name = name;
            writer = new Thread(() -> {
                try {
                    while (true) {
                        Object[] item = queue.take();
                        if (item.length == 0) {
                            break;
                        }
                        write((byte[]) item[0], (int) item[1], (int) item[2], (int) item[3], out);
                    }
                    if (process != null) {
                        process.getOutputStream().close();
                        process.waitFor();
                    }
                } catch (IOException | InterruptedException e) {
                    log("film " + name + " failed: " + e);
                }
            }, "Windblown film");
            writer.start();
        }

        void write(byte[] pixels, int width, int height, int copies, File out) throws IOException {
            if (process == null) {
                process = new ProcessBuilder(System.getProperty("windblown.ffmpeg"), "-y", "-loglevel", "error",
                        "-f", "rawvideo", "-pix_fmt", "rgba", "-s", width + "x" + height, "-r", "30", "-i", "-",
                        "-c:v", "libx264", "-crf", "16", "-preset", "fast", "-pix_fmt", "yuv420p", out.getPath())
                        .redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
            }
            for (int i = 0; i < copies; i++) {
                process.getOutputStream().write(pixels);
                written++;
            }
        }
    }

    public static void frame() {
        frames++;
        float[] look = pan;
        Minecraft mc = Minecraft.getInstance();
        if (look != null && mc.player != null) {
            double t = Math.min(1.0, (System.nanoTime() - panStart) / (look[4] * 1.0E9));
            double s = t * t * (3 - 2 * t);
            float yaw = (float) (look[0] + (look[1] - look[0]) * s);
            float pitch = (float) (look[2] + (look[3] - look[2]) * s);
            mc.player.setYRot(yaw);
            mc.player.yRotO = yaw;
            mc.player.setXRot(pitch);
            mc.player.xRotO = pitch;
        }
        Film current = film;
        if (current == null) {
            return;
        }
        long now = System.nanoTime();
        int copies = 0;
        while (current.next <= now) {
            copies++;
            current.next += FILM_STEP;
        }
        if (copies == 0) {
            return;
        }
        int count = copies;
        Captures.grab(Screens.renderTarget(mc), 1, image -> {
            try {
                int[] pixels = Captures.abgr(image);
                java.nio.ByteBuffer bytes = java.nio.ByteBuffer.allocate(pixels.length * 4).order(java.nio.ByteOrder.LITTLE_ENDIAN);
                bytes.asIntBuffer().put(pixels);
                int total = count + current.carry;
                if (current.queue.offer(new Object[]{bytes.array(), image.getWidth(), image.getHeight(), total})) {
                    current.carry = 0;
                } else {
                    current.carry = total;
                    current.dropped += count;
                }
            } finally {
                image.close();
            }
        });
    }

    private static long panStart;

    private void pan(float yaw0, float yaw1, float pitch0, float pitch1, float length) {
        then(0, () -> {
            panStart = System.nanoTime();
            pan = new float[]{yaw0, yaw1, pitch0, pitch1, length};
        });
    }

    private void still() {
        then(0, () -> pan = null);
    }

    private static void startFilm(Minecraft mc, String name) {
        try {
            File dir = new File(mc.gameDirectory, "film");
            Files.createDirectories(dir.toPath());
            film = new Film(new File(dir, name + ".mp4"), name);
            log("filming " + name);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void stopFilm() {
        Film current = film;
        film = null;
        if (current != null) {
            try {
                current.queue.put(new Object[0]);
                current.writer.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            log("filmed " + current.name + ": " + current.written + " frames, " + current.dropped + " dropped");
        }
    }

    private void then(int ticks, Runnable action) {
        steps.add(new Step(ticks, () -> true, action));
    }

    private void when(BooleanSupplier ready, Runnable action) {
        steps.add(new Step(0, ready, action));
    }

    private void within(int ticks, BooleanSupplier ready, Runnable action) {
        then(1, () -> patience = ticks);
        when(() -> ready.getAsBoolean() || --patience < 0, action);
    }

    private void finish(Minecraft mc, Throwable failure) {
        finished = true;
        nameShots(mc);
        if (failure == null) {
            log("ALL CHECKS PASSED");
        } else {
            log("FAILED: " + failure);
            failure.printStackTrace();
        }
        WindblownConfig config = WindblownClient.config();
        config.resetToDefaults();
        WindblownClient.saveConfig();
        mc.stop();
    }

    private void plan(Minecraft mc) {
        WindblownConfig config = WindblownClient.config();
        int floor = ServerCompat.FLOOR;
        boolean sodium = sodiumLoaded();

        when(() -> mc.level != null && mc.player != null && Screens.current(mc) == null
                && mc.getSingleplayerServer() != null, () -> {
            shaders = Compat.shaderPackInUse();
            log("world loaded, sodium " + sodium + ", shader pack " + shaders);
        });
        then(40, () -> {
            config.resetToDefaults();
            hideHud = true;
            run(mc, "gamerule doDaylightCycle false");
            run(mc, "gamerule advance_time false");
            run(mc, "gamerule doWeatherCycle false");
            run(mc, "gamerule advance_weather false");
            run(mc, "gamerule doMobSpawning false");
            run(mc, "gamerule spawn_mobs false");
            run(mc, "time set 6000");
            run(mc, "weather clear");
            meadow(mc, floor);
            run(mc, "tp @p 0.5 " + floor + " -1.5 0 12");
        });
        if (Boolean.getBoolean("windblown.film")) {
            filmScene(mc);
            return;
        }
        if (Boolean.getBoolean("windblown.perf")) {
            perfScene(mc);
            return;
        }
        then(60, () -> {
            if (sodium) {
                check(ShaderPatches.applied(ShaderPatches.SODIUM_TERRAIN), "Sodium's terrain shader was not patched");
                check(ShaderPatches.applied(ShaderPatches.SODIUM_TERRAIN + "-raw") || ShaderPatches.applied(ShaderPatches.SODIUM_GLOBALS),
                        "Sodium's terrain shader got no wind data (no globals patch, no raw uniforms)");
            } else if (Backend.RAW_UNIFORMS) {
                check(ShaderPatches.applied(ShaderPatches.VANILLA_TERRAIN + "-raw"), "the vanilla terrain shader did not get raw uniforms");
            } else {
                check(ShaderPatches.applied(ShaderPatches.VANILLA_GLOBALS), "the vanilla globals were not patched");
                check(ShaderPatches.applied(ShaderPatches.VANILLA_TERRAIN), "the vanilla terrain shader was not patched");
            }
            log("firelight layout seen: vanilla " + ShaderPatches.applied(ShaderPatches.VANILLA_GLOBALS + "-firelight")
                    + ", sodium " + ShaderPatches.applied(ShaderPatches.SODIUM_GLOBALS + "-firelight"));
            kind(mc, 0, floor, 3, Foliage.GRASS, Foliage.SINGLE, "short grass");
            kind(mc, 2, floor, 3, Foliage.GRASS, Foliage.LOWER, "tall grass bottom");
            kind(mc, 2, floor + 1, 3, Foliage.GRASS, Foliage.UPPER, "tall grass top");
            kind(mc, -2, floor, 3, Foliage.GRASS, Foliage.SINGLE, "poppy");
            kind(mc, -9, floor, 16, Foliage.CROPS, Foliage.SINGLE, "wheat");
            kind(mc, 6, floor + 3, 9, Foliage.LEAVES, Foliage.FULL, "oak leaves");
            kind(mc, -11, floor + 2, 6, Foliage.VINES, Foliage.WALL, "vine");
            kind(mc, 12, floor - 3, 18, Foliage.WATER, Foliage.STACK_UP, "kelp");
            kind(mc, 13, floor - 3, 18, Foliage.WATER, Foliage.SINGLE, "seagrass");
            kind(mc, 0, floor - 1, 3, Foliage.NONE, Foliage.SINGLE, "grass block");
            kind(mc, 7, floor, 9, Foliage.NONE, Foliage.SINGLE, "oak log");
            check(Foliage.tagging() == !shaders, "plants are tagged while a shader pack is on, or untagged without one");
            if (sodium && !shaders && !ShaderPatches.applied(ShaderPatches.SODIUM_TERRAIN + "-raw")) {
                check(SodiumHook.ran(), "the Sodium hook never ran");
            }
            if (!shaders && (Backend.RAW_UNIFORMS || ShaderPatches.applied(ShaderPatches.SODIUM_TERRAIN + "-raw"))) {
                check(RawUniforms.uploads() > 0, "the wind uniforms were never uploaded");
            }
            check(WindFrame.sending() == !shaders, "wind data sent while a shader pack is on, or not sent without one");
            screenshot(mc, "meadow");
            config.setStrength(3f);
        });
        motion(mc, "wind on");
        then(1, () -> {
            windMotion = motion();
            log(String.format(Locale.ROOT, "meadow motion with wind: %.3f", windMotion));
            log("wind strength sent " + WindFrame.strength());
            if (!shaders) {
                check(windMotion > 0.6, String.format(Locale.ROOT, "the meadow does not move in the wind (%.3f)", windMotion));
            }
            config.setEnabled(false);
        });
        then(60, () -> check(!Foliage.tagging() && !WindFrame.sending(), "wind still on after turning Windblown off"));
        motion(mc, "wind off");
        then(1, () -> {
            double still = motion();
            log(String.format(Locale.ROOT, "meadow motion with Windblown off: %.3f", still));
            check(shaders || still < windMotion * 0.25, String.format(Locale.ROOT, "the meadow still moves with Windblown off (%.3f)", still));
            config.setEnabled(true);
            config.setLeaves(false);
        });
        then(60, () -> {
            check(Foliage.tagging() == !shaders, "tagging did not come back after turning Windblown on");
            config.setLeaves(true);
            config.setStrength(1f);
        });

        then(20, () -> {
            run(mc, "tp @p 0.5 " + floor + " 2.5 0 10");
            rustles = Rustle.played();
        });
        for (int i = 0; i < 40; i++) {
            then(1, () -> mc.player.setDeltaMovement(0, mc.player.getDeltaMovement().y, 0.28));
        }
        then(10, () -> {
            long heard = Rustle.played() - rustles;
            log("rustles while walking through grass: " + heard);
            check(heard >= 3, "walking through the meadow made " + heard + " rustles");
            config.setRustle(false);
            run(mc, "tp @p 0.5 " + floor + " 2.5 0 10");
            rustles = Rustle.played();
        });
        for (int i = 0; i < 30; i++) {
            then(1, () -> mc.player.setDeltaMovement(0, mc.player.getDeltaMovement().y, 0.28));
        }
        then(10, () -> {
            check(Rustle.played() == rustles, "plants rustled with Rustling off");
            config.setRustle(true);
            run(mc, "tp @p 0.5 " + floor + " -1.5 0 12");
            run(mc, "weather thunder");
        });
        within(800, () -> WindState.weather() > 2.2f, () -> {
            log(String.format(Locale.ROOT, "wind in the thunderstorm: %.2fx", WindState.weather()));
            check(!config.weather() || WindState.weather() > 2.2f, "the storm did not raise the wind: " + WindState.weather());
            screenshot(mc, "meadow-storm");
            run(mc, "weather clear");
        });
        within(800, () -> WindState.weather() < 1.2f, () -> check(WindState.weather() < 1.2f, "the wind did not calm after the storm"));
        then(1, () -> run(mc, "tp @p 13.5 " + floor + " 15.5 0 45"));
        then(60, () -> screenshot(mc, "underwater"));

        if (irisLoaded()) {
            then(1, () -> setShaders(!shaders));
            then(160, () -> {
                boolean now = Compat.shaderPackInUse();
                log("toggled shaders mid-game, shader pack now " + now + ", wind sent " + WindFrame.sending());
                check(now != shaders, "toggling Iris shaders did not take effect");
                check(WindFrame.sending() == !now, "wrong wind state after toggling shaders");
                check(Foliage.tagging() == !now, "wrong tagging after toggling shaders");
                screenshot(mc, "shaders-toggled");
                setShaders(shaders);
            });
            then(160, () -> check(Compat.shaderPackInUse() == shaders, "toggling Iris shaders back did not take effect"));
        }

        then(20, () -> Screens.open(mc, new WindblownSettingsScreen(null, mc.options)));
        then(20, () -> {
            check(Screens.current(mc) instanceof WindblownSettingsScreen, "the settings screen did not open");
            screenshot(mc, "settings");
            AbstractSliderButton slider = slider(Screens.current(mc), "Wind Strength");
            check(slider.getMessage().getString().endsWith("100%"), "the strength slider does not start at 100%: " + slider.getMessage().getString());
            Ui.click(Screens.current(mc), Ui.x(slider) + slider.getWidth() - 2.0, Ui.y(slider) + slider.getHeight() / 2.0);
            check(Math.abs(config.strength() - 3f) < 1.0E-4f, "the strength slider did not reach 300%: " + config.strength());
            Ui.click(Screens.current(mc), Ui.x(slider) + 2.0, Ui.y(slider) + slider.getHeight() / 2.0);
            check(Math.abs(config.strength() - 0.1f) < 1.0E-4f, "the strength slider did not reach 10%: " + config.strength());
            AbstractSliderButton volume = slider(Screens.current(mc), "Rustle Volume");
            check(volume.getMessage().getString().endsWith("100%"), "the volume slider does not start at 100%: " + volume.getMessage().getString());
            Ui.click(Screens.current(mc), Ui.x(volume) + 2.0, Ui.y(volume) + volume.getHeight() / 2.0);
            check(config.rustleVolume() == 0f, "the volume slider did not reach 0%: " + config.rustleVolume());
        });
        then(5, () -> {
            Screens.open(mc, null);
            WindblownClient.saveConfig();
            check(read(WindblownClient.configPath()).contains("\"strength\": " + config.strength()), "the strength was not saved: " + read(WindblownClient.configPath()));
            config.resetToDefaults();
            Screens.open(mc, new WindblownSettingsScreen(null, mc.options));
        });
        then(5, () -> check(slider(Screens.current(mc), "Wind Strength").getMessage().getString().endsWith("100%"),
                "the strength slider did not come back at 100% after a reset"));
        then(5, () -> {
            Screens.open(mc, null);
            hideHud = false;
        });
    }

    private static void meadow(Minecraft mc, int f) {
        run(mc, "fill -16 " + f + " -4 16 " + (f + 10) + " 26 minecraft:air");
        run(mc, "fill -10 " + f + " 2 10 " + f + " 14 minecraft:short_grass");
        for (int x = -10; x <= 10; x += 2) {
            for (int z = 3; z <= 14; z += 3) {
                int px = x + Math.floorMod(z * 7, 3) - 1;
                run(mc, "setblock " + px + " " + f + " " + z + " minecraft:tall_grass[half=lower]");
                run(mc, "setblock " + px + " " + (f + 1) + " " + z + " minecraft:tall_grass[half=upper]");
            }
        }
        run(mc, "setblock 2 " + f + " 3 minecraft:tall_grass[half=lower]");
        run(mc, "setblock 2 " + (f + 1) + " 3 minecraft:tall_grass[half=upper]");
        String[] flowers = {"poppy", "dandelion", "cornflower", "oxeye_daisy", "azure_bluet", "allium"};
        for (int i = 0; i < 18; i++) {
            int x = -9 + (i * 7) % 19;
            int z = 4 + (i * 5) % 10;
            run(mc, "setblock " + x + " " + f + " " + z + " minecraft:" + flowers[i % flowers.length]);
        }
        run(mc, "setblock -2 " + f + " 3 minecraft:poppy");
        run(mc, "setblock 0 " + f + " 3 minecraft:short_grass");
        run(mc, "fill -10 " + (f - 1) + " 16 -6 " + (f - 1) + " 18 minecraft:farmland[moisture=7]");
        run(mc, "fill -10 " + f + " 16 -6 " + f + " 18 minecraft:wheat[age=7]");
        run(mc, "fill 7 " + f + " 9 7 " + (f + 3) + " 9 minecraft:oak_log");
        run(mc, "fill 5 " + (f + 2) + " 7 9 " + (f + 4) + " 11 minecraft:oak_leaves[persistent=true] keep");
        run(mc, "fill 6 " + (f + 5) + " 8 8 " + (f + 5) + " 10 minecraft:oak_leaves[persistent=true] keep");
        run(mc, "fill -12 " + f + " 4 -12 " + (f + 4) + " 8 minecraft:stone_bricks");
        run(mc, "fill -11 " + (f + 1) + " 5 -11 " + (f + 4) + " 7 minecraft:vine[west=true]");
        run(mc, "fill 11 " + (f - 3) + " 17 15 " + (f - 1) + " 21 minecraft:water");
        run(mc, "setblock 12 " + (f - 3) + " 18 minecraft:kelp_plant");
        run(mc, "setblock 12 " + (f - 2) + " 18 minecraft:kelp_plant");
        run(mc, "setblock 12 " + (f - 1) + " 18 minecraft:kelp");
        run(mc, "setblock 13 " + (f - 3) + " 18 minecraft:seagrass");
        run(mc, "setblock 14 " + (f - 3) + " 19 minecraft:seagrass");
    }

    private void kind(Minecraft mc, int x, int y, int z, int category, int shape, String what) {
        int info = Foliage.info(mc.level.getBlockState(new BlockPos(x, y, z)));
        check(Foliage.category(info) == category && (category == Foliage.NONE || Foliage.shape(info) == shape),
                what + " at " + x + " " + y + " " + z + " is " + mc.level.getBlockState(new BlockPos(x, y, z))
                        + ", category " + Foliage.category(info) + " shape " + Foliage.shape(info));
    }

    private void filmScene(Minecraft mc) {
        WindblownConfig config = WindblownClient.config();
        int f = ServerCompat.FLOOR;
        then(10, () -> {
            run(mc, "fill -24 " + f + " 15 24 " + (f + 12) + " 40 minecraft:air");
            run(mc, "fill -24 " + f + " 15 24 " + f + " 40 minecraft:short_grass");
            for (int x = -22; x <= 22; x += 2) {
                for (int z = 16; z <= 38; z += 2) {
                    if (Math.floorMod(x * 31 + z * 17, 5) < 2) {
                        run(mc, "setblock " + x + " " + f + " " + z + " minecraft:tall_grass[half=lower]");
                        run(mc, "setblock " + x + " " + (f + 1) + " " + z + " minecraft:tall_grass[half=upper]");
                    }
                }
            }
            for (int i = 0; i < 4; i++) {
                int x = -16 + i * 11;
                int z = 30 + (i % 2) * 5;
                run(mc, "fill " + x + " " + f + " " + z + " " + x + " " + (f + 4) + " " + z + " minecraft:birch_log");
                run(mc, "fill " + (x - 2) + " " + (f + 3) + " " + (z - 2) + " " + (x + 2) + " " + (f + 5) + " " + (z + 2)
                        + " minecraft:birch_leaves[persistent=true] keep");
                run(mc, "fill " + (x - 1) + " " + (f + 6) + " " + (z - 1) + " " + (x + 1) + " " + (f + 6) + " " + (z + 1)
                        + " minecraft:birch_leaves[persistent=true] keep");
            }
            String[] flowers = {"poppy", "dandelion", "cornflower", "oxeye_daisy", "azure_bluet"};
            for (int i = 0; i < 40; i++) {
                int x = -22 + (i * 13) % 45;
                int z = 16 + (i * 7) % 20;
                run(mc, "setblock " + x + " " + f + " " + z + " minecraft:" + flowers[i % flowers.length]);
            }
        });
        then(80, () -> {
            run(mc, "tp @p 0.5 " + (f + 1) + " 12.5 0 14");
            Ui.fov(mc, 70);
        });
        pan(0, 0, 14, 14, 1);
        then(60, () -> startFilm(mc, "1-breeze"));
        seconds(10, WindblownSelfTest::stopFilm);
        pan(-30, 30, 16, 10, 9);
        then(0, () -> startFilm(mc, "2-pan"));
        seconds(9, WindblownSelfTest::stopFilm);
        pan(0, 0, 14, 14, 1);
        then(0, () -> run(mc, "weather thunder"));
        within(800, () -> WindState.weather() > 2.2f, () -> startFilm(mc, "3-storm"));
        seconds(9, WindblownSelfTest::stopFilm);
        then(0, () -> run(mc, "weather clear"));
        within(800, () -> WindState.weather() < 1.1f, () -> config.setEnabled(false));
        then(60, () -> startFilm(mc, "4-off"));
        seconds(4, WindblownSelfTest::stopFilm);
        then(0, () -> config.setEnabled(true));
        still();
        then(5, () -> hideHud = false);
    }

    private int perfFrames;
    private long perfStart;

    private void perfScene(Minecraft mc) {
        WindblownConfig config = WindblownClient.config();
        int f = ServerCompat.FLOOR;
        then(10, () -> {
            run(mc, "fill -48 " + f + " -48 48 " + f + " 48 minecraft:short_grass");
            for (int x = -48; x <= 48; x += 3) {
                run(mc, "fill " + x + " " + f + " -48 " + x + " " + f + " 48 minecraft:tall_grass[half=lower]");
                run(mc, "fill " + x + " " + (f + 1) + " -48 " + x + " " + (f + 1) + " 48 minecraft:tall_grass[half=upper]");
            }
            run(mc, "tp @p 0.5 " + (f + 4) + " 0.5 45 25");
        });
        for (boolean on : new boolean[]{false, true, false, true}) {
            then(20, () -> config.setEnabled(on));
            then(100, () -> {
                perfFrames = frames;
                perfStart = System.nanoTime();
            });
            seconds(5, () -> {
                double fps = (frames - perfFrames) / ((System.nanoTime() - perfStart) / 1.0E9);
                log(String.format(Locale.ROOT, "fps with Windblown %s: %.1f", on ? "on" : "off", fps));
            });
        }
        then(5, () -> hideHud = false);
    }

    private long until;

    private void seconds(double length, Runnable action) {
        then(0, () -> until = System.nanoTime() + (long) (length * 1.0E9));
        when(() -> System.nanoTime() >= until, action);
    }

    private void motion(Minecraft mc, String label) {
        then(1, () -> {
            captures.clear();
            log("measuring meadow motion, " + label);
            run(mc, "tp @p 0.5 " + ServerCompat.FLOOR + " -1.5 0 12");
        });
        then(30, () -> {
        });
        for (int i = 0; i < 8; i++) {
            then(4, () -> {
                mc.player.setYRot(0f);
                mc.player.yRotO = 0f;
                mc.player.setXRot(12f);
                mc.player.xRotO = 12f;
                Captures.grab(Screens.renderTarget(mc), downscale(Screens.renderTarget(mc)), this::keep);
            });
        }
        within(40, () -> captures.size() >= 8, () -> {
            check(captures.size() >= 6, "only " + captures.size() + " captures arrived");
            int[] last = captures.get(captures.size() - 1);
            double mean = 0;
            for (int value : last) {
                mean += value;
            }
            mean /= last.length;
            double spread = 0;
            for (int value : last) {
                spread += (value - mean) * (value - mean);
            }
            spread = Math.sqrt(spread / last.length);
            log(String.format(Locale.ROOT, "meadow detail (brightness spread): %.1f", spread));
            check(spread > 6, String.format(Locale.ROOT, "the meadow is not on screen (brightness spread %.1f)", spread));
        });
    }

    private static int downscale(com.mojang.blaze3d.pipeline.RenderTarget target) {
        for (int factor : new int[]{4, 2}) {
            if (target.width % factor == 0 && target.height % factor == 0) {
                return factor;
            }
        }
        return 1;
    }

    private void keep(NativeImage image) {
        try {
            int x0 = image.getWidth() * 25 / 100;
            int x1 = image.getWidth() * 75 / 100;
            int y0 = image.getHeight() * 25 / 100;
            int y1 = image.getHeight() * 60 / 100;
            int[] values = new int[(x1 - x0) * (y1 - y0)];
            int i = 0;
            for (int y = y0; y < y1; y++) {
                for (int x = x0; x < x1; x++) {
                    int c = Captures.rgb(image, x, y);
                    values[i++] = (int) Math.round(0.299 * (c >> 16 & 0xFF) + 0.587 * (c >> 8 & 0xFF) + 0.114 * (c & 0xFF));
                }
            }
            captures.add(values);
        } finally {
            image.close();
        }
    }

    private double motion() {
        double total = 0;
        int pairs = 0;
        for (int i = 1; i < captures.size(); i++) {
            int[] a = captures.get(i - 1);
            int[] b = captures.get(i);
            if (a.length != b.length) {
                continue;
            }
            long sum = 0;
            for (int j = 0; j < a.length; j++) {
                sum += Math.abs(a[j] - b[j]);
            }
            total += sum / (double) a.length;
            pairs++;
        }
        return pairs == 0 ? 0 : total / pairs;
    }

    private static AbstractSliderButton slider(GuiEventListener node, String caption) {
        AbstractSliderButton found = findSlider(node, caption);
        check(found != null, "no slider " + caption);
        return found;
    }

    private static AbstractSliderButton findSlider(GuiEventListener node, String caption) {
        if (node instanceof AbstractSliderButton slider && slider.getMessage().getString().startsWith(caption)) {
            return slider;
        }
        if (node instanceof ContainerEventHandler container) {
            for (GuiEventListener child : container.children()) {
                AbstractSliderButton found = findSlider(child, caption);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static String read(Path file) {
        try {
            return Files.readString(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static boolean irisLoaded() {
        try {
            Class.forName("net.irisshaders.iris.api.v0.IrisApi", false, WindblownSelfTest.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }

    private static void setShaders(boolean enabled) {
        try {
            Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Object instance = api.getMethod("getInstance").invoke(null);
            Object config = api.getMethod("getConfig").invoke(instance);
            Class.forName("net.irisshaders.iris.api.v0.IrisApiConfig").getMethod("setShadersEnabledAndApply", boolean.class).invoke(config, enabled);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("could not toggle Iris shaders", e);
        }
    }

    private static boolean sodiumLoaded() {
        for (String name : new String[]{"net.caffeinemc.mods.sodium.client.gl.shader.GlProgram", "me.jellysquid.mods.sodium.client.gl.shader.GlProgram",
                "net.caffeinemc.mods.sodium.client.render.chunk.UniformBufferManager"}) {
            try {
                Class.forName(name, false, WindblownSelfTest.class.getClassLoader());
                return true;
            } catch (ClassNotFoundException | LinkageError e) {
                continue;
            }
        }
        return false;
    }

    static void run(Minecraft mc, String command) {
        IntegratedServer server = mc.getSingleplayerServer();
        server.execute(() -> ServerCompat.runCommand(server, command));
        if (command.contains("minecraft:short_grass")) {
            String legacy = command.replace("minecraft:short_grass", "minecraft:grass");
            server.execute(() -> ServerCompat.runCommand(server, legacy));
        }
    }

    private void screenshot(Minecraft mc, String name) {
        log("screenshot: " + name);
        shots.add(name);
        ServerCompat.screenshot(mc, Screens.renderTarget(mc));
    }

    private void rememberShots(Minecraft mc) {
        File[] files = new File(mc.gameDirectory, "screenshots").listFiles();
        if (files != null) {
            for (File file : files) {
                existingShots.add(file.getName());
            }
        }
    }

    private void nameShots(Minecraft mc) {
        File dir = new File(mc.gameDirectory, "screenshots");
        File[] files = dir.listFiles((parent, name) -> name.endsWith(".png") && !existingShots.contains(name));
        if (files == null) {
            return;
        }
        List<File> fresh = new ArrayList<>(List.of(files));
        fresh.sort(Comparator.comparingLong(File::lastModified).thenComparing(File::getName));
        for (int i = 0; i < fresh.size() && i < shots.size(); i++) {
            File target = new File(dir, String.format(Locale.ROOT, "wb-%02d-%s.png", i, shots.get(i)));
            if ((!target.exists() || target.delete()) && fresh.get(i).renameTo(target)) {
                log("saved " + target.getName());
            }
        }
    }

    private static void deleteWorld(Minecraft mc, String name) {
        Path dir = mc.gameDirectory.toPath().resolve("saves").resolve(name);
        if (!Files.exists(dir)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(dir)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    static void log(String message) {
        LogManager.getLogger("windblown-selftest").info("[windblown-selftest] " + message);
    }
}
