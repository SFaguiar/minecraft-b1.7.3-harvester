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

final class HarvesterServerOptInLoaderTest {

    @Test
    void loadOrCreateDefault_missingFile_createsItWithFalseAndReturnsFalse(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("example.com-aabbccdd.properties");

        HarvesterServerOptInLoader.LoadResult result =
                HarvesterServerOptInLoader.loadOrCreateDefault(file, "example.com:25565");

        assertTrue(Files.exists(file));
        assertFalse(result.optIn());
        assertTrue(result.warnings().isEmpty());

        String written = Files.readString(file, StandardCharsets.UTF_8);
        assertTrue(written.contains("address=example.com:25565"));
        assertTrue(written.contains("multiplayerOptIn=false"));
    }

    @Test
    void loadOrCreateDefault_missingParentDirectory_isCreated(@TempDir Path dir) {
        Path file = dir.resolve("servers").resolve("example.com-aabbccdd.properties");

        HarvesterServerOptInLoader.LoadResult result =
                HarvesterServerOptInLoader.loadOrCreateDefault(file, "example.com:25565");

        assertTrue(Files.exists(file));
        assertFalse(result.optIn());
    }

    @Test
    void loadOrCreateDefault_existingFileWithOptInTrue_isHonored(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("example.com-aabbccdd.properties");
        Files.writeString(file, "address=example.com:25565\nmultiplayerOptIn=true\n", StandardCharsets.UTF_8);

        HarvesterServerOptInLoader.LoadResult result =
                HarvesterServerOptInLoader.loadOrCreateDefault(file, "example.com:25565");

        assertTrue(result.optIn());
        assertTrue(result.warnings().isEmpty());
    }

    @Test
    void loadOrCreateDefault_existingFileWithOptInFalse_isHonored(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("example.com-aabbccdd.properties");
        Files.writeString(file, "address=example.com:25565\nmultiplayerOptIn=false\n", StandardCharsets.UTF_8);

        HarvesterServerOptInLoader.LoadResult result =
                HarvesterServerOptInLoader.loadOrCreateDefault(file, "example.com:25565");

        assertFalse(result.optIn());
        assertTrue(result.warnings().isEmpty());
    }

    @Test
    void loadOrCreateDefault_invalidOptInValue_fallsBackToFalseWithOneWarning(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("example.com-aabbccdd.properties");
        Files.writeString(file, "address=example.com:25565\nmultiplayerOptIn=maybe\n", StandardCharsets.UTF_8);

        HarvesterServerOptInLoader.LoadResult result =
                HarvesterServerOptInLoader.loadOrCreateDefault(file, "example.com:25565");

        assertFalse(result.optIn());
        assertEquals(1, result.warnings().size());
    }

    @Test
    void parse_missingKey_defaultsToFalseNoWarning() {
        HarvesterServerOptInLoader.LoadResult result = HarvesterServerOptInLoader.parse(new Properties());

        assertFalse(result.optIn());
        assertTrue(result.warnings().isEmpty());
    }
}
