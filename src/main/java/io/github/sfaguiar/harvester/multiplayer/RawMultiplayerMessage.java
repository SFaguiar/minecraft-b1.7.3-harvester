package io.github.sfaguiar.harvester.multiplayer;

/**
 * Mirrors {@code MessagePacket}'s full set of primitive-array payload
 * fields without importing StationAPI, so strict "no extra fields"
 * validation ({@link HarvesterActivePayload#parse}) is unit-testable
 * without Minecraft. The glue layer that actually receives a
 * {@code MessagePacket} constructs one of these from its own array
 * fields; this class never depends on that packet type itself.
 */
public final class RawMultiplayerMessage {

    public final int[] ints;
    public final boolean[] booleans;
    public final byte[] bytes;
    public final short[] shorts;
    public final char[] chars;
    public final long[] longs;
    public final float[] floats;
    public final double[] doubles;
    public final String[] strings;

    public RawMultiplayerMessage(
            int[] ints,
            boolean[] booleans,
            byte[] bytes,
            short[] shorts,
            char[] chars,
            long[] longs,
            float[] floats,
            double[] doubles,
            String[] strings
    ) {
        this.ints = ints;
        this.booleans = booleans;
        this.bytes = bytes;
        this.shorts = shorts;
        this.chars = chars;
        this.longs = longs;
        this.floats = floats;
        this.doubles = doubles;
        this.strings = strings;
    }

    /** A boolean-only message: exactly one boolean, every other field absent — the only shape {@code harvester:active} ever sends. */
    public static RawMultiplayerMessage ofSingleBoolean(boolean value) {
        return new RawMultiplayerMessage(null, new boolean[] {value}, null, null, null, null, null, null, null);
    }
}
