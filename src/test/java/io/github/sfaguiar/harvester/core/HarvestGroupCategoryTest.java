package io.github.sfaguiar.harvester.core;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Membership rules for the 1.0.0 dirt/gravel/leaf/crop categories. */
final class HarvestGroupCategoryTest {

    private static BlockDescriptor leaf(int meta) {
        return new BlockDescriptor(false, Set.of(), false, "minecraft:leaves", meta, false, false, true, false);
    }

    private static BlockDescriptor crop(int meta) {
        return new BlockDescriptor(false, Set.of(), false, "minecraft:wheat", meta, false, false, false, true);
    }

    private static BlockDescriptor dirt() {
        return new BlockDescriptor(false, Set.of(), false, "minecraft:dirt", 0, true, false, false, false);
    }

    private static BlockDescriptor gravel() {
        return new BlockDescriptor(false, Set.of(), false, "minecraft:gravel", 0, false, true, false, false);
    }

    @Test
    void dirtGroup_matchesDirtOnly() {
        HarvestGroup group = HarvestGroup.dirt();
        assertTrue(group.matches(dirt()));
        assertFalse(group.matches(gravel()));
        assertFalse(group.matches(leaf(0)));
    }

    @Test
    void gravelGroup_matchesGravelOnly() {
        HarvestGroup group = HarvestGroup.gravel();
        assertTrue(group.matches(gravel()));
        assertFalse(group.matches(dirt()));
    }

    @Test
    void leavesGroup_matchesSameSpeciesIgnoringDecayBits() {
        // Origin species = oak (0). Candidate meta 8 = oak + decay-flag bit → same species.
        HarvestGroup group = HarvestGroup.leaves(0);
        assertTrue(group.matches(leaf(0)));
        assertTrue(group.matches(leaf(8)));
        assertTrue(group.matches(leaf(12)));
    }

    @Test
    void leavesGroup_rejectsDifferentSpecies() {
        HarvestGroup group = HarvestGroup.leaves(0);
        // Species 1 (spruce) and 2 (birch), with and without flag bits.
        assertFalse(group.matches(leaf(1)));
        assertFalse(group.matches(leaf(9)));
        assertFalse(group.matches(leaf(2)));
    }

    @Test
    void leavesGroup_rejectsNonLeaf() {
        assertFalse(HarvestGroup.leaves(0).matches(dirt()));
    }

    @Test
    void cropsGroup_matchesMatureOnly() {
        HarvestGroup group = HarvestGroup.crops();
        assertTrue(group.matches(crop(7)));
        assertFalse(group.matches(crop(6)));
        assertFalse(group.matches(crop(0)));
    }

    @Test
    void cropsGroup_rejectsNonCrop() {
        assertFalse(HarvestGroup.crops().matches(dirt()));
    }
}
