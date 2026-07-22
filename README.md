# Harvester

> **Pre-release (`1.0.0-beta.1`).** This is not the final stable release.
> Additional functionality and UX adjustments are still planned before a
> final version (see `CHANGELOG.md` and `docs/PRE_RELEASE_HANDOFF.md`)  —
> notably in-game configuration UI integration, drop consolidation onto
> the originally-broken block, tool-gating for tree chains (axe-only),
> per-category/per-block toggles, and new block categories (dirt/gravel,
> leaves, mature crops). Behavior described below may change.
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

Harvester writes `harvester.properties` under the Fabric config
directory on first run (both client and dedicated server), with these
defaults:

```properties
enabled=true
maxChain=64
neighborhood=legacy_26
diagnosticLogging=false
harvestLogs=true
harvestOres=true
multiplayerAllowed=false
```

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

- **Logs**: any block tagged as a log (`c:logs` / vanilla `minecraft:log`).
- **Ores**: any block carrying a specific ore tag (`c:ores/<material>`,
  e.g. coal, iron, gold, redstone), gated by the same tool-suitability
  check vanilla uses for that block.

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
