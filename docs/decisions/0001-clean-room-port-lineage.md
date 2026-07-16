# ADR 0001: Use a clean Git lineage for the StationAPI port

- Status: Accepted
- Date: 2026-07-16

## Context

The legacy ModLoader repository tracks a complete decompiled Minecraft base class, a
reconstructed ModLoader annotation, and released bytecode derived from the base class.
Published tags must not be rewritten and proprietary material must not be carried into
Better Beta.

## Decision

Harvester 2.x starts in a new Git repository with no shared commits, copied Java, or
binary artifacts. The legacy implementation is evidence for a prose behavioral spec
only. New code targets Babric and StationAPI using Java 17.

## Consequences

- Legacy commit continuity is intentionally not preserved in the port.
- Authorship and licensing of new code can be audited independently.
- Historical compatibility must be demonstrated with tests rather than source reuse.
- The old public tag remains a separate remediation concern unless destructive history
  replacement is explicitly approved later.
