package io.github.sfaguiar.harvester.config;

import io.github.sfaguiar.harvester.core.HarvestGroupKind;
import io.github.sfaguiar.harvester.core.NeighborhoodPolicy;

import java.util.Objects;
import java.util.Set;

/**
 * Immutable, fully resolved Harvester configuration — the single source of
 * truth the {@code client}/{@code server}/{@code game} packages consume at
 * runtime, and (in singleplayer) the single authoritative store the optional
 * in-game screen edits through {@code harvester.properties}.
 *
 * <p>Carries no Minecraft, StationAPI, or Fabric import, so it can be
 * constructed and asserted on in a plain unit test. {@link #DEFAULTS} is the
 * exact behavior preserved when no configuration file exists or a property
 * is missing/invalid — see {@link HarvesterConfigLoader}.
 *
 * <p>The original seven-field constructor is preserved so pre-existing call
 * sites and tests keep compiling; the new 1.0.0 fields (drop consolidation,
 * the underground/leaf/crop category toggles and rules, allow/deny lists,
 * and per-category tool allowlists) are set through {@link Builder}.
 */
public final class HarvesterConfig {

    public static final boolean DEFAULT_ENABLED = true;
    public static final int DEFAULT_MAX_CHAIN = 64;
    public static final int MAX_ALLOWED_MAX_CHAIN = 100;
    public static final NeighborhoodChoice DEFAULT_NEIGHBORHOOD = NeighborhoodChoice.LEGACY_26;
    public static final boolean DEFAULT_DIAGNOSTIC_LOGGING = false;
    public static final boolean DEFAULT_HARVEST_LOGS = true;
    public static final boolean DEFAULT_HARVEST_ORES = true;
    public static final boolean DEFAULT_MULTIPLAYER_ALLOWED = false;

    /** Drop consolidation is on by default (owner Decision G). */
    public static final boolean DEFAULT_CONSOLIDATE_DROPS = true;

    /** The new underground/leaf/crop categories are opt-in (owner Decision G). */
    public static final boolean DEFAULT_HARVEST_DIRT = false;
    public static final boolean DEFAULT_HARVEST_GRAVEL = false;
    public static final boolean DEFAULT_HARVEST_LEAVES = false;
    public static final boolean DEFAULT_HARVEST_CROPS = false;

    /** Underground rule (owner Decision D): Overworld, no sky access, Y &le; 63. */
    public static final boolean DEFAULT_UNDERGROUND_REQUIRES_NO_SKY = true;
    public static final int DEFAULT_UNDERGROUND_MAX_Y = 63;
    public static final boolean DEFAULT_UNDERGROUND_OVERWORLD_ONLY = true;

    /** Inclusive bounds accepted for {@code undergroundMaxY} (Beta 1.7.3 world height is 0..127). */
    public static final int MIN_UNDERGROUND_MAX_Y = 0;
    public static final int MAX_UNDERGROUND_MAX_Y = 255;

    public static final HarvesterConfig DEFAULTS = new Builder().build();

    private final boolean enabled;
    private final int maxChain;
    private final NeighborhoodChoice neighborhood;
    private final boolean diagnosticLogging;
    private final boolean harvestLogs;
    private final boolean harvestOres;
    private final boolean multiplayerAllowed;
    private final boolean consolidateDrops;
    private final boolean harvestDirt;
    private final boolean harvestGravel;
    private final boolean harvestLeaves;
    private final boolean harvestCrops;
    private final boolean undergroundRequiresNoSky;
    private final int undergroundMaxY;
    private final boolean undergroundOverworldOnly;
    private final Set<String> allowlist;
    private final Set<String> denylist;
    private final Set<Integer> toolAxeIds;
    private final Set<Integer> toolShovelIds;
    private final Set<Integer> toolShearsIds;
    private final Set<Integer> toolHoeIds;

