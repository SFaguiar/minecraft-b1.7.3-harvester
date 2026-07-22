package io.github.sfaguiar.harvester.client;

import io.github.sfaguiar.harvester.platform.HarvesterEntrypoint;
import net.fabricmc.api.ClientModInitializer;

public final class HarvesterClientEntrypoint implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HarvesterConfigState.load();
        HarvesterEntrypoint.LOGGER.info("Harvester client entrypoint loaded.");
    }
}
