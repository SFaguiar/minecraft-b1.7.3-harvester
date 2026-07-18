package io.github.sfaguiar.harvester.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

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
        assertTrue(result.warnings().isEmpty());
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
        assertTrue(written.contains("maxChain=64"));
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

        HarvesterConfigLoader.LoadResult result = HarvesterConfigLoader.parse(properties);

        assertFalse(result.config().enabled());
        assertEquals(32, result.config().maxChain());
        assertEquals(NeighborhoodChoice.ORTHOGONAL_6, result.config().neighborhood());
        assertTrue(result.config().diagnosticLogging());
        assertTrue(result.warnings().isEmpty());
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
    void parse_unknownProperties_areIgnoredWithoutWarning() {
        Properties properties = new Properties();
        properties.setProperty("someUnknownFutureSetting", "whatever");

        HarvesterConfigLoader.LoadResult result = HarvesterConfigLoader.parse(properties);

        assertTrue(result.warnings().isEmpty());
        assertEquals(HarvesterConfig.DEFAULTS.enabled(), result.config().enabled());
    }

    @Test
    void loadOrCreateDefaults_existingValidFile_isLoadedAsIs(@TempDir Path dir) throws IOException {
        Path configFile = dir.resolve("harvester.properties");
        Files.writeString(
                configFile,
                "enabled=false\nmaxChain=16\nneighborhood=orthogonal_6\ndiagnosticLogging=true\n",
                StandardCharsets.UTF_8
        );

        HarvesterConfigLoader.LoadResult result = HarvesterConfigLoader.loadOrCreateDefaults(configFile);

        assertFalse(result.config().enabled());
        assertEquals(16, result.config().maxChain());
        assertEquals(NeighborhoodChoice.ORTHOGONAL_6, result.config().neighborhood());
        assertTrue(result.config().diagnosticLogging());
        assertTrue(result.warnings().isEmpty());
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
        assertEquals(1, result.warnings().size());
    }
}
