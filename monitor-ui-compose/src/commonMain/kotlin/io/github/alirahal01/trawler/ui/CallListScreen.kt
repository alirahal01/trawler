package io.github.alirahal01.trawler.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.alirahal01.trawler.core.CapturedCall

@Composable
internal fun CallListScreen(
    calls: List<CapturedCall>,
    onCallSelected: (CapturedCall) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var filter by remember { mutableStateOf("") }
    val filtered = remember(calls, filter) {
        if (filter.isBlank()) {
            calls
        } else {
            calls.filter {
                it.method.contains(filter, ignoreCase = true) ||
                    it.url.contains(filter, ignoreCase = true) ||
                    it.status?.toString()?.contains(filter) == true
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Trawler (${calls.size})", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = onClear) { Text("Clear") }
        }
        OutlinedTextField(
            value = filter,
            onValueChange = { filter = it },
            label = { Text("Filter by method, url, or status") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        )
        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(if (calls.isEmpty()) "No calls captured yet" else "No calls match \"$filter\"")
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(filtered.asReversed(), key = { it.id }) { call ->
                    CallRow(call = call, onClick = { onCallSelected(call) })
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun CallRow(call: CapturedCall, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text("${call.method} ${hostOf(call.url)}") },
        supportingContent = { Text(call.url, maxLines = 1) },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                Text(call.status?.toString() ?: call.error?.let { "ERR" } ?: "…", color = statusColor(call))
                call.durationMillis?.let { Text("${it}ms") }
            }
        },
    )
}

@Composable
private fun statusColor(call: CapturedCall): Color {
    val status = call.status
    return when {
        call.error != null -> MaterialTheme.colorScheme.error
        status != null && status >= 400 -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }
}
