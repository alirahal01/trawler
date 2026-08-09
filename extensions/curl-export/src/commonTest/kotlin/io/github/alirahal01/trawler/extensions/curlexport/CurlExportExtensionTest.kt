package io.github.alirahal01.trawler.extensions.curlexport

import io.github.alirahal01.trawler.core.CapturedCall
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CurlExportExtensionTest {

    @Test
    fun offersExactlyOneCopyAsCurlAction() {
        val extension = CurlExportExtension(onCurlGenerated = { _, _ -> })
        val call = CapturedCall(id = "1", url = "https://example.com", method = "GET", startedAtEpochMillis = 0L)

        val actions = extension.actions(call)

        assertEquals(1, actions.size)
        assertEquals("Copy as cURL", actions.single().label)
    }

    @Test
    fun invokingTheActionCallsBackWithTheCallAndItsCurlCommand() = kotlinx.coroutines.test.runTest {
        var received: Pair<CapturedCall, String>? = null
        val extension = CurlExportExtension(onCurlGenerated = { call, command -> received = call to command })
        val call = CapturedCall(id = "1", url = "https://example.com", method = "GET", startedAtEpochMillis = 0L)

        extension.actions(call).single().invoke()

        assertEquals(call, received?.first)
        assertTrue(received?.second == call.toCurlCommand())
    }
}
