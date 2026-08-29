package io.github.alirahal01.trawler.extensions.mcpserver

/**
 * The embedded HTTP server that speaks MCP JSON-RPC on `127.0.0.1:[port]`,
 * POST `/mcp` only. JVM-only (desktop + android share one `actual` via the
 * `jvmAndAndroidMain` source set) — see ADR-0004 for why there's no iOS
 * implementation and no configurable bind address.
 */
internal expect class McpHttpServer(port: Int, registry: McpToolRegistry) {
    suspend fun start()
    suspend fun stop()
}
