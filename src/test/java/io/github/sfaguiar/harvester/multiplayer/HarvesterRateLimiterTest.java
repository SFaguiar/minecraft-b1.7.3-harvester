package io.github.sfaguiar.harvester.multiplayer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class HarvesterRateLimiterTest {

    private static final long T0 = 1_000_000L;

    @Test
    void firstFourAcquisitions_withinWindow_areAccepted() {
        HarvesterRateLimiter limiter = new HarvesterRateLimiter();

        assertTrue(limiter.tryAcquire(T0));
        assertTrue(limiter.tryAcquire(T0 + 10));
        assertTrue(limiter.tryAcquire(T0 + 20));
        assertTrue(limiter.tryAcquire(T0 + 30));
    }

    @Test
    void fifthAcquisition_withinSameWindow_isRejected() {
        HarvesterRateLimiter limiter = new HarvesterRateLimiter();
        limiter.tryAcquire(T0);
        limiter.tryAcquire(T0 + 10);
        limiter.tryAcquire(T0 + 20);
        limiter.tryAcquire(T0 + 30);

        boolean fifth = limiter.tryAcquire(T0 + 40);

        assertFalse(fifth);
    }

    @Test
    void windowSlides_acceptsAgainOnceOldestAgesOut() {
        HarvesterRateLimiter limiter = new HarvesterRateLimiter();
        limiter.tryAcquire(T0);
        limiter.tryAcquire(T0 + 10);
        limiter.tryAcquire(T0 + 20);
        limiter.tryAcquire(T0 + 30);
        assertFalse(limiter.tryAcquire(T0 + 40));

        boolean afterWindow = limiter.tryAcquire(T0 + HarvesterRateLimiter.WINDOW_MILLIS);

        assertTrue(afterWindow);
    }

    @Test
    void rejectedAcquisition_doesNotConsumeASlot() {
        HarvesterRateLimiter limiter = new HarvesterRateLimiter();
        limiter.tryAcquire(T0);
        limiter.tryAcquire(T0 + 10);
        limiter.tryAcquire(T0 + 20);
        limiter.tryAcquire(T0 + 30);
        assertFalse(limiter.tryAcquire(T0 + 40));
        assertFalse(limiter.tryAcquire(T0 + 41));

        // The oldest slot (T0) is still the one that must age out — a rejection must not have advanced it.
        assertFalse(limiter.tryAcquire(T0 + HarvesterRateLimiter.WINDOW_MILLIS - 1));
        assertTrue(limiter.tryAcquire(T0 + HarvesterRateLimiter.WINDOW_MILLIS));
    }

    @Test
    void reset_clearsFullWindow_fourFreshAcquisitionsAcceptedAtTheSameInstant() {
        HarvesterRateLimiter limiter = new HarvesterRateLimiter();
        limiter.tryAcquire(T0);
        limiter.tryAcquire(T0 + 1);
        limiter.tryAcquire(T0 + 2);
        limiter.tryAcquire(T0 + 3);
        assertFalse(limiter.tryAcquire(T0 + 4));

        limiter.reset();

        // Same instant as the rejected acquisition above — reset alone,
        // with no time passing, must still allow a fresh run of 4.
        assertTrue(limiter.tryAcquire(T0 + 4));
        assertTrue(limiter.tryAcquire(T0 + 4));
        assertTrue(limiter.tryAcquire(T0 + 4));
        assertTrue(limiter.tryAcquire(T0 + 4));
        assertFalse(limiter.tryAcquire(T0 + 4));
    }

    @Test
    void reset_onFreshLimiter_isANoOp() {
        HarvesterRateLimiter limiter = new HarvesterRateLimiter();

        limiter.reset();

        assertTrue(limiter.tryAcquire(T0));
    }
}
