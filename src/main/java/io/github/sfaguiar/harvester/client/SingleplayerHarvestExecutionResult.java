package io.github.sfaguiar.harvester.client;

/**
 * Outcome of a {@link SingleplayerHarvestExecutor} chain trigger. Exists
 * purely for internal clarity and unambiguous logging - it is not an
 * extensibility surface and must not grow into a hierarchy.
 */
public enum SingleplayerHarvestExecutionResult {
    /** The automatic chain is disabled by configuration ({@code enabled=false}). */
    SKIPPED_DISABLED,
    /** Activation key not held when the chain would have started. */
    SKIPPED_INACTIVE,
    /** Another Harvester-initiated break was already in flight. */
    SKIPPED_REENTRANT,
    /** The plan contained no block beyond the origin. */
    NO_ADDITIONAL_CANDIDATE,
    /**
     * The held item's identity changed (broke or was replaced) during the
     * origin's own manual break, observed before the chain could start -
     * per instruction, the chain is never started in this case.
     */
    ORIGIN_TOOL_CHANGED_BEFORE_CHAIN_START,
    /** Every candidate in the plan was broken successfully. */
    CHAIN_COMPLETED,
    /** Stopped: the activation key was released before a candidate was attempted. */
    STOPPED_KEY_RELEASED,
    /** Stopped: the world, player, or interaction manager became unavailable mid-chain. */
    STOPPED_ENVIRONMENT_INVALID,
    /** Stopped: a candidate failed immediate revalidation (no longer present or no longer a member of the resolved group). */
    STOPPED_CANDIDATE_INVALID,
    /**
     * Stopped: an ore candidate revalidated as still a group member, but the
     * currently held item can no longer correctly harvest it (empty hand,
     * wrong tool tier, or the item changed since the origin). Never reached
     * for a log chain.
     */
    STOPPED_TOOL_UNSUITABLE,
    /** Stopped: {@code breakBlock} returned {@code false} for a candidate. */
    STOPPED_BREAK_REJECTED,
    /** Stopped: the held item's identity changed (broke or was replaced) immediately after a successful break. */
    STOPPED_TOOL_CHANGED
}
