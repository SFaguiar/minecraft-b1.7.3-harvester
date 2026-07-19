package io.github.sfaguiar.harvester.config;

import io.github.sfaguiar.harvester.core.HarvestGroupKind;
import io.github.sfaguiar.harvester.core.LegacyTwentySixNeighborhood;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class HarvesterConfigTest {

    @Test
    void defaults_matchDocumentedValues() {
        assertTrue(HarvesterConfig.DEFAULTS.enabled());
        assertEquals(64, HarvesterConfig.DEFAULTS.maxChain());
        assertEquals(NeighborhoodChoice.LEGACY_26, HarvesterConfig.DEFAULTS.neighborhood());
        assertFalse(HarvesterConfig.DEFAULTS.diagnosticLogging());
        assertTrue(HarvesterConfig.DEFAULTS.harvestLogs());
        assertTrue(HarvesterConfig.DEFAULTS.harvestOres());
        assertFalse(HarvesterConfig.DEFAULTS.multiplayerAllowed());
    }

    @Test
    void neighborhoodPolicy_reflectsConfiguredChoice() {
        HarvesterConfig config = new HarvesterConfig(true, 64, NeighborhoodChoice.LEGACY_26, false, true, true, false);
        assertInstanceOf(LegacyTwentySixNeighborhood.class, config.neighborhoodPolicy());
    }

    @Test
    void constructor_rejectsMaxChainBelowOne() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new HarvesterConfig(true, 0, NeighborhoodChoice.LEGACY_26, false, true, true, false)
        );
    }

    @Test
    void constructor_acceptsMaxChainAtUpperBound() {
        HarvesterConfig config = new HarvesterConfig(
                true, HarvesterConfig.MAX_ALLOWED_MAX_CHAIN, NeighborhoodChoice.LEGACY_26, false, true, true, false
        );
        assertEquals(HarvesterConfig.MAX_ALLOWED_MAX_CHAIN, config.maxChain());
    }

    @Test
    void constructor_rejectsMaxChainAboveUpperBound() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new HarvesterConfig(
                        true, HarvesterConfig.MAX_ALLOWED_MAX_CHAIN + 1, NeighborhoodChoice.LEGACY_26, false, true, true,
                        false
                )
        );
    }

    @Test
    void constructor_rejectsNullNeighborhood() {
        assertThrows(
                NullPointerException.class,
                () -> new HarvesterConfig(true, 64, null, false, true, true, false)
        );
    }

    @Test
    void harvestLogsAndHarvestOres_areIndependentlyConfigurable() {
        HarvesterConfig bothOff = new HarvesterConfig(true, 64, NeighborhoodChoice.LEGACY_26, false, false, false, false);
        assertFalse(bothOff.harvestLogs());
        assertFalse(bothOff.harvestOres());

        HarvesterConfig logsOnly = new HarvesterConfig(true, 64, NeighborhoodChoice.LEGACY_26, false, true, false, false);
        assertTrue(logsOnly.harvestLogs());
        assertFalse(logsOnly.harvestOres());

        HarvesterConfig oresOnly = new HarvesterConfig(true, 64, NeighborhoodChoice.LEGACY_26, false, false, true, false);
        assertFalse(oresOnly.harvestLogs());
        assertTrue(oresOnly.harvestOres());
    }

    @Test
    void isHarvestEnabledFor_mapsLogsToHarvestLogs() {
        HarvesterConfig config = new HarvesterConfig(true, 64, NeighborhoodChoice.LEGACY_26, false, false, true, false);
        assertFalse(config.isHarvestEnabledFor(HarvestGroupKind.LOGS));
    }

    @Test
    void isHarvestEnabledFor_mapsOreKindsToHarvestOres() {
        HarvesterConfig config = new HarvesterConfig(true, 64, NeighborhoodChoice.LEGACY_26, false, true, false, false);
        assertFalse(config.isHarvestEnabledFor(HarvestGroupKind.ORE_SPECIFIC_TAGS));
        assertFalse(config.isHarvestEnabledFor(HarvestGroupKind.ORE_IDENTITY_FALLBACK));
    }

    @Test
    void multiplayerAllowed_isIndependentlyConfigurable() {
        HarvesterConfig allowed = new HarvesterConfig(true, 64, NeighborhoodChoice.LEGACY_26, false, true, true, true);
        assertTrue(allowed.multiplayerAllowed());

        HarvesterConfig notAllowed = new HarvesterConfig(true, 64, NeighborhoodChoice.LEGACY_26, false, true, true, false);
        assertFalse(notAllowed.multiplayerAllowed());
    }
}
