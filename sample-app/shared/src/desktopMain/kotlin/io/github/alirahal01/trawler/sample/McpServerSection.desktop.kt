package io.github.alirahal01.trawler.sample

import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import io.github.alirahal01.trawler.extensions.NetworkMonitor
import io.github.alirahal01.trawler.extensions.mcpserver.McpServerExtension

@Composable
actual fun McpServerSection(monitor: NetworkMonitor, modifier: Modifier) {
    val mcpServer = remember { McpServerExtension(monitor = { monitor }) }
    OutlinedButton(onClick = { mcpServer.open() }, modifier = modifier) { Text("Open MCP Server") }
    mcpServer.standalonePanel.invoke()
}

actual val isMcpServerSupported: Boolean = true
