package io.github.sfaguiar.harvester.client.input;

/**
 * Client-side hold-to-activate state for the Harvester activation key.
 *
 * <p>Deliberately tiny: a single boolean, starting {@code false}, updated
 * <em>only</em> by {@link HarvesterKeyBindingListener} (the setter is
 * package-private on purpose) and read by the execution path after a
 * completed manual break. This class never performs any action itself —
 * key events only update state here; the break observer consults it.
 *
 * <p>Scope limits of this vertical slice, all deliberate: no persistence,
 * no per-server state, no configuration file, no exposure of the
 * {@code KeyBinding} object itself, no dependency on {@code core}. The
 * client game loop is single-threaded for input and block breaking, so no
 * synchronization is used.
 */
public final class HarvesterClientActivationState {

    private static boolean activationKeyHeld = false;

    private HarvesterClientActivationState() {
    }

    /** Updated only by {@link HarvesterKeyBindingListener}. */
    static void setActivationKeyHeld(boolean held) {
        activationKeyHeld = held;
    }

    /** True only while the activation key is currently held in-game. */
    public static boolean isActive() {
        return activationKeyHeld;
    }
}
