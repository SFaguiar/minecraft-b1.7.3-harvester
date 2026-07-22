package io.github.sfaguiar.harvester.mixin.server;

import io.github.sfaguiar.harvester.multiplayer.HarvesterMultiplayerPlayerState;
import io.github.sfaguiar.harvester.server.multiplayer.HarvesterMultiplayerServerRegistry;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.PlayerManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Two narrow, deterministic per-player-state cleanup hooks on {@code
 * PlayerManager} — neither cancels, alters a return value, or otherwise
 * changes vanilla behavior; each only calls into {@code
 * server.multiplayer} after the injection point.
 *
 * <p><b>Respawn:</b> {@code respawnPlayer(ServerPlayerEntity, int)}
 * constructs a genuinely new {@code ServerPlayerEntity} instance
 * (confirmed by disassembly: {@code new ServerPlayerEntity(...)} inside
 * the method body, copying only {@code id}/{@code networkHandler} from
 * the old instance) — the *old* instance, still available as this
 * method's own first parameter at {@code HEAD}, is removed from the
 * registry immediately, before the new instance even exists. The new
 * instance is simply never in the map, so it starts with every field at
 * its default ({@code supportAnnounced=false}, {@code active=false}, an
 * empty rate limiter, no warning state) with no extra code needed.
 * Confirmed the sole caller of {@code respawnPlayer} is {@code
 * ServerPlayNetworkHandler.onPlayerRespawn(PlayerRespawnPacket)} — the
 * client's post-death respawn request.
 *
 * <p><b>Dimension change:</b> {@code changePlayerDimension(ServerPlayerEntity)}
 * reuses the *same* instance (confirmed by disassembly: only {@code
 * ServerPlayerEntity.setWorld} is called, no new instance constructed),
 * so there is no new map key to rely on — the operational state ({@code
 * active}, the rate limiter, its warning-suppression timestamp) is
 * explicitly reset in place via {@link
 * HarvesterMultiplayerPlayerState#resetOnWorldChange} (a no-op if the
 * player has no entry yet), while {@code supportAnnounced} is preserved.
 * See that method's own documentation for the connection-scoped vs.
 * operational-scoped distinction.
 */
@Mixin(PlayerManager.class)
abstract class PlayerManagerLifecycleMixin {

    @Inject(
            method = "respawnPlayer(Lnet/minecraft/entity/player/ServerPlayerEntity;I)Lnet/minecraft/entity/player/ServerPlayerEntity;",
            at = @At("HEAD")
    )
    private void harvester$onRespawn(
            ServerPlayerEntity oldPlayer, int dimensionId, CallbackInfoReturnable<ServerPlayerEntity> cir
    ) {
        HarvesterMultiplayerServerRegistry.remove(oldPlayer);
    }

    @Inject(method = "changePlayerDimension(Lnet/minecraft/entity/player/ServerPlayerEntity;)V", at = @At("HEAD"))
    private void harvester$onDimensionChange(ServerPlayerEntity player, CallbackInfo ci) {
        HarvesterMultiplayerServerRegistry.get(player).ifPresent(HarvesterMultiplayerPlayerState::resetOnWorldChange);
    }
}
