package io.github.alirahal01.trawler.sample

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.alirahal01.trawler.extensions.NetworkMonitor
import io.github.alirahal01.trawler.extensions.replay.ReplayExtension

// extensions/mcp-server has no iOS target (ADR-0004: ktor-server-cio is
// JVM-only) — nothing to show here.
@Composable
actual fun McpServerSection(monitor: NetworkMonitor, replay: ReplayExtension, modifier: Modifier) {}

actual val isMcpServerSupported: Boolean = false
