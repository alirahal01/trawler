import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.androidKmpLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    id("maven-publish")
}

extensions.configure<KotlinMultiplatformExtension> {
    android {
        namespace = "io.github.alirahal01.trawler.extensions.mcpserver"
        compileSdk = 36
        minSdk = 24
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    jvm("desktop")

    // No iOS targets: the embedded HTTP server is built on ktor-server-cio,
    // which only publishes a JVM variant — see ADR-0004. android and
    // desktop share one actual implementation via this intermediate source
    // set, since both compile to JVM bytecode.
    val jvmAndAndroidMain by sourceSets.creating {
        dependsOn(sourceSets.getByName("commonMain"))
    }
    sourceSets.getByName("androidMain") { dependsOn(jvmAndAndroidMain) }
    sourceSets.getByName("desktopMain") { dependsOn(jvmAndAndroidMain) }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":monitor-core"))
            implementation(project(":monitor-extensions-api"))
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
        }
        jvmAndAndroidMain.dependencies {
            implementation(libs.ktor.server.core)
            implementation(libs.ktor.server.cio)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
        val desktopTest by getting {
            dependencies {
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.cio)
            }
        }
    }
}
