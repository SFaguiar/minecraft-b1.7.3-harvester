package io.github.sfaguiar.harvester.multiplayer;

/**
 * Client-side discovery/opt-in states — see {@code ARCHITECTURE.md},
 * "Client support state machine (design)". {@code
 * SUPPORT_AVAILABLE_DISABLED} means a valid announcement was received
 * but the server disallows multiplayer support or the player has not
 * opted in for this server; {@code SUPPORT_AVAILABLE_ENABLED} means both
 * are true, and holding the activation key sends {@code
 * harvester:active}.
 */
public enum HarvesterSupportState {
    DISCONNECTED,
    SUPPORT_UNKNOWN,
    SUPPORT_UNAVAILABLE,
    SUPPORT_AVAILABLE_DISABLED,
    SUPPORT_AVAILABLE_ENABLED
}
