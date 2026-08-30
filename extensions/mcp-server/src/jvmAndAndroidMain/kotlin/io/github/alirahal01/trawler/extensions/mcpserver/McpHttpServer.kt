package io.github.alirahal01.trawler.extensions.mcpserver

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondText
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal actual class McpHttpServer actual constructor(
    private val port: Int,
    private val registry: McpToolRegistry,
) {
    private var engine: EmbeddedServer<*, *>? = null

    actual suspend fun start() {
        if (engine != null) return
        val server = embeddedServer(CIO, port = port, host = "127.0.0.1") {
            routing {
                post("/mcp") {
                    val body = call.receiveText()
                    when (val response = McpJsonRpc.handleRequest(body, registry)) {
                        null -> call.respondText("", ContentType.Application.Json, HttpStatusCode.Accepted)
                        else -> call.respondText(response, ContentType.Application.Json)
                    }
                }
            }
        }
        withContext(Dispatchers.IO) { server.start(wait = false) }
        engine = server
    }

    actual suspend fun stop() {
        val server = engine ?: return
        withContext(Dispatchers.IO) { server.stop(gracePeriodMillis = 200, timeoutMillis = 1000) }
        engine = null
    }
}
