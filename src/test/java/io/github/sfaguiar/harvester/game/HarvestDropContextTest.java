package io.github.sfaguiar.harvester.game;

import net.minecraft.item.ItemStack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Capture gating of a single {@link HarvestDropContext}: only the currently
 * authorized coordinate, in this context's own world, while open, is
 * captured — everything else (a different coordinate, a different world, an
 * unauthorized or closed context) is left to spawn normally. Uses a real
 * {@link ItemStack}, constructed by id/count/damage without touching the
 * item registry (Beta 1.7.3 stacks carry only those three values).
 */
final class HarvestDropContextTest {

    private static final Object WORLD = new Object();
    private static final Object OTHER_WORLD = new Object();
    private static final Object OWNER = new Object();

    private static ItemStack stack() {
        return new ItemStack(1, 1, 0);
    }

    @Test
    void authorizedCoordinate_isCaptured() {
        HarvestDropContext context = new HarvestDropContext(OWNER, WORLD, 10, 20, 30);
        context.authorize(WORLD, 10, 20, 30);
        assertTrue(context.tryCapture(WORLD, 10, 20, 30, stack()));
        assertEquals(1, context.captured().size());
    }

    @Test
    void unauthorizedCoordinate_isNotCaptured() {
        // A side-effect drop (e.g. a torch popping off) at a different position.
        HarvestDropContext context = new HarvestDropContext(OWNER, WORLD, 10, 20, 30);
        context.authorize(WORLD, 10, 20, 30);
        assertFalse(context.tryCapture(WORLD, 11, 20, 30, stack()));
        assertTrue(context.captured().isEmpty());
    }

    @Test
    void differentWorld_isNotCaptured() {
        HarvestDropContext context = new HarvestDropContext(OWNER, WORLD, 10, 20, 30);
        context.authorize(WORLD, 10, 20, 30);
        assertFalse(context.tryCapture(OTHER_WORLD, 10, 20, 30, stack()));
    }

    @Test
    void notAuthorized_isNotCaptured() {
        HarvestDropContext context = new HarvestDropContext(OWNER, WORLD, 10, 20, 30);
        assertFalse(context.tryCapture(WORLD, 10, 20, 30, stack()));
    }

    @Test
    void deauthorize_stopsCapture() {
        HarvestDropContext context = new HarvestDropContext(OWNER, WORLD, 10, 20, 30);
        context.authorize(WORLD, 10, 20, 30);
        context.deauthorize();
        assertFalse(context.tryCapture(WORLD, 10, 20, 30, stack()));
    }

    @Test
    void closed_isNotCaptured() {
        HarvestDropContext context = new HarvestDropContext(OWNER, WORLD, 10, 20, 30);
        context.authorize(WORLD, 10, 20, 30);
        context.close();
        assertFalse(context.tryCapture(WORLD, 10, 20, 30, stack()));
    }

    @Test
    void reauthorizingDifferentCoordinate_movesCaptureWindow() {
        // Mirrors a chain: origin captured, then authorization moves to a candidate.
        HarvestDropContext context = new HarvestDropContext(OWNER, WORLD, 10, 20, 30);
        context.authorize(WORLD, 10, 20, 30);
        assertTrue(context.tryCapture(WORLD, 10, 20, 30, stack()));
        context.deauthorize();
        context.authorize(WORLD, 10, 21, 30);
        assertFalse(context.tryCapture(WORLD, 10, 20, 30, stack()), "origin no longer authorized");
        assertTrue(context.tryCapture(WORLD, 10, 21, 30, stack()), "candidate now authorized");
        assertEquals(2, context.captured().size());
    }
}
