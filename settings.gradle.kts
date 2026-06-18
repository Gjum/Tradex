pluginManagement {
    repositories {
        mavenCentral()
//        gradlePluginPortal()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
//        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
//        maven("https://maven.parchmentmc.org") { name = "ParchmentMC" }
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9.5"
    id("dev.kikugie.loom-back-compat") version "0.3"
}

stonecutter {
    create(rootProject) {
        versions("1.21.2", "1.21.4", "1.21.8", "1.21.11")
        version("26.1", "26.1.2")
        vcsVersion = "26.1"
    }
}

rootProject.name = "Tradex"
