package io.github.sfaguiar.harvester.server.multiplayer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pure unit tests for {@link ServerHarvestChainGate} — every precondition combination. */
final class ServerHarvestChainGateTest {

    @Test
    void allPreconditionsTrue_isEligible() {
        assertTrue(ServerHarvestChainGate.isEligible(true, true, true, true));
    }

    @Test
    void connectionNotModded_isNotEligible() {
        assertFalse(ServerHarvestChainGate.isEligible(false, true, true, true));
    }

    @Test
    void supportNotAnnounced_isNotEligible() {
        assertFalse(ServerHarvestChainGate.isEligible(true, false, true, true));
    }

    @Test
    void multiplayerNotAllowed_serverDenied_isNotEligible() {
        assertFalse(ServerHarvestChainGate.isEligible(true, true, false, true));
    }

    @Test
    void playerNotActive_isNotEligible() {
        assertFalse(ServerHarvestChainGate.isEligible(true, true, true, false));
    }

    @Test
    void everyPreconditionFalse_isNotEligible() {
        assertFalse(ServerHarvestChainGate.isEligible(false, false, false, false));
    }
}
