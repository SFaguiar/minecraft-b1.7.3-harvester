package io.github.sfaguiar.harvester.client.multiplayer;

import io.github.sfaguiar.harvester.multiplayer.HarvesterSupportPayload;
import io.github.sfaguiar.harvester.multiplayer.HarvesterSupportState;
import io.github.sfaguiar.harvester.multiplayer.HarvesterSupportStateMachine;
import io.github.sfaguiar.harvester.platform.HarvesterEntrypoint;

/**
 * Client-side static holder wrapping the pure
 * {@link HarvesterSupportStateMachine}, mirroring the existing
 * {@code HarvesterClientActivationState} precedent: the StationAPI event
 * glue ({@link HarvesterSupportClientListener}) only forwards events
 * here; this class owns the state and the "log once per connection"
 * bookkeeping the design calls for. Every entry point here is called only
 * from the single client main thread (see
 * {@link HarvesterSupportStateMachine}'s own class doc for the specific
 * events that guarantee this), so — matching
 * {@code HarvesterClientActivationState} — no synchronization is used.
 */
public final class HarvesterSupportClientState {

    private static final HarvesterSupportStateMachine STATE_MACHINE = new HarvesterSupportStateMachine();

    private static boolean warnedInvalidPayloadThisConnection;
    private static boolean warnedIncompatibleVersionThisConnection;

    private HarvesterSupportClientState() {
    }

    public static HarvesterSupportState state() {
        return STATE_MACHINE.state();
    }

    public static boolean lastMultiplayerAllowed() {
        return STATE_MACHINE.lastMultiplayerAllowed();
    }

    public static void onConnectionOperational(long nowMillis) {
        STATE_MACHINE.onConnectionOperational(nowMillis);
        warnedInvalidPayloadThisConnection = false;
        warnedIncompatibleVersionThisConnection = false;
        HarvesterEntrypoint.LOGGER.info(
                "[HARVEST-MP] Connection operational; support discovery armed ({} ms timeout).",
                HarvesterSupportStateMachine.DISCOVERY_TIMEOUT_MILLIS
        );
    }

    public static void onDisconnected() {
        if (STATE_MACHINE.state() == HarvesterSupportState.DISCONNECTED) {
            return;
        }
        STATE_MACHINE.onDisconnected();
        warnedInvalidPayloadThisConnection = false;
        warnedIncompatibleVersionThisConnection = false;
        HarvesterEntrypoint.LOGGER.debug("[HARVEST-MP] Disconnected; support state reset.");
    }

    public static void onTick(long nowMillis) {
        HarvesterSupportState before = STATE_MACHINE.state();
        STATE_MACHINE.onTick(nowMillis);
        if (before != HarvesterSupportState.SUPPORT_UNAVAILABLE
                && STATE_MACHINE.state() == HarvesterSupportState.SUPPORT_UNAVAILABLE) {
            HarvesterEntrypoint.LOGGER.info(
                    "[HARVEST-MP] Support discovery timed out; server treated as unavailable this session."
            );
        }
    }

    /** Returns whether the announcement was applied (compatible version). */
    public static boolean onAnnouncementReceived(HarvesterSupportPayload payload) {
        HarvesterSupportState before = STATE_MACHINE.state();
        boolean applied = STATE_MACHINE.onAnnouncementReceived(payload);
        if (applied) {
            if (before != HarvesterSupportState.SUPPORT_AVAILABLE_DISABLED) {
                HarvesterEntrypoint.LOGGER.info(
                        "[HARVEST-MP] Server support confirmed (multiplayerAllowed={}).",
                        payload.multiplayerAllowed()
                );
            }
        } else if (!warnedIncompatibleVersionThisConnection) {
            warnedIncompatibleVersionThisConnection = true;
            HarvesterEntrypoint.LOGGER.warn(
                    "[HARVEST-MP] Ignored harvester:support with unsupported protocol version {}.",
                    payload.protocolVersion()
            );
        }
        return applied;
    }

    public static void warnOnceInvalidPayload() {
        if (!warnedInvalidPayloadThisConnection) {
            warnedInvalidPayloadThisConnection = true;
            HarvesterEntrypoint.LOGGER.warn("[HARVEST-MP] Ignored malformed harvester:support payload.");
        }
    }

    /** Test-only reset, bypassing any StationAPI event wiring. */
    static void resetForTesting() {
        STATE_MACHINE.onDisconnected();
        warnedInvalidPayloadThisConnection = false;
        warnedIncompatibleVersionThisConnection = false;
    }
}
