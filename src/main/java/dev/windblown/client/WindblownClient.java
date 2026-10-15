package dev.windblown.client;

import dev.windblown.config.WindblownConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;

public final class WindblownClient {
    public static final String MOD_ID = "windblown";
    private static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    private static WindblownConfig config = new WindblownConfig();
    private static Path configPath;
    private static boolean failed;

    private WindblownClient() {
    }

    public static void init(Path configDir) {
        configPath = configDir.resolve(WindblownConfig.FILE_NAME);
        config = WindblownConfig.load(configPath);
    }

    public static WindblownConfig config() {
        return config;
    }

    public static Path configPath() {
        return configPath;
    }

    public static void saveConfig() {
        config.saveQuietly(configPath);
    }

    public static boolean active() {
        return !failed && config.enabled();
    }

    public static void fail(Throwable e) {
        if (!failed) {
            failed = true;
            LOGGER.error("Windblown hit an error and turned itself off for this session", e);
        }
    }
}
