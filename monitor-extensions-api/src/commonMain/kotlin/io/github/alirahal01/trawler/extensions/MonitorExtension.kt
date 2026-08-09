package io.github.alirahal01.trawler.extensions

import androidx.compose.runtime.Composable
import io.github.alirahal01.trawler.core.CapturedCall

/**
 * A host-app-registered plugin point for [NetworkMonitor]. Implementations
 * are registered via the `NetworkMonitor(extensions = listOf(...))`
 * constructor — there is no separate registry or discovery mechanism.
 */
interface MonitorExtension {
    val id: String
    val label: String

    fun onCapture(call: CapturedCall): CapturedCall = call

    fun actions(call: CapturedCall): List<CallAction> = emptyList()

    val standalonePanel: (@Composable () -> Unit)? get() = null
}
