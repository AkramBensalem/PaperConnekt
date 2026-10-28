package com.paperconnekt.app.legacygraph.components

import com.paperconnekt.app.legacygraph.interactivePointer

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
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

@Composable
internal fun GraphRenameDialog(
    title: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var newTitle by remember { mutableStateOf(title) }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Edit, contentDescription = null) },
        title = { Text("Rename Graph") },
        text = {
            OutlinedTextField(
                value = newTitle,
                onValueChange = { newTitle = it },
                label = { Text("Graph name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { onSave(newTitle.trim()) },
                enabled = newTitle.isNotBlank(), modifier = Modifier.interactivePointer(enabled = newTitle.isNotBlank())
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.interactivePointer()) {
                Text("Cancel")
            }
        }
    )
}
