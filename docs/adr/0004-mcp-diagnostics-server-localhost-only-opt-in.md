# ADR-0004: The MCP diagnostics server is HTTP, localhost-only, opt-in, and hand-rolled

## Status

Accepted.

## Context

An AI coding agent (Claude Code, Claude Desktop, or similar) needs a way to
ask a *running* host app diagnostic questions about its recent network
traffic — "are there duplicate calls," "is there a race condition," "what's
slow" — the same data a human would otherwise have to read out of the
Compose viewer and paste back into a chat.

MCP (Model Context Protocol) is the natural fit for that surface, but its
usual transport — the client spawns the server as a stdio subprocess — does
not apply here: Trawler runs *embedded inside a long-lived host app
process* (an Android app, an iOS app, a desktop app), not as a standalone
process an MCP client controls the lifecycle of.

## Decision

**HTTP transport, not stdio.** The host app itself starts an HTTP server;
the agent connects to it as a URL-based MCP server, the same way it would
point at any other local MCP endpoint.

**`127.0.0.1` only, not configurable.** This server hands out (already
redacted, but still real) request/response bodies over a network socket.
The bind address is hard-coded in `McpHttpServer`, not a constructor
parameter — there is deliberately no way for a host app to accidentally
expose it on `0.0.0.0` or a LAN-reachable interface.

**Never starts automatically.** `McpServerExtension.start()` must be called
explicitly (in the sample app, from the panel's Start button). Registering
the extension only wires it up; it doesn't open a port.

**Hand-rolled minimal JSON-RPC/MCP dispatch, not the official
`kotlin-sdk`.** `McpJsonRpc` implements exactly four methods —
`initialize`, `notifications/initialized`, `tools/list`, `tools/call` — as
one pure, synchronous string-in/string-out function, built on
`kotlinx-serialization-json` and served over one Ktor CIO route. No
resources, no prompts, no SSE streaming: the MCP Streamable-HTTP transport
permits a single synchronous JSON response for exchanges that don't need
server-initiated push, which covers this entire tool surface. This keeps
the new dependency footprint to `ktor-server-core` + `ktor-server-cio` —
already in the Ktor family `monitor-ktor` depends on for the client side —
instead of pulling in the official SDK's own transport stack, matching the
"lean footprint, no framework magic" position in
[docs/LEAN_FOOTPRINT.md](../LEAN_FOOTPRINT.md).

**Desktop + Android only, no iOS.** `ktor-server-cio` publishes a JVM
variant only, so `extensions/mcp-server` has no iOS targets — `android` and
`jvm("desktop")` share one `actual McpHttpServer` implementation through an
intermediate `jvmAndAndroidMain` source set, since both compile to JVM
bytecode. An iOS-native implementation is a separate, larger lift (a
different HTTP server story entirely), left for later the same way
LEAN_FOOTPRINT.md deferred a persistence layer rather than build it
speculatively.

## Trade-off

This does not implement the full MCP spec. Any future need for
server-initiated messages (sampling, resource subscriptions, progress
notifications) means revisiting this decision, not just adding a method to
the `when` in `McpJsonRpc`. Accepted because the only thing exposed today is
a fixed, known set of read-only(-ish) diagnostic tools — `list_calls`,
`get_call`, `find_duplicate_calls`, `find_concurrent_calls`,
`find_slow_calls`, `get_call_stats`, `clear_calls`, and an optional
`replay_call` — none of which need anything past a synchronous
request/response.
