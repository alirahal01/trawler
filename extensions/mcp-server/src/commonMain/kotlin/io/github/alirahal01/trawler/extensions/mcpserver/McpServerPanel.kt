package io.github.alirahal01.trawler.extensions.mcpserver

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@Composable
internal fun McpServerPanel(
    isOpen: StateFlow<Boolean>,
    isRunning: StateFlow<Boolean>,
    serverUrl: String,
    onStart: suspend () -> Unit,
    onStop: suspend () -> Unit,
    onClose: () -> Unit,
) {
    val open by isOpen.collectAsState()
    if (!open) return

    val running by isRunning.collectAsState()
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("MCP Server") },
        text = {
            Column {
                Text(if (running) "Running" else "Stopped", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "An AI coding agent can connect to $serverUrl and query recent " +
                        "calls, duplicate/concurrency diagnostics, and latency stats. " +
                        "Only reachable from this machine.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { clipboard.setText(AnnotatedString(serverUrl)) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Copy URL") }
            }
        },
        confirmButton = {
            if (running) {
                TextButton(onClick = { scope.launch { onStop() } }) { Text("Stop") }
            } else {
                Button(onClick = { scope.launch { onStart() } }) { Text("Start") }
            }
        },
        dismissButton = {
            TextButton(onClick = onClose) { Text("Close") }
        },
    )
}
