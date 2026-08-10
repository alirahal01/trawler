package io.github.alirahal01.trawler.sample

import androidx.compose.runtime.Composable

// No equivalent to an OS share sheet on Desktop/JVM — the dialog's own Copy
// button (clipboard) is the only sharing mechanism there.
@Composable
actual fun rememberShareAction(): ((String) -> Unit)? = null
