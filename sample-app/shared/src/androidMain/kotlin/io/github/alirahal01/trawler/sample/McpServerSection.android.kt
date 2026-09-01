package io.github.alirahal01.trawler.sample

import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import io.github.alirahal01.trawler.extensions.NetworkMonitor
import io.github.alirahal01.trawler.extensions.mcpserver.McpServerExtension
import io.github.alirahal01.trawler.extensions.replay.ReplayDraft
import io.github.alirahal01.trawler.extensions.replay.ReplayExtension

@Composable
actual fun McpServerSection(monitor: NetworkMonitor, replay: ReplayExtension, modifier: Modifier) {
    val mcpServer = remember {
        McpServerExtension(
            monitor = { monitor },
            replay = { call ->
                replay.send(ReplayDraft.from(call))
                monitor.observeCalls().value.last()
            },
        )
    }
    OutlinedButton(onClick = { mcpServer.open() }, modifier = modifier) { Text("Open MCP Server") }
    mcpServer.standalonePanel.invoke()
}

actual val isMcpServerSupported: Boolean = true
