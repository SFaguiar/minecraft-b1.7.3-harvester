package io.github.sfaguiar.harvester.client.multiplayer;

import io.github.sfaguiar.harvester.platform.HarvesterEntrypoint;
import net.modificationstation.stationapi.api.network.packet.MessagePacket;
import net.modificationstation.stationapi.api.network.packet.PacketHelper;
import net.modificationstation.stationapi.api.util.Identifier;

/**
 * Sends the {@code harvester:active} (client → server only) {@code
 * MessagePacket} — a single boolean, nothing else. Called only by {@link
 * HarvesterMultiplayerActivationState}, itself only ever invoked while
 * support is confirmed {@code SUPPORT_AVAILABLE_ENABLED}; this class
 * performs no gating of its own; it only sends.
 */
final class HarvesterActiveClientSender {

    static final Identifier ACTIVE_PACKET_ID = Identifier.of("harvester:active");

    private HarvesterActiveClientSender() {
    }

    static void send(boolean active) {
        MessagePacket packet = new MessagePacket(ACTIVE_PACKET_ID);
        packet.booleans = new boolean[] {active};
        PacketHelper.send(packet);
        HarvesterEntrypoint.LOGGER.debug("[HARVEST-MP] Sent harvester:active={}", active);
    }
}
