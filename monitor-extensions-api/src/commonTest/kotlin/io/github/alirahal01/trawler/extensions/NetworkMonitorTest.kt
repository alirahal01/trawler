package io.github.alirahal01.trawler.extensions

import io.github.alirahal01.trawler.core.CapturedCall
import io.github.alirahal01.trawler.core.RedactionConfig
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NetworkMonitorTest {

    private fun call(url: String = "https://example.com/path", headers: Map<String, List<String>> = emptyMap()) =
        CapturedCall(
            id = "1",
            url = url,
            method = "GET",
            requestHeaders = headers,
            startedAtEpochMillis = 0L,
        )

    @Test
    fun capturedCallsAreObservable() = runTest {
        val monitor = NetworkMonitor()

        monitor.capture(call())

        assertEquals(1, monitor.observeCalls().value.size)
    }

    @Test
    fun redactionRunsBeforeExtensionsSeeTheCall() = runTest {
        val seenByExtension = mutableListOf<CapturedCall>()
        val recordingExtension = object : MonitorExtension {
            override val id = "recorder"
            override val label = "Recorder"
            override fun onCapture(call: CapturedCall): CapturedCall {
                seenByExtension += call
                return call
            }
        }
        val monitor = NetworkMonitor(
            extensions = listOf(recordingExtension),
            redaction = RedactionConfig(),
        )

        monitor.capture(call(headers = mapOf("Authorization" to listOf("Bearer secret"))))

        assertEquals(listOf("[REDACTED]"), seenByExtension.single().requestHeaders["Authorization"])
    }

    @Test
    fun callsToIgnoredHostsAreNeverStored() = runTest {
        val monitor = NetworkMonitor(redaction = RedactionConfig(ignoredHosts = listOf("analytics.example.com")))

        monitor.capture(call(url = "https://analytics.example.com/track"))

        assertTrue(monitor.observeCalls().value.isEmpty())
    }

    @Test
    fun actionsForDelegatesToEachExtension() {
        val extensionA = object : MonitorExtension {
            override val id = "a"
            override val label = "A"
            override fun actions(call: CapturedCall) = listOf(CallAction("Action A") {})
        }
        val extensionB = object : MonitorExtension {
            override val id = "b"
            override val label = "B"
            override fun actions(call: CapturedCall) = listOf(CallAction("Action B") {})
        }
        val monitor = NetworkMonitor(extensions = listOf(extensionA, extensionB))

        val actions = monitor.actionsFor(call())

        assertEquals(listOf("Action A", "Action B"), actions.map { it.label })
    }
}
