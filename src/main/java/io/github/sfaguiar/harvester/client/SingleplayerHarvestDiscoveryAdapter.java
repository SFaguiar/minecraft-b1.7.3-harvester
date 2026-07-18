package io.github.sfaguiar.harvester.client;

import io.github.sfaguiar.harvester.core.BlockCoordinate;
import io.github.sfaguiar.harvester.core.BlockGroupView;
import io.github.sfaguiar.harvester.core.ConnectedBlockFinder;
import io.github.sfaguiar.harvester.core.HarvestPlan;
import io.github.sfaguiar.harvester.core.HarvestRequest;
import io.github.sfaguiar.harvester.core.NeighborhoodPolicy;
import net.minecraft.world.World;
import net.modificationstation.stationapi.api.block.BlockState;
import net.modificationstation.stationapi.api.registry.tag.BlockTags;

/**
 * Converts a completed singleplayer block break into a {@code core}
 * {@link HarvestRequest} and runs discovery. Diagnostic only: this class
 * never mutates the world, never breaks a block, and is only reached after
 * vanilla has already fully resolved the origin break.
 *
 * <p>Classification is by {@code BlockTags.LOGS}
 * ({@code better-beta-program/docs/knowledge/claims/CLM-0015.md},
 * {@code PROVEN_WITHIN_SCOPE}), not by a hard-coded block ID — see
 * {@code better-beta-program/docs/operations/OPEN_QUESTIONS.md} Q0010 for
 * the adoption decision this follows. Both the origin (via the
 * {@link BlockState} the caller already captured, since the world no
 * longer holds it once the break has completed) and every neighbor
 * candidate (via a fresh {@code world.getBlockState(...)} lookup) are
 * classified the same way: {@link BlockState#isIn(net.modificationstation.stationapi.api.tag.TagKey)}
 * against {@link BlockTags#LOGS}. No block ID literal drives this decision
 * anywhere in this class; the raw pre-break ID this method still accepts is
 * carried into {@link HarvestRequest#originBlockId()} purely as diagnostic
 * metadata for the caller's own logging, never read here for
 * classification.
 *
 * <p>Neighbor connectivity uses {@link DefaultHarvestNeighborhoodPolicy#INSTANCE}
 * ({@link io.github.sfaguiar.harvester.core.LegacyTwentySixNeighborhood},
 * full 26-neighbor adjacency) as the confirmed production default — see
 * {@code better-beta-program/docs/operations/OPEN_QUESTIONS.md} Q0011,
 * now {@code DECIDED}: chosen for topological parity with legacy
 * Harvester 1.x chain-break connectivity and support for irregular/2x2
 * trunk shapes, accepting that diagonally-adjacent but otherwise separate
 * structures may merge into one discovered plan.
 * {@link io.github.sfaguiar.harvester.core.OrthogonalSixNeighborhood}
 * remains fully implemented and tested; it is simply not wired in here.
 *
 * <p><strong>This method itself is not unit-testable in isolation:</strong>
 * {@link BlockState} and {@link BlockTags} require StationAPI's registry to
 * be initialized, which does not happen in a pure JUnit run without
 * starting Minecraft. The default-policy <em>selection</em> is a separate,
 * genuinely unit-tested concern — see
 * {@link DefaultHarvestNeighborhoodPolicy} and
 * {@code DefaultHarvestNeighborhoodPolicyTest}, which depend on nothing
 * beyond {@code core}. The exact manual/integration procedure that instead
 * covers this whole method (classification included) is recorded in
 * {@code better-beta-program/docs/knowledge/experiments/EXP-0003.md}'s
 * procedure section; the classification rule changed (literal {@code 17}
 * to {@code BlockTags.LOGS}) after that session ran, so a fresh manual
 * repetition of the same three-scenario procedure (two non-log breaks, one
 * isolated log, one connected log structure) is the open follow-up
 * required before a new claim can assert the tag-based path itself as
 * {@code PROVEN_WITHIN_SCOPE} in real runtime — this class alone does not
 * establish that.
 */
public final class SingleplayerHarvestDiscoveryAdapter {

    /**
     * Diagnostic-only chain limit. Not sourced from any production
     * configuration (none exists yet) and not a decision about the 2.x
     * port's eventual default — see
     * {@code better-beta-program/docs/operations/OPEN_QUESTIONS.md} Q0002,
     * whose adoption decision remains the repository owner's. This value
     * only bounds the diagnostic BFS run below.
     */
    private static final int DIAGNOSTIC_LIMIT = 64;

    private SingleplayerHarvestDiscoveryAdapter() {
    }

    /**
     * Returns a plan only when {@code preBreakState} is tagged
     * {@code BlockTags.LOGS}; returns {@code null} for any other block
     * (e.g. the dirt and leaves breaks already observed in EXP-0002/
     * EXP-0003, which must not trigger discovery).
     *
     * @param preBreakBlockId diagnostic metadata only (carried into the
     *                        resulting {@link HarvestRequest}), never used
     *                        to decide membership
     * @param preBreakState   the origin's own {@link BlockState}, captured
     *                        by the caller before the break completed —
     *                        never StationAPI's private per-mixin cached
     *                        state field
     */
    public static HarvestPlan discoverForCompletedBreak(
            World world,
            int originX,
            int originY,
            int originZ,
            int preBreakBlockId,
            BlockState preBreakState
    ) {
        if (!preBreakState.isIn(BlockTags.LOGS)) {
            return null;
        }

        BlockGroupView groupView = coordinate ->
                world.getBlockState(coordinate.x(), coordinate.y(), coordinate.z())
                        .isIn(BlockTags.LOGS);
        NeighborhoodPolicy neighborhoodPolicy = DefaultHarvestNeighborhoodPolicy.INSTANCE;

        HarvestRequest request = new HarvestRequest(
                new BlockCoordinate(originX, originY, originZ),
                preBreakBlockId,
                true,
                DIAGNOSTIC_LIMIT
        );

        return ConnectedBlockFinder.discover(request, groupView, neighborhoodPolicy);
    }
}
