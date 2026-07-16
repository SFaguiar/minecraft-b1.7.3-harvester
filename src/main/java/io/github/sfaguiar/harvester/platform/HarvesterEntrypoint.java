package io.github.sfaguiar.harvester.platform;

import net.fabricmc.api.ModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class HarvesterEntrypoint implements ModInitializer {
    public static final String MOD_ID = "harvester";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Harvester StationAPI foundation loaded; gameplay features are not enabled.");
    }
}
