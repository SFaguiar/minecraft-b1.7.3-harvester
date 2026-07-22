package io.github.sfaguiar.harvester.client.multiplayer;

import io.github.sfaguiar.harvester.multiplayer.HarvesterActiveTransitionTracker;

import java.util.Optional;

/**
 * Client-side static holder wrapping the pure {@link
 * HarvesterActiveTransitionTracker}, mirroring the {@link
 * HarvesterSupportClientState} precedent: {@code
 * HarvesterKeyBindingListener} forwards every raw activation-key
 * press/release edge here (in addition to updating {@code
 * HarvesterClientActivationState} for the existing singleplayer chain),
 * and this class decides whether that edge should send {@code
 * harvester:active} and performs the send.
 */
public final class HarvesterMultiplayerActivationState {

    private static final HarvesterActiveTransitionTracker TRACKER = new HarvesterActiveTransitionTracker();

    private HarvesterMultiplayerActivationState() {
    }

    public static boolean keyHeld() {
        return TRACKER.keyHeld();
    }

    /** Forwarded for every raw press/release edge of the Harvester activation key, regardless of current support state. */
    public static void onKeyStateChanged(boolean pressed) {
        Optional<Boolean> toSend = TRACKER.onKeyStateChanged(pressed, HarvesterSupportClientState.state());
        toSend.ifPresent(HarvesterActiveClientSender::send);
    }

    /** Disconnect (or a fresh connection replacing a prior one) clears the tracked key state — a new connection never inherits it. */
    public static void resetForDisconnect() {
        TRACKER.reset();
    }
}
