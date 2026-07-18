package io.github.sfaguiar.harvester.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Full 26-neighbor 3D adjacency (face, edge, and vertex connections),
 * matching the legacy Harvester 1.x connectivity fact confirmed in
 * {@code better-beta-program/docs/knowledge/claims/CLM-0009.md}-adjacent
 * research: the legacy {@code chainBreak} method iterated {@code dx, dy, dz}
 * each from -1 to 1 (skipping the {@code 0,0,0} origin offset), so any two
 * coordinates differing by at most 1 on every axis were treated as
 * connected.
 *
 * <p>This class exists only as an available policy for
 * {@link ConnectedBlockFinder} — it is <strong>not</strong> currently wired
 * into {@link io.github.sfaguiar.harvester.client.SingleplayerHarvestDiscoveryAdapter}
 * (which still defaults to {@link OrthogonalSixNeighborhood}), and its
 * adoption is an open decision, not a default this class asserts — see
 * {@code better-beta-program/docs/operations/OPEN_QUESTIONS.md} Q0011.
 *
 * <p>Neighbor order is fixed as {@code dx} outermost, {@code dy} middle,
 * {@code dz} innermost, each ascending from -1 to 1, mirroring the legacy
 * loop nesting order. Whether bit-for-bit visitation-order parity with the
 * legacy implementation is ever required (as opposed to merely
 * determinism) remains a separate open question — see
 * {@code better-beta-program/docs/operations/OPEN_QUESTIONS.md} Q0004; this
 * class only guarantees its own order is fixed and deterministic.
 */
public final class LegacyTwentySixNeighborhood implements NeighborhoodPolicy {

    @Override
    public Iterable<BlockCoordinate> neighborsOf(BlockCoordinate coordinate) {
        List<BlockCoordinate> neighbors = new ArrayList<>(26);
        int x = coordinate.x();
        int y = coordinate.y();
        int z = coordinate.z();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    neighbors.add(new BlockCoordinate(x + dx, y + dy, z + dz));
                }
            }
        }
        return neighbors;
    }
}
