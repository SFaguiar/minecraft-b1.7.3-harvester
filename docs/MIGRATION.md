# Migration

The legacy ModLoader implementation is evidence for externally observable parity, not a source tree for mechanical porting. Harvester 2.x uses a clean history and independently written StationAPI integration.

Config file migration is implemented: `HarvesterConfigLoader` detects a config file missing any current key (an older schema version) and non-destructively rewrites it — every existing value the player already set is preserved verbatim, and only the missing keys are appended with their defaults. There is no destructive rewrite and no migration from the legacy mod's own config format, which used an incompatible layout.
