package io.github.alirahal01.trawler.extensions.mcpserver

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

private const val SERVER_NAME = "trawler"
private const val SERVER_VERSION = "0.1.2"
private const val DEFAULT_PROTOCOL_VERSION = "2025-06-18"
private val json = Json { ignoreUnknownKeys = true }

/**
 * Minimal JSON-RPC 2.0 + MCP method dispatch covering exactly what this
 * surface needs: `initialize`, `notifications/initialized`, `tools/list`,
 * `tools/call`. No resources, no prompts, no SSE — see ADR-0004 for why.
 * Pure string (request) in, string-or-null (response) out: `null` means the
 * request was a notification and the HTTP layer should reply with an empty
 * body, not omit a response entirely. This makes the whole dispatch
 * testable without ever starting a server.
 */
object McpJsonRpc {
    suspend fun handleRequest(rawJson: String, registry: McpToolRegistry): String? {
        val request = runCatching { json.parseToJsonElement(rawJson).jsonObject }.getOrNull()
            ?: return errorResponse(JsonNull, -32700, "Parse error")

        val id = request["id"] ?: return null // notification: no response expected
        val method = request["method"]?.jsonPrimitive?.contentOrNull
            ?: return errorResponse(id, -32600, "Invalid request: missing method")
        val params = request["params"] as? JsonObject ?: JsonObject(emptyMap())

        return when (method) {
            "initialize" -> successResponse(id, initializeResult(params))
            "tools/list" -> successResponse(id, toolsListResult(registry))
            "tools/call" -> successResponse(id, toolsCallResult(params, registry))
            else -> errorResponse(id, -32601, "Method not found: $method")
        }
    }
}

private fun initializeResult(params: JsonObject): JsonObject = buildJsonObject {
    put("protocolVersion", params["protocolVersion"]?.jsonPrimitive?.contentOrNull ?: DEFAULT_PROTOCOL_VERSION)
    putJsonObject("capabilities") { putJsonObject("tools") {} }
    putJsonObject("serverInfo") {
        put("name", SERVER_NAME)
        put("version", SERVER_VERSION)
    }
}

private fun toolsListResult(registry: McpToolRegistry): JsonObject = buildJsonObject {
    putJsonArray("tools") {
        registry.tools.forEach { tool ->
            addJsonObject {
                put("name", tool.name)
                put("description", tool.description)
                put("inputSchema", tool.inputSchema)
            }
        }
    }
}

private suspend fun toolsCallResult(params: JsonObject, registry: McpToolRegistry): JsonObject {
    val name = params["name"]?.jsonPrimitive?.contentOrNull
    val arguments = params["arguments"] as? JsonObject ?: JsonObject(emptyMap())
    val tool = registry.tools.find { it.name == name }
    val result = when {
        tool == null -> ToolResult("Unknown tool: $name", isError = true)
        else -> runCatching { tool.handler(arguments) }
            .getOrElse { ToolResult("Tool \"$name\" failed: ${it.message}", isError = true) }
    }
    return toolResultEnvelope(result)
}

private fun toolResultEnvelope(result: ToolResult): JsonObject = buildJsonObject {
    putJsonArray("content") {
        addJsonObject {
            put("type", "text")
            put("text", result.text)
        }
    }
    put("isError", result.isError)
}

private fun successResponse(id: JsonElement, result: JsonObject): String = buildJsonObject {
    put("jsonrpc", "2.0")
    put("id", id)
    put("result", result)
}.toString()

private fun errorResponse(id: JsonElement, code: Int, message: String): String = buildJsonObject {
    put("jsonrpc", "2.0")
    put("id", id)
    putJsonObject("error") {
        put("code", code)
        put("message", message)
    }
}.toString()
