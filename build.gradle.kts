plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.17.3"
    kotlin("jvm") version "1.9.24"
}

group = "com.tanisha"
version = "0.0.2"

repositories { mavenCentral() }

// Explicitly configure the Java toolchain to resolve build issues.
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

// Configure Kotlin to use the same toolchain.
kotlin {
    jvmToolchain(17)
}

// Configure the IntelliJ Platform plugin
intellij {
    type.set("IC") // Use "IC" for Community or "IU" for Ultimate
    version.set("2024.2")
    plugins.set(listOf()) // Add plugin dependencies here if needed
}

// Set the compatibility range for the plugin
tasks.withType<org.jetbrains.intellij.tasks.PatchPluginXmlTask> {
    sinceBuild.set("242")
    untilBuild.set("") // Use an empty string to signify no upper bound
}

// Add dependencies
dependencies {
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.11.0")
}
