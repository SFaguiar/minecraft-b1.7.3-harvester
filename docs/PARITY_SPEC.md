# Behavioral parity specification

This document records externally observable Harvester 1.x behavior without carrying
forward its implementation. Every item remains subject to independent tests during
the StationAPI port. The legacy `README`/`CHANGELOG`/`docs/DESIGN.md`/`docs/HOOK.md`
and preserved source (`_legacy/harvester-remediation`) are used here strictly as a
source of *functional requirements* — never as architectural prescription. The
legacy hook mechanism (a ModLoader-era client base-edit of
`PlayerControllerSP.sendBlockRemoved`) is historical context only; it does not
mandate any particular Mixin, class, or injection point for the StationAPI port
(see `docs/HOOKS.md` and `better-beta-program/docs/decisions/0002-block-break-hook-selection.md`
for the port's own, independently chosen hook).

Harvester is a **single unified chain-breaking engine**, not two separate
features: the same 26-connected flood fill over a configurable block group serves
both the "vein miner" use case (ore groups) and the "tree feller" use case (the
wood group breaking connected logs). There is no separate tree-felling algorithm,
canopy detection, or leaf handling — see "Permanently out of parity scope" below.

## Activation and configuration

- Chaining is active only while the configured activation key is held (hold-to-
  activate, not a toggle).
- The historical default activation key is V.
- A separate configuration key opens the configuration UI; the historical default
  is H.
- Configuration covers group enablement, activation key, chain limit, and optional
  durability cost.
- Configuration values persist across game sessions (reloaded on startup, saved
  when changed). The legacy file path and format
  (`config/mod_Harvester.cfg`, Java `Properties`) are historical implementation
  detail, not a requirement — the 2.x port chooses its own versioned config
  format per `docs/ENGINEERING_STANDARDS.md` section 7.
- Missing configuration uses defaults. Invalid values must fail safely and produce a
  useful diagnostic instead of corrupting a world.

**Implemented (minimal autonomous config, no external dependency, no GUI):**
`config/harvester.properties`, resolved via Fabric Loader's config directory
(`FabricLoader.getInstance().getConfigDir()`), loaded once at client startup
(`HarvesterClientEntrypoint`) and consumed by `SingleplayerHarvestExecutor`,
`SingleplayerHarvestDiscoveryAdapter`, and the break-observer Mixin through
`HarvesterConfigState`. A missing file is created with these defaults, never
treated as an error.

| Property | Default | Notes |
| --- | --- | --- |
| `enabled` | `true` | Gates only the automatic additional-block chain; `false` never blocks the manual origin break. |
| `maxChain` | `64` | Total blocks per activation, including the origin block. Invalid (non-integer, `< 1`, or `> 100`) falls back to `64`. `100` is the largest limit the legacy Harvester's own UI ever exposed. |
| `neighborhood` | `legacy_26` | `legacy_26` or `orthogonal_6`. Invalid value falls back to `legacy_26`. |
| `diagnosticLogging` | `false` | When `false`, suppresses per-candidate and durability-snapshot debug/info logs; warnings and errors always log. Applies to logs and ores alike. |
| `harvestLogs` | `true` | Gates only the automatic log chain; `false` never blocks breaking a log by hand. |
| `harvestOres` | `true` | Gates only automatic ore chains; `false` never blocks breaking an ore by hand. |

`enabled=false` gates every automatic chain before discovery even runs;
`harvestLogs`/`harvestOres` gate only their own group kind, independently, and
both may be `true` at once. `maxChain`, `neighborhood`, and `diagnosticLogging`
apply uniformly to logs and ores — there is no separate limit or connectivity
setting per group kind. Any missing or invalid property falls back to that
property's own default with at most one warning logged; unknown properties are
ignored; a pre-existing file missing the two newer properties is never
rewritten to add them — it loads with both defaulting to `true`. The
activation key itself is intentionally **not** in this file — it stays
configurable only through the vanilla controls screen
(`HarvesterKeyBindingListener`), per the "no duplicated configuration
surface" rule. Example file:

```properties
enabled=true
maxChain=64
neighborhood=legacy_26
diagnosticLogging=false
harvestLogs=true
harvestOres=true
```

## Eligible blocks and tools

- Logs form one wood group, classified by the semantic tag `BlockTags.LOGS`/
  `c:logs` rather than raising a Harvester-specific tool requirement.
  **Decided** (see `better-beta-program/docs/operations/OPEN_QUESTIONS.md`
  Q0003, `DECIDED`): the 2.x port groups by this tag
  (`state.isIn(BlockTags.LOGS)`), not by raw block ID or metadata — every log
  species sharing that tag chains together, mirroring the legacy's
  ID-only (metadata-blind) grouping in effect, while additionally recognizing
  correctly-tagged logs from other mods. Already implemented in
  `SingleplayerHarvestDiscoveryAdapter` and exercised in real runtime
  (`CLM-0019`). Vanilla Beta 1.7.3 does not require a tool to harvest logs;
  the Harvester imposes no axe requirement of its own — each block is broken
  through the ordinary vanilla flow, which accepts an empty hand for wood.
  Validated in real runtime: a full additional-candidate chain completed
  successfully with no tool held. Do not read this bullet as documenting a
  future axe requirement as already-implemented behavior — none exists today.
- Coal, iron, gold, diamond, redstone, and lapis form distinct ore groups and require
  an appropriate pickaxe. **Decided** (Q0005/Q0006, both `DECIDED`): redstone
  groups by `BlockTags.REDSTONE_ORES`/`c:ores/redstone` (covering both the lit
  and unlit vanilla variants, StationAPI's own internal `BlockState`
  representation preserved as-is); lapis groups by
  `BlockTags.LAPIS_ORES`/`c:ores/lapis`, with no special ID/metadata handling,
  the same pattern as the other ore groups. **Decided** (Q0012, `DECIDED`,
  new): mod-added ores of the same material chain together via a shared
  specific `c:ores/<material>` tag (never the generic umbrella `c:ores` tag
  alone), with a conservative same-registered-block-identity fallback for
  mods that only tag the umbrella tag. **Implemented and validated in real
  runtime** (`HarvestGroupResolver`/`HarvestGroup` in `core`,
  `StationBlockDescriptors` adapter):
  - **Group model**: an origin resolves to exactly one of three kinds, in
    this priority order — `LOGS` (a block tagged both as a log and an ore,
    a datapack/mod misclassification, is always treated as a log; this
    priority is enforced for every candidate too, not just the origin, so
    an ore chain can never absorb a log block); `ORE_SPECIFIC_TAGS` (the
    origin's full captured set of specific material tags — a candidate
    matches if it shares at least one exact tag, never by prefix); or
    `ORE_IDENTITY_FALLBACK` (only when the origin is in the generic `c:ores`
    aggregator but carries no specific tag — the candidate must then share
    the exact same registered block identity, never a class name,
    translated name, or metadata value; two different blocks each only in
    `c:ores` never group together). A specific tag is detected generically
    as any tag with `namespace=c`, `path` starting with `ores/` and a
    non-empty suffix — enumerated from the block's own tags
    (`BlockState.streamTags()`), never a fixed list of `BlockTags`
    constants, so a correctly-tagged modded ore (e.g. `c:ores/tin`)
    participates without a code change. A block with no log tag and no ore
    tag (generic or specific) is ineligible: the manual break proceeds
    normally, no BFS runs, no chain starts, no error is raised.
  - **Tool safety**: the automatic ore chain never starts unless the
    pre-break held item can *correctly* harvest the origin, via the
    authoritative StationAPI check
    (`ItemStack.isSuitableFor(PlayerEntity, BlockView, BlockPos,
    BlockState)`, confirmed by bytecode to delegate, for an ordinary item,
    to the same per-item harvestability check the normal break pipeline
    itself consults) — never a hardcoded pickaxe class/ID check. Logs never
    gate on tool at all; an empty hand keeps working for logs exactly as
    before. Before every additional ore break, both the candidate's group
    membership and the currently held item's harvestability are
    revalidated fresh; the chain stops (without breaking that candidate) at
    the first failure of either. A block that itself breaks the tool still
    counts as a success, and the chain stops immediately after — unchanged
    from the existing durability-driven stop.

  Implemented and covered by unit tests (see `HarvestGroupResolverTest`,
  `HarvestGroupTest`, `HarvestGroupConnectedBlockFinderIntegrationTest`,
  `HarvestToolCompatibilityTest`). Confirmed in real singleplayer runtime:
  log regression unaffected by the ore addition; coal, iron, gold, and lit
  redstone each producing their own multi-block `CHAIN_COMPLETED` (up to
  34 additional blocks for coal), with adjacent different-material ore
  never merging into one plan; an empty hand and a wrong-tier tool each
  correctly preventing discovery on an ore origin while leaving the
  manual break itself unaffected; a tool breaking mid-ore-chain
  (`STOPPED_TOOL_CHANGED`, 5 of 14 planned candidates broken, the
  tool-breaking block itself counted as a success, nothing broken after);
  `maxChain` and `orthogonal_6` applying to ore exactly as they already
  did to logs; and `diagnosticLogging` gating the same per-candidate/
  durability detail for ore as for logs. **Not fully confirmed this session**: lapis was found only as an
  isolated block (classified correctly via its specific tag, but no
  multi-block lapis chain was exercised), and while two lit-redstone
  origins each produced multi-block chains, whether any unlit redstone
  was actually swept into one of those chains was never independently
  confirmed — no unlit redstone origin appeared on its own, but
  `includedBlocks` (which would show the mix directly) is not emitted at
  this `diagnosticLogging` level, and the player did not separately
  describe the geometry. Treat lit/unlit sharing one chain as plausible,
  not proven, pending a dedicated repetition.
- Vanilla harvest eligibility and tool tier remain authoritative.
- Breaking an ineligible block, using an unsuitable tool, or releasing the activation
  key preserves ordinary game behavior. In particular: a tool that cannot harvest
  the origin block must neither start a chain nor produce any drop the tool's
  ordinary vanilla use would not have produced — the tool/tier check happens
  before any chain-only removal or drop calculation, never after.

## Chain behavior

- Blocks connected through faces, edges, or corners are treated as connected
  (full 26-neighbor 3D adjacency, diagonals included).
- The configured maximum (`maxChain`) bounds the **total** size of the chain,
  including the block broken by hand. **Decided** (see
  `better-beta-program/docs/operations/OPEN_QUESTIONS.md` Q0002, `DECIDED`):
  the hand-broken block occupies one unit of the limit — `maxChain = 64`
  permits at most 1 original block plus 63 additional blocks — matching the
  legacy's proven behavior (`broken` starts at `1`). This is the interpretation
  the current `core` module already implements (`HarvestRequest`/`HarvestPlan`,
  see `CLM-0012`).
- The legacy UI exposed limits 8, 16, 32, 64, and 100; the historical default was 64.
- Processing must not force-load chunks or operate outside valid world height.
- Each position is processed at most once.
- The flood fill's visitation order must reproduce the legacy's exact
  deterministic order. **Decided** (Q0004, `DECIDED`): iterative BFS, FIFO
  queue, outer loop `dx`, then `dy`, then `dz`, values -1 to 1, excluding only
  `(0, 0, 0)` — exactly 26 neighbors in a stable order. This matters for which
  blocks are included when the limit truncates the search mid-fill. The
  existing `LegacyTwentySixNeighborhood` implementation already follows this
  exact loop nesting (confirmed by direct source review, not by a dedicated
  bit-for-bit comparison test).

## Drops and durability

- Eligible chained blocks use the game's applicable harvesting and drop rules,
  exactly as vanilla would compute them per block.
- **Decided** (see `better-beta-program/docs/operations/OPEN_QUESTIONS.md`
  Q0009, `DECIDED` — this corrects the previous revision of this section,
  which described consolidated drops): drops are **not** consolidated by the
  Harvester. Each additional block passes individually through the game's
  normal break flow — the world-owning authority (the local process in
  singleplayer, exclusively the server in multiplayer) executes the break and
  spawns whatever drops that normal flow produces, at that block's own
  position. The Harvester never aggregates stacks, never computes drops
  itself, and never calls a direct item-spawn as a substitute for a normal
  break — this preserves compatibility with modded ores/woods, custom drops,
  random quantities, experience, tool-tier requirements, events, terrain
  protections, `BlockState` changes, Fortune-like mechanics, and third-party
  machines/automation. Drop-entity consolidation may be studied later as a
  separate optimization, never as a requirement of the first functional
  version.
- Optional durability cost is proportional to additional blocks processed: one
  ordinary vanilla durability application per extra block, not a custom
  multiplier. **Decided** (Q0007, `DECIDED`): no charge for a block that
  disappeared before execution, stopped belonging to the group, was refused,
  could not be broken, or failed during the operation; immediate chain
  interruption when the tool breaks; no Harvester-specific multiplier; no
  direct manipulation of the damage value; no duplicate durability call. Tool
  compatibility is not restricted to concrete vanilla classes (e.g. `AxeItem`/
  `PickaxeItem`) — the normal break path is reused so tool/player/block/
  StationAPI hooks still run for modded tools. The origin block's own vanilla
  durability cost and drop remain entirely untouched by the chain — the hook
  observes the origin block only after vanilla has already fully resolved it.
- If the tool breaks, processing stops at the same boundary expected from ordinary
  tool use.
- "Unbreaking"-style enchantment interaction is `NOT_APPLICABLE` (Q0007,
  `DECIDED`) — Beta 1.7.3 predates the introduction of enchanting entirely. If
  a future backport ever adds an equivalent mechanic, reusing the normal break
  flow is expected to let that mod's own mechanic apply without special-casing
  here.

## Reliability and sides

- One failure must not cause repeated log spam or leave chaining permanently active.
- World restart, configuration reload, and mod removal where safe require manual
  coverage.
- Harvester 1.x was single-player. Client/server authority and dedicated-server
  behavior for 2.x are new design requirements, not presumed parity — the
  legacy implementation had no precedent to draw on for either aspect below,
  since it was single-process/single-player and explicitly disabled drop
  spawning in any multiplayer world (`world.multiplayerWorld` guard). Both are
  now **decided** (full text in
  `better-beta-program/docs/operations/OPEN_QUESTIONS.md` Q0008/Q0009, and in
  `better-beta-program/docs/decisions/0002-block-break-hook-selection.md`'s
  multiplayer activation policy section) — neither is implemented in code yet:
  - Multiplayer activation is opt-in per server, defaults to disabled, requires
    an explicit in-game warning before first enabling on a server, and is only
    effectively active when the player's per-server preference, the held key,
    and the server's own policy/validation all agree
    (`effectiveActive = playerOptIn AND keyHeld AND serverAllows AND
    serverValidationPasses`) — the server is always authoritative and the
    client can never force the mechanic to run.
  - Drops are never consolidated; each additional block is broken individually
    by the world-owning authority (exclusively the server in multiplayer),
    which spawns whatever drops the normal break flow produces at that block's
    own position, synchronized to observers by the platform's ordinary entity
    mechanism.

## Permanently out of parity scope

These are not "not yet implemented" — they were never part of Harvester 1.x and
are not a goal for 2.x parity at any point:

- Leaf decay, automatic leaf/canopy removal, or replanting after a wood-group
  chain break. The legacy design log records a period tree-felling mod
  ("Timber!") as a feasibility reference only — explicitly "not used as a code
  base" — and the shipped legacy implementation never grew leaf-handling logic.
  The wood group in this spec is scoped to chaining connected log blocks only.
- Any tree-feller behavior beyond what the same unified chain-break engine
  already provides for connected logs.

## Explicit non-goals for the foundation increment (temporary — not yet built)

No flood fill, vein mining, tree felling, durability algorithm, final UI, or
networking implementation is present yet. Unlike the permanent exclusions
above, everything in this section is expected to be built in a later
increment. Drop consolidation is not in this list because it is not a
temporary gap — see "Drops and durability" above (Q0009, `DECIDED`): drops
are never consolidated, by design, not yet.
