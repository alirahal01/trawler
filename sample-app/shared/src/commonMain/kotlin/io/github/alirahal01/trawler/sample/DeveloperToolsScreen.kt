package io.github.alirahal01.trawler.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * One screen for everything that fires requests for the sake of exercising the Trawler
 * monitor, rather than for real portfolio data — kept off the wallet dashboard so that
 * screen reads like an actual app instead of a developer test harness.
 */
@Composable
fun DeveloperToolsScreen(
    onBack: () -> Unit,
    onOpenPresetPlayground: () -> Unit,
    testScenariosContent: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TextButton(onClick = onBack, modifier = Modifier.padding(start = 8.dp, top = 8.dp)) { Text("‹ Close tools") }
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text("Developer Tools", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "Fire requests to inspect in the Trawler monitor",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ToolsSection(
                title = "Preset Playground",
                description = "Fire saved endpoint and payload presets on demand.",
            ) {
                OutlinedButton(onClick = onOpenPresetPlayground, modifier = Modifier.fillMaxWidth()) {
                    Text("Open Preset Playground")
                }
            }
            ToolsSection(
                title = "Test scenarios",
                description = "Trigger specific status codes, delays, and payload shapes.",
                content = testScenariosContent,
            )
        }
    }
}

@Composable
private fun ToolsSection(
    title: String,
    description: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(modifier = Modifier.padding(top = 12.dp)) { content() }
        }
    }
}

@Preview
@Composable
private fun DeveloperToolsScreenPreview() {
    MaterialTheme(colorScheme = TrawlerColorScheme) {
        DeveloperToolsScreen(
            onBack = {},
            onOpenPresetPlayground = {},
            testScenariosContent = {
                OutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("Success (200)") }
                OutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("Error (500)") }
                OutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("Slow (3s delay)") }
            },
        )
    }
}
