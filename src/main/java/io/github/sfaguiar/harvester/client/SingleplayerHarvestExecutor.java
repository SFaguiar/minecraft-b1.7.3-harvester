package io.github.sfaguiar.harvester.client;

import io.github.sfaguiar.harvester.client.input.HarvesterClientActivationState;
import io.github.sfaguiar.harvester.core.BlockCoordinate;
import io.github.sfaguiar.harvester.core.HarvestGroup;
import io.github.sfaguiar.harvester.core.HarvestPlan;
import io.github.sfaguiar.harvester.platform.HarvesterEntrypoint;
import net.minecraft.client.InteractionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.modificationstation.stationapi.api.block.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Executes the full additional-candidate chain of an already-computed
 * {@link HarvestPlan} - the plan's own limit (which already includes the
 * origin, per {@code maxChain} semantics) is respected as-is; this class
 * never recomputes or widens it, it only walks the plan's deterministic
 * order. Works for logs and ores alike, driven entirely by the resolved
 * {@link HarvestGroup} passed in - no group-kind-specific branching lives
 * here beyond {@link HarvestToolCompatibility#requiresToolCheck}.
 *
 * <p>Every break is performed exclusively through the game's normal
 * singleplayer flow, {@code InteractionManager.breakBlock(int, int, int,
 * int)} (virtual-dispatched to {@code SingleplayerInteractionManager}'s
 * override), so the ordinary pipeline decides harvestability, applies
 * durability, produces drops, and runs block/tool/StationAPI/mod hooks.
 * This class never mutates the world directly, never touches drops or
 * durability itself, and never damages a tool directly - it only
 * <em>reads</em> the held item's state for the {@code [HARVEST-DURABILITY]}
 * diagnostic and the ore tool-harvestability gate.
 *
 * <p><strong>Direction parameter</strong>: unchanged reasoning from the
 * single-break slice - direct bytecode inspection of the mapped merged jar
 * confirmed {@code breakBlock}'s 4th parameter is never read by the
 * override, its superclass body, or either StationAPI Mixin on the same
 * method. The origin manual break's direction is reused verbatim for every
 * candidate in the chain; no per-candidate geometric direction is
 * computed.
 */
public final class SingleplayerHarvestExecutor {

    private SingleplayerHarvestExecutor() {
    }

    /**
     * Attempts the plan's full additional-candidate chain, in the plan's
     * deterministic order, stopping at the first disqualifying condition
     * (never skipping a candidate to try the next one).
     *
     * <p>Caller contract: the caller (the observer Mixin) must hold its
     * reentrancy guard around this entire call, for the whole chain - the
     * internal {@code breakBlock} invocations below re-enter the same
     * Mixin on the same manager instance, and only the guard prevents
     * each of those inner invocations from generating a new plan or
     * another chain. {@code originHeldItemBefore}/{@code
     * originHeldItemAfter} are the origin's own manual-break snapshots,
     * already captured by the caller (never re-derived here); if they
     * indicate the tool changed identity during that manual break, the
     * chain is never started at all. {@code group} is the same resolved
     * group discovery already used to build the plan - re-supplied here
     * (not re-resolved) so candidate revalidation stays consistent with
     * what was actually discovered.
     */
    public static SingleplayerHarvestExecutionResult executeChain(
            Minecraft minecraft,
            InteractionManager interactionManager,
            HarvestPlan plan,
            HarvestGroup group,
            int direction,
            HarvesterHeldItemSnapshot originHeldItemBefore,
            HarvesterHeldItemSnapshot originHeldItemAfter
    ) {
        if (!HarvesterConfigState.current().enabled()) {
            HarvesterEntrypoint.LOGGER.debug("[HARVEST-EXEC] Skipped: automatic chain disabled by configuration.");
            return SingleplayerHarvestExecutionResult.SKIPPED_DISABLED;
        }
        if (!HarvesterClientActivationState.isActive()) {
            HarvesterEntrypoint.LOGGER.debug("[HARVEST-EXEC] Skipped: activation key not held.");
            return SingleplayerHarvestExecutionResult.SKIPPED_INACTIVE;
        }
        if (!environmentValid(minecraft, interactionManager)) {
            HarvesterEntrypoint.LOGGER.debug("[HARVEST-EXEC] Skipped: world, player, or interaction manager unavailable.");
            return SingleplayerHarvestExecutionResult.SKIPPED_INACTIVE;
        }

        boolean diagnosticLogging = HarvesterConfigState.current().diagnosticLogging();
        if (diagnosticLogging) {
            HarvesterEntrypoint.LOGGER.debug(
                    "[HARVEST-DURABILITY] origin pre-break: {}", originHeldItemBefore
            );
            HarvesterEntrypoint.LOGGER.debug(
                    "[HARVEST-DURABILITY] origin post-break: {} changed={}",
                    originHeldItemAfter, !originHeldItemAfter.sameIdentityAs(originHeldItemBefore)
            );
        }
        if (!originHeldItemAfter.sameIdentityAs(originHeldItemBefore)) {
            HarvesterEntrypoint.LOGGER.info(
                    "[HARVEST-EXEC] Chain not started: held item changed identity during the origin's own manual break "
                            + "(before={}, after={})",
                    originHeldItemBefore, originHeldItemAfter
            );
            return SingleplayerHarvestExecutionResult.ORIGIN_TOOL_CHANGED_BEFORE_CHAIN_START;
        }

        boolean requiresToolCheck = HarvestToolCompatibility.requiresToolCheck(group.kind());

        BlockCoordinate origin = plan.origin();
        List<BlockCoordinate> candidates = new ArrayList<>();
        for (BlockCoordinate included : plan.includedBlocks()) {
            if (!included.equals(origin)) {
                candidates.add(included);
            }
        }
        int totalPlanned = candidates.size();
        if (totalPlanned == 0) {
            HarvesterEntrypoint.LOGGER.debug("[HARVEST-EXEC] Skipped: plan has no additional candidate.");
            return SingleplayerHarvestExecutionResult.NO_ADDITIONAL_CANDIDATE;
        }

        HarvesterEntrypoint.LOGGER.info(
                "[HARVEST-EXEC] Chain starting: origin={} group={} planTotal={} totalPlanned={}",
                origin, group.kind(), plan.totalIncluded(), totalPlanned
        );

        int successes = 0;
        SingleplayerHarvestExecutionResult stopReason = SingleplayerHarvestExecutionResult.CHAIN_COMPLETED;

        for (int index = 0; index < totalPlanned; index++) {
            if (!HarvesterClientActivationState.isActive()) {
                stopReason = SingleplayerHarvestExecutionResult.STOPPED_KEY_RELEASED;
                break;
            }
            if (!environmentValid(minecraft, interactionManager)) {
                stopReason = SingleplayerHarvestExecutionResult.STOPPED_ENVIRONMENT_INVALID;
                break;
            }

            BlockCoordinate candidate = candidates.get(index);
            BlockState candidateState = candidateStateIfPresent(minecraft.world, candidate);
            if (candidateState == null || !group.matches(StationBlockDescriptors.describe(candidateState))) {
                HarvesterEntrypoint.LOGGER.debug(
                        "[HARVEST-EXEC] Chain candidate {}/{} no longer valid: {}",
                        index + 1, totalPlanned, candidate
                );
                stopReason = SingleplayerHarvestExecutionResult.STOPPED_CANDIDATE_INVALID;
                break;
            }

            if (requiresToolCheck) {
                ItemStack currentHeldItem = heldItem(minecraft);
                if (!HarvestToolCompatibility.canHarvest(
                        minecraft, currentHeldItem, candidate.x(), candidate.y(), candidate.z(), candidateState
                )) {
                    HarvesterEntrypoint.LOGGER.debug(
                            "[HARVEST-EXEC] Chain candidate {}/{} tool no longer suitable: {}",
                            index + 1, totalPlanned, candidate
                    );
                    stopReason = SingleplayerHarvestExecutionResult.STOPPED_TOOL_UNSUITABLE;
                    break;
                }
            }

            HarvesterHeldItemSnapshot before = HarvesterHeldItemSnapshot.capture(heldItem(minecraft));
            if (diagnosticLogging) {
                HarvesterEntrypoint.LOGGER.debug(
                        "[HARVEST-DURABILITY] candidate[{}] pre-break: {}", index, before
                );
                HarvesterEntrypoint.LOGGER.info(
                        "[HARVEST-EXEC] Chain candidate {}/{}: attempting break at {} (successesSoFar={})",
                        index + 1, totalPlanned, candidate, successes
                );
            }
            boolean broke = interactionManager.breakBlock(candidate.x(), candidate.y(), candidate.z(), direction);

            if (!broke) {
                if (diagnosticLogging) {
                    HarvesterEntrypoint.LOGGER.info(
                            "[HARVEST-EXEC] Chain candidate {}/{} rejected: {}", index + 1, totalPlanned, candidate
                    );
                }
                stopReason = SingleplayerHarvestExecutionResult.STOPPED_BREAK_REJECTED;
                break;
            }

            successes++;
            HarvesterHeldItemSnapshot after = HarvesterHeldItemSnapshot.capture(heldItem(minecraft));
            boolean toolChanged = !after.sameIdentityAs(before);
            if (diagnosticLogging) {
                HarvesterEntrypoint.LOGGER.debug(
                        "[HARVEST-DURABILITY] candidate[{}] post-break: {} changed={}", index, after, toolChanged
                );
                HarvesterEntrypoint.LOGGER.info(
                        "[HARVEST-EXEC] Chain candidate {}/{} succeeded: {} (successesSoFar={})",
                        index + 1, totalPlanned, candidate, successes
                );
            }

            if (toolChanged) {
                HarvesterEntrypoint.LOGGER.info(
                        "[HARVEST-EXEC] Chain stopping: held item changed identity after candidate {}/{} "
                                + "(before={}, after={})",
                        index + 1, totalPlanned, before, after
                );
                stopReason = SingleplayerHarvestExecutionResult.STOPPED_TOOL_CHANGED;
                break;
            }
        }

        HarvesterEntrypoint.LOGGER.info(
                "[HARVEST-EXEC] Chain finished: origin={} group={} totalPlanned={} successes={} stopReason={}",
                origin, group.kind(), totalPlanned, successes, stopReason
        );
        return stopReason;
    }

    private static boolean environmentValid(Minecraft minecraft, InteractionManager interactionManager) {
        return minecraft != null && minecraft.world != null && minecraft.player != null
                && interactionManager != null;
    }

    /** {@code null} when the candidate position no longer holds a real block (air). */
    private static BlockState candidateStateIfPresent(World world, BlockCoordinate candidate) {
        int x = candidate.x();
        int y = candidate.y();
        int z = candidate.z();
        if (world.getBlockId(x, y, z) == 0) {
            return null;
        }
        return world.getBlockState(x, y, z);
    }

    private static ItemStack heldItem(Minecraft minecraft) {
        return minecraft.player.getHand();
    }
}
