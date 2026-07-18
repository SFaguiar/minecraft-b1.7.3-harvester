package io.github.sfaguiar.harvester.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** In-memory {@link BlockDescriptorView} for pure unit tests. Never touches Minecraft. */
final class FakeDescriptorView implements BlockDescriptorView {

    private final Map<BlockCoordinate, BlockDescriptor> descriptors = new HashMap<>();
    private final BlockDescriptor defaultDescriptor;

    FakeDescriptorView(BlockDescriptor defaultDescriptor) {
        this.defaultDescriptor = defaultDescriptor;
    }

    void set(int x, int y, int z, BlockDescriptor descriptor) {
        descriptors.put(new BlockCoordinate(x, y, z), descriptor);
    }

    void setLog(int x, int y, int z) {
        set(x, y, z, new BlockDescriptor(true, Set.of(), false, "minecraft:log"));
    }

    void setOre(int x, int y, int z, String specificTag, String identity) {
        set(x, y, z, new BlockDescriptor(false, Set.of(specificTag), true, identity));
    }

    @Override
    public BlockDescriptor describe(BlockCoordinate coordinate) {
        return descriptors.getOrDefault(coordinate, defaultDescriptor);
    }
}
