package com.quantipixels.gbeewa.showcase

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.BackHandler
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.pushNew
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.resume
import com.quantipixels.gbeewa.ResultStore

internal sealed interface DecomposeRoute {
    data object Home : DecomposeRoute
    data class NameEditor(val initialName: String) : DecomposeRoute
    data class ColorPicker(val receiverId: String) : DecomposeRoute
}

internal sealed interface DecomposeChild {
    class Home(
        componentContext: ComponentContext,
    ) : DecomposeChild, ComponentContext by componentContext

    class NameEditor(
        componentContext: ComponentContext,
        val initialName: String,
    ) : DecomposeChild, ComponentContext by componentContext

    class ColorPicker(
        componentContext: ComponentContext,
        val resultKey: SelectionResultKey,
    ) : DecomposeChild, ComponentContext by componentContext
}

internal class DecomposeRoot(
    componentContext: ComponentContext,
) : ComponentContext by componentContext {
    private val navigation = StackNavigation<DecomposeRoute>()

    val stack: Value<ChildStack<DecomposeRoute, DecomposeChild>> = childStack(
        source = navigation,
        serializer = null,
        initialConfiguration = DecomposeRoute.Home,
        handleBackButton = false,
        childFactory = { route, childContext ->
            when (route) {
                DecomposeRoute.Home -> DecomposeChild.Home(childContext)
                is DecomposeRoute.NameEditor -> DecomposeChild.NameEditor(
                    componentContext = childContext,
                    initialName = route.initialName,
                )
                is DecomposeRoute.ColorPicker -> DecomposeChild.ColorPicker(
                    componentContext = childContext,
                    resultKey = SelectionResultKey(route.receiverId),
                )
            }
        },
    )

    fun openNameEditor(initialName: String): Unit =
        navigation.pushNew(DecomposeRoute.NameEditor(initialName))

    fun openColorPicker(resultKey: SelectionResultKey): Unit =
        navigation.pushNew(DecomposeRoute.ColorPicker(resultKey.receiverId))

    fun pop(): Unit = navigation.pop()
}

@Composable
@OptIn(ExperimentalComposeUiApi::class)
internal fun DecomposeSample(
    resultStore: ResultStore,
    onBackToFrameworkSelection: () -> Unit,
) {
    val lifecycle = remember {
        LifecycleRegistry().apply { resume() }
    }
    val root = remember(lifecycle) {
        DecomposeRoot(DefaultComponentContext(lifecycle = lifecycle))
    }
    DisposableEffect(lifecycle) {
        onDispose(lifecycle::destroy)
    }

    Children(stack = root.stack) { child ->
        val route = child.instance
        BackHandler(enabled = route !is DecomposeChild.Home, onBack = root::pop)
        when (route) {
            is DecomposeChild.Home -> ResultHome(
                frameworkName = "Decompose",
                onBack = onBackToFrameworkSelection,
                openNameEditor = root::openNameEditor,
                openColorPicker = root::openColorPicker,
            )
            is DecomposeChild.NameEditor -> NameEditor(
                frameworkName = "Decompose",
                initialName = route.initialName,
                onBack = root::pop,
            ) { name ->
                resultStore.put(EditedNameKey, name)
                root.pop()
            }
            is DecomposeChild.ColorPicker -> {
                ColorPicker(
                    frameworkName = "Decompose",
                    onBack = root::pop,
                ) { color ->
                    resultStore.put(route.resultKey, color)
                    root.pop()
                }
            }
        }
    }
}
