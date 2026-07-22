package io.github.sfaguiar.harvester.core;

/**
 * Decides whether a block ID belongs to a harvestable group.
 *
 * <p>Matches by block ID only, never by metadata. This mirrors a
 * confirmed legacy Harvester 1.x behavioral fact (see
 * {@code better-beta-program/docs/knowledge/claims/CLM-0009.md}): the
 * legacy chain-membership decision compares only the block ID, so
 * different metadata variants of the same ID chain together. This
 * interface enforces that same shape (an {@code int} block ID, no
 * metadata parameter) so a caller cannot accidentally filter by metadata
 * here; whether the 2.x port should diverge from this is tracked as an
 * open design decision, not decided by this interface
 * ({@code better-beta-program/docs/operations/OPEN_QUESTIONS.md} Q0003).
 *
 * <p>Combine with a {@link BlockWorldView} via
 * {@link BlockGroupView#byId(BlockWorldView, BlockMatcher)} to build the
 * {@link BlockGroupView} that {@link ConnectedBlockFinder} actually
 * consumes. Tag-based membership (e.g. StationAPI's {@code BlockTags.LOGS})
 * does not use this interface at all — see {@link BlockGroupView}'s
 * javadoc.
 */
@FunctionalInterface
public interface BlockMatcher {

    boolean matches(int blockId);

    static BlockMatcher ofId(int blockId) {
        return candidate -> candidate == blockId;
    }
}
