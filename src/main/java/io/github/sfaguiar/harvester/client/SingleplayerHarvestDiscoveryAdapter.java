package io.github.sfaguiar.harvester.client;

import io.github.sfaguiar.harvester.core.BlockCoordinate;
import io.github.sfaguiar.harvester.core.BlockDescriptor;
import io.github.sfaguiar.harvester.core.BlockDescriptorView;
import io.github.sfaguiar.harvester.core.BlockGroupView;
import io.github.sfaguiar.harvester.core.ConnectedBlockFinder;
import io.github.sfaguiar.harvester.core.HarvestGroup;
import io.github.sfaguiar.harvester.core.HarvestGroupResolver;
import io.github.sfaguiar.harvester.core.HarvestPlan;
import io.github.sfaguiar.harvester.core.HarvestRequest;
import io.github.sfaguiar.harvester.core.NeighborhoodPolicy;
import io.github.sfaguiar.harvester.platform.HarvesterEntrypoint;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.modificationstation.stationapi.api.block.BlockState;

import java.util.Optional;

/**
 * Converts a completed singleplayer block break into a {@code core}
 * {@link HarvestRequest} and runs discovery — for logs and ores alike.
 * Diagnostic only: this class never mutates the world, never breaks a
 * block, and is only reached after vanilla has already fully resolved the
 * origin break.
 *
 * <p>Classification is entirely {@link StationBlockDescriptors}' and
 * {@link HarvestGroupResolver}'s responsibility: this class captures the
 * origin's {@link BlockDescriptor} once, resolves its {@link HarvestGroup}
 * (log priority, specific ore tags, or identity fallback — see
 * {@link HarvestGroupResolver}), applies the matching
 * {@code harvestLogs}/{@code harvestOres} configuration gate, and — for an
 * ore group only — the pre-break tool-harvestability gate, before ever
 * running the BFS. No block ID or tag constant drives classification here;
 * the raw pre-break ID this method still accepts is carried into
 * {@link HarvestRequest#originBlockId()} purely as diagnostic metadata for
 * the caller's own logging.
 *
 * <p>Neighbor connectivity uses {@link HarvesterConfigState#current()}'s
 * configured {@link NeighborhoodPolicy} (default
 * {@link io.github.sfaguiar.harvester.core.LegacyTwentySixNeighborhood},
 * full 26-neighbor adjacency — see
 * {@code better-beta-program/docs/operations/OPEN_QUESTIONS.md} Q0011,
 * `DECIDED`) for both logs and ores; the same policy applies to whichever
 * group kind was resolved.
 *
 * <p><strong>This method itself is not unit-testable in isolation:</strong>
 * {@link BlockState} requires StationAPI's registry to be initialized,
 * which does not happen in a pure JUnit run without starting Minecraft.
 * Classification and gating are unit-tested at the pure layer instead —
 * see {@code HarvestGroupResolverTest}/{@code HarvestGroupTest} (core) and
 * {@code HarvesterConfigTest#isHarvestEnabledFor} (config). The exact
 * manual/integration procedure that covers this method end to end is
 * recorded in {@code better-beta-program/docs/knowledge/experiments/}.
 */
public final class SingleplayerHarvestDiscoveryAdapter {

    private SingleplayerHarvestDiscoveryAdapter() {
    }

    /**
     * @param preBreakBlockId diagnostic metadata only (carried into the
     *                        resulting {@link HarvestRequest}), never used
     *                        to decide membership
     * @param preBreakState   the origin's own {@link BlockState}, captured
     *                        by the caller before the break completed —
     *                        never StationAPI's private per-mixin cached
     *                        state field
     * @return {@code null} when the origin is ineligible (neither a log nor
     *         an ore), its group kind is gated off by configuration, or —
     *         for an ore group — the currently held item cannot correctly
     *         harvest the origin; discovery never runs in any of those
     *         cases
     */
    public static HarvestDiscoveryOutcome discoverForCompletedBreak(
            Minecraft minecraft,
            World world,
            int originX,
            int originY,
            int originZ,
            int preBreakBlockId,
            BlockState preBreakState
    ) {
        BlockDescriptor originDescriptor = StationBlockDescriptors.describe(preBreakState);
        Optional<HarvestGroup> resolved = HarvestGroupResolver.resolve(originDescriptor);
        if (resolved.isEmpty()) {
            HarvesterEntrypoint.LOGGER.debug(
                    "[HARVEST-EXEC] Discovery skipped: origin is not a log or a recognized ore ({}).",
                    originDescriptor
            );
            return null;
        }
        HarvestGroup group = resolved.get();

        if (!HarvesterConfigState.current().isHarvestEnabledFor(group.kind())) {
            HarvesterEntrypoint.LOGGER.debug(
                    "[HARVEST-EXEC] Discovery skipped: {} harvesting disabled by configuration.", group.kind()
            );
            return null;
        }

        if (HarvestToolCompatibility.requiresToolCheck(group.kind())) {
            ItemStack heldItem = minecraft.player != null ? minecraft.player.getHand() : null;
            if (!HarvestToolCompatibility.canHarvest(minecraft, heldItem, originX, originY, originZ, preBreakState)) {
                HarvesterEntrypoint.LOGGER.debug(
                        "[HARVEST-EXEC] Discovery skipped: held item cannot correctly harvest the {} origin.",
                        group.kind()
                );
                return null;
            }
        }

        BlockDescriptorView descriptorView = coordinate -> StationBlockDescriptors.describe(
                world.getBlockState(coordinate.x(), coordinate.y(), coordinate.z())
        );
        BlockGroupView groupView = BlockGroupView.byDescriptor(descriptorView, group);
        NeighborhoodPolicy neighborhoodPolicy = HarvesterConfigState.current().neighborhoodPolicy();

        HarvestRequest request = new HarvestRequest(
                new BlockCoordinate(originX, originY, originZ),
                preBreakBlockId,
                true,
                HarvesterConfigState.current().maxChain()
        );

        HarvestPlan plan = ConnectedBlockFinder.discover(request, groupView, neighborhoodPolicy);
        return new HarvestDiscoveryOutcome(plan, group);
    }
}
