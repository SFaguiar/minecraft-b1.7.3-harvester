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

## Eligible blocks and tools

- Logs form one wood group and require an appropriate axe. The legacy
  implementation matches by block ID only, with no metadata/species filter, so
  every log species sharing that ID chains together — whether the 2.x port
  preserves this exact granularity or adopts per-species grouping via
  StationAPI's `BlockState` system is an open design decision (see
  `better-beta-program/docs/operations/OPEN_QUESTIONS.md` Q0003).
- Coal, iron, gold, diamond, redstone, and lapis form distinct ore groups and require
  an appropriate pickaxe. The legacy redstone group spans two block IDs
  (lit/unlit) treated as one group (open design decision on how to express this
  under StationAPI's block-state model — Q0005). No special ID/metadata handling
  was found for lapis beyond a single block ID, the same pattern as the other
  single-ID ore groups (Q0006, open only for confirmation).
- Vanilla harvest eligibility and tool tier remain authoritative.
- Breaking an ineligible block, using an unsuitable tool, or releasing the activation
  key preserves ordinary game behavior. In particular: a tool that cannot harvest
  the origin block must neither start a chain nor produce any drop the tool's
  ordinary vanilla use would not have produced — the tool/tier check happens
  before any chain-only removal or drop calculation, never after.

## Chain behavior

- Blocks connected through faces, edges, or corners are treated as connected
  (full 26-neighbor 3D adjacency, diagonals included).
- The configured maximum bounds the size of the chain. **Whether this count
  includes the block broken by hand or only the additional chained blocks is an
  open design decision** — the legacy implementation counts the hand-broken
  block toward the limit (`broken` starts at `1`), which reads in tension with
  an "additional work" framing; see
  `better-beta-program/docs/operations/OPEN_QUESTIONS.md` Q0002. `PARITY_SPEC.md`
  intentionally does not resolve this by itself.
- The legacy UI exposed limits 8, 16, 32, 64, and 100; the historical default was 64.
- Processing must not force-load chunks or operate outside valid world height.
- Each position is processed at most once.
- The flood fill's visitation order is deterministic in the legacy implementation
  for a fixed world state, but no specific order is mandated by this spec — see
  Q0004 for whether the 2.x search order must match the legacy order exactly
  (this matters only for which blocks are included when the limit truncates the
  search mid-fill).

## Drops and durability

- Eligible chained blocks use the game's applicable harvesting and drop rules,
  exactly as vanilla would compute them per block.
- Drops are consolidated into legal stack sizes and spawn **at** the initiating
  block's position, with the same small positional jitter vanilla uses for any
  dropped item (not merely "near" it, and not scattered per chained block).
- Optional durability cost is proportional to additional blocks processed: one
  ordinary vanilla durability application per extra block, not a custom
  multiplier. The origin block's own vanilla durability cost and drop are
  entirely untouched by the chain — the hook observes the origin block only
  after vanilla has already fully resolved it (removal, tier check, durability,
  drop), and the chain logic only ever affects the *additional* blocks.
- If the tool breaks, processing stops at the same boundary expected from ordinary
  tool use.
- Exact durability cost per block and any interaction with an "Unbreaking"-style
  enchantment remain an open question (Q0007) — Beta 1.7.3 predates the
  introduction of enchanting, so the interaction is presumed not applicable, but
  this should be confirmed before implementation rather than assumed.

## Reliability and sides

- One failure must not cause repeated log spam or leave chaining permanently active.
- World restart, configuration reload, and mod removal where safe require manual
  coverage.
- Harvester 1.x was single-player. Client/server authority and dedicated-server
  behavior for 2.x are new design requirements, not presumed parity. In
  particular, two aspects have **no legacy precedent to draw on** and remain
  open design questions, not gaps in this spec's research:
  - How a server authoritative for the chain learns that the client-only
    activation key is held (Q0008) — the legacy implementation never faced this,
    being single-process/single-player.
  - Which side owns spawning consolidated drops, and how their position stays
    correct for every observing client (Q0009) — the legacy implementation
    spawned drops in-process and explicitly disabled drop spawning entirely in
    any multiplayer world (`world.multiplayerWorld` guard), so it never solved
    this; it avoided it.

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

No flood fill, vein mining, tree felling, drop consolidation, durability algorithm,
final UI, or networking implementation is present yet. Unlike the permanent
exclusions above, everything in this section is expected to be built in a later
increment.
