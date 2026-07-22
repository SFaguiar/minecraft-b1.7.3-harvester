package io.github.sfaguiar.harvester.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

final class BlockCoordinateTest {

    @Test
    void equalsAndHashCode_areBasedOnCoordinatesOnly() {
        BlockCoordinate a = new BlockCoordinate(1, 2, 3);
        BlockCoordinate b = new BlockCoordinate(1, 2, 3);
        BlockCoordinate different = new BlockCoordinate(1, 2, 4);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, different);
    }

    @Test
    void orthogonalNeighbors_excludesDiagonalsAndSelf() {
        BlockCoordinate origin = new BlockCoordinate(0, 0, 0);

        List<BlockCoordinate> neighbors = origin.orthogonalNeighbors();

        assertEquals(6, neighbors.size());
        assertEquals(
                List.of(
                        new BlockCoordinate(-1, 0, 0),
                        new BlockCoordinate(1, 0, 0),
                        new BlockCoordinate(0, -1, 0),
                        new BlockCoordinate(0, 1, 0),
                        new BlockCoordinate(0, 0, -1),
                        new BlockCoordinate(0, 0, 1)
                ),
                neighbors
        );
        assertNotEquals(neighbors, List.of(new BlockCoordinate(1, 1, 1)));
    }
}
