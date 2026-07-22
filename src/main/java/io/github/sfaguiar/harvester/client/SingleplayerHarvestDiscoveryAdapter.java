package io.github.sfaguiar.harvester.client;

import io.github.sfaguiar.harvester.game.HarvestDiscoveryAdapter;
import io.github.sfaguiar.harvester.game.HarvestDiscoveryOutcome;
import net.minecraft.client.Minecraft;
import net.minecraft.world.World;
import net.modificationstation.stationapi.api.block.BlockState;

/**
 * Thin singleplayer/client adapter over {@link HarvestDiscoveryAdapter} —
 * the side-agnostic implementation in {@code game} that actually runs
 * classification and BFS discovery. This class exists only to supply the
 * client's own {@link Minecraft}-sourced {@code player}/{@code
 * HarvesterConfigState}, never to re-implement or diverge from the shared
 * logic (see {@code ARCHITECTURE.md}, "Reuse vs new adapter" — the
 * multiplayer server adapter calls the exact same {@link
 * HarvestDiscoveryAdapter} method with its own player/world/config).
 */
public final class SingleplayerHarvestDiscoveryAdapter {

    private SingleplayerHarvestDiscoveryAdapter() {
    }

    /**
     * @param preBreakBlockId diagnostic metadata only, never used to decide
     *                        membership
     * @param preBreakState   the origin's own {@link BlockState}, captured
     *                        by the caller before the break completed —
     *                        never StationAPI's private per-mixin cached
     *                        state field
     * @return {@code null} when the origin is ineligible (neither a log nor
     *         an ore), its group kind is gated off by configuration, or —
     *         for an ore group — the currently held item cannot correctly
     *         harvest the origin; discovery never runs in any of those
     *         cases
     */
    public static HarvestDiscoveryOutcome discoverForCompletedBreak(
            Minecraft minecraft,
            World world,
            int originX,
            int originY,
            int originZ,
            int preBreakBlockId,
            BlockState preBreakState
    ) {
        return HarvestDiscoveryAdapter.discoverForCompletedBreak(
                HarvesterConfigState.current(),
                minecraft.player,
                world,
                originX,
                originY,
                originZ,
                preBreakBlockId,
                preBreakState
        );
    }
}
