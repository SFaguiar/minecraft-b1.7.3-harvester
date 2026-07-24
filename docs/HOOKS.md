# Hook register

| Hook | Preferred surface | Status | Evidence required |
| --- | --- | --- | --- |
| Mod initialization | Fabric Loader entrypoints | Implemented | Client and server log exactly once |
| Block-break observation | Two minimal Mixin adapters — one client-only (singleplayer), one server-only (multiplayer/dedicated server); no single point covers both, since Beta 1.7.3 has no integrated server | Implemented and manually validated (singleplayer and multiplayer) | See `better-beta-program/docs/decisions/0002-block-break-hook-selection.md` for the selection rationale |
| Client keybinding | StationAPI keybinding API | Implemented (harvest activation and config screen) | No server class loading or conflict |
| Drop consolidation | Mixin on `dropStack` plus a `@WrapMethod` around each side's break entry point | Implemented and manually validated | Single stack per item at the break origin, foreign drops untouched |

`harvester.mixins.json` registers the Mixins listed above (client, server, and shared). See that file for the exact class list.
