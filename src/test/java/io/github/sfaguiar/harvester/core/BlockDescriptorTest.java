package io.github.sfaguiar.harvester.core;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class BlockDescriptorTest {

    @Test
    void specificOreTags_isImmutableAndIndependentOfTheSourceSet() {
        Set<String> mutableSource = new HashSet<>(Set.of("c:ores/coal"));
        BlockDescriptor descriptor = new BlockDescriptor(false, mutableSource, true, "minecraft:coal_ore");

        mutableSource.add("c:ores/iron");

        assertEquals(Set.of("c:ores/coal"), descriptor.specificOreTags());
        assertThrows(UnsupportedOperationException.class, () -> descriptor.specificOreTags().add("c:ores/gold"));
    }

    @Test
    void constructor_rejectsNullRegistryIdentity() {
        assertThrows(NullPointerException.class, () -> new BlockDescriptor(false, Set.of(), false, null));
    }
}
