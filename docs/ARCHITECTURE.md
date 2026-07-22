# Architecture

Harvester 2.x separates deterministic domain logic in `core` from StationAPI/Minecraft integration in `platform`, client-only and server-only entrypoints, versioned `config`, minimal `mixin`, and explicit optional `compat`. `core`, `config` (loader), and the ore/log classification model (`BlockDescriptor`/`HarvestGroup`/`HarvestGroupResolver`) are already implemented and singleplayer-validated (see `PARITY_SPEC.md`); this document now also records the researched, not-yet-implemented architecture for optional multiplayer.

Server authority is the target for multiplayer behavior. Client classes must never be linked from common/server code. Events and public APIs precede Mixins; every future Mixin needs a documented missing hook, narrow injection, failure behavior, and regression test. `@Overwrite` is disallowed without a new approved ADR.

## Optional multiplayer (tranche 1 implemented; remainder researched, not implemented)

This section was a pure research/design record; this document now also
records the first implemented tranche (vanilla compatibility, side
safety, server-initiated `harvester:support` announcement) validated by
this session's Spikes A–D, below. Every class and method name in this
section was confirmed against the pinned Loader
`0.18.4`/StationAPI `2.0.0-alpha.6.2`/mappings `b1.7.3+e1fe071` toolchain
by reading source jars (this session additionally read the Loom-remapped,
human-named StationAPI submodule sources jars directly — not just
disassembled bytecode) and, where noted, disassembling the merged jar's
bytecode — never assumed from modern Fabric API memory. See ADR 0002's
already-`DECIDED` "Multiplayer activation policy (Q0008)" and "drop
ownership (Q0009)" sections for the product-level rules this design
implements; this section is purely technical. ADR 0002 itself is not
modified by this tranche (see "ADR status" below).

### Owner decisions locking this tranche's scope

Decided by the repository owner for this implementation session,
superseding the "requires the repository owner" open questions listed
lower in this document where they overlap:

- **Discovery timeout**: exactly 5 seconds after the client connection
  becomes operational (`ServerLoginSuccessEvent`), not a placeholder
  value. On expiry, `SUPPORT_UNKNOWN` → `SUPPORT_UNAVAILABLE`. A valid
  announcement arriving later still moves the state to
  `SUPPORT_AVAILABLE_DISABLED` (late announcements are accepted, not
  terminal). The timeout only changes local client state — it never
  disconnects and never sends a packet.
- **Opt-in storage**: confirmed as a future client-only, per-server file
  (not implemented this tranche), default `false`, no GUI at this
  milestone. This tranche implements no persistence at all — the state
  machine only tracks the discovery/announcement handshake
  (`SUPPORT_AVAILABLE_DISABLED` is as far as it goes); there is no
  `SUPPORT_AVAILABLE_ENABLED` state or opt-in flag yet.
- **Rate limit**: fixed at 4 transitions/second/player in protocol v1,
  not operator-configurable in this initial version (resolves the "should
  `multiplayerRateLimitPerSecond` be configurable" open question below in
  favor of "no config surface yet"). Not exercised by this tranche's code
  (no `harvester:active` exists yet) — recorded here so the future
  server-side rate limiter is built to this fixed value, not a
  configurable one.
