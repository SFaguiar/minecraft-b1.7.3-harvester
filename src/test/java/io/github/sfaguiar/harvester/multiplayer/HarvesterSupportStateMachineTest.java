package io.github.sfaguiar.harvester.multiplayer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers every scenario the task's "Testes automatizados" list requires
 * for the client discovery state machine. Uses only synthetic millisecond
 * timestamps — never {@code Thread.sleep} — per
 * {@link HarvesterSupportStateMachine}'s own "testable clock" contract.
 */
final class HarvesterSupportStateMachineTest {

    private static final long T0 = 1_000_000L;

    @Test
    void initialState_isDisconnected() {
        HarvesterSupportStateMachine machine = new HarvesterSupportStateMachine();
        assertEquals(HarvesterSupportState.DISCONNECTED, machine.state());
    }

    @Test
    void connectionOperational_movesToSupportUnknown() {
        HarvesterSupportStateMachine machine = new HarvesterSupportStateMachine();

        machine.onConnectionOperational(T0);

        assertEquals(HarvesterSupportState.SUPPORT_UNKNOWN, machine.state());
    }

    @Test
    void tick_beforeFiveSeconds_staysUnknown() {
        HarvesterSupportStateMachine machine = new HarvesterSupportStateMachine();
        machine.onConnectionOperational(T0);

        machine.onTick(T0 + HarvesterSupportStateMachine.DISCOVERY_TIMEOUT_MILLIS - 1);

        assertEquals(HarvesterSupportState.SUPPORT_UNKNOWN, machine.state());
    }

    @Test
    void tick_atFiveSeconds_timesOutToUnavailable() {
        HarvesterSupportStateMachine machine = new HarvesterSupportStateMachine();
        machine.onConnectionOperational(T0);

        machine.onTick(T0 + HarvesterSupportStateMachine.DISCOVERY_TIMEOUT_MILLIS);

        assertEquals(HarvesterSupportState.SUPPORT_UNAVAILABLE, machine.state());
    }

    @Test
    void tick_neverDisconnectsOrThrows_pastTimeout() {
        HarvesterSupportStateMachine machine = new HarvesterSupportStateMachine();
        machine.onConnectionOperational(T0);

        // Multiple ticks well past timeout must stay idempotent, no exception.
        machine.onTick(T0 + 10_000);
        machine.onTick(T0 + 20_000);
        machine.onTick(T0 + 30_000);

        assertEquals(HarvesterSupportState.SUPPORT_UNAVAILABLE, machine.state());
    }

    @Test
    void announcement_beforeTimeout_movesToAvailableDisabled() {
        HarvesterSupportStateMachine machine = new HarvesterSupportStateMachine();
        machine.onConnectionOperational(T0);

        boolean applied = machine.onAnnouncementReceived(new HarvesterSupportPayload(1, false));

        assertTrue(applied);
        assertEquals(HarvesterSupportState.SUPPORT_AVAILABLE_DISABLED, machine.state());
    }

    @Test
    void announcement_afterTimeout_lateAnnouncementIsAccepted() {
        HarvesterSupportStateMachine machine = new HarvesterSupportStateMachine();
        machine.onConnectionOperational(T0);
        machine.onTick(T0 + HarvesterSupportStateMachine.DISCOVERY_TIMEOUT_MILLIS);
        assertEquals(HarvesterSupportState.SUPPORT_UNAVAILABLE, machine.state());

        boolean applied = machine.onAnnouncementReceived(new HarvesterSupportPayload(1, true));

        assertTrue(applied);
        assertEquals(HarvesterSupportState.SUPPORT_AVAILABLE_DISABLED, machine.state());
        assertTrue(machine.lastMultiplayerAllowed());
    }

    @Test
    void announcement_duplicate_isIdempotent() {
        HarvesterSupportStateMachine machine = new HarvesterSupportStateMachine();
        machine.onConnectionOperational(T0);
        HarvesterSupportPayload payload = new HarvesterSupportPayload(1, true);

        boolean firstApplied = machine.onAnnouncementReceived(payload);
        boolean secondApplied = machine.onAnnouncementReceived(payload);

        assertTrue(firstApplied);
        assertTrue(secondApplied);
        assertEquals(HarvesterSupportState.SUPPORT_AVAILABLE_DISABLED, machine.state());
    }

    @Test
    void announcement_incompatibleVersion_isRejectedAndStateUnchanged() {
        HarvesterSupportStateMachine machine = new HarvesterSupportStateMachine();
        machine.onConnectionOperational(T0);

        boolean applied = machine.onAnnouncementReceived(
                new HarvesterSupportPayload(HarvesterSupportPayload.SUPPORTED_PROTOCOL_VERSION + 1, true)
        );

        assertFalse(applied);
        assertEquals(HarvesterSupportState.SUPPORT_UNKNOWN, machine.state());
    }

    @Test
    void announcement_whileDisconnected_isRejected() {
        HarvesterSupportStateMachine machine = new HarvesterSupportStateMachine();

        boolean applied = machine.onAnnouncementReceived(new HarvesterSupportPayload(1, true));

        assertFalse(applied);
        assertEquals(HarvesterSupportState.DISCONNECTED, machine.state());
    }

    @Test
    void disconnect_clearsStateBackToDisconnected() {
        HarvesterSupportStateMachine machine = new HarvesterSupportStateMachine();
        machine.onConnectionOperational(T0);
        machine.onAnnouncementReceived(new HarvesterSupportPayload(1, true));

        machine.onDisconnected();

        assertEquals(HarvesterSupportState.DISCONNECTED, machine.state());
    }

    @Test
    void newConnection_doesNotInheritPreviousSupport() {
        HarvesterSupportStateMachine machine = new HarvesterSupportStateMachine();
        machine.onConnectionOperational(T0);
        machine.onAnnouncementReceived(new HarvesterSupportPayload(1, true));
        assertEquals(HarvesterSupportState.SUPPORT_AVAILABLE_DISABLED, machine.state());

        // Reconnect to a different (or the same) server without an intervening onDisconnected().
        machine.onConnectionOperational(T0 + 60_000);

        assertEquals(HarvesterSupportState.SUPPORT_UNKNOWN, machine.state());
        assertFalse(machine.lastMultiplayerAllowed());
    }

    @Test
    void newConnection_afterExplicitDisconnect_startsAtUnknownNotAvailable() {
        HarvesterSupportStateMachine machine = new HarvesterSupportStateMachine();
        machine.onConnectionOperational(T0);
        machine.onAnnouncementReceived(new HarvesterSupportPayload(1, true));
        machine.onDisconnected();

        machine.onConnectionOperational(T0 + 120_000);

        assertEquals(HarvesterSupportState.SUPPORT_UNKNOWN, machine.state());
    }

    @Test
    void tick_whileAlreadyAvailable_doesNotRegressToUnavailable() {
        HarvesterSupportStateMachine machine = new HarvesterSupportStateMachine();
        machine.onConnectionOperational(T0);
        machine.onAnnouncementReceived(new HarvesterSupportPayload(1, false));

        machine.onTick(T0 + HarvesterSupportStateMachine.DISCOVERY_TIMEOUT_MILLIS + 10_000);

        assertEquals(HarvesterSupportState.SUPPORT_AVAILABLE_DISABLED, machine.state());
    }
}
