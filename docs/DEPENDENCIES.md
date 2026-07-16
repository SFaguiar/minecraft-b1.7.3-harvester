# Dependencies

Qualified 2026-07-16. All versions are exact.

| Component | Version | Purpose / side |
| --- | --- | --- |
| Temurin | 17.0.19+10 | Build and runtime; both |
| Gradle Wrapper | 8.12.1; distribution SHA-256 `8d97a97984f6cbd2b85fe4c60a743440a347544bf18818048e611f5288d46c94` | Build |
| Fabric Loom | 1.10.5 | Build; last common stable line verified with JVM 17 metadata |
| Babric Loom extension | 1.10.5 | Build; source reuse prohibited because checkout has no license file |
| Biny mappings | b1.7.3+e1fe071 v2 | Build mappings; CC0-1.0 |
| Fabric Loader | 0.19.3 | Runtime; both; Apache-2.0 |
| StationAPI | 2.0.0-alpha.6.2, tag `2fa8169dcdf80c8530a4730c2f84ea47de32a92e` | Runtime API; both; MIT |
| Sponge Mixin | `0.17.3+mixin.0.8.7` resolved through Loader | Runtime hooks; both; MIT |
| MixinExtras | 0.5.4 resolved through Loader | Transitive runtime; both |
| UnsafeEvents | e31096e | StationAPI transitive runtime |
| TypeTools | 0.8.3 | StationAPI transitive runtime |
| Spasm | 0.2.2 | StationAPI transitive runtime |
| SLF4J API | 2.0.9 resolved | Logging facade; both |
| Log4j SLF4J binding | 2.17.2 | Logging backend alignment; both |
| JUnit | 5.11.4 | Isolated tests only |

The Maven Forge repository hosts TypeTools 0.8.3; Forge/FML is not a project dependency. The Glass `snapshots` repository is needed by StationAPI publication metadata, but no dependency version contains `SNAPSHOT`.
