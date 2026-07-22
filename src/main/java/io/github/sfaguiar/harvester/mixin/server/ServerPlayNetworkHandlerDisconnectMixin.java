package io.github.sfaguiar.harvester.mixin.server;

import io.github.sfaguiar.harvester.platform.HarvesterEntrypoint;
import io.github.sfaguiar.harvester.server.multiplayer.HarvesterMultiplayerServerRegistry;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The actual per-player logout hook for a normal client disconnect
 * (connection lost, quit, timeout) — confirmed by live multiplayer
 * runtime testing plus disassembly, superseding the assumption behind
 * {@code ServerPlayerEntityDisconnectMixin}
 * ({@code ServerPlayerEntity.onDisconnect()}), which this session's
 * testing showed never fires for that case (its only caller found by
 * disassembly is {@code ServerPlayNetworkHandler.disconnect(String)},
 * the server-initiated-kick path). {@code onDisconnected(String,
 * Object[])} is what actually runs for {@code disconnect.endOfStream}/
 * {@code disconnect.genericReason} (logs "X lost connection", broadcasts
 * "X left the game"), confirmed against the pinned toolchain by
 * observing real dedicated-server logs during this tranche's T-series
 * runtime verification — see {@code ARCHITECTURE.md}'s T6 result.
 *
 * <p>Both this Mixin and {@code ServerPlayerEntityDisconnectMixin} are
 * kept: {@link HarvesterMultiplayerServerRegistry#remove} is idempotent,
 * so covering both the common (this class) and kick-initiated (the
 * other class) paths is harmless and strictly more correct than either
 * alone.
 */
@Mixin(ServerPlayNetworkHandler.class)
abstract class ServerPlayNetworkHandlerDisconnectMixin {

    @Shadow
    private ServerPlayerEntity player;

    @Inject(method = "onDisconnected(Ljava/lang/String;[Ljava/lang/Object;)V", at = @At("HEAD"))
    private void harvester$onDisconnected(String reason, Object[] args, CallbackInfo ci) {
        HarvesterMultiplayerServerRegistry.remove(player);
        HarvesterEntrypoint.LOGGER.info("[HARVEST-MP] Player logout (network): {}", player.name);
    }
}
