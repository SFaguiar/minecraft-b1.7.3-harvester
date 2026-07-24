package io.github.sfaguiar.harvester.mixin.client;

import io.github.sfaguiar.harvester.client.HarvesterConfigState;
import io.github.sfaguiar.harvester.client.SingleplayerHarvestDiscoveryAdapter;
import io.github.sfaguiar.harvester.client.SingleplayerHarvestExecutor;
import io.github.sfaguiar.harvester.client.input.HarvesterClientActivationState;
import io.github.sfaguiar.harvester.core.HarvestGroup;
import io.github.sfaguiar.harvester.core.HarvestPlan;
import io.github.sfaguiar.harvester.game.HarvestDiscoveryOutcome;
import io.github.sfaguiar.harvester.game.HarvesterHeldItemSnapshot;
import io.github.sfaguiar.harvester.platform.HarvesterEntrypoint;
import net.minecraft.client.InteractionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.SingleplayerInteractionManager;
import net.modificationstation.stationapi.api.block.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Observer for successful singleplayer block breaks (EXP-0002 tag), now
 * additionally the trigger point of the full additional-candidate chain:
 * when the activation key is held and the completed manual break produced
 * a {@code HarvestPlan}, {@link SingleplayerHarvestExecutor} is asked to
 * walk the plan's entire additional-candidate list, in order, through the
 * normal {@code breakBlock} flow.
 *
 * <p>This Mixin never cancels the observed method, never alters its
 * return value, and never reads StationAPI's private block-state cache.
 * The internal Harvester-initiated breaks re-enter this same Mixin on the
 * same manager instance; the per-instance reentrancy guard
 * ({@code harvester$executingAdditionalBreak}) makes every one of those
 * inner invocations completely inert here (no capture at HEAD, therefore
 * no EXP-0002 log, no discovery, no plan, and no further chain at RETURN)
 * — one manual break can trigger at most one chain, held for the chain's
 * entire duration, with no recursion or cascade.</p>
 */
