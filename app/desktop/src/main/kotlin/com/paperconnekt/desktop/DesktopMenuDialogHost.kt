package com.paperconnekt.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogModalityType
import androidx.compose.ui.window.v2.DialogWindow
import androidx.compose.ui.window.v2.WindowBoundsProvider
import androidx.compose.ui.window.v2.WindowPositionProvider
import androidx.compose.ui.window.v2.WindowSizeProvider
import androidx.compose.ui.window.v2.rememberDialogState
import com.paperconnekt.app.AppMenuDialogHost

internal object DesktopMenuDialogHost : AppMenuDialogHost {
    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    override fun Dialog(title: String, onDismiss: () -> Unit, content: @Composable () -> Unit) {
        val state = rememberDialogState(
            initialBoundsProvider = WindowBoundsProvider(
                positionProvider = WindowPositionProvider.CenteredInParentWindow,
                sizeProvider = WindowSizeProvider.Fixed(DpSize(560.dp, 440.dp)),
            ),
        )
        DialogWindow(
            onCloseRequest = onDismiss,
            state = state,
            title = title,
            minSize = DpSize(320.dp, 240.dp),
            modalityType = DialogModalityType.DocumentModal,
            onPreviewKeyEvent = { event ->
                if (event.key == Key.Escape && event.type == KeyEventType.KeyDown) {
                    onDismiss()
                    true
                } else {
                    false
                }
            },
        ) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(title, style = MaterialTheme.typography.headlineSmall)
                    Box(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                        content()
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onDismiss) { Text("Close") }
                    }
                }
            }
        }
    }
}
