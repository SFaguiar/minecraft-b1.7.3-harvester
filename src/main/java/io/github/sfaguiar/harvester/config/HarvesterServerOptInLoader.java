package io.github.sfaguiar.harvester.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

/**
 * Pure file-to-boolean loading for the client's per-server
 * {@code multiplayerOptIn} preference — one small properties file per
 * server identity, mirroring {@link HarvesterConfigLoader}'s exact
 * contract (missing file → write default-false contents and return
 * {@code false}; invalid value → default {@code false} plus one
 * warning; unreadable file → default {@code false} plus one warning).
 * Only {@code java.nio}/{@code java.util} types cross this class's
 * boundary, so it is unit-testable against a JUnit {@code @TempDir} with
 * no Minecraft/Fabric involved — resolving the real file's location
 * (server-identity-keyed, under Fabric Loader's config directory) is the
 * caller's job.
 */
public final class HarvesterServerOptInLoader {

    private static final String KEY_ADDRESS = "address";
    private static final String KEY_OPT_IN = "multiplayerOptIn";

    private HarvesterServerOptInLoader() {
    }

    /**
     * Loads {@code file}, creating it with {@code address=normalizedAddress}
     * and {@code multiplayerOptIn=false} first if it does not yet exist.
     * Never throws: any {@link IOException} is reported as a single
     * warning and {@code false} is returned instead.
     */
    public static LoadResult loadOrCreateDefault(Path file, String normalizedAddress) {
        try {
            if (Files.notExists(file)) {
                writeDefaultFile(file, normalizedAddress);
                return new LoadResult(false, List.of());
            }
            Properties properties = new Properties();
            try (InputStream in = Files.newInputStream(file)) {
                properties.load(in);
            }
            return parse(properties);
        } catch (IOException e) {
            return new LoadResult(
                    false,
                    List.of("Could not read or create " + file + " (" + e.getMessage() + "); using default (false).")
            );
        }
    }

    /** Parses an already-loaded {@link Properties}; used directly by tests. */
    public static LoadResult parse(Properties properties) {
        String raw = properties.getProperty(KEY_OPT_IN);
        if (raw == null) {
            return new LoadResult(false, List.of());
        }
        String trimmed = raw.trim();
        if (trimmed.equalsIgnoreCase("true")) {
            return new LoadResult(true, List.of());
        }
        if (trimmed.equalsIgnoreCase("false")) {
            return new LoadResult(false, List.of());
        }
        return new LoadResult(
                false,
                List.of("Invalid '" + KEY_OPT_IN + "' value '" + raw + "' (expected true/false); using default (false).")
        );
    }

    private static void writeDefaultFile(Path file, String normalizedAddress) throws IOException {
        Path parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        String contents = KEY_ADDRESS + "=" + normalizedAddress + "\n" + KEY_OPT_IN + "=false\n";
        Files.writeString(file, contents, StandardCharsets.UTF_8);
    }

    /** The resolved opt-in boolean plus at most one warning. */
    public static final class LoadResult {

        private final boolean optIn;
        private final List<String> warnings;

        public LoadResult(boolean optIn, List<String> warnings) {
            this.optIn = optIn;
            this.warnings = List.copyOf(Objects.requireNonNull(warnings, "warnings"));
        }

        public boolean optIn() {
            return optIn;
        }

        public List<String> warnings() {
            return warnings;
        }
    }
}
