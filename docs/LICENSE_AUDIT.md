# License audit

The authorship and provenance audit of Harvester's own code is complete. The owner's authorization to license this original content under 0BSD is recorded in `docs/decisions/0004-ownership-and-0bsd-authorization.md`; that document and this technical audit are the basis for the `license: 0BSD` value in `fabric.mod.json`. This audit is a technical inventory, not a legal certification.

StationAPI is MIT; Fabric Loader is Apache-2.0; Biny mappings are CC0-1.0. The Babric Loom extension checkout has no license file, so its source must not be copied. Maven consumption for build qualification does not authorize source reuse. Minecraft, MCP, decompiled sources, launcher libraries, and third-party JARs are never redistributed.

## Runtime dependencies

- `org.apache.logging.log4j:log4j-api:2.17.2` — direct compile/runtime dependency (Apache License 2.0). Not copied, modified, shaded, or bundled into the distributed Harvester JAR.
- `org.apache.logging.log4j:log4j-core:2.17.2` — runtime logging implementation (Apache License 2.0). Not copied, modified, shaded, or bundled into the distributed Harvester JAR.

Confirmed by inspecting the remapped JAR (`build/libs/harvester-b1.7.3-1.0.0-beta.1.jar`) contents directly: no `org/apache/logging/log4j/` classes are present. The build declares no shadow, shade, relocate, or Loom `include()` mechanism that would bundle either artifact.

## Test-only dependencies

JUnit 5.11.4 (`junit-bom`, `junit-jupiter`, `junit-platform-launcher`) is `testImplementation`/`testRuntimeOnly` only and does not enter the distributed artifact.

## Build tooling

The Gradle Wrapper (`gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`, `gradle-wrapper.properties`) remains under the Apache License 2.0 and is the only tool JAR deliberately tracked in this repository.
