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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.alirahal01.trawler.extensions.NetworkMonitor
import io.github.alirahal01.trawler.extensions.curlexport.CurlExportExtension
import io.github.alirahal01.trawler.extensions.presetplayground.Endpoint
import io.github.alirahal01.trawler.extensions.presetplayground.EndpointPreset
import io.github.alirahal01.trawler.extensions.presetplayground.PresetPlaygroundExtension
import io.github.alirahal01.trawler.extensions.replay.ReplayExtension
import io.github.alirahal01.trawler.ktor.TrawlerMonitor
import io.github.alirahal01.trawler.ui.NetworkMonitorUi
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import kotlinx.coroutines.launch

private const val BASE_URL = "https://httpbin.org"

private val TrawlerColorScheme = lightColorScheme(
    primary = Color(0xFF00695C),
    onPrimary = Color.White,
    secondary = Color(0xFF00897B),
    tertiary = Color(0xFFEF6C00),
)

// Holds the HttpClient reference so replay/preset-playground (constructed
// before the client, which itself needs the fully-built NetworkMonitor those
// extensions belong to) can defer reading it until they actually fire a
// request — see ReplayExtension's kdoc for why this ordering is circular.
private class ClientHolder {
    lateinit var client: HttpClient
}

@Composable
fun SampleApp() {
    val clientHolder = remember { ClientHolder() }
    val shareAction = rememberShareAction()
    val presetEndpoints = remember {
        listOf(
            Endpoint(label = "httpbin GET", method = "GET", url = "$BASE_URL/get"),
            Endpoint(
                label = "httpbin POST",
                method = "POST",
                url = "$BASE_URL/post",
                presets = listOf(
                    EndpointPreset(label = "Empty JSON", body = "{}"),
                    EndpointPreset(label = "Sample payload", body = """{"name":"Trawler","version":"0.1.1"}"""),
                ),
            ),
        )
    }
    val curlExport = remember { CurlExportExtension(onShare = shareAction) }
    val replay = remember { ReplayExtension(client = { clientHolder.client }) }
    val presetPlayground = remember { PresetPlaygroundExtension(client = { clientHolder.client }, presetEndpoints) }
    val monitor = remember { NetworkMonitor(extensions = listOf(curlExport, replay, presetPlayground)) }
    val client = remember {
        HttpClient { install(TrawlerMonitor) { this.monitor = monitor } }.also { clientHolder.client = it }
    }
    var showMonitor by remember { mutableStateOf(false) }

    MaterialTheme(colorScheme = TrawlerColorScheme) {
        if (showMonitor) {
            Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
                TextButton(onClick = { showMonitor = false }) { Text("‹ Close monitor") }
                NetworkMonitorUi(monitor = monitor, modifier = Modifier.weight(1f))
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp)) {
                Text("Trawler Sample", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    "Fire a request, then open the monitor to inspect it.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(16.dp))
                TestButton("Success (200)") { client.get("$BASE_URL/get") }
                TestButton("Error (500)") { client.get("$BASE_URL/status/500") }
                TestButton("Slow (3s delay)") { client.get("$BASE_URL/delay/3") }
                TestButton("Large body (500KB)") { client.get("$BASE_URL/bytes/500000") }
                TestButton("Non-JSON (HTML)") { client.get("$BASE_URL/html") }
                TestButton("Binary (PNG)") { client.get("$BASE_URL/image/png") }
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { showMonitor = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Open Trawler Monitor")
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = { presetPlayground.open() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Open Preset Playground")
                }
            }
            // NetworkMonitorUi already renders every registered extension's
            // standalonePanel (including this one) while it's mounted — only
            // render it here too, since "Open Preset Playground" is reachable
            // from this screen, not just from inside the monitor.
            presetPlayground.standalonePanel.invoke()
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
