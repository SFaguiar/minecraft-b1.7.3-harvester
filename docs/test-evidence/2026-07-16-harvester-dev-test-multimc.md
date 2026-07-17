# Harvester Dev Test — MultiMC client runtime evidence — 2026-07-16

Status: **PROVEN for the Harvester client entrypoints, world lifecycle, and clean
shutdown, by direct operator observation**; **INCONCLUSIVE** for complete absence
of non-fatal warnings (several were observed and are listed below); **PENDING**
for a dedicated server in a clean install outside the Loom development
environment; gameplay mechanics **not started**.

## Basis for this report

This report is based on **direct operator observation** during a manual MultiMC
launch, not on a raw client log file captured and hashed by an agent. No
`latest.log`/`debug.log` from this MultiMC run was provided or preserved
alongside this report. The distinction matters: the entrypoint-loading and
world-lifecycle claims below rest on the operator's first-hand account of what
was seen on screen and in-game, not on an independently verifiable artifact.
Should a sanitized client log become available later, it should be added
alongside this report and cross-checked against it.

## Environment

- Launcher: MultiMC `0.7.0-4268`
- Java: Temurin `17.0.19`
- Fabric Loader: `0.18.4`
- StationAPI: `2.0.0-alpha.6.2`
- Harvester: `2.0.0-alpha.1`
- Instance: `b1.7.3 - Harvester Dev Test` (per
  `better-beta-program/docs/test-strategy/MULTIMC_MANUAL_SETUP.md`)

## Observations reported by the operator

- Client reached the main menu.
- Common entrypoint executed, logging:
  `Harvester StationAPI foundation loaded; gameplay features are not enabled.`
- Client entrypoint executed, logging: `Harvester client entrypoint loaded.`
- Dedicated-server entrypoint did **not** appear in the client log — expected,
  per the per-context entrypoint checklist in `MULTIMC_MANUAL_SETUP.md`.
- A disposable world was created.
- The world was saved and reopened within the same MultiMC session.
- The world was reopened again after a full close and restart of Minecraft.
- Final shutdown was clean: `Process exited with exit code 0 (0x0).`

## Non-blocking warnings observed

- `Shift.BY` warnings in StationAPI Mixin modules (same class of warning
  already documented for the `runServer` dedicated-server evidence).
- Non-SemVer metadata warning for `unsafeevents`.
- JInput reporting Windows 11 as an unrecognized/unknown OS version string.
- A legacy attempt to reach `http://s3.amazonaws.com/MinecraftResources/` —
  this is vanilla Beta-era resource-fetch behavior predating Mojang account
  migration, not something introduced by Harvester or StationAPI; it did not
  block startup.

None of the warnings above prevented reaching the menu, loading the Harvester
entrypoints, creating/saving/reopening the world, or exiting cleanly.

## Redactions

This report contains no token, username, account identifier, IP address, or
personal filesystem path. No raw log file is attached; only the operator's
reported observations and the log lines they explicitly quoted are reproduced
above.

## Classification

- **`PROVEN`**: Harvester client entrypoint loading in MultiMC.
- **`PROVEN`**: common entrypoint executed (`Harvester StationAPI foundation
  loaded; gameplay features are not enabled.`).
- **`PROVEN`**: client entrypoint executed (`Harvester client entrypoint
  loaded.`).
- **`PROVEN`**: disposable world creation, save, and reopening within the same
  session.
- **`PROVEN`**: world persistence after a full Minecraft restart.
- **`PROVEN`**: clean shutdown with exit code 0.
- **`INCONCLUSIVE`**: complete absence of warnings — several non-blocking
  warnings were in fact observed (listed above); this closes the ambiguity left
  open by the previous MultiMC reports (`Babric Clean`, `StationAPI Baseline`),
  which had no log evidence to check against.
- **`PENDING`**: dedicated server in a clean installation outside the Loom
  development environment (distinct from the already-`PROVEN` `runServer`
  development server, see
  `docs/test-evidence/2026-07-16-runserver-runtime.md`).
- **Not started**: Harvester gameplay mechanics (flood-fill, tree-felling,
  durability, tiers, or any block-break hook behavior).

## What this closes

This completes manual validation of all three MultiMC instances defined in
`MULTIMC_MANUAL_SETUP.md` for the current qualification pass (`Babric Clean`,
`StationAPI Baseline`, `Harvester Dev Test`), together with the already-proven
Gradle/Loom `runServer` development dedicated server. The remaining runtime gate
is a dedicated server in a clean install outside Loom; gameplay work has not
started.

## Required next test (runtime track, independent of gameplay work)

Install the packaged release artifact into a clean, non-Loom dedicated-server
environment (a plain JVM launch of the packaged server distribution) and
observe the same lifecycle (entrypoint load, `Done`, `stop`, save, clean exit)
to close the "clean install outside Loom" gap.
