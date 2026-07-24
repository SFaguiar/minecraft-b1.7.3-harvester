package io.github.sfaguiar.harvester.mixin.server;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.sfaguiar.harvester.game.HarvestDropConsolidation;
import io.github.sfaguiar.harvester.multiplayer.HarvesterMultiplayerPlayerState;
import io.github.sfaguiar.harvester.server.HarvesterServerConfigState;
import io.github.sfaguiar.harvester.server.multiplayer.HarvesterMultiplayerServerRegistry;
import io.github.sfaguiar.harvester.server.multiplayer.ServerHarvestChainGate;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.world.ServerWorld;
import net.modificationstation.stationapi.api.network.ModdedPacketHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Optional;

/**
 * Server-authoritative counterpart of {@code
 * SingleplayerBreakConsolidationMixin}: wraps {@code tryBreakBlock} with a
 * real {@code try/finally} so the origin's own drop (spawned during the
 * vanilla break, before the observer's chain handler) is captured, and the
 * consolidated pile is flushed once at the origin center. The consolidation
 * context is opened only when the full server-authoritative eligibility gate
 * passes ({@link ServerHarvestChainGate} — same connection-modded /
 * support-announced / {@code multiplayerAllowed} / active checks the chain
 * itself uses), so a client that has not legitimately activated never causes
 * server-side drop relocation. Server authority is untouched: this never
 * trusts any client-supplied value and only diverts item entities the server
 * itself produced.
 */
@Mixin(ServerPlayerInteractionManager.class)
abstract class ServerPlayerInteractionManagerConsolidationMixin {

    @Shadow
    public PlayerEntity player;

    @Shadow
    private ServerWorld world;

    @WrapMethod(method = "tryBreakBlock(III)Z")
    private boolean harvester$consolidateBreak(int x, int y, int z, Operation<Boolean> original) {
        boolean mayHarvest = false;
        if (player instanceof ServerPlayerEntity serverPlayer) {
            boolean connectionModded = serverPlayer.networkHandler instanceof ModdedPacketHandler moddedHandler
                    && moddedHandler.isModded();
            Optional<HarvesterMultiplayerPlayerState> state = HarvesterMultiplayerServerRegistry.get(serverPlayer);
            mayHarvest = ServerHarvestChainGate.isEligible(
                    connectionModded,
                    state.map(HarvesterMultiplayerPlayerState::supportAnnounced).orElse(false),
                    HarvesterServerConfigState.current().multiplayerAllowed(),
                    state.map(HarvesterMultiplayerPlayerState::active).orElse(false)
            );
        }
        return HarvestDropConsolidation.aroundBreak(
                this, world, player, HarvesterServerConfigState.current(), mayHarvest, x, y, z,
                () -> original.call(x, y, z)
        );
    }
}
