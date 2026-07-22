package io.github.sfaguiar.harvester.multiplayer;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class HarvesterActivePayloadTest {

    @Test
    void parse_true_isValid() {
        Optional<HarvesterActivePayload> parsed = HarvesterActivePayload.parse(RawMultiplayerMessage.ofSingleBoolean(true));

        assertTrue(parsed.isPresent());
        assertTrue(parsed.get().active());
    }

    @Test
    void parse_false_isValid() {
        Optional<HarvesterActivePayload> parsed = HarvesterActivePayload.parse(RawMultiplayerMessage.ofSingleBoolean(false));

        assertTrue(parsed.isPresent());
        assertFalse(parsed.get().active());
    }

    @Test
    void parse_missingBoolean_isRejected() {
        RawMultiplayerMessage raw = new RawMultiplayerMessage(null, null, null, null, null, null, null, null, null);

        assertFalse(HarvesterActivePayload.parse(raw).isPresent());
    }

    @Test
    void parse_emptyBooleanArray_isRejected() {
        RawMultiplayerMessage raw = new RawMultiplayerMessage(null, new boolean[0], null, null, null, null, null, null, null);

        assertFalse(HarvesterActivePayload.parse(raw).isPresent());
    }

    @Test
    void parse_moreThanOneBoolean_isRejected() {
        RawMultiplayerMessage raw =
                new RawMultiplayerMessage(null, new boolean[] {true, false}, null, null, null, null, null, null, null);

        assertFalse(HarvesterActivePayload.parse(raw).isPresent());
    }

    @Test
    void parse_extraIntField_isRejected() {
        RawMultiplayerMessage raw =
                new RawMultiplayerMessage(new int[] {1}, new boolean[] {true}, null, null, null, null, null, null, null);

        assertFalse(HarvesterActivePayload.parse(raw).isPresent());
    }

    @Test
    void parse_extraStringField_isRejected() {
        RawMultiplayerMessage raw = new RawMultiplayerMessage(
                null, new boolean[] {true}, null, null, null, null, null, null, new String[] {"x"}
        );

        assertFalse(HarvesterActivePayload.parse(raw).isPresent());
    }

    @Test
    void parse_extraByteShortCharLongFloatDoubleFields_areEachRejected() {
        assertFalse(HarvesterActivePayload.parse(
                new RawMultiplayerMessage(null, new boolean[] {true}, new byte[] {1}, null, null, null, null, null, null)
        ).isPresent());
        assertFalse(HarvesterActivePayload.parse(
                new RawMultiplayerMessage(null, new boolean[] {true}, null, new short[] {1}, null, null, null, null, null)
        ).isPresent());
        assertFalse(HarvesterActivePayload.parse(
                new RawMultiplayerMessage(null, new boolean[] {true}, null, null, new char[] {'a'}, null, null, null, null)
        ).isPresent());
        assertFalse(HarvesterActivePayload.parse(
                new RawMultiplayerMessage(null, new boolean[] {true}, null, null, null, new long[] {1L}, null, null, null)
        ).isPresent());
        assertFalse(HarvesterActivePayload.parse(
                new RawMultiplayerMessage(null, new boolean[] {true}, null, null, null, null, new float[] {1f}, null, null)
        ).isPresent());
        assertFalse(HarvesterActivePayload.parse(
                new RawMultiplayerMessage(null, new boolean[] {true}, null, null, null, null, null, new double[] {1d}, null)
        ).isPresent());
    }
}
