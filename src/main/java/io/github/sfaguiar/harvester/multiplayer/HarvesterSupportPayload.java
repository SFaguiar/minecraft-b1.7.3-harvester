package io.github.sfaguiar.harvester.multiplayer;

import java.util.Objects;
import java.util.Optional;

/**
 * Pure model of the {@code harvester:support} (server → client) payload:
 * a protocol version and the server's {@code multiplayerAllowed} setting.
 * No capability list, no gameplay data — see {@code ARCHITECTURE.md},
 * "Protocol v1 (design)".
 *
 * <p>Mirrors {@code MessagePacket}'s {@code ints}/{@code booleans}
 * primitive-array shape ({@code ints[0]} = protocol version,
 * {@code booleans[0]} = {@code multiplayerAllowed}) without importing
 * StationAPI, so encoding/decoding is unit-testable without Minecraft.
 */
public final class HarvesterSupportPayload {

    /** The only protocol version this tranche implements. */
    public static final int SUPPORTED_PROTOCOL_VERSION = 1;

    private final int protocolVersion;
    private final boolean multiplayerAllowed;

    public HarvesterSupportPayload(int protocolVersion, boolean multiplayerAllowed) {
        this.protocolVersion = protocolVersion;
        this.multiplayerAllowed = multiplayerAllowed;
    }

    public int protocolVersion() {
        return protocolVersion;
    }

    public boolean multiplayerAllowed() {
        return multiplayerAllowed;
    }

    public boolean isSupportedVersion() {
        return protocolVersion == SUPPORTED_PROTOCOL_VERSION;
    }

    public int[] toInts() {
        return new int[] {protocolVersion};
    }

    public boolean[] toBooleans() {
        return new boolean[] {multiplayerAllowed};
    }

    /**
     * Parses a received payload's raw arrays. Empty when either array is
     * missing or too short to hold the one value each carries — the
     * caller (the client's message listener) treats an empty result as
     * "malformed payload": logged once, feature stays unavailable, never
     * a thrown exception.
     */
    public static Optional<HarvesterSupportPayload> parse(int[] ints, boolean[] booleans) {
        if (ints == null || ints.length < 1 || booleans == null || booleans.length < 1) {
            return Optional.empty();
        }
        return Optional.of(new HarvesterSupportPayload(ints[0], booleans[0]));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof HarvesterSupportPayload other)) {
            return false;
        }
        return protocolVersion == other.protocolVersion && multiplayerAllowed == other.multiplayerAllowed;
    }

    @Override
    public int hashCode() {
        return Objects.hash(protocolVersion, multiplayerAllowed);
    }

    @Override
    public String toString() {
        return "HarvesterSupportPayload{protocolVersion=" + protocolVersion
                + ", multiplayerAllowed=" + multiplayerAllowed + "}";
    }
}
