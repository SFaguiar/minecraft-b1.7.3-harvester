package io.github.sfaguiar.harvester.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class HarvesterConfigLoaderTest {

    @Test
    void parse_emptyProperties_usesAllDefaults() {
        HarvesterConfigLoader.LoadResult result = HarvesterConfigLoader.parse(new Properties());

        assertEquals(HarvesterConfig.DEFAULT_ENABLED, result.config().enabled());
        assertEquals(HarvesterConfig.DEFAULT_MAX_CHAIN, result.config().maxChain());
        assertEquals(HarvesterConfig.DEFAULT_NEIGHBORHOOD, result.config().neighborhood());
        assertEquals(HarvesterConfig.DEFAULT_DIAGNOSTIC_LOGGING, result.config().diagnosticLogging());
        assertEquals(HarvesterConfig.DEFAULT_HARVEST_LOGS, result.config().harvestLogs());
        assertEquals(HarvesterConfig.DEFAULT_HARVEST_ORES, result.config().harvestOres());
        assertEquals(HarvesterConfig.DEFAULT_MULTIPLAYER_ALLOWED, result.config().multiplayerAllowed());
        assertTrue(result.warnings().isEmpty());
    }

    @Test
    void parse_multiplayerAllowedTrue_isHonored() {
        Properties properties = new Properties();
        properties.setProperty("multiplayerAllowed", "true");

        HarvesterConfigLoader.LoadResult result = HarvesterConfigLoader.parse(properties);

        assertTrue(result.config().multiplayerAllowed());
        assertTrue(result.warnings().isEmpty());
    }

    @Test
    void parse_multiplayerAllowedFalse_isHonored() {
        Properties properties = new Properties();
        properties.setProperty("multiplayerAllowed", "false");

        HarvesterConfigLoader.LoadResult result = HarvesterConfigLoader.parse(properties);

        assertFalse(result.config().multiplayerAllowed());
        assertTrue(result.warnings().isEmpty());
    }

    @Test
    void parse_invalidMultiplayerAllowed_fallsBackToDefaultWithOneWarning() {
        Properties properties = new Properties();
        properties.setProperty("multiplayerAllowed", "not-a-boolean");

        HarvesterConfigLoader.LoadResult result = HarvesterConfigLoader.parse(properties);

        assertEquals(HarvesterConfig.DEFAULT_MULTIPLAYER_ALLOWED, result.config().multiplayerAllowed());
        assertEquals(1, result.warnings().size());
    }

    @Test
    void loadOrCreateDefaults_missingFile_createsItAndReturnsDefaults(@TempDir Path dir) throws IOException {
        Path configFile = dir.resolve("harvester.properties");

        HarvesterConfigLoader.LoadResult result = HarvesterConfigLoader.loadOrCreateDefaults(configFile);

        assertTrue(Files.exists(configFile));
        assertEquals(HarvesterConfig.DEFAULTS.enabled(), result.config().enabled());
        assertEquals(HarvesterConfig.DEFAULTS.maxChain(), result.config().maxChain());
        assertEquals(HarvesterConfig.DEFAULTS.neighborhood(), result.config().neighborhood());
        assertEquals(HarvesterConfig.DEFAULTS.diagnosticLogging(), result.config().diagnosticLogging());
        assertTrue(result.warnings().isEmpty());

        String written = Files.readString(configFile, StandardCharsets.UTF_8);
        assertTrue(written.contains("enabled=true"));
        assertTrue(written.contains("maxChain=64"));
        assertTrue(written.contains("neighborhood=legacy_26"));
        assertTrue(written.contains("diagnosticLogging=false"));
        assertTrue(written.contains("harvestLogs=true"));
        assertTrue(written.contains("harvestOres=true"));
        assertTrue(written.contains("multiplayerAllowed=false"));
    }

    @Test
    void loadOrCreateDefaults_missingParentDirectory_isCreated(@TempDir Path dir) {
        Path configFile = dir.resolve("nested").resolve("config").resolve("harvester.properties");

        HarvesterConfigLoader.LoadResult result = HarvesterConfigLoader.loadOrCreateDefaults(configFile);

        assertTrue(Files.exists(configFile));
        assertEquals(HarvesterConfig.DEFAULTS.maxChain(), result.config().maxChain());
    }

    @Test
    void parse_validValues_areAllHonored() {
        Properties properties = new Properties();
        properties.setProperty("enabled", "false");
        properties.setProperty("maxChain", "32");
        properties.setProperty("neighborhood", "orthogonal_6");
        properties.setProperty("diagnosticLogging", "true");
        properties.setProperty("harvestLogs", "false");
        properties.setProperty("harvestOres", "false");

        HarvesterConfigLoader.LoadResult result = HarvesterConfigLoader.parse(properties);

        assertFalse(result.config().enabled());
        assertEquals(32, result.config().maxChain());
        assertEquals(NeighborhoodChoice.ORTHOGONAL_6, result.config().neighborhood());
        assertTrue(result.config().diagnosticLogging());
        assertFalse(result.config().harvestLogs());
        assertFalse(result.config().harvestOres());
        assertTrue(result.warnings().isEmpty());
    }

    @Test
    void parse_harvestLogsFalse_onlyDisablesLogs() {
        Properties properties = new Properties();
        properties.setProperty("harvestLogs", "false");

        HarvesterConfigLoader.LoadResult result = HarvesterConfigLoader.parse(properties);

        assertFalse(result.config().harvestLogs());
        assertTrue(result.config().harvestOres());
        assertTrue(result.warnings().isEmpty());
    }

    @Test
    void parse_harvestOresFalse_onlyDisablesOres() {
        Properties properties = new Properties();
        properties.setProperty("harvestOres", "false");

        HarvesterConfigLoader.LoadResult result = HarvesterConfigLoader.parse(properties);

        assertTrue(result.config().harvestLogs());
        assertFalse(result.config().harvestOres());
        assertTrue(result.warnings().isEmpty());
    }

    @Test
    void parse_invalidHarvestLogs_fallsBackToDefaultWithOneWarning() {
        Properties properties = new Properties();
        properties.setProperty("harvestLogs", "not-a-boolean");

        HarvesterConfigLoader.LoadResult result = HarvesterConfigLoader.parse(properties);

        assertEquals(HarvesterConfig.DEFAULT_HARVEST_LOGS, result.config().harvestLogs());
        assertEquals(1, result.warnings().size());
    }

    @Test
    void parse_invalidHarvestOres_fallsBackToDefaultWithOneWarning() {
        Properties properties = new Properties();
        properties.setProperty("harvestOres", "not-a-boolean");

        HarvesterConfigLoader.LoadResult result = HarvesterConfigLoader.parse(properties);

        assertEquals(HarvesterConfig.DEFAULT_HARVEST_ORES, result.config().harvestOres());
        assertEquals(1, result.warnings().size());
    }

    @Test
    void parse_multipleSimultaneousInvalidProperties_warnOnceEachAndFallBackIndependently() {
        Properties properties = new Properties();
        properties.setProperty("enabled", "notabool");
        properties.setProperty("maxChain", "101");
        properties.setProperty("neighborhood", "bogus");
        properties.setProperty("harvestLogs", "notabool");
        properties.setProperty("harvestOres", "notabool");

        HarvesterConfigLoader.LoadResult result = HarvesterConfigLoader.parse(properties);

        assertEquals(5, result.warnings().size());
        assertEquals(HarvesterConfig.DEFAULT_ENABLED, result.config().enabled());
        assertEquals(HarvesterConfig.DEFAULT_MAX_CHAIN, result.config().maxChain());
        assertEquals(HarvesterConfig.DEFAULT_NEIGHBORHOOD, result.config().neighborhood());
        assertEquals(HarvesterConfig.DEFAULT_HARVEST_LOGS, result.config().harvestLogs());
        assertEquals(HarvesterConfig.DEFAULT_HARVEST_ORES, result.config().harvestOres());
    }

    @Test
    void parse_booleanValues_areCaseInsensitive() {
        Properties properties = new Properties();
        properties.setProperty("enabled", "FALSE");
        properties.setProperty("diagnosticLogging", "True");

        HarvesterConfigLoader.LoadResult result = HarvesterConfigLoader.parse(properties);

        assertFalse(result.config().enabled());
        assertTrue(result.config().diagnosticLogging());
        assertTrue(result.warnings().isEmpty());
    }

    @Test
    void parse_invalidBoolean_fallsBackToDefaultWithOneWarning() {
        Properties properties = new Properties();
        properties.setProperty("enabled", "not-a-boolean");

        HarvesterConfigLoader.LoadResult result = HarvesterConfigLoader.parse(properties);

        assertEquals(HarvesterConfig.DEFAULT_ENABLED, result.config().enabled());
        assertEquals(1, result.warnings().size());
    }

    @Test
    void parse_nonNumericMaxChain_fallsBackToDefaultWithOneWarning() {
        Properties properties = new Properties();
        properties.setProperty("maxChain", "sixty-four");

        HarvesterConfigLoader.LoadResult result = HarvesterConfigLoader.parse(properties);

        assertEquals(HarvesterConfig.DEFAULT_MAX_CHAIN, result.config().maxChain());
        assertEquals(1, result.warnings().size());
    }

    @Test
    void parse_zeroMaxChain_fallsBackToDefaultWithOneWarning() {
        Properties properties = new Properties();
        properties.setProperty("maxChain", "0");

        HarvesterConfigLoader.LoadResult result = HarvesterConfigLoader.parse(properties);

        assertEquals(HarvesterConfig.DEFAULT_MAX_CHAIN, result.config().maxChain());
        assertEquals(1, result.warnings().size());
    }

    @Test
    void parse_negativeMaxChain_fallsBackToDefaultWithOneWarning() {
        Properties properties = new Properties();
        properties.setProperty("maxChain", "-5");

        HarvesterConfigLoader.LoadResult result = HarvesterConfigLoader.parse(properties);

        assertEquals(HarvesterConfig.DEFAULT_MAX_CHAIN, result.config().maxChain());
        assertEquals(1, result.warnings().size());
    }

    @Test
    void parse_maxChainOfOne_isAccepted() {
        Properties properties = new Properties();
        properties.setProperty("maxChain", "1");

        HarvesterConfigLoader.LoadResult result = HarvesterConfigLoader.parse(properties);

        assertEquals(1, result.config().maxChain());
        assertTrue(result.warnings().isEmpty());
    }

    @Test
    void parse_maxChainAtUpperBound_isAccepted() {
        Properties properties = new Properties();
        properties.setProperty("maxChain", String.valueOf(HarvesterConfig.MAX_ALLOWED_MAX_CHAIN));

        HarvesterConfigLoader.LoadResult result = HarvesterConfigLoader.parse(properties);

        assertEquals(HarvesterConfig.MAX_ALLOWED_MAX_CHAIN, result.config().maxChain());
        assertTrue(result.warnings().isEmpty());
    }

    @Test
    void parse_maxChainAboveUpperBound_fallsBackToDefaultWithOneWarning() {
        Properties properties = new Properties();
        properties.setProperty("maxChain", String.valueOf(HarvesterConfig.MAX_ALLOWED_MAX_CHAIN + 1));

        HarvesterConfigLoader.LoadResult result = HarvesterConfigLoader.parse(properties);

        assertEquals(HarvesterConfig.DEFAULT_MAX_CHAIN, result.config().maxChain());
        assertEquals(1, result.warnings().size());
    }

    @Test
    void parse_invalidNeighborhood_fallsBackToDefaultWithOneWarning() {
        Properties properties = new Properties();
        properties.setProperty("neighborhood", "eight_neighbors");

        HarvesterConfigLoader.LoadResult result = HarvesterConfigLoader.parse(properties);

        assertEquals(HarvesterConfig.DEFAULT_NEIGHBORHOOD, result.config().neighborhood());
        assertEquals(1, result.warnings().size());
    }

    @Test
    void parse_newBooleanAndListKeys_areParsed() {
        Properties p = new Properties();
        p.setProperty("consolidateDrops", "false");
        p.setProperty("harvestDirt", "true");
        p.setProperty("undergroundMaxY", "40");
        p.setProperty("allowlist", "minecraft:dirt, minecraft:gravel");
        p.setProperty("denylist", "minecraft:log");
        p.setProperty("toolAxeIds", "279, 280");

        HarvesterConfigLoader.LoadResult result = HarvesterConfigLoader.parse(p);
        HarvesterConfig config = result.config();

        assertFalse(config.consolidateDrops());
        assertTrue(config.harvestDirt());
        assertEquals(40, config.undergroundMaxY());
        assertEquals(Set.of("minecraft:dirt", "minecraft:gravel"), config.allowlist());
        assertEquals(Set.of("minecraft:log"), config.denylist());
        assertEquals(Set.of(279, 280), config.toolAxeIds());
        assertTrue(result.warnings().isEmpty());
    }

    @Test
    void parse_invalidToolIdEntry_warnsAndSkipsThatEntryOnly() {
        Properties p = new Properties();
        p.setProperty("toolShovelIds", "277, not-a-number, 278");

        HarvesterConfigLoader.LoadResult result = HarvesterConfigLoader.parse(p);

        assertEquals(Set.of(277, 278), result.config().toolShovelIds());
        assertEquals(1, result.warnings().size());
        assertTrue(result.warnings().get(0).contains("toolShovelIds"));
    }

    @Test
    void parse_undergroundMaxYOutOfRange_fallsBackToDefault() {
        Properties p = new Properties();
        p.setProperty("undergroundMaxY", "9999");

        HarvesterConfigLoader.LoadResult result = HarvesterConfigLoader.parse(p);

        assertEquals(HarvesterConfig.DEFAULT_UNDERGROUND_MAX_Y, result.config().undergroundMaxY());
        assertEquals(1, result.warnings().size());
    }

    @Test
    void parse_unknownProperties_areIgnoredWithoutWarning() {
        Properties properties = new Properties();
        properties.setProperty("someUnknownFutureSetting", "whatever");

        HarvesterConfigLoader.LoadResult result = HarvesterConfigLoader.parse(properties);

        assertTrue(result.warnings().isEmpty());
        assertEquals(HarvesterConfig.DEFAULTS.enabled(), result.config().enabled());
    }

    @Test
    void loadOrCreateDefaults_existingValidFile_valuesLoadedAndMigratedInPlace(@TempDir Path dir) throws IOException {
        Path configFile = dir.resolve("harvester.properties");
        Files.writeString(
                configFile,
                "enabled=false\nmaxChain=16\nneighborhood=orthogonal_6\ndiagnosticLogging=true\n",
                StandardCharsets.UTF_8
        );

        HarvesterConfigLoader.LoadResult result = HarvesterConfigLoader.loadOrCreateDefaults(configFile);

        // Every value the user set is preserved verbatim.
        assertFalse(result.config().enabled());
        assertEquals(16, result.config().maxChain());
        assertEquals(NeighborhoodChoice.ORTHOGONAL_6, result.config().neighborhood());
        assertTrue(result.config().diagnosticLogging());
        // A pre-1.0.0 file is missing the new keys, so it is migrated in place:
        // exactly one migration notice, and the rewritten file now carries the
        // new keys while preserving the user's original values.
        assertEquals(1, result.warnings().size());
        assertTrue(result.warnings().get(0).contains("Migrated"));
        String rewritten = Files.readString(configFile, StandardCharsets.UTF_8);
        assertTrue(rewritten.contains("enabled=false"));
        assertTrue(rewritten.contains("maxChain=16"));
        assertTrue(rewritten.contains("consolidateDrops=true"));
        assertTrue(rewritten.contains("harvestDirt=false"));
    }

    /**
     * A file written before the 1.0.0 keys existed must keep loading cleanly
     * (each missing key defaults) and be migrated <em>non-destructively</em>:
     * the file is rewritten to include every current key, preserving each
     * value the user had set, and a single migration notice is emitted.
     */
    @Test
    void loadOrCreateDefaults_preExistingFileWithoutNewProperties_defaultsAndMigratesNonDestructively(
            @TempDir Path dir
    ) throws IOException {
        Path configFile = dir.resolve("harvester.properties");
        String oldFormatContents = "enabled=false\nmaxChain=16\nneighborhood=orthogonal_6\ndiagnosticLogging=true\n";
        Files.writeString(configFile, oldFormatContents, StandardCharsets.UTF_8);

        HarvesterConfigLoader.LoadResult result = HarvesterConfigLoader.loadOrCreateDefaults(configFile);

        assertTrue(result.config().harvestLogs());
        assertTrue(result.config().harvestOres());
        assertFalse(result.config().multiplayerAllowed());
        assertTrue(result.config().consolidateDrops());
        assertFalse(result.config().harvestDirt());
        assertEquals(1, result.warnings().size());
        assertTrue(result.warnings().get(0).contains("Migrated"));
        // Non-destructive: the user's original values survive the rewrite.
        String rewritten = Files.readString(configFile, StandardCharsets.UTF_8);
        assertTrue(rewritten.contains("enabled=false"));
        assertTrue(rewritten.contains("maxChain=16"));
        assertTrue(rewritten.contains("neighborhood=orthogonal_6"));
    }

    @Test
    void loadOrCreateDefaults_existingFileWithInvalidProperty_warnsAndFallsBackForThatPropertyOnly(
            @TempDir Path dir
    ) throws IOException {
        Path configFile = dir.resolve("harvester.properties");
        Files.writeString(
                configFile,
                "enabled=true\nmaxChain=not-a-number\nneighborhood=legacy_26\ndiagnosticLogging=false\n",
                StandardCharsets.UTF_8
        );

        HarvesterConfigLoader.LoadResult result = HarvesterConfigLoader.loadOrCreateDefaults(configFile);

        assertTrue(result.config().enabled());
        assertEquals(HarvesterConfig.DEFAULT_MAX_CHAIN, result.config().maxChain());
        assertEquals(NeighborhoodChoice.LEGACY_26, result.config().neighborhood());
        // One warning for the invalid maxChain, plus one migration notice
        // (this pre-1.0.0 file also lacks the new keys).
        assertEquals(2, result.warnings().size());
        assertTrue(result.warnings().stream().anyMatch(w -> w.contains("maxChain")));
        assertTrue(result.warnings().stream().anyMatch(w -> w.contains("Migrated")));
    }
}
