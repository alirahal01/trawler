package io.github.alirahal01.trawler.extensions.curlexport

import io.github.alirahal01.trawler.core.CapturedCall
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CurlExportExtensionTest {

    @Test
    fun offersExactlyOneCopyAsCurlAction() {
        val extension = CurlExportExtension()
        val call = CapturedCall(id = "1", url = "https://example.com", method = "GET", startedAtEpochMillis = 0L)

        val actions = extension.actions(call)

        assertEquals(1, actions.size)
        assertEquals("Copy as cURL", actions.single().label)
    }

    @Test
    fun invokingTheActionStagesTheCallsCurlCommand() = runTest {
        val extension = CurlExportExtension()
        val call = CapturedCall(id = "1", url = "https://example.com", method = "GET", startedAtEpochMillis = 0L)

        extension.actions(call).single().invoke()

        assertEquals(call.toCurlCommand(), extension.command.value)
    }

    @Test
    fun dismissClearsTheStagedCommand() = runTest {
        val extension = CurlExportExtension()
        val call = CapturedCall(id = "1", url = "https://example.com", method = "GET", startedAtEpochMillis = 0L)
        extension.actions(call).single().invoke()

        extension.dismiss()

        assertNull(extension.command.value)
    }
}
