package io.github.alirahal01.trawler.sample

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.alirahal01.trawler.extensions.NetworkMonitor
import io.github.alirahal01.trawler.extensions.replay.ReplayExtension

/**
 * Renders an "Open MCP Server" button and its control panel on platforms
 * `extensions/mcp-server` supports (Android, Desktop). A no-op on iOS,
 * where that module has no target — see ADR-0004. Kept out of
 * NetworkMonitor's own `extensions` list on purpose: McpServerExtension
 * only ever reads calls, so it doesn't need onCapture/actions.
 *
 * [replay] wires up the `replay_call` MCP tool: re-firing goes through the
 * same [ReplayExtension] the "Replay" call action already uses, so a
 * refired call gets captured exactly like any other request.
 */
@Composable
expect fun McpServerSection(monitor: NetworkMonitor, replay: ReplayExtension, modifier: Modifier = Modifier)

/** True on Android/Desktop; false on iOS, where extensions/mcp-server has no target. */
expect val isMcpServerSupported: Boolean
