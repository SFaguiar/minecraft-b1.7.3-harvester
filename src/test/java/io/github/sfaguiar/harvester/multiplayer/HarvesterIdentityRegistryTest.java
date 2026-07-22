package io.github.sfaguiar.harvester.multiplayer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the identity-map data structure directly with plain {@code
 * Object} keys standing in for {@code ServerPlayerEntity} instances (a
 * real one cannot be constructed outside a running server) — this is
 * where every "lifecycle determinism" guarantee the server registry
 * relies on is actually proven, deterministically, with no {@code
 * System.gc()} anywhere.
 */
final class HarvesterIdentityRegistryTest {

    private static HarvesterIdentityRegistry<Object, StringBuilder> newRegistry() {
        return new HarvesterIdentityRegistry<>(() -> new StringBuilder("default"));
    }

    @Test
    void getOrCreate_newKey_createsDefaultAndStoresIt() {
        HarvesterIdentityRegistry<Object, StringBuilder> registry = newRegistry();
        Object key = new Object();

        StringBuilder value = registry.getOrCreate(key);

        assertEquals("default", value.toString());
        assertEquals(1, registry.size());
    }

    @Test
    void getOrCreate_sameKeyTwice_returnsTheSameStoredInstance() {
        HarvesterIdentityRegistry<Object, StringBuilder> registry = newRegistry();
        Object key = new Object();

        StringBuilder first = registry.getOrCreate(key);
        first.append("-mutated");
        StringBuilder second = registry.getOrCreate(key);

        assertSame(first, second);
        assertEquals("default-mutated", second.toString());
    }

    @Test
    void independentKeys_neverShareState() {
        HarvesterIdentityRegistry<Object, StringBuilder> registry = newRegistry();
        Object playerA = new Object();
        Object playerB = new Object();

        registry.getOrCreate(playerA).append("-A");
        registry.getOrCreate(playerB).append("-B");

        assertEquals("default-A", registry.get(playerA).orElseThrow().toString());
        assertEquals("default-B", registry.get(playerB).orElseThrow().toString());
        assertEquals(2, registry.size());
    }

    @Test
    void equalButDistinctKeys_areTreatedAsDifferentEntries_identityNotEquality() {
        // Two distinct String instances with equal content: a HashMap would
        // collapse these; the identity map must not.
        HarvesterIdentityRegistry<String, StringBuilder> registry =
                new HarvesterIdentityRegistry<>(() -> new StringBuilder("default"));
        String keyA = new String("player");
        String keyB = new String("player");
        assertNotSame(keyA, keyB);
        assertEquals(keyA, keyB);

        registry.getOrCreate(keyA).append("-A");
        registry.getOrCreate(keyB).append("-B");

        assertEquals("default-A", registry.get(keyA).orElseThrow().toString());
        assertEquals("default-B", registry.get(keyB).orElseThrow().toString());
        assertEquals(2, registry.size());
    }

    @Test
    void instanceReplacement_removingOldKey_newInstanceStartsAtDefault_noStaleCarryover() {
        HarvesterIdentityRegistry<Object, StringBuilder> registry = newRegistry();
        Object oldInstance = new Object();
        registry.getOrCreate(oldInstance).append("-mutated");

        // Simulates respawn: the old instance's entry is explicitly removed
        // (mirroring PlayerManagerLifecycleMixin#harvester$onRespawn), then a
        // brand new instance (never seen before) is looked up.
        registry.remove(oldInstance);
        Object newInstance = new Object();
        StringBuilder freshState = registry.getOrCreate(newInstance);

        assertEquals("default", freshState.toString());
        assertTrue(registry.get(oldInstance).isEmpty());
        assertEquals(1, registry.size());
    }

    @Test
    void remove_clearsEntryImmediately_noGcDependency() {
        HarvesterIdentityRegistry<Object, StringBuilder> registry = newRegistry();
        Object key = new Object();
        registry.getOrCreate(key);
        assertEquals(1, registry.size());

        registry.remove(key);

        // Asserted immediately after remove() returns — no System.gc(), no
        // delay, no polling: removal is synchronous and observable at once.
        assertEquals(0, registry.size());
        assertTrue(registry.get(key).isEmpty());
    }

    @Test
    void remove_unknownKey_isANoOp() {
        HarvesterIdentityRegistry<Object, StringBuilder> registry = newRegistry();

        registry.remove(new Object());

        assertEquals(0, registry.size());
    }

    @Test
    void clearAll_removesEveryEntryImmediately_noGcDependency() {
        HarvesterIdentityRegistry<Object, StringBuilder> registry = newRegistry();
        registry.getOrCreate(new Object());
        registry.getOrCreate(new Object());
        registry.getOrCreate(new Object());
        assertEquals(3, registry.size());

        registry.clearAll();

        assertEquals(0, registry.size());
    }

    @Test
    void afterClearAll_sameKeyGetsAFreshDefault_newConnectionNeverInheritsState() {
        HarvesterIdentityRegistry<Object, StringBuilder> registry = newRegistry();
        Object key = new Object();
        registry.getOrCreate(key).append("-mutated");

        registry.clearAll();
        StringBuilder afterClear = registry.getOrCreate(key);

        assertEquals("default", afterClear.toString());
    }

    @Test
    void get_absentKey_isEmpty() {
        HarvesterIdentityRegistry<Object, StringBuilder> registry = newRegistry();

        assertFalse(registry.get(new Object()).isPresent());
    }
}
