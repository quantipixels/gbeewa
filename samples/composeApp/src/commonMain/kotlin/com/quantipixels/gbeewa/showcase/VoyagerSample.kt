package com.quantipixels.gbeewa.showcase

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.CurrentScreen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.quantipixels.gbeewa.ResultStore

private class VoyagerHome(
    private val onBack: () -> Unit,
    private val onNameChanged: (String) -> Unit,
    private val onColorSelected: (SelectionResultKey, String) -> Unit,
) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        ResultHome(
            frameworkName = "Voyager",
            onBack = onBack,
            openNameEditor = { initialName ->
                navigator.push(VoyagerNameEditor(initialName, onNameChanged))
            },
            openColorPicker = { resultKey ->
                navigator.push(
                    VoyagerColorPicker { color ->
                        onColorSelected(resultKey, color)
                    },
                )
            },
        )
    }
}

private class VoyagerNameEditor(
    private val initialName: String,
    private val onNameChanged: (String) -> Unit,
) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        NameEditor(
            frameworkName = "Voyager",
            initialName = initialName,
            onBack = navigator::pop,
        ) { name ->
            onNameChanged(name)
            navigator.pop()
        }
    }
}

private class VoyagerColorPicker(
    private val onColorSelected: (String) -> Unit,
) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        ColorPicker(
            frameworkName = "Voyager",
            onBack = navigator::pop,
        ) { color ->
            onColorSelected(color)
            navigator.pop()
        }
    }
}

@Composable
internal fun VoyagerSample(
    resultStore: ResultStore,
    onBackToFrameworkSelection: () -> Unit,
) {
    val currentOnBack by rememberUpdatedState(onBackToFrameworkSelection)
    val home = remember(resultStore) {
        VoyagerHome(
            onBack = { currentOnBack() },
            onNameChanged = { name -> resultStore.put(EditedNameKey, name) },
            onColorSelected = resultStore::put,
        )
    }
    Navigator(home) {
        CurrentScreen()
    }
}
