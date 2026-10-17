package dev.windblown.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class WindblownConfig {
    public static final String FILE_NAME = "windblown.json";

    private static final Logger LOGGER = LogManager.getLogger("windblown");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static final int MIN_STRENGTH_PERCENT = 10;
    public static final int MAX_STRENGTH_PERCENT = 300;
    public static final int MAX_VOLUME_PERCENT = 200;
    public static final int MAX_GUSTS_PERCENT = 100;

    private boolean enabled = true;
    private float strength = 1.0f;
    private float gusts = 1.0f;
    private boolean grass = true;
    private boolean crops = true;
    private boolean leaves = true;
    private boolean vines = true;
    private boolean waterPlants = true;
    private boolean weather = true;
    private boolean rustle = true;
    private float rustleVolume = 1.0f;

    public boolean enabled() {
        return enabled;
    }

    public void setEnabled(boolean value) {
        enabled = value;
    }

    public float strength() {
        return clamp(strength, MIN_STRENGTH_PERCENT / 100f, MAX_STRENGTH_PERCENT / 100f);
    }

    public void setStrength(float value) {
        strength = value;
    }

    public float gusts() {
        return clamp(gusts, 0f, MAX_GUSTS_PERCENT / 100f);
    }

    public void setGusts(float value) {
        gusts = value;
    }

    public float rustleVolume() {
        return clamp(rustleVolume, 0f, MAX_VOLUME_PERCENT / 100f);
    }

    public void setRustleVolume(float value) {
        rustleVolume = value;
    }

    public boolean grass() {
        return grass;
    }

    public void setGrass(boolean value) {
        grass = value;
    }

    public boolean crops() {
        return crops;
    }

    public void setCrops(boolean value) {
        crops = value;
    }

    public boolean leaves() {
        return leaves;
    }

    public void setLeaves(boolean value) {
        leaves = value;
    }

    public boolean vines() {
        return vines;
    }

    public void setVines(boolean value) {
        vines = value;
    }

    public boolean waterPlants() {
        return waterPlants;
    }

    public void setWaterPlants(boolean value) {
        waterPlants = value;
    }

    public boolean weather() {
        return weather;
    }

    public void setWeather(boolean value) {
        weather = value;
    }

    public boolean rustle() {
        return rustle;
    }

    public void setRustle(boolean value) {
        rustle = value;
    }

    public void resetToDefaults() {
        WindblownConfig fresh = new WindblownConfig();
        enabled = fresh.enabled;
        strength = fresh.strength;
        gusts = fresh.gusts;
        grass = fresh.grass;
        crops = fresh.crops;
        leaves = fresh.leaves;
        vines = fresh.vines;
        waterPlants = fresh.waterPlants;
        weather = fresh.weather;
        rustle = fresh.rustle;
        rustleVolume = fresh.rustleVolume;
    }

    private static float clamp(float value, float min, float max) {
        return Float.isNaN(value) ? min : Math.max(min, Math.min(max, value));
    }

    public static WindblownConfig load(Path file) {
        if (!Files.isRegularFile(file)) {
            WindblownConfig fresh = new WindblownConfig();
            fresh.saveQuietly(file);
            return fresh;
        }

        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            WindblownConfig loaded = GSON.fromJson(reader, WindblownConfig.class);
            if (loaded == null) {
                throw new JsonParseException("config file is empty");
            }
            return loaded;
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Could not read {}; using defaults. {}", file, e.toString());
            moveAside(file);
            WindblownConfig fresh = new WindblownConfig();
            fresh.saveQuietly(file);
            return fresh;
        }
    }

    public void save(Path file) throws IOException {
        Path absolute = file.toAbsolutePath();
        Path parent = absolute.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Path temp = absolute.resolveSibling(absolute.getFileName() + ".tmp");
        Files.write(temp, (GSON.toJson(this) + System.lineSeparator()).getBytes(StandardCharsets.UTF_8));
        try {
            Files.move(temp, absolute, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temp, absolute, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public void saveQuietly(Path file) {
        if (file == null) {
            return;
        }
        try {
            save(file);
        } catch (IOException e) {
            LOGGER.warn("Could not save {}: {}", file, e.toString());
        }
    }

    private static void moveAside(Path file) {
        try {
            Files.move(file, file.resolveSibling(file.getFileName() + ".broken"), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            LOGGER.warn("Could not back up unreadable config {}: {}", file, e.toString());
        }
    }
}
