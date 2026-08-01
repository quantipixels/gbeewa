package com.quantipixels.gbeewa.showcase

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.quantipixels.gbeewa.ResultKey
import com.quantipixels.gbeewa.rememberSaveableResultAsState

internal data object EditedNameKey : ResultKey<String>

internal data class SelectionResultKey(
    val receiverId: String,
) : ResultKey<String>

@Composable
internal fun ResultHome(
    frameworkName: String,
    onBack: () -> Unit,
    openNameEditor: (String) -> Unit,
    openColorPicker: (SelectionResultKey) -> Unit,
) {
    val primaryColorKey = remember { SelectionResultKey("primary-color") }
    val accentColorKey = remember { SelectionResultKey("accent-color") }
    val name by rememberSaveableResultAsState(EditedNameKey, initialValue = { "Ada" })
    val primaryColor by rememberSaveableResultAsState(
        primaryColorKey,
        initialValue = { "Blue" },
    )
    val accentColor by rememberSaveableResultAsState(
        accentColorKey,
        initialValue = { "Orange" },
    )

    ShowcaseScaffold(title = frameworkName, onBack = onBack) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Name: $name")
            Text("Primary color: $primaryColor")
            Text("Accent color: $accentColor")
            Button(onClick = { openNameEditor(name) }, modifier = Modifier.fillMaxWidth()) {
                Text("Edit name")
            }
            Button(
                onClick = { openColorPicker(primaryColorKey) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Select primary color")
            }
            Button(
                onClick = { openColorPicker(accentColorKey) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Select accent color")
            }
        }
    }
}

@Composable
internal fun NameEditor(
    frameworkName: String,
    initialName: String,
    onBack: () -> Unit,
    onSuccess: (String) -> Unit,
) {
    var value by remember(initialName) { mutableStateOf(initialName) }

    ShowcaseScaffold(title = "$frameworkName · Edit name", onBack = onBack) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = { onSuccess(value) },
                enabled = value.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save and go back")
            }
        }
    }
}

@Composable
internal fun ColorPicker(
    frameworkName: String,
    onBack: () -> Unit,
    onColorSelected: (String) -> Unit,
) {
    ShowcaseScaffold(title = "$frameworkName · Select color", onBack = onBack) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf("Blue", "Green", "Orange").forEach { color ->
                Button(
                    onClick = { onColorSelected(color) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(color)
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun ShowcaseScaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = BackArrowIcon,
                                contentDescription = "Back",
                            )
                        }
                    }
                },
            )
        },
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).padding(24.dp)) {
            content()
        }
    }
}

private val BackArrowIcon: ImageVector = ImageVector.Builder(
    name = "AutoMirrored.BackArrow",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
    autoMirror = true,
).apply {
    path(fill = SolidColor(Color.Black)) {
        moveTo(20f, 11f)
        horizontalLineTo(7.83f)
        lineToRelative(5.59f, -5.59f)
        lineTo(12f, 4f)
        lineToRelative(-8f, 8f)
        lineToRelative(8f, 8f)
        lineToRelative(1.41f, -1.41f)
        lineTo(7.83f, 13f)
        horizontalLineTo(20f)
        verticalLineToRelative(-2f)
        close()
    }
}.build()
