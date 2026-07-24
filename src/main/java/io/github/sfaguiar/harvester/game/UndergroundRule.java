package io.github.sfaguiar.harvester.game;

import io.github.sfaguiar.harvester.config.HarvesterConfig;

/**
 * The pure decision behind the underground gate for dirt and gravel
 * (owner Decision D): a position counts as underground only when it is in
 * the Overworld, has no direct sky access, and sits at or below the
 * configured maximum Y. Every world-sourced input is passed in as a
 * primitive, so this class imports no Minecraft type and is unit-testable
 * without starting the game; the {@code game}/{@code client}/{@code server}
 * adapters read {@code world.dimension.id}, {@code world.hasSkyLight(x,y,z)}
 * (Beta 1.7.3's "can this block see the sky" query), and the Y coordinate,
 * then call {@link #isUnderground}.
 */
public final class UndergroundRule {

    /** The Overworld dimension id in Beta 1.7.3 ({@code Dimension.id}). */
    public static final int OVERWORLD_DIMENSION_ID = 0;

    private UndergroundRule() {
    }

    /**
     * @param dimensionId    {@code world.dimension.id} of the position's world
     * @param exposedToSky   {@code world.hasSkyLight(x, y, z)} — {@code true}
     *                       when the position can see the sky directly
     * @param y              the position's Y coordinate
     * @param config         the active configuration (thresholds/toggles)
     */
    public static boolean isUnderground(int dimensionId, boolean exposedToSky, int y, HarvesterConfig config) {
        if (config.undergroundOverworldOnly() && dimensionId != OVERWORLD_DIMENSION_ID) {
            return false;
        }
        if (config.undergroundRequiresNoSky() && exposedToSky) {
            return false;
        }
        return y <= config.undergroundMaxY();
    }
}
