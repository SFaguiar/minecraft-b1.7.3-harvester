package io.github.sfaguiar.harvester.core;

/**
 * Reads a {@link BlockDescriptor} for a coordinate. The generalized
 * counterpart of {@link BlockWorldView} (which reads only a raw block ID) —
 * a platform adapter implements this by querying a real world for a
 * StationAPI {@code BlockState} and converting it to a {@link BlockDescriptor};
 * that conversion logic lives entirely in the {@code client} package, never
 * here.
 */
@FunctionalInterface
public interface BlockDescriptorView {

    BlockDescriptor describe(BlockCoordinate coordinate);
}
