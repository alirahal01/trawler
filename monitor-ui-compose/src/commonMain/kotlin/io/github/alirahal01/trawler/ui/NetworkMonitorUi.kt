package io.github.alirahal01.trawler.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.github.alirahal01.trawler.extensions.NetworkMonitor
import kotlinx.coroutines.launch

/**
 * The Trawler viewer: a filterable call list that drills into a
 * headers/body/timing detail screen. Host apps decide how this gets shown
 * (debug menu, shake gesture, a dedicated screen, ...) — this composable has
 * no baked-in trigger mechanism of its own.
 */
@Composable
fun NetworkMonitorUi(monitor: NetworkMonitor, modifier: Modifier = Modifier) {
    val calls by monitor.observeCalls().collectAsState()
    var selectedCallId by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val selectedCall = calls.find { it.id == selectedCallId }

    if (selectedCall != null) {
        CallDetailScreen(
            call = selectedCall,
            actions = monitor.actionsFor(selectedCall),
            onBack = { selectedCallId = null },
            modifier = modifier,
        )
    } else {
        CallListScreen(
            calls = calls,
            onCallSelected = { selectedCallId = it.id },
            onClear = { scope.launch { monitor.clear() } },
            modifier = modifier,
        )
    }

    // Each panel decides for itself whether it has anything to show (e.g.
    // ReplayExtension's returns early when its draft is null) — calling every
    // registered extension's panel unconditionally, on top of whichever
    // screen is showing, is what makes a CallAction like "Replay" or "Copy
    // as cURL" actually pop up a dialog instead of silently doing nothing.
    monitor.extensions.forEach { extension -> extension.standalonePanel?.invoke() }
}
