package io.github.alirahal01.trawler.ktor

import io.github.alirahal01.trawler.core.CapturedCall
import io.github.alirahal01.trawler.extensions.NetworkMonitor
import io.ktor.client.plugins.api.SendingRequest
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.observer.ResponseObserver
import io.ktor.http.Headers
import io.ktor.http.content.OutgoingContent
import io.ktor.util.AttributeKey
import io.ktor.utils.io.InternalAPI
import io.ktor.utils.io.toByteArray
import kotlinx.coroutines.CancellationException
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private val CallIdKey = AttributeKey<String>("TrawlerCallId")
private val StartedAtKey = AttributeKey<Long>("TrawlerStartedAt")
private val RequestBodyKey = AttributeKey<ByteArray>("TrawlerRequestBody")

class TrawlerMonitorConfig {
    var monitor: NetworkMonitor? = null
}

/**
 * Captures method/url/headers/status/timing/body for every request on the
 * client it's installed on. Response bodies are teed via Ktor's own
 * [ResponseObserver] (engine-agnostic — it operates on [io.ktor.utils.io.ByteReadChannel],
 * so OkHttp/Darwin/CIO all go through the same code path here). Request
 * bodies are captured only when they're already an in-memory
 * [OutgoingContent.ByteArrayContent] — `bytes()` is a pure, repeatable read
 * so it needs no teeing. Streamed request bodies (large file uploads) are
 * deliberately left uncaptured in v1 rather than risking the real upload
 * stream; every side effect here is wrapped so a bug in this plugin can
 * never fail, delay, or alter the host app's real HTTP call (ADR-0003).
 */
@OptIn(ExperimentalTime::class, InternalAPI::class)
val TrawlerMonitor = createClientPlugin("TrawlerMonitor", ::TrawlerMonitorConfig) {
    val networkMonitor = pluginConfig.monitor
        ?: error(
            "TrawlerMonitor requires a NetworkMonitor instance: " +
                "install(TrawlerMonitor) { monitor = myNetworkMonitor }",
        )

    onRequest { request, _ ->
        isolatingCaptureFailures {
            val startedAt = Clock.System.now().toEpochMilliseconds()
            request.attributes.put(StartedAtKey, startedAt)
            request.attributes.put(CallIdKey, "$startedAt-${Random.nextInt()}")
        }
    }

    on(SendingRequest) { request, content ->
        isolatingCaptureFailures {
            if (content is OutgoingContent.ByteArrayContent) {
                request.attributes.put(RequestBodyKey, content.bytes())
            }
        }
    }

    val responseObserver = ResponseObserver.prepare {
        onResponse { response ->
            isolatingCaptureFailures {
                val attributes = response.call.attributes
                val startedAt = attributes.getOrNull(StartedAtKey) ?: return@isolatingCaptureFailures
                val id = attributes.getOrNull(CallIdKey) ?: return@isolatingCaptureFailures
                val now = Clock.System.now().toEpochMilliseconds()

                networkMonitor.capture(
                    CapturedCall(
                        id = id,
                        url = response.call.request.url.toString(),
                        method = response.call.request.method.value,
                        requestHeaders = response.call.request.headers.toHeaderMap(),
                        responseHeaders = response.headers.toHeaderMap(),
                        requestBody = attributes.getOrNull(RequestBodyKey),
                        responseBody = response.rawContent.toByteArray(),
                        status = response.status.value,
                        startedAtEpochMillis = startedAt,
                        durationMillis = now - startedAt,
                    ),
                )
            }
        }
    }
    ResponseObserver.install(responseObserver, client)
}

/**
 * Cancellation is a legitimate signal, not a capture failure — it's always
 * rethrown. Everything else is swallowed: see the ADR-0003 note above.
 */
private suspend inline fun isolatingCaptureFailures(block: suspend () -> Unit) {
    try {
        block()
    } catch (c: CancellationException) {
        throw c
    } catch (t: Throwable) {
        // Intentionally swallowed.
    }
}

private fun Headers.toHeaderMap(): Map<String, List<String>> = entries().associate { it.key to it.value }
