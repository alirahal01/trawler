package io.github.alirahal01.trawler.sample

import androidx.compose.runtime.Composable
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication

@Composable
actual fun rememberShareAction(): ((String) -> Unit)? = { text ->
    val activityController = UIActivityViewController(activityItems = listOf(text), applicationActivities = null)
    UIApplication.sharedApplication.keyWindow?.rootViewController
        ?.presentViewController(activityController, animated = true, completion = null)
}
