package io.github.sfaguiar.harvester.client;

import io.github.sfaguiar.harvester.core.LegacyTwentySixNeighborhood;
import io.github.sfaguiar.harvester.core.NeighborhoodPolicy;

/**
 * The {@link NeighborhoodPolicy} {@link SingleplayerHarvestDiscoveryAdapter}
 * wires in by default.
 *
 * <p>Deliberately extracted into its own class with no Minecraft or
 * StationAPI import at all — {@code core}'s
 * {@link LegacyTwentySixNeighborhood} is the only dependency. This makes
 * the production default-policy selection unit-testable without starting
 * Minecraft: the {@code test} task ({@code build.gradle.kts},
 * {@code tasks.test { useJUnitPlatform() }}) runs on a plain JVM
 * classpath, with none of Fabric Loader's Knot classloading or Mixin
 * transformation applied. {@code SingleplayerHarvestDiscoveryAdapter}
 * itself imports {@code net.minecraft.world.World} and StationAPI's
 * {@code BlockState}/{@code BlockTags}, whose Mixin-added members (such as
 * {@code World.getBlockState}) only exist once that transformation has
 * run — referencing that class directly from a plain unit test would risk
 * depending on transformed members the test JVM never applies. This class
 * carries none of that risk.
 *
 * <p><strong>Production decision, confirmed by the repository owner</strong>
 * (see {@code better-beta-program/docs/operations/OPEN_QUESTIONS.md}
 * Q0011, now {@code DECIDED}): {@link LegacyTwentySixNeighborhood} — full
 * 26-neighbor adjacency — is the default, chosen for topological parity
 * with the legacy Harvester 1.x chain-break connectivity, support for
 * irregular/2x2 trunk shapes, and deterministic behavior; the accepted
 * trade-off is that diagonally-adjacent but otherwise separate structures
 * may merge into one discovered plan. {@code core}'s
 * {@link io.github.sfaguiar.harvester.core.OrthogonalSixNeighborhood}
 * remains fully implemented and tested; it is simply not wired in here.
 * Nothing here introduces a runtime-configurable choice between the two —
 * that remains future work, not implemented by this class.
 */
final class DefaultHarvestNeighborhoodPolicy {

    static final NeighborhoodPolicy INSTANCE = new LegacyTwentySixNeighborhood();

    private DefaultHarvestNeighborhoodPolicy() {
    }
}
