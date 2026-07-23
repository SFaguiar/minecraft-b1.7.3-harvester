package io.github.sfaguiar.harvester.game;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntPredicate;
import java.util.function.IntUnaryOperator;

/**
 * Pure merge/split of captured drop stacks — no Minecraft import, so the
 * whole algorithm is unit-testable. Stacks that the game would itself stack
 * (same item id and same damage/metadata, and stackable) are combined and
 * then re-split so no result exceeds that item's maximum stack size;
 * anything the game would not stack (a different item, a different
 * damage/metadata, or a non-stackable item such as a tool) is never merged.
 *
 * <p>The caller adapts real {@code ItemStack}s to {@link Unit}s and supplies
 * the version's own {@code getMaxCount}/{@code isStackable} semantics as
 * lookups, so this class never assumes {@code itemId + damage} equality on
 * its own — it uses exactly what the game reports.
 */
public final class HarvestDropMerge {

    private HarvestDropMerge() {
    }

    /** One captured drop: item id, damage/metadata, and count. Insertion order is preserved for stable output. */
    public static final class Unit {
        public final int itemId;
        public final int damage;
        public final int count;

        public Unit(int itemId, int damage, int count) {
            this.itemId = itemId;
            this.damage = damage;
            this.count = count;
        }

        @Override
        public String toString() {
            return "Unit{id=" + itemId + ", dmg=" + damage + ", count=" + count + "}";
        }
    }

    /**
     * @param input               the captured units, in capture order
     * @param maxCountByItemId     the item's maximum stack size ({@code ItemStack.getMaxCount})
     * @param stackableByItemId    whether the item may stack at all ({@code ItemStack.isStackable})
     * @return merged/split units; each result's count is {@code >= 1} and, for
     *         stackable items, {@code <= maxCount}. Order: stackable groups in
     *         first-seen order, each split into full-then-remainder stacks;
     *         non-stackable units passed through in their original order.
     */
    public static List<Unit> merge(List<Unit> input, IntUnaryOperator maxCountByItemId, IntPredicate stackableByItemId) {
        // Group stackable units by (itemId, damage), preserving first-seen order.
        Map<Long, Integer> totals = new LinkedHashMap<>();
        Map<Long, Unit> representatives = new LinkedHashMap<>();
        List<Unit> nonStackable = new ArrayList<>();

        for (Unit unit : input) {
            if (unit.count <= 0) {
                continue;
            }
            if (!stackableByItemId.test(unit.itemId)) {
                nonStackable.add(unit);
                continue;
            }
            long key = (((long) unit.itemId) << 32) ^ (unit.damage & 0xFFFFFFFFL);
            totals.merge(key, unit.count, Integer::sum);
            representatives.putIfAbsent(key, unit);
        }

        List<Unit> out = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : totals.entrySet()) {
            Unit rep = representatives.get(entry.getKey());
            int remaining = entry.getValue();
            int max = Math.max(1, maxCountByItemId.applyAsInt(rep.itemId));
            while (remaining > 0) {
                int take = Math.min(remaining, max);
                out.add(new Unit(rep.itemId, rep.damage, take));
                remaining -= take;
            }
        }
        out.addAll(nonStackable);
        return out;
    }
}
