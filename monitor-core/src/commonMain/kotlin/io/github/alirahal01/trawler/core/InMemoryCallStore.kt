package io.github.alirahal01.trawler.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Default [CallStore]: an in-memory ring buffer, oldest call evicted first
 * once [capacity] is exceeded. Not resizable after construction.
 */
class InMemoryCallStore(
    override val capacity: Int = 200,
) : CallStore {

    init {
        require(capacity > 0) { "capacity must be positive, was $capacity" }
    }

    private val mutex = Mutex()
    private val calls = MutableStateFlow<List<CapturedCall>>(emptyList())

    override fun observe(): StateFlow<List<CapturedCall>> = calls.asStateFlow()

    override suspend fun record(call: CapturedCall) {
        mutex.withLock {
            val updated = calls.value + call
            calls.value = if (updated.size > capacity) updated.takeLast(capacity) else updated
        }
    }

    override suspend fun clear() {
        mutex.withLock {
            calls.value = emptyList()
        }
    }
}
