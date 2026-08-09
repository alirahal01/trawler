package io.github.alirahal01.trawler.extensions.presetplayground

/**
 * A named payload for firing an [Endpoint] — independent of whether that
 * endpoint was ever captured live. Defined as plain Kotlin by the host app
 * rather than a loaded JSON/YAML file: type-safe and refactor-safe, with no
 * new file format or parser to maintain.
 */
data class EndpointPreset(
    val label: String,
    val headers: List<Pair<String, String>> = emptyList(),
    val body: String? = null,
)

/** A registered, fireable endpoint with zero or more saved [presets]. */
data class Endpoint(
    val label: String,
    val method: String,
    val url: String,
    val presets: List<EndpointPreset> = emptyList(),
)
