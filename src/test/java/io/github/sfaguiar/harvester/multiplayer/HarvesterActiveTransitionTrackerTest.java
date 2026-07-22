package io.github.sfaguiar.harvester.multiplayer;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class HarvesterActiveTransitionTrackerTest {

    @Test
    void press_whileEnabled_sendsOnce() {
        HarvesterActiveTransitionTracker tracker = new HarvesterActiveTransitionTracker();

        Optional<Boolean> toSend = tracker.onKeyStateChanged(true, HarvesterSupportState.SUPPORT_AVAILABLE_ENABLED);

        assertEquals(Optional.of(true), toSend);
        assertTrue(tracker.keyHeld());
    }

    @Test
    void hold_whileEnabled_doesNotRepeatSend() {
        HarvesterActiveTransitionTracker tracker = new HarvesterActiveTransitionTracker();
        tracker.onKeyStateChanged(true, HarvesterSupportState.SUPPORT_AVAILABLE_ENABLED);

        Optional<Boolean> repeated = tracker.onKeyStateChanged(true, HarvesterSupportState.SUPPORT_AVAILABLE_ENABLED);

        assertTrue(repeated.isEmpty());
    }

    @Test
    void release_whileEnabled_sendsOnce() {
        HarvesterActiveTransitionTracker tracker = new HarvesterActiveTransitionTracker();
        tracker.onKeyStateChanged(true, HarvesterSupportState.SUPPORT_AVAILABLE_ENABLED);

        Optional<Boolean> toSend = tracker.onKeyStateChanged(false, HarvesterSupportState.SUPPORT_AVAILABLE_ENABLED);

        assertEquals(Optional.of(false), toSend);
        assertFalse(tracker.keyHeld());
    }

    @Test
    void press_whileDisconnected_neverSends() {
        HarvesterActiveTransitionTracker tracker = new HarvesterActiveTransitionTracker();

        assertTrue(tracker.onKeyStateChanged(true, HarvesterSupportState.DISCONNECTED).isEmpty());
    }

    @Test
    void press_whileUnknown_neverSends() {
        HarvesterActiveTransitionTracker tracker = new HarvesterActiveTransitionTracker();

        assertTrue(tracker.onKeyStateChanged(true, HarvesterSupportState.SUPPORT_UNKNOWN).isEmpty());
    }

    @Test
    void press_whileUnavailable_neverSends() {
        HarvesterActiveTransitionTracker tracker = new HarvesterActiveTransitionTracker();

        assertTrue(tracker.onKeyStateChanged(true, HarvesterSupportState.SUPPORT_UNAVAILABLE).isEmpty());
    }

    @Test
    void press_whileDisabled_neverSends() {
        HarvesterActiveTransitionTracker tracker = new HarvesterActiveTransitionTracker();

        assertTrue(tracker.onKeyStateChanged(true, HarvesterSupportState.SUPPORT_AVAILABLE_DISABLED).isEmpty());
    }

    @Test
    void reset_clearsKeyHeldWithoutSending() {
        HarvesterActiveTransitionTracker tracker = new HarvesterActiveTransitionTracker();
        tracker.onKeyStateChanged(true, HarvesterSupportState.SUPPORT_AVAILABLE_ENABLED);

        tracker.reset();

        assertFalse(tracker.keyHeld());
        // A subsequent press is a genuine transition again (from the reset false), so it sends.
        assertEquals(Optional.of(true), tracker.onKeyStateChanged(true, HarvesterSupportState.SUPPORT_AVAILABLE_ENABLED));
    }
}
