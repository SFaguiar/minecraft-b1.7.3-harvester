package io.github.sfaguiar.harvester.core;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure unit tests for {@link NeighborhoodPolicy} implementations, and for
 * {@link ConnectedBlockFinder}'s independence from which policy it is
 * given. None of these start Minecraft.
 *
 * <p>{@code better-beta-program/docs/operations/OPEN_QUESTIONS.md} Q0011
 * remains open on which policy the 2.x port should adopt by default;
 * these tests only establish that both policies behave as specified and
 * that {@link ConnectedBlockFinder} treats the policy as a genuine
 * dependency (swapping it changes results, with no other code change
 * required) — they do not decide Q0011.
 */
final class NeighborhoodPolicyTest {

    private static final int LOG = 17;
    private static final int AIR = 0;

    private static List<BlockCoordinate> neighborsOf(NeighborhoodPolicy policy, BlockCoordinate origin) {
        return List.copyOf(toList(policy.neighborsOf(origin)));
    }

    private static List<BlockCoordinate> toList(Iterable<BlockCoordinate> iterable) {
        List<BlockCoordinate> list = new ArrayList<>();
        iterable.forEach(list::add);
        return list;
    }

    private static HarvestPlan discover(
            HarvestRequest request, FakeWorldView world, NeighborhoodPolicy policy
    ) {
        return ConnectedBlockFinder.discover(
                request, BlockGroupView.byId(world, BlockMatcher.ofId(LOG)), policy
        );
    }

    // --- OrthogonalSixNeighborhood: neighbor generation ---

    @Test
    void orthogonalSixNeighborhood_returnsExactlySixFaceNeighbors() {
        BlockCoordinate origin = new BlockCoordinate(0, 0, 0);
        List<BlockCoordinate> neighbors = neighborsOf(new OrthogonalSixNeighborhood(), origin);

        assertEquals(6, neighbors.size());
        assertEquals(origin.orthogonalNeighbors(), neighbors);
    }

    @Test
    void orthogonalSixNeighborhood_orderIsDeterministicAcrossRepeatedCalls() {
        BlockCoordinate origin = new BlockCoordinate(3, -2, 7);
        NeighborhoodPolicy policy = new OrthogonalSixNeighborhood();

        assertEquals(neighborsOf(policy, origin), neighborsOf(policy, origin));
    }

    // --- LegacyTwentySixNeighborhood: neighbor generation ---

    @Test
    void legacyTwentySixNeighborhood_returnsExactly26NeighborsExcludingOrigin() {
        BlockCoordinate origin = new BlockCoordinate(0, 0, 0);
        List<BlockCoordinate> neighbors = neighborsOf(new LegacyTwentySixNeighborhood(), origin);

        assertEquals(26, neighbors.size());
        assertEquals(26, neighbors.stream().distinct().count());
        assertFalse(neighbors.contains(origin));
        for (BlockCoordinate neighbor : neighbors) {
            int dx = Math.abs(neighbor.x() - origin.x());
            int dy = Math.abs(neighbor.y() - origin.y());
            int dz = Math.abs(neighbor.z() - origin.z());
            assertTrue(dx <= 1 && dy <= 1 && dz <= 1, "neighbor must be within 1 on every axis: " + neighbor);
            assertTrue(dx + dy + dz >= 1, "neighbor must differ from origin on at least one axis: " + neighbor);
        }
    }

    @Test
    void legacyTwentySixNeighborhood_orderIsDeterministicAcrossRepeatedCalls() {
        BlockCoordinate origin = new BlockCoordinate(-4, 10, 1);
        NeighborhoodPolicy policy = new LegacyTwentySixNeighborhood();

        assertEquals(neighborsOf(policy, origin), neighborsOf(policy, origin));
    }

    // --- Face / edge / vertex connectivity, both policies ---

