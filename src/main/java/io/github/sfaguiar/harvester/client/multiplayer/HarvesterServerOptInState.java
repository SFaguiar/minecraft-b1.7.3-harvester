package io.github.sfaguiar.harvester.client.multiplayer;

import io.github.sfaguiar.harvester.config.HarvesterServerOptInLoader;
import io.github.sfaguiar.harvester.multiplayer.HarvesterServerIdentity;
import io.github.sfaguiar.harvester.platform.HarvesterEntrypoint;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

/**
 * Resolves the player's per-server {@code multiplayerOptIn} preference,
 * loaded fresh from disk exactly once per connection — call only when a
 * {@code harvester:support} announcement actually arrives (never
 * hot-reloaded mid-session). Missing file → created with {@code false};
 * invalid file → {@code false} plus a logged warning; no connection
 * address captured yet → {@code false} plus a logged warning (defensive;
 * should not happen in real wiring, since an announcement can only
 * arrive after {@code ClientNetworkHandlerConnectMixin} has already
 * fired for this connection).
 */
public final class HarvesterServerOptInState {

    private HarvesterServerOptInState() {
    }

    public static boolean resolveOptIn() {
        return HarvesterConnectionAddressState.current()
                .map(HarvesterServerOptInState::loadFor)
                .orElseGet(() -> {
                    HarvesterEntrypoint.LOGGER.warn(
                            "[HARVEST-MP] No connection address captured; treating per-server opt-in as disabled."
                    );
                    return false;
                });
    }

    private static boolean loadFor(HarvesterServerIdentity identity) {
        Path file = FabricLoader.getInstance().getConfigDir()
                .resolve("harvester").resolve("servers").resolve(identity.fileName());
        HarvesterServerOptInLoader.LoadResult result =
                HarvesterServerOptInLoader.loadOrCreateDefault(file, identity.normalizedAddress());
        for (String warning : result.warnings()) {
            HarvesterEntrypoint.LOGGER.warn("[HARVEST-MP] {}", warning);
        }
        return result.optIn();
    }
}
