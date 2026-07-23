# Harvester

> **Pre-release (`1.0.0-beta.1`).** This is not the final stable release.
> The planned UX/scope work — an in-game configuration screen, drop
> consolidation onto the originally-broken block, tool-gating (axe-only
> tree chains, etc.), per-category/per-block toggles, and new block
> categories (dirt/gravel, leaves, mature crops) — is now implemented and
> covered by automated tests, but is pending a final manual runtime pass
> before a stable `1.0.0` is tagged. Behavior described below may still
> change (see `CHANGELOG.md` and `docs/PRE_RELEASE_HANDOFF.md`).
>
> **Back up any world you care about before trying this.** Testing and
> feedback are welcome and expected — please report issues against the
> repository.

Harvester automatically continues breaking a connected group of tree logs
or a single ore vein after you break one block by hand, using the same
tool and durability rules as a normal break — no new drops, no bypassed
game rules.

## Requirements

- Minecraft Java Edition Beta 1.7.3
- Babric / Fabric Loader `0.18.4`
- StationAPI `2.0.0-alpha.6.2`
- Java 17

## Installation (MultiMC)

1. Create or open a MultiMC instance already qualified for Babric +
   StationAPI on Beta 1.7.3 (Fabric Loader `0.18.4`, StationAPI
   `2.0.0-alpha.6.2`, Java 17).
2. Copy `harvester-b1.7.3-<version>.jar` into that instance's `mods`
   folder.
3. Launch the instance.

## Singleplayer

Harvester works out of the box. Hold the activation key (default `V`)
while breaking a log or ore block; the connected group of the same
material breaks in a chain, one block at a time, through the normal
break pipeline (normal drops, normal tool durability, normal breakage
rules). Releasing the key or letting the tool break stops the chain.

## Multiplayer (optional)

Multiplayer support is server-authoritative and off by default.

- **The dedicated server needs Harvester installed** to run the
  additional-block chain; the client only ever sends a single "I'm
  holding the key" boolean — the server recomputes everything else
  (group, chain, tool check, drops, durability) itself.
- A StationAPI client **without** Harvester installed connects normally
  to a Harvester server; it silently ignores the server's Harvester
  handshake and behaves exactly like a plain StationAPI client.
- A client **without StationAPI** is not compatible with a StationAPI
  server (Harvester or not) — this is a StationAPI-level requirement,
  not a Harvester-specific one.
- The server stays silent about Harvester unless its own
  `multiplayerAllowed=true` (see below). Even then, each client must
  separately opt in per server before the activation key does anything
  there — enabling Harvester on one server never enables it on another,
  and it never changes singleplayer behavior.

## Configuration

Harvester writes a fully documented `harvester.properties` under the
Fabric config directory on first run (both client and dedicated server),
and migrates an older file in place without losing your settings. On a
client you can also edit it in-game: press the **config key** (default
`H`, rebindable in Controls) to open the Harvester screen — a
self-contained screen that needs no other mod and edits the same file.
The dedicated server is configured by the file only; no graphical class
ever loads there.

Key settings (the file itself documents every option):

```properties
enabled=true
maxChain=64
neighborhood=legacy_26
consolidateDrops=true
harvestLogs=true
harvestOres=true
harvestDirt=false
harvestGravel=false
harvestLeaves=false
harvestCrops=false
undergroundRequiresNoSky=true
undergroundMaxY=63
undergroundOverworldOnly=true
allowlist=
denylist=
diagnosticLogging=false
multiplayerAllowed=false
```

- `consolidateDrops` — merge the whole action's drops into stacks at the
  center of the block you broke, instead of scattering them along the vein.
- The new `harvestDirt`/`harvestGravel`/`harvestLeaves`/`harvestCrops`
  categories are **off by default** — opt in per category.
- `allowlist`/`denylist` — comma-separated block identifiers. Precedence:
  denylist > allowlist > category toggle > default. The denylist always
  blocks; the allowlist only releases a block whose category is already
  recognized (it never invents one).

- `maxChain` — total blocks per activation, including the origin block.
  Whole number from 1 to 100; an invalid or out-of-range value falls
  back to 64.
- `neighborhood` — `legacy_26` (default, full 26-neighbor adjacency,
  matching legacy Harvester 1.x) or `orthogonal_6` (face adjacency
  only).
- `harvestLogs` / `harvestOres` — independently toggle the automatic
  chain for each material family; either `false` still lets you break
  that block type by hand.
- `multiplayerAllowed` — **server-only**, default `false`. A dedicated
  server never announces Harvester multiplayer support to clients
  unless this is `true`.
- A client's own local, per-server opt-in (`multiplayerOptIn`) is
  stored separately per server address under
  `config/harvester/servers/`, defaulting to `false` for every new
  server.

## Supported blocks

Each category chains only while you hold the matching tool; a bare hand
never starts a chain (but always still breaks the single block you aimed
at, exactly like vanilla).

- **Logs** (axe): any block tagged as a log (`c:logs` / vanilla
  `minecraft:log`); different species chain together.
- **Ores** (suitable pickaxe): any block carrying a specific ore tag
  (`c:ores/<material>`, e.g. coal, iron, gold, redstone), gated by the
  same tool-suitability check vanilla uses for that block.
- **Dirt / gravel** (shovel, off by default): only underground —
  Overworld, no direct sky access, and `Y <= 63` by default. Gravel
  breaks top-down so a column never collapses onto an unbroken block.
- **Leaves** (shears, off by default): the same leaf species, six-face
  connectivity.
- **Mature crops** (hoe, off by default): fully-grown wheat only, on one
  farmland layer — an immature plant is never broken by the chain, and
  there is no automatic replanting.

## Known limitations

- The dedicated-server administrative rate limit for multiplayer
  activation transitions is fixed at 4 per second per player; it is not
  yet configurable.
- There is no in-game GUI for configuration; all settings are plain
  properties files.
- Multi-block lapis veins and independently-triggered unlit-redstone
  participation in an adjacent lit-redstone chain are supported by the
  same generic logic but have not been directly observed in a large
  enough vein during manual testing.
- Log drops have only been confirmed by visual observation, not by
  automated inventory assertions.

See `CHANGELOG.md` for release history and `docs/ARCHITECTURE.md` for
implementation detail. Contributors: see `AGENTS.md` for provenance and
repository rules.

## License status

No Harvester 2.x license is granted yet. The intended program default is
0BSD, but relicensing is blocked until the authorship and provenance
audit is complete. Dependencies retain their own licenses.
