package io.github.alirahal01.trawler.sample.android

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import io.github.alirahal01.trawler.sample.SampleApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SampleApp() }
    }
}
