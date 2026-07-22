package io.github.sfaguiar.harvester.game;

import io.github.sfaguiar.harvester.core.HarvestGroup;
import io.github.sfaguiar.harvester.core.HarvestPlan;

import java.util.Objects;

/**
 * A completed discovery's plan together with the resolved
 * {@link HarvestGroup} that produced it — a chain executor needs the group
 * afterward (to generalize per-candidate revalidation beyond logs), and
 * re-resolving it a second time from the origin's {@code BlockState} would
 * duplicate {@link HarvestDiscoveryAdapter}'s own resolution.
 * {@link HarvestPlan} itself stays untouched; this is a carrier only, never
 * passed into {@code core}. Side-agnostic — shared unchanged by the
 * singleplayer client and the multiplayer server.
 */
public final class HarvestDiscoveryOutcome {

    private final HarvestPlan plan;
    private final HarvestGroup group;

    HarvestDiscoveryOutcome(HarvestPlan plan, HarvestGroup group) {
        this.plan = Objects.requireNonNull(plan, "plan");
        this.group = Objects.requireNonNull(group, "group");
    }

    public HarvestPlan plan() {
        return plan;
    }

    public HarvestGroup group() {
        return group;
    }
}
