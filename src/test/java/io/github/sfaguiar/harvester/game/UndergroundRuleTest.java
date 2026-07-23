package io.github.sfaguiar.harvester.game;

import io.github.sfaguiar.harvester.config.HarvesterConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pure decision behind the dirt/gravel underground gate (owner Decision D). */
final class UndergroundRuleTest {

    private static final int OVERWORLD = UndergroundRule.OVERWORLD_DIMENSION_ID;
    private static final int NETHER = -1;

    private static HarvesterConfig defaults() {
        return HarvesterConfig.DEFAULTS; // requiresNoSky=true, maxY=63, overworldOnly=true
    }

    @Test
    void overworldBuriedBelowSeaLevel_isUnderground() {
        assertTrue(UndergroundRule.isUnderground(OVERWORLD, false, 40, defaults()));
        assertTrue(UndergroundRule.isUnderground(OVERWORLD, false, 63, defaults()));
    }

    @Test
    void exposedToSky_isNotUnderground() {
        assertFalse(UndergroundRule.isUnderground(OVERWORLD, true, 40, defaults()));
    }

    @Test
    void aboveMaxY_isNotUnderground() {
        assertFalse(UndergroundRule.isUnderground(OVERWORLD, false, 64, defaults()));
        assertFalse(UndergroundRule.isUnderground(OVERWORLD, false, 90, defaults()));
    }

    @Test
    void nonOverworld_isNotUnderground_whenOverworldOnly() {
        assertFalse(UndergroundRule.isUnderground(NETHER, false, 40, defaults()));
    }

    @Test
    void nonOverworld_allowedWhenOverworldOnlyDisabled() {
        HarvesterConfig config = HarvesterConfig.DEFAULTS.toBuilder().undergroundOverworldOnly(false).build();
        assertTrue(UndergroundRule.isUnderground(NETHER, false, 40, config));
    }

    @Test
    void skyExposed_allowedWhenNoSkyRequirementDisabled() {
        HarvesterConfig config = HarvesterConfig.DEFAULTS.toBuilder().undergroundRequiresNoSky(false).build();
        assertTrue(UndergroundRule.isUnderground(OVERWORLD, true, 40, config));
    }

    @Test
    void higherMaxY_extendsRange() {
        HarvesterConfig config = HarvesterConfig.DEFAULTS.toBuilder().undergroundMaxY(120).build();
        assertTrue(UndergroundRule.isUnderground(OVERWORLD, false, 100, config));
    }
}
