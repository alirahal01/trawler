plugins {
    id("org.jetbrains.kotlin.jvm")
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":sample-app:shared"))
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
}

compose.desktop {
    application {
        mainClass = "io.github.alirahal01.trawler.sample.desktop.MainKt"

        nativeDistributions {
            packageName = "TrawlerSample"
            packageVersion = "0.1.0"
        }
    }
}
