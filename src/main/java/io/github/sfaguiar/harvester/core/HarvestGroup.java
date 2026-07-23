package io.github.sfaguiar.harvester.core;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable, resolved harvestable group for one origin — the output of
 * {@link HarvestGroupResolver#resolve}. {@link #matches(BlockDescriptor)}
 * is the pure membership test a {@code game}-package adapter composes with a
 * real block query into a {@link BlockGroupView} for
 * {@link ConnectedBlockFinder}.
 *
 * <p>Log priority is enforced here, not just at resolution: a candidate
 * that is itself a log never matches an ore group, even if it happens to
 * carry a matching ore tag (a datapack/mod misclassification) — the same
 * priority rule {@link HarvestGroupResolver} applies to the origin applies
 * symmetrically to every candidate, so an ore chain can never silently eat
 * a log block.
 *
 * <p>The underground/leaf/crop categories added for 1.0.0 match by the
 * pure category flags on {@link BlockDescriptor}: {@link HarvestGroupKind#DIRT}
 * and {@link HarvestGroupKind#GRAVEL} by exact category; {@link
 * HarvestGroupKind#LEAVES} additionally by leaf species ({@code metadata &
 * 0x3}, so different species never chain together); {@link
 * HarvestGroupKind#CROPS} additionally by full maturity ({@code metadata ==
 * MATURE_CROP_METADATA}, so an immature plant is never a member). World-context
 * gates that {@code core} cannot see — the underground rule for dirt/gravel —
 * are layered on top by the {@code game}-package adapter's own
 * {@link BlockGroupView}, never here.
 */
public final class HarvestGroup {

    /** The wheat metadata value denoting a fully-grown crop in Beta 1.7.3 (growth stages 0..7). */
    public static final int MATURE_CROP_METADATA = 7;

    /** Low-bit mask isolating leaf species from decay/placement flag bits (0x4/0x8). */
    public static final int LEAF_SPECIES_MASK = 0x3;

    private final HarvestGroupKind kind;
    private final Set<String> specificTags;
    private final String identity;
    private final int leafSpecies;

    private HarvestGroup(HarvestGroupKind kind, Set<String> specificTags, String identity, int leafSpecies) {
        this.kind = kind;
        this.specificTags = specificTags;
        this.identity = identity;
        this.leafSpecies = leafSpecies;
    }

    public static HarvestGroup logs() {
        return new HarvestGroup(HarvestGroupKind.LOGS, Set.of(), null, -1);
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
        return new HarvestGroup(HarvestGroupKind.ORE_SPECIFIC_TAGS, Set.copyOf(tags), null, -1);
    }

    public static HarvestGroup oreIdentityFallback(String identity) {
        return new HarvestGroup(
                HarvestGroupKind.ORE_IDENTITY_FALLBACK, Set.of(), Objects.requireNonNull(identity, "identity"), -1
        );
    }

    public static HarvestGroup dirt() {
        return new HarvestGroup(HarvestGroupKind.DIRT, Set.of(), null, -1);
    }

    public static HarvestGroup gravel() {
        return new HarvestGroup(HarvestGroupKind.GRAVEL, Set.of(), null, -1);
    }

    /** @param species the origin's leaf species ({@code metadata & LEAF_SPECIES_MASK}). */
    public static HarvestGroup leaves(int species) {
        return new HarvestGroup(HarvestGroupKind.LEAVES, Set.of(), null, species & LEAF_SPECIES_MASK);
    }

    public static HarvestGroup crops() {
        return new HarvestGroup(HarvestGroupKind.CROPS, Set.of(), null, -1);
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

    /** The origin's leaf species; meaningful only for {@link HarvestGroupKind#LEAVES}, else {@code -1}. */
    public int leafSpecies() {
        return leafSpecies;
    }

    /**
     * Whether {@code candidate} belongs to this resolved group. Deterministic
     * and total: every {@link BlockDescriptor} either matches or does not.
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
            case DIRT:
                return candidate.isDirt();
            case GRAVEL:
                return candidate.isGravel();
            case LEAVES:
                return candidate.isLeaves() && (candidate.metadata() & LEAF_SPECIES_MASK) == leafSpecies;
            case CROPS:
                return candidate.isCrop() && candidate.metadata() == MATURE_CROP_METADATA;
            default:
                throw new IllegalStateException("unreachable: " + kind);
        }
    }

    @Override
    public String toString() {
        return "HarvestGroup{kind=" + kind + ", specificTags=" + specificTags
                + ", identity=" + identity + ", leafSpecies=" + leafSpecies + "}";
    }
}