    /**
     * Preserved seven-argument constructor. New fields take their documented
     * defaults; use {@link Builder} to set them.
     */
    public HarvesterConfig(
            boolean enabled,
            int maxChain,
            NeighborhoodChoice neighborhood,
            boolean diagnosticLogging,
            boolean harvestLogs,
            boolean harvestOres,
            boolean multiplayerAllowed
    ) {
        this(new Builder()
                .enabled(enabled)
                .maxChain(maxChain)
                .neighborhood(neighborhood)
                .diagnosticLogging(diagnosticLogging)
                .harvestLogs(harvestLogs)
                .harvestOres(harvestOres)
                .multiplayerAllowed(multiplayerAllowed));
    }

    private HarvesterConfig(Builder b) {
        this.enabled = b.enabled;
        if (b.maxChain < 1) {
            throw new IllegalArgumentException("maxChain must be >= 1, got " + b.maxChain);
        }
        if (b.maxChain > MAX_ALLOWED_MAX_CHAIN) {
            throw new IllegalArgumentException(
                    "maxChain must be <= " + MAX_ALLOWED_MAX_CHAIN + ", got " + b.maxChain
            );
        }
        this.maxChain = b.maxChain;
        this.neighborhood = Objects.requireNonNull(b.neighborhood, "neighborhood");
        this.diagnosticLogging = b.diagnosticLogging;
        this.harvestLogs = b.harvestLogs;
        this.harvestOres = b.harvestOres;
        this.multiplayerAllowed = b.multiplayerAllowed;
        this.consolidateDrops = b.consolidateDrops;
        this.harvestDirt = b.harvestDirt;
        this.harvestGravel = b.harvestGravel;
        this.harvestLeaves = b.harvestLeaves;
        this.harvestCrops = b.harvestCrops;
        this.undergroundRequiresNoSky = b.undergroundRequiresNoSky;
        this.undergroundMaxY = b.undergroundMaxY;
        this.undergroundOverworldOnly = b.undergroundOverworldOnly;
        this.allowlist = Set.copyOf(b.allowlist);
        this.denylist = Set.copyOf(b.denylist);
        this.toolAxeIds = Set.copyOf(b.toolAxeIds);
        this.toolShovelIds = Set.copyOf(b.toolShovelIds);
        this.toolShearsIds = Set.copyOf(b.toolShearsIds);
        this.toolHoeIds = Set.copyOf(b.toolHoeIds);
    }

    /** A {@link Builder} pre-seeded with this config's values, for copy-with-change edits. */
    public Builder toBuilder() {
        return new Builder()
                .enabled(enabled).maxChain(maxChain).neighborhood(neighborhood)
                .diagnosticLogging(diagnosticLogging).harvestLogs(harvestLogs).harvestOres(harvestOres)
                .multiplayerAllowed(multiplayerAllowed).consolidateDrops(consolidateDrops)
                .harvestDirt(harvestDirt).harvestGravel(harvestGravel).harvestLeaves(harvestLeaves)
                .harvestCrops(harvestCrops).undergroundRequiresNoSky(undergroundRequiresNoSky)
                .undergroundMaxY(undergroundMaxY).undergroundOverworldOnly(undergroundOverworldOnly)
                .allowlist(allowlist).denylist(denylist)
                .toolAxeIds(toolAxeIds).toolShovelIds(toolShovelIds)
                .toolShearsIds(toolShearsIds).toolHoeIds(toolHoeIds);
    }

    public boolean enabled() {
        return enabled;
    }

    public int maxChain() {
        return maxChain;
    }

    public NeighborhoodChoice neighborhood() {
        return neighborhood;
    }

    public NeighborhoodPolicy neighborhoodPolicy() {
        return neighborhood.toPolicy();
    }

    public boolean diagnosticLogging() {
        return diagnosticLogging;
    }

    public boolean harvestLogs() {
        return harvestLogs;
    }

    public boolean harvestOres() {
        return harvestOres;
    }

