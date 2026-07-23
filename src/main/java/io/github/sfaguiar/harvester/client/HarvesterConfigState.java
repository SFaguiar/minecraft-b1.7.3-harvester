package io.github.sfaguiar.harvester.client;

import io.github.sfaguiar.harvester.config.HarvesterConfig;
import io.github.sfaguiar.harvester.config.HarvesterConfigLoader;
import io.github.sfaguiar.harvester.platform.HarvesterEntrypoint;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Client-side holder for the single {@link HarvesterConfig} instance loaded
 * for this session. Loaded exactly once, at client startup
 * ({@link HarvesterClientEntrypoint#onInitializeClient()}); every other
 * class in {@code client}/{@code mixin} reads {@link #current()} instead of
 * re-parsing the file or duplicating a default constant.
 *
 * <p>Resolving the config file's location via {@link FabricLoader} is
 * deliberately isolated to this class — {@link HarvesterConfigLoader}
 * itself takes a plain {@link Path} and has no Fabric dependency, so it
 * stays unit-testable without Fabric Loader's Knot classloading applied.
 */
public final class HarvesterConfigState {

    private static volatile HarvesterConfig current = HarvesterConfig.DEFAULTS;

    private HarvesterConfigState() {
    }

    /** Resolves, loads (creating the file with defaults if absent), and stores the config. Idempotent per call. */
    public static void load() {
        Path configFile = FabricLoader.getInstance().getConfigDir().resolve(HarvesterConfigLoader.FILE_NAME);
        HarvesterConfigLoader.LoadResult result = HarvesterConfigLoader.loadOrCreateDefaults(configFile);

        for (String warning : result.warnings()) {
            HarvesterEntrypoint.LOGGER.warn("[HARVEST-CONFIG] {}", warning);
        }
        current = result.config();
        HarvesterEntrypoint.LOGGER.info("[HARVEST-CONFIG] Loaded from {}: {}", configFile, current);
    }

    /**
     * Persists {@code config} to the same {@code harvester.properties} the
     * loader reads (the single authoritative source), then makes it the live
     * config for this session. Called by the in-game config screen on apply.
     * Never throws: a write failure is logged and the in-memory config is
     * still updated so the session reflects the change even if the disk write
     * failed.
     */
    public static void save(HarvesterConfig config) {
        Objects.requireNonNull(config, "config");
        Path configFile = FabricLoader.getInstance().getConfigDir().resolve(HarvesterConfigLoader.FILE_NAME);
        try {
            HarvesterConfigLoader.writeConfigFile(configFile, config);
            HarvesterEntrypoint.LOGGER.info("[HARVEST-CONFIG] Saved to {}: {}", configFile, config);
        } catch (IOException e) {
            HarvesterEntrypoint.LOGGER.warn(
                    "[HARVEST-CONFIG] Could not save to {} ({}); applying in memory only.", configFile, e.getMessage()
            );
        }
        current = config;
    }

    /** The most recently loaded config; {@link HarvesterConfig#DEFAULTS} until {@link #load()} has run. */
    public static HarvesterConfig current() {
        return current;
    }

    /** Test-only override, bypassing file resolution entirely. */
    static void setForTesting(HarvesterConfig config) {
        current = Objects.requireNonNull(config, "config");
    }
}
