package io.github.sfaguiar.harvester.game;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Lifecycle invariants of the drop-consolidation wrapper's pure core
 * ({@code runAround}): single open/flush/close, cleanup on normal return and
 * on exception, reentrant candidate reuse, per-owner isolation, and no
 * residual context left on the thread-local stack.
 */
final class HarvestDropConsolidationTest {

    @BeforeEach
    @AfterEach
    void drainStack() {
        while (!HarvestDropContexts.isEmpty()) {
            HarvestDropContexts.pop();
        }
    }

    private static HarvestDropContext ctx(Object owner, Object world, int x, int y, int z) {
        return new HarvestDropContext(owner, world, x, y, z);
    }

    @Test
    void normalReturn_opensFlushesOnce_closes_noResidual() {
        Object owner = new Object();
        Object world = new Object();
        HarvestDropContext context = ctx(owner, world, 1, 2, 3);
        List<HarvestDropContext> flushed = new ArrayList<>();

        boolean result = HarvestDropConsolidation.runAround(
                owner, world, 1, 2, 3, () -> context, flushed::add, () -> true
        );

        assertTrue(result);
        assertEquals(1, flushed.size());
        assertSame(context, flushed.get(0));
        assertTrue(context.isClosed());
        assertTrue(HarvestDropContexts.isEmpty());
    }

    @Test
    void exceptionInBreak_stillFlushesOnce_closes_noResidual() {
        Object owner = new Object();
        Object world = new Object();
        HarvestDropContext context = ctx(owner, world, 1, 2, 3);
        List<HarvestDropContext> flushed = new ArrayList<>();

        assertThrows(RuntimeException.class, () -> HarvestDropConsolidation.runAround(
                owner, world, 1, 2, 3, () -> context, flushed::add,
                () -> {
                    throw new RuntimeException("boom");
                }
        ));

        assertEquals(1, flushed.size());
        assertTrue(context.isClosed());
        assertTrue(HarvestDropContexts.isEmpty());
    }

    @Test
    void reentrantCandidateBreak_reusesContext_noSecondOpenOrFlush() {
        Object owner = new Object();
        Object world = new Object();
        HarvestDropContext outer = ctx(owner, world, 0, 0, 0);
        int[] openerCalls = {0};
        Supplier<HarvestDropContext> opener = () -> {
            openerCalls[0]++;
            return outer;
        };
        List<HarvestDropContext> flushed = new ArrayList<>();

        boolean result = HarvestDropConsolidation.runAround(
                owner, world, 0, 0, 0, opener, flushed::add,
                // The chain breaks a candidate: a reentrant call, same owner.
                () -> HarvestDropConsolidation.runAround(
                        owner, world, 1, 0, 0, opener, flushed::add, () -> true
                )
        );

        assertTrue(result);
        assertEquals(1, openerCalls[0], "reentrant call must not open a second context");
        assertEquals(1, flushed.size(), "flush happens once, in the outermost finally");
        assertSame(outer, flushed.get(0));
        assertTrue(HarvestDropContexts.isEmpty());
    }

    @Test
    void nullOpener_runsBreak_withoutConsolidation_noResidual() {
        List<HarvestDropContext> flushed = new ArrayList<>();
        boolean result = HarvestDropConsolidation.runAround(
                new Object(), new Object(), 1, 2, 3, () -> null, flushed::add, () -> true
        );
        assertTrue(result);
        assertTrue(flushed.isEmpty());
        assertTrue(HarvestDropContexts.isEmpty());
    }

    @Test
    void nestedDifferentOwners_areIsolated_eachFlushedOnce() {
        Object ownerA = new Object();
        Object ownerB = new Object();
        Object world = new Object();
        HarvestDropContext ctxA = ctx(ownerA, world, 0, 0, 0);
        HarvestDropContext ctxB = ctx(ownerB, world, 5, 5, 5);
        List<HarvestDropContext> flushed = new ArrayList<>();

        HarvestDropConsolidation.runAround(
                ownerA, world, 0, 0, 0, () -> ctxA, flushed::add,
                () -> HarvestDropConsolidation.runAround(
                        ownerB, world, 5, 5, 5, () -> ctxB, flushed::add, () -> true
                )
        );

        assertEquals(2, flushed.size());
        assertSame(ctxB, flushed.get(0), "inner (B) flushes first");
        assertSame(ctxA, flushed.get(1), "outer (A) flushes last");
        assertTrue(ctxA.isClosed());
        assertTrue(ctxB.isClosed());
        assertTrue(HarvestDropContexts.isEmpty());
    }

    @Test
    void breakReturnValue_isPreservedWhenRejected() {
        Object owner = new Object();
        Object world = new Object();
        List<HarvestDropContext> flushed = new ArrayList<>();
        boolean result = HarvestDropConsolidation.runAround(
                owner, world, 1, 2, 3, () -> ctx(owner, world, 1, 2, 3), flushed::add, () -> false
        );
        assertFalse(result);
        assertEquals(1, flushed.size());
        assertTrue(HarvestDropContexts.isEmpty());
    }
}
