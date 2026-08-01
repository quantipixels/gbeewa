package com.quantipixels.gbeewa.showcase

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

fun mainViewController(): UIViewController = ComposeUIViewController {
    ShowcaseApp()
}
