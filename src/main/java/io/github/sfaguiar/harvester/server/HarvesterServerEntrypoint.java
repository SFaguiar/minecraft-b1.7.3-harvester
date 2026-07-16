package io.github.sfaguiar.harvester.server;

import io.github.sfaguiar.harvester.platform.HarvesterEntrypoint;
import net.fabricmc.api.DedicatedServerModInitializer;

public final class HarvesterServerEntrypoint implements DedicatedServerModInitializer {
    @Override
    public void onInitializeServer() {
        HarvesterEntrypoint.LOGGER.info("Harvester dedicated-server entrypoint loaded.");
    }
}
