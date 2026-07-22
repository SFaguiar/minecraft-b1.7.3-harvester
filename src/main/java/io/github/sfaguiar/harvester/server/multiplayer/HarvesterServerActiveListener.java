package io.github.sfaguiar.harvester.server.multiplayer;

import io.github.sfaguiar.harvester.multiplayer.HarvesterActivePayload;
import io.github.sfaguiar.harvester.multiplayer.HarvesterMultiplayerPlayerState;
import io.github.sfaguiar.harvester.multiplayer.HarvesterTransitionOutcome;
import io.github.sfaguiar.harvester.multiplayer.RawMultiplayerMessage;
import io.github.sfaguiar.harvester.platform.HarvesterEntrypoint;
import io.github.sfaguiar.harvester.server.HarvesterServerConfigState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.mine_diver.unsafeevents.listener.EventListener;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.modificationstation.stationapi.api.event.registry.MessageListenerRegistryEvent;
import net.modificationstation.stationapi.api.network.ModdedPacketHandler;
import net.modificationstation.stationapi.api.network.packet.MessagePacket;
import net.modificationstation.stationapi.api.util.Identifier;

import java.util.Optional;

/**
 * Receives {@code harvester:active} (client → server only) and applies
 * it to the sender's transitory {@link HarvesterMultiplayerPlayerState},
 * subject to the task's strict acceptance rules — never trusting
 * anything from the client beyond the single boolean the payload
 * carries. Registered under the {@code stationapi:event_bus_server}
 * entrypoint in {@code fabric.mod.json}.
 *
 * <p>Accepted only when: the {@code Identifier} is exactly {@code
 * harvester:active} (structurally guaranteed — {@link
 * net.modificationstation.stationapi.api.registry.MessageListenerRegistry}
 * dispatches by identifier, so this listener is never invoked for any
 * other identifier); the payload is exactly one boolean and no other
 * field ({@link HarvesterActivePayload#parse}); the connection is
 * already confirmed StationAPI-modded ({@code isModded()}, the same
 * signal {@code HarvesterServerSupportListener} already piggybacks on);
 * {@code harvester:support} was already sent to this player ({@link
 * HarvesterMultiplayerPlayerState#supportAnnounced()}); and the server's
 * current {@code multiplayerAllowed} is {@code true} (re-checked live,
 * never assumed from the earlier announcement).
 *
 * <p>Any rejection is silent (at most a rate-limited {@code DEBUG}/{@code
 * WARN} log, never a disconnect) and either leaves {@code active}
 * unchanged (a rejected packet before the handshake, or while disabled)
 * or forces it to {@code false} (a rate-limited genuine transition) —
 * this class never cancels the connection.
 */
@Environment(EnvType.SERVER)
public class HarvesterServerActiveListener {

    /** {@code harvester:active} — client → server only; see {@code ARCHITECTURE.md}, "Protocol v1 (design)". */
    public static final Identifier ACTIVE_PACKET_ID = Identifier.of("harvester:active");

    @EventListener
    public void registerMessageListeners(MessageListenerRegistryEvent event) {
        event.register(ACTIVE_PACKET_ID, this::handleActiveMessage);
    }

    private void handleActiveMessage(PlayerEntity sender, MessagePacket message) {
        if (!(sender instanceof ServerPlayerEntity player)) {
            return;
        }

        ModdedPacketHandler moddedPacketHandler = (ModdedPacketHandler) player.networkHandler;
        if (!moddedPacketHandler.isModded()) {
            HarvesterEntrypoint.LOGGER.debug(
                    "[HARVEST-MP] Ignored harvester:active from non-modded peer {}.", player.name
            );
            return;
        }

        Optional<HarvesterActivePayload> payload = HarvesterActivePayload.parse(toRaw(message));
        if (payload.isEmpty()) {
            HarvesterEntrypoint.LOGGER.warn(
                    "[HARVEST-MP] Ignored malformed harvester:active from {}.", player.name
            );
            return;
        }

        HarvesterMultiplayerPlayerState state = HarvesterMultiplayerServerRegistry.getOrCreate(player);
        if (!state.supportAnnounced()) {
            HarvesterEntrypoint.LOGGER.warn(
                    "[HARVEST-MP] Ignored harvester:active from {} received before harvester:support was announced.",
                    player.name
            );
            return;
        }
        if (!HarvesterServerConfigState.current().multiplayerAllowed()) {
            HarvesterEntrypoint.LOGGER.warn(
                    "[HARVEST-MP] Ignored harvester:active from {}: multiplayer support is disabled on this server.",
                    player.name
            );
            return;
        }

        long now = System.currentTimeMillis();
        HarvesterTransitionOutcome outcome = state.applyTransition(payload.get().active(), now);
        switch (outcome) {
            case APPLIED -> HarvesterEntrypoint.LOGGER.debug(
                    "[HARVEST-MP] {} active={}", player.name, state.active()
            );
            case DUPLICATE_IGNORED -> {
                // Idempotent repeat; no log to avoid per-repeat spam.
            }
            case RATE_LIMITED -> {
                if (state.shouldWarnRateLimit(now)) {
                    HarvesterEntrypoint.LOGGER.warn(
                            "[HARVEST-MP] Rate limit exceeded for {}; forcing active=false.", player.name
                    );
                }
            }
        }
    }

    private static RawMultiplayerMessage toRaw(MessagePacket message) {
        return new RawMultiplayerMessage(
                message.ints, message.booleans, message.bytes, message.shorts, message.chars,
                message.longs, message.floats, message.doubles, message.strings
        );
    }
}
