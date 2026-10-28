package com.paperconnekt.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.paperconnekt.domain.model.LibraryEntry
import com.paperconnekt.domain.model.LibraryEntryKind
import com.paperconnekt.domain.model.Paper
import com.paperconnekt.storage.LocalResearchStore
import kotlinx.coroutines.launch
import kotlin.random.Random

private enum class LibraryView {
    ALL,
    UNFILED,
    RECENT,
}

@Composable
fun LibraryScreen(
    store: LocalResearchStore,
    paddingValues: PaddingValues,
    onOpenPaper: (Paper) -> Unit = {},
    onOpenGraph: (Paper) -> Unit = {},
) {
    val entries by store.libraryEntries.collectAsState()
    val papers by store.papers.collectAsState()
    val scope = rememberCoroutineScope()
    var selectedFolderId by remember { mutableStateOf<String?>(null) }
    var view by remember { mutableStateOf(LibraryView.ALL) }
    var query by remember { mutableStateOf("") }
    var showCreateFolder by remember { mutableStateOf(false) }
    var renameFolder by remember { mutableStateOf<LibraryEntry?>(null) }
    var deleteFolder by remember { mutableStateOf<LibraryEntry?>(null) }
    var movePaper by remember { mutableStateOf<LibraryEntry?>(null) }
    var expandedFolders by remember { mutableStateOf(setOf<String>()) }

    val folders = entries.filter { it.kind == LibraryEntryKind.FOLDER }
    val paperEntries = entries.filter { it.kind == LibraryEntryKind.PAPER }
    val paperById = papers.associateBy(Paper::id)
    val filedPaperIds = paperEntries.mapNotNull(LibraryEntry::paperId).toSet()
    val unfiledEntries = papers
        .filterNot { it.id in filedPaperIds }
        .map { paper ->
            LibraryEntry(
                id = "unfiled-${paper.id}",
                paperId = paper.id,
                label = paper.title,
                kind = LibraryEntryKind.PAPER,
            )
        }
    val libraryPaperEntries = paperEntries + unfiledEntries
    val selectedFolder = folders.firstOrNull { it.id == selectedFolderId }
    val visiblePaperEntries = libraryPaperEntries
        .filter { entry ->
            when {
                view == LibraryView.UNFILED -> entry.parentId == null
                view == LibraryView.RECENT -> entry in libraryPaperEntries.takeLast(8)
                selectedFolderId != null -> entry.parentId == selectedFolderId
                else -> true
            }
        }
        .filter { entry ->
            val paper = entry.paperId?.let(paperById::get)
            query.isBlank() ||
                entry.label.contains(query, ignoreCase = true) ||
                paper?.authors?.any { author -> author.contains(query, ignoreCase = true) } == true ||
                paper?.doi?.contains(query, ignoreCase = true) == true
        }
    val unfiledCount = unfiledEntries.size
    val orphanCount = paperEntries.count { it.paperId !in paperById }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
    ) {
        val compact = maxWidth < 900.dp
        Scaffold(
            topBar = {
                LibraryHeader(
                    paperCount = papers.size,
                    folderCount = folders.size,
                    unfiledCount = unfiledCount,
                    query = query,
                    onQueryChange = { query = it },
                    onNewFolder = { showCreateFolder = true },
                    compact = compact,
                )
            },
        ) { contentPadding ->
            if (compact) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    LibraryContextBar(
                        view = view,
                        selectedFolder = selectedFolder,
                        onViewChange = {
                            view = it
                            selectedFolderId = null
                        },
                    )
                    LibraryFolderList(
                        folders = folders,
                        entries = entries,
                        selectedFolderId = selectedFolderId,
                        expandedFolders = expandedFolders,
                        onSelectFolder = { selectedFolderId = it },
                        onToggleFolder = { folderId ->
                            expandedFolders = if (folderId in expandedFolders) {
                                expandedFolders - folderId
                            } else {
                                expandedFolders + folderId
                            }
                        },
                        onNewFolder = { showCreateFolder = true },
                        onRename = { renameFolder = it },
                        onDelete = { deleteFolder = it },
                    )
                    LibraryPaperList(
                        modifier = Modifier.weight(1f),
                        entries = visiblePaperEntries,
                        paperById = paperById,
                        emptyLabel = if (selectedFolder != null) {
                            "No papers in ${selectedFolder.label} yet."
                        } else {
                            "No saved papers match this view."
                        },
                        onOpenPaper = onOpenPaper,
                        onOpenGraph = onOpenGraph,
                        onMove = { movePaper = it },
                        onDelete = { entry ->
                            scope.launch { store.removeLibraryEntry(entry.id) }
                        },
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding)
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    LibraryFolderPane(
                        modifier = Modifier.width(286.dp),
                        folders = folders,
                        entries = entries,
                        selectedFolderId = selectedFolderId,
                        expandedFolders = expandedFolders,
                        onSelectFolder = { selectedFolderId = it },
                        onToggleFolder = { folderId ->
                            expandedFolders = if (folderId in expandedFolders) {
                                expandedFolders - folderId
                            } else {
                                expandedFolders + folderId
                            }
                        },
                        onNewFolder = { showCreateFolder = true },
                        onRename = { renameFolder = it },
                        onDelete = { deleteFolder = it },
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        LibraryContextBar(
                            view = view,
                            selectedFolder = selectedFolder,
                            onViewChange = {
                                view = it
                                selectedFolderId = null
                            },
                        )
                        if (orphanCount > 0) {
                            LibraryNotice("$orphanCount saved item(s) have missing paper metadata.")
                        }
                        LibraryPaperList(
                            modifier = Modifier.weight(1f),
                            entries = visiblePaperEntries,
                            paperById = paperById,
                            emptyLabel = if (selectedFolder != null) {
                                "No papers in ${selectedFolder.label} yet."
                            } else {
                                "No saved papers match this view."
                            },
                            onOpenPaper = onOpenPaper,
                            onOpenGraph = onOpenGraph,
                            onMove = { movePaper = it },
                            onDelete = { entry ->
                                scope.launch { store.removeLibraryEntry(entry.id) }
                            },
                        )
                    }
                }
            }
        }
    }

    if (showCreateFolder) {
        FolderNameDialog(
            title = "Create folder",
            confirmLabel = "Create",
            onDismiss = { showCreateFolder = false },
            onConfirm = { name ->
                scope.launch {
                    store.saveLibraryEntry(
                        LibraryEntry(
                            id = "folder-${Random.nextLong()}",
                            label = name,
                            parentId = selectedFolderId,
                            kind = LibraryEntryKind.FOLDER,
                        ),
                    )
                    showCreateFolder = false
                }
            },
        )
    }
    renameFolder?.let { folder ->
        FolderNameDialog(
            title = "Rename folder",
            initialValue = folder.label,
            confirmLabel = "Save",
            onDismiss = { renameFolder = null },
            onConfirm = { name ->
                scope.launch {
                    store.saveLibraryEntry(folder.copy(label = name))
                    renameFolder = null
                }
            },
        )
    }
    deleteFolder?.let { folder ->
        AlertDialog(
            onDismissRequest = { deleteFolder = null },
            title = { Text("Delete ${folder.label}?") },
            text = { Text("This removes the folder and its nested library links. Saved papers remain in your local collection.") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            store.removeLibraryEntry(folder.id)
                            if (selectedFolderId == folder.id) selectedFolderId = null
                            deleteFolder = null
                        }
                    },
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleteFolder = null }) { Text("Cancel") }
            },
        )
    }
    movePaper?.let { entry ->
        MovePaperDialog(
            entry = entry,
            folders = folders,
            onDismiss = { movePaper = null },
            onMove = { folderId ->
                scope.launch {
                    store.saveLibraryEntry(entry.copy(parentId = folderId))
                    movePaper = null
                }
            },
        )
    }
}

