# Test plan

Automated foundation gate: Java 17; pinned Wrapper; `clean check build`; unit tests; metadata JSON parse; Mixin JSON parse and empty-foundation assertion; prohibited-file and secret scans; artifact SHA-256.

Manual gate: Babric clean launch, StationAPI baseline, Harvester client load, dedicated-server load, restart, missing/invalid future config, copied disposable world, clean artifact installation, sanitized logs without exceptions, and safe removal where applicable. Gameplay parity tests are deferred until the corresponding feature increments.

Never describe client/server/manual status as passed from compilation alone.

## Foundation evidence

- Automated build and metadata gates: passed on 2026-07-16.
- MultiMC Babric Clean: passed manually on Java 17.
- MultiMC StationAPI Baseline: passed manually on Java 17.
- Harvester dedicated server: passed on 2026-07-16, including clean shutdown.
- Harvester client in MultiMC: pending.

See `docs/test-evidence/2026-07-16-foundation.md` for the exact qualified tuple,
artifact checksum, observations, and remaining limitations.
