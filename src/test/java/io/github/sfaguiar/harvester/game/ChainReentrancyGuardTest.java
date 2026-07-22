package io.github.sfaguiar.harvester.game;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ChainReentrancyGuardTest {

    @Test
    void freshGuard_isNotInside() {
        ChainReentrancyGuard guard = new ChainReentrancyGuard();

        assertFalse(guard.isInside());
    }

    @Test
    void tryEnter_firstCall_succeeds() {
        ChainReentrancyGuard guard = new ChainReentrancyGuard();

        assertTrue(guard.tryEnter());
        assertTrue(guard.isInside());
    }

    @Test
    void tryEnter_whileAlreadyInside_fails() {
        ChainReentrancyGuard guard = new ChainReentrancyGuard();
        guard.tryEnter();

        assertFalse(guard.tryEnter(), "a second entry while the first is still held must be rejected");
        assertTrue(guard.isInside(), "the rejected second attempt must not disturb the held state");
    }

    @Test
    void exit_releasesTheGuard_allowingReentry() {
        ChainReentrancyGuard guard = new ChainReentrancyGuard();
        guard.tryEnter();

        guard.exit();

        assertFalse(guard.isInside());
        assertTrue(guard.tryEnter(), "after exit, a fresh entry must succeed");
    }

    @Test
    void exit_whenNotHeld_isANoOp() {
        ChainReentrancyGuard guard = new ChainReentrancyGuard();

        guard.exit();

        assertFalse(guard.isInside());
        assertTrue(guard.tryEnter());
    }

    @Test
    void independentInstances_neverInterfere() {
        ChainReentrancyGuard guardA = new ChainReentrancyGuard();
        ChainReentrancyGuard guardB = new ChainReentrancyGuard();

        guardA.tryEnter();

        assertTrue(guardA.isInside());
        assertFalse(guardB.isInside(), "an independent guard instance must be unaffected by another instance's state");
        assertTrue(guardB.tryEnter(), "an independent guard instance must accept entry regardless of another instance's state");
    }
}
