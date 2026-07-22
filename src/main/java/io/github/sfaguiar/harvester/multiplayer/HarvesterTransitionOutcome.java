package io.github.sfaguiar.harvester.multiplayer;

/** Result of {@link HarvesterMultiplayerPlayerState#applyTransition}. */
public enum HarvesterTransitionOutcome {
    /** A genuine value change, accepted within the rate-limit window. */
    APPLIED,
    /** The requested value matched the already-stored one; idempotent, no quota consumed. */
    DUPLICATE_IGNORED,
    /** Rejected by the rate limiter; {@code active} was forced to {@code false}. */
    RATE_LIMITED
}
