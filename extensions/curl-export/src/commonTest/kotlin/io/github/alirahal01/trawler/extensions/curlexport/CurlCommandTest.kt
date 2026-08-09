package io.github.alirahal01.trawler.extensions.curlexport

import io.github.alirahal01.trawler.core.CapturedCall
import kotlin.test.Test
import kotlin.test.assertEquals

class CurlCommandTest {

    private fun call(
        method: String = "GET",
        headers: Map<String, List<String>> = emptyMap(),
        body: ByteArray? = null,
        url: String = "https://example.com/path",
    ) = CapturedCall(
        id = "1",
        url = url,
        method = method,
        requestHeaders = headers,
        requestBody = body,
        startedAtEpochMillis = 0L,
    )

    @Test
    fun rendersSimpleGetWithNoHeadersOrBody() {
        val command = call().toCurlCommand()

        assertEquals("curl -X GET \\\n  'https://example.com/path'", command)
    }

    @Test
    fun rendersHeadersAsSeparateDashHFlags() {
        val command = call(
            headers = mapOf("Authorization" to listOf("[REDACTED]"), "Accept" to listOf("application/json")),
        ).toCurlCommand()

        assertEquals(
            "curl -X GET \\\n" +
                "  -H 'Authorization: [REDACTED]' \\\n" +
                "  -H 'Accept: application/json' \\\n" +
                "  'https://example.com/path'",
            command,
        )
    }

    @Test
    fun rendersBodyAsDataRaw() {
        val command = call(
            method = "POST",
            body = """{"key":"value"}""".encodeToByteArray(),
        ).toCurlCommand()

        assertEquals(
            "curl -X POST \\\n" +
                "  --data-raw '{\"key\":\"value\"}' \\\n" +
                "  'https://example.com/path'",
            command,
        )
    }

    @Test
    fun escapesSingleQuotesInValues() {
        val command = call(url = "https://example.com/path?q=it's").toCurlCommand()

        assertEquals("curl -X GET \\\n  'https://example.com/path?q=it'\\''s'", command)
    }
}