    @Test
    void faceConnection_isReachableUnderBothPolicies() {
        // (1,0,0) differs from the origin by exactly 1 on a single axis - a
        // face connection, the only kind six-orthogonal connectivity
        // recognizes at all.
        FakeWorldView world = new FakeWorldView(AIR);
        world.set(1, 0, 0, LOG);
        HarvestRequest request = new HarvestRequest(new BlockCoordinate(0, 0, 0), LOG, true, 64);

        HarvestPlan sixNeighborPlan = discover(request, world, new OrthogonalSixNeighborhood());
        HarvestPlan legacyPlan = discover(request, world, new LegacyTwentySixNeighborhood());

        assertEquals(2, sixNeighborPlan.totalIncluded());
        assertEquals(2, legacyPlan.totalIncluded());
    }

    @Test
    void edgeConnection_isReachableOnlyUnderLegacyTwentySixNeighborhood() {
        // (1,0,1) differs from the origin by exactly 1 on two axes (x and
        // z) - an edge diagonal - with no intermediate matching block at
        // (1,0,0) or (0,0,1) to bridge it under six-orthogonal
        // connectivity.
        FakeWorldView world = new FakeWorldView(AIR);
        world.set(1, 0, 1, LOG);
        HarvestRequest request = new HarvestRequest(new BlockCoordinate(0, 0, 0), LOG, true, 64);

        HarvestPlan sixNeighborPlan = discover(request, world, new OrthogonalSixNeighborhood());
        HarvestPlan legacyPlan = discover(request, world, new LegacyTwentySixNeighborhood());

        assertEquals(1, sixNeighborPlan.totalIncluded(), "six-neighbor connectivity must not reach an edge diagonal");
        assertEquals(2, legacyPlan.totalIncluded(), "26-neighbor connectivity must reach an edge diagonal directly");
    }

    @Test
    void vertexConnection_isReachableOnlyUnderLegacyTwentySixNeighborhood() {
        // (1,1,1) differs from the origin by exactly 1 on all three axes -
        // a vertex (corner) diagonal.
        FakeWorldView world = new FakeWorldView(AIR);
        world.set(1, 1, 1, LOG);
        HarvestRequest request = new HarvestRequest(new BlockCoordinate(0, 0, 0), LOG, true, 64);

        HarvestPlan sixNeighborPlan = discover(request, world, new OrthogonalSixNeighborhood());
        HarvestPlan legacyPlan = discover(request, world, new LegacyTwentySixNeighborhood());

        assertEquals(1, sixNeighborPlan.totalIncluded(), "six-neighbor connectivity must not reach a vertex diagonal");
        assertEquals(2, legacyPlan.totalIncluded(), "26-neighbor connectivity must reach a vertex diagonal directly");
    }

    @Test
    void fullTwoByTwoTrunk_isFullyConnectedUnderBothPolicies() {
        // The four corners of a 2x2 square in the XZ plane are already
        // pairwise face-adjacent in a ring - (0,0,0)-(1,0,0)-(1,0,1)-(0,0,1)
        // - so a properly filled 2x2 trunk needs no diagonal connectivity
        // at all under either policy. This intentionally documents that
        // fact rather than assuming a 2x2 trunk requires 26-neighbor
        // connectivity; see twoDiagonallyAdjacentSingleBlockStructures_...
        // below for the case that actually distinguishes the two policies.
        FakeWorldView world = new FakeWorldView(AIR);
        world.set(1, 0, 0, LOG);
        world.set(1, 0, 1, LOG);
        world.set(0, 0, 1, LOG);
        HarvestRequest request = new HarvestRequest(new BlockCoordinate(0, 0, 0), LOG, true, 64);

        HarvestPlan sixNeighborPlan = discover(request, world, new OrthogonalSixNeighborhood());
        HarvestPlan legacyPlan = discover(request, world, new LegacyTwentySixNeighborhood());

        assertEquals(4, sixNeighborPlan.totalIncluded());
        assertEquals(4, legacyPlan.totalIncluded());
    }

