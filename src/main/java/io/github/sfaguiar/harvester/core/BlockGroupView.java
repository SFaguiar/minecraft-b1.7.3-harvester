package io.github.sfaguiar.harvester.core;

/**
 * Decides whether a coordinate belongs to the group {@link ConnectedBlockFinder}
 * is discovering. This is the single membership test the traversal
 * actually consumes; {@code core} deliberately stays agnostic about
 * <em>how</em> membership is decided.
 *
 * <p>Two known ways to build one:
 * <ul>
 *   <li>{@link #byId(BlockWorldView, BlockMatcher)} — membership by raw
 *   block ID, used throughout {@code ConnectedBlockFinderTest} with an
 *   in-memory {@link BlockWorldView}.</li>
 *   <li>a platform adapter's own lambda — e.g.
 *   {@code SingleplayerHarvestDiscoveryAdapter} builds one that queries a
 *   real {@code net.minecraft.world.World} for a StationAPI
 *   {@code BlockState} at each coordinate and checks
 *   {@code BlockState.isIn(BlockTags.LOGS)}. That StationAPI-specific logic
 *   lives entirely in the {@code client} package, never here — {@code core}
 *   still imports nothing beyond the JDK.</li>
 * </ul>
 */
@FunctionalInterface
public interface BlockGroupView {

    boolean isMember(BlockCoordinate coordinate);

    /**
     * Composes a {@link BlockWorldView} (read a block ID at a coordinate)
     * with a {@link BlockMatcher} (decide if an ID belongs to the group)
     * into a single membership test. This is the ID-based construction
     * path every existing {@code ConnectedBlockFinderTest} case uses.
     */
    static BlockGroupView byId(BlockWorldView view, BlockMatcher matcher) {
        return coordinate -> matcher.matches(view.getBlockId(coordinate));
    }

    /**
     * Composes a {@link BlockDescriptorView} (read a {@link BlockDescriptor}
     * at a coordinate) with an already-resolved {@link HarvestGroup} (decide
     * if a descriptor belongs to that group) into a single membership test.
     * The generalized, tag/identity-aware counterpart of {@link #byId}, used
     * by {@code SingleplayerHarvestDiscoveryAdapter} for logs and ores alike.
     */
    static BlockGroupView byDescriptor(BlockDescriptorView view, HarvestGroup group) {
        return coordinate -> group.matches(view.describe(coordinate));
    }
}
