package io.github.alirahal01.trawler.extensions.presetplayground

import androidx.compose.runtime.Composable
import io.github.alirahal01.trawler.extensions.MonitorExtension
import io.ktor.client.HttpClient
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.http.HttpMethod

/**
 * Fires any registered [Endpoint] with a saved [EndpointPreset], independent
 * of whether that endpoint was ever captured live. Not tied to a specific
 * captured call — unlike replay/curl-export, this has no per-call actions;
 * the whole feature lives behind [standalonePanel]. Firing through the same
 * client the host app already installed TrawlerMonitor on means fired calls
 * get captured exactly like any other request — no separate bookkeeping.
 */
class PresetPlaygroundExtension(
    private val client: HttpClient,
    val endpoints: List<Endpoint>,
) : MonitorExtension {
    override val id = "preset-playground"
    override val label = "Preset Playground"

    override val standalonePanel: (@Composable () -> Unit) = {
        PresetPlaygroundPanel(endpoints = endpoints, onFire = ::fire)
    }

    suspend fun fire(endpoint: Endpoint, preset: EndpointPreset?) {
        client.request(endpoint.url) {
            method = HttpMethod.parse(endpoint.method)
            preset?.headers?.forEach { (key, value) -> headers.append(key, value) }
            preset?.body?.let { setBody(it) }
        }
    }
}
