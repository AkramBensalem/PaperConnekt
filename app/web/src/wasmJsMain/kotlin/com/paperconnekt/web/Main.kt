package com.paperconnekt.web

import androidx.compose.ui.ComposeUiFlags
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.useSnapshotCache
import androidx.compose.ui.window.ComposeViewport
import com.paperconnekt.app.PaperConnektApp
import com.paperconnekt.domain.navigation.AppDestination
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    Napier.base(DebugAntilog())
    ComposeUiFlags.useSnapshotCache = false
    val initialDestination = BrowserHistory.initialDestination()
    ComposeViewport(viewportContainerId = "paperconnekt") {
        var requestedDestination by remember { mutableStateOf<AppDestination?>(null) }
        LaunchedEffect(Unit) {
            BrowserHistory.listen { name ->
                requestedDestination = runCatching { AppDestination.valueOf(name) }.getOrNull()
            }
        }
        PaperConnektApp(
            secretStore = BrowserSecretStore(),
            workspaceStorage = BrowserWorkspaceStorage(),
            pdfAttachments = BrowserPdfAttachmentUi(),
            initialDestination = initialDestination,
            requestedDestination = requestedDestination,
            onDestinationChanged = BrowserHistory::record,
        )
    }
}
