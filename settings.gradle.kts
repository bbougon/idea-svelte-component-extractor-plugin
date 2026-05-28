pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://www.jetbrains.com/intellij-repository/releases")
    }
}

plugins {
    id("org.jetbrains.intellij.platform.settings") version "2.16.0"
}

rootProject.name = "plugin-extrait-composant"
