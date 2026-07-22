package io.github.sfaguiar.harvester.server.multiplayer;

/**
 * A single candidate break attempt through the server's own normal break
 * pipeline — the {@code ServerPlayerInteractionManagerObserverMixin}'s
 * own {@code @Invoker} onto {@code ServerPlayerInteractionManager
 * .tryBreakBlock(int, int, int)}, passed in as a method reference so
 * {@link ServerHarvestExecutor} never depends on the Mixin type itself.
 * Every invocation goes through the exact same authoritative method the
 * origin break did — drops, durability, permissions, and protection all
 * resolve through the ordinary pipeline; this interface adds no bypass.
 */
@FunctionalInterface
public interface ServerBreakInvoker {
    boolean attempt(int x, int y, int z);
}
