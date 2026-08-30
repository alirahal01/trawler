package io.github.alirahal01.trawler.extensions.mcpserver

import io.github.alirahal01.trawler.core.CapturedCall
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private fun call(
    id: String,
    method: String = "GET",
    url: String = "https://api.example.com/users",
    startedAtEpochMillis: Long = 0,
    durationMillis: Long? = 100,
    status: Int? = 200,
    error: String? = null,
    requestBody: ByteArray? = null,
) = CapturedCall(
    id = id,
    url = url,
    method = method,
    requestBody = requestBody,
    status = status,
    startedAtEpochMillis = startedAtEpochMillis,
    durationMillis = durationMillis,
    error = error,
)

class McpDiagnosticsTest {

    @Test
    fun duplicateCallsWithinTheWindowAreGrouped() {
        val calls = listOf(
            call(id = "1", startedAtEpochMillis = 0),
            call(id = "2", startedAtEpochMillis = 500),
            call(id = "3", startedAtEpochMillis = 900),
        )

        val groups = findDuplicateCalls(calls, windowMillis = 2000)

        assertEquals(1, groups.size)
        assertEquals(listOf("1", "2", "3"), groups.single().callIds)
        assertEquals(900, groups.single().spanMillis)
    }

    @Test
    fun callsOutsideTheWindowAreNotGroupedAsDuplicates() {
        val calls = listOf(
            call(id = "1", startedAtEpochMillis = 0),
            call(id = "2", startedAtEpochMillis = 5000),
        )

        assertTrue(findDuplicateCalls(calls, windowMillis = 2000).isEmpty())
    }

    @Test
    fun aSingleCallIsNeverADuplicate() {
        assertTrue(findDuplicateCalls(listOf(call(id = "1")), windowMillis = 2000).isEmpty())
    }

    @Test
    fun differentUrlsAreNotGroupedTogether() {
        val calls = listOf(
            call(id = "1", url = "https://api.example.com/a", startedAtEpochMillis = 0),
            call(id = "2", url = "https://api.example.com/b", startedAtEpochMillis = 100),
        )

        assertTrue(findDuplicateCalls(calls, windowMillis = 2000).isEmpty())
    }

    @Test
    fun overlappingCallsToTheSameUrlAreFlaggedAsConcurrent() {
        val calls = listOf(
            call(id = "1", method = "PATCH", startedAtEpochMillis = 0, durationMillis = 500),
            call(id = "2", method = "GET", startedAtEpochMillis = 200, durationMillis = 100),
        )

        val groups = findConcurrentCalls(calls)

        assertEquals(1, groups.size)
        assertEquals(setOf("1", "2"), groups.single().callIds.toSet())
        assertEquals(setOf("PATCH", "GET"), groups.single().methods.toSet())
    }

    @Test
    fun sequentialCallsToTheSameUrlDoNotOverlap() {
        val calls = listOf(
            call(id = "1", startedAtEpochMillis = 0, durationMillis = 100),
            call(id = "2", startedAtEpochMillis = 200, durationMillis = 100),
        )

        assertTrue(findConcurrentCalls(calls).isEmpty())
    }

    @Test
    fun slowCallsAreReturnedAboveTheThresholdSlowestFirst() {
        val calls = listOf(
            call(id = "fast", durationMillis = 50),
            call(id = "slowest", durationMillis = 3000),
            call(id = "slow", durationMillis = 1200),
        )

        val slow = findSlowCalls(calls, thresholdMillis = 1000)

        assertEquals(listOf("slowest", "slow"), slow.map { it.id })
    }

    @Test
    fun callStatsSummarizeVolumeErrorsAndDurationPercentiles() {
        val calls = listOf(
            call(id = "1", status = 200, durationMillis = 100, url = "https://a.example.com/x"),
            call(id = "2", status = 500, durationMillis = 200, url = "https://a.example.com/x"),
            call(id = "3", status = null, error = "timeout", durationMillis = null, url = "https://b.example.com/y"),
        )

        val stats = callStats(calls)

        assertEquals(3, stats.totalCalls)
        assertEquals(2, stats.errorCount)
        assertEquals(2.0 / 3.0, stats.errorRate)
        assertEquals(mapOf("a.example.com" to 2, "b.example.com" to 1), stats.byHost)
        assertEquals(mapOf("200" to 1, "500" to 1, "error" to 1), stats.byStatus)
    }

    @Test
    fun callStatsOnAnEmptyListHasNoDivisionByZero() {
        val stats = callStats(emptyList())

        assertEquals(0, stats.totalCalls)
        assertEquals(0.0, stats.errorRate)
        assertEquals(null, stats.avgDurationMillis)
    }
}
