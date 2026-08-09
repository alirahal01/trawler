package io.github.alirahal01.trawler.core

import kotlinx.coroutines.flow.StateFlow

/**
 * A fixed-capacity ring buffer of [CapturedCall]s. Writes may land on any
 * thread the Ktor engine uses; reads happen via [observe] on the UI thread.
 */
interface CallStore {
    val capacity: Int

    suspend fun record(call: CapturedCall)

    fun observe(): StateFlow<List<CapturedCall>>

    suspend fun clear()
}
