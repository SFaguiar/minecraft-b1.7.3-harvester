package io.github.sfaguiar.harvester.core;

import java.util.Objects;
import java.util.Set;

/**
 * Immutable, pure-Java snapshot of the classification-relevant facts about
 * one block, captured once at query time. Carries no Minecraft, StationAPI,
 * Fabric, or Mixin type — the adapter that builds one from a real
 * {@code BlockState} lives entirely in the {@code client} package.
 *
 * <p>{@link #specificOreTags()} holds exact tag identifier strings (e.g.
 * {@code "c:ores/coal"}), not a parsed representation — {@code core} does
 * not need to know anything about tag namespaces or paths beyond treating
 * them as opaque, comparable strings. {@link #registryIdentity()} is the
 * block's own stable registry identifier string (e.g.
 * {@code "minecraft:iron_ore"}), used only by the identity fallback.
 */
public final class BlockDescriptor {

    private final boolean log;
    private final Set<String> specificOreTags;
    private final boolean genericOre;
    private final String registryIdentity;

    public BlockDescriptor(
            boolean log,
            Set<String> specificOreTags,
            boolean genericOre,
            String registryIdentity
    ) {
        this.log = log;
        this.specificOreTags = Set.copyOf(specificOreTags);
        this.genericOre = genericOre;
        this.registryIdentity = Objects.requireNonNull(registryIdentity, "registryIdentity");
    }

    /** Whether this block belongs to the global log group ({@code c:logs}). */
    public boolean isLog() {
        return log;
    }

    /**
     * Every specific material ore tag ({@code namespace=c, path=ores/<material>})
     * this block belongs to. Empty for non-ore blocks and for ore blocks only
     * reachable through the generic aggregator tag.
     */
    public Set<String> specificOreTags() {
        return specificOreTags;
    }

    /** Whether this block belongs to the generic ore aggregator tag ({@code c:ores}). */
    public boolean isGenericOre() {
        return genericOre;
    }

    /** Stable registry identifier string, used only by the identity fallback. */
    public String registryIdentity() {
        return registryIdentity;
    }

    @Override
    public String toString() {
        return "BlockDescriptor{log=" + log
                + ", specificOreTags=" + specificOreTags
                + ", genericOre=" + genericOre
                + ", registryIdentity=" + registryIdentity
                + "}";
    }
}
