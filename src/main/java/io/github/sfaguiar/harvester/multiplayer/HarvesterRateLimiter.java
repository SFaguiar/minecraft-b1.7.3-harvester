package io.github.sfaguiar.harvester.multiplayer;

/**
 * Sliding-window rate limiter: at most {@link #MAX_TRANSITIONS_PER_WINDOW}
 * accepted calls per any rolling {@link #WINDOW_MILLIS} window. Backed by
 * a fixed-size ring buffer of the last {@value #MAX_TRANSITIONS_PER_WINDOW}
 * acceptance timestamps (matching {@code ARCHITECTURE.md}'s "Per-player
 * server state (design)" — {@code recentTransitionTicks}, ring buffer of
 * size 4).
 *
 * <p>Driven entirely by a caller-supplied millisecond clock — never
 * {@code System.currentTimeMillis()} internally, never
 * {@code Thread.sleep} — so tests exercise it with synthetic timestamps
 * only, the same "testable clock" contract {@link
 * HarvesterSupportStateMachine} already established.
 *
 * <p>Not thread-safe; the server's packet-application thread is
 * confirmed single-threaded (see {@code ARCHITECTURE.md}, "Per-player
 * server state (design)"), so no synchronization is used.
 */
public final class HarvesterRateLimiter {

    /** Fixed at 4/second per the task's decided limit; not configurable in this tranche. */
    public static final int MAX_TRANSITIONS_PER_WINDOW = 4;

    public static final long WINDOW_MILLIS = 1000L;

    private final long[] timestamps = new long[MAX_TRANSITIONS_PER_WINDOW];
    private int count;
    private int oldestIndex;

    /**
     * Attempts to record one acceptance at {@code nowMillis}. Callers
     * must only invoke this for a genuine event to rate-limit (e.g. an
     * actual value change) — a duplicate/no-op event must never reach
     * this method, since every call here consumes one slot of the
     * window regardless of outcome.
     *
     * @return {@code true} if accepted (a slot was free or the oldest
     *         slot had already aged out of the window), {@code false} if
     *         the window is full and the oldest recorded acceptance is
     *         still within {@link #WINDOW_MILLIS}.
     */
    public boolean tryAcquire(long nowMillis) {
        if (count < MAX_TRANSITIONS_PER_WINDOW) {
            timestamps[(oldestIndex + count) % MAX_TRANSITIONS_PER_WINDOW] = nowMillis;
            count++;
            return true;
        }
        long oldest = timestamps[oldestIndex];
        if (nowMillis - oldest >= WINDOW_MILLIS) {
            timestamps[oldestIndex] = nowMillis;
            oldestIndex = (oldestIndex + 1) % MAX_TRANSITIONS_PER_WINDOW;
            return true;
        }
        return false;
    }

    /**
     * Discards every recorded acceptance, as if this limiter were newly
     * constructed — an immediately-following {@link #tryAcquire} sees an
     * empty window regardless of what happened before. Explicit and
     * synchronous; never relies on time passing or any external clock
     * tick.
     */
    public void reset() {
        count = 0;
        oldestIndex = 0;
    }
}
