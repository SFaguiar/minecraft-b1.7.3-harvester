package io.github.sfaguiar.harvester.game;

import io.github.sfaguiar.harvester.core.HarvestGroupKind;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The pure kind-to-required-category mapping. The {@code matches} check
 * itself (vanilla-class {@code instanceof} plus configured ID allowlist)
 * needs real Minecraft item instances and is validated at runtime, not here.
 */
final class HarvestToolCategoryTest {

    @Test
    void logs_requireAxe() {
        assertEquals(Optional.of(HarvestToolCategory.AXE), HarvestToolCategory.requiredFor(HarvestGroupKind.LOGS));
    }

    @Test
    void dirtAndGravel_requireShovel() {
        assertEquals(Optional.of(HarvestToolCategory.SHOVEL), HarvestToolCategory.requiredFor(HarvestGroupKind.DIRT));
        assertEquals(Optional.of(HarvestToolCategory.SHOVEL), HarvestToolCategory.requiredFor(HarvestGroupKind.GRAVEL));
    }

    @Test
    void leaves_requireShears() {
        assertEquals(Optional.of(HarvestToolCategory.SHEARS), HarvestToolCategory.requiredFor(HarvestGroupKind.LEAVES));
    }

    @Test
    void crops_requireHoe() {
        assertEquals(Optional.of(HarvestToolCategory.HOE), HarvestToolCategory.requiredFor(HarvestGroupKind.CROPS));
    }

    @Test
    void oreKinds_haveNoToolCategory() {
        assertTrue(HarvestToolCategory.requiredFor(HarvestGroupKind.ORE_SPECIFIC_TAGS).isEmpty());
        assertTrue(HarvestToolCategory.requiredFor(HarvestGroupKind.ORE_IDENTITY_FALLBACK).isEmpty());
    }
}
