package io.github.alirahal01.trawler.extensions.curlexport

import io.github.alirahal01.trawler.core.CapturedCall
import io.github.alirahal01.trawler.extensions.CallAction
import io.github.alirahal01.trawler.extensions.MonitorExtension

/**
 * The first-party extension built to validate the MonitorExtension contract
 * before anything bigger. A single action per call that hands the host app
 * a ready-to-run curl command — clipboard/share behavior is a host-app
 * concern (no baked-in trigger mechanism), so this just calls back with the
 * generated string rather than copying anything itself.
 */
class CurlExportExtension(
    private val onCurlGenerated: (CapturedCall, String) -> Unit,
) : MonitorExtension {
    override val id = "curl-export"
    override val label = "cURL Export"

    override fun actions(call: CapturedCall): List<CallAction> = listOf(
        CallAction("Copy as cURL") { onCurlGenerated(call, call.toCurlCommand()) },
    )
}
