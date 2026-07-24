# Changelog

All notable changes to Harvester are recorded here. Versions follow
[SemVer](https://semver.org/).

## 1.0.0 — 2026-07-24

Stable release. Implemented since `1.0.0-beta.1`, covered by 279
automated tests (0 failures) and a final 10/10 manual runtime gate
across singleplayer, multiplayer, and dedicated server. No known
blocking defects.

- **In-game configuration screen.** An autonomous, client-only screen
  (opened by a rebindable key, default `H`) editing the same
  `harvester.properties` — no external mod dependency, and nothing
  graphical ever loads on a dedicated server. (The Glass Config API was
  evaluated and set aside: it owns its own YAML file, so it cannot keep
  `harvester.properties` the single authoritative source, and its current
  line targets Java 21 against this project's pinned Java 17.)
- **Drop consolidation.** The whole action's drops — including the block
  you broke by hand — are merged (respecting real stack sizes) and appear
  in one pile at the center of the origin block. Vanilla drop calculation
  is unchanged; only where the item entities spawn changes. On by default.
- **Tool gates by category.** Logs now require an axe to start a chain
  (bare hands never do); dirt/gravel require a shovel, leaves shears,
  crops a hoe; ores keep the vanilla pickaxe-suitability check. Vanilla
  tools are recognized by class, with a configurable per-category ID
  allowlist for mod tools.
- **Per-category and per-block enable/disable.** Category toggles plus
  file-based `allowlist`/`denylist` (precedence: denylist > allowlist >
  category > default; the allowlist never invents a category).
- **New categories (off by default).** Underground dirt and gravel
  (shovel; Overworld + no direct sky + `Y <= 63`; six-face; gravel breaks
  top-down for gravity safety); leaves (shears; six-face; same species);
  fully-mature wheat (hoe; horizontal-only; never harvests an immature
  plant; no auto-replant).
- Configuration migrates pre-1.0.0 files in place, non-destructively
  (existing values preserved, new keys appended).

## 1.0.0-beta.1

**Pre-release, not the final stable version.** Published for testing and
feedback while additional functionality and UX adjustments are planned
before a final release — see `docs/PRE_RELEASE_HANDOFF.md` for the full
list of planned changes (in-game configuration UI integration, drop
consolidation, axe-only tree chains, per-category/per-block toggles,
dirt/gravel with shovel, leaves with shears, mature-only crops with hoe,
and any other gaps found relevant before a stable release). Behavior
described below may change before `1.0.0` ships. **Back up any world you
care about before testing this build.**

- Ore and log vein harvesting: hold the activation key (`V`) while
  breaking a log or a specifically-tagged ore block to chain-break the
  connected group, through the normal vanilla break pipeline (normal
  drops, normal tool durability, normal tool-suitability gating).
- Deterministic BFS discovery over a configurable neighborhood
  (`legacy_26`, matching legacy Harvester 1.x, or `orthogonal_6`).
- File-based configuration (`harvester.properties`): `enabled`,
  `maxChain` (1–100, default 64), `neighborhood`, `diagnosticLogging`,
  `harvestLogs`, `harvestOres`, `multiplayerAllowed`.
- Optional, server-authoritative multiplayer support:
  - a dedicated server announces Harvester support only when
    `multiplayerAllowed=true`;
  - each client opts in independently per server (default off, never
    inherited across servers);
  - the client sends only a single activation boolean, never
    coordinates, blocks, tools, or a computed plan;
  - the server validates activation (opt-in AND key held AND server
    allows), rate-limits activation transitions, and owns all gameplay
    decisions;
  - the server runs the same deterministic chain singleplayer runs,
    entirely through the vanilla break pipeline.
- Known compatibility: a StationAPI client without Harvester connects
  normally to a Harvester server and silently ignores the Harvester
  handshake; a client without StationAPI is not compatible with a
  StationAPI server.
- 221 automated tests, 0 failures.
