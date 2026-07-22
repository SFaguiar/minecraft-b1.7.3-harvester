package io.github.sfaguiar.harvester.game;

import io.github.sfaguiar.harvester.core.HarvestGroupKind;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.modificationstation.stationapi.api.block.BlockState;

/**
 * Authoritative tool-harvestability gate for ore chains — never a hardcoded
 * "is this a pickaxe" check by class or item ID. Side-agnostic: takes a
 * {@link PlayerEntity}/{@link World} directly (never {@code Minecraft}), so
 * both {@code ServerPlayerEntity}/{@code ServerWorld} (multiplayer) and the
 * client's own player/world (singleplayer) call this exact method — see
 * {@code ARCHITECTURE.md}, "Reuse vs new adapter": "Tool-harvestability
 * reuses the same {@code ItemStack.isSuitableFor(...)} call verified for
 * singleplayer — nothing about that API is client-only."
 *
 * <p>Delegates to {@code ItemStack.isSuitableFor(PlayerEntity, BlockView,
 * BlockPos, BlockState)}, the StationAPI method that decides whether a
 * held item can correctly harvest a block (confirmed by bytecode: the
 * mapped {@code net.minecraft.item.ItemStack} class implements
 * {@code net.modificationstation.stationapi.api.item.StationFlatteningItemStack},
 * whose Mixin-provided override
 * ({@code net.modificationstation.stationapi.mixin.flattening.ItemMixin})
 * delegates, for an ordinary item, to the original vanilla per-item
 * harvestability check — the same authority the normal {@code breakBlock}
 * pipeline itself consults for drops). {@code World implements BlockView}
 * and {@code BlockPos(int,int,int)} both confirmed by bytecode inspection
 * of the pinned merged jar before this was written.
 */
public final class HarvestToolCompatibility {

    private HarvestToolCompatibility() {
    }

    /** Logs never gate on tool; every ore group kind does. Pure — no Minecraft/StationAPI call. */
    public static boolean requiresToolCheck(HarvestGroupKind kind) {
        return kind != HarvestGroupKind.LOGS;
    }

    /**
     * Whether {@code heldItem} (possibly {@code null} for an empty hand) can
     * correctly harvest {@code state} at {@code (x, y, z)} right now. An
     * empty hand is never suitable for an ore-gated block — vanilla ore
     * materials always require a real tool for a correct harvest, and
     * {@code isSuitableFor} is an instance method with no null-safe overload
     * to fall back to.
     */
    public static boolean canHarvest(
            PlayerEntity player, World world, ItemStack heldItem, int x, int y, int z, BlockState state
    ) {
        if (heldItem == null || player == null || world == null) {
            return false;
        }
        BlockPos pos = new BlockPos(x, y, z);
        return heldItem.isSuitableFor(player, world, pos, state);
    }
}
