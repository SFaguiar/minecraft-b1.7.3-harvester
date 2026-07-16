# Architecture

Harvester 2.x separates deterministic domain logic in `core` from StationAPI/Minecraft integration in `platform`, client-only and server-only entrypoints, versioned `config`, minimal `mixin`, and explicit optional `compat`. The current foundation contains no gameplay algorithm.

Server authority is the target for multiplayer behavior. Client classes must never be linked from common/server code. Events and public APIs precede Mixins; every future Mixin needs a documented missing hook, narrow injection, failure behavior, and regression test. `@Overwrite` is disallowed without a new approved ADR.
