# Pre-release handoff — Harvester `1.0.0-beta.1`

Canonical handoff document for continuing Harvester development without
depending on any external conversation history. This is a living
document: update it in place rather than creating a parallel one when the
state below changes.

## Status

**Pre-release, not the final stable version.** Published on branch
`port/stationapi-v2`, tag `v1.0.0-beta.1` (see `git log`/`git tag -n99` for
the exact commit — this document does not restate a hash that would go
stale). The functional core (singleplayer vein harvesting and optional
server-authoritative multiplayer) is implemented, tested, and manually
validated, but several UX and scope adjustments are planned before a
final `1.0.0` — see "Required adjustments before the final version" below.
Do not present this build as final or stable to end users beyond
testing/feedback purposes.

## Current technical state

- Branch: `port/stationapi-v2`.
- Version: `1.0.0-beta.1` (`gradle.properties`, `mod_version` — the single
  canonical source; `fabric.mod.json` expands `${version}` at build time
  via `build.gradle.kts`, no duplicated source of truth).
- Pinned platform: Minecraft Beta 1.7.3, Babric, Fabric Loader `0.18.4`,
  StationAPI `2.0.0-alpha.6.2`, mappings `b1.7.3+e1fe071`, Java 17, Gradle
  Wrapper `8.12.1`. Exact versions only — never `latest`/`+`/`SNAPSHOT`.
  Full dependency table: `docs/DEPENDENCIES.md`.
- Build/test: `.\gradlew.bat clean test classes --console=plain`, always
  with the pinned JDK 17
  (`C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot`), never the
  machine's global Java. Production JAR: `.\gradlew.bat build`, output at
  `build/libs/harvester-b1.7.3-<version>.jar` (the remapped jar; the
  `-dev.jar` in `build/devlibs` and `-sources.jar` are not the
  distributable).
- Tests: 279 automated tests, 0 failures (221 at the beta.1 tag, plus the
  1.0.0 config/consolidation/category work).
- Architecture: `core` (pure BFS/classification domain logic, zero
  Minecraft/StationAPI/Fabric import) and `game` (side-agnostic
  classification/discovery/tool-compatibility shared between singleplayer
  and multiplayer) hold all gameplay logic; `client`/`server` hold only
  platform-specific glue (Mixin observers → executors). Singleplayer:
  `SingleplayerHarvestExecutor` via
  `SingleplayerInteractionManagerObserverMixin`. Multiplayer:
  `ServerHarvestExecutor` via
  `ServerPlayerInteractionManagerObserverMixin`, gated by
  `harvester:support` (server→client announcement),
  `harvester:active` (client→server, one boolean), per-player state
  (`HarvesterMultiplayerServerRegistry`), and a fixed 4/s rate limit. Full
  detail: `docs/ARCHITECTURE.md`.
- Safe defaults: `multiplayerAllowed=false` (server-only — a dedicated
  server never announces support unless set `true`);
  `diagnosticLogging=false`; per-server client opt-in
  (`multiplayerOptIn`) defaults `false` for every new server and is never
  inherited from another server.
