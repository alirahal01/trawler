package io.github.alirahal01.trawler.extensions.curlexport

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.StateFlow

@Composable
internal fun CurlCommandDialog(
    command: StateFlow<String?>,
    onDismiss: () -> Unit,
    onShare: ((String) -> Unit)?,
) {
    val current = command.collectAsState().value ?: return
    val clipboard = LocalClipboardManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("cURL command") },
        text = {
            Text(
                text = current,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.heightIn(max = 240.dp).verticalScroll(rememberScrollState()),
            )
        },
        confirmButton = {
            TextButton(onClick = { clipboard.setText(AnnotatedString(current)); onDismiss() }) { Text("Copy") }
        },
        dismissButton = {
            if (onShare != null) {
                TextButton(onClick = { onShare(current) }) { Text("Share") }
            }
        },
    )
}
