# License audit

The legacy Harvester license and contributor ownership must be audited before any Harvester 2.x repository-wide relicensing. The clean foundation does not claim that legacy code is 0BSD and uses `LicenseRef-Harvester-Audit-Pending` in metadata.

StationAPI is MIT; Fabric Loader is Apache-2.0; Biny mappings are CC0-1.0. The Babric Loom extension checkout has no license file, so its source must not be copied. Maven consumption for build qualification does not authorize source reuse. Minecraft, MCP, decompiled sources, launcher libraries, and third-party JARs are never redistributed.

## Runtime dependencies

- `org.apache.logging.log4j:log4j-api:2.17.2` — direct compile/runtime dependency (Apache License 2.0). Not copied, modified, shaded, or bundled into the distributed Harvester JAR.
- `org.apache.logging.log4j:log4j-core:2.17.2` — runtime logging implementation (Apache License 2.0). Not copied, modified, shaded, or bundled into the distributed Harvester JAR.

Confirmed by inspecting the remapped JAR (`build/libs/harvester-b1.7.3-1.0.0-beta.1.jar`) contents directly: no `org/apache/logging/log4j/` classes are present. The build declares no shadow, shade, relocate, or Loom `include()` mechanism that would bundle either artifact.

## Test-only dependencies

JUnit 5.11.4 (`junit-bom`, `junit-jupiter`, `junit-platform-launcher`) is `testImplementation`/`testRuntimeOnly` only and does not enter the distributed artifact.

## Build tooling

The Gradle Wrapper (`gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`, `gradle-wrapper.properties`) remains under the Apache License 2.0 and is the only tool JAR deliberately tracked in this repository.
