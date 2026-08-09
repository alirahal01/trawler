package io.github.alirahal01.trawler.core

private val DEFAULT_REDACTED_HEADERS = setOf("Authorization", "Cookie", "Set-Cookie")
private const val REDACTED_PLACEHOLDER = "[REDACTED]"

/**
 * Applied once at capture time, before a [CapturedCall] is ever handed to a
 * MonitorExtension or the viewer — nothing downstream ever sees raw values
 * for [redactedHeaders], and calls to [ignoredHosts] are never captured at
 * all. Header matching is case-insensitive, since HTTP header names are.
 */
class RedactionConfig(
    redactedHeaders: Set<String> = DEFAULT_REDACTED_HEADERS,
    val ignoredHosts: List<String> = emptyList(),
    val maxBodyLength: Int? = null,
) {
    private val redactedHeaders: Set<String> = redactedHeaders.map { it.lowercase() }.toSet()

    fun isIgnored(url: String): Boolean {
        val host = hostOf(url)
        return ignoredHosts.any { host.endsWith(it, ignoreCase = true) }
    }

    fun apply(call: CapturedCall): CapturedCall = call.copy(
        requestHeaders = call.requestHeaders.redactHeaders(),
        responseHeaders = call.responseHeaders.redactHeaders(),
        requestBody = call.requestBody?.truncate(),
        responseBody = call.responseBody?.truncate(),
    )

    private fun Map<String, List<String>>.redactHeaders(): Map<String, List<String>> = mapValues { (key, values) ->
        if (redactedHeaders.contains(key.lowercase())) listOf(REDACTED_PLACEHOLDER) else values
    }

    private fun ByteArray.truncate(): ByteArray {
        val limit = maxBodyLength ?: return this
        return if (size > limit) copyOf(limit) else this
    }
}

/**
 * Best-effort host extraction from a URL string, without pulling in a URL
 * parsing dependency: strips scheme, userinfo, path/query/fragment, and
 * port. Good enough for ordinary hostnames; not IPv6-literal-aware.
 */
private fun hostOf(url: String): String {
    val afterScheme = url.substringAfter("://", url)
    val authority = afterScheme.substringBefore("/").substringBefore("?").substringBefore("#")
    val hostAndPort = authority.substringAfter("@")
    return hostAndPort.substringBeforeLast(":")
}
