package io.github.sfaguiar.harvester.platform;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class FoundationTest {
    @Test
    void exposesStableModId() {
        assertEquals("harvester", HarvesterEntrypoint.MOD_ID);
    }
}
