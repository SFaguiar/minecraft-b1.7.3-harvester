# Harvester

Clean source lineage for the planned Harvester 2.x port to Minecraft Java
Edition Beta 1.7.3 using Babric, Fabric Loader, StationAPI, Java 17, Loom, and
Mixins.

## Current status

This repository contains compliance controls and design records only. It does
not yet contain gameplay code or a StationAPI project skeleton.

The ModLoader 1.x implementation remains a separate legacy artifact. Its Git
history is not a parent of this repository, and no Java source was imported
from it. Future implementation will be based on an independently written
behavior specification and tests.

## Provenance boundary

The following material is prohibited here:

- Minecraft or MCP source and bytecode;
- reconstructed or decompiled ModLoader source;
- Minecraft, ModLoader, dependency, or third-party mod JARs;
- launcher instances, accounts, worlds, logs, and caches;
- code copied from functionally similar mods.

Run the repository guard before staging changes:

```powershell
pwsh -NoProfile -File .\scripts\check-prohibited-files.ps1
```

## License status

No Harvester 2.x license is granted yet. The intended program default is 0BSD,
but relicensing this port is blocked until the authorship and provenance audit
is complete. Dependencies retain their own licenses.
