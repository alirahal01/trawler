package io.github.alirahal01.trawler.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
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
        TextButton(onClick = onBack, modifier = Modifier.padding(start = 8.dp, top = 8.dp)) { Text("‹ Back") }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MethodBadge(call.method)
            Column {
                Text(hostOf(call.url), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    call.url,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (actions.isNotEmpty()) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                actions.forEach { action ->
                    OutlinedButton(onClick = { scope.launch { action.invoke() } }) { Text(action.label) }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
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
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 6.dp),
    )
}

@Composable
private fun CodeBlock(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(8.dp))
            .padding(12.dp),
    )
}

@Composable
private fun HeadersTab(call: CapturedCall) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item { SectionLabel("Request Headers") }
        if (call.requestHeaders.isEmpty()) {
            item { CodeBlock("(none)") }
        } else {
            item { CodeBlock(call.requestHeaders.entries.joinToString("\n") { (k, v) -> "$k: ${v.joinToString("; ")}" }) }
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
        item { SectionLabel("Response Headers") }
        if (call.responseHeaders.isEmpty()) {
            item { CodeBlock("(none)") }
        } else {
            item { CodeBlock(call.responseHeaders.entries.joinToString("\n") { (k, v) -> "$k: ${v.joinToString("; ")}" }) }
        }
    }
}

@Composable
private fun BodyTab(call: CapturedCall) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item { SectionLabel("Request Body") }
        item { CodeBlock(call.requestBody.toDisplayText()) }
        item { Spacer(modifier = Modifier.height(16.dp)) }
        item { SectionLabel("Response Body") }
        item { CodeBlock(call.responseBody.toDisplayText()) }
    }
}

@Composable
private fun TimingTab(call: CapturedCall) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        TimingRow("Status", call.status?.toString() ?: "—")
        TimingRow("Started at (epoch ms)", call.startedAtEpochMillis.toString())
        TimingRow("Duration", call.durationMillis?.let { "${it}ms" } ?: "—")
        call.error?.let { TimingRow("Error", it) }
    }
}

@Composable
private fun TimingRow(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "$label:",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
    }
}

private fun ByteArray?.toDisplayText(): String = when {
    this == null -> "(no body)"
    isEmpty() -> "(empty)"
    else -> decodeToString()
}

private val previewDetailCall = CapturedCall(
    id = "1",
    url = "https://api.coingecko.com/api/v3/coins/markets?vs_currency=usd&ids=bitcoin",
    method = "GET",
    requestHeaders = mapOf("Accept" to listOf("application/json")),
    responseHeaders = mapOf("Content-Type" to listOf("application/json; charset=utf-8")),
    responseBody = """[{"id":"bitcoin","current_price":77195.0}]""".encodeToByteArray(),
    status = 200,
    startedAtEpochMillis = 0L,
    durationMillis = 312,
)

@Preview
@Composable
private fun CallDetailScreenPreview() {
    MaterialTheme {
        CallDetailScreen(call = previewDetailCall, actions = emptyList(), onBack = {})
    }
}