@Mixin(SingleplayerInteractionManager.class)
abstract class SingleplayerInteractionManagerObserverMixin
        extends InteractionManager {

    @Unique
    private boolean harvester$capturedPreBreakState;

    /**
     * Per-manager-instance reentrancy guard. True only while the executor
     * is invoking the internal additional break from within this Mixin's
     * own RETURN handler; always restored in {@code finally} so an
     * exception can never leave it stuck on.
     */
    @Unique
    private boolean harvester$executingAdditionalBreak;

    @Unique
    private int harvester$preBreakBlockId;

    @Unique
    private int harvester$preBreakBlockMeta;

    /**
     * Captured via {@code minecraft.world.getBlockState(x, y, z)} — a
     * public StationAPI-added {@code World} method, the same one
     * StationAPI's own
     * {@code SingleplayerInteractionManagerMixin.stationapi_breakBlock_state}
     * uses internally. This field is Harvester's own storage, populated by
     * Harvester's own call to that public method; it never reads
     * StationAPI's private {@code @Unique} field.
     */
    @Unique
    private BlockState harvester$preBreakBlockState;

    /**
     * Held-item snapshot captured at HEAD, before the origin's own manual
     * break - part of the {@code [HARVEST-DURABILITY]} diagnostic. Read
     * out as primitives via {@link HarvesterHeldItemSnapshot#capture};
     * never a live {@code ItemStack} reference held across the break.
     */
    @Unique
    private HarvesterHeldItemSnapshot harvester$preBreakHeldItem;

    private SingleplayerInteractionManagerObserverMixin(Minecraft minecraft) {
        super(minecraft);
    }

    @Inject(
            method = "breakBlock(IIII)Z",
            at = @At("HEAD")
    )
    private void harvester$capturePreBreakState(
            int x,
            int y,
            int z,
            int direction,
            CallbackInfoReturnable<Boolean> cir
    ) {
        harvester$capturedPreBreakState = false;

        if (harvester$executingAdditionalBreak) {
            // Internal Harvester-initiated break: capture nothing, so this
            // invocation's RETURN handler is inert (no EXP-0002 log, no
            // discovery, no plan, no further break) and no field can be
            // confused with the external break's context — which was
            // already copied into locals before the internal call began.
            // Fires once per additional block in the chain — genuine
            // per-candidate volume, gated the same as the executor's own
            // per-candidate logs so diagnosticLogging=false actually
            // suppresses it.
            if (HarvesterConfigState.current().diagnosticLogging()) {
                HarvesterEntrypoint.LOGGER.debug(
                        "[HARVEST-EXEC] Reentrant break observed; observer skipped for internal call."
                );
            }
            return;
        }

        if (minecraft == null || minecraft.world == null) {
            return;
        }

        harvester$preBreakBlockId = minecraft.world.getBlockId(x, y, z);
        harvester$preBreakBlockMeta = minecraft.world.getBlockMeta(x, y, z);
        harvester$preBreakBlockState = minecraft.world.getBlockState(x, y, z);
        harvester$preBreakHeldItem = HarvesterHeldItemSnapshot.capture(
                minecraft.player != null ? minecraft.player.getHand() : null
        );
        harvester$capturedPreBreakState = true;
    }

    @Inject(
            method = "breakBlock(IIII)Z",
            at = @At("RETURN")
    )
    private void harvester$observeSuccessfulBreak(
            int x,
            int y,
            int z,
            int direction,
            CallbackInfoReturnable<Boolean> cir
    ) {
        boolean captured = harvester$capturedPreBreakState;
        int blockId = harvester$preBreakBlockId;
        int blockMeta = harvester$preBreakBlockMeta;
        BlockState blockState = harvester$preBreakBlockState;
        HarvesterHeldItemSnapshot preBreakHeldItem = harvester$preBreakHeldItem;

        harvester$capturedPreBreakState = false;
        harvester$preBreakBlockState = null;
        harvester$preBreakHeldItem = null;

        if (!captured || !cir.getReturnValueZ()) {
            return;
        }

        int postBreakBlockId =
                minecraft != null && minecraft.world != null
                        ? minecraft.world.getBlockId(x, y, z)
                        : -1;

        HarvesterEntrypoint.LOGGER.info(
                "[EXP-0002] Successful singleplayer break: "
                        + "x={} y={} z={} direction={} "
                        + "preBlockId={} preMeta={} postBlockId={}",
                x,
                y,
                z,
                direction,
                blockId,
                blockMeta,
                postBreakBlockId
        );

        if (!HarvesterConfigState.current().enabled()) {
            // Guard discovery itself, not just execution: when the
            // automatic chain is disabled there is no reason to run BFS at
            // all. SingleplayerHarvestExecutor#executeChain still repeats
            // this same check independently (defense in depth — the two
            // guards do not share state), so a future caller that reaches
            // the executor by another path is still stopped there.
            HarvesterEntrypoint.LOGGER.debug(
                    "[HARVEST-EXEC] Discovery skipped: automatic chain disabled by configuration."
            );
            return;
        }

        HarvestDiscoveryOutcome outcome = logHarvestPlanIfEligible(x, y, z, blockId, blockMeta, blockState);
        if (outcome == null) {
            return;
        }

        HarvesterHeldItemSnapshot postBreakHeldItem = HarvesterHeldItemSnapshot.capture(
                minecraft != null && minecraft.player != null ? minecraft.player.getHand() : null
        );

        harvester$maybeExecuteChain(outcome.plan(), outcome.group(), direction, preBreakHeldItem, postBreakHeldItem);
    }

    /**
     * CLM-0012/CLM-0015/CLM-0021 diagnostic: resolves the origin's harvest
     * group (log, ore by specific tag, or ore by identity fallback),
     * applies the {@code harvestLogs}/{@code harvestOres} and (for ore) the
     * pre-break tool-harvestability gate, runs the pure BFS discovery core
     * only if all of that passes, logs the resulting plan, and returns it
     * together with the resolved group so the execution step can consume
     * both. Discovery itself never mutates the world - it only runs for an
     * origin {@link SingleplayerHarvestDiscoveryAdapter} actually resolved
     * a group for; an ineligible, gated-off, or tool-unsuitable origin
     * produces no plan at all. {@code blockId}/{@code blockMeta} are logged
     * here purely as diagnostic metadata - neither drives the
     * classification decision itself.
     *
     * @return the discovery outcome, or {@code null} when the broken block
     *         was ineligible, its group kind is disabled by configuration,
     *         the held item cannot correctly harvest an ore origin, or the
     *         world is unavailable
     */
    @Unique
    private HarvestDiscoveryOutcome logHarvestPlanIfEligible(
            int x, int y, int z, int blockId, int blockMeta, BlockState blockState
    ) {
        if (minecraft == null || minecraft.world == null) {
            return null;
        }

        HarvestDiscoveryOutcome outcome = SingleplayerHarvestDiscoveryAdapter.discoverForCompletedBreak(
                minecraft, minecraft.world, x, y, z, blockId, blockMeta, blockState
        );

        if (outcome == null) {
            return null;
        }
        HarvestPlan plan = outcome.plan();

        HarvesterEntrypoint.LOGGER.info(
                "[HARVEST-PLAN] origin={} group={} preBlockId={} preMeta={} limit={} "
                        + "totalIncluded={} additionalCandidates={} limitReached={}",
                plan.origin(),
                outcome.group().kind(),
                blockId,
                blockMeta,
                plan.limit(),
                plan.totalIncluded(),
                plan.additionalCandidateCount(),
                plan.limitReached()
        );

        if (HarvesterConfigState.current().diagnosticLogging() && HarvesterEntrypoint.LOGGER.isDebugEnabled()) {
            HarvesterEntrypoint.LOGGER.debug(
                    "[HARVEST-PLAN] includedBlocks={}", plan.includedBlocks()
            );
        }

        return outcome;
    }

    /**
     * Chain trigger: attempts the plan's full additional-candidate chain
     * via {@link SingleplayerHarvestExecutor} when the activation key is
     * held and the plan has at least one additional candidate. The
     * reentrancy guard is set once for the whole executor call - covering
     * every internal break the chain performs, not just the first - and
     * restored in {@code finally}, so an exception partway through the
     * chain can never leave the guard stuck on. All external-break data
     * the executor needs ({@code plan}, {@code direction}, the origin's
     * held-item snapshots) is passed as immutable locals/parameters -
     * nothing here reads the {@code @Unique} capture fields after the
     * internal calls start.
     */
    @Unique
    private void harvester$maybeExecuteChain(
            HarvestPlan plan,
            HarvestGroup group,
            int direction,
            HarvesterHeldItemSnapshot preBreakHeldItem,
            HarvesterHeldItemSnapshot postBreakHeldItem
    ) {
        if (!HarvesterClientActivationState.isActive()) {
            HarvesterEntrypoint.LOGGER.debug(
                    "[HARVEST-EXEC] Skipped: activation key not held."
            );
            return;
        }
        if (plan.additionalCandidateCount() < 1) {
            HarvesterEntrypoint.LOGGER.debug(
                    "[HARVEST-EXEC] Skipped: plan has no additional candidate."
            );
            return;
        }
        if (harvester$executingAdditionalBreak) {
            HarvesterEntrypoint.LOGGER.debug(
                    "[HARVEST-EXEC] Skipped: chain already in flight (reentrant trigger)."
            );
            return;
        }

        harvester$executingAdditionalBreak = true;
        try {
            SingleplayerHarvestExecutor.executeChain(
                    minecraft, this, plan, group, direction, preBreakHeldItem, postBreakHeldItem
            );
        } finally {
            harvester$executingAdditionalBreak = false;
        }
    }
}
