# Harvester Dev Test — MultiMC client runtime evidence — 2026-07-16

Status: **PROVEN by raw client log** for mod versions, entrypoint execution, and
several of the reported warnings; **PROVEN by direct operator observation** for
visual facts not captured in the log text (menu, world lifecycle, exact exit
message); **INCONCLUSIVE** for two of the originally reported warnings, which
were not found in the captured client logs; **PENDING** for a dedicated server
in a clean install outside the Loom development environment; gameplay mechanics
**not started**.

## Basis for this report

A read-only search of the local MultiMC instance directory for
`b1.7.3 - Harvester Dev Test` found real client log files that had not been
previously located or hashed. This report has been complemented with excerpts
and hashes from those files. The raw logs themselves are **not** versioned —
they live under the MultiMC instance directory, outside any Git repository, and
are excluded from this report except as sanitized excerpts and SHA-256 hashes
below.

Two separate client launches were found to contain the Harvester mod (a third,
earlier log at `19:01` predates the Harvester JAR being added to the instance's
`mods/` folder — confirmed by the mod count in that log, 48 mods without
`harvester`, versus 49 mods with `harvester` in the two sessions below — and is
therefore not part of this evidence):

- **Session A** (rotated log, local filename pattern `2026-07-16-2.log.gz`):
  started `21:21:37`, first session with the Harvester JAR present.
