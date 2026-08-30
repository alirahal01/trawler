package io.github.alirahal01.trawler.extensions.mcpserver

import io.github.alirahal01.trawler.core.CapturedCall
import kotlinx.serialization.Serializable

/**
 * Pure, target-independent analysis over a [CapturedCall] snapshot — no
 * Ktor, no JSON-RPC, no coroutines. Each function is data in, data out, so
 * it's unit-testable without ever starting a server (see
 * McpDiagnosticsTest). [McpTools] wraps these as MCP tools.
 */

@Serializable
data class CallSummary(
    val id: String,
    val method: String,
    val url: String,
    val status: Int?,
    val durationMillis: Long?,
    val startedAtEpochMillis: Long,
    val error: String?,
)

@Serializable
data class CallDetail(
    val id: String,
    val method: String,
    val url: String,
    val status: Int?,
    val durationMillis: Long?,
    val startedAtEpochMillis: Long,
    val error: String?,
    val requestHeaders: Map<String, List<String>>,
    val responseHeaders: Map<String, List<String>>,
    val requestBody: String?,
    val responseBody: String?,
)

@Serializable
data class DuplicateGroup(
    val method: String,
    val url: String,
    val callIds: List<String>,
    val count: Int,
    val spanMillis: Long,
)

@Serializable
data class ConcurrentGroup(
    val url: String,
    val methods: List<String>,
    val callIds: List<String>,
    val overlapStartEpochMillis: Long,
    val overlapEndEpochMillis: Long,
)

@Serializable
data class CallStats(
    val totalCalls: Int,
    val errorCount: Int,
    val errorRate: Double,
    val avgDurationMillis: Double?,
    val p50DurationMillis: Long?,
    val p95DurationMillis: Long?,
    val byHost: Map<String, Int>,
    val byStatus: Map<String, Int>,
)

fun CapturedCall.toSummary(): CallSummary =
    CallSummary(id, method, url, status, durationMillis, startedAtEpochMillis, error)

fun CapturedCall.toDetail(): CallDetail = CallDetail(
    id = id,
    method = method,
    url = url,
    status = status,
    durationMillis = durationMillis,
    startedAtEpochMillis = startedAtEpochMillis,
    error = error,
    requestHeaders = requestHeaders,
    responseHeaders = responseHeaders,
    requestBody = requestBody?.decodeToString(),
    responseBody = responseBody?.decodeToString(),
)

private fun CapturedCall.isError(): Boolean = error != null || (status ?: 0) >= 400

private fun CapturedCall.endedAtEpochMillis(): Long = startedAtEpochMillis + (durationMillis ?: 0)

/**
 * Groups calls sharing (method, url, request body) that recur more than
 * once inside a rolling [windowMillis] window — double-fired effects,
 * missing debounce/dedup, retry storms.
 */
fun findDuplicateCalls(calls: List<CapturedCall>, windowMillis: Long = 2000): List<DuplicateGroup> =
    calls
        .groupBy { Triple(it.method, it.url, it.requestBody?.decodeToString()) }
        .values
        .flatMap { group -> clusterByGap(group.sortedBy { it.startedAtEpochMillis }, windowMillis) }
        .filter { it.size > 1 }
        .map { cluster ->
            DuplicateGroup(
                method = cluster.first().method,
                url = cluster.first().url,
                callIds = cluster.map { it.id },
                count = cluster.size,
                spanMillis = cluster.last().startedAtEpochMillis - cluster.first().startedAtEpochMillis,
            )
        }

private fun clusterByGap(sorted: List<CapturedCall>, maxGapMillis: Long): List<List<CapturedCall>> {
    if (sorted.isEmpty()) return emptyList()
    val clusters = mutableListOf<MutableList<CapturedCall>>(mutableListOf(sorted.first()))
    for (call in sorted.drop(1)) {
        val current = clusters.last()
        if (call.startedAtEpochMillis - current.last().startedAtEpochMillis <= maxGapMillis) {
            current.add(call)
        } else {
            clusters.add(mutableListOf(call))
        }
    }
    return clusters
}

/**
 * Finds calls to the same URL whose [CapturedCall.startedAtEpochMillis,
 * endedAtEpochMillis] intervals overlap — the classic shape of a race
 * condition (e.g. two writes, or a read racing a write, to the same
 * resource).
 */
fun findConcurrentCalls(calls: List<CapturedCall>): List<ConcurrentGroup> =
    calls
        .groupBy { it.url }
        .values
        .flatMap { group -> mergeOverlapping(group.sortedBy { it.startedAtEpochMillis }) }

private fun mergeOverlapping(sorted: List<CapturedCall>): List<ConcurrentGroup> {
    if (sorted.size < 2) return emptyList()
    val groups = mutableListOf<ConcurrentGroup>()
    var cluster = mutableListOf(sorted.first())
    var clusterEnd = sorted.first().endedAtEpochMillis()
    for (call in sorted.drop(1)) {
        if (call.startedAtEpochMillis <= clusterEnd) {
            cluster.add(call)
            clusterEnd = maxOf(clusterEnd, call.endedAtEpochMillis())
        } else {
            if (cluster.size > 1) groups.add(cluster.toConcurrentGroup(clusterEnd))
            cluster = mutableListOf(call)
            clusterEnd = call.endedAtEpochMillis()
        }
    }
    if (cluster.size > 1) groups.add(cluster.toConcurrentGroup(clusterEnd))
    return groups
}

private fun List<CapturedCall>.toConcurrentGroup(overlapEnd: Long): ConcurrentGroup = ConcurrentGroup(
    url = first().url,
    methods = map { it.method }.distinct(),
    callIds = map { it.id },
    overlapStartEpochMillis = first().startedAtEpochMillis,
    overlapEndEpochMillis = overlapEnd,
)

/** Calls at or above [thresholdMillis], slowest first. */
fun findSlowCalls(calls: List<CapturedCall>, thresholdMillis: Long = 1000, limit: Int = 20): List<CallSummary> =
    calls
        .filter { (it.durationMillis ?: 0) >= thresholdMillis }
        .sortedByDescending { it.durationMillis }
        .take(limit)
        .map { it.toSummary() }

/** Aggregate overview: volume, error rate, latency distribution, hot hosts/statuses. */
fun callStats(calls: List<CapturedCall>): CallStats {
    val durations = calls.mapNotNull { it.durationMillis }.sorted()
    val errorCount = calls.count { it.isError() }
    return CallStats(
        totalCalls = calls.size,
        errorCount = errorCount,
        errorRate = if (calls.isEmpty()) 0.0 else errorCount.toDouble() / calls.size,
        avgDurationMillis = if (durations.isEmpty()) null else durations.average(),
        p50DurationMillis = durations.percentile(0.50),
        p95DurationMillis = durations.percentile(0.95),
        byHost = calls.groupingBy { hostOf(it.url) }.eachCount(),
        byStatus = calls.groupingBy { it.status?.toString() ?: "error" }.eachCount(),
    )
}

private fun List<Long>.percentile(fraction: Double): Long? {
    if (isEmpty()) return null
    val index = ((size - 1) * fraction).toInt().coerceIn(0, size - 1)
    return this[index]
}

/**
 * Best-effort host extraction from a URL string — mirrors
 * RedactionConfig's private `hostOf`, duplicated here rather than exposed
 * as shared API surface for a four-line parse.
 */
private fun hostOf(url: String): String {
    val afterScheme = url.substringAfter("://", url)
    val authority = afterScheme.substringBefore("/").substringBefore("?").substringBefore("#")
    val hostAndPort = authority.substringAfter("@")
    return hostAndPort.substringBeforeLast(":")
}
