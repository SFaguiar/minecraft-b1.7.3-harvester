package io.github.sfaguiar.harvester.core;

import java.util.List;

/**
 * Four-horizontal-neighbor connectivity at the same Y level (-x, +x, -z, +z),
 * with no vertical or diagonal adjacency. Used for crop chains, which only
 * ever spread across a single flat farmland layer — so a chain started on one
 * field never climbs to a crop stacked directly above or reaches diagonally
 * into an adjacent, separately-planted plot. Order is fixed and deterministic,
 * mirroring the horizontal subset of {@link BlockCoordinate#orthogonalNeighbors()}.
 */
public final class HorizontalFourNeighborhood implements NeighborhoodPolicy {

    @Override
    public Iterable<BlockCoordinate> neighborsOf(BlockCoordinate coordinate) {
        int x = coordinate.x();
        int y = coordinate.y();
        int z = coordinate.z();
        return List.of(
                new BlockCoordinate(x - 1, y, z),
                new BlockCoordinate(x + 1, y, z),
                new BlockCoordinate(x, y, z - 1),
                new BlockCoordinate(x, y, z + 1)
        );
    }
}
