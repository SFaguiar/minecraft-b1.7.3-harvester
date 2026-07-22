package io.github.sfaguiar.harvester.server.multiplayer;

import io.github.sfaguiar.harvester.multiplayer.HarvesterIdentityRegistry;
import io.github.sfaguiar.harvester.multiplayer.HarvesterMultiplayerPlayerState;
import net.minecraft.entity.player.ServerPlayerEntity;

import java.util.Optional;

/**
 * Transitory, in-memory-only registry of per-player multiplayer state,
 * keyed by {@code ServerPlayerEntity} instance identity (never a name or
 * a UUID — this Minecraft version has no {@code UUID} field on any
 * player/entity class; see {@code ARCHITECTURE.md}, "Per-player server
 * state (design)"). Never persisted to disk.
 *
 * <p>A thin, Minecraft-typed specialization of {@link
 * HarvesterIdentityRegistry} (identity-keyed, only explicit removals —
 * its own documentation covers the data-structure guarantees, unit-tested
 * there with plain {@code Object} keys since a real {@code
 * ServerPlayerEntity} cannot be constructed outside a running server).
 * Every lifecycle event that must clear state has a dedicated,
 * deterministic Mixin hook (see below), so correctness never depends on
 * garbage collection:
 * <ul>
 *   <li>logout (client-initiated): {@code
 *   ServerPlayNetworkHandlerDisconnectMixin} on {@code
 *   ServerPlayNetworkHandler.onDisconnected(String, Object[])};</li>
 *   <li>logout (server-initiated kick): {@code
 *   ServerPlayerEntityDisconnectMixin} on {@code
 *   ServerPlayerEntity.onDisconnect()};</li>
 *   <li>respawn (a genuinely new {@code ServerPlayerEntity} instance —
 *   confirmed by disassembling {@code PlayerManager.respawnPlayer}, which
 *   constructs a new instance and never reuses the old one): {@code
 *   PlayerManagerLifecycleMixin#harvester$onRespawn} removes the
 *   *old* instance's entry at the point it is still available as the
 *   method's own parameter, before the new instance is constructed;</li>
 *   <li>dimension/world change (the *same* instance — confirmed by
 *   disassembling {@code PlayerManager.changePlayerDimension}, which only
 *   calls {@code ServerPlayerEntity.setWorld} and never constructs a new
 *   instance): {@code PlayerManagerLifecycleMixin#harvester$onDimensionChange}
 *   resets the operational state in place (not the whole entry — see
 *   {@link HarvesterMultiplayerPlayerState#resetOnWorldChange}'s own
 *   documentation for the connection-scoped, preserved {@code
 *   supportAnnounced} vs. operational-scoped, fully reset {@code active}/
 *   rate limiter/warning state distinction);</li>
 *   <li>server shutdown: {@code MinecraftServerShutdownMixin} on {@code
 *   MinecraftServer.shutdown()} (the private method {@code run()} calls
 *   on every exit path — normal stop and exceptional — confirmed by
 *   disassembly) calls {@link #clearAll()}.</li>
 * </ul>
 *
 * <p>Single-threaded access only (server packet application and every
 * lifecycle hook above run on the main server thread), matching every
 * other static holder in this codebase — no synchronization is used.
 */
public final class HarvesterMultiplayerServerRegistry {

    private static final HarvesterIdentityRegistry<ServerPlayerEntity, HarvesterMultiplayerPlayerState> STATES =
            new HarvesterIdentityRegistry<>(HarvesterMultiplayerPlayerState::new);

    private HarvesterMultiplayerServerRegistry() {
    }

    public static HarvesterMultiplayerPlayerState getOrCreate(ServerPlayerEntity player) {
        return STATES.getOrCreate(player);
    }

    public static Optional<HarvesterMultiplayerPlayerState> get(ServerPlayerEntity player) {
        return STATES.get(player);
    }

    /** Called from the disconnect/respawn Mixin hooks — deterministic, immediate cleanup. */
    public static void remove(ServerPlayerEntity player) {
        STATES.remove(player);
    }

    /** Called from the server-shutdown Mixin hook — deterministic, immediate, full cleanup. */
    public static void clearAll() {
        STATES.clearAll();
    }

    /** Test-only: current entry count, for asserting cleanup happened without relying on GC. */
    static int sizeForTesting() {
        return STATES.size();
    }

    /** Test-only reset. */
    static void clearForTesting() {
        STATES.clearAll();
    }
}
