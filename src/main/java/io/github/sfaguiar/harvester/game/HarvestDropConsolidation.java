package io.github.sfaguiar.harvester.game;

import io.github.sfaguiar.harvester.config.HarvesterConfig;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import net.modificationstation.stationapi.api.block.BlockState;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Side-agnostic orchestration of one break's drop-consolidation lifecycle,
 * wrapped around the authoritative break method by the client and server
 * mixins with a real {@code try/finally} (MixinExtras {@code @WrapMethod}).
 * The origin's own break — whose vanilla {@code dropStack} runs before any
 * observer {@code RETURN} handler could — is covered because the context is
 * opened <em>before</em> {@code originalBreak} runs; the chain executes
 * nested inside that same call (the observer's own {@code RETURN} handler),
 * so candidate drops are captured under the same context; and the single
 * flush plus cleanup happen in the {@code finally}, on normal return,
 * rejection, or exception alike.
 *
 * <p>Just-in-time authorization: each invocation (origin or a reentrant
 * candidate break) authorizes only its own coordinate and revokes it in the
 * {@code finally}; the plan's positions are never authorized in bulk. Only
 * the outermost invocation for an owner opens/flushes/closes the context —
 * reentrant candidate breaks reuse the open top-of-stack context.
 */
public final class HarvestDropConsolidation {

    private HarvestDropConsolidation() {
    }

    /**
     * @param owner        the breaking player's interaction-manager instance
     *                     (identity key; one per player)
     * @param world        the operation's world (identity key)
     * @param player       the breaking player (for origin eligibility)
     * @param config       the authoritative config for this side
     * @param mayHarvest   whether this player may harvest here at all right
     *                     now (client: activation key held; server: the full
     *                     server-authoritative eligibility gate)
     * @param originalBreak runs the wrapped vanilla break (and, nested, the
     *                     observer's chain)
     * @return the wrapped break's own return value, unchanged
     */
    public static boolean aroundBreak(
            Object owner,
            World world,
            PlayerEntity player,
            HarvesterConfig config,
            boolean mayHarvest,
            int x,
            int y,
            int z,
            BooleanSupplier originalBreak
    ) {
        return runAround(
                owner, world, x, y, z,
                () -> maybeOpen(owner, world, player, config, mayHarvest, x, y, z),
                HarvestDropConsolidator::flush,
                originalBreak
        );
    }

    /**
     * The pure lifecycle core — no Minecraft dependency of its own (the
     * Minecraft-touching decisions come in as {@code opener}/{@code flush}),
     * so it is unit-testable: reentrancy detection, just-in-time
     * authorization, single open/flush/close, and cleanup on normal return,
     * rejection, and exception. {@code opener} returns a fresh context to
     * open, or {@code null} to run without consolidation; {@code flush} is
     * invoked exactly once per opened context, in the {@code finally}, before
     * the context is closed.
     */
    static boolean runAround(
            Object owner,
            Object world,
            int x,
            int y,
            int z,
            Supplier<HarvestDropContext> opener,
            Consumer<HarvestDropContext> flush,
            BooleanSupplier originalBreak
    ) {
        HarvestDropContext top = HarvestDropContexts.peek();
        boolean reentrant = top != null && !top.isClosed() && top.owner() == owner;

        HarvestDropContext context;
        boolean opened = false;
        if (reentrant) {
            context = top;
        } else {
            context = opener.get();
            if (context != null) {
                HarvestDropContexts.push(context);
                opened = true;
            }
        }

        if (context != null) {
            context.authorize(world, x, y, z);
        }
        try {
            return originalBreak.getAsBoolean();
        } finally {
            if (context != null) {
                context.deauthorize();
            }
            if (opened) {
                HarvestDropContexts.pop();
                try {
                    flush.accept(context);
                } finally {
                    context.close();
                }
            }
        }
    }

    private static HarvestDropContext maybeOpen(
            Object owner, World world, PlayerEntity player, HarvesterConfig config, boolean mayHarvest,
            int x, int y, int z
    ) {
        if (world == null || player == null || !mayHarvest) {
            return null;
        }
        if (!config.enabled() || !config.consolidateDrops()) {
            return null;
        }
        BlockState state = world.getBlockState(x, y, z);
        int meta = world.getBlockMeta(x, y, z);
        if (!HarvestDiscoveryAdapter.isOriginEligible(config, player, world, x, y, z, meta, state)) {
            return null;
        }
        return new HarvestDropContext(owner, world, x, y, z);
    }
}
