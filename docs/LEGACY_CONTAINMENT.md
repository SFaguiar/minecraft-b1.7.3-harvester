# Legacy containment record

## Completed locally

- Verified the legacy tag and commit without changing either.
- Created and verified a complete private Git bundle.
- Created this unrelated clean Git history.
- Imported no Java source or binary from the legacy repository.
- Added an automated prohibited-content check.

## Public remote containment

The public legacy repository currently exposes both the decompiled source snapshot and
a release JAR containing a modified base class. Under the standing rules, the safe
non-rewriting remediation is:

1. delete the `Harvester.jar` release asset while preserving release notes and tag;
2. add a normal legacy maintenance commit removing `PlayerControllerSP.java` and
   `MLProp.java` from the default branch;
3. revise legacy documentation so it does not claim all tracked material is MIT;
4. mark the legacy repository as superseded and do not use it for future releases;
5. use this clean repository for all StationAPI work.

Steps 1 through 4 require authenticated remote changes. They do not remove objects
reachable from the published tag. Complete erasure would require destructive history
replacement or repository deletion, which is outside the approved policy and is not
performed automatically.
