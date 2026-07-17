# Hook register

| Hook | Preferred surface | Status | Evidence required |
| --- | --- | --- | --- |
| Mod initialization | Fabric Loader entrypoints | Foundation implemented | Client and server log exactly once |
| Block-break observation | Minimal server-side Mixin (no sufficient StationAPI event/API found) | Research complete; ADR proposed, awaiting approval | See `better-beta-program/docs/decisions/0002-block-break-hook-selection.md` and the research worklog for the full evidence trail |
| Client keybinding | StationAPI keybinding API | Planned | No server class loading or conflict |

No gameplay hook or Mixin is implemented. `harvester.mixins.json` is deliberately empty so configuration validity can be tested before an injection target is chosen.