@Composable
private fun LibraryFolderList(
    folders: List<LibraryEntry>,
    entries: List<LibraryEntry>,
    selectedFolderId: String?,
    expandedFolders: Set<String>,
    onSelectFolder: (String?) -> Unit,
    onToggleFolder: (String) -> Unit,
    onNewFolder: () -> Unit,
    onRename: (LibraryEntry) -> Unit,
    onDelete: (LibraryEntry) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Collections", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = onNewFolder) { Text("New folder") }
            }
            TextButton(onClick = { onSelectFolder(null) }) {
                Text(
                    "All papers",
                    color = if (selectedFolderId == null) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
            }
            folders.filter { it.parentId == null }.forEach { folder ->
                FolderTreeRow(
                    folder = folder,
                    allFolders = folders,
                    allEntries = entries,
                    selectedFolderId = selectedFolderId,
                    expandedFolders = expandedFolders,
                    onSelectFolder = onSelectFolder,
                    onToggleFolder = onToggleFolder,
                    onRename = onRename,
                    onDelete = onDelete,
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun LibraryHeader(
    paperCount: Int,
    folderCount: Int,
    unfiledCount: Int,
    query: String,
    onQueryChange: (String) -> Unit,
    onNewFolder: () -> Unit,
    compact: Boolean,
) {
    TopAppBar(
        title = {
            Column {
                Text("Library", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "$paperCount papers · $folderCount folders · $unfiledCount unfiled",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        actions = {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.width(if (compact) 150.dp else 220.dp),
                placeholder = { Text("Search library") },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.width(8.dp))
            if (compact) {
                IconButton(onClick = onNewFolder) { Text("+") }
            } else {
                Button(onClick = onNewFolder) { Text("New folder") }
            }
            Spacer(Modifier.width(8.dp))
        },
    )
}

@Composable
private fun LibraryContextBar(
    view: LibraryView,
    selectedFolder: LibraryEntry?,
    onViewChange: (LibraryView) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Showing", style = MaterialTheme.typography.labelLarge)
        FilterChip(
            selected = view == LibraryView.ALL && selectedFolder == null,
            onClick = { onViewChange(LibraryView.ALL) },
            label = { Text("All papers") },
        )
        FilterChip(
            selected = view == LibraryView.UNFILED,
            onClick = { onViewChange(LibraryView.UNFILED) },
            label = { Text("Unfiled") },
        )
        FilterChip(
            selected = view == LibraryView.RECENT,
            onClick = { onViewChange(LibraryView.RECENT) },
            label = { Text("Recent") },
        )
        selectedFolder?.let {
            Text(" / ${it.label}", color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun LibraryFolderPane(
    modifier: Modifier,
    folders: List<LibraryEntry>,
    entries: List<LibraryEntry>,
    selectedFolderId: String?,
    expandedFolders: Set<String>,
    onSelectFolder: (String?) -> Unit,
    onToggleFolder: (String) -> Unit,
    onNewFolder: () -> Unit,
    onRename: (LibraryEntry) -> Unit,
    onDelete: (LibraryEntry) -> Unit,
) {
    Card(
        modifier = modifier.fillMaxSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Collections", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = onNewFolder) { Text("New") }
            }
            TextButton(
                onClick = { onSelectFolder(null) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    "All papers",
                    modifier = Modifier.fillMaxWidth(),
                    color = if (selectedFolderId == null) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                items(folders.filter { it.parentId == null }, key = LibraryEntry::id) { folder ->
                    FolderTreeRow(
                        folder = folder,
                        allFolders = folders,
                        allEntries = entries,
                        selectedFolderId = selectedFolderId,
                        expandedFolders = expandedFolders,
                        onSelectFolder = onSelectFolder,
                        onToggleFolder = onToggleFolder,
                        onRename = onRename,
                        onDelete = onDelete,
                    )
                }
            }
        }
    }
}

@Composable
private fun FolderTreeRow(
    folder: LibraryEntry,
    allFolders: List<LibraryEntry>,
    allEntries: List<LibraryEntry>,
    selectedFolderId: String?,
    expandedFolders: Set<String>,
    onSelectFolder: (String?) -> Unit,
    onToggleFolder: (String) -> Unit,
    onRename: (LibraryEntry) -> Unit,
    onDelete: (LibraryEntry) -> Unit,
    depth: Int = 0,
) {
    val children = allFolders.filter { it.parentId == folder.id }
    val paperCount = allEntries.count { it.kind == LibraryEntryKind.PAPER && it.parentId == folder.id }
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = (depth * 12).dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (children.isNotEmpty()) {
                TextButton(onClick = { onToggleFolder(folder.id) }) {
                    Text(if (folder.id in expandedFolders) "−" else "+")
                }
            } else {
                Spacer(Modifier.width(48.dp))
            }
            TextButton(
                onClick = { onSelectFolder(folder.id) },
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    "${folder.label}  $paperCount",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (selectedFolderId == folder.id) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
            }
            FolderActions(
                onRename = { onRename(folder) },
                onDelete = { onDelete(folder) },
            )
        }
        if (folder.id in expandedFolders) {
            children.forEach { child ->
                FolderTreeRow(
                    folder = child,
                    allFolders = allFolders,
                    allEntries = allEntries,
                    selectedFolderId = selectedFolderId,
                    expandedFolders = expandedFolders,
                    onSelectFolder = onSelectFolder,
                    onToggleFolder = onToggleFolder,
                    onRename = onRename,
                    onDelete = onDelete,
                    depth = depth + 1,
                )
            }
        }
    }
}

@Composable
private fun FolderActions(
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) { Text("⋯") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Rename") },
                onClick = {
                    expanded = false
                    onRename()
                },
            )
            DropdownMenuItem(
                text = { Text("Delete") },
                onClick = {
                    expanded = false
                    onDelete()
                },
            )
        }
    }
}

@Composable
private fun LibraryPaperList(
    modifier: Modifier,
    entries: List<LibraryEntry>,
    paperById: Map<String, Paper>,
    emptyLabel: String,
    onOpenPaper: (Paper) -> Unit,
    onOpenGraph: (Paper) -> Unit,
    onMove: (LibraryEntry) -> Unit,
    onDelete: (LibraryEntry) -> Unit,
) {
    if (entries.isEmpty()) {
        LibraryEmptyState(modifier = modifier, label = emptyLabel)
        return
    }
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(entries, key = LibraryEntry::id) { entry ->
            val paper = entry.paperId?.let(paperById::get)
            if (paper == null) {
                LibraryNotice("The saved paper for “${entry.label}” is no longer available.")
            } else {
                PaperLibraryCard(
                    paper = paper,
                    onOpen = { onOpenPaper(paper) },
                    onOpenGraph = { onOpenGraph(paper) },
                    onMove = { onMove(entry) },
                    onRemove = { onDelete(entry) },
                    canRemove = !entry.id.startsWith("unfiled-"),
                )
            }
        }
    }
}

@Composable
private fun PaperLibraryCard(
    paper: Paper,
    onOpen: () -> Unit,
    onOpenGraph: () -> Unit,
    onMove: () -> Unit,
    onRemove: () -> Unit,
    canRemove: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Text(
                paper.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                paper.authors.ifEmpty { listOf("Unknown author") }.joinToString(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                listOfNotNull(
                    paper.year?.toString(),
                    paper.doi?.let { "DOI $it" },
                    paper.pdfPath?.let { "PDF available" },
                ).ifEmpty { listOf("Metadata available locally") }.joinToString(" · "),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onOpenGraph) {
                    Text("Open graph")
                }
                if (paper.pdfPath != null) {
                    TextButton(onClick = onOpen) {
                        Text("Open paper")
                    }
                }
                TextButton(onClick = onMove) { Text("Move") }
                if (canRemove) {
                    TextButton(onClick = onRemove) { Text("Remove") }
                }
            }
        }
    }
}

@Composable
private fun LibraryEmptyState(
    modifier: Modifier,
    label: String,
) {
    Surface(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("Nothing here yet", style = MaterialTheme.typography.titleLarge)
            Text(
                label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LibraryNotice(message: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Text(
            message,
            modifier = Modifier.padding(12.dp),
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}

@Composable
private fun FolderNameDialog(
    title: String,
    initialValue: String = "",
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember(initialValue) { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
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
                onClick = { onConfirm(name.trim()) },
            ) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun MovePaperDialog(
    entry: LibraryEntry,
    folders: List<LibraryEntry>,
    onDismiss: () -> Unit,
    onMove: (String?) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Move ${entry.label}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = { onMove(null) }) { Text("Unfiled") }
                folders.forEach { folder ->
                    TextButton(onClick = { onMove(folder.id) }) {
                        Text(folder.label)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
