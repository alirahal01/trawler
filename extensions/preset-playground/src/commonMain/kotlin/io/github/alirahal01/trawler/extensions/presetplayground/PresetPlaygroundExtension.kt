package io.github.alirahal01.trawler.extensions.presetplayground

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import io.github.alirahal01.trawler.extensions.MonitorExtension
import io.ktor.client.HttpClient
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.http.HttpMethod
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Fires any registered [Endpoint] with a saved [EndpointPreset], independent
 * of whether that endpoint was ever captured live. Not tied to a specific
 * captured call — unlike replay/curl-export, this has no per-call actions,
 * so there's no CallAction to hang an "open" trigger off of; the host app
 * calls [open] directly from whatever UI element it wants (a menu item, a
 * tab, ...). [standalonePanel] is self-gating like every other extension's —
 * safe for NetworkMonitorUi to render unconditionally — and shows nothing
 * until [open] has been called. Firing through the same client the host app
 * already installed TrawlerMonitor on means fired calls get captured exactly
 * like any other request — no separate bookkeeping.
 *
 * [client] is a provider, not a constructed [HttpClient]: it breaks the
 * construction-order cycle with the NetworkMonitor this extension is
 * registered on (that NetworkMonitor's TrawlerMonitor plugin config needs a
 * NetworkMonitor to exist first, which needs this extension to exist first —
 * a provider means nothing here needs a real client until [fire] runs, well
 * after everything is wired up).
 */
class PresetPlaygroundExtension(
    private val client: () -> HttpClient,
    val endpoints: List<Endpoint>,
) : MonitorExtension {
    override val id = "preset-playground"
    override val label = "Preset Playground"

    private val _isOpen = MutableStateFlow(false)
    val isOpen: StateFlow<Boolean> = _isOpen.asStateFlow()

    fun open() {
        _isOpen.value = true
    }

    fun close() {
        _isOpen.value = false
    }

    override val standalonePanel: (@Composable () -> Unit) = {
        val open by isOpen.collectAsState()
        if (open) {
            PresetPlaygroundPanel(endpoints = endpoints, onFire = ::fire, onClose = ::close)
        }
    }

    suspend fun fire(endpoint: Endpoint, preset: EndpointPreset?) {
        client().request(endpoint.url) {
            method = HttpMethod.parse(endpoint.method)
            preset?.headers?.forEach { (key, value) -> headers.append(key, value) }
            preset?.body?.let { setBody(it) }
        }
    }
}
