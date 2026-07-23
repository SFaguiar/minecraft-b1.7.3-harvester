package io.github.sfaguiar.harvester.game;

import io.github.sfaguiar.harvester.config.HarvesterConfig;
import io.github.sfaguiar.harvester.core.BlockCoordinate;
import io.github.sfaguiar.harvester.core.BlockDescriptor;
import io.github.sfaguiar.harvester.core.BlockGroupView;
import io.github.sfaguiar.harvester.core.ConnectedBlockFinder;
import io.github.sfaguiar.harvester.core.HarvestGroup;
import io.github.sfaguiar.harvester.core.HarvestGroupKind;
import io.github.sfaguiar.harvester.core.HarvestGroupResolver;
import io.github.sfaguiar.harvester.core.HarvestPlan;
import io.github.sfaguiar.harvester.core.HarvestRequest;
import io.github.sfaguiar.harvester.platform.HarvesterEntrypoint;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.modificationstation.stationapi.api.block.BlockState;

import java.util.Optional;
import java.util.Set;

/**
 * Converts a completed block break into a {@code core} {@link HarvestRequest}
 * and runs discovery — for every supported category. Side-agnostic (takes a
 * {@link PlayerEntity}/{@link World}, never {@code Minecraft} or a server-only
 * type), so this is the single discovery implementation both the singleplayer
 * client adapter and the multiplayer server observer delegate to. Diagnostic
 * only: never mutates the world, never breaks a block.
 *
 * <p>Gating, in order, before any BFS runs:
 * <ol>
 *   <li>classification + resolution ({@link StationBlockDescriptors},
 *   {@link HarvestGroupResolver});</li>
 *   <li>enable/disable precedence for the origin identity
 *   ({@link HarvesterConfig#isBlockChainable} — denylist &gt; allowlist &gt;
 *   category toggle);</li>
 *   <li>the tool gate: ore kinds use vanilla harvestability
 *   ({@link HarvestToolCompatibility#canHarvest}); logs/dirt/gravel/leaves/
 *   crops require the right tool <em>category</em>
 *   ({@link HarvestToolCategory}) held at the origin;</li>
 *   <li>the underground rule for dirt/gravel ({@link UndergroundRule}).</li>
 * </ol>
 * The same precedence, and the underground rule, are re-applied per candidate
 * inside the {@link BlockGroupView} so a denied/exposed block partway through
 * never joins the chain.
 */
public final class HarvestDiscoveryAdapter {

    private HarvestDiscoveryAdapter() {
    }

    /**
     * @param preBreakBlockId diagnostic metadata only, never used to decide
     *                        membership
     * @param preBreakMeta    the origin's raw pre-break metadata (leaf
     *                        species / crop maturity)
     * @param preBreakState   the origin's own {@link BlockState}, captured by
     *                        the caller before the break completed
     * @return {@code null} when the origin is ineligible, disabled by
     *         precedence, missing the required tool, or (dirt/gravel) not
     *         underground; discovery never runs in any of those cases
     */
    public static HarvestDiscoveryOutcome discoverForCompletedBreak(
            HarvesterConfig config,
            PlayerEntity player,
            World world,
            int originX,
            int originY,
            int originZ,
            int preBreakBlockId,
            int preBreakMeta,
            BlockState preBreakState
    ) {
        BlockDescriptor originDescriptor = StationBlockDescriptors.describe(preBreakState, preBreakMeta);
        Optional<HarvestGroup> resolved = HarvestGroupResolver.resolve(originDescriptor);
        if (resolved.isEmpty()) {
            HarvesterEntrypoint.LOGGER.debug(
                    "[HARVEST-EXEC] Discovery skipped: origin is not a recognized harvestable ({}).", originDescriptor
            );
            return null;
        }
        HarvestGroup group = resolved.get();
        HarvestGroupKind kind = group.kind();

        if (!config.isBlockChainable(originDescriptor.registryIdentity(), kind)) {
            HarvesterEntrypoint.LOGGER.debug(
                    "[HARVEST-EXEC] Discovery skipped: {} ({}) disabled by configuration (toggle/denylist).",
                    kind, originDescriptor.registryIdentity()
            );
            return null;
        }

        if (!originToolGatePasses(config, player, world, originX, originY, originZ, preBreakState, kind)) {
            return null;
        }

        if (isUndergroundKind(kind) && !undergroundAt(config, world, originX, originY, originZ)) {
            HarvesterEntrypoint.LOGGER.debug(
                    "[HARVEST-EXEC] Discovery skipped: {} origin is not underground.", kind
            );
            return null;
        }

        BlockGroupView groupView = coordinate -> candidateIsMember(config, world, group, kind, coordinate);

        HarvestRequest request = new HarvestRequest(
                new BlockCoordinate(originX, originY, originZ),
                preBreakBlockId,
                true,
                config.maxChain()
        );

        HarvestPlan plan = ConnectedBlockFinder.discover(request, groupView, config.neighborhoodPolicy());
        return new HarvestDiscoveryOutcome(plan, group);
    }

