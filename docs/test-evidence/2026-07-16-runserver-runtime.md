# Development dedicated-server runtime evidence — Gradle/Loom `runServer` — 2026-07-16

Status: **PROVEN for the development `runServer` task**; **INCONCLUSIVE** for a
clean dedicated-server installation outside the Loom development environment, and
for the exact HEAD/working-tree content of this specific execution.

## Origin of the raw logs

- `run/server.log`
- `run/logs/latest.log`
- `run/logs/debug.log`

These files are local development artifacts produced by the Gradle/Loom
`runServer` task and are excluded from version control by `.gitignore`
(`run/`, `run/logs/latest.log`, `run/logs/debug.log`, `run/server.log`). The raw
logs are **not** committed; only this sanitized report and the hashes below are.

SHA-256 of the raw log files as they exist locally at the time this report was
written:

- `run/server.log`: `61e9dd7c0e648d8b756250e315c116d4878791a4372016772514d4a88c5ceb8e`
- `run/logs/latest.log`: `0012c64d0a1b8a769c8a968d9b7ea7f0d44ed1ad4684ca895ac816494f8c8239`
- `run/logs/debug.log`: `81c477c36beb90e84a29008f9ebeccf172b31cb6f6c9aea2e0822f1ac8a155f7`

These hashes let a future check confirm whether the local `run/` directory still
matches what this report describes, without redistributing the logs themselves.

## Task executed

`./gradlew.bat runServer` (Fabric Loom development dedicated-server task), run
three times in sequence during the same working session. This report is based on
the **third and final run**, timestamped `19:15:37`–`19:16:41` in `run/server.log`.

## Environment

- Java: Temurin 17 (JVM 17, per the qualified build tuple)
- Fabric Loader: `0.18.4`
- StationAPI: `2.0.0-alpha.6.2`
- Minecraft: Beta 1.7.3 (`minecraft 1.0.0-beta.7.3` mod id)
- Sponge Mixin: `0.17.0+mixin.0.8.7`

## HEAD attribution — three distinct HEADs, not to be conflated

1. **HEAD exact to the `runServer` execution that produced these logs**
   (19:09–19:16 on 2026-07-16): **`INCONCLUSIVE`**. The logs themselves contain no
   commit hash. The repository's `git reflog` shows no ref movement (`checkout`,
   `commit`, `reset`) between `c32bfd3` (18:31:59 -0300) and `b4c9698`
   (19:51:14 -0300) — a window that contains the log timestamps — so `HEAD` was
   pointing at `c32bfd3` throughout. This bounds which **commit ref** was checked
   out, but does **not** prove the **working-tree content** matched that commit
   exactly: uncommitted changes later folded into `b4c9698`/`319c21c`/`465218d`
   (all committed at 19:51) could already have been present, unstaged, when
   `runServer` ran. No timestamp-only inference is used to close this gap, so the
   exact HEAD/content of this execution remains **`INCONCLUSIVE`**.
2. **HEAD that contains this report** — the commit created immediately after
   writing this file. Recorded in the commit message/history itself; not
   restated here to avoid the same self-reference problem this checkpoint is
   correcting elsewhere.
3. **HEAD used for the `clean check build` re-validation** — executed *after* the
   commit in (2), on the resulting HEAD. Recorded separately in the worklog and in
   `CURRENT_STATE.md` once available.

## Minimal decisive excerpts

Mod list (from `run/logs/latest.log`, start of the run):

```text
[19:15:35] [main/INFO] (FabricLoader) Loading 40 mods:
	- fabricloader 0.18.4
	- gambac 1.3.0
	- harvester 2.0.0-alpha.1
	- java 17
	- minecraft 1.0.0-beta.7.3
	- mixinextras 0.5.0
	...
	- station-api-base 2.0.0-alpha.6.2
	...
```

Harvester entrypoints loading (`run/logs/latest.log`):

```text
[19:15:37] [main/INFO] (harvester) Harvester StationAPI foundation loaded; gameplay features are not enabled.
[19:15:37] [main/INFO] (harvester) Harvester dedicated-server entrypoint loaded.
```

Server reaching ready state, processing `stop`, saving, and exiting
(`run/server.log`):

```text
2026-07-16 19:15:39 [INFO] Done (2108837300ns)! For help, type "help" or "?"
2026-07-16 19:16:41 [INFO] CONSOLE: Stopping the server..
2026-07-16 19:16:41 [INFO] Stopping server
2026-07-16 19:16:41 [INFO] Saving chunks
2026-07-16 19:16:41 [INFO] Saving chunks
2026-07-16 19:16:41 [INFO] Saving chunks
```

## Redactions

The raw `run/logs/latest.log` file contains at least one line with a local
filesystem path under the operator's personal user profile (a Mixin subsystem
banner referencing the local Gradle module cache location). That line is not
reproduced in this report. No other personal path, username, IP address, or
account data was included in the excerpts above.

## What this proves

- **`PROVEN`**: the Gradle/Loom `runServer` development dedicated server loaded
  Fabric Loader 0.18.4, StationAPI 2.0.0-alpha.6.2, and the Harvester common and
  server entrypoints; reached `Done`; processed a `stop` command; saved chunks;
  and exited without an unhandled exception, across three separate runs on
  2026-07-16.

## What this does not prove

- **`INCONCLUSIVE`**: the exact commit/working-tree content checked out during
  this specific run (see HEAD attribution above).
- **`INCONCLUSIVE`/pending**: behavior of a dedicated server built from a
  packaged release JAR, launched outside the Gradle/Loom development
  environment, on a clean installation. `runServer` uses the Loom development
  run configuration and classpath, which is not identical to a standalone
  server JAR launch.
- **Pending**: MultiMC client launch of the Harvester artifact itself (tracked
  separately as the `b1.7.3 - Harvester Dev Test` next action).
- **Pending**: world creation/persistence with the Harvester artifact installed,
  restart behavior, and broader mod compatibility.

## Required next test

Install the exact release artifact into a clean, non-Loom dedicated-server
environment (a plain JVM launch of the packaged server distribution, not
`./gradlew.bat runServer`), and repeat the same observations (entrypoint load,
`Done`, `stop`, save, clean exit) to close the "clean install outside Loom" gap
identified above.
