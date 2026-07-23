package io.github.sfaguiar.harvester.mixin;

import io.github.sfaguiar.harvester.game.HarvestDropContext;
import io.github.sfaguiar.harvester.game.HarvestDropContexts;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Drop-consolidation capture point. {@code Block.dropStack(World, int, int,
 * int, ItemStack)} is the single per-stack drop spawner every block break
 * funnels through (confirmed by bytecode: {@code afterBreak} → {@code
 * dropStacks} → {@code dropStack} → {@code World.spawnEntity}). While a
 * Harvester operation is capturing, a drop at the exactly-authorized
 * coordinate is diverted into the operation's context and the vanilla spawn
 * is cancelled; every other drop (a different coordinate, a different world,
 * no operation active) proceeds untouched, so nothing outside the chain is
 * affected and vanilla drop calculation is never altered.
 *
 * <p>Common (both sides): singleplayer captures in the client world, the
 * dedicated server in the server world, driven by whichever side's break
 * wrapper opened the context. When no context is open (the overwhelmingly
 * common case) this is a single null-check and returns immediately.
 */
@Mixin(Block.class)
abstract class BlockDropStackMixin {

    @Inject(
            method = "dropStack(Lnet/minecraft/world/World;IIILnet/minecraft/item/ItemStack;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void harvester$captureConsolidatedDrop(
            World world, int x, int y, int z, ItemStack stack, CallbackInfo ci
    ) {
        HarvestDropContext context = HarvestDropContexts.peek();
        if (context != null && context.tryCapture(world, x, y, z, stack)) {
            ci.cancel();
        }
    }
}
