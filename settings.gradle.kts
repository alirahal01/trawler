rootProject.name = "trawler"

pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

include(
    ":monitor-core",
    ":monitor-ktor",
    ":monitor-ui-compose",
    ":monitor-extensions-api",
    ":extensions:curl-export",
    ":extensions:replay",
    ":extensions:preset-playground",
    ":extensions:mcp-server",
    ":sample-app:shared",
    ":sample-app:androidApp",
    ":sample-app:desktopApp",
)
