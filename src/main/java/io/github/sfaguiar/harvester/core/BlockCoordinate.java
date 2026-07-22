package io.github.sfaguiar.harvester.core;

import java.util.List;

/**
 * Immutable integer block position. Carries no Minecraft, StationAPI,
 * Fabric, or Mixin type.
 */
public final class BlockCoordinate {

    private final int x;
    private final int y;
    private final int z;

    public BlockCoordinate(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public int z() {
        return z;
    }

    /**
     * The six orthogonally adjacent coordinates (no diagonals), in a fixed,
     * deterministic order. Both {@link ConnectedBlockFinder} and any test
     * asserting visitation order depend on this exact order never changing
     * without a corresponding test update.
     */
    public List<BlockCoordinate> orthogonalNeighbors() {
        return List.of(
                new BlockCoordinate(x - 1, y, z),
                new BlockCoordinate(x + 1, y, z),
                new BlockCoordinate(x, y - 1, z),
                new BlockCoordinate(x, y + 1, z),
                new BlockCoordinate(x, y, z - 1),
                new BlockCoordinate(x, y, z + 1)
        );
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof BlockCoordinate)) {
            return false;
        }
        BlockCoordinate that = (BlockCoordinate) other;
        return x == that.x && y == that.y && z == that.z;
    }

    @Override
    public int hashCode() {
        int result = x;
        result = 31 * result + y;
        result = 31 * result + z;
        return result;
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ", " + z + ")";
    }
}