    public boolean multiplayerAllowed() {
        return multiplayerAllowed;
    }

    public boolean consolidateDrops() {
        return consolidateDrops;
    }

    public boolean harvestDirt() {
        return harvestDirt;
    }

    public boolean harvestGravel() {
        return harvestGravel;
    }

    public boolean harvestLeaves() {
        return harvestLeaves;
    }

    public boolean harvestCrops() {
        return harvestCrops;
    }

    public boolean undergroundRequiresNoSky() {
        return undergroundRequiresNoSky;
    }

    public int undergroundMaxY() {
        return undergroundMaxY;
    }

    public boolean undergroundOverworldOnly() {
        return undergroundOverworldOnly;
    }

    public Set<String> allowlist() {
        return allowlist;
    }

    public Set<String> denylist() {
        return denylist;
    }

    public Set<Integer> toolAxeIds() {
        return toolAxeIds;
    }

    public Set<Integer> toolShovelIds() {
        return toolShovelIds;
    }

    public Set<Integer> toolShearsIds() {
        return toolShearsIds;
    }

    public Set<Integer> toolHoeIds() {
        return toolHoeIds;
    }

    /**
     * Whether the automatic chain for {@code kind} is enabled by the
     * per-category toggle alone — the single place mapping a resolved
     * {@link HarvestGroupKind} to its toggle. The allow/deny precedence is
     * layered on top by {@link #isBlockChainable(String, HarvestGroupKind)}.
     */
    public boolean isHarvestEnabledFor(HarvestGroupKind kind) {
        switch (kind) {
            case LOGS:
                return harvestLogs;
            case ORE_SPECIFIC_TAGS:
            case ORE_IDENTITY_FALLBACK:
                return harvestOres;
            case DIRT:
                return harvestDirt;
            case GRAVEL:
                return harvestGravel;
            case LEAVES:
                return harvestLeaves;
            case CROPS:
                return harvestCrops;
            default:
                throw new IllegalStateException("unreachable: " + kind);
        }
    }

    /**
     * Applies the full enable/disable precedence for a block that has
     * already resolved to {@code kind}:
     * <pre>denylist &gt; allowlist &gt; category toggle &gt; default</pre>
     * The denylist always blocks (even a category-enabled block); the
     * allowlist releases a block whose category is resolvable even when that
     * category's toggle is off (it never invents a category — the block must
     * already resolve to {@code kind} for this to be consulted).
     */
    public boolean isBlockChainable(String registryIdentity, HarvestGroupKind kind) {
        if (denylist.contains(registryIdentity)) {
            return false;
        }
        if (allowlist.contains(registryIdentity)) {
            return true;
        }
        return isHarvestEnabledFor(kind);
    }

    @Override
    public String toString() {
        return "HarvesterConfig{enabled=" + enabled
                + ", maxChain=" + maxChain
                + ", neighborhood=" + neighborhood.propertyValue()
                + ", diagnosticLogging=" + diagnosticLogging
                + ", harvestLogs=" + harvestLogs
                + ", harvestOres=" + harvestOres
                + ", multiplayerAllowed=" + multiplayerAllowed
                + ", consolidateDrops=" + consolidateDrops
                + ", harvestDirt=" + harvestDirt
                + ", harvestGravel=" + harvestGravel
                + ", harvestLeaves=" + harvestLeaves
                + ", harvestCrops=" + harvestCrops
                + ", undergroundRequiresNoSky=" + undergroundRequiresNoSky
                + ", undergroundMaxY=" + undergroundMaxY
                + ", undergroundOverworldOnly=" + undergroundOverworldOnly
                + ", allowlist=" + allowlist
                + ", denylist=" + denylist
                + ", toolAxeIds=" + toolAxeIds
                + ", toolShovelIds=" + toolShovelIds
                + ", toolShearsIds=" + toolShearsIds
                + ", toolHoeIds=" + toolHoeIds
                + "}";
    }

