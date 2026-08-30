package io.github.alirahal01.trawler.extensions.mcpserver

import androidx.compose.runtime.Composable
import io.github.alirahal01.trawler.core.CapturedCall
import io.github.alirahal01.trawler.extensions.MonitorExtension
import io.github.alirahal01.trawler.extensions.NetworkMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Exposes the calls a [NetworkMonitor] has captured to an MCP client (an AI
 * coding agent) over a localhost-only HTTP JSON-RPC endpoint — see
 * ADR-0004. [monitor] is a provider, not a constructed [NetworkMonitor], for
 * the same construction-order reason `ReplayExtension`/
 * `PresetPlaygroundExtension` take one: this extension has to exist before
 * the `NetworkMonitor` it's registered on can be constructed.
 *
 * The server never starts on its own — [start] must be called explicitly
 * (typically from [standalonePanel]'s Start button) — and it only ever
 * binds to 127.0.0.1, never a host-configurable address: this is a
 * debugging surface with access to (redacted, but real) request traffic,
 * and it must never be exposed by accident. [replay], if supplied,
 * additionally registers a `replay_call` tool so the agent can re-fire a
 * flagged call itself, the same way `ReplayExtension` re-fires one for a
 * human.
 *
 * Like `PresetPlaygroundExtension`, this has no per-call actions to hang an
 * "open" trigger off of, so the host app calls [open] directly from its own
 * UI; [standalonePanel] is self-gating and shows nothing until then.
 */
class McpServerExtension(
    monitor: () -> NetworkMonitor,
    val port: Int = 4319,
    replay: (suspend (CapturedCall) -> CapturedCall)? = null,
) : MonitorExtension {
    override val id = "mcp-server"
    override val label = "MCP Server"

    val serverUrl: String = "http://127.0.0.1:$port/mcp"

    private val registry = McpToolRegistry(monitor, replay)
    private val server = McpHttpServer(port, registry)

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _isOpen = MutableStateFlow(false)
    val isOpen: StateFlow<Boolean> = _isOpen.asStateFlow()

    fun open() {
        _isOpen.value = true
    }

    fun close() {
        _isOpen.value = false
    }

    suspend fun start() {
        if (_isRunning.value) return
        server.start()
        _isRunning.value = true
    }

    suspend fun stop() {
        if (!_isRunning.value) return
        server.stop()
        _isRunning.value = false
    }

    override val standalonePanel: (@Composable () -> Unit) = {
        McpServerPanel(
            isOpen = isOpen,
            isRunning = isRunning,
            serverUrl = serverUrl,
            onStart = ::start,
            onStop = ::stop,
            onClose = ::close,
        )
    }
}
