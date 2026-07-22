package io.github.sfaguiar.harvester.multiplayer;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class HarvesterSupportPayloadTest {

    @Test
    void roundTrip_serializationAndParsing_preservesValues() {
        HarvesterSupportPayload original = new HarvesterSupportPayload(1, true);

        Optional<HarvesterSupportPayload> parsed =
                HarvesterSupportPayload.parse(original.toInts(), original.toBooleans());

        assertTrue(parsed.isPresent());
        assertEquals(original, parsed.get());
        assertEquals(1, parsed.get().protocolVersion());
        assertTrue(parsed.get().multiplayerAllowed());
    }

    @Test
    void roundTrip_multiplayerAllowedFalse_isPreserved() {
        HarvesterSupportPayload original = new HarvesterSupportPayload(1, false);

        Optional<HarvesterSupportPayload> parsed =
                HarvesterSupportPayload.parse(original.toInts(), original.toBooleans());

        assertTrue(parsed.isPresent());
        assertFalse(parsed.get().multiplayerAllowed());
    }

    @Test
    void parse_nullIntsArray_isInvalid() {
        assertTrue(HarvesterSupportPayload.parse(null, new boolean[] {true}).isEmpty());
    }

    @Test
    void parse_emptyIntsArray_isInvalid() {
        assertTrue(HarvesterSupportPayload.parse(new int[0], new boolean[] {true}).isEmpty());
    }

    @Test
    void parse_nullBooleansArray_isInvalid() {
        assertTrue(HarvesterSupportPayload.parse(new int[] {1}, null).isEmpty());
    }

    @Test
    void parse_emptyBooleansArray_isInvalid() {
        assertTrue(HarvesterSupportPayload.parse(new int[] {1}, new boolean[0]).isEmpty());
    }

    @Test
    void isSupportedVersion_matchesConstant() {
        assertTrue(new HarvesterSupportPayload(HarvesterSupportPayload.SUPPORTED_PROTOCOL_VERSION, true)
                .isSupportedVersion());
        assertFalse(new HarvesterSupportPayload(HarvesterSupportPayload.SUPPORTED_PROTOCOL_VERSION + 1, true)
                .isSupportedVersion());
    }
}
