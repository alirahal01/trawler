package io.github.alirahal01.trawler.extensions.presetplayground

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PresetPlaygroundExtensionTest {

    @Test
    fun firingWithAPresetSendsItsHeadersAndBody() = runTest {
        var captured: HttpRequestData? = null
        val client = HttpClient(MockEngine { request -> captured = request; respond("ok", HttpStatusCode.OK) })
        val extension = PresetPlaygroundExtension(client = { client }, endpoints = emptyList())
        val endpoint = Endpoint(label = "Login", method = "POST", url = "https://example.com/login")
        val preset = EndpointPreset(label = "Valid", headers = listOf("X-Custom" to "value"), body = "hello")

        extension.fire(endpoint, preset)

        assertEquals("POST", captured?.method?.value)
        assertEquals("https://example.com/login", captured?.url?.toString())
        assertEquals("value", captured?.headers?.get("X-Custom"))
        val body = captured?.body as? OutgoingContent.ByteArrayContent
        assertEquals("hello", body?.bytes()?.decodeToString())
    }

    @Test
    fun firingWithNoPresetSendsAPlainRequest() = runTest {
        var captured: HttpRequestData? = null
        val client = HttpClient(MockEngine { request -> captured = request; respond("ok", HttpStatusCode.OK) })
        val extension = PresetPlaygroundExtension(client = { client }, endpoints = emptyList())
        val endpoint = Endpoint(label = "Ping", method = "GET", url = "https://example.com/ping")

        extension.fire(endpoint, null)

        assertEquals("GET", captured?.method?.value)
        assertNull(captured?.headers?.get("X-Custom"))
    }

    @Test
    fun hasNoPerCallActions() {
        val extension = PresetPlaygroundExtension(
            client = { HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) }) },
            endpoints = emptyList(),
        )

        assertEquals(0, extension.actions(io.github.alirahal01.trawler.core.CapturedCall(
            id = "1", url = "https://example.com", method = "GET", startedAtEpochMillis = 0L,
        )).size)
    }
}