    @Test
    void twoDiagonallyAdjacentSingleBlockStructures_mergeOnlyUnderLegacyTwentySixNeighborhood() {
        // Two otherwise-isolated single-block "trunks" whose only spatial
        // relationship is a shared vertex - (0,0,0) and (1,1,1) - with
        // nothing at any of the intermediate face/edge positions. Under
        // six-orthogonal connectivity these are two separate structures;
        // under 26-neighbor connectivity they merge into one discovered
        // plan. This is the risk Q0011 flags for adopting 26-neighbor
        // parity: structures a player would reasonably consider distinct
        // could merge.
        FakeWorldView world = new FakeWorldView(AIR);
        world.set(1, 1, 1, LOG);
        HarvestRequest request = new HarvestRequest(new BlockCoordinate(0, 0, 0), LOG, true, 64);

        HarvestPlan sixNeighborPlan = discover(request, world, new OrthogonalSixNeighborhood());
        HarvestPlan legacyPlan = discover(request, world, new LegacyTwentySixNeighborhood());

        assertEquals(1, sixNeighborPlan.totalIncluded(), "the two structures must stay separate under six-neighbor connectivity");
        assertEquals(2, legacyPlan.totalIncluded(), "the two structures must merge under 26-neighbor connectivity");
    }

    @Test
    void bfsAlgorithm_isIndependentOfPolicy_swappingPolicyIsTheOnlyDifference() {
        // Same request, same world, same ConnectedBlockFinder.discover
        // call shape - only the NeighborhoodPolicy argument changes.
        // ConnectedBlockFinder itself contains no six-vs-26 branching; the
        // differing result below comes entirely from the policy
        // implementations, confirmed already for individual scenarios
        // above. This test exists specifically to assert that swapping the
        // dependency, with no other code path change, is sufficient to
        // change behavior - i.e. the policy is a real dependency, not a
        // vestigial parameter.
        FakeWorldView world = new FakeWorldView(AIR);
        world.set(1, 0, 1, LOG); // edge diagonal, per edgeConnection_... above
        HarvestRequest request = new HarvestRequest(new BlockCoordinate(0, 0, 0), LOG, true, 64);

        HarvestPlan withSixNeighborPolicy = discover(request, world, new OrthogonalSixNeighborhood());
        HarvestPlan withLegacyPolicy = discover(request, world, new LegacyTwentySixNeighborhood());

        assertNotEquals(withSixNeighborPolicy.totalIncluded(), withLegacyPolicy.totalIncluded());
    }

    @Test
    void legacyTwentySixNeighborhood_visitsEachNeighborExactlyOnceDuringTraversal() {
        // A structure with matches on several diagonal offsets around the
        // origin, verifying the BFS visited-set logic (shared with the
        // six-neighbor tests in ConnectedBlockFinderTest) also holds under
        // the wider policy - no duplicate visits, no infinite loop.
        FakeWorldView world = new FakeWorldView(AIR);
        world.set(1, 1, 1, LOG);
        world.set(-1, -1, -1, LOG);
        world.set(1, -1, 0, LOG);
        HarvestRequest request = new HarvestRequest(new BlockCoordinate(0, 0, 0), LOG, true, 64);

        HarvestPlan plan = discover(request, world, new LegacyTwentySixNeighborhood());

        assertEquals(4, plan.totalIncluded());
        assertEquals(4, plan.includedBlocks().stream().distinct().count());
        assertFalse(plan.limitReached());
    }

    @Test
    void deterministicSetOfNeighborsIsIdenticalRegardlessOfCallCount() {
        // Confirms determinism holds not just call-to-call but also as a
        // set (not merely list equality, which the two order-focused tests
        // above already check) - guards against a hypothetical future
        // implementation that returns an equal-as-list-but-different-as-
        // multiset result due to some non-deterministic tie-break.
        BlockCoordinate origin = new BlockCoordinate(2, 2, 2);
        NeighborhoodPolicy policy = new LegacyTwentySixNeighborhood();

        Set<BlockCoordinate> first = new HashSet<>(neighborsOf(policy, origin));
        Set<BlockCoordinate> second = new HashSet<>(neighborsOf(policy, origin));

        assertEquals(first, second);
    }
}
