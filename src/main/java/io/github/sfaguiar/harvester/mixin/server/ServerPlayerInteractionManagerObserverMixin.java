package io.github.sfaguiar.harvester.mixin.server;

import io.github.sfaguiar.harvester.game.ChainReentrancyGuard;
import io.github.sfaguiar.harvester.game.HarvestDiscoveryAdapter;
import io.github.sfaguiar.harvester.game.HarvestDiscoveryOutcome;
import io.github.sfaguiar.harvester.game.HarvesterHeldItemSnapshot;
import io.github.sfaguiar.harvester.multiplayer.HarvesterMultiplayerPlayerState;
import io.github.sfaguiar.harvester.platform.HarvesterEntrypoint;
import io.github.sfaguiar.harvester.server.HarvesterServerConfigState;
import io.github.sfaguiar.harvester.server.multiplayer.HarvesterMultiplayerServerRegistry;
import io.github.sfaguiar.harvester.server.multiplayer.ServerHarvestChainGate;
import io.github.sfaguiar.harvester.server.multiplayer.ServerHarvestExecutor;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.world.ServerWorld;
import net.modificationstation.stationapi.api.block.BlockState;
import net.modificationstation.stationapi.api.network.ModdedPacketHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * Server-side observer for {@code ServerPlayerInteractionManager}'s
 * authoritative completed-break method — the single convergence point
 * for a real multiplayer block break, chosen over the lower-level {@code
 * finishMining} because it is the only place both the player/tool
 * context and the removal result are available together (see {@code
 * ARCHITECTURE.md}, "Server-side break hook: two candidates compared").
 * No existing StationAPI Mixin targets this class, so this is a narrow,
 * newly-added {@code @Inject}, mirroring the existing
 * {@code SingleplayerInteractionManagerObserverMixin} pattern — now also
 * the trigger point of the server-authoritative additional-candidate
 * chain, exactly mirroring that client Mixin's own chain-trigger role.
 *
 * <p>This Mixin never cancels {@code tryBreakBlock}, never alters its
 * return value, and never mutates the world directly — every additional
 * break goes through {@link #harvester$invokeTryBreakBlock}, an invoker
 * onto this exact same method, so drops, durability, range, protection,
 * and permissions all resolve through the ordinary server pipeline exactly
 * as they do for the origin break.
 *
 * <p>Confirmed inert for a player's own singleplayer break: singleplayer
 * resolves the local player's manual break entirely through the
 * client-side {@code SingleplayerInteractionManager}, never through
 * {@code ServerPlayerInteractionManager} (see {@code ARCHITECTURE.md},
 * "Same-JAR side safety") — this Mixin's target method is simply never
 * invoked for that case, so it never fires there regardless of Mixin
 * loading rules.
 *
 * <p><strong>Reentrancy:</strong> the internal breaks this Mixin's own
 * chain trigger performs re-enter this same method on this same manager
 * instance (one {@code ServerPlayerInteractionManager} per player, so the
 * guard is naturally isolated per player — no cross-player interference,
 * no thread-local). {@link #harvester$chainGuard} is held for the whole
 * chain call and released in a {@code finally} block, so an exception
 * partway through can never leave it stuck on; while held, {@code
 * harvester$captureHead} captures nothing, making every inner invocation's
 * RETURN handler inert (no re-discovery, no nested chain).
 */
@Mixin(ServerPlayerInteractionManager.class)
abstract class ServerPlayerInteractionManagerObserverMixin {

    @Shadow
    public PlayerEntity player;

    @Shadow
    private ServerWorld world;

    @Invoker("tryBreakBlock")
    abstract boolean harvester$invokeTryBreakBlock(int x, int y, int z);

    @Unique
    private final ChainReentrancyGuard harvester$chainGuard = new ChainReentrancyGuard();

    @Unique
    private boolean harvester$capturedPreBreakState;

    @Unique
    private int harvester$preBlockId;

    @Unique
    private int harvester$preBlockMeta;

    @Unique
    private BlockState harvester$preBlockState;

    @Unique
    private HarvesterHeldItemSnapshot harvester$preBreakHeldItem;

    @Inject(method = "tryBreakBlock(III)Z", at = @At("HEAD"))
    private void harvester$captureHead(int x, int y, int z, CallbackInfoReturnable<Boolean> cir) {
        harvester$capturedPreBreakState = false;

        if (harvester$chainGuard.isInside()) {
            // Internal Harvester-initiated break: capture nothing, so this
            // invocation's RETURN handler is inert (no re-discovery, no
            // nested chain) — mirrors the client Mixin's own reentrancy
            // short-circuit exactly.
            return;
        }

        harvester$preBlockId = world.getBlockId(x, y, z);
        harvester$preBlockMeta = world.getBlockMeta(x, y, z);
        harvester$preBlockState = world.getBlockState(x, y, z);
        harvester$preBreakHeldItem = HarvesterHeldItemSnapshot.capture(player != null ? player.getHand() : null);
        harvester$capturedPreBreakState = true;
    }

    @Inject(method = "tryBreakBlock(III)Z", at = @At("RETURN"))
    private void harvester$observeReturn(int x, int y, int z, CallbackInfoReturnable<Boolean> cir) {
        boolean captured = harvester$capturedPreBreakState;
        int preBlockId = harvester$preBlockId;
        int preBlockMeta = harvester$preBlockMeta;
        BlockState preBlockState = harvester$preBlockState;
        HarvesterHeldItemSnapshot preBreakHeldItem = harvester$preBreakHeldItem;

        harvester$capturedPreBreakState = false;
        harvester$preBlockState = null;
        harvester$preBreakHeldItem = null;

        if (!captured) {
            return;
        }

        boolean success = cir.getReturnValueZ();
        boolean active = player instanceof ServerPlayerEntity diagnosticPlayer
                && HarvesterMultiplayerServerRegistry.get(diagnosticPlayer)
                        .map(HarvesterMultiplayerPlayerState::active)
                        .orElse(false);

        if (HarvesterServerConfigState.current().diagnosticLogging()) {
            int postBlockId = success ? world.getBlockId(x, y, z) : preBlockId;
            HarvesterEntrypoint.LOGGER.info(
                    "[HARVEST-MP-OBSERVE] player={} x={} y={} z={} preBlockId={} preMeta={} preBlockState={} "
                            + "preHeldItem={} active={} success={} postBlockId={}",
                    player != null ? player.name : "?",
                    x, y, z,
                    preBlockId, preBlockMeta, preBlockState,
                    preBreakHeldItem, active, success, postBlockId
            );
        }

        if (!success || !active || !(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }

        harvester$maybeExecuteChain(serverPlayer, x, y, z, preBlockId, preBlockMeta, preBlockState, preBreakHeldItem);
    }

    /**
     * Re-validates every precondition live (never trusting that {@code
     * active} alone still implies the rest), runs shared discovery, and —
     * only if the plan has at least one additional candidate — executes
     * the chain under {@link #harvester$chainGuard}.
     */
    @Unique
    private void harvester$maybeExecuteChain(
            ServerPlayerEntity serverPlayer,
            int x,
            int y,
            int z,
            int preBlockId,
            int preBlockMeta,
            BlockState preBlockState,
            HarvesterHeldItemSnapshot preBreakHeldItem
    ) {
        boolean connectionModded = serverPlayer.networkHandler instanceof ModdedPacketHandler moddedHandler
                && moddedHandler.isModded();
        Optional<HarvesterMultiplayerPlayerState> state = HarvesterMultiplayerServerRegistry.get(serverPlayer);
        boolean eligible = ServerHarvestChainGate.isEligible(
                connectionModded,
                state.map(HarvesterMultiplayerPlayerState::supportAnnounced).orElse(false),
                HarvesterServerConfigState.current().multiplayerAllowed(),
                state.map(HarvesterMultiplayerPlayerState::active).orElse(false)
        );
        if (!eligible) {
            return;
        }

        HarvestDiscoveryOutcome outcome = HarvestDiscoveryAdapter.discoverForCompletedBreak(
                HarvesterServerConfigState.current(), serverPlayer, world, x, y, z, preBlockId, preBlockMeta, preBlockState
        );
        if (outcome == null) {
            return;
        }

        HarvesterEntrypoint.LOGGER.info(
                "[HARVEST-MP-PLAN] player={} origin={} group={} preBlockId={} limit={} totalIncluded={} "
                        + "additionalCandidates={} limitReached={}",
                serverPlayer.name,
                outcome.plan().origin(),
                outcome.group().kind(),
                preBlockId,
                outcome.plan().limit(),
                outcome.plan().totalIncluded(),
                outcome.plan().additionalCandidateCount(),
                outcome.plan().limitReached()
        );

        if (outcome.plan().additionalCandidateCount() < 1) {
            return;
        }
        if (!harvester$chainGuard.tryEnter()) {
            // Structurally unreachable given the HEAD-capture short-circuit
            // above, kept as defense in depth.
            HarvesterEntrypoint.LOGGER.debug("[HARVEST-MP-EXEC] Skipped: chain already in flight (reentrant trigger).");
            return;
        }

        ItemStack heldItemAfterOrigin = serverPlayer.getHand();
        HarvesterHeldItemSnapshot postBreakHeldItem = HarvesterHeldItemSnapshot.capture(heldItemAfterOrigin);
        try {
            ServerHarvestExecutor.executeChain(
                    serverPlayer, world, this::harvester$invokeTryBreakBlock,
                    outcome.plan(), outcome.group(), preBreakHeldItem, postBreakHeldItem
            );
        } finally {
            harvester$chainGuard.exit();
        }
    }
}
