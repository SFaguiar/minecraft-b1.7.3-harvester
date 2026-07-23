package io.github.sfaguiar.harvester.core;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The crop connectivity: four horizontal neighbors at the same Y, in fixed order. */
final class HorizontalFourNeighborhoodTest {

    private static List<BlockCoordinate> neighborsOf(int x, int y, int z) {
        List<BlockCoordinate> list = new ArrayList<>();
        new HorizontalFourNeighborhood().neighborsOf(new BlockCoordinate(x, y, z)).forEach(list::add);
        return list;
    }

    @Test
    void returnsExactlyTheFourSameLevelHorizontalNeighbors() {
        assertEquals(
                List.of(
                        new BlockCoordinate(4, 10, 5),
                        new BlockCoordinate(6, 10, 5),
                        new BlockCoordinate(5, 10, 4),
                        new BlockCoordinate(5, 10, 6)
                ),
                neighborsOf(5, 10, 5)
        );
    }

    @Test
    void neverIncludesAVerticalOrDiagonalNeighbor() {
        List<BlockCoordinate> neighbors = neighborsOf(0, 0, 0);
        assertEquals(4, neighbors.size());
        assertTrue(neighbors.stream().allMatch(c -> c.y() == 0), "crops never chain up or down a level");
    }
}
