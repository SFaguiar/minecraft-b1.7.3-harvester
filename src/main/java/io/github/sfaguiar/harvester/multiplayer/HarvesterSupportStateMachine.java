package io.github.sfaguiar.harvester.multiplayer;

/**
 * Pure client-side discovery state machine — see {@code ARCHITECTURE.md},
 * "Client support state machine (design)" and "Owner decisions locking
 * this tranche's scope" for the transition table this implements.
 *
 * <p>Driven entirely by caller-supplied millisecond timestamps (fed from
 * {@code System.currentTimeMillis()} at each real {@code GameTickEvent.End}
 * in production) rather than a tick counter, so it never assumes a
 * specific tick rate and never calls {@code Thread.sleep} — tests drive
 * it with synthetic timestamps only.
 *
 * <p>Never sends a packet and never disconnects anything; it only tracks
 * local state. Not thread-safe by design — every StationAPI event this is
 * driven from (`ServerLoginSuccessEvent`, `MultiplayerLogoutEvent`,
 * `GameTickEvent.End`, an incoming `MessagePacket`) already runs
 * synchronously on the single client main thread (confirmed in
 * `ARCHITECTURE.md`'s "Networking API" section), matching the existing
 * {@code HarvesterClientActivationState} precedent of no synchronization.
 */
public final class HarvesterSupportStateMachine {

    /** Owner-decided exact value — see "Owner decisions locking this tranche's scope". */
    public static final long DISCOVERY_TIMEOUT_MILLIS = 5000L;

    private HarvesterSupportState state = HarvesterSupportState.DISCONNECTED;
    private long unknownEnteredAtMillis = -1L;
    private boolean timedOut;
    private boolean lastMultiplayerAllowed;

    public HarvesterSupportState state() {
        return state;
    }

    /** The server's last-announced {@code multiplayerAllowed} value; meaningless before any announcement was applied. */
    public boolean lastMultiplayerAllowed() {
        return lastMultiplayerAllowed;
    }

    /**
     * The client connection became operational ({@code ServerLoginSuccessEvent}).
     * Unconditional: a reconnect never inherits the previous connection's
     * state — this always resets to {@code SUPPORT_UNKNOWN} regardless of
     * the current state.
     */
    public void onConnectionOperational(long nowMillis) {
        state = HarvesterSupportState.SUPPORT_UNKNOWN;
        unknownEnteredAtMillis = nowMillis;
        timedOut = false;
        lastMultiplayerAllowed = false;
    }

    /** Disconnected ({@code MultiplayerLogoutEvent}) or switching servers: full reset. */
    public void onDisconnected() {
        state = HarvesterSupportState.DISCONNECTED;
        unknownEnteredAtMillis = -1L;
        timedOut = false;
        lastMultiplayerAllowed = false;
    }

    /**
     * Drive the discovery timeout. A no-op unless currently
     * {@code SUPPORT_UNKNOWN} and the timeout has not already fired for
     * this connection.
     */
    public void onTick(long nowMillis) {
        if (state == HarvesterSupportState.SUPPORT_UNKNOWN
                && !timedOut
                && unknownEnteredAtMillis >= 0
                && nowMillis - unknownEnteredAtMillis >= DISCOVERY_TIMEOUT_MILLIS) {
            state = HarvesterSupportState.SUPPORT_UNAVAILABLE;
            timedOut = true;
        }
    }

    /**
     * A validated {@code harvester:support} payload arrived. Returns
     * {@code false} without changing state for an incompatible protocol
     * version or while {@code DISCONNECTED} (defensive — real wiring never
     * calls this before a connection is operational); returns {@code true}
     * and moves to {@code SUPPORT_AVAILABLE_DISABLED} otherwise, from
     * {@code SUPPORT_UNKNOWN} (before timeout) or
     * {@code SUPPORT_UNAVAILABLE} (a late announcement, explicitly accepted
     * per owner decision) or idempotently from
     * {@code SUPPORT_AVAILABLE_DISABLED} itself (a duplicate packet).
     */
    public boolean onAnnouncementReceived(HarvesterSupportPayload payload) {
        if (state == HarvesterSupportState.DISCONNECTED || !payload.isSupportedVersion()) {
            return false;
        }
        state = HarvesterSupportState.SUPPORT_AVAILABLE_DISABLED;
        lastMultiplayerAllowed = payload.multiplayerAllowed();
        return true;
    }
}
