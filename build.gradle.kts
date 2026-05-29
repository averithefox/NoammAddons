plugins {
    `maven-publish`
    kotlin("jvm") version "2.3.21"
    id("net.fabricmc.fabric-loom-remap")
    id("com.gradleup.shadow") version "9.4.1"
    kotlin("plugin.serialization") version "2.3.21"
}

val minecraft_version: String by project
val loader_version: String by project
val fabric_kotlin_version: String by project
val mod_version: String by project
val maven_group: String by project
val fabric_version: String by project
val modmenu_version: String by project
val iris_version: String by project
val ktor_version: String by project

version = mod_version
group = maven_group

repositories {
    maven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1")
    maven("https://maven.terraformersmc.com/")
    maven("https://api.modrinth.com/maven")
    maven("https://jitpack.io")
}

val shadowImplementation by configurations.creating {
    configurations.implementation {
        extendsFrom(this@creating)
    }
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraft_version")
    mappings(loom.officialMojangMappings())

    modImplementation("net.fabricmc:fabric-loader:$loader_version")
    modImplementation("net.fabricmc.fabric-api:fabric-api:$fabric_version")
    modImplementation("net.fabricmc:fabric-language-kotlin:$fabric_kotlin_version")

    modRuntimeOnly("me.djtheredstoner:DevAuth-fabric:1.2.2")
    modCompileOnly("maven.modrinth:iris:$iris_version")
    modCompileOnly("com.terraformersmc:modmenu:$modmenu_version")

    shadowImplementation("io.github.classgraph:classgraph:4.8.174")
    shadowImplementation("io.ktor:ktor-client-cio:$ktor_version")
    shadowImplementation("io.ktor:ktor-client-websockets-jvm:$ktor_version")
    shadowImplementation("io.ktor:ktor-client-content-negotiation-jvm:$ktor_version")
    shadowImplementation("io.ktor:ktor-client-encoding:$ktor_version")
    shadowImplementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktor_version")

    modImplementation("com.github.Noamm9:DataFixer:7148621a34")
    include("com.github.Noamm9:DataFixer:7148621a34")

    testImplementation(kotlin("test"))
}

loom {
    accessWidenerPath = file("src/main/resources/noammaddons.classTweaker")

    runConfigs.named("client") {
        isIdeConfigGenerated = true
        vmArgs.add("-Dmixin.debug.export=true")
        vmArgs.add("-Ddevauth.enabled=true")
        vmArgs.add("-Ddevauth.account=main")
        vmArgs.add("-XX:+AllowEnhancedClassRedefinition")
        vmArgs.add("-XX:+IgnoreUnrecognizedVMOptions")
    }
}

afterEvaluate {
    loom.runs.named("client") {
        vmArg("-javaagent:${configurations.compileClasspath.get().find { it.name.contains("sponge-mixin") }}")
    }
}

tasks {
    processResources {
        from("LICENSE") {
            rename { "${it}_${project.name}" }
        }

        filesMatching("fabric.mod.json") {
            expand(project.properties)
        }
    }

    jar {
        archiveClassifier = "dev-nodeps"
        destinationDirectory = layout.buildDirectory.dir("badjars")
    }

    shadowJar {
        archiveClassifier = "dev-shadow"
        destinationDirectory = layout.buildDirectory.dir("badjars")

        configurations = listOf(shadowImplementation)

        minimize()

        exclude("META-INF/maven/")
    }

    remapJar {
        archiveClassifier = null
        inputFile = shadowJar.get().archiveFile
    }

    test {
        failOnNoDiscoveredTests = false
    }
}

kotlin {
    jvmToolchain(21)
}

publishing {
    publications {
        register<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
