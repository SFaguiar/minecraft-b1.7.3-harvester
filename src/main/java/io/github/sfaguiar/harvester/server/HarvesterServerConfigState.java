package io.github.sfaguiar.harvester.server;

import io.github.sfaguiar.harvester.config.HarvesterConfig;
import io.github.sfaguiar.harvester.config.HarvesterConfigLoader;
import io.github.sfaguiar.harvester.platform.HarvesterEntrypoint;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Server-side holder for the single {@link HarvesterConfig} instance
 * loaded for this dedicated-server process. Mirrors
 * {@code client.HarvesterConfigState} exactly (same file, same loader,
 * same defaults-on-missing/invalid semantics) but is a distinct
 * server-package class — a dedicated server never links the {@code
 * client} package, and a client never links this one; see
 * {@code ARCHITECTURE.md}, "Configuration (design)" for why one shared
 * {@code harvester.properties} with a server-only key is not a
 * side-safety violation (the file is shared, the code reading it is
 * not).
 *
 * <p>Loaded exactly once, at server startup
 * ({@link io.github.sfaguiar.harvester.server.HarvesterServerEntrypoint#onInitializeServer()}).
 */
public final class HarvesterServerConfigState {

    private static volatile HarvesterConfig current = HarvesterConfig.DEFAULTS;

    private HarvesterServerConfigState() {
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

    /** The most recently loaded config; {@link HarvesterConfig#DEFAULTS} until {@link #load()} has run. */
    public static HarvesterConfig current() {
        return current;
    }

    /** Test-only override, bypassing file resolution entirely. */
    static void setForTesting(HarvesterConfig config) {
        current = Objects.requireNonNull(config, "config");
    }
}
