# Foundation qualification evidence — 2026-07-16

Status: **PROVEN for build, baseline MultiMC, and dedicated server; client pending**

## Qualified tuple

- Temurin JDK 17.0.19+10
- Gradle Wrapper 8.12.1
- Fabric Loom 1.10.5
- Babric Loom extension 1.10.5
- Biny mappings `b1.7.3+e1fe071` v2
- Fabric Loader 0.18.4
- StationAPI 2.0.0-alpha.6.2
- Sponge Mixin `0.17.0+mixin.0.8.7`
- MixinExtras 0.5.0

## Artifact

- File: `harvester-b1.7.3-2.0.0-alpha.1.jar`
- SHA-256: `3BEB4629B0EB44FA888CC572C1D96DCBC9A67719AEE40673CA05AB216C846F67`
- This is a development artifact, not a release.

## Observed

- `clean check build` completed successfully with Java 17.
- Metadata, Mixin configuration, tests, secret checks, and prohibited-file checks passed.
- The operator launched the official Babric Clean package in MultiMC with Java 17.
- The operator launched StationAPI 2.0.0-alpha.6.2 in MultiMC with Java 17 and reached the menu.
- The dedicated server loaded Loader 0.18.4, StationAPI, the Harvester common
  entrypoint, and the Harvester server entrypoint.
- The server reached `Done`, accepted `stop`, saved chunks, and exited with code 0.
- No Harvester gameplay mechanic is implemented or enabled.

## Known development-runtime warnings

- StationAPI emits legacy `Shift.BY` Mixin warnings.
- The Loom-generated server logging configuration reports a missing queue plugin and
  GUI console appender during startup.
- A transitive SLF4J 2 API reports that no SLF4J provider is present. Harvester and
  StationAPI logging through Log4j remain functional.

These warnings did not prevent initialization or clean shutdown. They are not evidence
of compatibility beyond this exact tuple and test surface.

## Not yet proven

- Harvester client entrypoint loading in MultiMC.
- World creation or loading with the Harvester artifact installed.
- Restart, installation removal, or broader mod compatibility.
- Hosted CI; the port branch has not been published.

## Required next test

Install the exact artifact above into a copied StationAPI Baseline instance named
`b1.7.3 - Harvester Dev Test`, launch with the qualified Java 17 executable, reach the
menu, create a disposable world, exit normally, and preserve a sanitized log showing
both Harvester client-side load messages without exceptions.
