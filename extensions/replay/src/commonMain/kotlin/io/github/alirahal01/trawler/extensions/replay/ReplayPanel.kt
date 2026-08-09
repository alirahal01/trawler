package io.github.alirahal01.trawler.extensions.replay

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@Composable
internal fun ReplayPanel(
    draft: StateFlow<ReplayDraft?>,
    onDismiss: () -> Unit,
    onSend: suspend (ReplayDraft) -> Unit,
) {
    val current = draft.collectAsState().value ?: return
    var method by remember(current.originalCallId) { mutableStateOf(current.method) }
    var url by remember(current.originalCallId) { mutableStateOf(current.url) }
    var headersText by remember(current.originalCallId) { mutableStateOf(current.headers.toHeaderLines()) }
    var bodyText by remember(current.originalCallId) { mutableStateOf(current.body.orEmpty()) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Replay request") },
        text = {
            Column {
                OutlinedTextField(value = method, onValueChange = { method = it }, label = { Text("Method") })
                OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("URL") })
                OutlinedTextField(
                    value = headersText,
                    onValueChange = { headersText = it },
                    label = { Text("Headers (one per line: Key: Value)") },
                )
                OutlinedTextField(value = bodyText, onValueChange = { bodyText = it }, label = { Text("Body") })
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val edited = current.copy(
                    method = method,
                    url = url,
                    headers = parseHeaderLines(headersText),
                    body = bodyText.ifBlank { null },
                )
                scope.launch { onSend(edited) }
            }) { Text("Send") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
