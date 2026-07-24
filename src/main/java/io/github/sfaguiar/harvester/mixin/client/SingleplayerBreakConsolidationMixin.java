package io.github.sfaguiar.harvester.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.sfaguiar.harvester.client.HarvesterConfigState;
import io.github.sfaguiar.harvester.client.input.HarvesterClientActivationState;
import io.github.sfaguiar.harvester.game.HarvestDropConsolidation;
import net.minecraft.client.InteractionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.SingleplayerInteractionManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Wraps the singleplayer authoritative break with a real {@code try/finally}
 * (MixinExtras {@code @WrapMethod}) so the drop-consolidation context is
 * opened before the origin break — capturing the origin's own drop, which
 * spawns during the vanilla break body, before the observer's {@code RETURN}
 * chain handler runs — and is flushed and closed exactly once afterward, on
 * normal return, a rejected break, or an exception alike. The observer
 * mixin's own discovery/chain still run (nested inside {@code original.call},
 * capturing each candidate under the same context); this mixin only manages
 * the consolidation lifecycle and never changes the break's return value.
 */
@Mixin(SingleplayerInteractionManager.class)
abstract class SingleplayerBreakConsolidationMixin extends InteractionManager {

    private SingleplayerBreakConsolidationMixin(Minecraft minecraft) {
        super(minecraft);
    }

    @WrapMethod(method = "breakBlock(IIII)Z")
    private boolean harvester$consolidateBreak(int x, int y, int z, int direction, Operation<Boolean> original) {
        World world = minecraft != null ? minecraft.world : null;
        PlayerEntity player = minecraft != null ? minecraft.player : null;
        return HarvestDropConsolidation.aroundBreak(
                this, world, player, HarvesterConfigState.current(),
                HarvesterClientActivationState.isActive(),
                x, y, z,
                () -> original.call(x, y, z, direction)
        );
    }
}