- **Session B** (the instance's current `latest.log`): started `21:24:56`, a
  separate JVM launch (a new log file is created per launch), consistent with
  the operator's report of a full close-and-restart between sessions.

SHA-256 of the raw log files, computed locally (not versioned):

- Session A (`2026-07-16-2.log.gz`): `09d2f7830b9f68e86570aa87c46c6f27a454940422d0cc0a2dec54620d1a1bb0`
- Session A debug log (`debug-2.log.gz`): `626c0ba1354ec74748b487c0d120d67f520b87624aabcfaa3ad60a1e0f9add3c`
- Session B (`latest.log`): `563c1a14783dc9a444af0c60bf78420fe97c2d81e138fd098291d5dbc1269bf9`
- Session B debug log (`debug.log`): `3d79d8abe67bced31d9ce4bfc5046e57117fddf77e31209a0245719395c2e395`

The instance's `saves/Harvester Test - Disposable/` directory contains a
`level.dat`, a `level.dat_old` (evidence of at least one save-and-rewrite
cycle), a `session.lock`, and region files, with filesystem timestamps spanning
`21:23`–`21:26` — i.e. across both Session A and Session B — consistent with
the world being created in Session A and reopened in Session B.

## Environment (log-confirmed)

- Launcher: MultiMC `0.7.0-4268` (operator-reported; not independently
  re-verified against a MultiMC version string in these logs)
- Java: Temurin `17.0.19` (operator-reported)
- Fabric Loader: `0.18.4` — **PROVEN**, both sessions log
  `Loading Minecraft Beta 1.7.3 with Fabric Loader 0.18.4`.
- StationAPI: `2.0.0-alpha.6.2` — **PROVEN**, both sessions' mod list include
  `stationapi 2.0.0-alpha.6.2`.
- Harvester: `2.0.0-alpha.1` — **PROVEN**, both sessions' mod list include
  `harvester 2.0.0-alpha.1`.
- Instance: `b1.7.3 - Harvester Dev Test` (per
  `better-beta-program/docs/test-strategy/MULTIMC_MANUAL_SETUP.md`)

## Observations

- **`PROVEN`** (log line, both sessions): common entrypoint executed —
  `(harvester) Harvester StationAPI foundation loaded; gameplay features are
  not enabled.`
- **`PROVEN`** (log line, both sessions): client entrypoint executed —
  `(harvester) Harvester client entrypoint loaded.`
- **`PROVEN`** (absence confirmed by search of both session logs): no
  dedicated-server entrypoint line appears in either client log — expected,
  per the per-context entrypoint checklist in `MULTIMC_MANUAL_SETUP.md`.
- **`PROVEN — confirmação direta do operador`**: client reached the main menu
  (a GUI/visual fact; the text log does not narrate reaching a screen).
- **`PROVEN — confirmação direta do operador`**, corroborated by the world
  save directory's file timestamps spanning both sessions: a disposable world
  was created, saved, and reopened within the same session, and reopened again
  after a full close and restart of Minecraft. The log text itself does not
  narrate world creation/save/reopen as discrete events (no "Preparing spawn
  area" or equivalent lines were found in either session's client log — such
  messages, seen in the dedicated-server evidence, appear not to be printed by
  the integrated/client world-creation flow at INFO level), so this rests on
  the operator's account, strengthened by the save-directory timestamps.
- **`PROVEN — confirmação direta do operador`**: final shutdown was clean,
  reported as `Process exited with exit code 0 (0x0).` This exact string was
  not found in the locally available MultiMC console logs (it may be a
  UI-only notification not persisted to text); MultiMC's own console log did
  record the corresponding launch task completing without an error status,
  which is consistent with, but not identical proof of, the operator's report.

## Warnings — re-checked against the captured logs

- **`PROVEN`** (log line, both sessions): `Shift.BY` warnings in StationAPI
  Mixin modules — e.g. `@Inject(@At("INVOKE")) Shift.BY=2 on
  station-flattening-v0.mixins.json:StatsMixin ... exceeds the maximum allowed
  value: 0`.
- **`PROVEN`** (log line, both sessions): non-SemVer metadata warning for
  `unsafeevents` — `Mod com_github_mineldiver_unsafeevents uses the version
  e31096e which isn't compatible with Loader's extended semantic version
  format`.
- **`INCONCLUSIVE`**: JInput reporting Windows 11 as an unrecognized/unknown
  OS version string — searched for in both session logs (INFO and DEBUG level)
  and not found as a distinct warning line; only a plain classpath-loading
  debug entry for the JInput JAR was present. This may have appeared in a
  native/stderr stream not captured by the log4j-based client log, or in a
  MultiMC-internal log not searched here. Originally reported by the operator;
  not independently confirmed.
- **`INCONCLUSIVE`**: a legacy attempt to reach
  `http://s3.amazonaws.com/MinecraftResources/` — searched for in both session
  logs and not found. Originally reported by the operator; not independently
  confirmed in this pass. If this recurs, prefer capturing it directly from a
  fresh log rather than relying on operator recall.

None of the confirmed or reported warnings above prevented reaching the menu,
loading the Harvester entrypoints, creating/saving/reopening the world, or
exiting cleanly.

## Redactions

This report contains no token, username, account identifier, IP address, or
personal filesystem path. The raw logs contained at least one line with a
local filesystem path under the operator's personal user profile (a native
library classpath entry) and MultiMC's own console log separately contained an
offline-account profile identifier; neither is reproduced here. Only the
sanitized excerpts and hashes above are included.

## Classification

- **`PROVEN`**: Fabric Loader 0.18.4, StationAPI 2.0.0-alpha.6.2, and Harvester
  2.0.0-alpha.1 all present and loaded, in both captured client sessions.
- **`PROVEN`**: common and client entrypoints executed, in both captured
  sessions.
- **`PROVEN`**: no dedicated-server entrypoint in the client log (expected).
- **`PROVEN`**: `Shift.BY` Mixin warnings and non-SemVer `unsafeevents`
  metadata warning, in both captured sessions.
- **`PROVEN — confirmação direta do operador`**: client reached the main menu;
  disposable world created, saved, and reopened in the same session and after
  a full restart; final clean shutdown with exit code 0.
- **`INCONCLUSIVE`**: JInput/Windows-11 warning and the legacy S3 resource-
  fetch attempt — reported by the operator, not found in the captured client
  logs on this pass.
- **`INCONCLUSIVE`**: complete absence of *any other* non-blocking warning
  beyond what is listed above — the logs were searched, not exhaustively
  parsed line-by-line for every possible warning class.
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
