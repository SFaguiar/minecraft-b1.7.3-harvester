package io.github.sfaguiar.harvester.server.multiplayer;

import io.github.sfaguiar.harvester.core.BlockCoordinate;
import io.github.sfaguiar.harvester.core.BlockGroupView;
import io.github.sfaguiar.harvester.core.BlockMatcher;
import io.github.sfaguiar.harvester.core.BlockWorldView;
import io.github.sfaguiar.harvester.core.ConnectedBlockFinder;
import io.github.sfaguiar.harvester.core.HarvestGroup;
import io.github.sfaguiar.harvester.core.HarvestPlan;
import io.github.sfaguiar.harvester.core.HarvestRequest;
import io.github.sfaguiar.harvester.core.OrthogonalSixNeighborhood;
import io.github.sfaguiar.harvester.game.HarvestChainOutcome;
import io.github.sfaguiar.harvester.game.HarvesterHeldItemSnapshot;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure unit tests for {@link ServerHarvestExecutor#runChain} — the
 * side-agnostic loop behind the public, real-typed {@link
 * ServerHarvestExecutor#executeChain}. Every Minecraft/StationAPI-touching
 * decision is a fake here; none of these start Minecraft. {@link
 * HarvestPlan} fixtures are built through the real, already-tested {@link
 * ConnectedBlockFinder} (same technique as {@code ConnectedBlockFinderTest})
 * rather than duplicating its BFS.
 */
final class ServerHarvestExecutorTest {

    private static final int LOG_ID = 17;
    private static final HarvesterHeldItemSnapshot ABSENT = HarvesterHeldItemSnapshot.capture(null);

    /** A straight line of {@code matchingCount} matching blocks starting at (1,0,0), origin at (0,0,0). */
    private static HarvestPlan linearPlan(int limit, int matchingCount) {
        Map<BlockCoordinate, Integer> world = new HashMap<>();
        for (int i = 1; i <= matchingCount; i++) {
            world.put(new BlockCoordinate(i, 0, 0), LOG_ID);
        }
        BlockWorldView view = coordinate -> world.getOrDefault(coordinate, 0);
        HarvestRequest request = new HarvestRequest(new BlockCoordinate(0, 0, 0), LOG_ID, true, limit);
        return ConnectedBlockFinder.discover(
                request, BlockGroupView.byId(view, BlockMatcher.ofId(LOG_ID)), new OrthogonalSixNeighborhood()
        );
    }

    private static List<BlockCoordinate> additionalCandidatesOf(HarvestPlan plan) {
        List<BlockCoordinate> result = new ArrayList<>(plan.includedBlocks());
        result.remove(plan.origin());
        return result;
    }

    @Test
    void disabled_skipsWithoutAttemptingAnyBreak() {
        HarvestPlan plan = linearPlan(64, 3);
        List<BlockCoordinate> attempted = new ArrayList<>();

        HarvestChainOutcome outcome = ServerHarvestExecutor.runChain(
                false, () -> true, () -> true, c -> true, false, c -> true,
                () -> ABSENT, c -> { attempted.add(c); return true; },
                plan, ABSENT, ABSENT, "player", HarvestGroup.logs()
        );

        assertEquals(HarvestChainOutcome.SKIPPED_DISABLED, outcome);
        assertTrue(attempted.isEmpty());
    }

    @Test
    void inactiveAtStart_skipsWithoutAttemptingAnyBreak_onlyOriginBreakStands() {
        HarvestPlan plan = linearPlan(64, 3);
        List<BlockCoordinate> attempted = new ArrayList<>();

        HarvestChainOutcome outcome = ServerHarvestExecutor.runChain(
                true, () -> false, () -> true, c -> true, false, c -> true,
                () -> ABSENT, c -> { attempted.add(c); return true; },
                plan, ABSENT, ABSENT, "player", HarvestGroup.logs()
        );

        assertEquals(HarvestChainOutcome.SKIPPED_INACTIVE, outcome);
        assertTrue(attempted.isEmpty(), "an inactive player must never get an additional break beyond the origin");
    }

    @Test
    void environmentInvalidAtStart_skipsWithoutAttemptingAnyBreak() {
        HarvestPlan plan = linearPlan(64, 3);
        List<BlockCoordinate> attempted = new ArrayList<>();

        HarvestChainOutcome outcome = ServerHarvestExecutor.runChain(
                true, () -> true, () -> false, c -> true, false, c -> true,
                () -> ABSENT, c -> { attempted.add(c); return true; },
                plan, ABSENT, ABSENT, "player", HarvestGroup.logs()
        );

        assertEquals(HarvestChainOutcome.SKIPPED_INACTIVE, outcome);
        assertTrue(attempted.isEmpty());
    }

    @Test
    void originToolChangedBeforeStart_skipsWithoutAttemptingAnyBreak() {
        HarvestPlan plan = linearPlan(64, 3);
        List<BlockCoordinate> attempted = new ArrayList<>();
        HarvesterHeldItemSnapshot before = HarvesterHeldItemSnapshot.presentForTesting(1);
        HarvesterHeldItemSnapshot after = ABSENT;

        HarvestChainOutcome outcome = ServerHarvestExecutor.runChain(
                true, () -> true, () -> true, c -> true, false, c -> true,
                () -> ABSENT, c -> { attempted.add(c); return true; },
                plan, before, after, "player", HarvestGroup.logs()
        );

        assertEquals(HarvestChainOutcome.ORIGIN_TOOL_CHANGED_BEFORE_CHAIN_START, outcome);
        assertTrue(attempted.isEmpty());
    }

    @Test
    void noAdditionalCandidate_reportsNoAdditionalCandidate() {
        HarvestPlan plan = linearPlan(64, 0);
        List<BlockCoordinate> attempted = new ArrayList<>();

        HarvestChainOutcome outcome = ServerHarvestExecutor.runChain(
                true, () -> true, () -> true, c -> true, false, c -> true,
                () -> ABSENT, c -> { attempted.add(c); return true; },
                plan, ABSENT, ABSENT, "player", HarvestGroup.logs()
        );

        assertEquals(HarvestChainOutcome.NO_ADDITIONAL_CANDIDATE, outcome);
        assertTrue(attempted.isEmpty());
    }

    @Test
    void active_allCandidatesValid_chainCompletesInDeterministicOrder() {
        HarvestPlan plan = linearPlan(64, 4);
        List<BlockCoordinate> attempted = new ArrayList<>();

        HarvestChainOutcome outcome = ServerHarvestExecutor.runChain(
                true, () -> true, () -> true, c -> true, false, c -> true,
                () -> ABSENT, c -> { attempted.add(c); return true; },
                plan, ABSENT, ABSENT, "player", HarvestGroup.logs()
        );

        assertEquals(HarvestChainOutcome.CHAIN_COMPLETED, outcome);
        assertEquals(additionalCandidatesOf(plan), attempted, "candidates must be attempted in the plan's own deterministic order");
    }

    @Test
    void maxChainLimit_onlyPlanCandidatesAreAttempted_neverMore() {
        // 10 matching blocks are physically available in the world, but the plan's own limit caps it at 4.
        HarvestPlan plan = linearPlan(4, 10);
        assertTrue(plan.limitReached());
        List<BlockCoordinate> attempted = new ArrayList<>();

        HarvestChainOutcome outcome = ServerHarvestExecutor.runChain(
                true, () -> true, () -> true, c -> true, false, c -> true,
                () -> ABSENT, c -> { attempted.add(c); return true; },
                plan, ABSENT, ABSENT, "player", HarvestGroup.logs()
        );

        assertEquals(HarvestChainOutcome.CHAIN_COMPLETED, outcome);
        assertEquals(plan.additionalCandidateCount(), attempted.size());
        assertEquals(additionalCandidatesOf(plan), attempted);
    }

    @Test
    void candidateNoLongerMember_stopsWithCandidateInvalid() {
        HarvestPlan plan = linearPlan(64, 3);
        List<BlockCoordinate> candidates = additionalCandidatesOf(plan);
        BlockCoordinate invalidAt = candidates.get(1);
        List<BlockCoordinate> attempted = new ArrayList<>();

        HarvestChainOutcome outcome = ServerHarvestExecutor.runChain(
                true, () -> true, () -> true, c -> !c.equals(invalidAt), false, c -> true,
                () -> ABSENT, c -> { attempted.add(c); return true; },
                plan, ABSENT, ABSENT, "player", HarvestGroup.logs()
        );

        assertEquals(HarvestChainOutcome.STOPPED_CANDIDATE_INVALID, outcome);
        assertEquals(List.of(candidates.get(0)), attempted, "only the still-valid candidate before the changed one is broken");
    }

    @Test
    void toolBecomesUnsuitable_stopsWithToolUnsuitable() {
        HarvestPlan plan = linearPlan(64, 3);
        List<BlockCoordinate> candidates = additionalCandidatesOf(plan);
        BlockCoordinate unsuitableAt = candidates.get(1);
        List<BlockCoordinate> attempted = new ArrayList<>();

        HarvestChainOutcome outcome = ServerHarvestExecutor.runChain(
                true, () -> true, () -> true, c -> true, true, c -> !c.equals(unsuitableAt),
                () -> ABSENT, c -> { attempted.add(c); return true; },
                plan, ABSENT, ABSENT, "player", HarvestGroup.oreIdentityFallback("test:ore")
        );

        assertEquals(HarvestChainOutcome.STOPPED_TOOL_UNSUITABLE, outcome);
        assertEquals(List.of(candidates.get(0)), attempted);
    }

    @Test
    void toolCheckNotRequired_neverConsultedEvenIfItWouldRejectEverything() {
        HarvestPlan plan = linearPlan(64, 3);
        List<BlockCoordinate> attempted = new ArrayList<>();

        HarvestChainOutcome outcome = ServerHarvestExecutor.runChain(
                true, () -> true, () -> true, c -> true, false, c -> false,
                () -> ABSENT, c -> { attempted.add(c); return true; },
                plan, ABSENT, ABSENT, "player", HarvestGroup.logs()
        );

        assertEquals(HarvestChainOutcome.CHAIN_COMPLETED, outcome, "a log chain never gates on tool suitability");
        assertEquals(additionalCandidatesOf(plan), attempted);
    }

    @Test
    void breakRejected_stopsWithBreakRejected() {
        HarvestPlan plan = linearPlan(64, 3);
        List<BlockCoordinate> candidates = additionalCandidatesOf(plan);
        BlockCoordinate rejectAt = candidates.get(1);
        List<BlockCoordinate> attempted = new ArrayList<>();

        HarvestChainOutcome outcome = ServerHarvestExecutor.runChain(
                true, () -> true, () -> true, c -> true, false, c -> true,
                () -> ABSENT, c -> { attempted.add(c); return !c.equals(rejectAt); },
                plan, ABSENT, ABSENT, "player", HarvestGroup.logs()
        );

        assertEquals(HarvestChainOutcome.STOPPED_BREAK_REJECTED, outcome);
        assertEquals(candidates.subList(0, 2), attempted);
    }

    @Test
    void toolIdentityChanges_immediatelyAfterASuccessfulBreak_stopsWithToolChanged() {
        HarvestPlan plan = linearPlan(64, 3);
        List<BlockCoordinate> candidates = additionalCandidatesOf(plan);
        List<BlockCoordinate> attempted = new ArrayList<>();
        HarvesterHeldItemSnapshot toolA = HarvesterHeldItemSnapshot.presentForTesting(1);
        HarvesterHeldItemSnapshot toolB = HarvesterHeldItemSnapshot.presentForTesting(2);
        int[] calls = {0};
        // Candidate 0: before=toolA (call 1), after=toolA (call 2) -> unchanged, continue.
        // Candidate 1: before=toolA (call 3), after=toolB (call 4) -> changed, stop.
        java.util.function.Supplier<HarvesterHeldItemSnapshot> currentHeldItem = () -> {
            calls[0]++;
            return calls[0] <= 3 ? toolA : toolB;
        };

        HarvestChainOutcome outcome = ServerHarvestExecutor.runChain(
                true, () -> true, () -> true, c -> true, false, c -> true,
                currentHeldItem, c -> { attempted.add(c); return true; },
                plan, ABSENT, ABSENT, "player", HarvestGroup.logs()
        );

        assertEquals(HarvestChainOutcome.STOPPED_TOOL_CHANGED, outcome);
        assertEquals(candidates.subList(0, 2), attempted, "the break that revealed the tool change still counts as broken");
    }

    @Test
    void deactivatedMidChain_stopsWithStoppedDeactivated() {
        HarvestPlan plan = linearPlan(64, 3);
        List<BlockCoordinate> candidates = additionalCandidatesOf(plan);
        List<BlockCoordinate> attempted = new ArrayList<>();
        int[] activeChecks = {0};
        // Call 1: the upfront pre-check before the loop even starts. Call 2:
        // the loop's own first-iteration check (candidate 0). Call 3: the
        // second-iteration check (candidate 1) - this one goes false.
        HarvestChainOutcome outcome = ServerHarvestExecutor.runChain(
                true, () -> { activeChecks[0]++; return activeChecks[0] <= 2; }, () -> true, c -> true, false, c -> true,
                () -> ABSENT, c -> { attempted.add(c); return true; },
                plan, ABSENT, ABSENT, "player", HarvestGroup.logs()
        );

        assertEquals(HarvestChainOutcome.STOPPED_DEACTIVATED, outcome);
        assertEquals(List.of(candidates.get(0)), attempted);
    }

    @Test
    void environmentBecomesInvalidMidChain_stopsWithEnvironmentInvalid() {
        HarvestPlan plan = linearPlan(64, 3);
        List<BlockCoordinate> candidates = additionalCandidatesOf(plan);
        List<BlockCoordinate> attempted = new ArrayList<>();
        int[] envChecks = {0};
        // Same call-counting reasoning as deactivatedMidChain above: call 1
        // is the upfront pre-check, call 2 is the loop's first iteration.
        HarvestChainOutcome outcome = ServerHarvestExecutor.runChain(
                true, () -> true, () -> { envChecks[0]++; return envChecks[0] <= 2; }, c -> true, false, c -> true,
                () -> ABSENT, c -> { attempted.add(c); return true; },
                plan, ABSENT, ABSENT, "player", HarvestGroup.logs()
        );

        assertEquals(HarvestChainOutcome.STOPPED_ENVIRONMENT_INVALID, outcome);
        assertEquals(List.of(candidates.get(0)), attempted);
    }

    @Test
    void independentInvocations_neverShareState() {
        HarvestPlan plan = linearPlan(64, 2);

        HarvestChainOutcome inactiveOutcome = ServerHarvestExecutor.runChain(
                true, () -> false, () -> true, c -> true, false, c -> true,
                () -> ABSENT, c -> true,
                plan, ABSENT, ABSENT, "playerA", HarvestGroup.logs()
        );
        HarvestChainOutcome activeOutcome = ServerHarvestExecutor.runChain(
                true, () -> true, () -> true, c -> true, false, c -> true,
                () -> ABSENT, c -> true,
                plan, ABSENT, ABSENT, "playerB", HarvestGroup.logs()
        );

        assertEquals(HarvestChainOutcome.SKIPPED_INACTIVE, inactiveOutcome, "player A's inactivity must not leak into player B's run");
        assertEquals(HarvestChainOutcome.CHAIN_COMPLETED, activeOutcome, "player B must complete independently of player A's outcome");
    }
}
