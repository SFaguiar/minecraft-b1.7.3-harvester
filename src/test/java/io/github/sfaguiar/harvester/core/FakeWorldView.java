package io.github.sfaguiar.harvester.core;

import java.util.HashMap;
import java.util.Map;

/** In-memory {@link BlockWorldView} for pure unit tests. Never touches Minecraft. */
final class FakeWorldView implements BlockWorldView {

    private final Map<BlockCoordinate, Integer> blockIds = new HashMap<>();
    private final int defaultBlockId;

    FakeWorldView(int defaultBlockId) {
        this.defaultBlockId = defaultBlockId;
    }

    void set(int x, int y, int z, int blockId) {
        blockIds.put(new BlockCoordinate(x, y, z), blockId);
    }

    @Override
    public int getBlockId(BlockCoordinate coordinate) {
        return blockIds.getOrDefault(coordinate, defaultBlockId);
    }
}
