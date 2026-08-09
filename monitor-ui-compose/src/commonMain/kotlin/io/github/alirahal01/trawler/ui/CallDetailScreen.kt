package io.github.alirahal01.trawler.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
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
import io.github.alirahal01.trawler.core.CapturedCall
import io.github.alirahal01.trawler.extensions.CallAction
import kotlinx.coroutines.launch

private enum class DetailTab(val label: String) { HEADERS("Headers"), BODY("Body"), TIMING("Timing") }

@Composable
internal fun CallDetailScreen(
    call: CapturedCall,
    actions: List<CallAction>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTab by remember(call.id) { mutableStateOf(DetailTab.HEADERS) }
    val scope = rememberCoroutineScope()

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(onClick = onBack) { Text("‹ Back") }
            Text("${call.method} ${hostOf(call.url)}", style = MaterialTheme.typography.titleMedium)
        }
        if (actions.isNotEmpty()) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                actions.forEach { action ->
                    OutlinedButton(onClick = { scope.launch { action.invoke() } }) { Text(action.label) }
                }
            }
        }
        SecondaryTabRow(selectedTabIndex = selectedTab.ordinal) {
            DetailTab.entries.forEach { tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    text = { Text(tab.label) },
                )
            }
        }
        when (selectedTab) {
            DetailTab.HEADERS -> HeadersTab(call)
            DetailTab.BODY -> BodyTab(call)
            DetailTab.TIMING -> TimingTab(call)
        }
    }
}

@Composable
private fun HeadersTab(call: CapturedCall) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item { Text("Request Headers", style = MaterialTheme.typography.titleSmall) }
        if (call.requestHeaders.isEmpty()) item { Text("(none)") }
        items(call.requestHeaders.toList()) { (key, values) -> Text("$key: ${values.joinToString("; ")}") }
        item { Spacer(modifier = Modifier.height(16.dp)) }
        item { Text("Response Headers", style = MaterialTheme.typography.titleSmall) }
        if (call.responseHeaders.isEmpty()) item { Text("(none)") }
        items(call.responseHeaders.toList()) { (key, values) -> Text("$key: ${values.joinToString("; ")}") }
    }
}

@Composable
private fun BodyTab(call: CapturedCall) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item { Text("Request Body", style = MaterialTheme.typography.titleSmall) }
        item { Text(call.requestBody.toDisplayText()) }
        item { Spacer(modifier = Modifier.height(16.dp)) }
        item { Text("Response Body", style = MaterialTheme.typography.titleSmall) }
        item { Text(call.responseBody.toDisplayText()) }
    }
}

@Composable
private fun TimingTab(call: CapturedCall) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Status: ${call.status ?: "—"}")
        Text("Started at (epoch ms): ${call.startedAtEpochMillis}")
        Text("Duration: ${call.durationMillis?.let { "${it}ms" } ?: "—"}")
        call.error?.let { Text("Error: $it") }
    }
}

private fun ByteArray?.toDisplayText(): String = when {
    this == null -> "(no body)"
    isEmpty() -> "(empty)"
    else -> decodeToString()
}
