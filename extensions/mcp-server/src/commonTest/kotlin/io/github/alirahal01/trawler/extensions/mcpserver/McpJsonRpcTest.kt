package io.github.alirahal01.trawler.extensions.mcpserver

import io.github.alirahal01.trawler.core.CapturedCall
import io.github.alirahal01.trawler.extensions.NetworkMonitor
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class McpJsonRpcTest {

    private val json = Json

    private suspend fun registryWith(vararg calls: CapturedCall): McpToolRegistry {
        val monitor = NetworkMonitor()
        calls.forEach { monitor.capture(it) }
        return McpToolRegistry(monitor = { monitor })
    }

    private fun sampleCall(id: String = "1") = CapturedCall(
        id = id,
        url = "https://api.example.com/users",
        method = "GET",
        status = 200,
        startedAtEpochMillis = 0,
        durationMillis = 42,
    )

    @Test
    fun notificationsGetNoResponse() = runTest {
        val registry = registryWith()

        val response = McpJsonRpc.handleRequest(
            """{"jsonrpc":"2.0","method":"notifications/initialized"}""",
            registry,
        )

        assertNull(response)
    }

    @Test
    fun initializeEchoesProtocolVersionAndNamesTheServer() = runTest {
        val registry = registryWith()

        val response = McpJsonRpc.handleRequest(
            """{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18"}}""",
            registry,
        )!!

        val result = json.parseToJsonElement(response).jsonObject["result"]!!.jsonObject
        assertEquals("2025-06-18", result["protocolVersion"]!!.jsonPrimitive.contentOrNull)
        assertEquals("trawler", result["serverInfo"]!!.jsonObject["name"]!!.jsonPrimitive.contentOrNull)
    }

    @Test
    fun toolsListIncludesEveryReadOnlyTool() = runTest {
        val registry = registryWith()

        val response = McpJsonRpc.handleRequest("""{"jsonrpc":"2.0","id":1,"method":"tools/list"}""", registry)!!

        val names = json.parseToJsonElement(response).jsonObject["result"]!!.jsonObject["tools"]!!
            .jsonArray.map { it.jsonObject["name"]!!.jsonPrimitive.contentOrNull }
        assertEquals(
            setOf(
                "list_calls",
                "get_call",
                "find_duplicate_calls",
                "find_concurrent_calls",
                "find_slow_calls",
                "get_call_stats",
                "clear_calls",
            ),
            names.toSet(),
        )
    }

    @Test
    fun toolsListOmitsReplayCallWhenNoReplayCallbackIsSupplied() = runTest {
        val names = json.parseToJsonElement(
            McpJsonRpc.handleRequest(
                """{"jsonrpc":"2.0","id":1,"method":"tools/list"}""",
                registryWith(),
            )!!,
        ).jsonObject["result"]!!.jsonObject["tools"]!!.jsonArray.map { it.jsonObject["name"]!!.jsonPrimitive.contentOrNull }

        assertTrue("replay_call" !in names)
    }

    @Test
    fun toolsCallListCallsReturnsCapturedCalls() = runTest {
        val registry = registryWith(sampleCall("1"), sampleCall("2"))

        val response = McpJsonRpc.handleRequest(
            """{"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"list_calls","arguments":{}}}""",
            registry,
        )!!

        val result = json.parseToJsonElement(response).jsonObject["result"]!!.jsonObject
        val text = result["content"]!!.jsonArray.single().jsonObject["text"]!!.jsonPrimitive.contentOrNull!!
        assertEquals(false, result["isError"]!!.jsonPrimitive.boolean, text)
        val calls = json.parseToJsonElement(text).jsonArray
        assertEquals(2, calls.size)
    }

    @Test
    fun toolsCallUnknownToolIsReportedAsAToolError() = runTest {
        val response = McpJsonRpc.handleRequest(
            """{"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"not_a_real_tool","arguments":{}}}""",
            registryWith(),
        )!!

        val result = json.parseToJsonElement(response).jsonObject["result"]!!.jsonObject
        assertEquals(true, result["isError"]!!.jsonPrimitive.boolean)
    }

    @Test
    fun unknownMethodIsAJsonRpcMethodNotFoundError() = runTest {
        val response = McpJsonRpc.handleRequest(
            """{"jsonrpc":"2.0","id":1,"method":"not/a/real/method"}""",
            registryWith(),
        )!!

        val error = json.parseToJsonElement(response).jsonObject["error"]!!.jsonObject
        assertEquals(-32601, error["code"]!!.jsonPrimitive.int)
    }

    @Test
    fun malformedJsonIsAParseError() = runTest {
        val response = McpJsonRpc.handleRequest("not json at all", registryWith())!!

        val error = json.parseToJsonElement(response).jsonObject["error"]!!.jsonObject
        assertEquals(-32700, error["code"]!!.jsonPrimitive.int)
    }
}
