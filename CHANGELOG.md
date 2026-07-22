# Changelog

All notable changes to Harvester are recorded here. Versions follow
[SemVer](https://semver.org/).

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
