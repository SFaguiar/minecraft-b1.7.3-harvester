package io.github.sfaguiar.harvester.core;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The classification matrix the harvestable-groups model exists to satisfy
 * — every case is expressed purely in terms of {@link BlockDescriptor},
 * with no Minecraft or StationAPI type involved.
 */
final class HarvestGroupTest {

    private static BlockDescriptor log(String identity) {
        return new BlockDescriptor(true, Set.of(), false, identity);
    }

    private static BlockDescriptor ore(String specificTag, String identity) {
        return new BlockDescriptor(false, Set.of(specificTag), true, identity);
    }

    private static BlockDescriptor genericOreOnly(String identity) {
        return new BlockDescriptor(false, Set.of(), true, identity);
    }

    private static BlockDescriptor commonBlock(String identity) {
        return new BlockDescriptor(false, Set.of(), false, identity);
    }

    @Test
    void logsGroup_matchesAnyLogRegardlessOfIdentity() {
        HarvestGroup logs = HarvestGroup.logs();

        assertTrue(logs.matches(log("minecraft:log")));
        assertTrue(logs.matches(log("modded:exotic_log")));
    }

    @Test
    void logsGroup_neverMatchesANonLog() {
        HarvestGroup logs = HarvestGroup.logs();

        assertFalse(logs.matches(ore("c:ores/coal", "minecraft:coal_ore")));
        assertFalse(logs.matches(commonBlock("minecraft:stone")));
    }

    @Test
    void coalMatchesCoal() {
        HarvestGroup coal = HarvestGroup.oreSpecificTags(Set.of("c:ores/coal"));

        assertTrue(coal.matches(ore("c:ores/coal", "minecraft:coal_ore")));
    }

    @Test
    void coalDoesNotMatchIron() {
        HarvestGroup coal = HarvestGroup.oreSpecificTags(Set.of("c:ores/coal"));

        assertFalse(coal.matches(ore("c:ores/iron", "minecraft:iron_ore")));
    }

    @Test
    void ironDoesNotMatchGold() {
        HarvestGroup iron = HarvestGroup.oreSpecificTags(Set.of("c:ores/iron"));

        assertFalse(iron.matches(ore("c:ores/gold", "minecraft:gold_ore")));
    }

    @Test
    void diamondDoesNotMatchLapis() {
        HarvestGroup diamond = HarvestGroup.oreSpecificTags(Set.of("c:ores/diamond"));

        assertFalse(diamond.matches(ore("c:ores/lapis", "minecraft:lapis_ore")));
    }

    @Test
    void litAndUnlitRedstoneMatchTheSameSpecificTag() {
        HarvestGroup redstone = HarvestGroup.oreSpecificTags(Set.of("c:ores/redstone"));

        assertTrue(redstone.matches(ore("c:ores/redstone", "minecraft:redstone_ore")));
        assertTrue(redstone.matches(ore("c:ores/redstone", "minecraft:redstone_ore_lit")));
    }

    @Test
    void moddedAndVanillaOreSharingTheSameSpecificTagMatch() {
        HarvestGroup iron = HarvestGroup.oreSpecificTags(Set.of("c:ores/iron"));

        assertTrue(iron.matches(ore("c:ores/iron", "minecraft:iron_ore")));
        assertTrue(iron.matches(ore("c:ores/iron", "modded:deepslate_iron_ore")));
    }

    @Test
    void differentSpecificTags_neverMatch() {
        HarvestGroup coal = HarvestGroup.oreSpecificTags(Set.of("c:ores/coal"));
        HarvestGroup tin = HarvestGroup.oreSpecificTags(Set.of("c:ores/tin"));

        BlockDescriptor coalOre = ore("c:ores/coal", "minecraft:coal_ore");
        BlockDescriptor tinOre = ore("c:ores/tin", "modded:tin_ore");

        assertFalse(coal.matches(tinOre));
        assertFalse(tin.matches(coalOre));
    }

    @Test
    void multipleSpecificTagsOnGroup_matchIfCandidateSharesAtLeastOne() {
        HarvestGroup coalOrRedstone = HarvestGroup.oreSpecificTags(Set.of("c:ores/coal", "c:ores/redstone"));

        assertTrue(coalOrRedstone.matches(ore("c:ores/coal", "minecraft:coal_ore")));
        assertTrue(coalOrRedstone.matches(ore("c:ores/redstone", "minecraft:redstone_ore")));
        assertFalse(coalOrRedstone.matches(ore("c:ores/iron", "minecraft:iron_ore")));
    }

    @Test
    void identityFallback_matchesOnlyTheSameRegisteredIdentity() {
        HarvestGroup fallback = HarvestGroup.oreIdentityFallback("modded:mystery_ore");

        assertTrue(fallback.matches(genericOreOnly("modded:mystery_ore")));
        assertFalse(fallback.matches(genericOreOnly("modded:other_mystery_ore")));
    }

    @Test
    void genericOresTagAlone_neverGroupsDifferentIdentitiesTogether() {
        // Two different blocks, both only in the generic c:ores aggregator
        // (no usable specific tag) - each resolves its own identity
        // fallback group, and neither's group matches the other's block.
        HarvestGroup fallbackA = HarvestGroup.oreIdentityFallback("modded:ore_a");
        HarvestGroup fallbackB = HarvestGroup.oreIdentityFallback("modded:ore_b");

        assertFalse(fallbackA.matches(genericOreOnly("modded:ore_b")));
        assertFalse(fallbackB.matches(genericOreOnly("modded:ore_a")));
    }

    @Test
    void identityFallback_neverMatchesALog() {
        HarvestGroup fallback = HarvestGroup.oreIdentityFallback("minecraft:log");

        // Even a contrived case where a log happened to share the fallback's
        // identity string - log priority still wins on the candidate side.
        assertFalse(fallback.matches(log("minecraft:log")));
    }

    @Test
    void specificTagGroup_neverMatchesALog() {
        HarvestGroup coal = HarvestGroup.oreSpecificTags(Set.of("c:ores/coal"));
        // A candidate simultaneously tagged as a log and as coal ore (a
        // datapack/mod misclassification) - log priority excludes it.
        BlockDescriptor confusedLog = new BlockDescriptor(true, Set.of("c:ores/coal"), true, "modded:confused");

        assertFalse(coal.matches(confusedLog));
    }

    @Test
    void oreSpecificTags_rejectsEmptySet() {
        assertThrows(IllegalArgumentException.class, () -> HarvestGroup.oreSpecificTags(Set.of()));
    }
}
