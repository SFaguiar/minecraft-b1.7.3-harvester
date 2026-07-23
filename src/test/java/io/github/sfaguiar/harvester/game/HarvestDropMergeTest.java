package io.github.sfaguiar.harvester.game;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.IntPredicate;
import java.util.function.IntUnaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class HarvestDropMergeTest {

    private static final IntUnaryOperator MAX_64 = id -> 64;
    private static final IntPredicate ALWAYS_STACKABLE = id -> true;

    private static HarvestDropMerge.Unit unit(int id, int dmg, int count) {
        return new HarvestDropMerge.Unit(id, dmg, count);
    }

    @Test
    void compatibleStacks_areMerged() {
        List<HarvestDropMerge.Unit> out = HarvestDropMerge.merge(
                List.of(unit(5, 0, 10), unit(5, 0, 20)), MAX_64, ALWAYS_STACKABLE
        );
        assertEquals(1, out.size());
        assertEquals(5, out.get(0).itemId);
        assertEquals(0, out.get(0).damage);
        assertEquals(30, out.get(0).count);
    }

    @Test
    void differentItemId_isNotMerged() {
        List<HarvestDropMerge.Unit> out = HarvestDropMerge.merge(
                List.of(unit(5, 0, 10), unit(6, 0, 10)), MAX_64, ALWAYS_STACKABLE
        );
        assertEquals(2, out.size());
    }

    @Test
    void differentDamageMetadata_isNotMerged() {
        // e.g. two leaf/log species that share an item id but differ by metadata.
        List<HarvestDropMerge.Unit> out = HarvestDropMerge.merge(
                List.of(unit(5, 0, 10), unit(5, 1, 10)), MAX_64, ALWAYS_STACKABLE
        );
        assertEquals(2, out.size());
    }

    @Test
    void overMaxStack_isSplit() {
        List<HarvestDropMerge.Unit> out = HarvestDropMerge.merge(
                List.of(unit(5, 0, 100)), MAX_64, ALWAYS_STACKABLE
        );
        assertEquals(2, out.size());
        assertEquals(64, out.get(0).count);
        assertEquals(36, out.get(1).count);
    }

    @Test
    void mergeThenSplit_respectsMaxAcrossManyInputs() {
        List<HarvestDropMerge.Unit> out = HarvestDropMerge.merge(
                List.of(unit(5, 0, 40), unit(5, 0, 40), unit(5, 0, 40)), MAX_64, ALWAYS_STACKABLE
        );
        // 120 total -> 64 + 56
        assertEquals(2, out.size());
        assertEquals(64, out.get(0).count);
        assertEquals(56, out.get(1).count);
    }

    @Test
    void nonStackableItems_areNeverMerged() {
        List<HarvestDropMerge.Unit> out = HarvestDropMerge.merge(
                List.of(unit(300, 0, 1), unit(300, 0, 1)), MAX_64, id -> false
        );
        assertEquals(2, out.size());
    }

    @Test
    void zeroAndNegativeCounts_areSkipped() {
        List<HarvestDropMerge.Unit> out = HarvestDropMerge.merge(
                List.of(unit(5, 0, 0), unit(5, 0, -3), unit(5, 0, 7)), MAX_64, ALWAYS_STACKABLE
        );
        assertEquals(1, out.size());
        assertEquals(7, out.get(0).count);
    }

    @Test
    void emptyInput_yieldsEmptyOutput() {
        assertTrue(HarvestDropMerge.merge(List.of(), MAX_64, ALWAYS_STACKABLE).isEmpty());
    }
}
