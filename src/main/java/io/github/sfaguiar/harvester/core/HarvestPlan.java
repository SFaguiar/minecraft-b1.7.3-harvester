package io.github.sfaguiar.harvester.core;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Diagnostic-only result of {@link ConnectedBlockFinder#discover}. Nothing
 * in this class mutates a world or breaks a block — it is purely a plan
 * describing what discovery found.
 */
public final class HarvestPlan {

    private final BlockCoordinate origin;
    private final boolean originAlreadyRemoved;
    private final List<BlockCoordinate> includedBlocks;
    private final int limit;
    private final boolean limitReached;

    HarvestPlan(
            BlockCoordinate origin,
            boolean originAlreadyRemoved,
            List<BlockCoordinate> includedBlocks,
            int limit,
            boolean limitReached
    ) {
        this.origin = Objects.requireNonNull(origin, "origin");
        this.originAlreadyRemoved = originAlreadyRemoved;
        this.includedBlocks = Collections.unmodifiableList(includedBlocks);
        this.limit = limit;
        this.limitReached = limitReached;
    }

    public BlockCoordinate origin() {
        return origin;
    }

    /** Whether the origin block was already removed at request time. */
    public boolean originAlreadyRemoved() {
        return originAlreadyRemoved;
    }

    /** Origin first, then the rest in BFS discovery order. Never empty. */
    public List<BlockCoordinate> includedBlocks() {
        return includedBlocks;
    }

    public int limit() {
        return limit;
    }

    /** True when at least one further matching candidate existed beyond the limit. */
    public boolean limitReached() {
        return limitReached;
    }

    /** Total blocks in the plan, including the origin. */
    public int totalIncluded() {
        return includedBlocks.size();
    }

    /** Blocks in the plan beyond the origin. */
    public int additionalCandidateCount() {
        return includedBlocks.size() - 1;
    }
}
