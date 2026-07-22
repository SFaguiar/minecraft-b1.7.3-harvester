package io.github.sfaguiar.harvester.client.multiplayer;

import io.github.sfaguiar.harvester.multiplayer.HarvesterSupportPayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.mine_diver.unsafeevents.listener.EventListener;
import net.modificationstation.stationapi.api.client.event.network.MultiplayerLogoutEvent;
import net.modificationstation.stationapi.api.client.event.network.ServerLoginSuccessEvent;
import net.modificationstation.stationapi.api.event.registry.MessageListenerRegistryEvent;
import net.modificationstation.stationapi.api.event.tick.GameTickEvent;
import net.modificationstation.stationapi.api.network.packet.MessagePacket;
import net.modificationstation.stationapi.api.util.Identifier;

import java.util.Optional;

/**
 * Registers the {@code harvester:support} message listener and drives
 * {@link HarvesterSupportClientState} from StationAPI's client event bus.
 * Registered under the {@code stationapi:event_bus_client} entrypoint in
 * {@code fabric.mod.json}, the same idiom
 * {@code HarvesterKeyBindingListener} already uses.
 *
 * <p>This class never constructs or sends any packet — it only receives.
 * There is no {@code harvester:active} (or any other client→server
 * message) in this tranche.
 */
@Environment(EnvType.CLIENT)
public class HarvesterSupportClientListener {

    /** {@code harvester:support} — server → client only; see {@code ARCHITECTURE.md}, "Protocol v1 (design)". */
    public static final Identifier SUPPORT_PACKET_ID = Identifier.of("harvester:support");

    @EventListener
    public void onLoginSuccess(ServerLoginSuccessEvent event) {
        HarvesterSupportClientState.onConnectionOperational(System.currentTimeMillis());
    }

    @EventListener
    public void onLogout(MultiplayerLogoutEvent event) {
        HarvesterSupportClientState.onDisconnected();
    }

    @EventListener
    public void onTick(GameTickEvent.End event) {
        HarvesterSupportClientState.onTick(System.currentTimeMillis());
    }

    @EventListener
    public void registerMessageListeners(MessageListenerRegistryEvent event) {
        event.register(SUPPORT_PACKET_ID, (player, message) -> handleSupportMessage(message));
    }

    private void handleSupportMessage(MessagePacket message) {
        Optional<HarvesterSupportPayload> payload = HarvesterSupportPayload.parse(message.ints, message.booleans);
        if (payload.isEmpty()) {
            HarvesterSupportClientState.warnOnceInvalidPayload();
            return;
        }
        HarvesterSupportClientState.onAnnouncementReceived(payload.get());
    }
}
