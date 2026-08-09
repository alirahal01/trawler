package io.github.alirahal01.trawler.sample.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.github.alirahal01.trawler.sample.SampleApp

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Trawler Sample") {
        SampleApp()
    }
}
