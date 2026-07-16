# Test plan

Automated foundation gate: Java 17; pinned Wrapper; `clean check build`; unit tests; metadata JSON parse; Mixin JSON parse and empty-foundation assertion; prohibited-file and secret scans; artifact SHA-256.

Manual gate: Babric clean launch, StationAPI baseline, Harvester client load, dedicated-server load, restart, missing/invalid future config, copied disposable world, clean artifact installation, sanitized logs without exceptions, and safe removal where applicable. Gameplay parity tests are deferred until the corresponding feature increments.

Never describe client/server/manual status as passed from compilation alone.
