package io.github.alirahal01.trawler.extensions.mcpserver

import io.github.alirahal01.trawler.core.CapturedCall
import io.github.alirahal01.trawler.extensions.NetworkMonitor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

private val json = Json { encodeDefaults = true }

/** The result of one `tools/call` invocation, before it's wrapped in the JSON-RPC envelope. */
data class ToolResult(val text: String, val isError: Boolean = false)

class McpTool(
    val name: String,
    val description: String,
    val inputSchema: JsonObject,
    val handler: suspend (JsonObject) -> ToolResult,
)

/**
 * The fixed set of diagnostic tools this MCP surface exposes. [monitor] is a
 * provider, not a constructed [NetworkMonitor] — see [McpServerExtension]'s
 * kdoc for why. [replay] is optional: `replay_call` only appears in [tools]
 * when the host app supplies one.
 */
class McpToolRegistry(
    private val monitor: () -> NetworkMonitor,
    private val replay: (suspend (CapturedCall) -> CapturedCall)? = null,
) {
    private fun calls(): List<CapturedCall> = monitor().observeCalls().value

    val tools: List<McpTool> = buildList {
        add(listCallsTool())
        add(getCallTool())
        add(findDuplicateCallsTool())
        add(findConcurrentCallsTool())
        add(findSlowCallsTool())
        add(getCallStatsTool())
        add(clearCallsTool())
        replay?.let { add(replayCallTool(it)) }
    }

    private fun listCallsTool() = McpTool(
        name = "list_calls",
        description = "List recently captured HTTP calls, most recent first, with optional filtering.",
        inputSchema = objectSchema(
            "limit" to integerProperty("Maximum number of calls to return (default 50)."),
            "method" to stringProperty("Exact HTTP method to filter by, e.g. \"POST\"."),
            "urlContains" to stringProperty("Only include calls whose URL contains this substring."),
            "onlyErrors" to booleanProperty("Only include calls with a network error or a 4xx/5xx status."),
        ),
    ) { args ->
        val limit = args.intOrNull("limit") ?: 50
        val method = args.stringOrNull("method")
        val urlContains = args.stringOrNull("urlContains")
        val onlyErrors = args.booleanOrNull("onlyErrors") ?: false
        val result = calls()
            .asReversed()
            .filter { method == null || it.method.equals(method, ignoreCase = true) }
            .filter { urlContains == null || it.url.contains(urlContains) }
            .filter { !onlyErrors || it.error != null || (it.status ?: 0) >= 400 }
            .take(limit)
            .map { it.toSummary() }
        ok(result)
    }

    private fun getCallTool() = McpTool(
        name = "get_call",
        description = "Get full detail for one captured call by id, including headers and decoded bodies.",
        inputSchema = objectSchema("id" to stringProperty("The call id, from list_calls."), required = listOf("id")),
    ) { args ->
        val id = args.stringOrNull("id")
        val call = id?.let { wanted -> calls().find { it.id == wanted } }
        when {
            id == null -> err("Missing required argument \"id\".")
            call == null -> err("No call found with id \"$id\".")
            else -> ok(call.toDetail())
        }
    }

    private fun findDuplicateCallsTool() = McpTool(
        name = "find_duplicate_calls",
        description = "Find calls with the same method, URL, and request body fired more than once within a " +
            "short time window — double-fired effects, missing debounce/dedup, retry storms.",
        inputSchema = objectSchema(
            "windowMs" to integerProperty("Clustering window in milliseconds (default 2000)."),
        ),
    ) { args ->
        val windowMs = args.longOrNull("windowMs") ?: 2000
        ok(findDuplicateCalls(calls(), windowMs))
    }

    private fun findConcurrentCallsTool() = McpTool(
        name = "find_concurrent_calls",
        description = "Find calls to the same URL whose in-flight time windows overlap — the classic shape of " +
            "a race condition (two writes, or a read racing a write, to the same resource).",
        inputSchema = objectSchema(),
    ) { _ ->
        ok(findConcurrentCalls(calls()))
    }

    private fun findSlowCallsTool() = McpTool(
        name = "find_slow_calls",
        description = "List the slowest captured calls at or above a duration threshold.",
        inputSchema = objectSchema(
            "thresholdMs" to integerProperty("Minimum duration in milliseconds to include (default 1000)."),
            "limit" to integerProperty("Maximum number of calls to return (default 20)."),
        ),
    ) { args ->
        val thresholdMs = args.longOrNull("thresholdMs") ?: 1000
        val limit = args.intOrNull("limit") ?: 20
        ok(findSlowCalls(calls(), thresholdMs, limit))
    }

    private fun getCallStatsTool() = McpTool(
        name = "get_call_stats",
        description = "Aggregate overview of captured traffic: volume, error rate, latency percentiles, and " +
            "breakdowns by host and status code.",
        inputSchema = objectSchema(),
    ) { _ ->
        ok(callStats(calls()))
    }

    private fun clearCallsTool() = McpTool(
        name = "clear_calls",
        description = "Clear the in-memory ring buffer of captured calls.",
        inputSchema = objectSchema(),
    ) { _ ->
        monitor().clear()
        ok(buildJsonObject { put("cleared", true) })
    }

    private fun replayCallTool(replay: suspend (CapturedCall) -> CapturedCall) = McpTool(
        name = "replay_call",
        description = "Re-fire a previously captured call by id, through the host app's real HTTP client, and " +
            "return the newly captured result — useful for checking whether a fix changed the outcome.",
        inputSchema = objectSchema("id" to stringProperty("The call id, from list_calls."), required = listOf("id")),
    ) { args ->
        val id = args.stringOrNull("id")
        val call = id?.let { wanted -> calls().find { it.id == wanted } }
        when {
            id == null -> err("Missing required argument \"id\".")
            call == null -> err("No call found with id \"$id\".")
            else -> ok(replay(call).toDetail())
        }
    }
}

private inline fun <reified T> ok(value: T): ToolResult = ToolResult(json.encodeToString(value))
private fun err(message: String): ToolResult = ToolResult(message, isError = true)

private fun JsonObject.stringOrNull(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull
private fun JsonObject.intOrNull(key: String): Int? = this[key]?.jsonPrimitive?.intOrNull
private fun JsonObject.longOrNull(key: String): Long? = this[key]?.jsonPrimitive?.longOrNull
private fun JsonObject.booleanOrNull(key: String): Boolean? = this[key]?.jsonPrimitive?.booleanOrNull

private fun objectSchema(vararg properties: Pair<String, JsonObject>, required: List<String> = emptyList()): JsonObject =
    buildJsonObject {
        put("type", "object")
        putJsonObject("properties") { properties.forEach { (name, schema) -> put(name, schema) } }
        if (required.isNotEmpty()) putJsonArray("required") { required.forEach { add(it) } }
    }

private fun stringProperty(description: String) = buildJsonObject {
    put("type", "string")
    put("description", description)
}

private fun integerProperty(description: String) = buildJsonObject {
    put("type", "integer")
    put("description", description)
}

private fun booleanProperty(description: String) = buildJsonObject {
    put("type", "boolean")
    put("description", description)
}
