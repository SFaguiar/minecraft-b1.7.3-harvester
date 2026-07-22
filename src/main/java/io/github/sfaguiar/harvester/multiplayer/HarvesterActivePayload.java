package io.github.sfaguiar.harvester.multiplayer;

import java.util.Optional;

/**
 * Pure model of the {@code harvester:active} (client → server) payload:
 * exactly one boolean (the new {@code active} value), no other field —
 * see {@code ARCHITECTURE.md}, "Protocol v1 (design)". Unlike {@link
 * HarvesterSupportPayload#parse}, this parse is strict about extra
 * fields: a modified client attaching coordinates, a tool id, or any
 * other array/string alongside the boolean is rejected outright, never
 * partially applied — the server never trusts anything from the client
 * beyond this single boolean.
 */
public final class HarvesterActivePayload {

    private final boolean active;

    private HarvesterActivePayload(boolean active) {
        this.active = active;
    }

    public boolean active() {
        return active;
    }

    /**
     * Empty unless {@code raw} carries exactly one boolean and every
     * other field is absent or empty.
     */
    public static Optional<HarvesterActivePayload> parse(RawMultiplayerMessage raw) {
        if (raw.booleans == null || raw.booleans.length != 1) {
            return Optional.empty();
        }
        if (isNonEmpty(raw.ints) || isNonEmpty(raw.bytes) || isNonEmpty(raw.shorts) || isNonEmpty(raw.chars)
                || isNonEmpty(raw.longs) || isNonEmpty(raw.floats) || isNonEmpty(raw.doubles)
                || isNonEmpty(raw.strings)) {
            return Optional.empty();
        }
        return Optional.of(new HarvesterActivePayload(raw.booleans[0]));
    }

    private static boolean isNonEmpty(int[] array) {
        return array != null && array.length > 0;
    }

    private static boolean isNonEmpty(byte[] array) {
        return array != null && array.length > 0;
    }

    private static boolean isNonEmpty(short[] array) {
        return array != null && array.length > 0;
    }

    private static boolean isNonEmpty(char[] array) {
        return array != null && array.length > 0;
    }

    private static boolean isNonEmpty(long[] array) {
        return array != null && array.length > 0;
    }

    private static boolean isNonEmpty(float[] array) {
        return array != null && array.length > 0;
    }

    private static boolean isNonEmpty(double[] array) {
        return array != null && array.length > 0;
    }

    private static boolean isNonEmpty(Object[] array) {
        return array != null && array.length > 0;
    }

    @Override
    public String toString() {
        return "HarvesterActivePayload{active=" + active + "}";
    }
}
