package io.github.sfaguiar.harvester.multiplayer;

/**
 * Client-side discovery states for this tranche only. No enabled/active
 * state exists yet — {@code SUPPORT_AVAILABLE_DISABLED} is terminal for
 * this slice; opt-in (a future {@code SUPPORT_AVAILABLE_ENABLED}) is not
 * implemented.
 */
public enum HarvesterSupportState {
    DISCONNECTED,
    SUPPORT_UNKNOWN,
    SUPPORT_UNAVAILABLE,
    SUPPORT_AVAILABLE_DISABLED
}
