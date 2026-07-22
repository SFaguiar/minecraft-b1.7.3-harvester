package io.github.sfaguiar.harvester.client.multiplayer;

import io.github.sfaguiar.harvester.multiplayer.HarvesterServerIdentity;

import java.util.Optional;

/**
 * Client-side static holder for the most recently attempted connection's
 * address, captured by {@code ClientNetworkHandlerConnectMixin} at the
 * one point the raw, unresolved host string and explicit port are both
 * directly available (see that Mixin's own documentation). Every other
 * class reads {@link #current()} instead of touching the Mixin or the
 * underlying connection object.
 *
 * <p>Overwritten on every new connection attempt (a fresh {@code
 * ClientNetworkHandler} is constructed each time), so staleness across
 * sessions is not a concern; only single-threaded client-main-thread
 * access is assumed, matching every other static holder in this package.
 */
public final class HarvesterConnectionAddressState {

    private static volatile HarvesterServerIdentity current;

    private HarvesterConnectionAddressState() {
    }

    /** Called only by {@code ClientNetworkHandlerConnectMixin}, once per new connection attempt. */
    public static void onConnectionAttempt(String host, int port) {
        current = HarvesterServerIdentity.of(host, port);
    }

    public static Optional<HarvesterServerIdentity> current() {
        return Optional.ofNullable(current);
    }

    /** Test-only reset. */
    static void resetForTesting() {
        current = null;
    }
}
