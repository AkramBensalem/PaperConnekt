package com.akram.bensalem.paper.connekt.feature.assistant.component

import com.paperconnekt.app.resources.Res
import com.paperconnekt.app.resources.filled_add
import com.paperconnekt.app.resources.filled_delete
import org.jetbrains.compose.resources.painterResource
import com.akram.bensalem.bistosh.core.designsystem.component.interactivePointer

import androidx.compose.foundation.layout.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.akram.bensalem.paper.connekt.feature.assistant.Note

@Composable
fun NotesManager(
    notes: List<Note>,
    onAddNote: (String, Int?) -> Unit,
    onDeleteNote: (String) -> Unit,
    modifier: Modifier = Modifier,
    headerCollapsed: Boolean = false
) {
    var showDialog by remember { mutableStateOf(false) }
    var noteText by remember { mutableStateOf("") }

    Column(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(visible = !headerCollapsed) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Research Notes", style = MaterialTheme.typography.titleLarge)
                    Button(onClick = { showDialog = true }, modifier = Modifier.interactivePointer()) {
                        Icon(painterResource(Res.drawable.filled_add), contentDescription = "Add Note")
                        Spacer(Modifier.width(4.dp))
                        Text("New Note")
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }

        if (notes.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(text = "No notes yet. Start by adding one!", color = MaterialTheme.colorScheme.outline)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(notes) { note ->
                    NoteItem(note, onDeleteNote)
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Add Note") },
            text = {
                TextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Type your note here...") }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (noteText.isNotBlank()) {
                            onAddNote(noteText, null)
                            noteText = ""
                            showDialog = false
                        }
                    }, modifier = Modifier.interactivePointer()
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }, modifier = Modifier.interactivePointer()) {
                    Text("Cancel")
                }
            }
        )
    }

}

@Composable
fun NoteItem(note: Note, onDeleteNote: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = note.content, style = MaterialTheme.typography.bodyLarge)
                if (note.pageNumber != null) {
                    Text(
                        text = "Page: ${note.pageNumber}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            IconButton(onClick = { onDeleteNote(note.id) }, modifier = Modifier.interactivePointer()) {
                Icon(painterResource(Res.drawable.filled_delete), contentDescription = "Delete Note", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
