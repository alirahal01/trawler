package io.github.alirahal01.trawler.sample

import androidx.compose.runtime.Composable

/**
 * A platform share action, or null where the platform has no equivalent to
 * an OS share sheet (Desktop/JVM). There's no cross-platform primitive for
 * this in Compose (unlike the clipboard, which CurlExportExtension already
 * uses directly) — real per-platform behavior belongs behind an expect/actual
 * seam in the app, not in the library.
 */
@Composable
expect fun rememberShareAction(): ((String) -> Unit)?
