package io.github.sfaguiar.harvester.core;

/**
 * Decides which coordinates count as "adjacent" to a given coordinate for
 * the purposes of {@link ConnectedBlockFinder}'s traversal.
 *
 * <p>This abstraction exists because the 6-vs-26-neighbor connectivity
 * question is an explicitly open design decision, not one this increment
 * resolves — see
 * {@code better-beta-program/docs/operations/OPEN_QUESTIONS.md} Q0011.
 * {@link ConnectedBlockFinder} receives a policy as an explicit dependency
 * instead of hard-coding one, so the traversal algorithm itself stays
 * unaware of and unaffected by which policy is in use (see
 * {@code NeighborhoodPolicyTest} for tests demonstrating this
 * independence).
 *
 * <p>Implementations must return coordinates in a fixed, deterministic
 * order for a given input coordinate — callers and tests may depend on
 * that order never changing without a corresponding test update.
 */
public interface NeighborhoodPolicy {

    Iterable<BlockCoordinate> neighborsOf(BlockCoordinate coordinate);
}
