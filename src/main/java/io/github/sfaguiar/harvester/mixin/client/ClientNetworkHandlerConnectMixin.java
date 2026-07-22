package io.github.sfaguiar.harvester.mixin.client;

import io.github.sfaguiar.harvester.client.multiplayer.HarvesterConnectionAddressState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.ClientNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Captures the exact, unresolved host string and port the player
 * connected with, at the one point both are directly available:
 * {@code ClientNetworkHandler}'s own constructor parameters. Confirmed
 * by disassembling this constructor: the host string is passed straight
 * into {@code InetAddress.getByName} and never itself stored as a field
 * — so this injection point is the only place to observe it before DNS
 * resolution, which the task's per-server identity rule explicitly
 * requires ("host lowercased + explicit port, never a resolved IP, MOTD,
 * or other server-supplied value"). No public StationAPI event carries
 * this raw address (checked: {@code ServerLoginSuccessEvent} only
 * carries the already-connected {@code ClientNetworkHandler} and the
 * login packet, neither of which retains it), so this narrow
 * {@code @Inject} is the smallest available hook, mirroring the existing
 * {@code SingleplayerInteractionManagerObserverMixin}/{@code
 * ServerPlayerEntityDisconnectMixin} pattern.
 *
 * <p>Fires for every new {@code ClientNetworkHandler} regardless of
 * singleplayer or multiplayer; this is harmless because {@code
 * HarvesterConnectionAddressState} is only ever read by {@code
 * HarvesterServerOptInState}, itself only invoked when a {@code
 * harvester:support} announcement actually arrives — and no such
 * announcement is ever sent for a client's own internal singleplayer
 * server, since StationAPI's {@code stationapi:event_bus_server}
 * entrypoint (which registers the announcing listener) is set up once
 * per process keyed by Fabric Loader's own environment type, never for
 * a client process's embedded server (confirmed by reading {@code
 * StationAPI#setupMods}). Never cancels, mutates, or delays the
 * constructor it observes.
 */
@Mixin(ClientNetworkHandler.class)
abstract class ClientNetworkHandlerConnectMixin {

    @Inject(
            method = "<init>(Lnet/minecraft/client/Minecraft;Ljava/lang/String;I)V",
            at = @At("RETURN")
    )
    private void harvester$captureConnectionAddress(
            Minecraft minecraft, String host, int port, CallbackInfo ci
    ) {
        HarvesterConnectionAddressState.onConnectionAttempt(host, port);
    }
}
