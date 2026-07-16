package io.github.sfaguiar.harvester.platform;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HarvesterEntrypoint implements ModInitializer {
    public static final String MOD_ID = "harvester";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Harvester StationAPI foundation loaded; gameplay features are not enabled.");
    }
}
