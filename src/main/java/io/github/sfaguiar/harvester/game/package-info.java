/**
 * Side-agnostic game-integration logic (block classification, tool
 * compatibility, discovery, chain vocabulary) shared by the singleplayer
 * client and the multiplayer server — the single implementation each side
 * adapts, never duplicates. May import common Minecraft/StationAPI types
 * (e.g. {@code PlayerEntity}, {@code World}, {@code ItemStack},
 * {@code BlockState}); must never import a client-only ({@code
 * net.minecraft.client.*}) or server-only ({@code
 * net.minecraft.server.*}) type — that would defeat the side-safety this
 * package exists to preserve. {@code core} itself stays free of even the
 * common Minecraft/StationAPI types this package depends on.
 */
package io.github.sfaguiar.harvester.game;
