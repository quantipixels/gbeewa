package com.quantipixels.gbeewa.showcase

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.quantipixels.gbeewa.ResultStore
import kotlinx.serialization.Serializable

@Serializable
private data object Navigation2Home

@Serializable
private data class Navigation2NameEditor(val initialName: String)

@Serializable
private data class Navigation2ColorPicker(val receiverId: String)

@Composable
internal fun Navigation2Sample(
    resultStore: ResultStore,
    onBackToFrameworkSelection: () -> Unit,
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Navigation2Home) {
        composable<Navigation2Home> {
            ResultHome(
                frameworkName = "Navigation 2",
                onBack = onBackToFrameworkSelection,
                openNameEditor = { initialName ->
                    navController.navigate(Navigation2NameEditor(initialName))
                },
                openColorPicker = { resultKey ->
                    navController.navigate(Navigation2ColorPicker(resultKey.receiverId))
                },
            )
        }
        composable<Navigation2NameEditor> { backStackEntry ->
            val route = backStackEntry.toRoute<Navigation2NameEditor>()
            NameEditor(
                frameworkName = "Navigation 2",
                initialName = route.initialName,
                onBack = { navController.popBackStack() },
            ) { name ->
                resultStore.put(EditedNameKey, name)
                navController.popBackStack()
            }
        }
        composable<Navigation2ColorPicker> { backStackEntry ->
            val route = backStackEntry.toRoute<Navigation2ColorPicker>()
            val resultKey = SelectionResultKey(route.receiverId)
            ColorPicker(
                frameworkName = "Navigation 2",
                onBack = { navController.popBackStack() },
            ) { color ->
                resultStore.put(resultKey, color)
                navController.popBackStack()
            }
        }
    }
}
