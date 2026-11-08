package com.paperconnekt.app.legacygraph.components

import com.paperconnekt.app.resources.Res
import org.jetbrains.compose.resources.stringResource
import com.paperconnekt.app.resources.filled_edit
import org.jetbrains.compose.resources.painterResource
import com.paperconnekt.app.legacygraph.interactivePointer

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.paperconnekt.app.resources.cancel
import com.paperconnekt.app.resources.graph_name
import com.paperconnekt.app.resources.rename_graph_title
import com.paperconnekt.app.resources.save

@Composable
internal fun GraphRenameDialog(
    title: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var newTitle by remember { mutableStateOf(title) }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(painterResource(Res.drawable.filled_edit), contentDescription = null) },
        title = { Text(stringResource(Res.string.rename_graph_title)) },
        text = {
            OutlinedTextField(
                value = newTitle,
                onValueChange = { newTitle = it },
                label = { Text(stringResource(Res.string.graph_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { onSave(newTitle.trim()) },
                enabled = newTitle.isNotBlank(), modifier = Modifier.interactivePointer(enabled = newTitle.isNotBlank())
            ) {
                Text(stringResource(Res.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.interactivePointer()) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}
