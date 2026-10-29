package com.paperconnekt.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.paperconnekt.domain.model.LibraryEntry
import com.paperconnekt.domain.model.LibraryEntryKind
import com.paperconnekt.domain.model.Paper
import com.paperconnekt.storage.LocalResearchStore
import kotlinx.coroutines.launch
import kotlin.random.Random
import com.paperconnekt.app.resources.Res
import com.paperconnekt.app.resources.filled_add
import com.paperconnekt.app.resources.filled_archive
import com.paperconnekt.app.resources.filled_attach_file
import com.paperconnekt.app.resources.filled_check_circle
import com.paperconnekt.app.resources.filled_chevron_right
import com.paperconnekt.app.resources.filled_close
import com.paperconnekt.app.resources.filled_create_new_folder
import com.paperconnekt.app.resources.filled_delete
import com.paperconnekt.app.resources.filled_delete_outline
import com.paperconnekt.app.resources.filled_description
import com.paperconnekt.app.resources.filled_folder
import com.paperconnekt.app.resources.filled_folder_open
import com.paperconnekt.app.resources.filled_more_horiz
import com.paperconnekt.app.resources.filled_restore_from_trash
import com.paperconnekt.app.resources.filled_search
import com.paperconnekt.app.resources.filled_unpublished
import com.paperconnekt.app.resources.research_graph_empty
import org.jetbrains.compose.resources.painterResource

private enum class LibraryView {
    ALL,
    UNFILED,
    RECENT,
    ARCHIVE,
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
    var editPaper by remember { mutableStateOf<Paper?>(null) }
    var selectedPaper by remember { mutableStateOf<Paper?>(null) }
    var permanentlyDeletePaper by remember { mutableStateOf<Paper?>(null) }
    var expandedFolders by remember { mutableStateOf(setOf<String>()) }

