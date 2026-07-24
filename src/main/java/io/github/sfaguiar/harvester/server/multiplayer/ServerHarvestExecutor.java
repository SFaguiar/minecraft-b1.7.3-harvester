package io.github.sfaguiar.harvester.server.multiplayer;

import io.github.sfaguiar.harvester.core.BlockCoordinate;
import io.github.sfaguiar.harvester.core.HarvestGroup;
import io.github.sfaguiar.harvester.core.HarvestGroupKind;
import io.github.sfaguiar.harvester.core.HarvestPlan;
import io.github.sfaguiar.harvester.game.HarvestChainOutcome;
import io.github.sfaguiar.harvester.game.HarvestToolCompatibility;
import io.github.sfaguiar.harvester.game.HarvesterHeldItemSnapshot;
import io.github.sfaguiar.harvester.game.StationBlockDescriptors;
import io.github.sfaguiar.harvester.multiplayer.HarvesterMultiplayerPlayerState;
import io.github.sfaguiar.harvester.platform.HarvesterEntrypoint;
import io.github.sfaguiar.harvester.server.HarvesterServerConfigState;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.ServerWorld;
import net.modificationstation.stationapi.api.block.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Server-authoritative counterpart of {@code
 * client.SingleplayerHarvestExecutor} — structurally mirrors it (per
 * {@code ARCHITECTURE.md}, "Reuse vs new adapter": "a {@code
 * ServerHarvestDiscoveryAdapter}/{@code ServerHarvestExecutor} pair
 * mirroring the singleplayer ones structurally"), reading {@code
 * ServerWorld}/{@code ServerPlayerEntity}/{@link ServerBreakInvoker}
 * instead of {@code Minecraft}/{@code ClientPlayerEntity}/{@code
 * InteractionManager.breakBlock}. Classification ({@link
 * StationBlockDescriptors}), tool compatibility ({@link
 * HarvestToolCompatibility}), and the stop-reason vocabulary ({@link
 * HarvestChainOutcome}) are the exact same shared implementation the
 * singleplayer executor uses — never a divergent copy.
 *
 * <p>Every break is performed exclusively through {@code breakInvoker},
 * the caller's method reference onto {@code ServerPlayerInteractionManager
 * .tryBreakBlock(int, int, int)} — the same authoritative method the
 * origin break itself went through — so drops, durability, range,
 * protection, and permissions all resolve through the ordinary server
 * pipeline. This class never mutates the world directly, never touches
 * drops or durability itself, and never damages a tool directly.
 *
 * <p>The public {@link #executeChain} entry point (real Minecraft/
 * StationAPI types, called by the observer Mixin) is a thin wrapper over
 * the package-private {@link #runChain}, which holds the actual loop as
 * pure control flow over small functional seams — unit-testable with
 * fakes, with no Minecraft/StationAPI dependency of its own. See {@code
 * ServerHarvestExecutorTest}.
 */
public final class ServerHarvestExecutor {

    private ServerHarvestExecutor() {
    }

    /**
     * @param player          the breaking player; also read for {@code
     *                        multiplayerAllowed}-independent per-candidate
     *                        tool checks via its currently held item
     * @param world           the same {@code ServerWorld} the origin break
     *                        already resolved against
     * @param breakInvoker    invokes the normal server break pipeline for
     *                        one candidate coordinate
     * @param plan            the already-computed discovery plan (origin +
     *                        candidates, in deterministic order)
     * @param group           the resolved group the plan was built from —
     *                        re-supplied, never re-resolved, so
     *                        revalidation stays consistent with what was
     *                        actually discovered
     */
    public static HarvestChainOutcome executeChain(
            ServerPlayerEntity player,
            ServerWorld world,
            ServerBreakInvoker breakInvoker,
            HarvestPlan plan,
            HarvestGroup group,
            HarvesterHeldItemSnapshot originHeldItemBefore,
            HarvesterHeldItemSnapshot originHeldItemAfter
    ) {
        boolean requiresToolCheck = HarvestToolCompatibility.requiresToolCheck(group.kind());

        HarvestChainOutcome outcome = runChain(
                HarvesterServerConfigState.current().enabled(),
                () -> isActive(player),
                () -> environmentValid(player, world),
                candidate -> {
                    BlockState state = candidateStateIfPresent(world, candidate);
                    return state != null && group.matches(StationBlockDescriptors.describe(
                            state, world.getBlockMeta(candidate.x(), candidate.y(), candidate.z())
                    ));
                },
                requiresToolCheck,
                candidate -> {
                    BlockState state = candidateStateIfPresent(world, candidate);
                    return state != null && HarvestToolCompatibility.canHarvest(
                            player, world, player.getHand(), candidate.x(), candidate.y(), candidate.z(), state
                    );
                },
                () -> HarvesterHeldItemSnapshot.capture(player.getHand()),
                candidate -> breakInvoker.attempt(candidate.x(), candidate.y(), candidate.z()),
                plan,
                originHeldItemBefore,
                originHeldItemAfter,
                player.name,
                group
        );

        return outcome;
    }

    /**
     * Pure loop: every Minecraft/StationAPI-touching decision is injected
     * as a small functional seam instead of called directly, so this
     * method has no Minecraft/StationAPI import of its own and can be
     * driven entirely by fakes in a plain JUnit test.
     */
    static HarvestChainOutcome runChain(
            boolean enabled,
            BooleanSupplier isActive,
            BooleanSupplier isEnvironmentValid,
            Predicate<BlockCoordinate> candidateStillMember,
            boolean requiresToolCheck,
            Predicate<BlockCoordinate> toolSuitableFor,
            Supplier<HarvesterHeldItemSnapshot> currentHeldItem,
            Predicate<BlockCoordinate> attemptBreak,
            HarvestPlan plan,
            HarvesterHeldItemSnapshot originHeldItemBefore,
            HarvesterHeldItemSnapshot originHeldItemAfter,
            String playerNameForLogging,
            HarvestGroup groupForLogging
    ) {
        if (!enabled) {
            HarvesterEntrypoint.LOGGER.debug("[HARVEST-MP-EXEC] Skipped: automatic chain disabled by configuration.");
            return HarvestChainOutcome.SKIPPED_DISABLED;
        }
        if (!isActive.getAsBoolean()) {
            HarvesterEntrypoint.LOGGER.debug("[HARVEST-MP-EXEC] Skipped: player not active.");
            return HarvestChainOutcome.SKIPPED_INACTIVE;
        }
        if (!isEnvironmentValid.getAsBoolean()) {
            HarvesterEntrypoint.LOGGER.debug("[HARVEST-MP-EXEC] Skipped: player or world unavailable.");
            return HarvestChainOutcome.SKIPPED_INACTIVE;
        }
        if (!originHeldItemAfter.sameIdentityAs(originHeldItemBefore)) {
            HarvesterEntrypoint.LOGGER.info(
                    "[HARVEST-MP-EXEC] Chain not started: held item changed identity during the origin's own break "
                            + "(before={}, after={})",
                    originHeldItemBefore, originHeldItemAfter
            );
            return HarvestChainOutcome.ORIGIN_TOOL_CHANGED_BEFORE_CHAIN_START;
        }

        BlockCoordinate origin = plan.origin();
        List<BlockCoordinate> candidates = new ArrayList<>();
        for (BlockCoordinate included : plan.includedBlocks()) {
            if (!included.equals(origin)) {
                candidates.add(included);
            }
        }
        if (groupForLogging.kind() == HarvestGroupKind.GRAVEL) {
            // Gravity-safe order: break gravel top-down so a column never
            // collapses onto a not-yet-broken candidate (mirrors the client).
            candidates.sort((a, b) -> Integer.compare(b.y(), a.y()));
        }
        int totalPlanned = candidates.size();
        if (totalPlanned == 0) {
            HarvesterEntrypoint.LOGGER.debug("[HARVEST-MP-EXEC] Skipped: plan has no additional candidate.");
            return HarvestChainOutcome.NO_ADDITIONAL_CANDIDATE;
        }

        HarvesterEntrypoint.LOGGER.info(
                "[HARVEST-MP-EXEC] Chain starting: player={} origin={} group={} planTotal={} totalPlanned={}",
                playerNameForLogging, origin, groupForLogging.kind(), plan.totalIncluded(), totalPlanned
        );

        int successes = 0;
        HarvestChainOutcome stopReason = HarvestChainOutcome.CHAIN_COMPLETED;

        for (int index = 0; index < totalPlanned; index++) {
            if (!isActive.getAsBoolean()) {
                stopReason = HarvestChainOutcome.STOPPED_DEACTIVATED;
                break;
            }
            if (!isEnvironmentValid.getAsBoolean()) {
                stopReason = HarvestChainOutcome.STOPPED_ENVIRONMENT_INVALID;
                break;
            }

            BlockCoordinate candidate = candidates.get(index);
            if (!candidateStillMember.test(candidate)) {
                stopReason = HarvestChainOutcome.STOPPED_CANDIDATE_INVALID;
                break;
            }
            if (requiresToolCheck && !toolSuitableFor.test(candidate)) {
                stopReason = HarvestChainOutcome.STOPPED_TOOL_UNSUITABLE;
                break;
            }

            HarvesterHeldItemSnapshot before = currentHeldItem.get();
            if (!attemptBreak.test(candidate)) {
                stopReason = HarvestChainOutcome.STOPPED_BREAK_REJECTED;
                break;
            }

            successes++;
            HarvesterHeldItemSnapshot after = currentHeldItem.get();
            if (!after.sameIdentityAs(before)) {
                stopReason = HarvestChainOutcome.STOPPED_TOOL_CHANGED;
                break;
            }
        }

        HarvesterEntrypoint.LOGGER.info(
                "[HARVEST-MP-EXEC] Chain finished: player={} origin={} group={} totalPlanned={} successes={} stopReason={}",
                playerNameForLogging, origin, groupForLogging.kind(), totalPlanned, successes, stopReason
        );
        return stopReason;
    }

    private static boolean isActive(ServerPlayerEntity player) {
        return HarvesterMultiplayerServerRegistry.get(player)
                .map(HarvesterMultiplayerPlayerState::active)
                .orElse(false);
    }

    private static boolean environmentValid(ServerPlayerEntity player, ServerWorld world) {
        return player != null && world != null;
    }

    /** {@code null} when the candidate position no longer holds a real block (air). */
    private static BlockState candidateStateIfPresent(ServerWorld world, BlockCoordinate candidate) {
        int x = candidate.x();
        int y = candidate.y();
        int z = candidate.z();
        if (world.getBlockId(x, y, z) == 0) {
            return null;
        }
        return world.getBlockState(x, y, z);
    }
}
