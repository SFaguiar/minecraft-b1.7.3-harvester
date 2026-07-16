# Behavioral parity specification

This document records externally observable Harvester 1.x behavior without carrying
forward its implementation. Every item remains subject to independent tests during
the StationAPI port.

## Activation and configuration

- Chaining is active only while the configured activation key is held.
- The historical default activation key is V.
- A separate configuration key opens the configuration UI; the historical default
  is H.
- Configuration covers group enablement, activation key, chain limit, and optional
  durability cost.
- Missing configuration uses defaults. Invalid values must fail safely and produce a
  useful diagnostic instead of corrupting a world.

## Eligible blocks and tools

- Logs form one wood group and require an appropriate axe.
- Coal, iron, gold, diamond, redstone, and lapis form distinct ore groups and require
  an appropriate pickaxe.
- Vanilla harvest eligibility and tool tier remain authoritative.
- Breaking an ineligible block, using an unsuitable tool, or releasing the activation
  key preserves ordinary game behavior.

## Chain behavior

- Blocks connected through faces, edges, or corners are treated as connected.
- The configured maximum bounds the amount of additional work.
- The legacy UI exposed limits 8, 16, 32, 64, and 100; the historical default was 64.
- Processing must not force-load chunks or operate outside valid world height.
- Each position is processed at most once.

## Drops and durability

- Eligible chained blocks use the game's applicable harvesting and drop rules.
- Drops are consolidated into legal stack sizes near the initiating position.
- Optional durability cost is proportional to additional blocks processed.
- If the tool breaks, processing stops at the same boundary expected from ordinary
  tool use.

## Reliability and sides

- One failure must not cause repeated log spam or leave chaining permanently active.
- World restart, configuration reload, and mod removal where safe require manual
  coverage.
- Harvester 1.x was single-player. Client/server authority and dedicated-server
  behavior for 2.x are new design requirements, not presumed parity.

## Explicit non-goals for the foundation increment

No flood fill, vein mining, tree felling, drop consolidation, durability algorithm,
final UI, or networking implementation is present yet.
