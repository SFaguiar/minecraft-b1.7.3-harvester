package io.github.sfaguiar.harvester.core;

/**
 * Six-orthogonal-neighbor connectivity (no diagonals) — face adjacency
 * only.
 *
 * <p>This is the transitional default used by
 * {@link io.github.sfaguiar.harvester.client.SingleplayerHarvestDiscoveryAdapter}
 * today, preserving the exact behavior already covered by
 * {@code ConnectedBlockFinderTest} and exercised in real runtime by
 * {@code EXP-0003}/{@code CLM-0018}. Its continued use is <strong>not</strong>
 * a final product decision — see
 * {@code better-beta-program/docs/operations/OPEN_QUESTIONS.md} Q0011, which
 * remains open pending the repository owner choosing between this and
 * {@link LegacyTwentySixNeighborhood} (or some other policy) for the 2.x
 * port.
 *
 * <p>Delegates to {@link BlockCoordinate#orthogonalNeighbors()}, which
 * already fixes the exact order (-x, +x, -y, +y, -z, +z).
 */
public final class OrthogonalSixNeighborhood implements NeighborhoodPolicy {

    @Override
    public Iterable<BlockCoordinate> neighborsOf(BlockCoordinate coordinate) {
        return coordinate.orthogonalNeighbors();
    }
}
