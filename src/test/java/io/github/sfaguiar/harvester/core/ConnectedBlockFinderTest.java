package io.github.sfaguiar.harvester.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure unit tests for {@link ConnectedBlockFinder}. None of these start
 * Minecraft; {@link FakeWorldView} is an in-memory stand-in.
 *
 * <p>Air (block ID 0) is the default for any unset coordinate throughout;
 * the log/wood ID (17) is used as the "matching" ID for readability - it
 * carries no meaning inside {@code core}, which never interprets block
 * IDs semantically.
 *
 * <p>Every test here uses {@link BlockGroupView#byId} (ID-based membership)
 * and {@link OrthogonalSixNeighborhood} (six-orthogonal connectivity) via
 * {@link #discoverLogGroup}. {@code NeighborhoodPolicyTest} covers
 * {@link LegacyTwentySixNeighborhood} and policy-vs-algorithm independence
 * separately, since that is a property of the policy, not of this class.
 */
final class ConnectedBlockFinderTest {

    private static final int LOG = 17;
    private static final int AIR = 0;
    private static final int STONE = 1;

    private static HarvestPlan discoverLogGroup(HarvestRequest request, FakeWorldView world) {
        return ConnectedBlockFinder.discover(
                request,
                BlockGroupView.byId(world, BlockMatcher.ofId(LOG)),
                new OrthogonalSixNeighborhood()
        );
    }

    @Test
    void onlyOrigin_whenNoNeighborMatches() {
        FakeWorldView world = new FakeWorldView(AIR);
        HarvestRequest request = new HarvestRequest(
                new BlockCoordinate(0, 0, 0), LOG, true, 64
        );

        HarvestPlan plan = discoverLogGroup(request, world);

        assertEquals(List.of(new BlockCoordinate(0, 0, 0)), plan.includedBlocks());
        assertEquals(1, plan.totalIncluded());
        assertEquals(0, plan.additionalCandidateCount());
        assertFalse(plan.limitReached());
    }

    @Test
    void singleOrthogonalNeighbor_isIncluded() {
        FakeWorldView world = new FakeWorldView(AIR);
        world.set(1, 0, 0, LOG);
        HarvestRequest request = new HarvestRequest(
                new BlockCoordinate(0, 0, 0), LOG, true, 64
        );

        HarvestPlan plan = discoverLogGroup(request, world);

        assertEquals(2, plan.totalIncluded());
        assertTrue(plan.includedBlocks().contains(new BlockCoordinate(1, 0, 0)));
        assertFalse(plan.limitReached());
    }

    @Test
    void linearChain_isFullyDiscoveredInDeterministicOrder() {
        FakeWorldView world = new FakeWorldView(AIR);
        for (int x = 1; x <= 4; x++) {
            world.set(x, 0, 0, LOG);
        }
        HarvestRequest request = new HarvestRequest(
                new BlockCoordinate(0, 0, 0), LOG, true, 64
        );

        HarvestPlan plan = discoverLogGroup(request, world);

        assertEquals(
                List.of(
                        new BlockCoordinate(0, 0, 0),
                        new BlockCoordinate(1, 0, 0),
                        new BlockCoordinate(2, 0, 0),
                        new BlockCoordinate(3, 0, 0),
                        new BlockCoordinate(4, 0, 0)
                ),
                plan.includedBlocks()
        );
        assertFalse(plan.limitReached());
    }

    @Test
    void branchingStructure_includesBothBranches() {
        FakeWorldView world = new FakeWorldView(AIR);
        // Origin has two matching arms: +x and +z.
        world.set(1, 0, 0, LOG);
        world.set(2, 0, 0, LOG);
        world.set(0, 0, 1, LOG);
        world.set(0, 0, 2, LOG);
        HarvestRequest request = new HarvestRequest(
                new BlockCoordinate(0, 0, 0), LOG, true, 64
        );

        HarvestPlan plan = discoverLogGroup(request, world);

        assertEquals(5, plan.totalIncluded());
        assertTrue(plan.includedBlocks().containsAll(List.of(
                new BlockCoordinate(0, 0, 0),
                new BlockCoordinate(1, 0, 0),
                new BlockCoordinate(2, 0, 0),
                new BlockCoordinate(0, 0, 1),
                new BlockCoordinate(0, 0, 2)
        )));
        assertFalse(plan.limitReached());
    }

    @Test
    void cycle_isVisitedOnceWithoutDuplicatesOrInfiniteLoop() {
        FakeWorldView world = new FakeWorldView(AIR);
        // A 4-block ring in the XZ plane: (0,0,0)-(1,0,0)-(1,0,1)-(0,0,1)-back to origin.
        world.set(1, 0, 0, LOG);
        world.set(1, 0, 1, LOG);
        world.set(0, 0, 1, LOG);
        HarvestRequest request = new HarvestRequest(
                new BlockCoordinate(0, 0, 0), LOG, true, 64
        );

        HarvestPlan plan = discoverLogGroup(request, world);

        assertEquals(4, plan.totalIncluded());
        assertEquals(4, plan.includedBlocks().stream().distinct().count());
        assertFalse(plan.limitReached());
    }

    @Test
    void diagonalNeighbor_isExcluded() {
        FakeWorldView world = new FakeWorldView(AIR);
        // (1,0,1) is a face-diagonal of the origin - never a 6-orthogonal
        // neighbor - and there is no orthogonal path to it (its own
        // orthogonal neighbors (1,0,0) and (0,0,1) are left as air).
        world.set(1, 0, 1, LOG);
        HarvestRequest request = new HarvestRequest(
                new BlockCoordinate(0, 0, 0), LOG, true, 64
        );

        HarvestPlan plan = discoverLogGroup(request, world);

        assertEquals(1, plan.totalIncluded());
        assertFalse(plan.includedBlocks().contains(new BlockCoordinate(1, 0, 1)));
    }

    @Test
    void differentBlockId_isExcluded() {
        FakeWorldView world = new FakeWorldView(AIR);
        world.set(1, 0, 0, STONE);
        HarvestRequest request = new HarvestRequest(
                new BlockCoordinate(0, 0, 0), LOG, true, 64
        );

        HarvestPlan plan = discoverLogGroup(request, world);

        assertEquals(1, plan.totalIncluded());
    }

    @Test
    void sameBlockIdRepresentingDifferentSpecies_isIncluded() {
        // core.BlockMatcher operates on block ID only - it has no metadata
        // parameter at all, mirroring the confirmed legacy fact that
        // different log species sharing one block ID chain together
        // (better-beta-program/docs/knowledge/claims/CLM-0009.md). This
        // test documents that guarantee: a neighbor reporting the same ID
        // is included regardless of any conceptual "species" it might
        // represent, because the model has no way to distinguish them.
        FakeWorldView world = new FakeWorldView(AIR);
        world.set(1, 0, 0, LOG); // stands in for "a different species, same ID"
        HarvestRequest request = new HarvestRequest(
                new BlockCoordinate(0, 0, 0), LOG, true, 64
        );

        HarvestPlan plan = discoverLogGroup(request, world);

        assertEquals(2, plan.totalIncluded());
    }

    @Test
    void originMissingFromWorldView_isStillIncludedFromCapturedRequestData() {
        FakeWorldView world = new FakeWorldView(AIR);
        // Simulate the real integration scenario: by the time discovery
        // runs, vanilla has already turned the origin into air. The view
        // reports air at the origin position; ConnectedBlockFinder must
        // never re-query the origin and must include it purely from the
        // captured HarvestRequest data.
        world.set(0, 0, 0, AIR);
        world.set(1, 0, 0, LOG);
        HarvestRequest request = new HarvestRequest(
                new BlockCoordinate(0, 0, 0), LOG, true, 64
        );

        HarvestPlan plan = discoverLogGroup(request, world);

        assertTrue(plan.includedBlocks().contains(new BlockCoordinate(0, 0, 0)));
        assertTrue(plan.originAlreadyRemoved());
        assertEquals(2, plan.totalIncluded());
    }

    @Test
    void limitExactlyMatchesStructureSize_includesEverythingWithoutTruncation() {
        FakeWorldView world = new FakeWorldView(AIR);
        world.set(1, 0, 0, LOG);
        world.set(2, 0, 0, LOG);
        HarvestRequest request = new HarvestRequest(
                new BlockCoordinate(0, 0, 0), LOG, true, 3
        );

        HarvestPlan plan = discoverLogGroup(request, world);

        assertEquals(3, plan.totalIncluded());
        assertFalse(plan.limitReached());
    }

    @Test
    void limitSmallerThanStructure_truncatesAndReportsLimitReached() {
        FakeWorldView world = new FakeWorldView(AIR);
        world.set(1, 0, 0, LOG);
        world.set(2, 0, 0, LOG);
        world.set(3, 0, 0, LOG);
        HarvestRequest request = new HarvestRequest(
                new BlockCoordinate(0, 0, 0), LOG, true, 2
        );

        HarvestPlan plan = discoverLogGroup(request, world);

        assertEquals(2, plan.totalIncluded());
        assertTrue(plan.limitReached());
    }

    @Test
    void discoveryOrder_isDeterministicAcrossRepeatedRuns() {
        FakeWorldView world = new FakeWorldView(AIR);
        world.set(1, 0, 0, LOG);
        world.set(2, 0, 0, LOG);
        HarvestRequest request = new HarvestRequest(
                new BlockCoordinate(0, 0, 0), LOG, true, 64
        );

        HarvestPlan first = discoverLogGroup(request, world);
        HarvestPlan second = discoverLogGroup(request, world);

        assertEquals(first.includedBlocks(), second.includedBlocks());
    }

    @Test
    void negativeCoordinates_areHandledLikeAnyOther() {
        FakeWorldView world = new FakeWorldView(AIR);
        // (-6,-5,-5) differs from the origin by exactly 1 on a single axis,
        // making it a genuine orthogonal neighbor (unlike, e.g., (-6,-6,-6),
        // which would differ on all three axes and so not be reachable).
        world.set(-6, -5, -5, LOG);
        HarvestRequest request = new HarvestRequest(
                new BlockCoordinate(-5, -5, -5), LOG, true, 64
        );

        HarvestPlan plan = discoverLogGroup(request, world);

        assertEquals(2, plan.totalIncluded());
        assertTrue(plan.includedBlocks().contains(new BlockCoordinate(-6, -5, -5)));
    }

    @Test
    void verticalStructure_isDiscoveredAlongYAxis() {
        FakeWorldView world = new FakeWorldView(AIR);
        world.set(0, 1, 0, LOG);
        world.set(0, 2, 0, LOG);
        HarvestRequest request = new HarvestRequest(
                new BlockCoordinate(0, 0, 0), LOG, true, 64
        );

        HarvestPlan plan = discoverLogGroup(request, world);

        assertEquals(3, plan.totalIncluded());
    }

    @Test
    void longChainWithinLimit_completesIterativelyWithoutStackOverflow() {
        FakeWorldView world = new FakeWorldView(AIR);
        int chainLength = 5000;
        for (int x = 1; x < chainLength; x++) {
            world.set(x, 0, 0, LOG);
        }
        HarvestRequest request = new HarvestRequest(
                new BlockCoordinate(0, 0, 0), LOG, true, chainLength
        );

        HarvestPlan plan = discoverLogGroup(request, world);

        assertEquals(chainLength, plan.totalIncluded());
        assertFalse(plan.limitReached());
    }

    @Test
    void zeroLimit_isRejectedExplicitly() {
        // Local decision (not a legacy-derived contract, see
        // HarvestRequest's constructor comment): limit < 1 fails fast
        // rather than silently returning an empty or origin-only plan.
        assertThrows(IllegalArgumentException.class, () ->
                new HarvestRequest(new BlockCoordinate(0, 0, 0), LOG, true, 0)
        );
    }

    @Test
    void negativeLimit_isRejectedExplicitly() {
        assertThrows(IllegalArgumentException.class, () ->
                new HarvestRequest(new BlockCoordinate(0, 0, 0), LOG, true, -1)
        );
    }
}
