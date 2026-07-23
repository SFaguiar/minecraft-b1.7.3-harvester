package io.github.sfaguiar.harvester.game;

import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Flushes a finished {@link HarvestDropContext}: merges its captured stacks
 * with {@link HarvestDropMerge} (using the version's own
 * {@code getMaxCount}/{@code isStackable}), then spawns the resulting stacks
 * as item entities at the exact center of the origin block, with no
 * horizontal velocity, so the whole action's drops appear in one tidy pile
 * where the player broke the first block.
 *
 * <p>Vanilla drop <em>calculation</em> is never touched — every captured
 * stack was produced by the ordinary {@code Block.dropStack} path (the mixin
 * only diverts the resulting item entity). Reconstruction copies a captured
 * representative stack (Beta 1.7.3 item stacks carry only id/count/damage —
 * no NBT — so a copy is exact) and sets the merged count. Called exactly
 * once per operation, from the break wrapper's {@code finally}.
 */
public final class HarvestDropConsolidator {

    private HarvestDropConsolidator() {
    }

    public static void flush(HarvestDropContext context) {
        List<ItemStack> captured = context.captured();
        if (captured.isEmpty()) {
            return;
        }
        if (!(context.world() instanceof World world)) {
            return;
        }

        Map<Long, ItemStack> repByKey = new LinkedHashMap<>();
        Map<Integer, Integer> maxCountById = new HashMap<>();
        Map<Integer, Boolean> stackableById = new HashMap<>();
        List<HarvestDropMerge.Unit> units = new ArrayList<>();

        for (ItemStack stack : captured) {
            if (stack == null || stack.count <= 0) {
                continue;
            }
            int damage = stack.getDamage();
            long key = key(stack.itemId, damage);
            repByKey.putIfAbsent(key, stack);
            maxCountById.putIfAbsent(stack.itemId, stack.getMaxCount());
            stackableById.putIfAbsent(stack.itemId, stack.isStackable());
            units.add(new HarvestDropMerge.Unit(stack.itemId, damage, stack.count));
        }

        List<HarvestDropMerge.Unit> merged = HarvestDropMerge.merge(
                units,
                id -> maxCountById.getOrDefault(id, 64),
                id -> stackableById.getOrDefault(id, Boolean.TRUE)
        );

        double cx = context.originX() + 0.5;
        double cy = context.originY() + 0.5;
        double cz = context.originZ() + 0.5;
        for (HarvestDropMerge.Unit unit : merged) {
            ItemStack rep = repByKey.get(key(unit.itemId, unit.damage));
            ItemStack out = rep != null ? rep.copy() : new ItemStack(unit.itemId, unit.count, unit.damage);
            out.count = unit.count;
            ItemEntity entity = new ItemEntity(world, cx, cy, cz, out);
            entity.velocityX = 0.0;
            entity.velocityY = 0.2;
            entity.velocityZ = 0.0;
            entity.pickupDelay = 10;
            world.spawnEntity(entity);
        }
    }

    private static long key(int itemId, int damage) {
        return (((long) itemId) << 32) ^ (damage & 0xFFFFFFFFL);
    }
}