    /** Mutable builder; every field starts at its documented default. */
    public static final class Builder {
        private boolean enabled = DEFAULT_ENABLED;
        private int maxChain = DEFAULT_MAX_CHAIN;
        private NeighborhoodChoice neighborhood = DEFAULT_NEIGHBORHOOD;
        private boolean diagnosticLogging = DEFAULT_DIAGNOSTIC_LOGGING;
        private boolean harvestLogs = DEFAULT_HARVEST_LOGS;
        private boolean harvestOres = DEFAULT_HARVEST_ORES;
        private boolean multiplayerAllowed = DEFAULT_MULTIPLAYER_ALLOWED;
        private boolean consolidateDrops = DEFAULT_CONSOLIDATE_DROPS;
        private boolean harvestDirt = DEFAULT_HARVEST_DIRT;
        private boolean harvestGravel = DEFAULT_HARVEST_GRAVEL;
        private boolean harvestLeaves = DEFAULT_HARVEST_LEAVES;
        private boolean harvestCrops = DEFAULT_HARVEST_CROPS;
        private boolean undergroundRequiresNoSky = DEFAULT_UNDERGROUND_REQUIRES_NO_SKY;
        private int undergroundMaxY = DEFAULT_UNDERGROUND_MAX_Y;
        private boolean undergroundOverworldOnly = DEFAULT_UNDERGROUND_OVERWORLD_ONLY;
        private Set<String> allowlist = Set.of();
        private Set<String> denylist = Set.of();
        private Set<Integer> toolAxeIds = Set.of();
        private Set<Integer> toolShovelIds = Set.of();
        private Set<Integer> toolShearsIds = Set.of();
        private Set<Integer> toolHoeIds = Set.of();

        public Builder enabled(boolean v) { this.enabled = v; return this; }
        public Builder maxChain(int v) { this.maxChain = v; return this; }
        public Builder neighborhood(NeighborhoodChoice v) { this.neighborhood = v; return this; }
        public Builder diagnosticLogging(boolean v) { this.diagnosticLogging = v; return this; }
        public Builder harvestLogs(boolean v) { this.harvestLogs = v; return this; }
        public Builder harvestOres(boolean v) { this.harvestOres = v; return this; }
        public Builder multiplayerAllowed(boolean v) { this.multiplayerAllowed = v; return this; }
        public Builder consolidateDrops(boolean v) { this.consolidateDrops = v; return this; }
        public Builder harvestDirt(boolean v) { this.harvestDirt = v; return this; }
        public Builder harvestGravel(boolean v) { this.harvestGravel = v; return this; }
        public Builder harvestLeaves(boolean v) { this.harvestLeaves = v; return this; }
        public Builder harvestCrops(boolean v) { this.harvestCrops = v; return this; }
        public Builder undergroundRequiresNoSky(boolean v) { this.undergroundRequiresNoSky = v; return this; }
        public Builder undergroundMaxY(int v) { this.undergroundMaxY = v; return this; }
        public Builder undergroundOverworldOnly(boolean v) { this.undergroundOverworldOnly = v; return this; }
        public Builder allowlist(Set<String> v) { this.allowlist = Set.copyOf(v); return this; }
        public Builder denylist(Set<String> v) { this.denylist = Set.copyOf(v); return this; }
        public Builder toolAxeIds(Set<Integer> v) { this.toolAxeIds = Set.copyOf(v); return this; }
        public Builder toolShovelIds(Set<Integer> v) { this.toolShovelIds = Set.copyOf(v); return this; }
        public Builder toolShearsIds(Set<Integer> v) { this.toolShearsIds = Set.copyOf(v); return this; }
        public Builder toolHoeIds(Set<Integer> v) { this.toolHoeIds = Set.copyOf(v); return this; }

        public HarvesterConfig build() {
            return new HarvesterConfig(this);
        }
    }
}
