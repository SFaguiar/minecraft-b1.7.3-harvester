package io.github.sfaguiar.harvester.mixin.server;

import io.github.sfaguiar.harvester.multiplayer.HarvesterMultiplayerPlayerState;
import io.github.sfaguiar.harvester.platform.HarvesterEntrypoint;
import io.github.sfaguiar.harvester.server.HarvesterServerConfigState;
import io.github.sfaguiar.harvester.server.multiplayer.HarvesterMultiplayerServerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.world.ServerWorld;
import net.modificationstation.stationapi.api.block.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Server-side observer for {@code ServerPlayerInteractionManager}'s
 * authoritative completed-break method — the single convergence point
 * for a real multiplayer block break, chosen over the lower-level {@code
 * finishMining} because it is the only place both the player/tool
 * context and the removal result are available together (see {@code
 * ARCHITECTURE.md}, "Server-side break hook: two candidates compared").
 * No existing StationAPI Mixin targets this class, so this is a narrow,
 * newly-added {@code @Inject}, mirroring the existing
 * {@code SingleplayerInteractionManagerObserverMixin} pattern.
 *
 * <p>This tranche is observer-only: it never cancels {@code
 * tryBreakBlock}, never alters its return value, never runs BFS, never
 * breaks an additional block, never mutates the world, and never touches
 * drops, durability, range, protection, or permissions — those all
 * continue to resolve exactly as the un-mixed vanilla/StationAPI
 * pipeline already does. It only reads already-computed state to emit an
 * optional diagnostic log line.
 *
 * <p>Confirmed inert for a player's own singleplayer break: singleplayer
 * resolves the local player's manual break entirely through the
 * client-side {@code SingleplayerInteractionManager}, never through
 * {@code ServerPlayerInteractionManager} (see {@code ARCHITECTURE.md},
 * "Same-JAR side safety") — this Mixin's target method is simply never
 * invoked for that case, so it never fires there regardless of Mixin
 * loading rules.
 */
@Mixin(ServerPlayerInteractionManager.class)
abstract class ServerPlayerInteractionManagerObserverMixin {

    @Shadow
    public PlayerEntity player;

    @Shadow
    private ServerWorld world;

    @Unique
    private int harvester$preBlockId;

    @Unique
    private int harvester$preBlockMeta;

    @Unique
    private BlockState harvester$preBlockState;

    @Unique
    private int harvester$preHeldItemId;

    @Inject(method = "tryBreakBlock(III)Z", at = @At("HEAD"))
    private void harvester$captureHead(int x, int y, int z, CallbackInfoReturnable<Boolean> cir) {
        harvester$preBlockId = world.getBlockId(x, y, z);
        harvester$preBlockMeta = world.getBlockMeta(x, y, z);
        harvester$preBlockState = world.getBlockState(x, y, z);
        ItemStack heldItem = player != null ? player.getHand() : null;
        harvester$preHeldItemId = heldItem != null ? heldItem.itemId : -1;
    }

    @Inject(method = "tryBreakBlock(III)Z", at = @At("RETURN"))
    private void harvester$observeReturn(int x, int y, int z, CallbackInfoReturnable<Boolean> cir) {
        int preBlockId = harvester$preBlockId;
        int preBlockMeta = harvester$preBlockMeta;
        BlockState preBlockState = harvester$preBlockState;
        int preHeldItemId = harvester$preHeldItemId;
        harvester$preBlockState = null;

        if (!HarvesterServerConfigState.current().diagnosticLogging()) {
            // "com false, nenhum log por quebra" — skip even the post-state read below.
            return;
        }

        boolean success = cir.getReturnValueZ();
        boolean active = player instanceof ServerPlayerEntity serverPlayer
                && HarvesterMultiplayerServerRegistry.get(serverPlayer)
                        .map(HarvesterMultiplayerPlayerState::active)
                        .orElse(false);
        int postBlockId = success ? world.getBlockId(x, y, z) : preBlockId;

        HarvesterEntrypoint.LOGGER.info(
                "[HARVEST-MP-OBSERVE] player={} x={} y={} z={} preBlockId={} preMeta={} preBlockState={} "
                        + "preHeldItemId={} active={} success={} postBlockId={}",
                player != null ? player.name : "?",
                x, y, z,
                preBlockId, preBlockMeta, preBlockState,
                preHeldItemId, active, success, postBlockId
        );
    }
}