    private static boolean originToolGatePasses(
            HarvesterConfig config, PlayerEntity player, World world,
            int x, int y, int z, BlockState state, HarvestGroupKind kind
    ) {
        if (HarvestToolCompatibility.requiresToolCheck(kind)) {
            ItemStack heldItem = player != null ? player.getHand() : null;
            if (!HarvestToolCompatibility.canHarvest(player, world, heldItem, x, y, z, state)) {
                HarvesterEntrypoint.LOGGER.debug(
                        "[HARVEST-EXEC] Discovery skipped: held item cannot correctly harvest the {} origin.", kind
                );
                return false;
            }
            return true;
        }
        Optional<HarvestToolCategory> requiredCategory = HarvestToolCategory.requiredFor(kind);
        if (requiredCategory.isPresent()) {
            HarvestToolCategory category = requiredCategory.get();
            ItemStack heldItem = player != null ? player.getHand() : null;
            if (!category.matches(heldItem, toolAllowlist(config, category))) {
                HarvesterEntrypoint.LOGGER.debug(
                        "[HARVEST-EXEC] Discovery skipped: origin requires a {} to start the {} chain.", category, kind
                );
                return false;
            }
        }
        return true;
    }

    private static boolean candidateIsMember(
            HarvesterConfig config, World world, HarvestGroup group, HarvestGroupKind kind, BlockCoordinate coordinate
    ) {
        int x = coordinate.x();
        int y = coordinate.y();
        int z = coordinate.z();
        BlockDescriptor descriptor = StationBlockDescriptors.describe(
                world.getBlockState(x, y, z), world.getBlockMeta(x, y, z)
        );
        if (!group.matches(descriptor)) {
            return false;
        }
        if (!config.isBlockChainable(descriptor.registryIdentity(), kind)) {
            return false;
        }
        return !isUndergroundKind(kind) || undergroundAt(config, world, x, y, z);
    }

    private static boolean isUndergroundKind(HarvestGroupKind kind) {
        return kind == HarvestGroupKind.DIRT || kind == HarvestGroupKind.GRAVEL;
    }

    private static boolean undergroundAt(HarvesterConfig config, World world, int x, int y, int z) {
        return UndergroundRule.isUnderground(world.dimension.id, world.hasSkyLight(x, y, z), y, config);
    }

    private static Set<Integer> toolAllowlist(HarvesterConfig config, HarvestToolCategory category) {
        switch (category) {
            case AXE:
                return config.toolAxeIds();
            case SHOVEL:
                return config.toolShovelIds();
            case SHEARS:
                return config.toolShearsIds();
            case HOE:
                return config.toolHoeIds();
            default:
                throw new IllegalStateException("unreachable: " + category);
        }
    }
}
