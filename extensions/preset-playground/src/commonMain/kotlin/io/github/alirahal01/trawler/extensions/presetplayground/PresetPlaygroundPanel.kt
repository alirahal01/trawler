package io.github.alirahal01.trawler.extensions.presetplayground

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch

@Composable
internal fun PresetPlaygroundPanel(
    endpoints: List<Endpoint>,
    onFire: suspend (Endpoint, EndpointPreset?) -> Unit,
    onClose: () -> Unit,
) {
    val scope = rememberCoroutineScope()

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                TextButton(onClick = onClose) { Text("‹ Close") }
                Text(
                    "Preset Playground",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp, 0.dp, 16.dp, 8.dp),
                )
                if (endpoints.isEmpty()) {
                    Text("No endpoints registered.", modifier = Modifier.padding(16.dp))
                } else {
                    LazyColumn {
                        items(endpoints) { endpoint ->
                            Column {
                                Text(
                                    "${endpoint.method} ${endpoint.label}",
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.padding(16.dp, 12.dp, 16.dp, 4.dp),
                                )
                                if (endpoint.presets.isEmpty()) {
                                    ListItem(
                                        modifier = Modifier.fillMaxWidth()
                                            .clickable { scope.launch { onFire(endpoint, null) } },
                                        headlineContent = { Text("Fire with no preset") },
                                    )
                                } else {
                                    endpoint.presets.forEach { preset ->
                                        ListItem(
                                            modifier = Modifier.fillMaxWidth()
                                                .clickable { scope.launch { onFire(endpoint, preset) } },
                                            headlineContent = { Text(preset.label) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
