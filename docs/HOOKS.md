# Hook register

| Hook | Preferred surface | Status | Evidence required |
| --- | --- | --- | --- |
| Mod initialization | Fabric Loader entrypoints | Foundation implemented | Client and server log exactly once |
| Block-break observation | StationAPI event/public API | Research pending | Correct side, cancellation, tool/block state, multiplayer authority |
| Client keybinding | StationAPI keybinding API | Planned | No server class loading or conflict |

No gameplay hook or Mixin is implemented. `harvester.mixins.json` is deliberately empty so configuration validity can be tested before an injection target is chosen.
