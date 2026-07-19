package io.github.sfaguiar.harvester.mixin.server;

import io.github.sfaguiar.harvester.platform.HarvesterEntrypoint;
import net.minecraft.entity.player.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Server-side player-logout hook. No StationAPI server-side logout event
 * exists (only a client-side {@code MultiplayerLogoutEvent} was found —
 * see {@code ARCHITECTURE.md}, "Lifecycle events found vs. requiring a
 * Mixin"), so this narrow {@code @Inject} on the vanilla method itself is
 * the smallest available hook, mirroring the existing
 * {@code SingleplayerInteractionManagerObserverMixin} pattern.
 *
 * <p>This tranche only logs. Per-player multiplayer state does not exist
 * yet (see {@code ARCHITECTURE.md}, "Per-player server state (design)")
 * — this injection point is where its future cleanup attaches, once that
 * state exists; there is nothing to clear today.
 */
@Mixin(ServerPlayerEntity.class)
abstract class ServerPlayerEntityDisconnectMixin {

    @Inject(method = "onDisconnect()V", at = @At("HEAD"))
    private void harvester$onDisconnect(CallbackInfo ci) {
        ServerPlayerEntity self = (ServerPlayerEntity) (Object) this;
        HarvesterEntrypoint.LOGGER.info("[HARVEST-MP] Player logout: {}", self.name);
    }
}
