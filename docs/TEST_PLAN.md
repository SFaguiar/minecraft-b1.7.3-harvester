# Test plan

Automated gate: Java 17; pinned Wrapper; `clean check build`; unit tests; metadata JSON parse; Mixin JSON parse; prohibited-file scan; artifact SHA-256. GitHub-native secret scanning and push protection run separately, at the platform level, not inside this script.

Manual gate: Babric clean launch, StationAPI baseline, Harvester client load, dedicated-server load, restart, and gameplay validation across all harvest categories (logs, ores, dirt, gravel, leaves, crops), tool gating, drop consolidation, stack splitting, foreign-drop isolation, and full server-authoritative multiplayer behavior.

Never describe client/server/manual status as passed from compilation alone.

## Foundation evidence

- Automated build and metadata gates: passed on 2026-07-16.
- MultiMC Babric Clean: passed manually on Java 17.
- MultiMC StationAPI Baseline: passed manually on Java 17.
- Harvester dedicated server (foundation entrypoints only): passed on 2026-07-16, including clean shutdown.

See `docs/test-evidence/2026-07-16-foundation.md` for the exact qualified tuple,
artifact checksum, observations, and remaining limitations from that foundation gate.

## Gameplay manual gate

A full 10-test manual gate covering every harvest category, tool gating, drop
consolidation, and server-authoritative multiplayer behavior has since passed
against the current feature set, on both singleplayer and a dedicated server.
Current status and evidence are tracked in governance, not in this repository:
`better-beta-program/docs/operations/CURRENT_STATE.md`.
