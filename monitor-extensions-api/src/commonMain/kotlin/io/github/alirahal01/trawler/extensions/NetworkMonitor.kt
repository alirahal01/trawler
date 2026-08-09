package io.github.alirahal01.trawler.extensions

import io.github.alirahal01.trawler.core.CallStore
import io.github.alirahal01.trawler.core.CapturedCall
import io.github.alirahal01.trawler.core.InMemoryCallStore
import io.github.alirahal01.trawler.core.RedactionConfig
import kotlinx.coroutines.flow.StateFlow

/**
 * The instance-scoped facade a host app constructs explicitly and passes to
 * both the Ktor plugin installation and `NetworkMonitorUi(monitor)`. Never a
 * process-wide singleton — an app with multiple HttpClients constructs one
 * NetworkMonitor per client it wants to inspect independently (ADR-0001).
 */
class NetworkMonitor(
    val extensions: List<MonitorExtension> = emptyList(),
    private val redaction: RedactionConfig = RedactionConfig(),
    private val store: CallStore = InMemoryCallStore(),
) {
    fun observeCalls(): StateFlow<List<CapturedCall>> = store.observe()

    suspend fun capture(call: CapturedCall) {
        if (redaction.isIgnored(call.url)) return
        val redacted = redaction.apply(call)
        val transformed = extensions.fold(redacted) { acc, extension -> extension.onCapture(acc) }
        store.record(transformed)
    }

    suspend fun clear() {
        store.clear()
    }

    fun actionsFor(call: CapturedCall): List<CallAction> = extensions.flatMap { it.actions(call) }
}
