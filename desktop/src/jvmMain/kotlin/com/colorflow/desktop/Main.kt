package com.colorflow.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import androidx.compose.ui.unit.dp
import com.colorflow.desktop.ui.ColorFlowApp

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Color Flow",
        state = WindowState(width = 800.dp, height = 800.dp)
    ) {
        ColorFlowApp()
    }
}
