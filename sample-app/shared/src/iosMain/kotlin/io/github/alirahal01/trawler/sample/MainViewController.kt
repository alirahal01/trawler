package io.github.alirahal01.trawler.sample

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

/** The entry point the iOS host app's Swift code calls to get Trawler's Compose UI. */
fun MainViewController(): UIViewController = ComposeUIViewController { SampleApp() }
