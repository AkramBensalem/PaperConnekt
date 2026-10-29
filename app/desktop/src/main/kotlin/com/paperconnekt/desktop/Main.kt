package com.paperconnekt.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.runtime.remember
import com.paperconnekt.app.PaperConnektApp
import com.paperconnekt.storage.DesktopWorkspaceStorage

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "PaperConnekt",
    ) {
        PaperConnektApp(
            secretStore = remember { DesktopSecretStore() },
            workspaceStorage = remember { DesktopWorkspaceStorage() },
            pdfAttachments = remember { DesktopPdfAttachmentUi() },
        )
    }
}
