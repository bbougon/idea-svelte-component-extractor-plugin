import java.net.URL

plugins {
    id("java")
    kotlin("jvm") version "2.3.0"
    id("org.jetbrains.intellij.platform")
}

group = "com.github.bertrand"
version = "1.0-SNAPSHOT"

val sveltePluginVersion = "261.25134.10"

// Download during configuration phase if not already present
val sveltePluginFile: File = layout.buildDirectory.dir("downloads").get().asFile
    .resolve("svelte-$sveltePluginVersion.zip")
    .also { file ->
        if (!file.exists()) {
            file.parentFile.mkdirs()
            logger.lifecycle("Downloading Svelte plugin $sveltePluginVersion...")
            val url = URL("https://plugins.jetbrains.com/files/12375/1053488/svelte-$sveltePluginVersion.zip")
            file.writeBytes(url.readBytes())
        }
    }

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaUltimate("2026.1")
        bundledPlugins("JavaScript")
        localPlugin(sveltePluginFile)
    }
    testImplementation(kotlin("test"))
}

intellijPlatform {
    pluginConfiguration {
        name.set("Svelte Extract Component")
        changeNotes.set("""
          Initial version of Svelte Extract Component.
          Upgraded to IntelliJ Platform 2026.1.
        """)
    }

    signing {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishing {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}

kotlin {
    jvmToolchain(26)
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
    test {
        useJUnitPlatform()
    }
}
