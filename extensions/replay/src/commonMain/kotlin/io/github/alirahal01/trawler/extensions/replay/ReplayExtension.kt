package io.github.alirahal01.trawler.extensions.replay

import androidx.compose.runtime.Composable
import io.github.alirahal01.trawler.core.CapturedCall
import io.github.alirahal01.trawler.extensions.CallAction
import io.github.alirahal01.trawler.extensions.MonitorExtension
import io.ktor.client.HttpClient
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.http.HttpMethod
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Edits headers/body on a captured call and refires it through the same
 * client. The "Replay" action only stages a [ReplayDraft]; the actual edit
 * UI is [standalonePanel] (Compose), and firing the request is [send] —
 * split out so both can be exercised without a Composable test harness.
 *
 * [client] is a provider, not a constructed [HttpClient], since the client
 * this extension fires through is usually the very client that installs
 * TrawlerMonitor — and TrawlerMonitor's config needs this extension's owning
 * NetworkMonitor to exist first. A provider breaks that construction-order
 * cycle: nothing here needs a real client until [send] actually runs, well
 * after everything is wired up.
 */
class ReplayExtension(private val client: () -> HttpClient) : MonitorExtension {
    override val id = "replay"
    override val label = "Replay"

    private val _draft = MutableStateFlow<ReplayDraft?>(null)
    val draft: StateFlow<ReplayDraft?> = _draft.asStateFlow()

    override fun actions(call: CapturedCall): List<CallAction> = listOf(
        CallAction("Replay") { _draft.value = ReplayDraft.from(call) },
    )

    override val standalonePanel: (@Composable () -> Unit) = {
        ReplayPanel(draft = draft, onDismiss = ::dismiss, onSend = ::send)
    }

    fun dismiss() {
        _draft.value = null
    }

    suspend fun send(draft: ReplayDraft) {
        client().request(draft.url) {
            method = HttpMethod.parse(draft.method)
            draft.headers.forEach { (key, value) -> headers.append(key, value) }
            draft.body?.let { setBody(it) }
        }
        dismiss()
    }
}