    val folders = entries.filter { it.kind == LibraryEntryKind.FOLDER }
    val paperEntries = entries.filter { it.kind == LibraryEntryKind.PAPER }
    val paperById = papers.associateBy(Paper::id)
    val activePapers = papers.filterNot(Paper::archived)
    val archivedPapers = papers.filter(Paper::archived)
    val filedPaperIds = paperEntries.mapNotNull(LibraryEntry::paperId).toSet()
    val unfiledEntries = activePapers
        .filterNot { it.id in filedPaperIds }
        .map { paper ->
            LibraryEntry(
                id = "unfiled-${paper.id}",
                paperId = paper.id,
                label = paper.title,
                kind = LibraryEntryKind.PAPER,
            )
        }
    val libraryPaperEntries = paperEntries.filter { it.paperId?.let(paperById::get)?.archived != true } + unfiledEntries
    val archivedEntries = archivedPapers.map { paper ->
        LibraryEntry("archived-${paper.id}", paper.id, null, paper.title, LibraryEntryKind.PAPER)
    }
    val selectedFolder = folders.firstOrNull { it.id == selectedFolderId }
    fun belongsToSelectedFolder(entry: LibraryEntry): Boolean {
        var parentId = entry.parentId
        while (parentId != null) {
            if (parentId == selectedFolderId) return true
            parentId = folders.firstOrNull { it.id == parentId }?.parentId
        }
        return false
    }
    val visiblePaperEntries = libraryPaperEntries
        .filter { entry ->
            when {
                view == LibraryView.UNFILED -> entry.parentId == null
                view == LibraryView.RECENT -> entry in libraryPaperEntries.takeLast(8)
                selectedFolderId != null -> entry.parentId == selectedFolderId || belongsToSelectedFolder(entry)
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
                    paperCount = activePapers.size,
                    folderCount = folders.size,
                    unfiledCount = unfiledCount,
                    archiveCount = archivedPapers.size,
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
                        onArchive = { view = LibraryView.ARCHIVE; selectedFolderId = null },
                        archiveCount = archivedPapers.size,
                        archiveSelected = view == LibraryView.ARCHIVE,
                        onNewSubcollection = { folder ->
                            selectedFolderId = folder.id
                            expandedFolders = expandedFolders + folder.id
                            showCreateFolder = true
                        },
                        onRename = { renameFolder = it },
                        onDelete = { deleteFolder = it },
                    )
                    LibraryPaperList(
                        modifier = Modifier.weight(1f),
                        entries = if (view == LibraryView.ARCHIVE) archivedEntries else visiblePaperEntries,
                        paperById = paperById,
                        emptyLabel = if (selectedFolder != null) {
                            "No papers in ${selectedFolder.label} yet."
                        } else {
                            "No saved papers match this view."
                        },
                        onOpenPaper = onOpenPaper,
                        onOpenGraph = onOpenGraph,
                        onMove = { movePaper = it },
                        onEdit = { editPaper = it },
                        onToggleCompleted = { paper -> scope.launch { store.savePaper(paper.copy(completed = !paper.completed)) } },
                        onSelect = { selectedPaper = it },
                        onDelete = { entry ->
                            entry.paperId?.let { paperById[it] }?.let { paper ->
                                if (view == LibraryView.ARCHIVE) permanentlyDeletePaper = paper
                                else scope.launch { store.savePaper(paper.copy(archived = true)) }
                            }
                        },
                        onRestore = { entry ->
                            entry.paperId?.let { paperById[it] }?.let { paper ->
                                scope.launch { store.savePaper(paper.copy(archived = false)) }
                            }
                        },
                        archiveView = view == LibraryView.ARCHIVE,
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
                        onArchive = { view = LibraryView.ARCHIVE; selectedFolderId = null },
                        archiveCount = archivedPapers.size,
                        archiveSelected = view == LibraryView.ARCHIVE,
                        onNewSubcollection = { folder ->
                            selectedFolderId = folder.id
                            expandedFolders = expandedFolders + folder.id
                            showCreateFolder = true
                        },
                        onRename = { renameFolder = it },
                        onDelete = { deleteFolder = it },
                    )
                    VerticalDivider(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
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
                            entries = if (view == LibraryView.ARCHIVE) archivedEntries else visiblePaperEntries,
                            paperById = paperById,
                            emptyLabel = if (selectedFolder != null) {
                                "No papers in ${selectedFolder.label} yet."
                            } else {
                                "No saved papers match this view."
                            },
                            onOpenPaper = onOpenPaper,
                            onOpenGraph = onOpenGraph,
                            onMove = { movePaper = it },
                            onEdit = { editPaper = it },
                            onToggleCompleted = { paper -> scope.launch { store.savePaper(paper.copy(completed = !paper.completed)) } },
                            onSelect = { selectedPaper = it },
                            onDelete = { entry ->
                            entry.paperId?.let { paperById[it] }?.let { paper ->
                                if (view == LibraryView.ARCHIVE) permanentlyDeletePaper = paper
                                else scope.launch { store.savePaper(paper.copy(archived = true)) }
                            }
                            },
                            onRestore = { entry ->
                                entry.paperId?.let { paperById[it] }?.let { paper ->
                                    scope.launch { store.savePaper(paper.copy(archived = false)) }
                                }
                            },
                            archiveView = view == LibraryView.ARCHIVE,
                        )
                    }
                    selectedPaper?.let { paper ->
                        PaperDetailsPane(
                            modifier = Modifier.width(320.dp).fillMaxHeight(),
                            paper = paper,
                            onRead = { if (paper.pdfPath != null) onOpenPaper(paper) },
                            onEdit = { editPaper = paper },
                            onClose = { selectedPaper = null },
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
                    selectedFolderId?.let { expandedFolders = expandedFolders + it }
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
    editPaper?.let { paper ->
        PaperDetailsDialog(
            paper = paper,
            onDismiss = { editPaper = null },
            onSave = { updated ->
                scope.launch {
                    store.savePaper(updated)
                    editPaper = null
                }
            },
        )
    }
    permanentlyDeletePaper?.let { paper ->
        AlertDialog(
            onDismissRequest = { permanentlyDeletePaper = null },
            title = { Text("Delete permanently?") },
            text = { Text("${paper.title} will be removed from this workspace and cannot be restored.") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            store.removePaper(paper.id)
                            permanentlyDeletePaper = null
                        }
                    },
                ) { Text("Delete permanently") }
            },
            dismissButton = { TextButton(onClick = { permanentlyDeletePaper = null }) { Text("Cancel") } },
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
    onArchive: () -> Unit,
    archiveCount: Int,
    archiveSelected: Boolean = false,
    onNewSubcollection: (LibraryEntry) -> Unit = {},
    onRename: (LibraryEntry) -> Unit,
    onDelete: (LibraryEntry) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().heightIn(min = 220.dp, max = 330.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
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
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                item {
                    LibraryRootRow(selected = selectedFolderId == null, onClick = { onSelectFolder(null) })
                }
                item {
                    FolderTreeGroup(
                        folders = folders.filter { it.parentId == null },
                        allFolders = folders,
                        allEntries = entries,
                        selectedFolderId = selectedFolderId,
                        expandedFolders = expandedFolders,
                        onSelectFolder = onSelectFolder,
                        onToggleFolder = onToggleFolder,
                        onRename = onRename,
                        onDelete = onDelete,
                        onNewChild = onNewSubcollection,
                    )
                }
            }
            HorizontalDivider(
                modifier = Modifier.padding(top = 6.dp, bottom = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
            )
            Spacer(Modifier.height(8.dp))
            ArchiveRow(count = archiveCount, selected = archiveSelected, onClick = onArchive)
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun LibraryHeader(
    paperCount: Int,
    folderCount: Int,
    unfiledCount: Int,
    archiveCount: Int,
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
                    "$paperCount papers · $folderCount collections · $unfiledCount unfiled · $archiveCount archived",
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
                leadingIcon = { Icon(painterResource(Res.drawable.filled_search), contentDescription = null) },
                placeholder = { Text("Search library") },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.width(8.dp))
            if (compact) {
                IconButton(onClick = onNewFolder) { Icon(painterResource(Res.drawable.filled_add), contentDescription = "New collection") }
            } else {
                Button(onClick = onNewFolder) {
                    Icon(painterResource(Res.drawable.filled_create_new_folder), contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("New collection")
                }
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
        FilterChip(
            selected = view == LibraryView.ARCHIVE,
            onClick = { onViewChange(LibraryView.ARCHIVE) },
            label = { Text("Archive") },
            leadingIcon = { Icon(painterResource(Res.drawable.filled_archive), contentDescription = null) },
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
    onArchive: () -> Unit,
    archiveCount: Int,
    archiveSelected: Boolean = false,
    onNewSubcollection: (LibraryEntry) -> Unit = {},
    onRename: (LibraryEntry) -> Unit,
    onDelete: (LibraryEntry) -> Unit,
) {
    Card(
        modifier = modifier.fillMaxSize(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
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
                Column {
                    Text("Collections", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Build a calm reading space",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onNewFolder) {
                    Icon(painterResource(Res.drawable.filled_create_new_folder), contentDescription = "Create collection")
                }
            }
            LibraryRootRow(selected = selectedFolderId == null, onClick = { onSelectFolder(null) })
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                item {
                    FolderTreeGroup(
                        folders = folders.filter { it.parentId == null },
                        allFolders = folders,
                        allEntries = entries,
                        selectedFolderId = selectedFolderId,
                        expandedFolders = expandedFolders,
                        onSelectFolder = onSelectFolder,
                        onToggleFolder = onToggleFolder,
                        onRename = onRename,
                        onDelete = onDelete,
                        onNewChild = onNewSubcollection,
                    )
                }
            }
            HorizontalDivider(
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
            )
            ArchiveRow(count = archiveCount, selected = archiveSelected, onClick = onArchive)
        }
    }
}

@Composable
private fun FolderTreeGroup(
    folders: List<LibraryEntry>,
    allFolders: List<LibraryEntry>,
    allEntries: List<LibraryEntry>,
    selectedFolderId: String?,
    expandedFolders: Set<String>,
    onSelectFolder: (String?) -> Unit,
    onToggleFolder: (String) -> Unit,
    onRename: (LibraryEntry) -> Unit,
    onDelete: (LibraryEntry) -> Unit,
    onNewChild: (LibraryEntry) -> Unit,
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            folders.forEach { folder ->
                FolderTreeRow(
                    folder = folder,
                    allFolders = allFolders,
                    allEntries = allEntries,
                    selectedFolderId = selectedFolderId,
                    expandedFolders = expandedFolders,
                    onSelectFolder = onSelectFolder,
                    onToggleFolder = onToggleFolder,
                    onRename = onRename,
                    onDelete = onDelete,
                    onNewChild = onNewChild,
                )
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
    onNewChild: (LibraryEntry) -> Unit = {},
    depth: Int = 0,
) {
    val children = allFolders.filter { it.parentId == folder.id }
    val paperCount = allEntries.count { it.kind == LibraryEntryKind.PAPER && it.parentId == folder.id }
    val siblingGuideColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.32f)
    Column {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = (depth * 12).dp)
                .clip(MaterialTheme.shapes.small)
                .clickable { onSelectFolder(folder.id) },
            color = if (folder.id == selectedFolderId) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                Color.Transparent
            },
            border = if (folder.id == selectedFolderId) {
                BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.32f))
            } else {
                null
            },
            shape = MaterialTheme.shapes.small,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (children.isNotEmpty()) {
                    IconButton(
                        onClick = { onToggleFolder(folder.id) },
                        modifier = Modifier.width(32.dp),
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.filled_chevron_right),
                            contentDescription = if (folder.id in expandedFolders) "Collapse ${folder.label}" else "Expand ${folder.label}",
                            modifier = Modifier.rotate(if (folder.id in expandedFolders) 90f else 0f),
                        )
                    }
                } else {
                    Spacer(Modifier.width(32.dp))
                }
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 6.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = if (folder.id == selectedFolderId) painterResource(Res.drawable.filled_folder_open) else painterResource(Res.drawable.filled_folder),
                        contentDescription = null,
                        tint = if (folder.id == selectedFolderId) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        folder.label,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (folder.id == selectedFolderId) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                    Surface(
                        color = if (folder.id == selectedFolderId) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = MaterialTheme.shapes.extraSmall,
                    ) {
                        Text(
                            paperCount.toString(),
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                FolderActions(
                    onRename = { onRename(folder) },
                    onDelete = { onDelete(folder) },
                    onNewChild = { onNewChild(folder) },
                )
            }
        }
        if (folder.id != selectedFolderId) {
            HorizontalDivider(
                modifier = Modifier.padding(start = (depth * 12 + 44).dp, end = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f),
            )
        }
        if (folder.id in expandedFolders) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        val x = (depth * 12 + 16).dp.toPx()
                        drawLine(
                            color = siblingGuideColor,
                            start = Offset(x, 0f),
                            end = Offset(x, size.height),
                            strokeWidth = 1.dp.toPx(),
                        )
                    },
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(start = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
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
                            onNewChild = onNewChild,
                            depth = depth + 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryRootRow(
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.32f)) else null,
        shape = MaterialTheme.shapes.small,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                painterResource(Res.drawable.filled_folder_open),
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "All papers",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
            )
            Text("Library", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ArchiveRow(
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick),
        color = if (selected) MaterialTheme.colorScheme.errorContainer else Color.Transparent,
        shape = MaterialTheme.shapes.small,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                painterResource(Res.drawable.filled_delete_outline),
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text("Archive", color = if (selected) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface)
                Text("Deleted items", style = MaterialTheme.typography.labelSmall, color = if (selected) MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(count.toString(), style = MaterialTheme.typography.labelMedium, color = if (selected) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun FolderActions(
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onNewChild: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) { Icon(painterResource(Res.drawable.filled_more_horiz), contentDescription = "Collection actions") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("New subcollection") },
                onClick = {
                    expanded = false
                    onNewChild()
                },
            )
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
    onRestore: (LibraryEntry) -> Unit,
    archiveView: Boolean,
    onEdit: (Paper) -> Unit,
    onToggleCompleted: (Paper) -> Unit,
    onSelect: (Paper) -> Unit,
) {
    if (entries.isEmpty()) {
        LibraryEmptyState(modifier = modifier, label = emptyLabel)
        return
    }
    LazyColumn(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainer),
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
                    onRestore = { onRestore(entry) },
                    onEdit = { onEdit(paper) },
                    onToggleCompleted = { onToggleCompleted(paper) },
                    onSelect = { onSelect(paper) },
                    canRemove = !entry.id.startsWith("unfiled-"),
                    archiveView = archiveView,
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
    onRestore: () -> Unit,
    onEdit: () -> Unit,
    onToggleCompleted: () -> Unit,
    onSelect: () -> Unit,
    canRemove: Boolean,
    archiveView: Boolean,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp)
            .clickable(onClick = onSelect),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                painter = if (paper.pdfPath != null) painterResource(Res.drawable.filled_description) else painterResource(Res.drawable.filled_unpublished),
                contentDescription = if (paper.pdfPath != null) "PDF attached" else "No PDF attached",
                tint = if (paper.pdfPath != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            )
            Column(Modifier.weight(1f)) {
                Text(paper.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    paper.authors.ifEmpty { listOf("Unknown author") }.joinToString() +
                        "  ·  " + (paper.year?.toString() ?: "Year unknown"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (paper.completed) {
                Icon(painterResource(Res.drawable.filled_check_circle), contentDescription = "Completed", tint = MaterialTheme.colorScheme.tertiary)
            }
            TextButton(onClick = onSelect) { Text("View") }
            var actionsExpanded by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { actionsExpanded = true }) { Icon(painterResource(Res.drawable.filled_more_horiz), contentDescription = "Item actions") }
                DropdownMenu(expanded = actionsExpanded, onDismissRequest = { actionsExpanded = false }) {
                    if (paper.pdfPath != null) {
                        DropdownMenuItem(
                            text = { Text("Read PDF") },
                            onClick = { actionsExpanded = false; onOpen() },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Open graph") },
                        onClick = { actionsExpanded = false; onOpenGraph() },
                    )
                    DropdownMenuItem(
                        text = { Text("Edit details") },
                        onClick = { actionsExpanded = false; onEdit() },
                    )
                    DropdownMenuItem(
                        text = { Text("Move to collection") },
                        onClick = { actionsExpanded = false; onMove() },
                    )
                    DropdownMenuItem(
                        text = { Text(if (paper.completed) "Mark as to read" else "Mark completed") },
                        onClick = { actionsExpanded = false; onToggleCompleted() },
                    )
                    if (archiveView) {
                        DropdownMenuItem(
                            text = { Text("Restore to library") },
                            onClick = { actionsExpanded = false; onRestore() },
                            leadingIcon = { Icon(painterResource(Res.drawable.filled_restore_from_trash), contentDescription = null) },
                        )
                        DropdownMenuItem(
                            text = { Text("Delete permanently") },
                            onClick = { actionsExpanded = false; onRemove() },
                            leadingIcon = { Icon(painterResource(Res.drawable.filled_delete), contentDescription = null) },
                        )
                    } else if (canRemove) {
                        DropdownMenuItem(
                            text = { Text("Move to archive") },
                            onClick = { actionsExpanded = false; onRemove() },
                            leadingIcon = { Icon(painterResource(Res.drawable.filled_archive), contentDescription = null) },
                        )
                    }
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
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                painter = painterResource(Res.drawable.research_graph_empty),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.width(150.dp).padding(bottom = 12.dp),
            )
            Text("Nothing here yet", style = MaterialTheme.typography.titleLarge)
            Text(
                label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PaperDetailsPane(
    modifier: Modifier,
    paper: Paper,
    onRead: () -> Unit,
    onEdit: () -> Unit,
    onClose: () -> Unit,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Item details", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = onClose) { Icon(painterResource(Res.drawable.filled_close), contentDescription = "Close details") }
            }
            HorizontalDivider()
            Icon(
                painter = if (paper.pdfPath != null) painterResource(Res.drawable.filled_description) else painterResource(Res.drawable.filled_unpublished),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 6.dp),
            )
            Text(paper.title, style = MaterialTheme.typography.titleLarge)
            Text(
                paper.authors.ifEmpty { listOf("Unknown author") }.joinToString(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (paper.pdfPath != null) {
                    Button(onClick = onRead, modifier = Modifier.weight(1f)) { Text("Read PDF") }
                }
                TextButton(onClick = onEdit) { Text("Edit") }
            }
            HorizontalDivider()
            DetailLine("Year", paper.year?.toString() ?: "—")
            DetailLine("DOI", paper.doi ?: "—")
            DetailLine("Attachment", if (paper.pdfPath != null) "PDF attached" else "No PDF")
            DetailLine("Status", if (paper.completed) "Completed" else "To read")
            paper.abstractText?.takeIf { it.isNotBlank() }?.let {
                Text("Abstract", style = MaterialTheme.typography.labelLarge)
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(82.dp))
        Text(value, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
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
private fun PaperDetailsDialog(
    paper: Paper,
    onDismiss: () -> Unit,
    onSave: (Paper) -> Unit,
) {
    var title by remember(paper.id) { mutableStateOf(paper.title) }
    var authors by remember(paper.id) { mutableStateOf(paper.authors.joinToString(", ")) }
    var year by remember(paper.id) { mutableStateOf(paper.year?.toString().orEmpty()) }
    var doi by remember(paper.id) { mutableStateOf(paper.doi.orEmpty()) }
    var pdfPath by remember(paper.id) { mutableStateOf(paper.pdfPath.orEmpty()) }
    var abstractText by remember(paper.id) { mutableStateOf(paper.abstractText.orEmpty()) }
    var completed by remember(paper.id) { mutableStateOf(paper.completed) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Item details") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true)
                OutlinedTextField(authors, { authors = it }, label = { Text("Authors") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(year, { year = it }, label = { Text("Year") }, singleLine = true, modifier = Modifier.weight(0.35f))
                    OutlinedTextField(doi, { doi = it }, label = { Text("DOI") }, singleLine = true, modifier = Modifier.weight(0.65f))
                }
                OutlinedTextField(
                    value = pdfPath,
                    onValueChange = { pdfPath = it },
                    label = { Text("Attached PDF path") },
                    placeholder = { Text("/Users/you/Papers/article.pdf") },
                    leadingIcon = { Icon(painterResource(Res.drawable.filled_attach_file), contentDescription = null) },
                    supportingText = { Text("Local-first for now: paste the file path to attach a PDF.") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = abstractText,
                    onValueChange = { abstractText = it },
                    label = { Text("Abstract / notes") },
                    minLines = 3,
                    maxLines = 5,
                )
                FilterChip(
                    selected = completed,
                    onClick = { completed = !completed },
                    label = { Text(if (completed) "Completed item" else "Mark as completed") },
                    leadingIcon = { Icon(painterResource(Res.drawable.filled_check_circle), contentDescription = null) },
                )
                HorizontalDivider()
                Text("Attach a PDF now, then use Read PDF on the item card to open the reading workspace.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            Button(
                enabled = title.isNotBlank(),
                onClick = {
                    onSave(
                        paper.copy(
                            title = title.trim(),
                            authors = authors.split(',').map(String::trim).filter(String::isNotBlank),
                            year = year.toIntOrNull(),
                            doi = doi.trim().ifBlank { null },
                            pdfPath = pdfPath.trim().ifBlank { null },
                            abstractText = abstractText.trim().ifBlank { null },
                            completed = completed,
                        ),
                    )
                },
            ) { Text("Save changes") }
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
