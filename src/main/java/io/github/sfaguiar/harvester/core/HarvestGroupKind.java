package io.github.sfaguiar.harvester.core;

/**
 * The kind of harvestable group a {@link HarvestGroupResolver#resolve}
 * origin resolved to. Purely descriptive — used by callers to decide which
 * configuration toggle applies and which tool-suitability/tool-category gate
 * is relevant; the matching logic itself lives on {@link HarvestGroup}.
 */
public enum HarvestGroupKind {
    /** {@code c:logs}. Axe-category gate; empty hand never starts the chain. */
    LOGS,
    /** One or more specific material ore tags ({@code c:ores/<material>}) captured from the origin. */
    ORE_SPECIFIC_TAGS,
    /** Origin is in the generic {@code c:ores} tag but has no usable specific tag; matched by registry identity instead. */
    ORE_IDENTITY_FALLBACK,
    /** Vanilla dirt cluster; shovel-category gate, underground-only. */
    DIRT,
    /** Vanilla gravel cluster; shovel-category gate, underground-only, gravity-safe order. */
    GRAVEL,
    /** Leaf cluster of a single species; shears-category gate. */
    LEAVES,
    /** Fully-mature crop cluster ({@code metadata == 7}); hoe-category gate. */
    CROPS
}
