# Migration

The legacy ModLoader implementation is evidence for externally observable parity, not a source tree for mechanical porting. Harvester 2.x uses a clean history and independently written StationAPI integration.

Legacy configuration migration is planned after hook and domain design. It must parse copied disposable fixtures, preserve unknown input, validate values, write atomically, and provide rollback. No migration is implemented in the foundation.