- **ADR status**: ADR 0002 is not edited by this tranche. A new,
  dedicated ADR for optional multiplayer and server-side authority is
  created only after this tranche's server-side break hook
  (`ServerPlayerInteractionManager.tryBreakBlock`) has actual runtime
  validation — not yet done; this tranche only validates the handshake
  (Spikes A–D), not the break hook (that remains Spike 7 in "Minimal
  experiments" below, still not run).
- **Per-player identity**: see "Per-player server state (design)" below
  — `UUID` is confirmed absent from this Minecraft version by bytecode
  (not merely "unproven"), so the transient key is the `ServerPlayerEntity`
  instance itself, not a `UUID`.
- **Same-JAR side safety classification**: downgraded from this
  document's earlier `PROVEN_WITHIN_SCOPE` to `STRONG_INFERENCE` by
  owner instruction, pending this session's actual client/dedicated-server
  runtime tests (Spikes A/B) — bytecode/source analysis alone, no matter
  how thorough, is evidence short of a real client and a real dedicated
  server both actually starting and behaving correctly. See "Spike
  results" below for whether this session's runtime tests promote it
  back.

### Same-JAR side safety (`PROVEN_WITHIN_SCOPE` — bytecode/source-grounded AND runtime-confirmed this session)

Started this tranche downgraded to `STRONG_INFERENCE` by owner
instruction pending this session's own Spike A/B runtime results;
promoted back to `PROVEN_WITHIN_SCOPE` after both actually ran — see
"Spike results (this session)" below. `runClient`'s log shows only
`stationapi:event_bus_client` entrypoints for `harvester`
(`HarvesterKeyBindingListener`, `HarvesterSupportClientListener`), never
`HarvesterServerSupportListener`; `runServer`'s log shows only
`stationapi:event_bus_server` for `harvester`
(`HarvesterServerSupportListener`), never either client-only class — the
same-JAR/opposite-side non-loading claim below is no longer only a
bytecode/`fabric.mod.json` reading, it is an observed fact from two real
process launches this session, not just the dev/Loom launches:

- `SingleplayerInteractionManager` (client, singleplayer) and
  `MultiplayerInteractionManager` (client, connected to a **remote**
  server — vanilla or modded) are distinct classes; a client connected
  remotely never instantiates `SingleplayerInteractionManager`.
  Confirmed by disassembling `SingleplayerInteractionManager.breakBlock`:
  it resolves harvestability/durability/drops entirely against the
  client's own local `World`/`ClientPlayerEntity` and never calls
  `ServerPlayerInteractionManager` — singleplayer in this codebase is
  client-authoritative for the player's own manual break, not routed
  through the internal server's per-player interaction manager.
- Beta 1.7.3 has no `IntegratedServer` subclass — singleplayer runs a
  real `MinecraftServer` internally (same class dedicated servers use),
  as a background thread. A **future server-side Mixin will load in
  singleplayer's internal server too** — its target method
  (`ServerPlayerInteractionManager.tryBreakBlock`) is simply never
  invoked for the player's own break in singleplayer, per the previous
  bullet. This must stay a property the implementation preserves
  (e.g. a manual test breaking a log/ore in singleplayer, then checking
  no `[HARVEST-EXEC]`-style server log line appears), not something
  assumed permanently true without a regression check.
- `fabric.mod.json` already declares separate `client`/`server`
  entrypoints (`HarvesterClientEntrypoint`/`HarvesterServerEntrypoint`,
  the latter already loads and logs on a real dedicated server per
  `COMPATIBILITY.md`) and `harvester.mixins.json` already has empty
  top-level `mixins` with separate `client`/`server` arrays — Fabric
  Loader guarantees a `client`-listed Mixin never applies on a dedicated
  server process and a `server`-listed Mixin never applies on a client
  that never starts a `MinecraftServer` (i.e. any environment other than
  singleplayer/LAN-host), so a future server-side break-observer Mixin
  is inert on a real remote connection by construction.
- `net.minecraft.world.ServerWorld extends net.minecraft.world.World`
  (confirmed by bytecode) and does not override `getBlockState` — every
  singleplayer-validated pure/adapter piece that takes a `World`
  (`StationBlockDescriptors.describe`, `BlockGroupView.byDescriptor`,
  `HarvestGroupResolver.resolve`, `ConnectedBlockFinder.discover`) is
  directly reusable with a `ServerWorld` instance, unchanged.
- No `stationapi:verify_client` custom `fabric.mod.json` field may ever
  be added — this opts a mod into StationAPI's `VanillaChecker`
  strict-parity mechanism (`ServerVanillaChecker.onPlayerLogin`, see
  below), which disconnects any connecting client missing the mod or
  running a different version. Harvester must never set this; it is not
  needed for the design below.

### Vanilla-server compatibility (`PROVEN_WITHIN_SCOPE` — runtime-confirmed this session)

StationAPI already ships a client↔server "is my peer also running this
exact StationAPI build" detector, and Harvester's own multiplayer
handshake is designed to sit entirely on top of it rather than
reinventing detection:

- The vanilla login packet (`class_118`/`LoginHelloPacket`) carries two
  fields StationAPI repurposes without changing the packet's wire
  shape: the client sets a bit-flag in the otherwise-client-unused
  `worldSeed` field of its outgoing hello
  (`worldSeed |= VanillaChecker.MASK`, `MASK` derived from
  `sipHash24("stationapi")`); the server, in its own response, appends
  `";stationapi;modid=version:..."` to the `username` field of the
  packet it sends back (a field the vanilla client already tolerates,
  now read with a raised string-length ceiling —
  `LoginHelloPacketMixin` changes a hard-coded `16` to
  `Short.MAX_VALUE` via `@ModifyConstant`, both sides). Neither change
  adds a field, changes a type, or changes packet framing — a
  StationAPI-modded client's outgoing hello is byte-for-byte the same
  shape a vanilla client would send (same fields, just a different
  value in one already-existing, otherwise-ignored `long`), so a real
  vanilla server has no reason to reject or misparse it.
- `ClientVanillaChecker`/`ServerVanillaChecker` set
  `((ModdedPacketHandler) networkHandler).isModded()` (mixed onto
  `NetworkHandler` by `NetworkHandlerMixin`) only when the peer's
  namespace string was actually found — a real vanilla peer never
  triggers this, and **no StationAPI mod, Harvester included, sends any
  extra packet before this check passes**: `ClientVanillaChecker` only
  sends its own `MessagePacket` mod-list reply after confirming the
  server's response contained `"stationapi"` in the split username.
- Harvester's own support handshake (below) piggybacks on this same
  `isModded()` signal rather than adding a second detection layer: the
  server only attempts to announce Harvester support to a player whose
  network handler is already confirmed `isModded()`; the client only
  arms its `SUPPORT_UNKNOWN` timeout — never sends anything — under the
  same condition.
- **Confirmed by this session's Spike C** (see "Spike results" below):
  a genuine, unmodified Mojang `minecraft-server.jar` accepted a Direct
  Connect from the Harvester+StationAPI client, the client's discovery
  timeout ran and expired exactly as designed, and nothing in the
  vanilla server's log showed an unrecognized-packet error. This was
  previously grounded only in source reading; it is now also a live
  runtime result, not just an inference.

### Networking API (`PROVEN_WITHIN_SCOPE` for the classes/methods; usage is new)

- `net.modificationstation.stationapi.api.network.packet.PacketHelper` —
  the sided send API. `send(Packet)`: client → server if genuinely
  multiplayer, else **handled in-process on the calling thread** if
  singleplayer (no network round-trip at all); server → handled
  in-process. `sendTo(PlayerEntity, Packet)`: server → that player's
  client; a no-op on the client side. Neither method exists as a
  broadcast-to-all; `sendToAllTracking`/`dispatchLocallyAndToAllTracking`
  exist for entity-tracking-radius broadcasts, not needed here.
- `net.modificationstation.stationapi.api.network.packet.MessagePacket` —
  a single pre-registered packet (`ManagedPacket.PACKET_ID = 254`,
  registered once by StationAPI itself) that carries an arbitrary
  `Identifier` plus optional primitive arrays/strings, routed by
  identifier to a listener in
  `net.modificationstation.stationapi.api.registry.MessageListenerRegistry`
  (`BiConsumer<PlayerEntity, MessagePacket>`, keyed by the same
  `Identifier`). This is the intended mechanism for exactly Harvester's
  use case — the class's own Javadoc names hold-to-activate-style
  keybind state as the motivating example — and means **Harvester never
  registers its own raw packet ID** (sidestepping the legacy 0–255
  byte-ID space and any collision risk entirely). Registering a listener
  is a local, in-process map insertion; it causes no network traffic by
  itself.
- Thread: confirmed by disassembling `net.minecraft.network.Connection`
  — it owns separate `reader`/`writer` `Thread` fields plus a
  `readQueue`, but packet application happens in `Connection.tick()`,
  called once per game tick from the main client/server loop, which
  drains `readQueue` and invokes each packet's handling synchronously
  on the calling (main) thread. **No explicit "schedule to main thread"
  utility is needed** — unlike modern Fabric networking, a
  `MessagePacket` listener registered here already executes on the main
  client thread (client-received) or main server thread
  (server-received), safe for direct world/player access.
- Packet identifiers: `Identifier`-based (e.g. `harvester:active`,
  `harvester:support`), not the raw byte-ID registry
  (`PacketTypeRegistry`/`PacketHelper.register`) — that path exists for
  higher-throughput dedicated packet types and is unnecessary here.
- Size/format limits relevant here: none meaningfully — a `boolean[]`
  of length 1 is a handful of bytes; `MessagePacket`'s own string
  fields cap at 32767 characters, far beyond anything this design sends.
- An unrecognized packet ID arriving at a genuine vanilla server is
  expected (era-consistent, not yet independently re-verified this
  session) to be a fatal protocol error causing disconnect — this is
  exactly why nothing may ever be sent before `isModded()` plus
  Harvester's own support confirmation both pass.

### Server-side break hook: two candidates compared, one recommended

**Recommended: `ServerPlayerInteractionManager.tryBreakBlock(int, int, int)Z`.**
Disassembled this session (not previously bytecode-verified — ADR 0002
identified it from source reading only). Confirmed to be the single
convergence point for a completed, authoritative server break: it reads
`world.getBlockId`/`getBlockMeta` before removal, calls
`this.finishMining(x,y,z)` (world-mutation only — see below), then
`this.player.getHand()` for `ItemStack.postMine(...)` (durability;
clears the hand if the stack empties, i.e. the tool broke), then, only
if both the removal and `player.canHarvest(Block.BLOCKS[blockId])`
succeeded, calls `Block.afterBreak(world, player, x, y, z, meta)`
(drops) and sends a corrective `BlockUpdateS2CPacket` to the breaking
player's own handler. `this.player` (a bound field, not a parameter) and
`this.world` are both directly available inside the method — no
separate lookup needed. `onBlockBreakingAction` (the instant-break path)
already calls this same method internally, confirming it is the
convergence point regardless of which upstream trigger (instant break
vs. completed progressive dig) is used.

**Rejected: `finishMining(int, int, int)Z`.** Disassembled: it only
reads the block, calls `ServerWorld.setBlock(x,y,z,0)`, and — if that
succeeded — `Block.onMetadataChange(...)`. No player, tool,
harvestability, or drop logic at all; it is a private "remove this
block from the world" primitive `tryBreakBlock` itself calls. Hooking
it directly would require re-deriving the player/tool/drop context
`tryBreakBlock` already assembles, and would re-enter on every one of
Harvester's own internal chain breaks exactly like the client's
`finishMining`-adjacent concern already solved by the existing
reentrancy guard — strictly worse than hooking the higher method once.

No existing StationAPI Mixin targets `ServerPlayerInteractionManager`
(confirmed — unlike the client-side `SingleplayerInteractionManager`,
which StationAPI already Mixins for `BlockState` capture); a Harvester
server Mixin here has no prior injector to coordinate with, but also no
existing `BlockState`-capture helper to reuse — it must call
`world.getBlockState(x,y,z)` itself, exactly as the client-side Mixin
already does for `World`.

### Client support state machine (design)

```
DISCONNECTED
  -> (join a server confirmed isModded()) -> SUPPORT_UNKNOWN
  -> (join a server not confirmed isModded(), or singleplayer) -> stays DISCONNECTED-equivalent; feature never arms

SUPPORT_UNKNOWN
  -> (receive harvester:support announcement, multiplayerAllowed=true) -> SUPPORT_AVAILABLE_DISABLED
  -> (receive harvester:support announcement, multiplayerAllowed=false) -> SUPPORT_UNAVAILABLE
  -> (discovery timeout elapses, no announcement received) -> SUPPORT_UNAVAILABLE
  -> (disconnect) -> DISCONNECTED

SUPPORT_UNAVAILABLE
  -> (disconnect) -> DISCONNECTED
  (terminal for the connection otherwise — a server that never announces stays unavailable for that session)

SUPPORT_AVAILABLE_DISABLED
  -> (player opts in for this server, first-time warning accepted) -> SUPPORT_AVAILABLE_ENABLED
  -> (disconnect) -> DISCONNECTED

SUPPORT_AVAILABLE_ENABLED
  -> (player opts out) -> SUPPORT_AVAILABLE_DISABLED
  -> (disconnect) -> DISCONNECTED
```

- Discovery timeout: a short fixed window (e.g. a few seconds, exact
  value an implementation detail) after `SUPPORT_UNKNOWN` is entered,
  driven by `GameTickEvent.End` counting — not a blocking wait. A server
  that never announces support (vanilla, or modded-without-Harvester)
  is indistinguishable from a Harvester server that is merely slow, so
  the timeout exists to bound how long the key/UI stays in a
  "checking" state, not to declare anything false with certainty before
  it must.
- On disconnect (`MultiplayerLogoutEvent`, client) or any server-change:
  full reset to `DISCONNECTED` immediately — no state carries across
  connections; the *opt-in preference* (below) is the only thing that
  persists, and it is per-server, not per-connection-state.
- Per-server opt-in storage: a local client-side map keyed by a stable
  server identity, persisted across sessions (a simple properties/JSON
  file under the client's own mod-config directory, analogous to
  `harvester.properties`, distinct from it). Server identity for a
  direct-connect/multiplayer-list entry is the configured address
  string (host:port) exactly as stored in the vanilla server list — not
  a resolved IP, so a hostname change is treated as a new server and a
  dynamic-IP server is treated as stable as long as the player keeps
  using the same saved address/hostname. LAN games have no stable
  identity worth persisting (see below) and are never opt-in-eligible
  across sessions — the state machine still runs per-connection, it
  simply never reaches `SUPPORT_AVAILABLE_ENABLED` from a saved
  preference for LAN.
- Key behavior per state: `DISCONNECTED`/singleplayer — existing
  behavior, completely unchanged (activation key drives
  `HarvesterClientActivationState` directly, as already implemented).
  `SUPPORT_UNKNOWN`/`SUPPORT_UNAVAILABLE` — holding the key does
  nothing and sends nothing (matches "no packet before confirmed
  support" and "no packet while support is absent"). `SUPPORT_AVAILABLE_DISABLED`
  — holding the key surfaces the mandatory warning (already drafted in
  ADR 0002) at most once per connection, never chains. `SUPPORT_AVAILABLE_ENABLED`
  — holding/releasing the key sends the `active` transition packet,
  exactly as `HarvesterClientActivationState` already tracks locally for
  singleplayer, now additionally mirrored to the server.
- Messaging: at most one informational line per state entry per
  connection (e.g. once on reaching `SUPPORT_UNAVAILABLE`, once on
  reaching `SUPPORT_AVAILABLE_DISABLED` the first time the key is
  pressed) — never per tick, never per key repeat.

### Protocol v1 (design)

Both directions use `MessagePacket` with a Harvester-namespaced
`Identifier`, registered via `MessageListenerRegistry`.

**`harvester:support`** (server → client, sent once per player after
login, only to a player whose handler is already `isModded()`):
- `booleans[0]` = `multiplayerAllowed` (the server's own
  `allowHarvester` setting, already `DECIDED` default `false` in ADR
  0002).
- `ints[0]` = protocol version (`1` for this design). A client that
  does not recognize the version treats the server as
  `SUPPORT_UNAVAILABLE` (fails closed, never assumes compatibility).
- No capability list, no chain limit, no neighborhood policy, no
  material list — the client never needs to know any of that; sending
  it would violate "the server recomputes everything itself" and
  invites exactly the kind of speculative-capabilities surface the ADR
  already warns against.

**`harvester:active`** (client → server, sent only after
`SUPPORT_AVAILABLE_ENABLED`, only on a genuine state transition):
- `booleans[0]` = the new `active` value.
- No protocol version field needed here — the client already knows the
  server's version from `harvester:support` and would not have reached
  `SUPPORT_AVAILABLE_ENABLED` on an incompatible one.
- Idempotent by construction: the server stores the last-known value
  per player and ignores a repeat of the already-stored value (ADR
  0002, already `DECIDED`) rather than erroring — a duplicate or
  out-of-order-but-identical packet is inert, not a violation.
- Rate limit: server-enforced, 4 transitions/second/player (already
  `DECIDED`), excess silently dropped and logged at `DEBUG` only, never
  a disconnect for mere repetition.
- The server never trusts anything from the client beyond this one
  boolean — no coordinates, tool, or material ever cross the wire in
  either direction; the server owns discovery, classification, limit,
  and execution entirely, reusing the already-implemented
  singleplayer core unchanged (see "Reuse" below).
- Pre-handshake packets: a `harvester:active` received before the
  server ever announced support to that player (or before the
  connection is confirmed `isModded()`) is rejected and logged, never
  processed — the server does not trust client-claimed readiness.

### Per-player server state (design)

**Identity decision (`PROVEN_WITHIN_SCOPE`, bytecode-verified this
session):** `UUID` is not used. Disassembling the pinned merged jar's
`net.minecraft.entity.Entity`, `net.minecraft.entity.player.PlayerEntity`,
and `net.minecraft.entity.player.ServerPlayerEntity` (via `javap` against
`minecraft-merged-...jar`) shows no `UUID`-typed field, no `UUID`-returning
method, and no field or method with `uuid` anywhere in its name on any of
the three classes — this Minecraft version identifies entities by a
mutable `int id` (`Entity.id`) and players additionally by `String name`
(confirmed used as the player's stable display/lookup key throughout
StationAPI's own source, e.g. `ServerVanillaChecker`'s
`player.name`/`serverPlayer.name` logging). `UUID`-per-entity is a much
later Minecraft addition; this is a genuine absence, not an unproven
convenience assumption — "não usar UUID sem comprovação" is satisfied by
the negative: there is nothing to use.

The chosen transient key is the **`ServerPlayerEntity` instance itself**
(reference identity), not a wrapper ID:

- `net.modificationstation.stationapi.api.server.event.network.PlayerLoginEvent`
  (StationAPI, confirmed by reading the Loom-remapped
  `station-lifecycle-events-v0` sources jar this session) already carries
  a typed `public final ServerPlayerEntity player` field — no lookup or
  cast needed to obtain the key at the moment a future per-player state
  map would need to create an entry.
- `ServerPlayerEntity`'s constructor (confirmed by bytecode:
  `ServerPlayerEntity(MinecraftServer, World, String, ServerPlayerInteractionManager)`)
  runs fresh on every login — a reconnecting player is a **new object**,
  never the same instance as a previous, already-disconnected session.
  This is what makes instance identity safe against leaking state between
  connections: even a same-named player reconnecting immediately gets a
  reference-distinct key, so a map keyed this way cannot accidentally
  resurrect a stale entry the way a name- or address-keyed map could.
- Cleanup hook: `ServerPlayerEntity.onDisconnect()` is a public,
  no-argument, bytecode-confirmed method with no existing StationAPI
  Mixin on it and no StationAPI server-side logout event (see "Lifecycle
  events" below) — the single narrow `@Inject(at = @At("HEAD"))` Mixin
  this tranche adds (`ServerPlayerEntityDisconnectMixin`, `server`-sided
  in `harvester.mixins.json`) is the future cleanup point. This tranche's
  Mixin only logs; no map exists yet to clear (see below).
- `ServerPlayNetworkHandler` (`ServerPlayerEntity.networkHandler`, a
  public field) was also considered and rejected as the *primary* key
  only because `PlayerLoginEvent` already hands back the player instance
  directly, one field access cheaper; the network handler remains the
  identity this tranche's `isModded()` check reads
  (`(ModdedPacketHandler) player.networkHandler`), so both objects are
  already in scope wherever the future state map would be touched either
  way.

This tranche adds **no actual map** — per the owner's instruction, any
future per-player registry stays empty and unabstracted until the
gameplay tranche that needs it. What exists now: the identity decision
above (documented and bytecode-justified) and the disconnect Mixin hook
it will attach to.

A future map, `Map<ServerPlayerEntity, HarvesterPlayerState>` (instance
identity, per the decision above — not `UUID`), owned by a server-side
singleton parallel to `HarvesterConfigState`:

- `HarvesterPlayerState { boolean active; long[] recentTransitionTicks (ring buffer of size 4, for the rate limit window); }`.
- Default on creation (first login): `active = false`.
- Cleared entirely (map entry removed) on player disconnect — the Mixin
  hook this tranche already adds (`ServerPlayerEntityDisconnectMixin` on
  `ServerPlayerEntity.onDisconnect()`; no `PlayerLogoutEvent` was found in
  `station-lifecycle-events-v0`; see "Lifecycle events" below) — and on
  server stop.
- Reset to `false` (not removed) on respawn and on dimension change,
  per ADR 0002's already-`DECIDED` policy — both likely need a Mixin
  hook too (see below).
- Concurrency: Beta 1.7.3's server tick loop and packet application are
  both confirmed single-threaded on the main server thread (via the
  `Connection.tick()` finding above) for anything reaching a
  `MessagePacket` listener, so the map itself does not need concurrent
  data structures for that access pattern — only genuine cross-thread
  access (if any future code reads it from off the main thread) would
  require synchronization, and none is planned.
- Stuck-`true` protection: since the server is the sole writer of
  `active` (never trusts the client's own belief about its state), the
  only way it could get stuck `true` is a missed disconnect-cleanup
  event — mitigated by clearing on every lifecycle event listed above,
  not by a client-driven "are you still there" heartbeat. No additional
  timeout is planned beyond the existing per-connection lifecycle
  events; if implementation reveals a gap (e.g. a lifecycle case with
  no available hook), a periodic `GameTickEvent`-driven sweep comparing
  against the connection's own liveness is the fallback, not a new
  client packet.

### Lifecycle events found vs. requiring a Mixin

Found as StationAPI `api` events (`station-lifecycle-events-v0`,
`station-networking-v0`):

| Event | Side | Carries |
| --- | --- | --- |
| `ServerLoginSuccessEvent` | client | `clientNetworkHandler`, `loginHelloPacket` — fires on successful login to a remote server |
| `MultiplayerLogoutEvent` | client | `disconnectPacket`, `dropped` — fires on leaving a multiplayer connection |
| `PlayerAttemptLoginEvent` | server | `serverLoginNetworkHandler`, `loginHelloPacket` — before a player is fully admitted |
| `PlayerLoginEvent` | server | `player` — a player has fully joined |
| `PlayerPacketHandlerSetEvent` | server | `player` |
| `GameTickEvent.End` | both | none — end of tick, either side |

Not found as an `api` event in the searched modules (would need a new
Mixin, each narrow and single-purpose per `docs/ENGINEERING_STANDARDS.md`):
player disconnect on the **server** side (only a client-side logout
event was found); player death; respawn; dimension change; integrated
server started/stopped as a distinct signal from `PlayerLoginEvent`
(singleplayer's internal server start is not itself an event — only the
player-join that follows it is, via the same `PlayerLoginEvent` used
for real multiplayer). Each of these, if needed, is a single
`@Inject` on the vanilla method already responsible for that
transition, mirroring the existing
`SingleplayerInteractionManagerObserverMixin` pattern — not a new
architectural surface.

### Reuse vs. new adapter

Reusable unchanged (all already `core`/pure or already `World`-generic):
`BlockDescriptor`, `HarvestGroup`, `HarvestGroupKind`,
`HarvestGroupResolver`, `BlockDescriptorView`, `BlockGroupView`
(including `byDescriptor`), `ConnectedBlockFinder`, `HarvestRequest`,
`HarvestPlan`, `NeighborhoodPolicy`/both implementations,
`StationBlockDescriptors` (takes a `BlockState`, not a `World` subtype),
`HarvesterConfig`'s `maxChain`/`neighborhood`/`diagnosticLogging`
semantics, and the `SingleplayerHarvestExecutionResult` stop-reason
vocabulary (the same names — `STOPPED_TOOL_CHANGED`,
`STOPPED_TOOL_UNSUITABLE`, `STOPPED_CANDIDATE_INVALID`, etc. — describe
identical situations server-side).

Needs a server-side adapter (new code, not yet implemented): a
`ServerHarvestDiscoveryAdapter`/`ServerHarvestExecutor` pair mirroring
the singleplayer ones structurally, but reading `ServerWorld`/
`ServerPlayerEntity`/`InteractionManager.breakBlock` on
`ServerPlayerInteractionManager` instead of `Minecraft`/
`ClientPlayerEntity`/`SingleplayerInteractionManager`, gated by the
per-player `active` state (this design's new state, not
`HarvesterClientActivationState`, which is client-only) and by
`allowHarvester`/`multiplayerAllowed`. Tool-harvestability reuses the
same `ItemStack.isSuitableFor(PlayerEntity, BlockView, BlockPos, BlockState)`
call verified for singleplayer — nothing about that API is client-only.

### Configuration (design)

Server keeps its own section in `config/harvester.properties`, not a
separate file: the same load-once-at-startup, defaults-on-missing-file,
isolated-per-property-fallback pattern already implemented in
`HarvesterConfigLoader` extends cleanly to server-only keys, and a
dedicated-server process only ever loads the `server`
(`DedicatedServerModInitializer`) entrypoint — it never sees or needs
the client-only keys, so there is no meaningful separation-of-concerns
cost to one file. A genuinely separate file would only pay off if
client and server config needed different reload semantics or
different physical distribution, neither of which applies here.

New server keys: `multiplayerAllowed` (default `false`, matching ADR
0002's `allowHarvester`), plus a `multiplayerRateLimitPerSecond`
(default `4`, matching ADR 0002's already-`DECIDED` limit, exposed as
configurable rather than hard-coded so an operator can tighten it
without a rebuild — never loosened by a client). `maxChain`,
`neighborhood`, `harvestLogs`, `harvestOres`, and `diagnosticLogging`
are reused as-is: in multiplayer they describe the server's own
authoritative gameplay behavior, exactly as they already describe the
singleplayer client's. The server never reads or trusts any
client-supplied value for any of these — there is no client-side
equivalent to reuse from in the first place, since the client's own
`harvester.properties` only ever described its own singleplayer
behavior.

Client's per-server opt-in (`multiplayerHarvesterEnabled` equivalent)
is a separate, new, client-only local store (see state machine above),
not a `harvester.properties` key — it is per-server data, not a single
scalar setting, and mixing it into the existing single-scalar file
would be a structural mismatch.

### Threat model highlights (server-side mitigation for each)

| Risk | Mitigation |
| --- | --- |
| Client spams `active` transitions | Server-enforced 4/sec rate limit, excess dropped silently (`DEBUG` log only) |
| Client sends `active` before any announcement/handshake | Rejected and logged; server never trusts unprompted state |
| Modified client sends malformed/out-of-range payload | `MessagePacket`'s own array-based decoding fails closed (missing/wrong-shaped array → no valid `booleans[0]`) on malformed input; treat as a rejected packet, never partially applied |
| State stuck `true` | Server is sole writer; cleared on every found/added lifecycle hook (login, disconnect, respawn, dimension change); see "stuck-true protection" above |
| Two players breaking the same vein simultaneously | Each player's chain uses the server's own authoritative world state at candidate-revalidation time (identical to the existing singleplayer per-candidate revalidation) — a candidate already claimed by another player's break is simply no longer a matching block when revalidated, stopping that chain at `STOPPED_CANDIDATE_INVALID`, never a race on a shared mutable plan |
| Chunk unloaded mid-chain | Revalidation (`world.getBlockId==0` check, already existing pattern) catches this the same way an already-removed candidate is caught singleplayer |
| Player far from the chain (teleported, disconnected mid-chain) | Server break pipeline (`tryBreakBlock`) is reused as-is, so vanilla's own range/permission checks apply per candidate exactly as they do for the origin; no separate range check needed |
| Tool changes or breaks mid-chain | Reuses the existing `HarvesterHeldItemSnapshot`-style identity comparison, server-side, per candidate — same stop semantics already proven singleplayer |
| Creative mode | Reuses whatever the normal `tryBreakBlock`/`canHarvest` pipeline already does for creative (not modified by this design) |
| Death/disconnect mid-chain | Server environment-validity check (mirroring `environmentValid` singleplayer) stops the chain; state cleared on the same lifecycle event |
| Server-side world protection (claims, etc.) | Reusing the normal `tryBreakBlock` pipeline means any protection mod already hooking that method or `canHarvest` applies to every candidate exactly as it would to a manual break — Harvester adds no bypass |
| Excessively large chain (DoS via BFS) | `maxChain` ceiling (already implemented, capped at 100) bounds candidate count; BFS itself is the already-tested `ConnectedBlockFinder`, unchanged |
| Coordinate spoofing | Structurally impossible — coordinates are never part of the client→server payload in this protocol (only a boolean), so there is nothing to spoof |
| Client fabricates a fake "server announced support" locally | Irrelevant — the state machine only matters for what the client *sends*; the server never trusts a client's belief about the server's own `multiplayerAllowed`/protocol state, it only trusts its own `active` flag store |

### Minimal experiments, in risk order

1. **A StationAPI-modded client (this exact toolchain) connects to a genuine unmodified vanilla Beta 1.7.3 server.** Success: connects, plays, no protocol error. Riskiest hypothesis (the vanilla-compatibility inference above) — first.
2. **The same JAR loads on a client instance and never instantiates `HarvesterServerEntrypoint`; loads on a dedicated server and never instantiates `HarvesterClientEntrypoint`.** Success: correct entrypoint set logged on each side, no missing-class error.
3. **A modded server (Harvester + StationAPI, no gameplay yet) announces support via one `harvester:support` `MessagePacket` sent from `PlayerLoginEvent`, without the client sending anything first.** Success: server-side log shows the packet sent; no client action preceded it.
4. **A client (Harvester installed) receives the announcement and transitions `SUPPORT_UNKNOWN` → `SUPPORT_AVAILABLE_DISABLED`/`SUPPORT_AVAILABLE_ENABLED` per the server's `multiplayerAllowed` value.** Success: state observed via log, no packet sent by the client yet.
5. **The client sends exactly one `harvester:active` packet on a key-hold transition, only after step 4, never before.** Success: server log shows exactly one receipt per transition, none before the announcement.
6. **Disconnect clears client state (`MultiplayerLogoutEvent` observed) and server per-player state (lifecycle hook observed).** Success: reconnecting starts fresh at `SUPPORT_UNKNOWN`/`active=false`, no stale carry-over.
7. **A server-side Mixin on `ServerPlayerInteractionManager.tryBreakBlock` observes one authoritative break (log only, no chain) — and, separately, the same JAR in singleplayer confirms this hook never fires for the player's own manual break.** Success: one server log line per real multiplayer break; zero server-side `[HARVEST-EXEC]`-equivalent lines during a singleplayer session that already produces client-side ones.
8. **The server-side pipeline executes exactly one additional controlled break through `tryBreakBlock` for a pre-set test candidate (mirroring the original singleplayer single-break slice), confirming drops/durability/permissions still resolve through the normal pipeline.** Success: matches the already-established singleplayer evidence bar (one extra break, vanilla pipeline, no custom drop/durability code).
9. **Two players, two independent chains on the same or adjacent veins, do not share or corrupt each other's per-player state.** Success: both chains complete independently; no cross-player state leakage observed in logs.
10. **Dedicated server outside Loom/dev environment still exhibits all of the above** — `COMPATIBILITY.md` already flags this as unqualified; multiplayer work should not assume the Loom dev server generalizes.

Ordered to fail the riskiest, least-reversible assumption (vanilla
compatibility) first, and the most foundational safety property (no
premature packet, no cross-player leakage) before any gameplay-shaped
spike.

### Spike results (this session)

This is a first tranche's worth of the "Minimal experiments" list above
(experiments 1–6, the handshake/side-safety/vanilla-compat spikes, not
the server break hook or gameplay chain — those remain future work, not
attempted this session). Mapped to the task's own Spike A–D naming:

**Spike A (client-only, no external Harvester server) — `PROVEN_WITHIN_SCOPE`
for classloading; timeout behavior inferred, not independently observed
in an isolated singleplayer world this session.** `runClient` reached a
stable, interactive state (the multiplayer connect screen was reachable
and used — see Spike C/D below), the activation key registered, config
loaded, no server-only Harvester class appeared anywhere in the client's
log. A dedicated, isolated singleplayer-world entry (as opposed to
reaching the multiplayer screen) was not separately performed this
session; the discovery-timeout code path was instead observed end-to-end
against a real server connection in Spike C below, which exercises the
identical `ServerLoginSuccessEvent` → arm timer → `GameTickEvent.End` →
`SUPPORT_UNAVAILABLE` path singleplayer would also use.

**Spike B (dedicated server, headless) — `PROVEN_WITHIN_SCOPE`.**
`runServer` loaded only `stationapi:event_bus_server`'s
`HarvesterServerSupportListener` for `harvester` (never a client-only
class), loaded `HarvesterServerConfigState` with
`multiplayerAllowed=false` logged, reached `Done (2098168400ns)! For
help, type "help" or "?"`, and stopped cleanly on a `stop` console
command (`Stopping the server..` → `Saving chunks` ×3, no error).

**Spike C (client connects to a real, unmodified vanilla Beta 1.7.3
server) — `PROVEN_WITHIN_SCOPE`.** The genuine, unmodified Mojang
`minecraft-server.jar` (`Main-Class: net.minecraft.server.MinecraftServer`,
`Created-By: 1.6.0` — already present locally from Fabric Loom's own
normal minecraft-dependency cache at
`_downloads/gradle-home/caches/fabric-loom/b1.7.3/minecraft-server.jar`;
nothing was downloaded this session) was run standalone
(`online-mode=false` for local testing, no other change) and reached
`Done`. The Harvester+StationAPI client connected to it via Direct
Connect: `Connecting to 127.0.0.1, 25566` → immediately
`[HARVEST-MP] Connection operational; support discovery armed (5000 ms
timeout).` → exactly 5 seconds later, `[HARVEST-MP] Support discovery
timed out; server treated as unavailable this session.` — no crash, no
disconnect caused by Harvester, no packet sent by the client (only log
lines from the client's own local state machine appear; nothing in the
vanilla server's log shows an unrecognized-packet error). This is the
riskiest hypothesis in the whole design and it held exactly as
predicted.

**Spike D (server-initiated `harvester:support` announcement, both sides
modded) — resolved in a follow-up session.** The crash first observed here:
```
java.lang.NullPointerException: Cannot invoke "String.equalsIgnoreCase(String)" because "<local4>" is null
	at knot//net.minecraft.client.network.ClientNetworkHandler.onHandshake(ClientNetworkHandler.java:440)
	at knot//net.minecraft.network.packet.handshake.HandshakePacket.apply(HandshakePacket.java:25)
	at knot//net.minecraft.network.Connection.wrapOperation$zkb000$station-networking-v0$stationapi_ifIdentifiable(Connection.java:542)
	...
```
was root-caused in a dedicated follow-up debugging session, not by adding
Harvester code but by disassembling the pinned, named-mapped
`minecraft-merged` jar. `ClientNetworkHandler.onHandshake` (confirmed
100% vanilla Beta 1.7.3 bytecode — not a StationAPI or Harvester Mixin)
branches on whether the server's `HandshakePacket.name` equals `"-"`:
- `"-"` (offline-mode server) → sends `LoginHelloPacket` immediately, no
  network call.
- anything else (online-mode server) → calls the legacy, pre-Yggdrasil
  `http://www.minecraft.net/game/joinserver.jsp?...` endpoint,
  `BufferedReader.readLine()`s the response, then does
  `<result>.equalsIgnoreCase("ok")` — the exact call site above.
  `readLine()` returns `null` at end-of-stream with no data, and that
  `null.equalsIgnoreCase(...)` is the NPE.

That endpoint is a decommissioned 2011-era Mojang auth service; as of
this repository's current date it resolves (Akamai) and accepts a TCP
connection but never sends an HTTP response. The original Spike D
server's `run/server.properties` had `online-mode=true` (confirmed by
inspection); the original Spike C vanilla-server comparison explicitly
used `online-mode=false`. That was the actual confound the original
session missed — not "vanilla vs. StationAPI-modded server" as first
concluded above, but purely `online-mode`.

**Live confirmation (isolated 2×2 matrix, this follow-up session):** an
isolated `BASE` checkout (`git archive` of commit `7be5980`, the commit
this multiplayer tranche started from) and the `CURRENT` working tree
(including this tranche's uncommitted changes) were each built and run
standalone, each with its own dedicated server (`online-mode=true`,
distinct ports) and its own client, with no changes to the real
repository working tree. All four pairings — BASE client → BASE server,
CURRENT client → BASE server, BASE client → CURRENT server, CURRENT
client → CURRENT server — reproduced the **identical** NPE at the
identical `ClientNetworkHandler.java:440`, including when the BASE side
predates the multiplayer tranche entirely. This proves the crash is
independent of any Harvester or StationAPI code on either peer. A final
check restarted the CURRENT server with `online-mode=false`: the CURRENT
client connected cleanly, no NPE, and the `harvester:support` handshake
completed end-to-end (`[HARVEST-MP] Connection operational...` →
`[HARVEST-MP] Server support confirmed (multiplayerAllowed=false).` on
the client; `[HARVEST-MP] Sent harvester:support to Player
(multiplayerAllowed=false).` on the server) — `SUPPORT_AVAILABLE_DISABLED`
reached exactly as designed.

**Conclusion:** this is a pre-existing vanilla Beta 1.7.3 behavior
interacting with a defunct external Mojang endpoint, exposed purely by
`online-mode=true` in a local test server's `server.properties`. It is
not a Harvester regression and not a StationAPI defect — the offending
method has zero StationAPI or Harvester involvement, and would crash an
entirely vanilla, mod-free Beta 1.7.3 client/server pair identically. No
Harvester code was changed to "fix" this (per this repository's policy of
not masking pre-existing platform defects). **Operational guidance:**
local/dev Harvester servers must use `online-mode=false`; this is now a
documented precondition, not a code-level workaround. MultiMC/distribution
comparison (originally flagged as a next step) was not performed — the
offending code path is a generic `java.net` HTTP call to a hardcoded real
hostname with no Loom- or dev-runtime-specific dependency, so it is not
expected to behave differently outside Loom, and the live matrix already
isolates the cause without it.

**NPE classification summary (recorded separately per claim):**
- Hypothesis "the Harvester tranche caused the NPE": `REFUTED`. The 2×2
  BASE/CURRENT matrix reproduced the identical crash at the identical
  vanilla bytecode call site regardless of which side (or neither side)
  ran the Harvester multiplayer tranche, including the BASE checkout
  which predates this tranche entirely.
- Root cause of the NPE under `online-mode=true`: `PROVEN_WITHIN_SCOPE`.
  Bytecode disassembly of vanilla `ClientNetworkHandler.onHandshake`
  plus the live 2×2 matrix jointly demonstrate that an online-mode
  server drives the client through the legacy `joinserver.jsp`
  authentication call; `BufferedReader.readLine()` returns `null` (the
  stream ends with no HTTP response from the now-defunct endpoint), and
  the immediately following `null.equalsIgnoreCase("ok")` throws. This
  is evidence of what was observed (a null read followed by a call on
  that null), not a claim about *why* the endpoint never answers beyond
  what was directly observed (it accepts the TCP connection but returns
  no response before the stream ends).
- Harvester handshake success under `online-mode=false`: `PROVEN_WITHIN_SCOPE`.
  The follow-up session's final check restarted the CURRENT server with
  `online-mode=false` and observed the client connect without the NPE
  and the `harvester:support` handshake complete end-to-end to
  `SUPPORT_AVAILABLE_DISABLED` on both peers' logs, as quoted above.

**Scope of `online-mode=false`:** this is an operational requirement of
the local/dev Beta 1.7.3 servers used by this test suite — a
precondition for reaching a server at all with `online-mode=true`
present in this toolchain's environment — not a requirement of the
Harvester protocol itself. Harvester's handshake (`harvester:support`
announcement, discovery timeout, state machine) has no `online-mode`
dependency of its own; it only runs after the vanilla login sequence
already completed. Real public servers, which may have a working
Yggdrasil-era or otherwise-functioning authentication path under
`online-mode=true`, remain outside this test's scope — this session
did not test against one. No workaround for the NPE was added to
Harvester's code.

**Manual actions taken (follow-up session):** three rounds of Direct
Connect clicks (two connects each in two client windows to cover the
2×2 matrix, plus one final connect to confirm the `online-mode=false`
fix) — one more than the task's nominal two-action budget, spent on the
decisive confirmation step per explicit owner instruction not to accept
the root cause until the runtime matrix isolated it. Everything else
(both BASE/CURRENT checkouts, both servers' boot/stop/reconfigure, all
log analysis, bytecode disassembly) was performed without further manual
steps.

### Runtime regressions V1/V2 (closeout session, `online-mode=false`)

Final validation before this tranche's commit, against the CURRENT
working tree's dedicated server (`online-mode=false`, port 25565).

**V1 (vanilla client → CURRENT Harvester server) — negative result,
`REFUTED` as a Harvester defect, `PROVEN_WITHIN_SCOPE` root cause.** The
client used was the local MultiMC instance `b1.7.3 - Babric Clean`
(Babric + Fabric Loader 0.18.4, zero mods — confirmed empty `mods`
folder — no Harvester, no StationAPI). Direct Connect to the CURRENT
server produced an immediate client-side crash:
```
Internal exception: java.io.IOException: Bad packet id 254
```
No corresponding `logged in` (or any player-related) line ever appeared
in the server log — the connection died during login, before the player
object completed setup. Root-caused by reading the relevant StationAPI
submodule sources (Loom-remapped, not disassembled bytecode, since these
are already human-named sources jars): StationAPI's own
`ManagedPacket.PACKET_ID = 254` is the wire ID for its internal
message-wrapper packet, and
`net.modificationstation.stationapi.impl.server.registry.ServerRegistrySynchronizer#sendWorldRegistry`
(module `station-registry-sync-v0`) calls
`RegistrySyncManager.configureClient(event.player)` **unconditionally**
on every `PlayerPacketHandlerSetEvent` — the `isModded()` guard is
present in the source but commented out, with the comment "only StAPI
clients can join StAPI servers anyway, at least at the moment". A
non-StationAPI client has no packet class registered for ID 254 and
rejects it as an unrecognized packet, per vanilla Beta 1.7.3's own
packet-decoding behavior. This is StationAPI dependency code, not
Harvester code, unmodified by this or any prior Harvester tranche — it
would reproduce identically on the BASE commit's dedicated server, since
`station-registry-sync-v0` has been part of the StationAPI dependency
set since before this multiplayer tranche existed.

**Confirms:** the server never sent `harvester:support` to this peer —
Harvester's own `HarvesterServerSupportListener` hooks the *later*
`PlayerLoginEvent`, which this connection never reached (no
`[HARVEST-MP]` log line of any kind appears for this connection,
including the "not modded; support not announced" debug line the
listener would log if it had run); there was no disconnect caused by
Harvester, and no classloading/Mixin error. **Does not confirm:** basic
gameplay working end-to-end for a non-StationAPI peer — that part of
V1's checklist failed, for a reason unrelated to Harvester or this
tranche. The dedicated server itself stayed up with no server-side
exception; only the client's own connection was rejected. No fix was
attempted — `station-registry-sync-v0` is an upstream StationAPI
submodule dependency, and any change to it is out of this session's and
this tranche's scope (no gameplay/hook work beyond validation was
authorized). This is a pre-existing StationAPI/vanilla-client
compatibility gap that predates and is independent of Harvester's
optional-multiplayer design; it does not block this tranche's own
scope (a StationAPI-modded peer discovering Harvester support), only
the separate, broader claim that this server is safe for a genuinely
vanilla player to join.

**V2 (Harvester client, disconnect/reconnect cycle) — `PROVEN_WITHIN_SCOPE`.**
Client used: `./gradlew.bat runClient` against the CURRENT working tree
(exact tranche code, not a possibly-stale packaged jar). Full observed
sequence, server and client logs cross-checked:
- Connect 1 (21:06:36): client `[HARVEST-MP] Connection operational;
  support discovery armed (5000 ms timeout).` → immediately `[HARVEST-MP]
  Server support confirmed (multiplayerAllowed=false).`; server
  `[HARVEST-MP] Sent harvester:support to Player (multiplayerAllowed=false).`
  — `SUPPORT_AVAILABLE_DISABLED` reached, announced exactly once.
- Disconnect (21:06:40): operator confirmed a clean return to the
  title/server-list screen (not a crash screen). The log shows a
  trailing `java.io.IOException: Bad packet id stationapi:items/interact`
  immediately followed by `java.net.SocketException: Socket closed` —
  a `stationapi:`-namespaced packet (not Harvester's), consistent with
  an in-flight read racing the socket's own teardown during a normal
  disconnect, not a live crash; the operator-confirmed clean return to
  menu corroborates this reading. No Harvester code is on this call
  path (confirmed: neither `src/main/java/.../client/multiplayer/` nor
  `src/main/java/.../multiplayer/` sends any client→server packet at
  all in this tranche).
- Reconnect (21:07:16), same server, same client process: fresh
  `[HARVEST-MP] Connection operational; support discovery armed (5000 ms
  timeout).` → `[HARVEST-MP] Server support confirmed
  (multiplayerAllowed=false).` on the client; a second, independent
  `[HARVEST-MP] Sent harvester:support to Player (multiplayerAllowed=false).`
  on the server — a fresh discovery/announcement cycle, not a stale or
  leaked state from connection 1.

**Confirms:** no state leaked across the connection lifecycle (the
second connection re-armed and re-confirmed exactly as the first did,
rather than skipping straight to a cached state); the server-initiated
announcement remained one-per-connection (not duplicated within a
connection, not skipped on the second); no client→server (`C2S`)
Harvester packet exists; `multiplayerAllowed=false` was preserved
identically across both connections.

**Manual actions taken (this session):** one connect, one disconnect,
one reconnect against the Harvester dev client (V2), plus one Direct
Connect against the vanilla-equivalent client (V1) — four total,
consistent with the task's stated intent to ask the operator only for
the indispensable connections. Server/client process boot, shutdown,
and all log analysis were automated.

### StationAPI-only client compatibility (`PROVEN_WITHIN_SCOPE` — bytecode-grounded AND runtime-confirmed this session)

The V1 spike above (`REFUTED`/`PROVEN_WITHIN_SCOPE` results) tested a
**fully vanilla** client (no Loader, no StationAPI at all) and found it
rejected at login by an unrelated, pre-existing StationAPI limitation
(`station-registry-sync-v0` unconditionally pushing packet ID 254 to
every connecting peer). That result says nothing about the actual
question this section resolves: does a client with **StationAPI but not
Harvester** work correctly against a Harvester server? This was untested
until this session and is the reason for the three-way client
terminology below, replacing any earlier ambiguous "client without
Harvester" phrasing.

**`isModded()` semantics, confirmed by disassembling the pinned
`station-vanilla-checker-v0` and `station-networking-v0` submodule jars
(`javap` on the Loom-remapped classes inside the published
`StationAPI-2.0.0-alpha.6.2.jar`'s `META-INF/jars/`, not decompiled/copied
source):**
- `NetworkHandlerMixin.isModded()` is exactly `this.mods != null` —
  a simple non-null check on a `Map<String, String>` field.
- That field is set by `ServerVanillaChecker.onPlayerLogin`
  (`PlayerAttemptLoginEvent`, i.e. *before* `PlayerLoginEvent`/before any
  player object exists), triggered purely by the masked bit in the
  client's `LoginHelloPacket.worldSeed` matching `VanillaChecker.MASK`
  (`sipHash24("stationapi")`) — a bit every StationAPI client sets
  unconditionally, regardless of which mods (if any) it carries beyond
  StationAPI itself. **`isModded()` becomes `true` before the server ever
  asks the client which mods it has** — it proves "this peer is running
  this exact StationAPI build," never "this peer has Harvester."
- The subsequent `stationapi:modlist` exchange (`ServerVanillaChecker`'s
  `registerMessages`) only ever inspects `VanillaChecker.CLIENT_REQUIRED_MODS`
  — mods whose own `fabric.mod.json` sets a truthy
  `stationapi:verify_client` custom value. Harvester's
  `fabric.mod.json` (confirmed by reading it this session) sets no
  `custom` block at all, so Harvester is never a member of
  `CLIENT_REQUIRED_MODS` and the disconnect-on-missing-mod branch inside
  that listener's lambda is structurally unreachable on account of
  Harvester — confirming the "never add `stationapi:verify_client`" rule
  already stated above is sufficient, not merely necessary, for this
  guarantee.

**Unknown-channel `MessagePacket` handling, confirmed by disassembling
`station-networking-v0`'s `MessagePacket`/`MessageListenerRegistry`
classes:**
- `MessagePacket`'s read method (`method_806`) decodes the identifier
  string and then each primitive array purely from a 9-bit presence mask
  read off the wire — it never inspects or depends on the identifier's
  value or on whether any listener is registered for it. Decoding a
  `harvester:support` payload requires nothing Harvester-specific to be
  present on the receiving peer.
- `MessagePacket.apply` (`method_808`) is exactly:
  `BiConsumer<?> listener = MessageListenerRegistry.INSTANCE.get(identifier); if (listener != null) listener.accept(player, this);`
  — a `null` registry lookup (the case when no mod registered that
  identifier, which is what happens when `HarvesterSupportClientListener`
  never loads because Harvester isn't installed) is a silent no-op: no
  exception, no log line, no disconnect. Registering a listener is
  needed only to *consume* a `MessagePacket`, never to *decode* one.

Together these two findings are the complete mechanism behind this
section's runtime result below: a StationAPI-only client is confirmed
`isModded()` (so the server sends `harvester:support`), decodes the
packet fine (decoding is identifier-agnostic), finds no listener
registered for `harvester:support` (Harvester's client listener class
never loaded), and drops it — structurally, not by any Harvester-side
defensive code.

**Spike (runtime, this session).** Client: the existing, already-clean
`b1.7.3 - StationAPI Baseline` MultiMC instance (component versions
confirmed via `mmc-pack.json` — Minecraft `b1.7.3`, Babric `b1.7.3`,
Fabric Loader `0.18.4`, matching this repo's pin exactly; `mods/` folder
contains only `StationAPI-2.0.0-alpha.6.2.jar`, confirmed by a recursive
search of the whole `.minecraft` tree for `*harvester*` returning zero
matches — no jar, class, config, or `.fabric` cache entry from Harvester
anywhere in this instance). Server: this repository's CURRENT working
tree (`port/stationapi-v2`, `HEAD` at the start of this session),
`./gradlew.bat runServer`, `run/server.properties` `online-mode=false`
(already present from a prior session), `run/config/harvester.properties`
carrying no `multiplayerAllowed` override so the server loaded its
`false` default (confirmed in the server's own boot log:
`HarvesterConfig{..., multiplayerAllowed=false}`).

Server boot showed only `stationapi:event_bus_server`'s
`HarvesterServerSupportListener` for `harvester` — no client-only
Harvester class — consistent with the already-`PROVEN_WITHIN_SCOPE`
same-JAR side-safety finding above. One manual Direct Connect
(`127.0.0.1:25565`) was performed by the repository owner: login
completed, the connection stayed open roughly 93 seconds (`09:39:57` to
`09:41:30`, exceeding the 60-second checklist minimum), the owner
confirmed movement, opening the inventory, and placing/breaking a
vanilla block all worked normally, and disconnect was a clean,
self-initiated `disconnect.endOfStream` — not a kick or crash. The
server's own log for the whole session contains exactly one
`[HARVEST-MP]` line for this connection,
`Sent harvester:support to SFAguiar (multiplayerAllowed=false).`, no
`Bad packet id` anywhere, no Mixin or classloading error, and the server
process remained live and accepting after the disconnect (confirmed
before it was deliberately stopped for this session's closeout). No
crash report appeared under the client instance's `.minecraft` (the
client's own `logs/latest.log`/`debug.log` stop mid-session — an
unflushed-buffer artifact of how this instance's process was closed, not
evidence of a fault; the repository owner separately confirmed nothing
looked wrong on screen at any point, and the absence of a crash report
plus the server-observed clean 93-second connection corroborate that).

**Conclusion:** the StationAPI-only client case works exactly as the
static analysis predicted. No Harvester-side code change was needed or
made.

### Final multiplayer compatibility matrix

Using precise three-way client terminology (a client either has no
StationAPI, has StationAPI without Harvester, or has both StationAPI and
Harvester) rather than the ambiguous "client without Harvester" used
earlier in this document's history:

| Client | Server | Result |
| --- | --- | --- |
| Harvester (+ StationAPI) | vanilla (no StationAPI) | Compatible — confirmed vanilla-server compatibility above (Spike C); Harvester's multiplayer handshake never arms (server never confirmed `isModded()`), singleplayer-only behavior unaffected. |
| No StationAPI (fully vanilla) | StationAPI + Harvester (CURRENT) | **Incompatible**, by a pre-existing StationAPI limitation (V1: `station-registry-sync-v0` unconditionally pushes packet ID 254 before any mod-list check runs) — `REFUTED` as a Harvester defect, out of this repository's scope to fix. |
| StationAPI, no Harvester | StationAPI + Harvester (CURRENT) | **Compatible, vanilla-equivalent behavior** — this session's spike, above. Login, movement, inventory, and vanilla block placement/breaking all work; the server announces `harvester:support` (because `isModded()` only requires StationAPI), the client silently drops it (no listener registered), and no C2S Harvester packet exists to send. |
| StationAPI + Harvester | StationAPI + Harvester (CURRENT) | Compatible; `harvester:support` discovery completes end-to-end to `SUPPORT_AVAILABLE_DISABLED` (V2 above). No gameplay (`harvester:active`) exists yet. |
| any | StationAPI + Harvester (CURRENT) | The server requires StationAPI on the client's side to admit it at all (a separate, pre-existing StationAPI constraint, not something Harvester adds or could relax) but never requires the client to have Harvester itself. |

### Decisions requiring the repository owner

All four questions this section originally raised were resolved by the
repository owner for this tranche — see "Owner decisions locking this
tranche's scope" near the top of this section for the resolutions
(timeout = 5s exact; opt-in stays config-file-only with no GUI, and no
persistence at all yet; rate limit fixed at 4/s, no config surface;
ADR 0002 stays as-is, a new ADR follows only after the server break-hook
runtime spike). Nothing remains open from this original list.

## Optional multiplayer, tranche 2 (implemented — opt-in, `harvester:active`, rate limit, observer hook)

Builds directly on tranche 1's handshake (`harvester:support`,
`SUPPORT_UNKNOWN`/`SUPPORT_UNAVAILABLE`/`SUPPORT_AVAILABLE_DISABLED`)
without modifying its wire format. Still no BFS, classification,
additional-block chain, drops, or durability for multiplayer — this
tranche is the activation signal and an observation-only server hook,
nothing gameplay-shaped.

### `harvester:active` (client → server)

`MessagePacket` on `Identifier` `harvester:active`, `booleans[0]` = the
new `active` value, no other field. `HarvesterActivePayload.parse`
rejects anything else outright (missing/extra booleans, or any other
array/string field present) — modeled after `harvester:support`'s
payload but stricter, since this is the one channel a modified client
could try to abuse. The server (`HarvesterServerActiveListener`, under
`stationapi:event_bus_server`) accepts a message only when: the
connection is already confirmed `isModded()`; the payload parses
strictly; `harvester:support` was already sent to this player
(`HarvesterMultiplayerPlayerState#supportAnnounced`); and the server's
*current* `multiplayerAllowed` is `true` (re-checked live, never assumed
from the earlier announcement). Any rejection is silent — logged at
`DEBUG`/rate-limited `WARN`, never a disconnect — and never sets `active`
to anything but its current value or `false`.

### Client per-server opt-in

A new, client-only, per-server file:
`config/harvester/servers/<readableHost>-<shortHash>.properties`, e.g.
`127.0.0.1-723ec373.properties`, containing `address=host:port` and
`multiplayerOptIn=false|true`. Identity (`HarvesterServerIdentity`) is
the lowercased host plus the *explicit* port the player connected with —
never a resolved IP, MOTD, or other server-supplied value — captured at
the one point both are available before DNS resolution:
`ClientNetworkHandlerConnectMixin` on `ClientNetworkHandler`'s own
constructor (confirmed by disassembly that the constructor passes its
`String host` parameter straight into `InetAddress.getByName` without
ever storing it as a field — no public API/event exposes this value
otherwise). Missing file → created with `false`; invalid value → `false`
plus a warning; no GUI, no hot reload — loaded once, fresh from disk,
only when a `harvester:support` announcement actually arrives
(`HarvesterServerOptInState.resolveOptIn`). Different ports on the same
host, or different hosts, get isolated files (hash covers the full
`host:port` string). Never written to under any circumstance during
singleplayer — see "Singleplayer non-interference" below.

### `SUPPORT_AVAILABLE_ENABLED` / `SUPPORT_AVAILABLE_DISABLED`

`HarvesterSupportStateMachine.onAnnouncementReceived(payload, localOptIn)`
now takes the resolved opt-in preference alongside the payload and picks
between the two states: `ENABLED` only when `multiplayerAllowed &&
localOptIn`; `DISABLED` otherwise (server disallows, opt-in is `false`,
or both). The activation key's held/released state
(`HarvesterActiveTransitionTracker`, wrapped by
`HarvesterMultiplayerActivationState`) is tracked independently of this
state — holding the key while support is unavailable, then support later
becoming available, does not itself send anything; only a genuine
press/release edge does, and only while currently `ENABLED`. Press sends
exactly one `true`; holding never repeats; release sends exactly one
`false`. Disconnect (and a fresh `onConnectionOperational`, covering
reconnect-without-an-intervening-disconnect) clears both the support
state and the key-tracker — a new connection never inherits either.

### Server-side per-player state and rate limit

`HarvesterMultiplayerServerRegistry` (`server.multiplayer`, keyed by
`ServerPlayerEntity` instance identity, never a name or UUID — this
Minecraft version has neither) holds one pure
`HarvesterMultiplayerPlayerState` per player: `supportAnnounced`,
`active`, and a `HarvesterRateLimiter` (sliding window, a 4-slot ring
buffer of acceptance timestamps — matches the size this document already
specified in tranche 1's design). `applyTransition(newValue, nowMillis)`:
a repeat of the already-stored value is idempotent and never consumes a
slot; a genuine change either applies (within 4 per rolling second) or is
rejected and **forces `active=false`** (never leaves the previous value
in place) — at most one rate-limit warning per window
(`shouldWarnRateLimit`). All of this is pure and unit-tested with a fake
clock.

The registry itself is a thin, `ServerPlayerEntity`-typed specialization
of `HarvesterIdentityRegistry<K, V>` — a pure, generic identity-keyed
registry (`IdentityHashMap`, only explicit `remove`/`clearAll`, no GC
dependence anywhere) unit-tested directly with plain `Object` keys
standing in for player instances, since a real `ServerPlayerEntity`
cannot be constructed outside a running server. Every lifecycle event
that must clear or reset state has its own deterministic Mixin hook — see
"Deterministic lifecycle" below; a first version of this registry used a
`WeakHashMap` and left dimension change unhandled, letting GC-eventual
cleanup stand in for correctness. That was rejected on review as
insufficient (garbage collection is not a functional or security
guarantee) and replaced with the explicit-hook design described here.

### Deterministic lifecycle (four hooks, all explicit, no GC dependence)

Every event that must clear or reset per-player state has its own narrow,
disassembly-confirmed Mixin hook — none alter vanilla control flow, each
only calls into `server.multiplayer` after (or, for respawn, using a
parameter already available at) the injection point:

- **Client-initiated disconnect** (`disconnect.endOfStream`,
  `disconnect.genericReason` — the common case): `ServerPlayNetworkHandlerDisconnectMixin`
  on `ServerPlayNetworkHandler.onDisconnected(String, Object[])`, calling
  `remove(player)`. Tranche 1 had instead added
  `ServerPlayerEntityDisconnectMixin` on `ServerPlayerEntity.onDisconnect()`,
  assumed to be the per-player logout hook — **this tranche's T6 runtime
  testing found that assumption wrong**: disassembly plus live
  dedicated-server logs show `onDisconnect()` is only ever called from
  `ServerPlayNetworkHandler.disconnect(String)`, the
  server-initiated-kick path; a normal client disconnect never reaches
  it (confirmed: zero `[HARVEST-MP] Player logout` lines appeared across
  six client-initiated disconnects in the original T6 session, on either
  the console or `logs/debug.log`). Both Mixins are kept — `remove` is
  idempotent, so covering the kick path in addition to the common path
  is harmless — but `ServerPlayNetworkHandlerDisconnectMixin` is the one
  that actually fires for a normal logout.
- **Server-initiated kick**: `ServerPlayerEntityDisconnectMixin` on
  `ServerPlayerEntity.onDisconnect()`, calling `remove(player)` (kept
  from tranche 1, now correctly scoped to this path only).
- **Respawn (death) — a genuinely new instance**: `PlayerManager
  .respawnPlayer(ServerPlayerEntity, int)` constructs a new
  `ServerPlayerEntity` (confirmed by disassembly: `new
  ServerPlayerEntity(...)` inside the method body, copying only `id`/
  `networkHandler` from the old instance; confirmed its sole caller is
  `ServerPlayNetworkHandler.onPlayerRespawn(PlayerRespawnPacket)`, the
  client's post-death respawn request). `PlayerManagerLifecycleMixin
  #harvester$onRespawn`, injected at `HEAD`, removes the *old* instance's
  entry immediately — it is still available as the method's own first
  parameter at that point, before the new instance is even constructed.
  The new instance is simply never a key in the map, so it starts with
  every field at its default (`supportAnnounced=false`, `active=false`,
  an empty rate limiter, no warning state) with no extra reset code
  needed.
- **Dimension/world change — the *same* instance**: `PlayerManager
  .changePlayerDimension(ServerPlayerEntity)` reuses the same instance
  (confirmed by disassembly: only `ServerPlayerEntity.setWorld` is
  called, no new instance constructed). Since there is no new map key to
  rely on, `PlayerManagerLifecycleMixin#harvester$onDimensionChange`
  explicitly resets the entry's **operational state** in place
  (`HarvesterMultiplayerPlayerState#resetOnWorldChange`) while
  preserving its **connection state** — a deliberate split, not a
  half-measure:
  - preserved: `supportAnnounced` — the StationAPI connection itself
    does not change on a dimension change, and the server already
    announced `harvester:support` to it exactly once; nothing re-sends
    that announcement on a world change, so clearing this here would
    permanently lock the player out of `harvester:active` for the rest
    of that connection the first time they cross a dimension boundary —
    a real regression, not a stricter guarantee, with no re-announce
    protocol to recover;
  - reset in full: `active` (must never survive a world boundary without
    the key being pressed again there), the rate limiter's entire window
    (`HarvesterRateLimiter#reset()` — a fresh run of up to 4 real
    transitions is accepted in the new world regardless of the old
    window's state), and the warning-suppression timestamp (a stale
    "just warned" state from the old world must not suppress a genuine
    first warning in the new one).

  This mirrors tranche 1's own original design record ("Reset to false
  (not removed) on respawn and on dimension change") for the
  dimension-change case specifically, refined during a correction round
  to cover the rate limiter and warning state explicitly rather than only
  `active` — an earlier version of this fix reset `active` alone,
  leaving the old world's rate-limit window and warning-suppression
  state to leak into the new one.
- **Server shutdown**: `MinecraftServerShutdownMixin` on the private
  `MinecraftServer.shutdown()` — confirmed by disassembly to be called
  from three separate points inside `run()` (normal loop exit plus at
  least one exceptional path), making it the single point reliably
  reached on every way the server loop can end, unlike the public
  `stop()` (which only flips a `running` flag the loop checks). Calls
  `clearAll()` at `HEAD`. **Runtime-confirmed this session**: a server
  was booted, sent a piped `stop` console command, and its log showed
  `[HARVEST-MP] Server shutting down; multiplayer registry cleared.`
  immediately — fully automated, no manual interaction needed.

None of these four hooks needed a "strong justification" to deviate from
`IdentityHashMap`; the map itself is exactly that structure, per the
requirement, wrapped only for testability (see above).

### Observer-only server hook

`ServerPlayerInteractionManagerObserverMixin` on
`ServerPlayerInteractionManager.tryBreakBlock(int,int,int)Z` (`@Shadow`
on its own `player`/`world` fields, no superclass reuse — unlike the
singleplayer Mixin, nothing useful is inherited here). Captures at
`HEAD`: pre-break block id/meta/`BlockState`, held item id. At `RETURN`:
the player's current `active` flag (read from the registry, defaulting
`false` if the player has no entry yet) and the break's success/post
block id. Never cancels, never mutates, never runs BFS, never breaks an
additional block. With `diagnosticLogging=false`, no log call and no
post-state read happen at all (only the cheap `HEAD`-time reads, matching
the existing client-side observer's own gating precedent); with `true`,
one `[HARVEST-MP-OBSERVE]` line per break. Confirmed inert for a
player's own singleplayer break (unchanged from tranche 1's finding:
singleplayer never calls `ServerPlayerInteractionManager` for the local
player).

### Singleplayer non-interference (`PROVEN_WITHIN_SCOPE`, source-grounded)

None of this tranche's new client-side machinery (connection-address
capture, opt-in file lookup/creation, `harvester:active` sending)
depends on any singleplayer-specific check — instead it is structurally
unreachable in singleplayer, confirmed by reading
`StationAPI#setupMods`: `stationapi:event_bus_server` entrypoints
(`HarvesterServerSupportListener`, `HarvesterServerActiveListener`) are
set up once per process, keyed by `FabricLoader.getEnvironmentType()` —
always `CLIENT` for a client process, even one that later starts an
embedded singleplayer server. `harvester:support` is therefore never
sent to a singleplayer client, the discovery timer always expires to
`SUPPORT_UNAVAILABLE`, and `HarvesterServerOptInState.resolveOptIn`
(the only code that touches the opt-in file) is only ever invoked from
inside the announcement handler — never reached, never a file created or
read for singleplayer. This mirrors and extends tranche 1's own
same-JAR/opposite-side finding rather than adding a new guard.

### T1–T6 runtime results (this session, `port/stationapi-v2`)

All six performed against a real dedicated server (`online-mode=false`)
and the Loom dev client, cross-checked between client and server logs
(`logs/debug.log`, which carries `DEBUG`-level lines the console/`INFO`
log does not). Manual actions: two guided play sessions in the original
session (connect/observe only, then press/hold/release/break/
disconnect-while-held/reconnect), plus one additional focused session in
a follow-up correction round (isolated T4 revalidation, after
reconciling that the original T4 result had been mis-scoped — see that
bullet below) and one graceful `stop` sent via piped stdin with no
manual interaction at all (server-shutdown hook verification).

- **T1 (client → genuine vanilla server) — PASS.** `Connection
  operational` → exactly 5000 ms later `Support discovery timed out;
  server treated as unavailable`; no `harvester:active` ever sent (key
  press while unavailable is a structural no-op); vanilla server logged
  a normal login/clean disconnect, no protocol error.
- **T2 (`multiplayerAllowed=false`, opt-in `true`) — PASS.** Client
  reached `multiplayerAllowed=false, localOptIn=true,
  state=SUPPORT_AVAILABLE_DISABLED`; no C2S sent; server `active` never
  set.
- **T3 (`multiplayerAllowed=true`, opt-in `false`) — PASS.** Client
  reached `multiplayerAllowed=true, localOptIn=false,
  state=SUPPORT_AVAILABLE_DISABLED`; blocks broken during this state
  observed server-side with `active=false`.
- **T4 (both `true`) — INITIALLY MIS-REPORTED, then corrected and
  re-passed.** The original session's report claimed both "T4 passed
  cleanly" and "the rate limiter fired live" without noticing both
  observations came from the *same* single connection: reconciling the
  exact logs afterward showed that one connection (login 13:51:15,
  disconnect 13:58:01) carried 10 `harvester:active` sends in two rapid
  bursts of 4-accepted-then-1-rejected each (8 applied, 2
  `RATE_LIMITED`, 0 duplicates — every send alternated value) — the
  product of the operator's confused early "pressed V multiple times at
  the main menu" attempt, not a controlled single press/hold/release.
  Evaluating T4's literal criteria ("exactly two transitions accepted,
  no rate-limit trigger") against that whole connection, it does **not**
  pass — a corrected, isolated revalidation was required. **Focused
  revalidation (new connection, this follow-up session):** login
  16:32:20; disconnect 16:34:30. The single-cycle script (press once,
  hold, release once, nothing more) was **not** followed literally — the
  operator performed **two independent press-hold(~2s)-release cycles**
  in the same connection. Each cycle, taken on its own, produced exactly:
  1 `true` accepted, 1 `false` accepted, 0 duplicates, 0 rejected, 0
  rate-limit warnings — the edge/hold semantics T4 requires (press sends
  once, holding never repeats, release sends once) are confirmed **per
  cycle**, twice over, with no ambiguity in either cycle. This is not
  reported as an aggregate "accepted true = 2, accepted false = 2" figure
  standing in for a single clean run — it is two separate, individually
  clean 1-true/1-false results. Server-side disconnect cleanup fired
  correctly on this connection too (`[HARVEST-MP] Player logout
  (network): Player`). The rate-limiter-under-load behavior from the
  original messy connection is retained as separate, valid evidence that
  the limiter itself works under real network timing (not a T4 result)
  — the limiter's primary evidence remains its synthetic-clock unit
  tests (`HarvesterRateLimiterTest`, `HarvesterMultiplayerPlayerStateTest`).
- **T5 (observer hook) — PASS.** A block broken with the key not held
  logged `active=false`; a block broken while held logged `active=true`
  (a `LogBlock` break sandwiched between its `true` and `false` sends);
  no BFS or additional break observed either time.
- **T6 (lifecycle) — PASS, after a fix.** Holding the key, setting
  `active=true`, then disconnecting without releasing, followed by a
  reconnect: the reconnect logged in with a *new* entity id each time
  (fresh `ServerPlayerEntity` instance, confirmed via the vanilla
  per-connection entity-id log) and re-ran the full discovery/announce
  cycle from `SUPPORT_UNKNOWN`, never skipping to a cached state — no
  stale `active=true` carried over. Getting here surfaced the
  `onDisconnect()` bug described above; after adding
  `ServerPlayNetworkHandlerDisconnectMixin`, a follow-up disconnect
  showed the expected `[HARVEST-MP] Player logout (network): Player`
  line.

### Not implemented (tranche 2 scope boundary — superseded by tranche 3 below)

At the end of tranche 2, BFS/discovery, log/ore classification, the
additional-block chain, drops, and durability were all still
singleplayer-only; `active` and the observer log were purely diagnostic
groundwork. Tranche 3 below closes that gap. `multiplayerRateLimitPerSecond`
remains hard-coded at 4, not operator-configurable, per the tranche-1
owner decision no later tranche has revisited. Dimension change *is*
handled deterministically (see "Deterministic lifecycle" above).

## Optional multiplayer, tranche 3 (implemented — server-authoritative chain execution)

When every precondition holds — StationAPI-modded connection,
`harvester:support` announced, `multiplayerAllowed=true`, the breaking
player's `active=true`, the origin's own manual break already succeeded,
and the origin resolves to a supported log/ore group with a suitable
tool — the server now runs the exact same deterministic chain singleplayer
already ran, entirely server-side. The client protocol is unchanged from
tranche 2: still one boolean (`harvester:active`), never coordinates,
blocks, tools, or plans.

### Shared implementation, not a divergent copy

A new side-agnostic package, `game`, holds every piece of classification/
discovery logic that was previously `client`-only but has no client-only
dependency: `StationBlockDescriptors`, `HarvestToolCompatibility` (now
parameterized on `PlayerEntity`/`World` instead of `Minecraft`),
`HarvesterHeldItemSnapshot`, `HarvestDiscoveryOutcome`, the shared
`HarvestChainOutcome` stop-reason enum, and — new — `HarvestDiscoveryAdapter`,
the single discovery implementation both
`client.SingleplayerHarvestDiscoveryAdapter` (now a thin wrapper) and the
server adapter call. `core`'s BFS (`ConnectedBlockFinder`) and
classification (`HarvestGroupResolver`/`HarvestGroup`) were already shared
and are unchanged. Only the break-invocation control loop itself is
mirrored rather than shared — `server.multiplayer.ServerHarvestExecutor`
structurally parallels `client.SingleplayerHarvestExecutor`, since the two
sides differ in break-method signature (`tryBreakBlock(x,y,z)` vs.
`breakBlock(x,y,z,direction)`), active-state source (per-player registry
vs. a client-local flag), and environment model — genuinely
platform-specific glue, not classification or BFS. `ServerHarvestExecutor`
splits into a thin public wrapper (real `ServerPlayerEntity`/`ServerWorld`/
`ServerBreakInvoker` types, called by the Mixin) over a package-private
pure `runChain` loop (small functional seams, no Minecraft/StationAPI
import) so the loop itself is unit-testable with fakes.

### Vanilla pipeline integration

Every additional break goes through
`ServerPlayerInteractionManagerObserverMixin`'s own `@Invoker` onto
`ServerPlayerInteractionManager.tryBreakBlock(int,int,int)Z` — the exact
authoritative method the origin break itself went through. No manual drop,
no manual durability change, no direct world mutation: drops, durability,
range/permission checks, and any third-party protection Mixin on the same
method apply to every chain candidate exactly as they would to a manual
break.

### Reentrancy

The chain's own internal breaks re-enter this same Mixin on this same
`ServerPlayerInteractionManager` instance (one instance per player, so the
guard is naturally player-isolated with no thread-local and no
cross-player interference). A new pure primitive,
`game.ChainReentrancyGuard` (`tryEnter`/`exit`, unit-tested in isolation),
is held as a `@Unique` field for the whole chain call and released in
`finally`; while held, the Mixin's own `HEAD` capture is skipped, making
every internal `tryBreakBlock` re-entry's `RETURN` handler inert — no
re-discovery, no nested chain, regardless of exception.

### Known limitations

- `environmentValid` server-side is a coarse non-null check on the player/
  world references (mirroring the singleplayer executor's own null-only
  check) — it does not independently re-verify range or teleport distance;
  this relies entirely on `tryBreakBlock` reusing the vanilla pipeline's
  own range/permission checks per candidate, not a Harvester-added check.
- `multiplayerRateLimitPerSecond` is still hard-coded (see above).
- No GUI/hot-reload for any of `multiplayerAllowed`, `diagnosticLogging`,
  or the per-server opt-in file — all remain manual file edits, unchanged
  from tranches 1–2.

### Evidence

**Automated** (`./gradlew.bat clean test classes`, JDK 17): 221 tests, 0
failures, 0 skipped — the pre-existing 194 plus 27 new/relocated
(`game.ChainReentrancyGuardTest`, `server.multiplayer.ServerHarvestChainGateTest`,
`server.multiplayer.ServerHarvestExecutorTest` exercising `runChain`'s pure
loop — deterministic order, `maxChain`, candidate/tool revalidation,
break-rejected, tool-changed-mid-chain, deactivation/environment-invalid
mid-chain, independent invocations — and the relocated
`game.HarvestToolCompatibilityTest`). No singleplayer test changed
behavior; the pre-existing 194 remained green throughout.

**Runtime** (dedicated `runServer`, `online-mode=false`,
`multiplayerAllowed=true`, MultiMC `b1.7.3 - Harvester Dev Test` client,
one guided session): server log (`diagnosticLogging=true` for this session
only) shows, in order — a break with `active=false` (only the origin
broke); a break with `active=true` producing
`[HARVEST-MP-PLAN] ... group=LOGS ... totalIncluded=6 additionalCandidates=5`
→ `[HARVEST-MP-EXEC] Chain starting: ... totalPlanned=5` →
`[HARVEST-MP-EXEC] Chain finished: ... successes=5 stopReason=CHAIN_COMPLETED`
(exactly one plan/chain pair despite five internal `tryBreakBlock`
re-entries, confirming the reentrancy guard); then a further break with
`active=false` (single block again, confirming clean deactivation). Zero
exceptions/errors on either side; both sessions ended with a clean
`disconnect.quitting`/`Player logout (network)` pair. `multiplayerAllowed`
and `diagnosticLogging` were restored to `false` in the local dev `run/`
config after the session (not version-controlled).
