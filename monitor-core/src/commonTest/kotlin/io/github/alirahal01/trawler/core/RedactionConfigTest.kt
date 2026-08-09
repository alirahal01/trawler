package io.github.alirahal01.trawler.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RedactionConfigTest {

    private fun call(
        headers: Map<String, List<String>> = emptyMap(),
        url: String = "https://example.com/path",
        body: ByteArray? = null,
    ) = CapturedCall(
        id = "1",
        url = url,
        method = "GET",
        requestHeaders = headers,
        responseHeaders = headers,
        requestBody = body,
        responseBody = body,
        startedAtEpochMillis = 0L,
    )

    @Test
    fun redactsDefaultHeadersCaseInsensitively() {
        val redaction = RedactionConfig()
        val result = redaction.apply(
            call(
                headers = mapOf(
                    "authorization" to listOf("Bearer secret"),
                    "X-Custom" to listOf("keep-me"),
                ),
            ),
        )

        assertEquals(listOf("[REDACTED]"), result.requestHeaders["authorization"])
        assertEquals(listOf("keep-me"), result.requestHeaders["X-Custom"])
    }

    @Test
    fun redactsCookieAndSetCookieByDefault() {
        val redaction = RedactionConfig()
        val result = redaction.apply(
            call(headers = mapOf("Cookie" to listOf("a=b"), "Set-Cookie" to listOf("c=d"))),
        )

        assertEquals(listOf("[REDACTED]"), result.requestHeaders["Cookie"])
        assertEquals(listOf("[REDACTED]"), result.requestHeaders["Set-Cookie"])
    }

    @Test
    fun hostAppCanExtendRedactedHeaders() {
        val redaction = RedactionConfig(redactedHeaders = setOf("Authorization", "Cookie", "Set-Cookie", "X-Api-Key"))
        val result = redaction.apply(call(headers = mapOf("X-Api-Key" to listOf("secret"))))

        assertEquals(listOf("[REDACTED]"), result.requestHeaders["X-Api-Key"])
    }

    @Test
    fun truncatesBodiesLongerThanMaxBodyLength() {
        val redaction = RedactionConfig(maxBodyLength = 4)
        val result = redaction.apply(call(body = "0123456789".encodeToByteArray()))

        assertEquals("0123", result.requestBody?.decodeToString())
        assertEquals("0123", result.responseBody?.decodeToString())
    }

    @Test
    fun leavesShortBodiesUntouched() {
        val redaction = RedactionConfig(maxBodyLength = 100)
        val result = redaction.apply(call(body = "short".encodeToByteArray()))

        assertEquals("short", result.requestBody?.decodeToString())
    }

    @Test
    fun isIgnoredMatchesHostBySuffix() {
        val redaction = RedactionConfig(ignoredHosts = listOf("analytics.example.com"))

        assertTrue(redaction.isIgnored("https://analytics.example.com/track"))
        assertTrue(redaction.isIgnored("https://sub.analytics.example.com/track"))
        assertFalse(redaction.isIgnored("https://example.com/track"))
    }

    @Test
    fun isIgnoredDefaultsToNothingIgnored() {
        val redaction = RedactionConfig()

        assertFalse(redaction.isIgnored("https://example.com/anything"))
    }
}
