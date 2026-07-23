package io.github.sfaguiar.harvester.core;

import java.util.Optional;

/**
 * Resolves an origin's {@link BlockDescriptor} into an immutable
 * {@link HarvestGroup}, or {@link Optional#empty()} when the origin is
 * ineligible for any automatic chain.
 *
 * <p>Priority order, applied in this exact sequence:
 * <ol>
 *   <li>{@link HarvestGroupKind#LOGS} — a block simultaneously tagged as a
 *   log and an ore is always treated as a log.</li>
 *   <li>{@link HarvestGroupKind#ORE_SPECIFIC_TAGS} — the origin's full
 *   captured set of specific material ore tags, whenever non-empty.</li>
 *   <li>{@link HarvestGroupKind#ORE_IDENTITY_FALLBACK} — only when the
 *   origin is in the generic ore aggregator tag but has no usable specific
 *   tag.</li>
 *   <li>{@link HarvestGroupKind#DIRT} / {@link HarvestGroupKind#GRAVEL} —
 *   the dedicated underground clusters (world-context gating is applied
 *   later by the adapter, never here).</li>
 *   <li>{@link HarvestGroupKind#LEAVES} — carrying the origin's own leaf
 *   species so different species never chain together.</li>
 *   <li>{@link HarvestGroupKind#CROPS} — only when the origin crop is
 *   itself fully mature; an immature origin starts no chain.</li>
 *   <li>Ineligible — anything else.</li>
 * </ol>
 */
public final class HarvestGroupResolver {

    private HarvestGroupResolver() {
    }

    public static Optional<HarvestGroup> resolve(BlockDescriptor origin) {
        if (origin.isLog()) {
            return Optional.of(HarvestGroup.logs());
        }
        if (!origin.specificOreTags().isEmpty()) {
            return Optional.of(HarvestGroup.oreSpecificTags(origin.specificOreTags()));
        }
        if (origin.isGenericOre()) {
            return Optional.of(HarvestGroup.oreIdentityFallback(origin.registryIdentity()));
        }
        if (origin.isDirt()) {
            return Optional.of(HarvestGroup.dirt());
        }
        if (origin.isGravel()) {
            return Optional.of(HarvestGroup.gravel());
        }
        if (origin.isLeaves()) {
            return Optional.of(HarvestGroup.leaves(origin.metadata()));
        }
        if (origin.isCrop() && origin.metadata() == HarvestGroup.MATURE_CROP_METADATA) {
            return Optional.of(HarvestGroup.crops());
        }
        return Optional.empty();
    }
}
