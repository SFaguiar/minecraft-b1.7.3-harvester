package io.github.sfaguiar.harvester.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

/**
 * Pure file-to-{@link HarvesterConfig} loading and property parsing. Only
 * {@code java.nio}/{@code java.util} types cross this class's boundary —
 * no Minecraft, StationAPI, or Fabric import — so every behavior here is
 * unit-testable against a JUnit {@code @TempDir}, with no Minecraft
 * started. Resolving the real config file's location (via Fabric Loader's
 * config directory) is deliberately kept out of this class; the caller
 * passes the already-resolved {@link Path}.
 *
 * <p>Contract: a missing file is not an error — defaults are written and
 * returned. A present file with a missing or invalid property falls back
 * to that single property's own default and yields at most one warning for
 * it; unknown properties are silently ignored. A file that cannot be read
 * at all yields defaults and one warning.
 */
public final class HarvesterConfigLoader {

    public static final String FILE_NAME = "harvester.properties";

    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_MAX_CHAIN = "maxChain";
    private static final String KEY_NEIGHBORHOOD = "neighborhood";
    private static final String KEY_DIAGNOSTIC_LOGGING = "diagnosticLogging";

    private static final String DEFAULT_FILE_CONTENTS =
            "# Harvester configuration.\n"
            + "#\n"
            + "# enabled: true or false. Controls only the automatic additional-block\n"
            + "# chain in singleplayer; false never prevents breaking the origin block\n"
            + "# by hand.\n"
            + "enabled=true\n"
            + "\n"
            + "# maxChain: total blocks per activation, including the origin block.\n"
            + "# Must be a whole number from 1 to 100 (100 is the largest limit the\n"
            + "# legacy Harvester's UI ever exposed); an invalid or out-of-range value\n"
            + "# falls back to 64.\n"
            + "maxChain=64\n"
            + "\n"
            + "# neighborhood: legacy_26 (default, full 26-neighbor adjacency, matches\n"
            + "# legacy Harvester 1.x) or orthogonal_6 (face adjacency only, no diagonals).\n"
            + "neighborhood=legacy_26\n"
            + "\n"
            + "# diagnosticLogging: true or false. When false, per-candidate and\n"
            + "# durability-snapshot debug logs are suppressed; warnings and errors are\n"
            + "# always logged regardless of this setting.\n"
            + "diagnosticLogging=false\n";

    private HarvesterConfigLoader() {
    }

    /**
     * Loads {@code configFile}, creating it with default contents first if
     * it does not yet exist. Never throws: any {@link IOException} (missing
     * parent, permissions, unreadable file, ...) is reported as a single
     * warning and defaults are returned instead.
     */
    public static LoadResult loadOrCreateDefaults(Path configFile) {
        try {
            if (Files.notExists(configFile)) {
                writeDefaultFile(configFile);
                return new LoadResult(HarvesterConfig.DEFAULTS, List.of());
            }
            Properties properties = new Properties();
            try (InputStream in = Files.newInputStream(configFile)) {
                properties.load(in);
            }
            return parse(properties);
        } catch (IOException e) {
            return new LoadResult(
                    HarvesterConfig.DEFAULTS,
                    List.of("Could not read or create " + configFile + " (" + e.getMessage()
                            + "); using built-in defaults.")
            );
        }
    }

    /** Parses an already-loaded {@link Properties}; used directly by tests. */
    public static LoadResult parse(Properties properties) {
        List<String> warnings = new ArrayList<>();
        boolean enabled = parseBoolean(properties, KEY_ENABLED, HarvesterConfig.DEFAULT_ENABLED, warnings);
        int maxChain = parseMaxChain(properties, warnings);
        NeighborhoodChoice neighborhood = parseNeighborhood(properties, warnings);
        boolean diagnosticLogging =
                parseBoolean(properties, KEY_DIAGNOSTIC_LOGGING, HarvesterConfig.DEFAULT_DIAGNOSTIC_LOGGING, warnings);
        return new LoadResult(
                new HarvesterConfig(enabled, maxChain, neighborhood, diagnosticLogging),
                warnings
        );
    }

    private static boolean parseBoolean(
            Properties properties, String key, boolean defaultValue, List<String> warnings
    ) {
        String raw = properties.getProperty(key);
        if (raw == null) {
            return defaultValue;
        }
        String trimmed = raw.trim();
        if (trimmed.equalsIgnoreCase("true")) {
            return true;
        }
        if (trimmed.equalsIgnoreCase("false")) {
            return false;
        }
        warnings.add("Invalid '" + key + "' value '" + raw + "' (expected true/false); using default ("
                + defaultValue + ").");
        return defaultValue;
    }

    private static int parseMaxChain(Properties properties, List<String> warnings) {
        String raw = properties.getProperty(KEY_MAX_CHAIN);
        if (raw == null) {
            return HarvesterConfig.DEFAULT_MAX_CHAIN;
        }
        int parsed;
        try {
            parsed = Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            warnings.add("Invalid '" + KEY_MAX_CHAIN + "' value '" + raw + "' (not a whole number); using default ("
                    + HarvesterConfig.DEFAULT_MAX_CHAIN + ").");
            return HarvesterConfig.DEFAULT_MAX_CHAIN;
        }
        if (parsed < 1) {
            warnings.add("Invalid '" + KEY_MAX_CHAIN + "' value '" + raw + "' (must be >= 1); using default ("
                    + HarvesterConfig.DEFAULT_MAX_CHAIN + ").");
            return HarvesterConfig.DEFAULT_MAX_CHAIN;
        }
        if (parsed > HarvesterConfig.MAX_ALLOWED_MAX_CHAIN) {
            warnings.add("Invalid '" + KEY_MAX_CHAIN + "' value '" + raw + "' (must be <= "
                    + HarvesterConfig.MAX_ALLOWED_MAX_CHAIN + "); using default ("
                    + HarvesterConfig.DEFAULT_MAX_CHAIN + ").");
            return HarvesterConfig.DEFAULT_MAX_CHAIN;
        }
        return parsed;
    }

    private static NeighborhoodChoice parseNeighborhood(Properties properties, List<String> warnings) {
        String raw = properties.getProperty(KEY_NEIGHBORHOOD);
        if (raw == null) {
            return HarvesterConfig.DEFAULT_NEIGHBORHOOD;
        }
        return NeighborhoodChoice.fromPropertyValue(raw.trim()).orElseGet(() -> {
            warnings.add("Invalid '" + KEY_NEIGHBORHOOD + "' value '" + raw + "' (expected legacy_26 or orthogonal_6); "
                    + "using default (" + HarvesterConfig.DEFAULT_NEIGHBORHOOD.propertyValue() + ").");
            return HarvesterConfig.DEFAULT_NEIGHBORHOOD;
        });
    }

    private static void writeDefaultFile(Path configFile) throws IOException {
        Path parent = configFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(configFile, DEFAULT_FILE_CONTENTS, StandardCharsets.UTF_8);
    }

    /** {@link HarvesterConfig} plus at most one warning per invalid property found. */
    public static final class LoadResult {

        private final HarvesterConfig config;
        private final List<String> warnings;

        public LoadResult(HarvesterConfig config, List<String> warnings) {
            this.config = Objects.requireNonNull(config, "config");
            this.warnings = List.copyOf(warnings);
        }

        public HarvesterConfig config() {
            return config;
        }

        public List<String> warnings() {
            return warnings;
        }
    }
}
