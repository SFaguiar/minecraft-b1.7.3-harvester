package io.github.sfaguiar.harvester.server.multiplayer;

import io.github.sfaguiar.harvester.multiplayer.HarvesterSupportPayload;
import io.github.sfaguiar.harvester.platform.HarvesterEntrypoint;
import io.github.sfaguiar.harvester.server.HarvesterServerConfigState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.mine_diver.unsafeevents.listener.EventListener;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.modificationstation.stationapi.api.network.ModdedPacketHandler;
import net.modificationstation.stationapi.api.network.packet.MessagePacket;
import net.modificationstation.stationapi.api.network.packet.PacketHelper;
import net.modificationstation.stationapi.api.server.event.network.PlayerLoginEvent;
import net.modificationstation.stationapi.api.util.Identifier;

/**
 * Announces {@code harvester:support} to a newly logged-in player, once,
 * only when the peer is already confirmed StationAPI-modded — see
 * {@code ARCHITECTURE.md}, "Vanilla-server compatibility" for why this
 * piggybacks on StationAPI's own {@code isModded()} signal instead of a
 * second detection layer. Registered under the
 * {@code stationapi:event_bus_server} entrypoint in {@code fabric.mod.json}.
 *
 * <p>This class never sends anything to a peer not already confirmed
 * modded, and never sends any packet other than {@code harvester:support}
 * — there is no {@code harvester:active} (or any other server-initiated
 * gameplay message) in this tranche.
 */
@Environment(EnvType.SERVER)
public class HarvesterServerSupportListener {

    /** {@code harvester:support} — server → client only; see {@code ARCHITECTURE.md}, "Protocol v1 (design)". */
    public static final Identifier SUPPORT_PACKET_ID = Identifier.of("harvester:support");

    @EventListener
    public void onPlayerLogin(PlayerLoginEvent event) {
        ServerPlayerEntity player = event.player;

        ModdedPacketHandler moddedPacketHandler = (ModdedPacketHandler) player.networkHandler;
        if (!moddedPacketHandler.isModded()) {
            HarvesterEntrypoint.LOGGER.debug(
                    "[HARVEST-MP] {} is not a StationAPI-modded peer; support not announced.", player.name
            );
            return;
        }

        boolean multiplayerAllowed = HarvesterServerConfigState.current().multiplayerAllowed();
        HarvesterSupportPayload payload =
                new HarvesterSupportPayload(HarvesterSupportPayload.SUPPORTED_PROTOCOL_VERSION, multiplayerAllowed);

        MessagePacket packet = new MessagePacket(SUPPORT_PACKET_ID);
        packet.ints = payload.toInts();
        packet.booleans = payload.toBooleans();
        PacketHelper.sendTo(player, packet);

        HarvesterEntrypoint.LOGGER.info(
                "[HARVEST-MP] Sent harvester:support to {} (multiplayerAllowed={}).", player.name, multiplayerAllowed
        );
    }
}
