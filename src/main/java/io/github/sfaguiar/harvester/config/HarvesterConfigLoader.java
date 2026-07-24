package io.github.sfaguiar.harvester.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

/**
 * Pure file-to-{@link HarvesterConfig} loading, parsing, and rendering. Only
 * {@code java.nio}/{@code java.util} types cross this class's boundary — no
 * Minecraft, StationAPI, or Fabric import — so every behavior here is
 * unit-testable against a JUnit {@code @TempDir}, with no Minecraft started.
 * Resolving the real config file's location (via Fabric Loader's config
 * directory) is deliberately kept out of this class; the caller passes the
 * already-resolved {@link Path}.
 *
 * <p>Contract: a missing file is not an error — a fully documented default
 * file is written and defaults returned. A present file with a missing or
 * invalid property falls back to that single property's own default and
 * yields at most one warning for it; unknown properties are silently ignored.
 * A present file that predates any current key is <em>migrated in place,
 * non-destructively</em> — the file is rewritten with every documented key,
 * preserving each value the user had set, appending the new keys at their
 * defaults. A file that cannot be read at all yields defaults and one warning.
 */
public final class HarvesterConfigLoader {

    public static final String FILE_NAME = "harvester.properties";

    /** Bumped whenever the documented key set changes; recorded in the file, drives migration. */
    public static final int SCHEMA_VERSION = 2;

    private static final String KEY_SCHEMA_VERSION = "schemaVersion";
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_MAX_CHAIN = "maxChain";
    private static final String KEY_NEIGHBORHOOD = "neighborhood";
    private static final String KEY_DIAGNOSTIC_LOGGING = "diagnosticLogging";
    private static final String KEY_HARVEST_LOGS = "harvestLogs";
    private static final String KEY_HARVEST_ORES = "harvestOres";
    private static final String KEY_MULTIPLAYER_ALLOWED = "multiplayerAllowed";
    private static final String KEY_CONSOLIDATE_DROPS = "consolidateDrops";
    private static final String KEY_HARVEST_DIRT = "harvestDirt";
    private static final String KEY_HARVEST_GRAVEL = "harvestGravel";
    private static final String KEY_HARVEST_LEAVES = "harvestLeaves";
    private static final String KEY_HARVEST_CROPS = "harvestCrops";
    private static final String KEY_UNDERGROUND_NO_SKY = "undergroundRequiresNoSky";
    private static final String KEY_UNDERGROUND_MAX_Y = "undergroundMaxY";
    private static final String KEY_UNDERGROUND_OVERWORLD_ONLY = "undergroundOverworldOnly";
    private static final String KEY_ALLOWLIST = "allowlist";
    private static final String KEY_DENYLIST = "denylist";
    private static final String KEY_TOOL_AXE_IDS = "toolAxeIds";
    private static final String KEY_TOOL_SHOVEL_IDS = "toolShovelIds";
    private static final String KEY_TOOL_SHEARS_IDS = "toolShearsIds";
    private static final String KEY_TOOL_HOE_IDS = "toolHoeIds";

    /** Every documented key; a present file missing any of these triggers non-destructive migration. */
    private static final String[] ALL_KEYS = {
            KEY_SCHEMA_VERSION, KEY_ENABLED, KEY_MAX_CHAIN, KEY_NEIGHBORHOOD, KEY_DIAGNOSTIC_LOGGING,
            KEY_HARVEST_LOGS, KEY_HARVEST_ORES, KEY_MULTIPLAYER_ALLOWED, KEY_CONSOLIDATE_DROPS,
            KEY_HARVEST_DIRT, KEY_HARVEST_GRAVEL, KEY_HARVEST_LEAVES, KEY_HARVEST_CROPS,
            KEY_UNDERGROUND_NO_SKY, KEY_UNDERGROUND_MAX_Y, KEY_UNDERGROUND_OVERWORLD_ONLY,
            KEY_ALLOWLIST, KEY_DENYLIST, KEY_TOOL_AXE_IDS, KEY_TOOL_SHOVEL_IDS,
            KEY_TOOL_SHEARS_IDS, KEY_TOOL_HOE_IDS
    };

    private HarvesterConfigLoader() {
    }

