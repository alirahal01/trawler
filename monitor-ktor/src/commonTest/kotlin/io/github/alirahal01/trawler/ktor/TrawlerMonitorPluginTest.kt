package io.github.alirahal01.trawler.ktor

import io.github.alirahal01.trawler.core.CapturedCall
import io.github.alirahal01.trawler.extensions.MonitorExtension
import io.github.alirahal01.trawler.extensions.NetworkMonitor
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TrawlerMonitorPluginTest {

    @Test
    fun capturesMethodUrlHeadersStatusAndTiming() = runTest {
        val monitor = NetworkMonitor()
        val client = clientWith(monitor) {
            respond(
                content = "ok",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/plain"),
            )
        }

        client.get("https://example.com/ping")

        val call = monitor.observeCalls().value.single()
        assertEquals("https://example.com/ping", call.url)
        assertEquals("GET", call.method)
        assertEquals(200, call.status)
        assertTrue(call.durationMillis != null && call.durationMillis!! >= 0)
        assertTrue(call.responseHeaders[HttpHeaders.ContentType]?.contains("text/plain") == true)
    }

    @Test
    fun captureFailureNeverBreaksTheRealRequest() = runTest {
        val throwingExtension = object : MonitorExtension {
            override val id = "throwing"
            override val label = "Throwing"
            override fun onCapture(call: CapturedCall): CapturedCall = error("boom")
        }
        val monitor = NetworkMonitor(extensions = listOf(throwingExtension))
        val client = clientWith(monitor) { respond("ok", HttpStatusCode.OK) }

        val response = client.get("https://example.com/ping")

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(monitor.observeCalls().value.isEmpty())
    }

    private fun clientWith(
        monitor: NetworkMonitor,
        handler: suspend io.ktor.client.engine.mock.MockRequestHandleScope.(
            request: io.ktor.client.request.HttpRequestData,
        ) -> io.ktor.client.request.HttpResponseData,
    ): HttpClient {
        val engine = MockEngine(handler)
        return HttpClient(engine) {
            install(TrawlerMonitor) { this.monitor = monitor }
            expectSuccess = false
        }
    }
}
