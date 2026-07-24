package io.github.sfaguiar.harvester.core;

import java.util.Objects;
import java.util.Set;

/**
 * Immutable, pure-Java snapshot of the classification-relevant facts about
 * one block, captured once at query time. Carries no Minecraft, StationAPI,
 * Fabric, or Mixin type — the adapter that builds one from a real
 * {@code BlockState} lives entirely in the {@code game} package.
 *
 * <p>{@link #specificOreTags()} holds exact tag identifier strings (e.g.
 * {@code "c:ores/coal"}), not a parsed representation — {@code core} does
 * not need to know anything about tag namespaces or paths beyond treating
 * them as opaque, comparable strings. {@link #registryIdentity()} is the
 * block's own stable registry identifier string (e.g.
 * {@code "minecraft:iron_ore"}), used by the identity fallback and by the
 * allowlist/denylist precedence rules.
 *
 * <p>{@link #metadata()} is the block's raw metadata value, needed by the
 * categories that cannot be distinguished by tag alone (leaf species —
 * {@code metadata & 0x3}; crop maturity — {@code metadata == 7}). The
 * category flags ({@link #isDirt()}, {@link #isGravel()}, {@link #isLeaves()},
 * {@link #isCrop()}) are decided by the {@code game}-package adapter against
 * the real block instance, never by this pure class.
 */
public final class BlockDescriptor {

    private final boolean log;
    private final Set<String> specificOreTags;
    private final boolean genericOre;
    private final String registryIdentity;
    private final int metadata;
    private final boolean dirt;
    private final boolean gravel;
    private final boolean leaves;
    private final boolean crop;

    /**
     * Backward-compatible constructor for the log/ore-only classification
     * (metadata {@code 0}, no dirt/gravel/leaves/crop). Existing call sites
     * and tests that predate the underground/leaf/crop categories keep
     * working unchanged.
     */
    public BlockDescriptor(
            boolean log,
            Set<String> specificOreTags,
            boolean genericOre,
            String registryIdentity
    ) {
        this(log, specificOreTags, genericOre, registryIdentity, 0, false, false, false, false);
    }

    public BlockDescriptor(
            boolean log,
            Set<String> specificOreTags,
            boolean genericOre,
            String registryIdentity,
            int metadata,
            boolean dirt,
            boolean gravel,
            boolean leaves,
            boolean crop
    ) {
        this.log = log;
        this.specificOreTags = Set.copyOf(specificOreTags);
        this.genericOre = genericOre;
        this.registryIdentity = Objects.requireNonNull(registryIdentity, "registryIdentity");
        this.metadata = metadata;
        this.dirt = dirt;
        this.gravel = gravel;
        this.leaves = leaves;
        this.crop = crop;
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

    /** Stable registry identifier string, used by the identity fallback and allow/deny rules. */
    public String registryIdentity() {
        return registryIdentity;
    }

    /** Raw block metadata; drives leaf-species ({@code & 0x3}) and crop-maturity ({@code == 7}) matching. */
    public int metadata() {
        return metadata;
    }

    /** Whether this block is the vanilla dirt block (or an adapter-classified equivalent). */
    public boolean isDirt() {
        return dirt;
    }

    /** Whether this block is the vanilla gravel block (or an adapter-classified equivalent). */
    public boolean isGravel() {
        return gravel;
    }

    /** Whether this block is a leaf block; species is distinguished by {@code metadata & 0x3}. */
    public boolean isLeaves() {
        return leaves;
    }

    /** Whether this block is a growth-staged crop; maturity is {@code metadata == 7}. */
    public boolean isCrop() {
        return crop;
    }

    @Override
    public String toString() {
        return "BlockDescriptor{log=" + log
                + ", specificOreTags=" + specificOreTags
                + ", genericOre=" + genericOre
                + ", registryIdentity=" + registryIdentity
                + ", metadata=" + metadata
                + ", dirt=" + dirt
                + ", gravel=" + gravel
                + ", leaves=" + leaves
                + ", crop=" + crop
                + "}";
    }
}
