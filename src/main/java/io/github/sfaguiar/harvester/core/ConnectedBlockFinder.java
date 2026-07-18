package io.github.sfaguiar.harvester.core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Iterative (never recursive) breadth-first search over whatever
 * connectivity {@link NeighborhoodPolicy} defines, bounded by a rigid limit
 * that includes the origin block.
 *
 * <p>Connectivity is an explicit dependency, not a hard-coded assumption:
 * this class calls {@code neighborhoodPolicy.neighborsOf(current)} instead
 * of assuming six-orthogonal or 26-neighbor adjacency itself. See
 * {@link NeighborhoodPolicy}, {@link OrthogonalSixNeighborhood}, and
 * {@link LegacyTwentySixNeighborhood}. Which policy is the right default
 * for the 2.x port is an open decision (
 * {@code better-beta-program/docs/operations/OPEN_QUESTIONS.md} Q0011),
 * not one this class makes on its own — callers choose.
 *
 * <p>Counting the origin toward the limit mirrors a confirmed legacy fact
 * (see {@code better-beta-program/docs/knowledge/claims/CLM-0008.md}:
 * legacy {@code broken} starts at {@code 1}), which the port intentionally
 * follows here per the recommended default in
 * {@code better-beta-program/docs/operations/OPEN_QUESTIONS.md} Q0002.
 *
 * <p>Visitation order per node is fixed by whichever
 * {@link NeighborhoodPolicy} is supplied; this class does not attempt to
 * reproduce the legacy visitation order bit-for-bit (Q0004 remains open on
 * whether that is ever required) — it only guarantees its own order is
 * deterministic for a fixed world state, request, and policy.
 */
public final class ConnectedBlockFinder {

    private ConnectedBlockFinder() {
    }

    /**
     * Never breaks, removes, or otherwise mutates any block. Never queries
     * {@code groupView} for the origin coordinate — the origin's inclusion
     * comes entirely from {@code request}, not from {@code groupView},
     * since the world may already show the origin as air or another block
     * by the time this runs.
     */
    public static HarvestPlan discover(
            HarvestRequest request,
            BlockGroupView groupView,
            NeighborhoodPolicy neighborhoodPolicy
    ) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(groupView, "groupView");
        Objects.requireNonNull(neighborhoodPolicy, "neighborhoodPolicy");

        BlockCoordinate origin = request.origin();
        int limit = request.limit();

        List<BlockCoordinate> included = new ArrayList<>();
        Set<BlockCoordinate> visited = new HashSet<>();
        Deque<BlockCoordinate> queue = new ArrayDeque<>();

        included.add(origin);
        visited.add(origin);
        queue.addLast(origin);

        boolean limitReached = false;

        while (!queue.isEmpty() && !limitReached) {
            BlockCoordinate current = queue.removeFirst();
            for (BlockCoordinate neighbor : neighborhoodPolicy.neighborsOf(current)) {
                if (!visited.add(neighbor)) {
                    continue;
                }
                if (!groupView.isMember(neighbor)) {
                    continue;
                }
                if (included.size() >= limit) {
                    limitReached = true;
                    break;
                }
                included.add(neighbor);
                queue.addLast(neighbor);
            }
        }

        return new HarvestPlan(origin, request.originAlreadyRemoved(), included, limit, limitReached);
    }
}
