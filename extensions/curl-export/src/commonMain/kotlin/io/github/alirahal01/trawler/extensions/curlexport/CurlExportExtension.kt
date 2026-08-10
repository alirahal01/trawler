package io.github.alirahal01.trawler.extensions.curlexport

import androidx.compose.runtime.Composable
import io.github.alirahal01.trawler.core.CapturedCall
import io.github.alirahal01.trawler.extensions.CallAction
import io.github.alirahal01.trawler.extensions.MonitorExtension
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The first-party extension built to validate the MonitorExtension contract
 * before anything bigger. "Copy as cURL" stages the generated command;
 * [standalonePanel] shows it in a dialog with a Copy button wired to
 * Compose's own cross-platform clipboard (no host wiring needed for that
 * part — it's a platform primitive Compose already abstracts, not a
 * trigger-mechanism choice). Sharing it further (Slack, Mail, ...) has no
 * such cross-platform primitive, so [onShare] is optional and host-supplied;
 * the Share button only appears when one is given.
 */
class CurlExportExtension(private val onShare: ((String) -> Unit)? = null) : MonitorExtension {
    override val id = "curl-export"
    override val label = "cURL Export"

    private val _command = MutableStateFlow<String?>(null)
    val command: StateFlow<String?> = _command.asStateFlow()

    override fun actions(call: CapturedCall): List<CallAction> = listOf(
        CallAction("Copy as cURL") { _command.value = call.toCurlCommand() },
    )

    override val standalonePanel: (@Composable () -> Unit) = {
        CurlCommandDialog(command = command, onDismiss = ::dismiss, onShare = onShare)
    }

    fun dismiss() {
        _command.value = null
    }
}
