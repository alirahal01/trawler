package io.github.alirahal01.trawler.extensions.mcpserver

import io.github.alirahal01.trawler.extensions.NetworkMonitor
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The one true end-to-end check that the embedded Ktor CIO server actually
 * binds a real socket and speaks MCP over it — everything else in this
 * module tests [McpJsonRpc] as a pure function, without a server at all.
 */
class McpServerExtensionDesktopTest {

    private val port = 24319
    private lateinit var extension: McpServerExtension
    private val httpClient = HttpClient(CIO)

    @AfterTest
    fun tearDown() = runTest {
        extension.stop()
        httpClient.close()
    }

    @Test
    fun aRealHttpRequestReachesTheEmbeddedServerAndListsTools() = runTest {
        val monitor = NetworkMonitor()
        extension = McpServerExtension(monitor = { monitor }, port = port)
        extension.start()

        val response = httpClient.post("http://127.0.0.1:$port/mcp") {
            setBody("""{"jsonrpc":"2.0","id":1,"method":"tools/list"}""")
        }

        val tools = Json.parseToJsonElement(response.bodyAsText())
            .jsonObject["result"]!!.jsonObject["tools"]!!.jsonArray
            .map { it.jsonObject["name"]!!.jsonPrimitive.content }
        assertTrue("list_calls" in tools)
        assertTrue("find_concurrent_calls" in tools)
    }
}
