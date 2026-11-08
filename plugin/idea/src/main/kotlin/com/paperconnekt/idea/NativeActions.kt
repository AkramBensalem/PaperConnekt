package com.paperconnekt.idea

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import javax.swing.JOptionPane

@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun NativeActions(content: @Composable RowScope.() -> Unit) {
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

internal fun confirmNative(message: String): Boolean =
    JOptionPane.showConfirmDialog(null, message, "PaperConnekt", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.OK_OPTION
