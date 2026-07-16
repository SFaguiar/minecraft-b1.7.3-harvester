# Security and publication:

- a complete decompiled `PlayerControllerSP.java` base class with a Harvester hook;
- a reconstructed `MLProp.java` annotation based on ModLoader behavior;
- original-looking Harvester classes mixed with legacy API dependencies;
- a build pipeline that packages reobfuscated changed classes;
- a release asset whose JAR includes `os.class`, the reobfuscated base class.

The release asset recorded during the audit was 10,556 bytes with SHA-256
`6d00af7b7d7eea76cc3b2c596d45bc64077321e57fd08b08f607385f7551c69f`.

These facts mean the legacy repository cannot be used as the Git ancestor or copied
source base of the public StationAPI port.

## Clean-room rule

The modern implementation may use only:

1. behavioral requirements written in ordinary prose;
2. independently designed tests and test fixtures;
3. public Babric, Fabric Loader, StationAPI, Loom, and Mixin APIs;
4. original new code written for this repository;
5. third-party code only when its license is verified, preserved, and documented.

Do not copy identifiers, comments, control flow, source structure, patches, bytecode,
or reconstructed third-party interfaces from the legacy source. Names required for
user-facing compatibility, such as configuration keys, must be documented as an
explicit compatibility decision.

## Evidence preservation

A complete Git bundle of the legacy checkout is kept outside this repository under
the workspace's private `_legacy/harvester-backups` area. It is audit evidence, not a
build input and not a publication artifact.
