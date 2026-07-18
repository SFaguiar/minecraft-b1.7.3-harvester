package io.github.sfaguiar.harvester.client;

import io.github.sfaguiar.harvester.core.HarvestGroup;
import io.github.sfaguiar.harvester.core.HarvestPlan;

import java.util.Objects;

/**
 * A completed discovery's plan together with the resolved
 * {@link HarvestGroup} that produced it — the executor needs the group
 * afterward (to generalize per-candidate revalidation beyond logs), and
 * re-resolving it a second time from the origin's {@code BlockState} would
 * duplicate {@code SingleplayerHarvestDiscoveryAdapter}'s own resolution.
 * {@link HarvestPlan} itself stays untouched; this is a {@code client}-only
 * carrier, never passed into {@code core}.
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
