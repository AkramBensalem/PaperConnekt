package com.paperconnekt.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.paperconnekt.app.PaperConnektApp
import com.paperconnekt.storage.DesktopWorkspaceStorage

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "PaperConnekt",
    ) {
        PaperConnektApp(
            secretStore = DesktopSecretStore(),
            workspaceStorage = DesktopWorkspaceStorage(),
        )
    }
}
