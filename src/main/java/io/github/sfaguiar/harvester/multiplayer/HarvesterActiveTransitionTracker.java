package io.github.sfaguiar.harvester.multiplayer;

import java.util.Optional;

/**
 * Pure decision logic for whether a client-side activation-key edge
 * should produce a {@code harvester:active} send, and what value —
 * fully unit-testable without any StationAPI/Minecraft dependency,
 * mirroring {@link HarvesterSupportStateMachine}'s "pure decision, impure
 * glue sends" split.
 *
 * <p>Tracks the activation key's held/released state independently of
 * {@link HarvesterSupportState} (per the task's "o estado da tecla deve
 * ser separado do estado de suporte") — holding the key while support is
 * unavailable, then the server later becoming available, does not by
 * itself trigger a send; only a genuine press/release edge does, and
 * only while support is currently {@link
 * HarvesterSupportState#SUPPORT_AVAILABLE_ENABLED}.
 */
public final class HarvesterActiveTransitionTracker {

    private boolean keyHeld;

    public boolean keyHeld() {
        return keyHeld;
    }

    /**
     * @param pressed the key's new raw state (true = pressed, false = released)
     * @param currentSupportState the support state at the moment this edge occurs
     * @return the value to send, or empty when this edge produces no send —
     *         either because it is not a genuine transition (holding the
     *         key repeats no send) or because support is not currently
     *         {@code SUPPORT_AVAILABLE_ENABLED}
     */
    public Optional<Boolean> onKeyStateChanged(boolean pressed, HarvesterSupportState currentSupportState) {
        if (keyHeld == pressed) {
            return Optional.empty();
        }
        keyHeld = pressed;
        if (currentSupportState != HarvesterSupportState.SUPPORT_AVAILABLE_ENABLED) {
            return Optional.empty();
        }
        return Optional.of(pressed);
    }

    /** Disconnect (or a fresh connection that never inherits prior state) resets the tracked key state; never itself sends anything. */
    public void reset() {
        keyHeld = false;
    }
}
