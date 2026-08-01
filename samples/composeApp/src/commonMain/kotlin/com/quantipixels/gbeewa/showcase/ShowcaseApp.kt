package com.quantipixels.gbeewa.showcase

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.unit.dp
import com.quantipixels.gbeewa.ResultConsumer
import com.quantipixels.gbeewa.ResultStore
import com.quantipixels.gbeewa.LocalResultConsumer

private enum class Integration(
    val label: String,
) {
    Decompose("Decompose"),
    Navigation2("Navigation 2"),
    Voyager("Voyager"),
}

@Composable
fun ShowcaseApp() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            ShowcaseContent()
        }
    }
}

@Composable
private fun ShowcaseContent() {
    var integration by remember { mutableStateOf<Integration?>(null) }

    when (val selected = integration) {
        null -> FrameworkMenu(openFramework = { integration = it })
        else -> FrameworkContent(
            integration = selected,
            onBackToFrameworkSelection = { integration = null },
        )
    }
}

@Composable
private fun FrameworkMenu(openFramework: (Integration) -> Unit) {
    ShowcaseScaffold(title = "Gbéewá") {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Select a navigation framework. Each flow returns typed data before it pops.")
            Integration.entries.forEach { integration ->
                Button(
                    onClick = { openFramework(integration) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(integration.label)
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalComposeUiApi::class)
private fun FrameworkContent(
    integration: Integration,
    onBackToFrameworkSelection: () -> Unit,
) {
    BackHandler(onBack = onBackToFrameworkSelection)
    val resultStore = remember(integration) { ResultStore() }
    val resultConsumer = remember(resultStore) { ResultConsumer(resultStore) }
    CompositionLocalProvider(LocalResultConsumer provides resultConsumer) {
        when (integration) {
            Integration.Decompose -> DecomposeSample(resultStore, onBackToFrameworkSelection)
            Integration.Navigation2 -> Navigation2Sample(resultStore, onBackToFrameworkSelection)
            Integration.Voyager -> VoyagerSample(resultStore, onBackToFrameworkSelection)
        }
    }
}
