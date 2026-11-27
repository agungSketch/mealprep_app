import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.File
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    kotlin("plugin.serialization") version "2.0.21"
}

abstract class GenerateBuildConfigTask : DefaultTask() {
    @get:Input
    abstract val apiKey: Property<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val file = File(outputDir.get().asFile, "BuildKonfig.kt")
        file.parentFile.mkdirs()
        file.writeText(
            """
            package com.example.mealprep

            object BuildKonfig {
                const val GEMINI_API_KEY: String = "${apiKey.get()}"
            }
            """.trimIndent()
        )
    }
}

val envFile: File = rootProject.file(".env")
val localPropertiesFile: File = rootProject.file("local.properties")

val geminiApiKey: String = when {
    envFile.exists() -> {
        val envMap = envFile.readLines()
            .filter { it.contains("=") && !it.trim().startsWith("#") }
            .associate {
                val parts = it.split("=", limit = 2)
                parts[0].trim() to parts[1].trim().removeSurrounding("\"").removeSurrounding("'")
            }
        envMap["GEMINI_API_KEY"] ?: ""
    }
    localPropertiesFile.exists() -> {
        val properties = Properties()
        localPropertiesFile.inputStream().use { properties.load(it) }
        properties.getProperty("GEMINI_API_KEY", "")
    }
    else -> ""
}

val generateBuildConfigTask = tasks.register<GenerateBuildConfigTask>("generateBuildConfig") {
    apiKey.set(geminiApiKey)
    outputDir.set(layout.buildDirectory.dir("generated/buildconfig/commonMain/kotlin/com/example/mealprep"))
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }
    android {
       namespace = "com.example.mealprep.shared"
       compileSdk = libs.versions.android.compileSdk.get().toInt()
       minSdk = libs.versions.android.minSdk.get().toInt()
       compilerOptions {
           jvmTarget = JvmTarget.JVM_11
       }
       androidResources {
           enable = true
       }
       withHostTest {
           isIncludeAndroidResources = true
       }
       withDeviceTestBuilder {
           sourceSetTreeName = "test"
       }.configure {
           instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
       }
    }
    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.compose.uiTooling)
            implementation("io.ktor:ktor-client-okhttp:3.0.1")
        }
        iosMain.dependencies {
            implementation("io.ktor:ktor-client-darwin:3.0.1")
        }
        commonMain {
            kotlin.srcDir(generateBuildConfigTask)
            dependencies {
                implementation(libs.compose.runtime)
                implementation(libs.compose.foundation)
                implementation(libs.compose.material3)
                implementation(libs.compose.ui)
                implementation(libs.compose.components.resources)
                implementation(libs.compose.uiToolingPreview)
                implementation(libs.androidx.lifecycle.viewmodelCompose)
                implementation(libs.androidx.lifecycle.runtimeCompose)
                implementation("io.ktor:ktor-client-core:3.0.1")
                implementation("io.ktor:ktor-client-content-negotiation:3.0.1")
                implementation("io.ktor:ktor-serialization-kotlinx-json:3.0.1")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
                implementation("io.github.onseok:peekaboo-ui:0.5.2")
                implementation("io.github.onseok:peekaboo-image-picker:0.5.2")
            }
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        getByName("androidDeviceTest").dependencies {
            implementation(libs.androidx.testRunner)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}
