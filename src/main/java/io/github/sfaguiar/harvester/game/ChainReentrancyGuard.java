package io.github.sfaguiar.harvester.game;

/**
 * Per-instance reentrancy guard for a break-chain trigger. {@link
 * #tryEnter()} returns {@code false} when already inside; a caller that
 * receives {@code true} owns the guard until it calls {@link #exit()},
 * which callers must always do from a {@code finally} block wrapping the
 * entire chain call so an exception partway through can never leave the
 * guard stuck on.
 *
 * <p>No Minecraft/StationAPI import, no persistence, no thread-local
 * global: intended to be held as a single {@code @Unique} field on the
 * per-connection interaction-manager Mixin it guards (one manager instance
 * per player), so distinct players never share or interfere with one
 * another's guard — matching the equivalent inline boolean field already
 * used by {@code SingleplayerInteractionManagerObserverMixin} for the
 * singleplayer case, extracted here so its exact semantics are unit
 * testable. Not thread-safe by design: both the client and server break
 * pipelines this guards only ever run on their own single main thread.
 */
public final class ChainReentrancyGuard {

    private boolean inside;

    /** True and marks the guard held, only if not already held; false (no state change) otherwise. */
    public boolean tryEnter() {
        if (inside) {
            return false;
        }
        inside = true;
        return true;
    }

    /** Releases the guard. Idempotent — calling this when not held is a no-op. */
    public void exit() {
        inside = false;
    }

    public boolean isInside() {
        return inside;
    }
}
