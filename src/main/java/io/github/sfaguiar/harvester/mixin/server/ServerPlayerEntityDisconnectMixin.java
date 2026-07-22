package io.github.sfaguiar.harvester.mixin.server;

import io.github.sfaguiar.harvester.platform.HarvesterEntrypoint;
import io.github.sfaguiar.harvester.server.multiplayer.HarvesterMultiplayerServerRegistry;
import net.minecraft.entity.player.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Server-side player-logout hook on {@code ServerPlayerEntity.onDisconnect()}
 * itself. No StationAPI server-side logout event exists (only a
 * client-side {@code MultiplayerLogoutEvent} was found — see {@code
 * ARCHITECTURE.md}, "Lifecycle events found vs. requiring a Mixin").
 *
 * <p><b>Confirmed by this tranche's live runtime testing (T6) that this
 * method is NOT invoked for a normal client disconnect</b> (connection
 * lost, quit, timeout) — disassembly shows its only caller is {@code
 * ServerPlayNetworkHandler.disconnect(String)}, the server-initiated-kick
 * path. The common case is covered instead by {@code
 * ServerPlayNetworkHandlerDisconnectMixin} (on {@code
 * ServerPlayNetworkHandler.onDisconnected(String, Object[])}, the method
 * actually observed firing for "player lost connection" in this
 * session's dedicated-server logs). Both Mixins are kept — {@link
 * HarvesterMultiplayerServerRegistry#remove} is idempotent, so covering
 * the kick path here in addition to the common path there is harmless
 * and strictly more correct than either alone; the respawn-instance-change
 * case neither Mixin observes is instead covered by its own dedicated
 * hook, {@code PlayerManagerLifecycleMixin#harvester$onRespawn} (see that
 * class's own documentation) — the registry itself is a plain {@code
 * IdentityHashMap}, with no GC-dependent cleanup anywhere.
 */
@Mixin(ServerPlayerEntity.class)
abstract class ServerPlayerEntityDisconnectMixin {

    @Inject(method = "onDisconnect()V", at = @At("HEAD"))
    private void harvester$onDisconnect(CallbackInfo ci) {
        ServerPlayerEntity self = (ServerPlayerEntity) (Object) this;
        HarvesterMultiplayerServerRegistry.remove(self);
        HarvesterEntrypoint.LOGGER.info("[HARVEST-MP] Player logout: {}", self.name);
    }
}
