package io.github.sfaguiar.harvester.server.multiplayer;

/**
 * Pure precondition check for whether a multiplayer harvest chain is even
 * allowed to start for a completed break — independent of Minecraft/
 * StationAPI, so it is directly unit-testable. Mirrors the acceptance
 * rules {@link HarvesterServerActiveListener} already applies to an
 * incoming {@code harvester:active} packet; re-checked here, live, at
 * break time rather than assumed from the last-known {@code active} value,
 * per the task's explicit precondition list ("conexão StationAPI válida;
 * servidor anunciou suporte; multiplayerAllowed=true; jogador está
 * active=true").
 */
public final class ServerHarvestChainGate {

    private ServerHarvestChainGate() {
    }

    public static boolean isEligible(
            boolean connectionModded,
            boolean supportAnnounced,
            boolean multiplayerAllowed,
            boolean playerActive
    ) {
        return connectionModded && supportAnnounced && multiplayerAllowed && playerActive;
    }
}
