package io.github.sfaguiar.harvester.core;

import java.util.Optional;

/**
 * Resolves an origin's {@link BlockDescriptor} into an immutable
 * {@link HarvestGroup}, or {@link Optional#empty()} when the origin is
 * ineligible for any automatic chain (not a log, not an ore).
 *
 * <p>Priority order, applied in this exact sequence:
 * <ol>
 *   <li>{@link HarvestGroupKind#LOGS} — a block simultaneously tagged as a
 *   log and an ore (a datapack/mod misclassification) is always treated as
 *   a log. Logs never require a specific tag beyond {@code c:logs}.</li>
 *   <li>{@link HarvestGroupKind#ORE_SPECIFIC_TAGS} — the origin's full
 *   captured set of specific material ore tags, whenever non-empty.</li>
 *   <li>{@link HarvestGroupKind#ORE_IDENTITY_FALLBACK} — only when the
 *   origin is in the generic ore aggregator tag but has no usable specific
 *   tag.</li>
 *   <li>Ineligible — anything else (common blocks never receive the
 *   identity fallback).</li>
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
        return Optional.empty();
    }
}
