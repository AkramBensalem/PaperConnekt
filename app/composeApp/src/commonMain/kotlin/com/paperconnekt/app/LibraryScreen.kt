package com.paperconnekt.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.paperconnekt.domain.model.LibraryEntry
import com.paperconnekt.domain.model.LibraryEntryKind
import com.paperconnekt.domain.model.Paper
import com.paperconnekt.storage.LocalResearchStore
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun LibraryScreen(
    store: LocalResearchStore,
    paddingValues: PaddingValues,
) {
    val entries by store.libraryEntries.collectAsState()
    val papers by store.papers.collectAsState()
    var selectedFolderId by remember { mutableStateOf<String?>(null) }
    var showFolderDialog by remember { mutableStateOf(false) }
    var showPaperDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val selectedFolder = entries.firstOrNull { it.id == selectedFolderId }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Library")
        Text("Organize saved papers in a local hierarchy inspired by Zotero.")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { showFolderDialog = true }) {
                Text("New folder")
            }
            Button(
                enabled = papers.any { paper ->
                    entries.none { it.paperId == paper.id }
                },
                onClick = { showPaperDialog = true },
            ) {
                Text("Add saved paper")
            }
        }

        if (entries.isEmpty()) {
            Text("Your library is empty. Create a folder or save a paper from the dashboard.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(entries.filter { it.parentId == null }, key = LibraryEntry::id) { entry ->
                    LibraryTreeEntry(
                        entry = entry,
                        allEntries = entries,
                        papers = papers,
                        selectedFolderId = selectedFolderId,
                        onSelectFolder = { selectedFolderId = it },
                        onRemove = { entryId ->
                            scope.launch { store.removeLibraryEntry(entryId) }
                        },
                    )
                }
            }
        }

        selectedFolder?.let {
            Text("Selected folder: ${it.label}")
        }
    }

    if (showFolderDialog) {
        CreateFolderDialog(
            onDismiss = { showFolderDialog = false },
            onCreate = { name ->
                scope.launch {
                    store.saveLibraryEntry(
                        LibraryEntry(
                            id = "folder-${Random.nextLong()}",
                            label = name,
                            parentId = selectedFolderId,
                            kind = LibraryEntryKind.FOLDER,
                        ),
                    )
                    showFolderDialog = false
                }
            },
        )
    }

    if (showPaperDialog) {
        AddPaperToLibraryDialog(
            papers = papers.filter { paper ->
                entries.none { it.paperId == paper.id }
            },
            onDismiss = { showPaperDialog = false },
            onAdd = { paper ->
                scope.launch {
                    store.saveLibraryEntry(
                        LibraryEntry(
                            id = "library-paper-${Random.nextLong()}",
                            paperId = paper.id,
                            parentId = selectedFolderId,
                            label = paper.title,
                            kind = LibraryEntryKind.PAPER,
                        ),
                    )
                    showPaperDialog = false
                }
            },
        )
    }
}

@Composable
private fun LibraryTreeEntry(
    entry: LibraryEntry,
    allEntries: List<LibraryEntry>,
    papers: List<Paper>,
    selectedFolderId: String?,
    onSelectFolder: (String) -> Unit,
    onRemove: (String) -> Unit,
    depth: Int = 0,
) {
    val paper = papers.firstOrNull { it.id == entry.paperId }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (depth * 16).dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(if (entry.kind == LibraryEntryKind.FOLDER) "[Folder] ${entry.label}" else entry.label)
                if (entry.kind == LibraryEntryKind.PAPER) {
                    paper?.authors?.joinToString()?.let { Text(it) }
                }
            }
            Row {
                if (entry.kind == LibraryEntryKind.FOLDER) {
                    TextButton(onClick = { onSelectFolder(entry.id) }) {
                        Text(if (selectedFolderId == entry.id) "Selected" else "Select")
                    }
                }
                TextButton(onClick = { onRemove(entry.id) }) {
                    Text("Remove")
                }
            }
        }
    }

    allEntries
        .filter { it.parentId == entry.id }
        .forEach { child ->
            LibraryTreeEntry(
                entry = child,
                allEntries = allEntries,
                papers = papers,
                selectedFolderId = selectedFolderId,
                onSelectFolder = onSelectFolder,
                onRemove = onRemove,
                depth = depth + 1,
            )
        }
}

@Composable
private fun CreateFolderDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New folder") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Folder name") },
                singleLine = true,
            )
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank(),
                onClick = { onCreate(name.trim()) },
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun AddPaperToLibraryDialog(
    papers: List<Paper>,
    onDismiss: () -> Unit,
    onAdd: (Paper) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add saved paper") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (papers.isEmpty()) {
                    Text("No unfiled saved papers.")
                } else {
                    papers.forEach { paper ->
                        TextButton(onClick = { onAdd(paper) }) {
                            Text(paper.title)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
    )
}
