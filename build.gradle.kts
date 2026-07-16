plugins {
    java
    `maven-publish`
    id("fabric-loom") version "1.10.5"
    id("babric-loom-extension") version "1.10.5"
}
group = property("maven_group") as String
version = property("mod_version") as String
base { archivesName = property("archives_base_name") as String }
java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
    withSourcesJar()
}
repositories {
    maven("https://maven.glass-launcher.net/releases")
    maven("https://maven.glass-launcher.net/snapshots")
    maven("https://maven.glass-launcher.net/babric")
    maven("https://maven.minecraftforge.net")
    maven("https://jitpack.io")
    mavenCentral()
    exclusiveContent {
        forRepository { maven("https://api.modrinth.com/maven") }
        filter { includeGroup("maven.modrinth") }
    }
}
dependencies {
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    mappings("net.glasslauncher:biny:${property("mappings_version")}:v2")
    modImplementation("net.fabricmc:fabric-loader:${property("loader_version")}")
    modImplementation("net.modificationstation:StationAPI:${property("stationapi_version")}")
    implementation("org.slf4j:slf4j-api:1.8.0-beta4")
    runtimeOnly("org.apache.logging.log4j:log4j-slf4j18-impl:2.17.2")
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") { expand("version" to project.version) }
}
tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(17)
}
tasks.test { useJUnitPlatform() }
tasks.jar { from("LICENSES") { into("META-INF/licenses") } }
