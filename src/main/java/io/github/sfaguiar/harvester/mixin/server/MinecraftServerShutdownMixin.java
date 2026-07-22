package io.github.sfaguiar.harvester.mixin.server;

import io.github.sfaguiar.harvester.platform.HarvesterEntrypoint;
import io.github.sfaguiar.harvester.server.multiplayer.HarvesterMultiplayerServerRegistry;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Full, deterministic registry teardown on server shutdown. Targets the
 * private {@code MinecraftServer.shutdown()} method — confirmed by
 * disassembly to be called from three separate points inside {@code
 * run()} (the normal loop-exit path and at least one exceptional path),
 * making it the single reliable point reached on every way the server
 * loop can end, unlike the public {@code stop()} (which only flips a
 * {@code running} flag the loop checks). Never alters vanilla shutdown
 * behavior — only calls {@link HarvesterMultiplayerServerRegistry#clearAll()}
 * at {@code HEAD}, before any of vanilla's own save/cleanup logic runs.
 *
 * <p>This also fires for a client's own embedded singleplayer server on
 * world exit — harmless, since {@code stationapi:event_bus_server}
 * (which registers {@code HarvesterServerActiveListener}/{@code
 * HarvesterServerSupportListener}) never sets up in a client process
 * (see {@code ARCHITECTURE.md}, "Singleplayer non-interference"), so the
 * registry is always empty there regardless.
 */
@Mixin(MinecraftServer.class)
abstract class MinecraftServerShutdownMixin {

    @Inject(method = "shutdown()V", at = @At("HEAD"))
    private void harvester$onShutdown(CallbackInfo ci) {
        HarvesterMultiplayerServerRegistry.clearAll();
        HarvesterEntrypoint.LOGGER.info("[HARVEST-MP] Server shutting down; multiplayer registry cleared.");
    }
}
