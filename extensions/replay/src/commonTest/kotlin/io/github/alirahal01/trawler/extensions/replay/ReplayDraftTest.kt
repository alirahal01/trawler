package io.github.alirahal01.trawler.extensions.replay

import io.github.alirahal01.trawler.core.CapturedCall
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ReplayDraftTest {

    @Test
    fun fromFlattensMultiValueHeadersIntoOneRowPerValue() {
        val call = CapturedCall(
            id = "1",
            url = "https://example.com",
            method = "GET",
            requestHeaders = mapOf("Accept" to listOf("application/json", "text/plain")),
            startedAtEpochMillis = 0L,
        )

        val draft = ReplayDraft.from(call)

        assertEquals(
            listOf("Accept" to "application/json", "Accept" to "text/plain"),
            draft.headers,
        )
    }

    @Test
    fun fromCarriesTheRedactedPlaceholderLiterallyIntoTheDraft() {
        val call = CapturedCall(
            id = "1",
            url = "https://example.com",
            method = "GET",
            requestHeaders = mapOf("Authorization" to listOf("[REDACTED]")),
            startedAtEpochMillis = 0L,
        )

        val draft = ReplayDraft.from(call)

        assertEquals(listOf("Authorization" to "[REDACTED]"), draft.headers)
    }

    @Test
    fun fromDecodesBodyAsTextAndNullWhenAbsent() {
        val withBody = ReplayDraft.from(
            CapturedCall(
                id = "1",
                url = "https://example.com",
                method = "POST",
                requestBody = "hello".encodeToByteArray(),
                startedAtEpochMillis = 0L,
            ),
        )
        val withoutBody = ReplayDraft.from(
            CapturedCall(id = "2", url = "https://example.com", method = "GET", startedAtEpochMillis = 0L),
        )

        assertEquals("hello", withBody.body)
        assertNull(withoutBody.body)
    }

    @Test
    fun parseHeaderLinesSkipsBlankAndMalformedLines() {
        val parsed = parseHeaderLines(
            "Accept: application/json\n" +
                "\n" +
                "not-a-header\n" +
                "X-Empty-Key: value\n" +
                ": no-key",
        )

        assertEquals(listOf("Accept" to "application/json", "X-Empty-Key" to "value"), parsed)
    }

    @Test
    fun toHeaderLinesIsTheInverseOfParseHeaderLines() {
        val original = listOf("Accept" to "application/json", "X-Custom" to "value")

        assertEquals(original, parseHeaderLines(original.toHeaderLines()))
    }
}
