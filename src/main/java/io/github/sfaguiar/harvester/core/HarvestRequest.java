package io.github.sfaguiar.harvester.core;

import java.util.Objects;

/**
 * Input to {@link ConnectedBlockFinder#discover(HarvestRequest, BlockGroupView, NeighborhoodPolicy)}.
 *
 * <p>{@code originBlockId} and {@code originAlreadyRemoved} are captured
 * data describing the origin block's state <em>before</em> it was broken,
 * not a live lookup — the caller must capture this before vanilla removes
 * the block, since by the time discovery runs the world may no longer
 * contain it at the origin position.
 *
 * <p>This class deliberately does not hold a group-membership rule (no
 * {@code BlockMatcher}/{@code BlockGroupView} field) — membership is a
 * separate concern, passed directly to {@code discover} as its own
 * parameter, so a request only ever describes <em>where</em> and <em>how
 * much</em>, never <em>what counts</em>.
 */
public final class HarvestRequest {

    private final BlockCoordinate origin;
    private final int originBlockId;
    private final boolean originAlreadyRemoved;
    private final int limit;

    public HarvestRequest(
            BlockCoordinate origin,
            int originBlockId,
            boolean originAlreadyRemoved,
            int limit
    ) {
        this.origin = Objects.requireNonNull(origin, "origin");
        this.originBlockId = originBlockId;
        this.originAlreadyRemoved = originAlreadyRemoved;
        if (limit < 1) {
            // Explicit local decision (not a legacy-derived behavior): a
            // limit below 1 cannot include even the origin block, which
            // every known legacy configuration path prevented anyway
            // (the legacy MLProp annotation on maxChain declares min = 1).
            // Rather than silently clamping or returning an empty plan,
            // this fails fast so a misconfigured caller finds out
            // immediately, per docs/ENGINEERING_STANDARDS.md's requirement
            // that configuration errors be visible, not silently absorbed.
            throw new IllegalArgumentException(
                    "limit must be >= 1 (origin always counts toward it), got " + limit
            );
        }
        this.limit = limit;
    }

    public BlockCoordinate origin() {
        return origin;
    }

    public int originBlockId() {
        return originBlockId;
    }

    public boolean originAlreadyRemoved() {
        return originAlreadyRemoved;
    }

    public int limit() {
        return limit;
    }
}
