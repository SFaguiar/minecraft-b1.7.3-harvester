package io.github.sfaguiar.harvester.core;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class HarvestGroupResolverTest {

    @Test
    void resolve_logOrigin_resolvesToLogs() {
        BlockDescriptor origin = new BlockDescriptor(true, Set.of(), false, "minecraft:log");

        Optional<HarvestGroup> resolved = HarvestGroupResolver.resolve(origin);

        assertTrue(resolved.isPresent());
        assertEquals(HarvestGroupKind.LOGS, resolved.get().kind());
    }

    @Test
    void resolve_logAndOreSimultaneously_logPriorityWins() {
        // A datapack/mod misclassification: tagged as both a log and a specific ore.
        BlockDescriptor origin = new BlockDescriptor(true, Set.of("c:ores/coal"), true, "modded:confused_block");

        Optional<HarvestGroup> resolved = HarvestGroupResolver.resolve(origin);

        assertTrue(resolved.isPresent());
        assertEquals(HarvestGroupKind.LOGS, resolved.get().kind());
    }

    @Test
    void resolve_specificOreTag_resolvesToOreSpecificTags() {
        BlockDescriptor origin = new BlockDescriptor(false, Set.of("c:ores/coal"), true, "minecraft:coal_ore");

        Optional<HarvestGroup> resolved = HarvestGroupResolver.resolve(origin);

        assertTrue(resolved.isPresent());
        assertEquals(HarvestGroupKind.ORE_SPECIFIC_TAGS, resolved.get().kind());
        assertEquals(Set.of("c:ores/coal"), resolved.get().specificTags());
    }

    @Test
    void resolve_genericOreWithoutSpecificTag_resolvesToIdentityFallback() {
        BlockDescriptor origin = new BlockDescriptor(false, Set.of(), true, "modded:mystery_ore");

        Optional<HarvestGroup> resolved = HarvestGroupResolver.resolve(origin);

        assertTrue(resolved.isPresent());
        assertEquals(HarvestGroupKind.ORE_IDENTITY_FALLBACK, resolved.get().kind());
        assertEquals("modded:mystery_ore", resolved.get().identity());
    }

    @Test
    void resolve_notMarkedAsOre_isIneligible() {
        // Neither c:logs nor c:ores — an ordinary block (stone, dirt, ...).
        BlockDescriptor origin = new BlockDescriptor(false, Set.of(), false, "minecraft:stone");

        Optional<HarvestGroup> resolved = HarvestGroupResolver.resolve(origin);

        assertTrue(resolved.isEmpty());
    }

    @Test
    void resolve_multipleSpecificTagsOnOrigin_capturesAllOfThemDeterministically() {
        BlockDescriptor origin = new BlockDescriptor(
                false, Set.of("c:ores/coal", "c:ores/redstone"), true, "modded:dual_tagged_ore"
        );

        Optional<HarvestGroup> resolved = HarvestGroupResolver.resolve(origin);

        assertTrue(resolved.isPresent());
        assertEquals(HarvestGroupKind.ORE_SPECIFIC_TAGS, resolved.get().kind());
        assertEquals(Set.of("c:ores/coal", "c:ores/redstone"), resolved.get().specificTags());
    }
}
