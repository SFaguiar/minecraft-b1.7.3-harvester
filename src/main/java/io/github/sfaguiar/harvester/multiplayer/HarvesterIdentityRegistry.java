package io.github.sfaguiar.harvester.multiplayer;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Pure, generic identity-keyed registry backed by {@link IdentityHashMap}
 * — reference identity only, never {@code equals()}/{@code hashCode()},
 * matching the requirement that a reconnecting or respawned player (a
 * new object, even if otherwise identical) never collides with a prior
 * entry. Every removal is explicit ({@link #remove}/{@link #clearAll});
 * this class never relies on garbage collection for correctness — an
 * entry exists until something calls one of those two methods, and never
 * a moment longer than that in terms of what {@link #get}/{@link
 * #getOrCreate} can observe.
 *
 * <p>No Minecraft/StationAPI import, so this is unit-testable with plain
 * {@code Object} keys standing in for {@code ServerPlayerEntity}
 * instances — {@code
 * io.github.sfaguiar.harvester.server.multiplayer.HarvesterMultiplayerServerRegistry}
 * is a thin, Minecraft-typed specialization of one instance of this
 * class.
 */
public final class HarvesterIdentityRegistry<K, V> {

    private final Map<K, V> entries = new IdentityHashMap<>();
    private final Supplier<V> defaultFactory;

    public HarvesterIdentityRegistry(Supplier<V> defaultFactory) {
        this.defaultFactory = defaultFactory;
    }

    /** Returns the existing entry for {@code key}, or creates and stores a fresh default one. */
    public V getOrCreate(K key) {
        return entries.computeIfAbsent(key, ignored -> defaultFactory.get());
    }

    public Optional<V> get(K key) {
        return Optional.ofNullable(entries.get(key));
    }

    /** Explicit, immediate removal — the only way an entry is ever cleared for one key. */
    public void remove(K key) {
        entries.remove(key);
    }

    /** Explicit, immediate removal of every entry — the only way the whole registry is ever cleared. */
    public void clearAll() {
        entries.clear();
    }

    public int size() {
        return entries.size();
    }
}