    /**
     * Loads {@code configFile}, creating it with default contents first if it
     * does not yet exist, or migrating it in place if it is missing any
     * current key. Never throws: any {@link IOException} is reported as a
     * single warning and defaults are returned instead.
     */
    public static LoadResult loadOrCreateDefaults(Path configFile) {
        try {
            if (Files.notExists(configFile)) {
                writeConfigFile(configFile, HarvesterConfig.DEFAULTS);
                return new LoadResult(HarvesterConfig.DEFAULTS, List.of());
            }
            Properties properties = new Properties();
            try (InputStream in = Files.newInputStream(configFile)) {
                properties.load(in);
            }
            LoadResult parsed = parse(properties);
            if (isMissingAnyKey(properties)) {
                List<String> warnings = new ArrayList<>(parsed.warnings());
                try {
                    writeConfigFile(configFile, parsed.config());
                    warnings.add("Migrated " + configFile
                            + " to the current schema (added new keys at their defaults; existing values preserved).");
                } catch (IOException e) {
                    warnings.add("Could not migrate " + configFile + " (" + e.getMessage()
                            + "); loaded values are still in effect for this session.");
                }
                return new LoadResult(parsed.config(), warnings);
            }
            return parsed;
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
        HarvesterConfig config = new HarvesterConfig.Builder()
                .enabled(parseBoolean(properties, KEY_ENABLED, HarvesterConfig.DEFAULT_ENABLED, warnings))
                .maxChain(parseMaxChain(properties, warnings))
                .neighborhood(parseNeighborhood(properties, warnings))
                .diagnosticLogging(parseBoolean(
                        properties, KEY_DIAGNOSTIC_LOGGING, HarvesterConfig.DEFAULT_DIAGNOSTIC_LOGGING, warnings))
                .harvestLogs(parseBoolean(properties, KEY_HARVEST_LOGS, HarvesterConfig.DEFAULT_HARVEST_LOGS, warnings))
                .harvestOres(parseBoolean(properties, KEY_HARVEST_ORES, HarvesterConfig.DEFAULT_HARVEST_ORES, warnings))
                .multiplayerAllowed(parseBoolean(
                        properties, KEY_MULTIPLAYER_ALLOWED, HarvesterConfig.DEFAULT_MULTIPLAYER_ALLOWED, warnings))
                .consolidateDrops(parseBoolean(
                        properties, KEY_CONSOLIDATE_DROPS, HarvesterConfig.DEFAULT_CONSOLIDATE_DROPS, warnings))
                .harvestDirt(parseBoolean(properties, KEY_HARVEST_DIRT, HarvesterConfig.DEFAULT_HARVEST_DIRT, warnings))
                .harvestGravel(parseBoolean(
                        properties, KEY_HARVEST_GRAVEL, HarvesterConfig.DEFAULT_HARVEST_GRAVEL, warnings))
                .harvestLeaves(parseBoolean(
                        properties, KEY_HARVEST_LEAVES, HarvesterConfig.DEFAULT_HARVEST_LEAVES, warnings))
                .harvestCrops(parseBoolean(
                        properties, KEY_HARVEST_CROPS, HarvesterConfig.DEFAULT_HARVEST_CROPS, warnings))
                .undergroundRequiresNoSky(parseBoolean(
                        properties, KEY_UNDERGROUND_NO_SKY, HarvesterConfig.DEFAULT_UNDERGROUND_REQUIRES_NO_SKY, warnings))
                .undergroundMaxY(parseBoundedInt(
                        properties, KEY_UNDERGROUND_MAX_Y, HarvesterConfig.DEFAULT_UNDERGROUND_MAX_Y,
                        HarvesterConfig.MIN_UNDERGROUND_MAX_Y, HarvesterConfig.MAX_UNDERGROUND_MAX_Y, warnings))
                .undergroundOverworldOnly(parseBoolean(
                        properties, KEY_UNDERGROUND_OVERWORLD_ONLY, HarvesterConfig.DEFAULT_UNDERGROUND_OVERWORLD_ONLY,
                        warnings))
                .allowlist(parseStringSet(properties, KEY_ALLOWLIST))
                .denylist(parseStringSet(properties, KEY_DENYLIST))
                .toolAxeIds(parseIntSet(properties, KEY_TOOL_AXE_IDS, warnings))
                .toolShovelIds(parseIntSet(properties, KEY_TOOL_SHOVEL_IDS, warnings))
                .toolShearsIds(parseIntSet(properties, KEY_TOOL_SHEARS_IDS, warnings))
                .toolHoeIds(parseIntSet(properties, KEY_TOOL_HOE_IDS, warnings))
                .build();
        return new LoadResult(config, warnings);
    }

    private static boolean isMissingAnyKey(Properties properties) {
        for (String key : ALL_KEYS) {
            if (properties.getProperty(key) == null) {
                return true;
            }
        }
        return false;
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
        return parseBoundedInt(
                properties, KEY_MAX_CHAIN, HarvesterConfig.DEFAULT_MAX_CHAIN, 1, HarvesterConfig.MAX_ALLOWED_MAX_CHAIN,
                warnings
        );
    }

    private static int parseBoundedInt(
            Properties properties, String key, int defaultValue, int min, int max, List<String> warnings
    ) {
        String raw = properties.getProperty(key);
        if (raw == null) {
            return defaultValue;
        }
        int parsed;
        try {
            parsed = Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            warnings.add("Invalid '" + key + "' value '" + raw + "' (not a whole number); using default ("
                    + defaultValue + ").");
            return defaultValue;
        }
        if (parsed < min || parsed > max) {
            warnings.add("Invalid '" + key + "' value '" + raw + "' (must be " + min + ".." + max + "); using default ("
                    + defaultValue + ").");
            return defaultValue;
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

    /** Comma-separated identifiers; blanks ignored. Preserves insertion order for stable file rendering. */
    private static Set<String> parseStringSet(Properties properties, String key) {
        String raw = properties.getProperty(key);
        Set<String> out = new LinkedHashSet<>();
        if (raw == null) {
            return out;
        }
        for (String token : raw.split(",")) {
            String trimmed = token.trim();
            if (!trimmed.isEmpty()) {
                out.add(trimmed);
            }
        }
        return out;
    }

    /** Comma-separated whole numbers; each invalid token yields one warning and is skipped. */
    private static Set<Integer> parseIntSet(Properties properties, String key, List<String> warnings) {
        String raw = properties.getProperty(key);
        Set<Integer> out = new LinkedHashSet<>();
        if (raw == null) {
            return out;
        }
        for (String token : raw.split(",")) {
            String trimmed = token.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                out.add(Integer.parseInt(trimmed));
            } catch (NumberFormatException e) {
                warnings.add("Invalid '" + key + "' entry '" + trimmed + "' (not a whole number); skipped.");
            }
        }
        return out;
    }

    /**
     * Writes the fully documented config file for {@code config}, atomically
     * where possible (write to a sibling temp file, then move). Public so the
     * client-side config screen and the migration path both render the exact
     * same canonical file format from a {@link HarvesterConfig}.
     */
    public static void writeConfigFile(Path configFile, HarvesterConfig config) throws IOException {
        Path parent = configFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        String content = render(config);
        Path tmp = parent != null ? parent.resolve(FILE_NAME + ".tmp") : Path.of(FILE_NAME + ".tmp");
        Files.writeString(tmp, content, StandardCharsets.UTF_8);
        try {
            Files.move(tmp, configFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                    java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException atomicUnsupported) {
            Files.move(tmp, configFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String render(HarvesterConfig c) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Harvester configuration. Single authoritative source of truth.\n");
        sb.append("# ").append(KEY_SCHEMA_VERSION).append('=').append(SCHEMA_VERSION)
                .append(" — do not edit; used for non-destructive migration.\n");
        sb.append(KEY_SCHEMA_VERSION).append('=').append(SCHEMA_VERSION).append('\n');
        sb.append('\n');
        sb.append("# enabled: master toggle for the automatic additional-block chain (never the manual origin break).\n");
        sb.append(KEY_ENABLED).append('=').append(c.enabled()).append('\n');
        sb.append('\n');
        sb.append("# maxChain: total blocks per activation, including the origin. Whole number 1..100; invalid falls back to 64.\n");
        sb.append(KEY_MAX_CHAIN).append('=').append(c.maxChain()).append('\n');
        sb.append('\n');
        sb.append("# neighborhood: legacy_26 (full 26-neighbor, matches legacy Harvester 1.x) or orthogonal_6 (faces only).\n");
        sb.append("# Applies to logs and ores; dirt/gravel/leaves use 6 faces and crops use horizontal-4 regardless.\n");
        sb.append(KEY_NEIGHBORHOOD).append('=').append(c.neighborhood().propertyValue()).append('\n');
        sb.append('\n');
        sb.append("# diagnosticLogging: verbose per-candidate/durability debug logs. Warnings/errors always log regardless.\n");
        sb.append(KEY_DIAGNOSTIC_LOGGING).append('=').append(c.diagnosticLogging()).append('\n');
        sb.append('\n');
        sb.append("# consolidateDrops: merge the whole action's drops into stacks at the center of the origin block.\n");
        sb.append("# Vanilla drop calculation is unchanged; only where the item entities appear changes.\n");
        sb.append(KEY_CONSOLIDATE_DROPS).append('=').append(c.consolidateDrops()).append('\n');
        sb.append('\n');
        sb.append("# Category toggles. Each gates only its own automatic chain; a false toggle never blocks a manual break.\n");
        sb.append("# Logs require an axe, ores a suitable pickaxe, dirt/gravel a shovel, leaves shears, crops a hoe.\n");
        sb.append(KEY_HARVEST_LOGS).append('=').append(c.harvestLogs()).append('\n');
        sb.append(KEY_HARVEST_ORES).append('=').append(c.harvestOres()).append('\n');
        sb.append(KEY_HARVEST_DIRT).append('=').append(c.harvestDirt()).append('\n');
        sb.append(KEY_HARVEST_GRAVEL).append('=').append(c.harvestGravel()).append('\n');
        sb.append(KEY_HARVEST_LEAVES).append('=').append(c.harvestLeaves()).append('\n');
        sb.append(KEY_HARVEST_CROPS).append('=').append(c.harvestCrops()).append('\n');
        sb.append('\n');
        sb.append("# Underground rule for dirt/gravel: a block chains only when Overworld + no direct sky + Y <= undergroundMaxY.\n");
        sb.append(KEY_UNDERGROUND_NO_SKY).append('=').append(c.undergroundRequiresNoSky()).append('\n');
        sb.append(KEY_UNDERGROUND_MAX_Y).append('=').append(c.undergroundMaxY()).append('\n');
        sb.append(KEY_UNDERGROUND_OVERWORLD_ONLY).append('=').append(c.undergroundOverworldOnly()).append('\n');
        sb.append('\n');
        sb.append("# allowlist/denylist: comma-separated block identifiers (e.g. minecraft:dirt). Precedence:\n");
        sb.append("# denylist > allowlist > category toggle > default. denylist always blocks; allowlist only releases\n");
        sb.append("# a block whose category is already resolvable (it never invents a category).\n");
        sb.append(KEY_ALLOWLIST).append('=').append(joinStrings(c.allowlist())).append('\n');
        sb.append(KEY_DENYLIST).append('=').append(joinStrings(c.denylist())).append('\n');
        sb.append('\n');
        sb.append("# Tool ID allowlists per category, for mod tools that do not extend the vanilla tool class.\n");
        sb.append("# Comma-separated item IDs; vanilla tools are always recognized without listing them here.\n");
        sb.append(KEY_TOOL_AXE_IDS).append('=').append(joinInts(c.toolAxeIds())).append('\n');
        sb.append(KEY_TOOL_SHOVEL_IDS).append('=').append(joinInts(c.toolShovelIds())).append('\n');
        sb.append(KEY_TOOL_SHEARS_IDS).append('=').append(joinInts(c.toolShearsIds())).append('\n');
        sb.append(KEY_TOOL_HOE_IDS).append('=').append(joinInts(c.toolHoeIds())).append('\n');
        sb.append('\n');
        sb.append("# multiplayerAllowed: SERVER-ONLY. A dedicated server announces Harvester support only when true.\n");
        sb.append("# false (default) keeps the server silent; it never disconnects a client either way. Ignored by a client.\n");
        sb.append(KEY_MULTIPLAYER_ALLOWED).append('=').append(c.multiplayerAllowed()).append('\n');
        return sb.toString();
    }

    private static String joinStrings(Set<String> values) {
        return String.join(",", values);
    }

    private static String joinInts(Set<Integer> values) {
        Set<Integer> sorted = new TreeSet<>(values);
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (int v : sorted) {
            if (!first) {
                sb.append(',');
            }
            sb.append(v);
            first = false;
        }
        return sb.toString();
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
