package io.github.sfaguiar.harvester.client;

import io.github.sfaguiar.harvester.core.HarvestGroupKind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers only {@link HarvestToolCompatibility#requiresToolCheck} - the one
 * pure decision in this class. {@link HarvestToolCompatibility#canHarvest}
 * itself calls the real StationAPI {@code isSuitableFor} API and cannot run
 * without Minecraft started; it is covered by manual runtime validation
 * instead (R6/R7 in the harvester repository's runtime test procedure).
 */
final class HarvestToolCompatibilityTest {

    @Test
    void requiresToolCheck_logsNeverGateOnTool() {
        assertFalse(HarvestToolCompatibility.requiresToolCheck(HarvestGroupKind.LOGS));
    }

    @Test
    void requiresToolCheck_oreKindsAlwaysGateOnTool() {
        assertTrue(HarvestToolCompatibility.requiresToolCheck(HarvestGroupKind.ORE_SPECIFIC_TAGS));
        assertTrue(HarvestToolCompatibility.requiresToolCheck(HarvestGroupKind.ORE_IDENTITY_FALLBACK));
    }
}
