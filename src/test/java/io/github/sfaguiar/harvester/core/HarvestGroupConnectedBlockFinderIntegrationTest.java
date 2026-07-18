package io.github.sfaguiar.harvester.core;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end (still Minecraft-free) proof that {@link BlockGroupView#byDescriptor}
 * correctly wires a resolved {@link HarvestGroup} into the unmodified
 * {@link ConnectedBlockFinder}/{@link HarvestRequest} engine: {@code maxChain}
 * and neighborhood policy continue to apply exactly as before, and different
 * materials/identities never merge into one plan.
 */
final class HarvestGroupConnectedBlockFinderIntegrationTest {

    private static final BlockDescriptor AIR_LIKE = new BlockDescriptor(false, Set.of(), false, "");

    @Test
    void maxChainAndNeighborhood_stillApplyThroughGroupBasedView() {
        FakeDescriptorView world = new FakeDescriptorView(AIR_LIKE);
        // A straight line of 5 logs; limit set to 3 (origin + 2).
        for (int x = 0; x <= 4; x++) {
            world.setLog(x, 0, 0);
        }
        HarvestGroup logs = HarvestGroup.logs();
        HarvestRequest request = new HarvestRequest(new BlockCoordinate(0, 0, 0), 17, true, 3);

        HarvestPlan plan = ConnectedBlockFinder.discover(
                request, BlockGroupView.byDescriptor(world, logs), new OrthogonalSixNeighborhood()
        );

        assertEquals(3, plan.totalIncluded());
        assertTrue(plan.limitReached());
    }

    @Test
    void orthogonalNeighborhood_excludesDiagonalOreCandidate() {
        FakeDescriptorView world = new FakeDescriptorView(AIR_LIKE);
        world.setOre(0, 0, 0, "c:ores/coal", "minecraft:coal_ore");
        world.setOre(1, 1, 0, "c:ores/coal", "minecraft:coal_ore"); // diagonal only
        HarvestGroup coal = HarvestGroup.oreSpecificTags(Set.of("c:ores/coal"));
        HarvestRequest request = new HarvestRequest(new BlockCoordinate(0, 0, 0), 0, true, 64);

        HarvestPlan plan = ConnectedBlockFinder.discover(
                request, BlockGroupView.byDescriptor(world, coal), new OrthogonalSixNeighborhood()
        );

        assertEquals(1, plan.totalIncluded());
    }

    @Test
    void differentMaterials_neverMergeIntoOnePlan() {
        FakeDescriptorView world = new FakeDescriptorView(AIR_LIKE);
        world.setOre(0, 0, 0, "c:ores/iron", "minecraft:iron_ore");
        world.setOre(1, 0, 0, "c:ores/gold", "minecraft:gold_ore"); // face-adjacent, different material
        HarvestGroup iron = HarvestGroup.oreSpecificTags(Set.of("c:ores/iron"));
        HarvestRequest request = new HarvestRequest(new BlockCoordinate(0, 0, 0), 0, true, 64);

        HarvestPlan plan = ConnectedBlockFinder.discover(
                request, BlockGroupView.byDescriptor(world, iron), new LegacyTwentySixNeighborhood()
        );

        assertEquals(1, plan.totalIncluded());
    }

    @Test
    void identityFallback_doesNotCrossIntoADifferentBlockEvenIfAdjacent() {
        FakeDescriptorView world = new FakeDescriptorView(AIR_LIKE);
        world.set(0, 0, 0, new BlockDescriptor(false, Set.of(), true, "modded:ore_a"));
        world.set(1, 0, 0, new BlockDescriptor(false, Set.of(), true, "modded:ore_b")); // adjacent, different identity
        HarvestGroup fallback = HarvestGroup.oreIdentityFallback("modded:ore_a");
        HarvestRequest request = new HarvestRequest(new BlockCoordinate(0, 0, 0), 0, true, 64);

        HarvestPlan plan = ConnectedBlockFinder.discover(
                request, BlockGroupView.byDescriptor(world, fallback), new LegacyTwentySixNeighborhood()
        );

        assertEquals(1, plan.totalIncluded());
    }

    @Test
    void logsWithEmptyHand_conceptuallyEligible_discoveryDoesNotDependOnAnyHeldItem() {
        // core has no notion of a held item at all - discovery for logs
        // never consults one, which is exactly what lets an empty-handed
        // chain work. This test documents that absence rather than testing
        // a held-item type that doesn't exist in this package.
        FakeDescriptorView world = new FakeDescriptorView(AIR_LIKE);
        world.setLog(0, 0, 0);
        world.setLog(1, 0, 0);
        HarvestGroup logs = HarvestGroup.logs();
        HarvestRequest request = new HarvestRequest(new BlockCoordinate(0, 0, 0), 17, true, 64);

        HarvestPlan plan = ConnectedBlockFinder.discover(
                request, BlockGroupView.byDescriptor(world, logs), new OrthogonalSixNeighborhood()
        );

        assertEquals(2, plan.totalIncluded());
    }
}
