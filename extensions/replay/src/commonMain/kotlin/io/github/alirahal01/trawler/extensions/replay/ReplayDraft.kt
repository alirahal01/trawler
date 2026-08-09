package io.github.alirahal01.trawler.extensions.replay

import io.github.alirahal01.trawler.core.CapturedCall

/**
 * An editable draft of a captured request, pre-filled from the call that
 * started it. Redacted fields arrive already carrying the literal
 * "[REDACTED]" placeholder (RedactionConfig already ran by capture time) —
 * if the user doesn't overwrite it, the refire goes out with that literal
 * value and fails obviously rather than silently omitting the header.
 */
data class ReplayDraft(
    val originalCallId: String,
    val method: String,
    val url: String,
    val headers: List<Pair<String, String>>,
    val body: String?,
) {
    companion object {
        fun from(call: CapturedCall): ReplayDraft = ReplayDraft(
            originalCallId = call.id,
            method = call.method,
            url = call.url,
            headers = call.requestHeaders.flatMap { (key, values) -> values.map { key to it } },
            body = call.requestBody?.decodeToString(),
        )
    }
}

/** Parses "Key: Value" lines from a headers text field; blank/malformed lines are skipped. */
fun parseHeaderLines(text: String): List<Pair<String, String>> = text.lines().mapNotNull { line ->
    val separatorIndex = line.indexOf(':')
    if (separatorIndex <= 0) return@mapNotNull null
    val key = line.substring(0, separatorIndex).trim()
    val value = line.substring(separatorIndex + 1).trim()
    if (key.isEmpty()) null else key to value
}

/** Inverse of [parseHeaderLines], used to seed the editable text field from a draft. */
fun List<Pair<String, String>>.toHeaderLines(): String = joinToString("\n") { (key, value) -> "$key: $value" }
