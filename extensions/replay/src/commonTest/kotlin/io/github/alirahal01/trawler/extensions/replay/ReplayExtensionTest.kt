package io.github.alirahal01.trawler.extensions.replay

import io.github.alirahal01.trawler.core.CapturedCall
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

class ReplayExtensionTest {

    @Test
    fun replayActionStagesADraftFromTheCall() = runTest {
        val extension = ReplayExtension(HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) }))
        val call = CapturedCall(id = "1", url = "https://example.com", method = "GET", startedAtEpochMillis = 0L)

        extension.actions(call).single().invoke()

        assertEquals("1", extension.draft.value?.originalCallId)
    }

    @Test
    fun sendFiresTheEditedRequestAndClearsTheDraft() = runTest {
        var capturedRequest: HttpRequestData? = null
        val client = HttpClient(
            MockEngine { request ->
                capturedRequest = request
                respond("ok", HttpStatusCode.OK)
            },
        )
        val extension = ReplayExtension(client)
        val call = CapturedCall(id = "1", url = "https://example.com", method = "GET", startedAtEpochMillis = 0L)
        extension.actions(call).single().invoke()

        val edited = extension.draft.value!!.copy(
            method = "POST",
            url = "https://example.com/edited",
            headers = listOf("X-Custom" to "value"),
            body = "hello",
        )
        extension.send(edited)

        assertEquals("POST", capturedRequest?.method?.value)
        assertEquals("https://example.com/edited", capturedRequest?.url?.toString())
        assertEquals("value", capturedRequest?.headers?.get("X-Custom"))
        val bodyContent = capturedRequest?.body as? OutgoingContent.ByteArrayContent
        assertEquals("hello", bodyContent?.bytes()?.decodeToString())
        assertNull(extension.draft.value)
    }
}
