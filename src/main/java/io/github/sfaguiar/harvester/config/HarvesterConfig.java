package io.github.sfaguiar.harvester.config;

import io.github.sfaguiar.harvester.core.NeighborhoodPolicy;

import java.util.Objects;

/**
 * Immutable, fully resolved Harvester configuration — the single source of
 * truth the {@code client} package consumes at runtime.
 *
 * <p>Carries no Minecraft, StationAPI, or Fabric import, so it can be
 * constructed and asserted on in a plain unit test. {@link #DEFAULTS} is the
 * exact behavior preserved when no configuration file exists or a property
 * is missing/invalid — see {@link HarvesterConfigLoader}.
 */
public final class HarvesterConfig {

    /** {@code enabled} default: the automatic chain runs unless disabled. */
    public static final boolean DEFAULT_ENABLED = true;

    /**
     * {@code maxChain} default — the exact limit the diagnostic slice
     * already used before this configuration existed (previously
     * {@code SingleplayerHarvestDiscoveryAdapter}'s hard-coded
     * {@code DIAGNOSTIC_LIMIT}), preserving current behavior for a missing
     * or absent config file.
     */
    public static final int DEFAULT_MAX_CHAIN = 64;

    /**
     * Upper bound for {@code maxChain}, centralized here as the single
     * source of truth for both {@link #HarvesterConfig(boolean, int,
     * NeighborhoodChoice, boolean) the constructor} and
     * {@link HarvesterConfigLoader#parse(java.util.Properties)}.
     *
     * <p>100 is not an arbitrary round number: it is the largest value the
     * legacy Harvester's own UI ever exposed (see
     * {@code docs/PARITY_SPEC.md}, "Chain behavior" — legacy limits were 8,
     * 16, 32, 64, 100), so no real user-facing configuration has ever
     * exceeded it. It also keeps a single activation's BFS discovery and
     * synchronous {@code breakBlock} loop — both run on the client thread
     * within one game tick — bounded to a size already exercised safely in
     * real runtime (up to 42 additional blocks in one activation).
     */
    public static final int MAX_ALLOWED_MAX_CHAIN = 100;

    /** {@code neighborhood} default: the confirmed production default (Q0011). */
    public static final NeighborhoodChoice DEFAULT_NEIGHBORHOOD = NeighborhoodChoice.LEGACY_26;

    /** {@code diagnosticLogging} default: verbose per-candidate/durability logs off. */
    public static final boolean DEFAULT_DIAGNOSTIC_LOGGING = false;

    public static final HarvesterConfig DEFAULTS = new HarvesterConfig(
            DEFAULT_ENABLED, DEFAULT_MAX_CHAIN, DEFAULT_NEIGHBORHOOD, DEFAULT_DIAGNOSTIC_LOGGING
    );

    private final boolean enabled;
    private final int maxChain;
    private final NeighborhoodChoice neighborhood;
    private final boolean diagnosticLogging;

    public HarvesterConfig(
            boolean enabled,
            int maxChain,
            NeighborhoodChoice neighborhood,
            boolean diagnosticLogging
    ) {
        this.enabled = enabled;
        if (maxChain < 1) {
            // Mirrors core.HarvestRequest's own contract (limit must be
            // >= 1, the origin always counts toward it). HarvesterConfig is
            // constructed only from already-validated values (defaults or
            // HarvesterConfigLoader's parsed/clamped-to-default result), so
            // this can only be reached by a programming error, not a bad
            // config file.
            throw new IllegalArgumentException("maxChain must be >= 1, got " + maxChain);
        }
        if (maxChain > MAX_ALLOWED_MAX_CHAIN) {
            // Same reasoning as the lower-bound check above: a bad config
            // file never reaches this constructor with an out-of-range
            // value (HarvesterConfigLoader#parseMaxChain rejects it first
            // and falls back to DEFAULT_MAX_CHAIN with a warning), so this
            // can only be reached by a programming error.
            throw new IllegalArgumentException(
                    "maxChain must be <= " + MAX_ALLOWED_MAX_CHAIN + ", got " + maxChain
            );
        }
        this.maxChain = maxChain;
        this.neighborhood = Objects.requireNonNull(neighborhood, "neighborhood");
        this.diagnosticLogging = diagnosticLogging;
    }

    /** Controls only the automatic additional-candidate chain; never the manual origin break. */
    public boolean enabled() {
        return enabled;
    }

    /** Total blocks per activation, including the origin block. */
    public int maxChain() {
        return maxChain;
    }

    public NeighborhoodChoice neighborhood() {
        return neighborhood;
    }

    /** A fresh {@code core} policy instance matching {@link #neighborhood()}. */
    public NeighborhoodPolicy neighborhoodPolicy() {
        return neighborhood.toPolicy();
    }

    /** When {@code false}, per-candidate and durability-snapshot logs must not be emitted. */
    public boolean diagnosticLogging() {
        return diagnosticLogging;
    }

    @Override
    public String toString() {
        return "HarvesterConfig{enabled=" + enabled
                + ", maxChain=" + maxChain
                + ", neighborhood=" + neighborhood.propertyValue()
                + ", diagnosticLogging=" + diagnosticLogging
                + "}";
    }
}
