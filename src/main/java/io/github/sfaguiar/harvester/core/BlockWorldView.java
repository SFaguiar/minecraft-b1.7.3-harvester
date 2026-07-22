package io.github.sfaguiar.harvester.core;

/**
 * Read-only access to block IDs at a coordinate, abstracted away from any
 * concrete world implementation. The core module never imports Minecraft,
 * StationAPI, Fabric, or Mixin types; a platform adapter is responsible for
 * implementing this against a real world.
 *
 * <p>Implementations are not required to represent the origin position
 * specially. {@link ConnectedBlockFinder} never re-queries a
 * {@link BlockGroupView} built from this view for the origin coordinate
 * itself — the origin's identity comes from the {@link HarvestRequest} it
 * was constructed with, captured before any removal happened. This matters
 * because, by the time discovery runs (typically from a callback observing
 * a just-completed break), the world may already report the origin
 * position as air or another block.
 *
 * <p>Used together with a {@link BlockMatcher} via
 * {@link BlockGroupView#byId(BlockWorldView, BlockMatcher)}. Not every
 * {@link BlockGroupView} is built this way — tag-based membership (see
 * {@link BlockGroupView}) skips this interface entirely.
 */
@FunctionalInterface
public interface BlockWorldView {

    int getBlockId(BlockCoordinate coordinate);
}
