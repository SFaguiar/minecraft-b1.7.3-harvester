package io.github.sfaguiar.harvester.core;

/**
 * The kind of harvestable group a {@link HarvestGroupResolver#resolve}
 * origin resolved to. Purely descriptive — used by callers to decide which
 * configuration toggle applies and whether a tool-harvestability gate is
 * relevant; the matching logic itself lives on {@link HarvestGroup}.
 */
public enum HarvestGroupKind {
    /** {@code c:logs}. No tool gate; empty hand is always eligible. */
    LOGS,
    /** One or more specific material ore tags ({@code c:ores/<material>}) captured from the origin. */
    ORE_SPECIFIC_TAGS,
    /** Origin is in the generic {@code c:ores} tag but has no usable specific tag; matched by registry identity instead. */
    ORE_IDENTITY_FALLBACK
}
