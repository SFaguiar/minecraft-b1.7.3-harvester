package io.github.sfaguiar.harvester.config;

import io.github.sfaguiar.harvester.core.LegacyTwentySixNeighborhood;
import io.github.sfaguiar.harvester.core.OrthogonalSixNeighborhood;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class NeighborhoodChoiceTest {

    @Test
    void legacy26_mapsToLegacyTwentySixNeighborhood() {
        assertInstanceOf(LegacyTwentySixNeighborhood.class, NeighborhoodChoice.LEGACY_26.toPolicy());
    }

    @Test
    void orthogonal6_mapsToOrthogonalSixNeighborhood() {
        assertInstanceOf(OrthogonalSixNeighborhood.class, NeighborhoodChoice.ORTHOGONAL_6.toPolicy());
    }

    @Test
    void fromPropertyValue_acceptsExactValues() {
        assertEquals(Optional.of(NeighborhoodChoice.LEGACY_26), NeighborhoodChoice.fromPropertyValue("legacy_26"));
        assertEquals(Optional.of(NeighborhoodChoice.ORTHOGONAL_6), NeighborhoodChoice.fromPropertyValue("orthogonal_6"));
    }

    @Test
    void fromPropertyValue_isCaseInsensitive() {
        assertEquals(Optional.of(NeighborhoodChoice.LEGACY_26), NeighborhoodChoice.fromPropertyValue("LEGACY_26"));
    }

    @Test
    void fromPropertyValue_rejectsUnknownValue() {
        assertTrue(NeighborhoodChoice.fromPropertyValue("diagonal_only").isEmpty());
    }
}
