package io.github.sfaguiar.harvester.multiplayer;

/**
 * Pure per-player transitory multiplayer state — see {@code
 * ARCHITECTURE.md}, "Per-player server state (design)". Deliberately has
 * no Minecraft/StationAPI import: the {@code ServerPlayerEntity}-keyed
 * registry that owns one instance of this per connected player lives in
 * {@code server.multiplayer} instead, so the rate-limit/idempotency
 * rules here stay unit-testable with a fake clock.
 *
 * <p>Default state on creation: {@code supportAnnounced = false},
 * {@code active = false} — matching "server is the sole writer of
 * {@code active}" and "default active=false on creation".
 */
public final class HarvesterMultiplayerPlayerState {

    private final HarvesterRateLimiter rateLimiter = new HarvesterRateLimiter();

    private boolean supportAnnounced;
    private boolean active;
    private long lastRateLimitWarningAtMillis = -1L;

    /** Called once the server has actually sent {@code harvester:support} to this player. */
    public void markSupportAnnounced() {
        supportAnnounced = true;
    }

    public boolean supportAnnounced() {
        return supportAnnounced;
    }

    public boolean active() {
        return active;
    }

    /**
     * World/dimension boundary. Splits per-player state into two
     * distinct scopes and resets only the second:
     *
     * <ul>
     *   <li><b>Connection-scoped, preserved:</b> {@code supportAnnounced}.
     *   The StationAPI connection itself does not change on a dimension
     *   change — the server already announced {@code harvester:support}
     *   to it exactly once, and nothing re-sends that announcement on a
     *   world change. Clearing this here would permanently lock the
     *   player out of {@code harvester:active} for the rest of that
     *   connection the first time they cross a dimension boundary, with
     *   no re-announcement protocol to recover — a real regression, not
     *   a stricter guarantee. Do not remove this preservation without
     *   also designing a reliable re-announce protocol.</li>
     *   <li><b>Operational, reset in full:</b> {@code active} (a player
     *   mid-activation in one world must not be reported {@code active}
     *   in another without pressing the key again there), the rate
     *   limiter's entire window ({@link HarvesterRateLimiter#reset()} —
     *   a fresh sequence of up to 4 real transitions is accepted in the
     *   new world regardless of what happened in the old one), and the
     *   warning-suppression timestamp (a stale warning-just-shown state
     *   from the old world must not suppress a genuine first warning in
     *   the new one).</li>
     * </ul>
     *
     * <p>Mirrors tranche 1's own original design record ("Reset to false
     * (not removed) on respawn and on dimension change") for the
     * dimension-change case specifically; respawn is handled instead by
     * removing the pre-respawn instance's entire entry (see {@code
     * HarvesterMultiplayerServerRegistry}'s own documentation), since
     * respawn constructs a genuinely new instance and there is nothing to
     * preserve.
     */
    public void resetOnWorldChange() {
        active = false;
        rateLimiter.reset();
        lastRateLimitWarningAtMillis = -1L;
    }

    /**
     * Applies a client-requested {@code active} value at {@code nowMillis}.
     * A repeat of the already-stored value is idempotent and never
     * consumes rate-limit quota. A genuine change is subject to the
     * sliding-window rate limit; a rejected change forces {@code active}
     * to {@code false} (never leaves the previous value in place) per
     * the task's "quinta transição... força active=false" rule.
     */
    public HarvesterTransitionOutcome applyTransition(boolean newActive, long nowMillis) {
        if (active == newActive) {
            return HarvesterTransitionOutcome.DUPLICATE_IGNORED;
        }
        if (!rateLimiter.tryAcquire(nowMillis)) {
            active = false;
            return HarvesterTransitionOutcome.RATE_LIMITED;
        }
        active = newActive;
        return HarvesterTransitionOutcome.APPLIED;
    }

    /**
     * Whether a rate-limit warning should be emitted now — at most one
     * per window. Call only immediately after a {@link
     * HarvesterTransitionOutcome#RATE_LIMITED} outcome; each call that
     * returns {@code true} also arms the next window's suppression.
     */
    public boolean shouldWarnRateLimit(long nowMillis) {
        if (lastRateLimitWarningAtMillis < 0 || nowMillis - lastRateLimitWarningAtMillis >= HarvesterRateLimiter.WINDOW_MILLIS) {
            lastRateLimitWarningAtMillis = nowMillis;
            return true;
        }
        return false;
    }
}
