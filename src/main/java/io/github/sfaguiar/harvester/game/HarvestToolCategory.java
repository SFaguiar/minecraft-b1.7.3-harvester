package io.github.sfaguiar.harvester.game;

import io.github.sfaguiar.harvester.core.HarvestGroupKind;
import net.minecraft.item.AxeItem;
import net.minecraft.item.HoeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShearsItem;
import net.minecraft.item.ShovelItem;

import java.util.Optional;
import java.util.Set;

/**
 * The tool <em>category</em> a group kind requires to <em>start</em> its
 * automatic chain — a Harvester product rule, distinct from vanilla
 * harvestability ({@code ItemStack.isSuitableFor}, which the ore kinds use
 * instead and which this enum never touches).
 *
 * <p>Recognition is a union of sources, so a mod tool that neither subclasses
 * the vanilla base nor is listed still cannot chain, but one that does either
 * can:
 * <ol>
 *   <li>the vanilla base class ({@link AxeItem}/{@link ShovelItem}/
 *   {@link ShearsItem}/{@link HoeItem}) — covers vanilla and any mod tool
 *   that extends it;</li>
 *   <li>a configured allowlist of item IDs — the escape hatch for a mod
 *   tool that does not extend the vanilla base.</li>
 * </ol>
 * StationAPI tool tags were checked for on the pinned {@code 2.0.0-alpha.6.2}
 * platform and do not exist (only block tags do), so they are not a source
 * here; if a future StationAPI adds them they would slot in ahead of the
 * class check without changing this contract.
 *
 * <p>Ore kinds map to {@link Optional#empty()}: they are gated by
 * {@code ItemStack.isSuitableFor} (pickaxe tier), never by this category.
 */
public enum HarvestToolCategory {

    AXE {
        @Override
        boolean isVanillaToolItem(Item item) {
            return item instanceof AxeItem;
        }
    },
    SHOVEL {
        @Override
        boolean isVanillaToolItem(Item item) {
            return item instanceof ShovelItem;
        }
    },
    SHEARS {
        @Override
        boolean isVanillaToolItem(Item item) {
            return item instanceof ShearsItem;
        }
    },
    HOE {
        @Override
        boolean isVanillaToolItem(Item item) {
            return item instanceof HoeItem;
        }
    };

    abstract boolean isVanillaToolItem(Item item);

    /**
     * The tool category that must be held to <em>start</em> a chain of
     * {@code kind}, or empty for the ore kinds (which use
     * {@code isSuitableFor} instead).
     */
    public static Optional<HarvestToolCategory> requiredFor(HarvestGroupKind kind) {
        switch (kind) {
            case LOGS:
                return Optional.of(AXE);
            case DIRT:
            case GRAVEL:
                return Optional.of(SHOVEL);
            case LEAVES:
                return Optional.of(SHEARS);
            case CROPS:
                return Optional.of(HOE);
            default:
                return Optional.empty();
        }
    }

    /**
     * Whether {@code heldItem} satisfies this category — a real (non-null)
     * stack whose item either is the vanilla base class or whose item ID is
     * in {@code allowlistIds}. An empty hand never satisfies any category.
     *
     * @param allowlistIds the configured allowlist for <em>this</em> category
     *                     (e.g. {@code toolAxeIds} for {@link #AXE}); never
     *                     {@code null}
     */
    public boolean matches(ItemStack heldItem, Set<Integer> allowlistIds) {
        if (heldItem == null) {
            return false;
        }
        Item item = heldItem.getItem();
        if (item == null) {
            return false;
        }
        return isVanillaToolItem(item) || allowlistIds.contains(heldItem.itemId);
    }
}
