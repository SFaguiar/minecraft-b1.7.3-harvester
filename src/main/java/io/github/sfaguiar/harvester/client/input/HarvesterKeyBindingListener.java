package io.github.sfaguiar.harvester.client.input;

import io.github.sfaguiar.harvester.platform.HarvesterEntrypoint;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.mine_diver.unsafeevents.listener.EventListener;
import net.minecraft.client.option.KeyBinding;
import net.modificationstation.stationapi.api.client.event.keyboard.KeyStateChangedEvent;
import net.modificationstation.stationapi.api.client.event.option.KeyBindingRegisterEvent;
import org.lwjgl.input.Keyboard;

/**
 * Registers the Harvester activation key (default: V, user-rebindable via
 * the vanilla controls screen) and tracks its held/released state in
 * {@link HarvesterClientActivationState}.
 *
 * <p>Registered under the {@code stationapi:event_bus_client} entrypoint in
 * {@code fabric.mod.json}, following the exact pattern of StationAPI's own
 * test listeners ({@code sltest.option.OptionListener} /
 * {@code sltest.keyboard.KeyboardListener} in the pinned
 * {@code 2.0.0-alpha.6.2} source): {@link KeyBindingRegisterEvent} adds the
 * binding to the options screen list, and {@link KeyStateChangedEvent}
 * handlers compare {@link Keyboard#getEventKey()} against the binding's
 * public {@code code} field (so a user rebind is respected without any
 * extra plumbing).
 *
 * <p>Semantics, per this vertical slice's contract:
 * <ul>
 *   <li>press while {@code IN_GAME} → active;</li>
 *   <li>press while {@code IN_GUI} → ignored (GUIs never activate);</li>
 *   <li>release in any environment → inactive (so a key released while a
 *   GUI is open can never leave activation stuck on);</li>
 *   <li>the event handler only updates state — it never executes any
 *   break; the break observer consults the state after a completed
 *   manual break;</li>
 *   <li>state transitions are logged at DEBUG only, and only on an
 *   actual change, never per repeated event.</li>
 * </ul>
 */
@Environment(EnvType.CLIENT)
public class HarvesterKeyBindingListener {

    private static KeyBinding activateKeyBinding;

    @EventListener
    public void registerKeyBindings(KeyBindingRegisterEvent event) {
        activateKeyBinding = new KeyBinding("key.harvester.activate", Keyboard.KEY_V);
        event.register(activateKeyBinding);
        HarvesterEntrypoint.LOGGER.info(
                "[HARVEST-EXEC] Activation key binding registered (default: V)."
        );
    }

    @EventListener
    public void keyStateChanged(KeyStateChangedEvent event) {
        if (activateKeyBinding == null) {
            return;
        }
        if (Keyboard.getEventKey() != activateKeyBinding.code) {
            return;
        }

        boolean pressed = Keyboard.getEventKeyState();
        if (pressed && event.environment != KeyStateChangedEvent.Environment.IN_GAME) {
            // A press inside a GUI must never activate the Harvester.
            return;
        }

        if (HarvesterClientActivationState.isActive() != pressed) {
            HarvesterClientActivationState.setActivationKeyHeld(pressed);
            HarvesterEntrypoint.LOGGER.debug(
                    "[HARVEST-EXEC] Activation state changed: held={}", pressed
            );
        }
    }
}
