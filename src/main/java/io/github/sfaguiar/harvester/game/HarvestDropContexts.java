package io.github.sfaguiar.harvester.game;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Per-thread stack of in-flight {@link HarvestDropContext}s. Beta 1.7.3's
 * client and server each run their break/tick loop on a single thread, so a
 * {@link ThreadLocal} deque both keeps singleplayer's client thread and the
 * internal/dedicated server thread completely separate and supports nested
 * operations defensively (the deepest — top — context is the one a drop
 * belongs to). The {@code Block.dropStack} mixin consults only {@link #peek()};
 * the break wrapper owns {@link #push}/{@link #pop}. Reentrant chain breaks
 * do <em>not</em> push a new context — they authorize a coordinate on the
 * existing top — so in practice the stack depth is one per real operation.
 */
public final class HarvestDropContexts {

    private static final ThreadLocal<Deque<HarvestDropContext>> STACK = ThreadLocal.withInitial(ArrayDeque::new);

    private HarvestDropContexts() {
    }

    public static void push(HarvestDropContext context) {
        STACK.get().push(context);
    }

    /** The current (top) context, or {@code null} when none is open on this thread. */
    public static HarvestDropContext peek() {
        return STACK.get().peek();
    }

    /** Removes and returns the top context, or {@code null} if the stack is empty. */
    public static HarvestDropContext pop() {
        return STACK.get().poll();
    }

    public static boolean isEmpty() {
        return STACK.get().isEmpty();
    }
}
