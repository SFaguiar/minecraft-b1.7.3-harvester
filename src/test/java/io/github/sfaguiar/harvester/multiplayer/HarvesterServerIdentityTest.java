package io.github.sfaguiar.harvester.multiplayer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

final class HarvesterServerIdentityTest {

    @Test
    void normalizedAddress_lowercasesHost() {
        HarvesterServerIdentity identity = HarvesterServerIdentity.of("Example.COM", 25565);

        assertEquals("example.com:25565", identity.normalizedAddress());
    }

    @Test
    void normalizedAddress_trimsWhitespace() {
        HarvesterServerIdentity identity = HarvesterServerIdentity.of("  example.com  ", 25565);

        assertEquals("example.com:25565", identity.normalizedAddress());
    }

    @Test
    void sameHostAndPort_differentCaseOrWhitespace_producesDeterministicallyEqualIdentity() {
        HarvesterServerIdentity a = HarvesterServerIdentity.of("Example.com", 25565);
        HarvesterServerIdentity b = HarvesterServerIdentity.of(" example.COM ", 25565);

        assertEquals(a, b);
        assertEquals(a.fileName(), b.fileName());
        assertEquals(a.shortHash(), b.shortHash());
    }

    @Test
    void differentPorts_sameHost_areIsolatedFromEachOther() {
        HarvesterServerIdentity portA = HarvesterServerIdentity.of("example.com", 25565);
        HarvesterServerIdentity portB = HarvesterServerIdentity.of("example.com", 25566);

        assertNotEquals(portA, portB);
        assertNotEquals(portA.fileName(), portB.fileName());
    }

    @Test
    void differentHosts_samePort_areIsolatedFromEachOther() {
        HarvesterServerIdentity hostA = HarvesterServerIdentity.of("alpha.example.com", 25565);
        HarvesterServerIdentity hostB = HarvesterServerIdentity.of("beta.example.com", 25565);

        assertNotEquals(hostA, hostB);
        assertNotEquals(hostA.fileName(), hostB.fileName());
    }

    @Test
    void fileName_endsWithPropertiesExtension() {
        HarvesterServerIdentity identity = HarvesterServerIdentity.of("example.com", 25565);

        assertEquals(true, identity.fileName().endsWith(".properties"));
    }

    @Test
    void readableName_sanitizesUnsafeCharacters() {
        HarvesterServerIdentity identity = HarvesterServerIdentity.of("my:weird/host*name", 25565);

        assertEquals("my_weird_host_name", identity.readableName());
    }

    @Test
    void shortHash_isEightHexCharacters() {
        HarvesterServerIdentity identity = HarvesterServerIdentity.of("example.com", 25565);

        String hash = identity.shortHash();

        assertEquals(8, hash.length());
        assertEquals(true, hash.matches("[0-9a-f]{8}"));
    }
}
