package io.github.sfaguiar.harvester.game;

import io.github.sfaguiar.harvester.config.HarvesterConfig;
import io.github.sfaguiar.harvester.core.BlockCoordinate;
import io.github.sfaguiar.harvester.core.BlockDescriptor;
import io.github.sfaguiar.harvester.core.BlockDescriptorView;
import io.github.sfaguiar.harvester.core.BlockGroupView;
import io.github.sfaguiar.harvester.core.ConnectedBlockFinder;
import io.github.sfaguiar.harvester.core.HarvestGroup;
import io.github.sfaguiar.harvester.core.HarvestGroupResolver;
import io.github.sfaguiar.harvester.core.HarvestPlan;
import io.github.sfaguiar.harvester.core.HarvestRequest;
import io.github.sfaguiar.harvester.platform.HarvesterEntrypoint;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.modificationstation.stationapi.api.block.BlockState;

import java.util.Optional;

/**
 * Converts a completed block break into a {@code core} {@link
 * HarvestRequest} and runs discovery — for logs and ores alike. Side-agnostic
 * (takes a {@link PlayerEntity}/{@link World}, never {@code Minecraft} or a
 * server-only type), so this is the single discovery implementation both
 * {@code client.SingleplayerHarvestDiscoveryAdapter} and the multiplayer
 * server adapter delegate to — see {@code ARCHITECTURE.md}, "Reuse vs new
 * adapter". Diagnostic only: never mutates the world, never breaks a block.
 *
 * <p>Classification is entirely {@link StationBlockDescriptors}' and
 * {@link HarvestGroupResolver}'s responsibility: this class captures the
 * origin's {@link BlockDescriptor} once, resolves its {@link HarvestGroup}
 * (log priority, specific ore tags, or identity fallback — see
 * {@link HarvestGroupResolver}), applies the matching {@code
 * harvestLogs}/{@code harvestOres} configuration gate, and — for an ore
 * group only — the pre-break tool-harvestability gate, before ever running
 * the BFS. No block ID or tag constant drives classification here; the raw
 * pre-break ID this method still accepts is carried into {@link
 * HarvestRequest#originBlockId()} purely as diagnostic metadata for the
 * caller's own logging.
 *
 * <p>Neighbor connectivity uses {@code config}'s configured
 * {@link io.github.sfaguiar.harvester.core.NeighborhoodPolicy} for both logs
 * and ores; the same policy applies to whichever group kind was resolved.
 *
 * <p><strong>Not unit-testable in isolation:</strong> {@link BlockState}
 * requires StationAPI's registry to be initialized, which does not happen
 * in a pure JUnit run without starting Minecraft. Classification and
 * gating are unit-tested at the pure layer instead — see {@code
 * HarvestGroupResolverTest}/{@code HarvestGroupTest} (core) and {@code
 * HarvesterConfigTest#isHarvestEnabledFor} (config).
 */
public final class HarvestDiscoveryAdapter {

    private HarvestDiscoveryAdapter() {
    }

    /**
     * @param preBreakBlockId diagnostic metadata only (carried into the
     *                        resulting {@link HarvestRequest}), never used
     *                        to decide membership
     * @param preBreakState   the origin's own {@link BlockState}, captured
     *                        by the caller before the break completed
     * @return {@code null} when the origin is ineligible (neither a log nor
     *         an ore), its group kind is gated off by {@code config}, or —
     *         for an ore group — {@code player}'s currently held item
     *         cannot correctly harvest the origin; discovery never runs in
     *         any of those cases
     */
    public static HarvestDiscoveryOutcome discoverForCompletedBreak(
            HarvesterConfig config,
            PlayerEntity player,
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

        if (!config.isHarvestEnabledFor(group.kind())) {
            HarvesterEntrypoint.LOGGER.debug(
                    "[HARVEST-EXEC] Discovery skipped: {} harvesting disabled by configuration.", group.kind()
            );
            return null;
        }

        if (HarvestToolCompatibility.requiresToolCheck(group.kind())) {
            ItemStack heldItem = player != null ? player.getHand() : null;
            if (!HarvestToolCompatibility.canHarvest(player, world, heldItem, originX, originY, originZ, preBreakState)) {
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

        HarvestRequest request = new HarvestRequest(
                new BlockCoordinate(originX, originY, originZ),
                preBreakBlockId,
                true,
                config.maxChain()
        );

        HarvestPlan plan = ConnectedBlockFinder.discover(request, groupView, config.neighborhoodPolicy());
        return new HarvestDiscoveryOutcome(plan, group);
    }
}
