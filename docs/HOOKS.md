# Hook register

| Hook | Preferred surface | Status | Evidence required |
| --- | --- | --- | --- |
| Mod initialization | Fabric Loader entrypoints | Foundation implemented | Client and server log exactly once |
| Block-break observation | Two minimal Mixin adapters — one client-only (singleplayer), one server-only (multiplayer/dedicated server); no single point covers both, since Beta 1.7.3 has no integrated server | Research complete (revised); ADR proposed, awaiting approval | See `better-beta-program/docs/decisions/0002-block-break-hook-selection.md` (Revision 2) and the research + revision worklogs for the full evidence trail |
| Client keybinding | StationAPI keybinding API | Planned | No server class loading or conflict |

No gameplay hook or Mixin is implemented. `harvester.mixins.json` is deliberately empty so configuration validity can be tested before an injection target is chosen.
