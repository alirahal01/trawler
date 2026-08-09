package io.github.alirahal01.trawler.core

import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class InMemoryCallStoreTest {

    private fun call(id: String) = CapturedCall(
        id = id,
        url = "https://example.com/$id",
        method = "GET",
        startedAtEpochMillis = 0L,
    )

    @Test
    fun recordAppendsCalls() = runTest {
        val store = InMemoryCallStore(capacity = 10)

        store.record(call("1"))
        store.record(call("2"))

        assertEquals(listOf(call("1"), call("2")), store.observe().value)
    }

    @Test
    fun recordEvictsOldestOnceCapacityExceeded() = runTest {
        val store = InMemoryCallStore(capacity = 2)

        store.record(call("1"))
        store.record(call("2"))
        store.record(call("3"))

        assertEquals(listOf(call("2"), call("3")), store.observe().value)
    }

    @Test
    fun clearEmptiesTheStore() = runTest {
        val store = InMemoryCallStore(capacity = 10)
        store.record(call("1"))

        store.clear()

        assertTrue(store.observe().value.isEmpty())
    }

    @Test
    fun constructorRejectsNonPositiveCapacity() {
        assertFailsWith<IllegalArgumentException> { InMemoryCallStore(capacity = 0) }
    }

    @Test
    fun concurrentRecordsAreAllRetainedUpToCapacity() = runTest {
        val store = InMemoryCallStore(capacity = 500)

        val jobs = (1..200).map { i -> async { store.record(call(i.toString())) } }
        jobs.forEach { it.await() }

        assertEquals(200, store.observe().value.size)
    }
}
