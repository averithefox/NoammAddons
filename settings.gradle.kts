pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/")
        gradlePluginPortal()
        mavenCentral()
    }

    plugins {
        id("net.fabricmc.fabric-loom-remap") version providers.gradleProperty("loom_version")
    }
}

rootProject.name = "NoammAddons"
