import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKmpLibrary)
    id("maven-publish")
}

extensions.configure<KotlinMultiplatformExtension> {
    android {
        namespace = "io.github.alirahal01.trawler.extensions.curlexport"
        compileSdk = 36
        minSdk = 24
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    jvm("desktop")
    listOf(iosArm64(), iosSimulatorArm64()).forEach {
        it.binaries.framework { baseName = "curl-export" }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":monitor-core"))
            implementation(project(":monitor-extensions-api"))
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
