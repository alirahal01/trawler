package io.github.alirahal01.trawler.extensions.curlexport

import io.github.alirahal01.trawler.core.CapturedCall

/**
 * Renders a ready-to-run curl command reproducing this call. Redacted
 * headers already carry the literal "[REDACTED]" placeholder from
 * RedactionConfig, so the generated command inherits that safety by
 * construction — there is no separate redaction step here.
 */
fun CapturedCall.toCurlCommand(): String = buildString {
    append("curl -X ").append(method)
    requestHeaders.forEach { (key, values) ->
        append(" \\\n  -H ").append(shellQuote("$key: ${values.joinToString("; ")}"))
    }
    requestBody?.let { body ->
        append(" \\\n  --data-raw ").append(shellQuote(body.decodeToString()))
    }
    append(" \\\n  ").append(shellQuote(url))
}

private fun shellQuote(value: String): String = "'" + value.replace("'", "'\\''") + "'"
