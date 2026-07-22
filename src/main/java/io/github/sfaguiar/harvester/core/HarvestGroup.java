package io.github.sfaguiar.harvester.core;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable, resolved harvestable group for one origin — the output of
 * {@link HarvestGroupResolver#resolve}. {@link #matches(BlockDescriptor)}
 * is the pure membership test a {@code client}-package adapter composes
 * with a real block query into a {@link BlockGroupView} for
 * {@link ConnectedBlockFinder}.
 *
 * <p>Log priority is enforced here, not just at resolution: a candidate
 * that is itself a log never matches an ore group, even if it happens to
 * carry a matching ore tag (a datapack/mod misclassification) — the same
 * priority rule {@link HarvestGroupResolver} applies to the origin applies
 * symmetrically to every candidate, so an ore chain can never silently eat
 * a log block.
 */
public final class HarvestGroup {

    private final HarvestGroupKind kind;
    private final Set<String> specificTags;
    private final String identity;

    private HarvestGroup(HarvestGroupKind kind, Set<String> specificTags, String identity) {
        this.kind = kind;
        this.specificTags = specificTags;
        this.identity = identity;
    }

    public static HarvestGroup logs() {
        return new HarvestGroup(HarvestGroupKind.LOGS, Set.of(), null);
    }

    /**
     * @param tags the origin's exact captured set of specific ore tags;
     *             must not be empty (an empty set is not a valid specific-tag
     *             group — see {@link HarvestGroupResolver}, which falls back
     *             to identity in that case instead of calling this).
     */
    public static HarvestGroup oreSpecificTags(Set<String> tags) {
        if (tags.isEmpty()) {
            throw new IllegalArgumentException("tags must not be empty");
        }
        return new HarvestGroup(HarvestGroupKind.ORE_SPECIFIC_TAGS, Set.copyOf(tags), null);
    }

    public static HarvestGroup oreIdentityFallback(String identity) {
        return new HarvestGroup(
                HarvestGroupKind.ORE_IDENTITY_FALLBACK, Set.of(), Objects.requireNonNull(identity, "identity")
        );
    }

    public HarvestGroupKind kind() {
        return kind;
    }

    /** Non-empty only for {@link HarvestGroupKind#ORE_SPECIFIC_TAGS}. */
    public Set<String> specificTags() {
        return Collections.unmodifiableSet(specificTags);
    }

    /** Non-null only for {@link HarvestGroupKind#ORE_IDENTITY_FALLBACK}. */
    public String identity() {
        return identity;
    }

    /**
     * Whether {@code candidate} belongs to this resolved group. Deterministic
     * and total: every {@link BlockDescriptor} either matches or does not,
     * regardless of how many specific tags this group or the candidate carry
     * — matching is exact set intersection, never a prefix or partial
     * comparison.
     */
    public boolean matches(BlockDescriptor candidate) {
        switch (kind) {
            case LOGS:
                return candidate.isLog();
            case ORE_SPECIFIC_TAGS:
                if (candidate.isLog()) {
                    return false;
                }
                return !Collections.disjoint(candidate.specificOreTags(), specificTags);
            case ORE_IDENTITY_FALLBACK:
                if (candidate.isLog()) {
                    return false;
                }
                return identity.equals(candidate.registryIdentity());
            default:
                throw new IllegalStateException("unreachable: " + kind);
        }
    }

    @Override
    public String toString() {
        return "HarvestGroup{kind=" + kind + ", specificTags=" + specificTags + ", identity=" + identity + "}";
    }
}
