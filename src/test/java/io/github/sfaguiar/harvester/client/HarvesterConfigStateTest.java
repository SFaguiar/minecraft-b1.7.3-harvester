package io.github.sfaguiar.harvester.client;

import io.github.sfaguiar.harvester.config.HarvesterConfig;
import io.github.sfaguiar.harvester.config.NeighborhoodChoice;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Covers only {@link HarvesterConfigState}'s storage/read behavior.
 * {@link HarvesterConfigState#load()} itself is not covered here — it
 * requires an initialized {@code FabricLoader}, which does not exist on a
 * plain JUnit classpath; that resolution glue is exercised manually per
 * the project's runtime validation gate, not by this unit test.
 */
final class HarvesterConfigStateTest {

    @AfterEach
    void resetToDefaults() {
        HarvesterConfigState.setForTesting(HarvesterConfig.DEFAULTS);
    }

    @Test
    void current_beforeAnyLoad_isDefaults() {
        assertSame(HarvesterConfig.DEFAULTS, HarvesterConfigState.current());
    }

    @Test
    void setForTesting_updatesWhatCurrentReturns() {
        HarvesterConfig custom = new HarvesterConfig(false, 8, NeighborhoodChoice.ORTHOGONAL_6, true, true, true, false);

        HarvesterConfigState.setForTesting(custom);

        assertFalse(HarvesterConfigState.current().enabled());
        assertEquals(8, HarvesterConfigState.current().maxChain());
        assertEquals(NeighborhoodChoice.ORTHOGONAL_6, HarvesterConfigState.current().neighborhood());
    }
}
