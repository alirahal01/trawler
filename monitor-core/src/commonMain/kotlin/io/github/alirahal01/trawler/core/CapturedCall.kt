package io.github.alirahal01.trawler.core

import kotlinx.serialization.Serializable

/**
 * A single request/response pair, already redacted at capture time — nothing
 * upstream of [CallStore] ever sees the raw, unredacted header or body values.
 */
@Serializable
data class CapturedCall(
    val id: String,
    val url: String,
    val method: String,
    val requestHeaders: Map<String, List<String>> = emptyMap(),
    val responseHeaders: Map<String, List<String>> = emptyMap(),
    val requestBody: ByteArray? = null,
    val responseBody: ByteArray? = null,
    val status: Int? = null,
    val startedAtEpochMillis: Long,
    val durationMillis: Long? = null,
    val error: String? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CapturedCall) return false
        return id == other.id &&
            url == other.url &&
            method == other.method &&
            requestHeaders == other.requestHeaders &&
            responseHeaders == other.responseHeaders &&
            (requestBody?.contentEquals(other.requestBody) ?: (other.requestBody == null)) &&
            (responseBody?.contentEquals(other.responseBody) ?: (other.responseBody == null)) &&
            status == other.status &&
            startedAtEpochMillis == other.startedAtEpochMillis &&
            durationMillis == other.durationMillis &&
            error == other.error
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + url.hashCode()
        result = 31 * result + method.hashCode()
        result = 31 * result + requestHeaders.hashCode()
        result = 31 * result + responseHeaders.hashCode()
        result = 31 * result + (requestBody?.contentHashCode() ?: 0)
        result = 31 * result + (responseBody?.contentHashCode() ?: 0)
        result = 31 * result + (status ?: 0)
        result = 31 * result + startedAtEpochMillis.hashCode()
        result = 31 * result + (durationMillis?.hashCode() ?: 0)
        result = 31 * result + (error?.hashCode() ?: 0)
        return result
    }
}
