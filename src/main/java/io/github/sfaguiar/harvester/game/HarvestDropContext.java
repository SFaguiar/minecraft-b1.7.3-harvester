package io.github.sfaguiar.harvester.game;

import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * One in-flight consolidation operation: the drops captured for a single
 * Harvester action (origin plus its chain), scoped by owner and world
 * identity, with a single currently-authorized coordinate.
 *
 * <p>Isolation is by reference identity, never by value: {@link #owner} is
 * the breaking player's own interaction-manager instance (one per player,
 * so two players never share a context), and {@link #world} is the exact
 * world object the operation runs against (so two dimensions never cross).
 * Only the coordinate that is currently authorized may be captured, and the
 * authorization is granted just before each individual break and revoked in
 * a {@code finally} — the discovery plan's positions are never authorized in
 * bulk, so a side-effect drop from an unrelated block at another coordinate
 * (a torch popping off, say) is never captured.
 */
public final class HarvestDropContext {

    private final Object owner;
    private final Object world;
    private final int originX;
    private final int originY;
    private final int originZ;
    private final List<ItemStack> captured = new ArrayList<>();

    private boolean authorized;
    private int authX;
    private int authY;
    private int authZ;
    private boolean closed;

    public HarvestDropContext(Object owner, Object world, int originX, int originY, int originZ) {
        this.owner = Objects.requireNonNull(owner, "owner");
        this.world = Objects.requireNonNull(world, "world");
        this.originX = originX;
        this.originY = originY;
        this.originZ = originZ;
    }

    public Object owner() {
        return owner;
    }

    public Object world() {
        return world;
    }

    public boolean isClosed() {
        return closed;
    }

    public int originX() {
        return originX;
    }

    public int originY() {
        return originY;
    }

    public int originZ() {
        return originZ;
    }

    /** Authorizes exactly one coordinate (in this context's world) for capture. */
    public void authorize(Object world, int x, int y, int z) {
        if (this.world != world) {
            // A break in a different world than this operation's own: never authorize it.
            this.authorized = false;
            return;
        }
        this.authorized = true;
        this.authX = x;
        this.authY = y;
        this.authZ = z;
    }

    /** Revokes the current authorization; the next capture attempt fails until re-authorized. */
    public void deauthorize() {
        this.authorized = false;
    }

    /**
     * Captures {@code stack} (as a copy) iff this context is open, the given
     * world is this context's own world by identity, and {@code (x,y,z)} is
     * exactly the currently-authorized coordinate. Returns whether it was
     * captured — the caller cancels the vanilla spawn only on {@code true}.
     */
    public boolean tryCapture(Object world, int x, int y, int z, ItemStack stack) {
        if (closed || !authorized || this.world != world || stack == null) {
            return false;
        }
        if (x != authX || y != authY || z != authZ) {
            return false;
        }
        captured.add(stack.copy());
        return true;
    }

    /** The captured stacks, in capture order. */
    public List<ItemStack> captured() {
        return captured;
    }

    /** Marks this context finished; no further capture is possible. Idempotent. */
    public void close() {
        this.closed = true;
        this.authorized = false;
    }
}
