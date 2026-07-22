package io.github.sfaguiar.harvester.config;

import io.github.sfaguiar.harvester.core.LegacyTwentySixNeighborhood;
import io.github.sfaguiar.harvester.core.NeighborhoodPolicy;
import io.github.sfaguiar.harvester.core.OrthogonalSixNeighborhood;

import java.util.Optional;

/**
 * The {@code neighborhood} configuration property's accepted values, each
 * mapped to the {@code core} {@link NeighborhoodPolicy} it selects.
 *
 * <p>Depends on nothing beyond {@code core} — no Minecraft, StationAPI, or
 * Fabric import — so it is fully unit-testable without starting Minecraft.
 */
public enum NeighborhoodChoice {

    LEGACY_26("legacy_26") {
        @Override
        public NeighborhoodPolicy toPolicy() {
            return new LegacyTwentySixNeighborhood();
        }
    },
    ORTHOGONAL_6("orthogonal_6") {
        @Override
        public NeighborhoodPolicy toPolicy() {
            return new OrthogonalSixNeighborhood();
        }
    };

    private final String propertyValue;

    NeighborhoodChoice(String propertyValue) {
        this.propertyValue = propertyValue;
    }

    public String propertyValue() {
        return propertyValue;
    }

    public abstract NeighborhoodPolicy toPolicy();

    public static Optional<NeighborhoodChoice> fromPropertyValue(String value) {
        for (NeighborhoodChoice choice : values()) {
            if (choice.propertyValue.equalsIgnoreCase(value)) {
                return Optional.of(choice);
            }
        }
        return Optional.empty();
    }
}
