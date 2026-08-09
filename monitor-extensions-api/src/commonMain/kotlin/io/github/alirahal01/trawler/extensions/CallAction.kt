package io.github.alirahal01.trawler.extensions

/**
 * A single action an extension offers for a call — a label the viewer shows
 * and a callback it invokes and forgets about. Deliberately data-only: an
 * extension that needs to display something does so itself (e.g. via its own
 * [MonitorExtension.standalonePanel]), not by returning a typed result here.
 */
data class CallAction(
    val label: String,
    val invoke: suspend () -> Unit,
)
