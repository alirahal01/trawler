plugins {
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.androidKmpLibrary) apply false
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
}

// Trawler is a from-scratch alternative to KtorMonitor (ro.cosminmihu.ktor) and
// must never depend on it, directly or transitively — enforced here rather than
// left to code review, since a transitive pull-in would be easy to miss.
val bannedGroups = listOf("ro.cosminmihu.ktor")

subprojects {
    configurations.configureEach {
        bannedGroups.forEach { exclude(group = it) }
    }

    val checkBannedDependencies = tasks.register("checkBannedDependencies") {
        group = "verification"
        description = "Fails if any configuration directly declares a banned dependency group."
        doLast {
            configurations.forEach { config ->
                config.dependencies.forEach { dep ->
                    val depGroup = dep.group
                    if (depGroup != null && bannedGroups.contains(depGroup)) {
                        throw GradleException(
                            "Banned dependency detected: $depGroup:${dep.name} " +
                                "in configuration '${config.name}' of project '$path'. " +
                                "Trawler must never depend on $bannedGroups."
                        )
                    }
                }
            }
        }
    }

    tasks.matching { it.name == "check" }.configureEach {
        dependsOn(checkBannedDependencies)
    }
}