- Known compatibility: a StationAPI client without Harvester connects
  normally to a Harvester server and silently ignores the handshake; a
  client without StationAPI is not compatible with a StationAPI server
  (Harvester or not) — StationAPI-level requirement, not Harvester's.
  Full matrix: `docs/COMPATIBILITY.md`, `docs/ARCHITECTURE.md` ("Final
  multiplayer compatibility matrix").
- Known limitations (do not re-litigate without a documented regression):
  fixed (non-configurable) multiplayer rate limit (kept fixed by owner
  decision); the in-game screen covers the simple toggles and `maxChain`
  only — the advanced `allowlist`/`denylist` and per-tool ID lists stay
  file-only for 1.0.0; lapis multi-block veins and independent unlit-redstone
  participation supported by the same generic logic but not directly
  observed in a large enough vein; log drops confirmed only by visual
  observation, not automated inventory assertion; no clean dedicated-server
  install outside the Loom/dev environment tested.

## Existing functionality

- Ore and log vein harvesting: hold the activation key (`V`) while
  breaking a supported log or ore block to chain-break the connected
  group.
- Deterministic BFS discovery over a configurable neighborhood
  (`legacy_26` default, full 26-neighbor adjacency matching legacy
  Harvester 1.x, or `orthogonal_6`, face adjacency only).
- Entirely through the normal vanilla break pipeline per block: normal
  tool-suitability gating, normal drops, normal tool durability — no
  Harvester-computed drops, no bypassed game rules.
- File-based configuration (`harvester.properties`, written with defaults
  on first run under the Fabric config directory): `enabled`, `maxChain`
  (1–100, default 64, invalid/out-of-range falls back to 64),
  `neighborhood`, `diagnosticLogging`, `harvestLogs`, `harvestOres`,
  `multiplayerAllowed`.
- Hold-to-activate key binding (`V`), no GUI.
- Optional, server-authoritative multiplayer: support discovery,
  per-player opt-in and activation state, server-side rate limiting,
  deterministic lifecycle cleanup, and full server-side chain execution
  (the server runs the same BFS chain singleplayer runs, through the same
  vanilla `tryBreakBlock` pipeline, with a reentrancy guard preventing the
  chain's own breaks from re-triggering discovery).
- Local per-server client opt-in plus server-side `multiplayerAllowed`
  permission — both required simultaneously for the feature to activate
  on a given server.

## Required adjustments before the final version

**Status: all eight are now implemented and covered by automated tests
(279 total, 0 failures), across three commits — configurable categories;
drop consolidation; and underground/leaf/crop harvesting. What remains
before a stable `1.0.0` is the single reduced manual runtime pass (see
below) and the operator's approval to bump the version and tag.** A
server-side runtime smoke has already confirmed the mod (including the new
`@WrapMethod` drop-consolidation mixins) loads cleanly and that config
migration works in real runtime. The list below is kept as the record of
what each item became.

1. **In-game configuration UI integration.** Investigate and integrate
   with whatever graphical configuration solution is actually available
   for Babric/StationAPI (Mod Menu Babric, Glass Config API, or
   equivalent — verify current state before assuming either exists in a
   usable form). Needs to cover at minimum: enable/disable, a
   configurable activation key, chain limits (`maxChain`), categories
   (logs/ores/future categories), and any other setting currently only
   reachable by hand-editing `harvester.properties`.
2. **Consolidate chain drops.** Currently each block in the chain drops
   normally and independently (vanilla per-block behavior, scattered
   along the vein). Change this so that: compatible item stacks combine,
   normal max stack size is respected, and all drops surface at the block
   the player originally broke by hand — while still preserving vanilla
   drop *calculation* (quantity, fortune, etc.) per block, only changing
   *where* the resulting item entities spawn.
3. **Require the correct tool to start a chain.** Trees currently chain
   regardless of held item; change so that only an axe (any valid axe,
   mirroring vanilla tool-suitability) starts a tree chain — bare hands
   must not trigger it. Note this only affects whether the *chain*
   starts; a manual bare-hand break of a single log must continue to work
   exactly as vanilla does today.
4. **Per-category and per-block enable/disable.** Beyond the current
   `harvestLogs`/`harvestOres` pair, allow enabling or disabling specific
   categories and specific blocks independently.
5. **Add underground dirt and gravel.** Shovel-only (any valid shovel).
   Underground detection still needs an architectural decision — surface
   dirt/gravel chaining indiscriminately would almost certainly be
   unwanted, but the exact detection strategy (light level, Y threshold,
   sky visibility, biome, something else) is undecided; do not implement
   before that decision is made.
6. **Add leaves.** Shears-only.
7. **Add fully-mature crops.** Hoe-only, and only when the crop's growth
   stage is fully mature — never harvest an immature plant via the chain.
8. **Audit for other genuinely relevant gaps** before the stable release,
   without arbitrarily expanding scope beyond what is actually needed for
   a coherent final release.

## Remaining before `1.0.0`: reduced manual runtime

Operator-run, one client pass plus one dedicated-server pass
(`online-mode=false`). Only the new 1.0.0 behavior — the earlier
tranches (StationAPI-only compatibility, vanilla-server, rate limiter,
full lifecycle, dimension change, T1–T6) are not repeated:

1. Config screen opens (config key), toggles/`maxChain` change and persist
   to `harvester.properties`; absence of any optional mod causes no error.
2. A log does **not** chain with a bare hand or the wrong tool; it does
   with an axe; a bare-hand break of a single log still works.
3. Consolidation: a supported chain drops **one** merged pile at the
   origin center, **including the origin's own drop**.
4. Stacks above the max size are split into multiple stacks.
5. A drop from an unrelated adjacent block is not captured.
6. Dirt/gravel chain only underground, never on the open surface; gravel
   does not collapse improperly.
7. Leaves chain by species with shears only (no pulling a neighboring tree).
8. Mature wheat chains with a hoe; an immature plant is left intact.
9. One multiplayer server-authoritative pass of a chain plus consolidation.

A second player is only needed if the collector is found to share global
state; its per-world/per-owner isolation is already covered by pure tests,
so a single client is sufficient otherwise. Once this passes, bump the
version and tag per the release process — not done here.

## Where to look next

- `docs/ARCHITECTURE.md` — full implementation detail, including the
  multiplayer protocol and the accepted block-break hook decision
  (`better-beta-program/docs/decisions/0002-block-break-hook-selection.md`,
  `Accepted`, Revision 5).
- `docs/COMPATIBILITY.md`, `docs/PARITY_SPEC.md`, `docs/TEST_PLAN.md` —
  compatibility, behavioral parity, and test coverage detail.
- `better-beta-program/docs/operations/CURRENT_STATE.md` and
  `NEXT_ACTIONS.md` — canonical program-wide operational state (separate
  repository; this document is the Harvester-repository-local summary and
  does not replace it).
- `better-beta-program/docs/knowledge/` — the full evidence ledger
  (claims, experiments) behind every `PROVEN_WITHIN_SCOPE`/
  `STRONG_INFERENCE` statement referenced above.
