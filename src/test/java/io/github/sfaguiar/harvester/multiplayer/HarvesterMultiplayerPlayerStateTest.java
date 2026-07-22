package io.github.sfaguiar.harvester.multiplayer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class HarvesterMultiplayerPlayerStateTest {

    private static final long T0 = 1_000_000L;

    @Test
    void initialState_activeFalse_supportNotAnnounced() {
        HarvesterMultiplayerPlayerState state = new HarvesterMultiplayerPlayerState();

        assertFalse(state.active());
        assertFalse(state.supportAnnounced());
    }

    @Test
    void markSupportAnnounced_setsFlag() {
        HarvesterMultiplayerPlayerState state = new HarvesterMultiplayerPlayerState();

        state.markSupportAnnounced();

        assertTrue(state.supportAnnounced());
    }

    @Test
    void applyTransition_genuineChange_isApplied() {
        HarvesterMultiplayerPlayerState state = new HarvesterMultiplayerPlayerState();

        HarvesterTransitionOutcome outcome = state.applyTransition(true, T0);

        assertEquals(HarvesterTransitionOutcome.APPLIED, outcome);
        assertTrue(state.active());
    }

    @Test
    void applyTransition_duplicateValue_isIdempotentAndDoesNotConsumeQuota() {
        HarvesterMultiplayerPlayerState state = new HarvesterMultiplayerPlayerState();
        state.applyTransition(true, T0);

        // Repeat the same value many times — must never be rate-limited, since duplicates consume no quota.
        for (int i = 0; i < 10; i++) {
            HarvesterTransitionOutcome outcome = state.applyTransition(true, T0 + i);
            assertEquals(HarvesterTransitionOutcome.DUPLICATE_IGNORED, outcome);
        }
        assertTrue(state.active());
    }

    @Test
    void applyTransition_fourRealTransitionsAccepted_fifthRejectedAndForcesFalse() {
        HarvesterMultiplayerPlayerState state = new HarvesterMultiplayerPlayerState();

        assertEquals(HarvesterTransitionOutcome.APPLIED, state.applyTransition(true, T0));
        assertEquals(HarvesterTransitionOutcome.APPLIED, state.applyTransition(false, T0 + 10));
        assertEquals(HarvesterTransitionOutcome.APPLIED, state.applyTransition(true, T0 + 20));
        assertEquals(HarvesterTransitionOutcome.APPLIED, state.applyTransition(false, T0 + 30));

        HarvesterTransitionOutcome fifth = state.applyTransition(true, T0 + 40);

        assertEquals(HarvesterTransitionOutcome.RATE_LIMITED, fifth);
        assertFalse(state.active());
    }

    @Test
    void applyTransition_windowSlides_acceptsAgain() {
        HarvesterMultiplayerPlayerState state = new HarvesterMultiplayerPlayerState();
        state.applyTransition(true, T0);
        state.applyTransition(false, T0 + 10);
        state.applyTransition(true, T0 + 20);
        state.applyTransition(false, T0 + 30);
        assertEquals(HarvesterTransitionOutcome.RATE_LIMITED, state.applyTransition(true, T0 + 40));

        HarvesterTransitionOutcome afterWindow =
                state.applyTransition(true, T0 + HarvesterRateLimiter.WINDOW_MILLIS);

        assertEquals(HarvesterTransitionOutcome.APPLIED, afterWindow);
        assertTrue(state.active());
    }

    @Test
    void shouldWarnRateLimit_atMostOncePerWindow() {
        HarvesterMultiplayerPlayerState state = new HarvesterMultiplayerPlayerState();

        assertTrue(state.shouldWarnRateLimit(T0));
        assertFalse(state.shouldWarnRateLimit(T0 + 10));
        assertFalse(state.shouldWarnRateLimit(T0 + HarvesterRateLimiter.WINDOW_MILLIS - 1));
        assertTrue(state.shouldWarnRateLimit(T0 + HarvesterRateLimiter.WINDOW_MILLIS));
    }

    @Test
    void resetOnWorldChange_clearsActive_keepsSupportAnnounced() {
        HarvesterMultiplayerPlayerState state = new HarvesterMultiplayerPlayerState();
        state.markSupportAnnounced();
        state.applyTransition(true, T0);
        assertTrue(state.active());

        state.resetOnWorldChange();

        assertFalse(state.active());
        assertTrue(state.supportAnnounced(), "supportAnnounced is connection-scoped and must survive a world change");
    }

    @Test
    void resetOnWorldChange_emptiesTheRateLimiter_freshFourTransitionsAcceptedRegardlessOfPriorWindow() {
        HarvesterMultiplayerPlayerState state = new HarvesterMultiplayerPlayerState();
        state.applyTransition(true, T0);
        state.applyTransition(false, T0 + 10);
        state.applyTransition(true, T0 + 20);
        state.applyTransition(false, T0 + 30);
        // Window is full (4 real transitions in the old world); a 5th at
        // this same instant would be rejected without a world change.
        assertEquals(HarvesterTransitionOutcome.RATE_LIMITED, state.applyTransition(true, T0 + 35));

        state.resetOnWorldChange();

        // Same instant as the rejected 5th above — a fresh sequence of up
        // to 4 real transitions must be accepted in the new world,
        // independent of the old window entirely.
        assertEquals(HarvesterTransitionOutcome.APPLIED, state.applyTransition(true, T0 + 35));
        assertEquals(HarvesterTransitionOutcome.APPLIED, state.applyTransition(false, T0 + 36));
        assertEquals(HarvesterTransitionOutcome.APPLIED, state.applyTransition(true, T0 + 37));
        assertEquals(HarvesterTransitionOutcome.APPLIED, state.applyTransition(false, T0 + 38));
        assertEquals(HarvesterTransitionOutcome.RATE_LIMITED, state.applyTransition(true, T0 + 39));
    }

    @Test
    void resetOnWorldChange_clearsWarningSuppression_freshWarningIsNotSuppressed() {
        HarvesterMultiplayerPlayerState state = new HarvesterMultiplayerPlayerState();
        assertTrue(state.shouldWarnRateLimit(T0));
        assertFalse(state.shouldWarnRateLimit(T0 + 10), "second warning within the same window must be suppressed before any reset");

        state.resetOnWorldChange();

        assertTrue(
                state.shouldWarnRateLimit(T0 + 10),
                "a stale suppression window from the old world must not suppress a genuine first warning in the new one"
        );
    }

    @Test
    void resetOnWorldChange_onAlreadyDefaultState_isANoOp() {
        HarvesterMultiplayerPlayerState state = new HarvesterMultiplayerPlayerState();

        state.resetOnWorldChange();

        assertFalse(state.active());
        assertFalse(state.supportAnnounced());
    }

    @Test
    void resetOnWorldChange_independentPlayers_remainIsolated() {
        HarvesterMultiplayerPlayerState playerA = new HarvesterMultiplayerPlayerState();
        HarvesterMultiplayerPlayerState playerB = new HarvesterMultiplayerPlayerState();
        playerA.markSupportAnnounced();
        playerB.markSupportAnnounced();
        playerA.applyTransition(true, T0);
        playerB.applyTransition(true, T0);

        // Only player A changes world.
        playerA.resetOnWorldChange();

        assertFalse(playerA.active());
        assertTrue(playerA.supportAnnounced());
        assertTrue(playerB.active(), "player B's state must be untouched by player A's world change");
        assertTrue(playerB.supportAnnounced());
    }
}
