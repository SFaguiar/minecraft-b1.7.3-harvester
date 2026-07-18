package io.github.sfaguiar.harvester.client;

import net.minecraft.item.ItemStack;

/**
 * Immutable, primitive-only snapshot of the player's held item at one
 * instant. Exists specifically so the chain executor never retains a live
 * {@link ItemStack} reference across a {@code breakBlock} call — the
 * values are read out immediately at capture time.
 *
 * <p>Used both for the {@code [HARVEST-DURABILITY]} diagnostic log and for
 * {@link #sameIdentityAs(HarvesterHeldItemSnapshot)}, which the chain
 * executor uses to detect a broken or substituted tool between two
 * captures. Wear (damage/count changing while the item identity stays the
 * same) is expected and never counted as a change.
 */
public final class HarvesterHeldItemSnapshot {

    private static final HarvesterHeldItemSnapshot ABSENT =
            new HarvesterHeldItemSnapshot(false, -1, -1, -1, 0);

    private final boolean present;
    private final int itemId;
    private final int damage;
    private final int maxDamage;
    private final int count;

    private HarvesterHeldItemSnapshot(boolean present, int itemId, int damage, int maxDamage, int count) {
        this.present = present;
        this.itemId = itemId;
        this.damage = damage;
        this.maxDamage = maxDamage;
        this.count = count;
    }

    /** Captures {@code heldItem}'s current values, or the empty-hand snapshot for {@code null}. */
    public static HarvesterHeldItemSnapshot capture(ItemStack heldItem) {
        if (heldItem == null) {
            return ABSENT;
        }
        return new HarvesterHeldItemSnapshot(
                true, heldItem.itemId, heldItem.getDamage(), heldItem.getMaxDamage(), heldItem.count
        );
    }

    public boolean present() {
        return present;
    }

    public int itemId() {
        return itemId;
    }

    public int damage() {
        return damage;
    }

    public int maxDamage() {
        return maxDamage;
    }

    public int count() {
        return count;
    }

    /**
     * True when {@code other} is logically the same held item as this
     * snapshot (both absent, or both present with the same
     * {@code itemId}). Damage and count differences are ordinary wear and
     * never make two snapshots "not the same identity" - only presence
     * changing or the item ID changing does (a broken tool disappears; a
     * substituted tool has a different ID).
     */
    public boolean sameIdentityAs(HarvesterHeldItemSnapshot other) {
        if (this.present != other.present) {
            return false;
        }
        if (!this.present) {
            return true;
        }
        return this.itemId == other.itemId;
    }

    @Override
    public String toString() {
        return present
                ? "present=true itemId=" + itemId + " damage=" + damage + " maxDamage=" + maxDamage + " count=" + count
                : "present=false";
    }
}
