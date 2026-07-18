package io.github.sfaguiar.harvester.client;

import io.github.sfaguiar.harvester.core.BlockCoordinate;
import io.github.sfaguiar.harvester.core.BlockGroupView;
import io.github.sfaguiar.harvester.core.ConnectedBlockFinder;
import io.github.sfaguiar.harvester.core.HarvestPlan;
import io.github.sfaguiar.harvester.core.HarvestRequest;
import io.github.sfaguiar.harvester.core.LegacyTwentySixNeighborhood;
import io.github.sfaguiar.harvester.core.OrthogonalSixNeighborhood;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure unit tests for {@link DefaultHarvestNeighborhoodPolicy} — the
 * production default {@code NeighborhoodPolicy}
 * {@link SingleplayerHarvestDiscoveryAdapter} wires in. None of these
 * start Minecraft; {@link DefaultHarvestNeighborhoodPolicy} depends on
 * nothing beyond {@code core}, so referencing it here carries none of the
 * risk {@link SingleplayerHarvestDiscoveryAdapter} itself would (see that
 * class's javadoc) of depending on Mixin-added members the plain
 * {@code test} task never applies.
 *
 * <p>Confirms the production decision recorded in
 * {@code better-beta-program/docs/operations/OPEN_QUESTIONS.md} Q0011
 * ({@code DECIDED}): {@link LegacyTwentySixNeighborhood} is the default;
 * {@link OrthogonalSixNeighborhood} remains implemented, tested, and
 * available, just not wired in as the default. Properties shared by
 * {@code ConnectedBlockFinder} regardless of which policy is used
 * (origin-never-duplicated, limit still counts the origin,
 * {@code limitReached} contract, deterministic order, BFS independence
 * from the concrete policy) are already covered generically for both
 * policies by {@code core.ConnectedBlockFinderTest} and
 * {@code core.NeighborhoodPolicyTest}, which this class does not
 * duplicate; the tests below instead close the loop specifically for the
 * exact constant {@code SingleplayerHarvestDiscoveryAdapter} uses in
 * production.
 */
final class DefaultHarvestNeighborhoodPolicyTest {

    private static BlockGroupView members(BlockCoordinate... coordinates) {
        Set<BlockCoordinate> set = new HashSet<>(Arrays.asList(coordinates));
        return set::contains;
    }

    @Test
    void adapterDefaultPolicy_isLegacyTwentySixNeighborhood() {
        assertInstanceOf(LegacyTwentySixNeighborhood.class, DefaultHarvestNeighborhoodPolicy.INSTANCE);
    }

    @Test
    void orthogonalSixNeighborhood_remainsAvailableAndFunctional() {
        // The non-default policy was not removed - it is still fully
        // wired and usable through the same ConnectedBlockFinder.discover
        // entry point, only no longer what the adapter selects.
        BlockCoordinate origin = new BlockCoordinate(0, 0, 0);
        BlockCoordinate faceNeighbor = new BlockCoordinate(1, 0, 0);
        HarvestRequest request = new HarvestRequest(origin, 17, true, 64);

        HarvestPlan plan = ConnectedBlockFinder.discover(
                request, members(origin, faceNeighbor), new OrthogonalSixNeighborhood()
        );

        assertEquals(2, plan.totalIncluded());
        assertTrue(plan.includedBlocks().contains(faceNeighbor));
    }

    @Test
    void adapterDefaultPolicy_includesDiagonalOnlyConnection() {
        // (1,0,1) differs from the origin by exactly 1 on two axes (x and
        // z) - an edge diagonal, unreachable under six-orthogonal
        // connectivity but a direct neighbor under the confirmed default.
        BlockCoordinate origin = new BlockCoordinate(0, 0, 0);
        BlockCoordinate edgeDiagonal = new BlockCoordinate(1, 0, 1);
        HarvestRequest request = new HarvestRequest(origin, 17, true, 64);

        HarvestPlan plan = ConnectedBlockFinder.discover(
                request, members(origin, edgeDiagonal), DefaultHarvestNeighborhoodPolicy.INSTANCE
        );

        assertEquals(2, plan.totalIncluded());
        assertTrue(plan.includedBlocks().contains(edgeDiagonal));
    }

    @Test
    void adapterDefaultPolicy_includesVertexOnlyConnection() {
        // (1,1,1) differs from the origin by exactly 1 on all three axes -
        // a vertex (corner) diagonal.
        BlockCoordinate origin = new BlockCoordinate(0, 0, 0);
        BlockCoordinate vertexDiagonal = new BlockCoordinate(1, 1, 1);
        HarvestRequest request = new HarvestRequest(origin, 17, true, 64);

        HarvestPlan plan = ConnectedBlockFinder.discover(
                request, members(origin, vertexDiagonal), DefaultHarvestNeighborhoodPolicy.INSTANCE
        );

        assertEquals(2, plan.totalIncluded());
        assertTrue(plan.includedBlocks().contains(vertexDiagonal));
    }

    @Test
    void adapterDefaultPolicy_discoversTwoByTwoTrunkAsSingleStructure() {
        // The four corners of a 2x2 square in the XZ plane, discovered as
        // one structure under the confirmed default policy.
        BlockCoordinate origin = new BlockCoordinate(0, 0, 0);
        BlockCoordinate a = new BlockCoordinate(1, 0, 0);
        BlockCoordinate b = new BlockCoordinate(1, 0, 1);
        BlockCoordinate c = new BlockCoordinate(0, 0, 1);
        HarvestRequest request = new HarvestRequest(origin, 17, true, 64);

        HarvestPlan plan = ConnectedBlockFinder.discover(
                request, members(origin, a, b, c), DefaultHarvestNeighborhoodPolicy.INSTANCE
        );

        assertEquals(4, plan.totalIncluded());
    }

    @Test
    void adapterDefaultPolicy_originNeverDuplicated() {
        BlockCoordinate origin = new BlockCoordinate(0, 0, 0);
        BlockCoordinate neighbor = new BlockCoordinate(1, 1, 1);
        HarvestRequest request = new HarvestRequest(origin, 17, true, 64);

        HarvestPlan plan = ConnectedBlockFinder.discover(
                request, members(origin, neighbor), DefaultHarvestNeighborhoodPolicy.INSTANCE
        );

        assertEquals(1, plan.includedBlocks().stream().filter(origin::equals).count());
    }

    @Test
    void adapterDefaultPolicy_limitStillCountsOrigin() {
        BlockCoordinate origin = new BlockCoordinate(0, 0, 0);
        BlockCoordinate neighbor = new BlockCoordinate(1, 1, 1);
        HarvestRequest request = new HarvestRequest(origin, 17, true, 1);

        HarvestPlan plan = ConnectedBlockFinder.discover(
                request, members(origin, neighbor), DefaultHarvestNeighborhoodPolicy.INSTANCE
        );

        assertEquals(1, plan.totalIncluded());
        assertTrue(plan.limitReached());
    }

    @Test
    void adapterDefaultPolicy_planOrderIsDeterministicAcrossRepeatedRuns() {
        BlockCoordinate origin = new BlockCoordinate(0, 0, 0);
        BlockCoordinate a = new BlockCoordinate(1, 1, 1);
        BlockCoordinate b = new BlockCoordinate(-1, -1, -1);
        HarvestRequest request = new HarvestRequest(origin, 17, true, 64);
        BlockGroupView groupView = members(origin, a, b);

        HarvestPlan first = ConnectedBlockFinder.discover(
                request, groupView, DefaultHarvestNeighborhoodPolicy.INSTANCE
        );
        HarvestPlan second = ConnectedBlockFinder.discover(
                request, groupView, DefaultHarvestNeighborhoodPolicy.INSTANCE
        );

        assertEquals(first.includedBlocks(), second.includedBlocks());
    }
}
