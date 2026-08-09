package io.github.alirahal01.trawler.sample

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.alirahal01.trawler.extensions.NetworkMonitor
import io.github.alirahal01.trawler.ktor.TrawlerMonitor
import io.github.alirahal01.trawler.ui.NetworkMonitorUi
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import kotlinx.coroutines.launch

private const val BASE_URL = "https://httpbin.org"

@Composable
fun SampleApp() {
    val monitor = remember { NetworkMonitor() }
    val client = remember { HttpClient { install(TrawlerMonitor) { this.monitor = monitor } } }
    var showMonitor by remember { mutableStateOf(false) }

    MaterialTheme {
        if (showMonitor) {
            Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
                TextButton(onClick = { showMonitor = false }) { Text("‹ Close monitor") }
                NetworkMonitorUi(monitor = monitor, modifier = Modifier.weight(1f))
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp)) {
                Text("Trawler Sample", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(16.dp))
                TestButton("Success (200)") { client.get("$BASE_URL/get") }
                TestButton("Error (500)") { client.get("$BASE_URL/status/500") }
                TestButton("Slow (3s delay)") { client.get("$BASE_URL/delay/3") }
                TestButton("Large body (500KB)") { client.get("$BASE_URL/bytes/500000") }
                TestButton("Non-JSON (HTML)") { client.get("$BASE_URL/html") }
                TestButton("Binary (PNG)") { client.get("$BASE_URL/image/png") }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { showMonitor = true },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Open Trawler Monitor") }
            }
        }
    }
}

@Composable
private fun TestButton(label: String, request: suspend () -> Unit) {
    val scope = rememberCoroutineScope()
    Button(
        onClick = { scope.launch { runCatching { request() } } },
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    ) { Text(label) }
}
