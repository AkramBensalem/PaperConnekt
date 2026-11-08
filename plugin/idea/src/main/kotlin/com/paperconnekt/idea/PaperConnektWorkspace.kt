package com.paperconnekt.idea

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intellij.openapi.project.Project
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import com.akram.bensalem.paper.connekt.models.ZoteroConfig
import com.akram.bensalem.paper.connekt.shared.zotero.KtorZoteroClient
import com.akram.bensalem.paper.connekt.shared.zotero.model.zotero.ZoteroCollection
import com.akram.bensalem.paper.connekt.shared.zotero.model.zotero.ZoteroGroup
import com.akram.bensalem.paper.connekt.shared.zotero.model.zotero.ZoteroItem
import com.paperconnekt.domain.model.AssistantMessage
import com.paperconnekt.domain.model.AssistantRole
import com.paperconnekt.domain.model.AssistantSession
import com.paperconnekt.domain.model.LibraryEntry
import com.paperconnekt.domain.model.LibraryEntryKind
import com.paperconnekt.domain.model.Paper
import com.paperconnekt.domain.model.ResearchGraph
import com.paperconnekt.plugins.common.PluginWorkspaceSnapshot
import com.paperconnekt.secrets.SecretKey
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.combine
import org.jetbrains.jewel.ui.component.DefaultButton
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField
import java.io.File
import java.text.DateFormat
import java.util.Date
import kotlinx.datetime.Instant

private enum class WorkspaceTab(
    val title: String,
    val icon: NativeIcon,
) {
    HOME("Home", NativeIcon.Home),
    ASSISTANT("Assistant", NativeIcon.Assistant),
    LIBRARY("Library", NativeIcon.Library),
    GRAPH("Graph", NativeIcon.Graph),
    READER("Reader", NativeIcon.Book),
}

private class WorkspaceController(
    val store: NativeContestStore = NativeContestStore(),
) {
    var snapshot by mutableStateOf(PluginWorkspaceSnapshot())
        private set
    var loading by mutableStateOf(true)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var zoteroGroups by mutableStateOf<List<ZoteroGroup>>(emptyList())
        private set
    var zoteroCollections by mutableStateOf<List<ZoteroCollection>>(emptyList())
        private set
    var zoteroPaperCollections by mutableStateOf<Map<String, Set<String>>>(emptyMap())
        private set
    var selectedZoteroGroupId by mutableStateOf<Int?>(null)
        private set
    var selectedZoteroCollectionKey by mutableStateOf<String?>(null)
        private set
    var zoteroLoading by mutableStateOf(false)
        private set
    var zoteroError by mutableStateOf<String?>(null)
        private set

    suspend fun refresh() {
        loading = true
        error = null
        try {
            store.initialize()
            try {
                syncZotero()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                reportZoteroError(exception)
                reportError("Could not sync Zotero library", exception)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (exception: Exception) {
            reportError("Unable to read the local workspace", exception)
        } finally {
            loading = false
        }
    }

    private suspend fun syncZotero() {
        val apiKey = IdeaSecretStore().read(SecretKey.ZOTERO).orEmpty()
        val userId = store.settings.value.zoteroUserId.trim()
        if (apiKey.isBlank() || userId.isBlank()) {
            zoteroGroups = emptyList()
            zoteroCollections = emptyList()
            zoteroPaperCollections = emptyMap()
            zoteroError = null
            zoteroLoading = false
            return
        }

        zoteroLoading = true
        zoteroError = null
        val client = KtorZoteroClient()
        val groups = client.listGroups(ZoteroConfig(apiKey = apiKey, userId = userId))
        zoteroGroups = groups
        selectedZoteroGroupId = selectedZoteroGroupId?.takeIf { id -> groups.any { it.id == id } }
        selectedZoteroCollectionKey = null
        val config = ZoteroConfig(
            apiKey = apiKey,
            userId = userId,
            groupId = selectedZoteroGroupId?.toString(),
        )
        val items = client.listLibraryItems(config).filter { it.data.itemType != "attachment" }
        val collections = client.listCollections(config)
        zoteroCollections = collections
        zoteroPaperCollections = items.associate { item ->
            "zotero:${selectedZoteroGroupId ?: "personal"}:${item.key}" to item.data.collections.toSet()
        }
        val zoteroIds = items.mapTo(mutableSetOf()) {
            "zotero:${selectedZoteroGroupId ?: "personal"}:${it.key}"
        }
        store.papers.value
            .filter { it.source.equals("Zotero", ignoreCase = true) && it.id !in zoteroIds }
            .forEach { store.removePaper(it.id) }
        items.forEach { store.savePaper(it.toContestPaper(selectedZoteroGroupId)) }
        zoteroLoading = false
    }

    suspend fun selectZoteroGroup(groupId: Int?) {
        selectedZoteroGroupId = groupId
        selectedZoteroCollectionKey = null
        syncZotero()
    }

    fun selectZoteroCollection(collectionKey: String?) {
        selectedZoteroCollectionKey = collectionKey
    }

    fun reportZoteroError(exception: Exception) {
        zoteroError = exception.message ?: "Could not load the Zotero library."
        zoteroLoading = false
    }

    suspend fun observe() {
        combine(store.papers, store.graphs, store.assistantSessions, store.libraryEntries) { papers, graphs, sessions, entries ->
            PluginWorkspaceSnapshot(papers, graphs, sessions, entries)
        }.collect { snapshot = it }
    }

    fun reportError(message: String, exception: Exception) {
        error = "$message: ${exception.message ?: "Unknown error"}"
        com.intellij.openapi.diagnostic.Logger.getInstance(WorkspaceController::class.java).warn(message, exception)
    }
}

@Composable
@OptIn(FlowPreview::class)
internal fun PaperConnektWorkspace(project: Project) {
    val controller = remember { WorkspaceController() }
    val scope = rememberCoroutineScope()
    val features = remember(controller, scope) { NativeFeatures(controller.store, scope) }
    DisposableEffect(features) { onDispose { features.close() } }
    var tab by remember { mutableStateOf(WorkspaceTab.HOME) }
    var selectedPaperId by remember { mutableStateOf<String?>(null) }
    var selectedGraphId by remember { mutableStateOf<String?>(null) }
    var selectedAssistantSessionId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(controller) {
        controller.refresh()
        controller.observe()
    }
    LaunchedEffect(features) {
        snapshotFlow { Triple(features.paper?.id, features.reader.state.readingPosition, features.reader.state.zoom) }
            .debounce(500).collect {
                try {
                    features.saveReadingPosition()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (exception: Exception) {
                    features.reportError("Could not save reading position", exception)
                }
            }
    }

    val openReader: (Paper) -> Unit = { paper ->
        if (features.open(paper)) {
            selectedPaperId = paper.id
            tab = WorkspaceTab.READER
        }
    }
    val openLocal: () -> Unit = {
        FileChooser.chooseFile(FileChooserDescriptorFactory.createSingleFileDescriptor("pdf"), project, null)?.let { file ->
            scope.launch {
                try {
                    openReader(features.importPdf(file.path))
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (exception: Exception) {
                    controller.reportError("Could not import the PDF", exception)
                }
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val showLabels = maxWidth >= 420.dp
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                NativeToolButton("Home", NativeIcon.Home, selected = tab == WorkspaceTab.HOME, showLabel = showLabels) {
                    tab = WorkspaceTab.HOME
                }
                NativeToolButton("Assistant", NativeIcon.Assistant, selected = tab == WorkspaceTab.ASSISTANT, showLabel = showLabels) {
                    tab = WorkspaceTab.ASSISTANT
                }
                NativeToolButton("Graph", NativeIcon.Graph, selected = tab == WorkspaceTab.GRAPH, showLabel = showLabels) {
                    tab = WorkspaceTab.GRAPH
                }
                NativeToolButton("Library", NativeIcon.Library, selected = tab == WorkspaceTab.LIBRARY, showLabel = showLabels) {
                    tab = WorkspaceTab.LIBRARY
                }
                if (features.paper != null) {
                    val readerSelected = tab == WorkspaceTab.READER
                    Row(
                        Modifier.clip(RoundedCornerShape(5.dp))
                            .background(if (readerSelected) nativeAccent.copy(alpha = .25f) else Color.Transparent),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        NativeToolButton("Reader", NativeIcon.Book, showLabel = showLabels) {
                            tab = WorkspaceTab.READER
                        }
                        NativeToolButton("Close reader", NativeIcon.Close, compact = true) {
                            if (features.closeReader() && readerSelected) tab = WorkspaceTab.LIBRARY
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                NativeToolButton("Refresh workspace", NativeIcon.Refresh, enabled = !controller.loading) {
                    scope.launch { controller.refresh() }
                }
                NativeMenu(
                    label = "Workspace actions",
                    actions = listOf(
                        NativeMenuAction("Provider settings…", NativeIcon.Settings) {
                            scope.launch {
                                try {
                                    editNativeProviderSettings(project, features)
                                    controller.refresh()
                                } catch (cancelled: CancellationException) {
                                    throw cancelled
                                } catch (exception: Exception) {
                                    controller.reportError("Could not save provider settings", exception)
                                }
                            }
                        },
                        NativeMenuAction("Refresh local data", NativeIcon.Refresh, enabled = !controller.loading) {
                            scope.launch { controller.refresh() }
                        },
                    ),
                    icon = NativeIcon.Settings,
                )
            }
        }
        NativeDivider()
        when {
            controller.loading && controller.snapshot == PluginWorkspaceSnapshot() ->
                NativeEmptyState("Loading your local workspace…")
            controller.error != null && controller.snapshot == PluginWorkspaceSnapshot() ->
                NativeEmptyState(controller.error ?: "Unable to read the local workspace.")
            else -> Box(Modifier.weight(1f).fillMaxWidth()) {
                when (tab) {
                    WorkspaceTab.HOME -> DashboardScreen(
                        snapshot = controller.snapshot,
                        onPaperClick = { paper ->
                            selectedPaperId = paper.id
                            openReader(paper)
                        },
                        onGraphClick = { graph ->
                            selectedGraphId = graph.id
                            tab = WorkspaceTab.GRAPH
                        },
                        onAssistantClick = { session ->
                            selectedAssistantSessionId = session.id
                            tab = WorkspaceTab.ASSISTANT
                        },
                    )
                    WorkspaceTab.ASSISTANT -> AssistantScreen(
                        snapshot = controller.snapshot,
                        selectedSessionId = selectedAssistantSessionId,
                        onSelect = { selectedAssistantSessionId = it.id },
                    )
                    WorkspaceTab.LIBRARY -> LibraryScreen(
                        snapshot = controller.snapshot,
                        selectedPaperId = selectedPaperId,
                        onSelect = { selectedPaperId = it.id },
                        onOpen = openReader,
                        onImport = openLocal,
                        zoteroGroups = controller.zoteroGroups,
                        zoteroCollections = controller.zoteroCollections,
                        zoteroPaperCollections = controller.zoteroPaperCollections,
                        selectedZoteroGroupId = controller.selectedZoteroGroupId,
                        selectedZoteroCollectionKey = controller.selectedZoteroCollectionKey,
                        zoteroLoading = controller.zoteroLoading,
                        zoteroError = controller.zoteroError,
                        onZoteroGroup = { groupId ->
                            scope.launch {
                                try {
                                    controller.selectZoteroGroup(groupId)
                                } catch (cancelled: CancellationException) {
                                    throw cancelled
                                } catch (exception: Exception) {
                                    controller.reportZoteroError(exception)
                                }
                            }
                        },
                        onZoteroCollection = controller::selectZoteroCollection,
                    )
                    WorkspaceTab.GRAPH -> NativeGraphWorkspace(
                        snapshot = controller.snapshot,
                        store = controller.store,
                        secrets = features.secrets,
                        selectedGraphId = selectedGraphId,
                        onSelectGraph = { selectedGraphId = it },
                        onOpenPaper = openReader,
                        onError = controller::reportError,
                        onPersistPositions = { id, positions -> scope.launch {
                            try {
                                if (controller.store.graphs.value.any { it.id == id }) {
                                    controller.store.savePositions(id, positions)
                                }
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (exception: Exception) {
                                controller.reportError("Could not save the graph layout", exception)
                            }
                        } },
                    )
                    WorkspaceTab.READER -> ReaderScreen(
                        features = features,
                    )
                }
            }
        }
        controller.error?.takeIf { controller.snapshot != PluginWorkspaceSnapshot() }?.let {
            Text(it, color = Color(0xFFE5534B))
        }
    }
}

@Composable
private fun WorkspaceHeader(
    tab: WorkspaceTab,
    snapshot: PluginWorkspaceSnapshot,
    loading: Boolean,
    error: String?,
    onRefresh: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        NativeHeading(
            title = tab.title,
            subtitle = when {
                error != null -> error
                loading -> "Refreshing local workspace…"
                else -> "${snapshot.papers.size} papers · ${snapshot.graphs.size} graphs · ${snapshot.assistantSessions.size} sessions"
            },
            modifier = Modifier.weight(1f),
        )
        NativeToolButton("Refresh", NativeIcon.Refresh, enabled = !loading, onClick = onRefresh)
        NativeMenu(
            label = "Workspace actions",
            actions = listOf(
                NativeMenuAction("Refresh local data", NativeIcon.Refresh, enabled = !loading, action = onRefresh),
            ),
        )
    }
}

@Composable
private fun DashboardScreen(
    snapshot: PluginWorkspaceSnapshot,
    onPaperClick: (Paper) -> Unit,
    onGraphClick: (ResearchGraph) -> Unit,
    onAssistantClick: (AssistantSession) -> Unit,
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DashboardMetric("Papers", snapshot.papers.size.toString(), NativeIcon.Library, Modifier.weight(1f))
            DashboardMetric("Graphs", snapshot.graphs.size.toString(), NativeIcon.Graph, Modifier.weight(1f))
            DashboardMetric("Conversations", snapshot.assistantSessions.size.toString(), NativeIcon.Assistant, Modifier.weight(1f))
            DashboardMetric("Folders", snapshot.libraryEntries.count { it.kind == LibraryEntryKind.FOLDER }.toString(), NativeIcon.Folder, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                NativeSectionLabel("Recent papers")
                val recent = snapshot.papers.sortedByDescending { it.lastOpenedAt }.take(5)
                if (recent.isEmpty()) {
                    NativeEmptyState("No papers in the local library.", Modifier.fillMaxWidth().weight(1f))
                } else {
                    recent.forEach { paper -> PaperCard(paper, onClick = { onPaperClick(paper) }) }
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                NativeSectionLabel("Research graphs")
                if (snapshot.graphs.isEmpty()) {
                    NativeEmptyState("Create a graph in the desktop app to see it here.", Modifier.fillMaxWidth().weight(1f))
                } else {
                    snapshot.graphs.take(5).forEach { graph ->
                        NativeCard(onClick = { onGraphClick(graph) }) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                NativeGlyph(NativeIcon.Graph, tint = nativeAccent)
                                Column(Modifier.weight(1f)) {
                                    NativeCardTitle(graph.name)
                                    NativeMeta("${graph.paperIds.size} papers · ${graph.connections.size} connections")
                                }
                                NativeGlyph(NativeIcon.External, Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            NativeSectionLabel("Assistant sessions")
            snapshot.assistantSessions.take(4).forEach { session ->
                NativeCard(onClick = { onAssistantClick(session) }) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        NativeGlyph(NativeIcon.Assistant, tint = nativeAccent)
                        Column(Modifier.weight(1f)) {
                            NativeCardTitle(session.title)
                            NativeMeta("${session.messages.size} messages${session.updatedAt?.let { " · ${formatInstant(it)}" } ?: ""}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardMetric(label: String, value: String, icon: NativeIcon, modifier: Modifier) {
    NativeCard(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NativeGlyph(icon, tint = nativeAccent)
            Column {
                Text(value, fontSize = 20.sp)
                NativeMeta(label)
            }
        }
    }
}

@Composable
private fun LibraryScreen(
    snapshot: PluginWorkspaceSnapshot,
    selectedPaperId: String?,
    onSelect: (Paper) -> Unit,
    onOpen: (Paper) -> Unit,
    onImport: () -> Unit,
    zoteroGroups: List<ZoteroGroup>,
    zoteroCollections: List<ZoteroCollection>,
    zoteroPaperCollections: Map<String, Set<String>>,
    selectedZoteroGroupId: Int?,
    selectedZoteroCollectionKey: String?,
    zoteroLoading: Boolean,
    zoteroError: String?,
    onZoteroGroup: (Int?) -> Unit,
    onZoteroCollection: (String?) -> Unit,
) {
    val query = rememberTextFieldState()
    var filter by remember { mutableStateOf("All papers") }
    var sourceFilter by remember { mutableStateOf("All sources") }
    var selectedLocalFolderId by remember { mutableStateOf<String?>(null) }
    var expandedLocalFolderIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var expandedZoteroCollectionKeys by remember { mutableStateOf<Set<String>>(emptySet()) }
    val localFolders = snapshot.libraryEntries.filter { it.kind == LibraryEntryKind.FOLDER }
    val localPaperFolders = snapshot.libraryEntries
        .filter { it.kind == LibraryEntryKind.PAPER && it.paperId != null }
        .associate { it.paperId!! to it.parentId }
    val localPaperCount = snapshot.papers.count { !it.source.equals("Zotero", ignoreCase = true) }
    val localFolderCounts = remember(localFolders, localPaperFolders, localPaperCount) {
        localFolders.associate { folder ->
            folder.id to localPaperFolders.values.count { it in libraryFolderScope(folder.id, localFolders) }
        }
    }
    val zoteroPaperCount = snapshot.papers.count { it.source.equals("Zotero", ignoreCase = true) }
    val zoteroCollectionCounts = remember(zoteroCollections, zoteroPaperCollections, zoteroPaperCount) {
        zoteroCollections.associate { collection ->
            collection.key to zoteroPaperCollections.values.count {
                it.any(zoteroCollectionScope(collection.key, zoteroCollections)::contains)
            }
        }
    }
    val localFolderScope = remember(selectedLocalFolderId, localFolders) {
        libraryFolderScope(selectedLocalFolderId, localFolders)
    }
    val selectedCollectionKeys = remember(selectedZoteroCollectionKey, zoteroCollections) {
        zoteroCollectionScope(selectedZoteroCollectionKey, zoteroCollections)
    }
    val filtered = remember(
        query.text,
        snapshot.papers,
        filter,
        sourceFilter,
        selectedZoteroCollectionKey,
        zoteroPaperCollections,
        selectedCollectionKeys,
    ) {
        val normalized = query.text.toString().trim().lowercase()
        snapshot.papers.filter { paper ->
            (normalized.isEmpty() ||
                paper.title.lowercase().contains(normalized) ||
                paper.authors.any { it.lowercase().contains(normalized) } ||
                paper.tags.any { it.lowercase().contains(normalized) }) &&
                when (sourceFilter) {
                    "Local" -> !paper.source.equals("Zotero", ignoreCase = true)
                    "Zotero" -> paper.source.equals("Zotero", ignoreCase = true)
                    else -> true
                } &&
                (sourceFilter != "Zotero" ||
                    selectedZoteroCollectionKey == null ||
                    zoteroPaperCollections[paper.id].orEmpty().any(selectedCollectionKeys::contains)) &&
                (sourceFilter != "Local" ||
                    selectedLocalFolderId == null ||
                    localPaperFolders[paper.id] in localFolderScope) &&
                when (filter) {
                    "With PDF" -> paper.pdfPath != null
                    "Completed" -> paper.completed
                    else -> true
                }
        }
    }
    val selected = filtered.firstOrNull { it.id == selectedPaperId } ?: filtered.firstOrNull()
    var detailDialogPaper by remember { mutableStateOf<Paper?>(null) }
    val selectedCollectionLabel = when {
        sourceFilter == "Zotero" && selectedZoteroCollectionKey != null ->
            zoteroCollections.firstOrNull { it.key == selectedZoteroCollectionKey }?.data?.name
        sourceFilter == "Local" && selectedLocalFolderId != null ->
            localFolders.firstOrNull { it.id == selectedLocalFolderId }?.label
        else -> null
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val compact = maxWidth < 760.dp
        val showLabels = maxWidth >= 520.dp
        if (compact) {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (sourceFilter != "All sources") {
                    LibraryCollectionPane(
                        sourceFilter = sourceFilter,
                        localFolders = localFolders,
                        zoteroGroups = zoteroGroups,
                        zoteroCollections = zoteroCollections,
                        selectedLocalFolderId = selectedLocalFolderId,
                        selectedZoteroGroupId = selectedZoteroGroupId,
                        selectedZoteroCollectionKey = selectedZoteroCollectionKey,
                        expandedLocalFolderIds = expandedLocalFolderIds,
                        expandedZoteroCollectionKeys = expandedZoteroCollectionKeys,
                        localPaperCount = localPaperCount,
                        localFolderCounts = localFolderCounts,
                        zoteroPaperCount = zoteroPaperCount,
                        zoteroCollectionCounts = zoteroCollectionCounts,
                        onLocalFolder = { selectedLocalFolderId = it },
                        onZoteroGroup = onZoteroGroup,
                        onZoteroCollection = onZoteroCollection,
                        onToggleLocalFolder = { id ->
                            expandedLocalFolderIds = expandedLocalFolderIds.toggle(id)
                        },
                        onToggleZoteroCollection = { key ->
                            expandedZoteroCollectionKeys = expandedZoteroCollectionKeys.toggle(key)
                        },
                        modifier = Modifier.width(220.dp).fillMaxHeight(),
                    )
                }
                LibraryPaperList(
                    query = query,
                    sourceFilter = sourceFilter,
                    filter = filter,
                    filtered = filtered,
                    selected = selected,
                    showLabels = showLabels,
                    onSourceFilter = { sourceFilter = it },
                    onFilter = { filter = it },
                    onImport = onImport,
                    zoteroLoading = zoteroLoading,
                    zoteroError = zoteroError,
                    collectionLabel = selectedCollectionLabel,
                    onClearCollection = {
                        if (sourceFilter == "Zotero") onZoteroCollection(null)
                        else selectedLocalFolderId = null
                    },
                    onSelect = {
                        onSelect(it)
                        detailDialogPaper = it
                    },
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            }
        } else {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (sourceFilter != "All sources") {
                    LibraryCollectionPane(
                        sourceFilter = sourceFilter,
                        localFolders = localFolders,
                        zoteroGroups = zoteroGroups,
                        zoteroCollections = zoteroCollections,
                        selectedLocalFolderId = selectedLocalFolderId,
                        selectedZoteroGroupId = selectedZoteroGroupId,
                        selectedZoteroCollectionKey = selectedZoteroCollectionKey,
                        expandedLocalFolderIds = expandedLocalFolderIds,
                        expandedZoteroCollectionKeys = expandedZoteroCollectionKeys,
                        localPaperCount = localPaperCount,
                        localFolderCounts = localFolderCounts,
                        zoteroPaperCount = zoteroPaperCount,
                        zoteroCollectionCounts = zoteroCollectionCounts,
                        onLocalFolder = { selectedLocalFolderId = it },
                        onZoteroGroup = onZoteroGroup,
                        onZoteroCollection = onZoteroCollection,
                        onToggleLocalFolder = { id ->
                            expandedLocalFolderIds = expandedLocalFolderIds.toggle(id)
                        },
                        onToggleZoteroCollection = { key ->
                            expandedZoteroCollectionKeys = expandedZoteroCollectionKeys.toggle(key)
                        },
                        modifier = Modifier.width(240.dp).fillMaxHeight(),
                    )
                }
                LibraryPaperList(
                    query = query,
                    sourceFilter = sourceFilter,
                    filter = filter,
                    filtered = filtered,
                    selected = selected,
                    showLabels = showLabels,
                    onSourceFilter = { sourceFilter = it },
                    onFilter = { filter = it },
                    onImport = onImport,
                    zoteroLoading = zoteroLoading,
                    zoteroError = zoteroError,
                    collectionLabel = selectedCollectionLabel,
                    onClearCollection = {
                        if (sourceFilter == "Zotero") onZoteroCollection(null)
                        else selectedLocalFolderId = null
                    },
                    onSelect = {
                        onSelect(it)
                        detailDialogPaper = it
                    },
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            }
        }
        detailDialogPaper?.let { paper ->
            NativeDialog("Paper details", onClose = { detailDialogPaper = null }) {
                PaperDetails(paper, onOpen, Modifier.weight(1f).fillMaxWidth())
            }
        }
    }
}

@Composable
private fun LibraryPaperList(
    query: androidx.compose.foundation.text.input.TextFieldState,
    sourceFilter: String,
    filter: String,
    filtered: List<Paper>,
    selected: Paper?,
    showLabels: Boolean,
    zoteroLoading: Boolean,
    zoteroError: String?,
    collectionLabel: String?,
    onClearCollection: () -> Unit,
    onSourceFilter: (String) -> Unit,
    onFilter: (String) -> Unit,
    onImport: () -> Unit,
    onSelect: (Paper) -> Unit,
    modifier: Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TextField(query, Modifier.fillMaxWidth(), placeholder = { Text("Search title, author or tag") })
        if (collectionLabel != null) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                NativeChip(collectionLabel, NativeIcon.Folder, nativeAccent)
                NativeToolButton(
                    "Clear collection",
                    NativeIcon.Close,
                    compact = true,
                    onClick = onClearCollection,
                )
            }
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NativeChip("${filtered.size} results", NativeIcon.Search)
            NativeMenu(
                label = "Paper source: $sourceFilter",
                icon = NativeIcon.Library,
                showLabel = showLabels,
                actions = listOf(
                    NativeMenuAction("All sources", NativeIcon.Library, selected = sourceFilter == "All sources") {
                        onSourceFilter("All sources")
                    },
                    NativeMenuAction("Local", NativeIcon.Folder, selected = sourceFilter == "Local") {
                        onSourceFilter("Local")
                    },
                    NativeMenuAction("Zotero", NativeIcon.Library, selected = sourceFilter == "Zotero") {
                        onSourceFilter("Zotero")
                    },
                ),
            )
            NativeMenu(
                label = "Library filters",
                icon = NativeIcon.Filter,
                showLabel = showLabels,
                actions = listOf(
                    NativeMenuAction("All papers", NativeIcon.Library, selected = filter == "All papers") {
                        onFilter("All papers")
                    },
                    NativeMenuAction("With PDF", NativeIcon.File, selected = filter == "With PDF") {
                        onFilter("With PDF")
                    },
                    NativeMenuAction("Completed", NativeIcon.Check, selected = filter == "Completed") {
                        onFilter("Completed")
                    },
                ),
            )
            NativeToolButton("Import PDF", NativeIcon.Add, showLabel = showLabels, onClick = onImport)
        }
        if (sourceFilter == "Zotero" && zoteroError != null) {
            NativeChip(zoteroError, NativeIcon.Close, Color(0xFFE5534B))
        } else if (sourceFilter == "Zotero" && zoteroLoading) {
            NativeMeta("Loading Zotero library…")
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (filtered.isEmpty()) {
                NativeEmptyState(
                    when {
                        collectionLabel != null -> "No papers are in this collection."
                        sourceFilter == "Zotero" && zoteroError != null -> "Zotero could not be loaded."
                        sourceFilter == "Zotero" -> "No papers are available in this Zotero library."
                        sourceFilter == "Local" && filter == "With PDF" -> "No local papers have a PDF attached."
                        else -> "No matching papers."
                    },
                    Modifier.fillMaxWidth().weight(1f),
                )
            } else {
                filtered.forEach { paper ->
                    PaperCard(paper, selected = paper.id == selected?.id, onClick = { onSelect(paper) })
                }
            }
        }
    }
}

@Composable
private fun LibraryCollectionPane(
    sourceFilter: String,
    localFolders: List<LibraryEntry>,
    zoteroGroups: List<ZoteroGroup>,
    zoteroCollections: List<ZoteroCollection>,
    selectedLocalFolderId: String?,
    selectedZoteroGroupId: Int?,
    selectedZoteroCollectionKey: String?,
    expandedLocalFolderIds: Set<String>,
    expandedZoteroCollectionKeys: Set<String>,
    localPaperCount: Int,
    localFolderCounts: Map<String, Int>,
    zoteroPaperCount: Int,
    zoteroCollectionCounts: Map<String, Int>,
    onLocalFolder: (String?) -> Unit,
    onZoteroGroup: (Int?) -> Unit,
    onZoteroCollection: (String?) -> Unit,
    onToggleLocalFolder: (String) -> Unit,
    onToggleZoteroCollection: (String) -> Unit,
    modifier: Modifier,
) {
    NativeCard(modifier) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                NativeHeading(
                    if (sourceFilter == "Zotero") "Zotero collections" else "Local collections",
                    if (sourceFilter == "Zotero") {
                        zoteroGroups.firstOrNull { it.id == selectedZoteroGroupId }?.data?.name ?: "Personal library"
                    } else {
                        "Folders in this workspace"
                    },
                    Modifier.weight(1f),
                )
                if (sourceFilter == "Zotero") {
                    NativeMenu(
                        label = zoteroGroups.firstOrNull { it.id == selectedZoteroGroupId }?.data?.name
                            ?: "Personal library",
                        icon = NativeIcon.Library,
                        showLabel = true,
                        actions = listOf(
                            NativeMenuAction(
                                "Personal library",
                                NativeIcon.Library,
                                selected = selectedZoteroGroupId == null,
                            ) { onZoteroGroup(null) },
                        ) + zoteroGroups.map { group ->
                            NativeMenuAction(
                                group.data.name,
                                NativeIcon.Library,
                                selected = selectedZoteroGroupId == group.id,
                            ) { onZoteroGroup(group.id) }
                        },
                    )
                }
            }
            NativeDivider()
            if (sourceFilter == "Zotero") {
                NativeToolButton(
                    "All items ($zoteroPaperCount)",
                    NativeIcon.Library,
                    selected = selectedZoteroCollectionKey == null,
                    showLabel = true,
                ) { onZoteroCollection(null) }
                if (zoteroCollections.isEmpty()) {
                    NativeMeta("No collections found in this library.")
                } else {
                    zoteroCollectionRows(zoteroCollections, expandedZoteroCollectionKeys).forEach { (collection, depth) ->
                        CollectionTreeRow(
                            label = collection.data.name,
                            depth = depth,
                            count = zoteroCollectionCounts[collection.key] ?: 0,
                            selected = selectedZoteroCollectionKey == collection.key,
                            hasChildren = zoteroCollections.any { it.data.parentCollectionKey == collection.key },
                            expanded = collection.key in expandedZoteroCollectionKeys,
                            onSelect = { onZoteroCollection(collection.key) },
                            onToggle = { onToggleZoteroCollection(collection.key) },
                        )
                    }
                }
            } else {
                NativeToolButton(
                    "All papers ($localPaperCount)",
                    NativeIcon.Library,
                    selected = selectedLocalFolderId == null,
                    showLabel = true,
                ) { onLocalFolder(null) }
                localFolderRows(localFolders, expandedLocalFolderIds).forEach { (folder, depth) ->
                    CollectionTreeRow(
                        label = folder.label,
                        depth = depth,
                        count = localFolderCounts[folder.id] ?: 0,
                        selected = selectedLocalFolderId == folder.id,
                        hasChildren = localFolders.any { it.parentId == folder.id },
                        expanded = folder.id in expandedLocalFolderIds,
                        onSelect = { onLocalFolder(folder.id) },
                        onToggle = { onToggleLocalFolder(folder.id) },
                    )
                }
                if (localFolders.isEmpty()) NativeMeta("No local folders have been created.")
            }
        }
    }
}

@Composable
private fun CollectionTreeRow(
    label: String,
    depth: Int,
    count: Int,
    selected: Boolean,
    hasChildren: Boolean,
    expanded: Boolean,
    onSelect: () -> Unit,
    onToggle: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(start = (depth * 12).dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (hasChildren) {
            NativeToolButton(
                if (expanded) "Collapse $label" else "Expand $label",
                if (expanded) NativeIcon.Down else NativeIcon.Next,
                compact = true,
                onClick = onToggle,
            )
        } else {
            Spacer(Modifier.size(22.dp))
        }
        NativeToolButton(
            "$label ($count)",
            NativeIcon.Folder,
            selected = selected,
            showLabel = true,
            onClick = onSelect,
        )
    }
}

private fun localFolderRows(
    folders: List<LibraryEntry>,
    expanded: Set<String>,
): List<Pair<LibraryEntry, Int>> {
    val children = folders.groupBy { it.parentId }
    val rows = mutableListOf<Pair<LibraryEntry, Int>>()

    fun append(parentId: String?, depth: Int) {
        children[parentId].orEmpty().sortedBy { it.label.lowercase() }.forEach { folder ->
            rows += folder to depth
            if (folder.id in expanded) append(folder.id, depth + 1)
        }
    }

    append(null, 0)
    return rows
}

private fun zoteroCollectionRows(
    collections: List<ZoteroCollection>,
    expanded: Set<String>,
): List<Pair<ZoteroCollection, Int>> {
    val children = collections.groupBy { it.data.parentCollectionKey }
    val rows = mutableListOf<Pair<ZoteroCollection, Int>>()

    fun append(parentKey: String?, depth: Int) {
        children[parentKey].orEmpty().sortedBy { it.data.name.lowercase() }.forEach { collection ->
            rows += collection to depth
            if (collection.key in expanded) append(collection.key, depth + 1)
        }
    }

    append(null, 0)
    return rows
}

private fun Set<String>.toggle(value: String): Set<String> =
    if (value in this) this - value else this + value

private fun zoteroCollectionScope(
    selectedKey: String?,
    collections: List<ZoteroCollection>,
): Set<String> {
    if (selectedKey == null) return emptySet()
    val children = collections.groupBy { it.data.parentCollectionKey }
    val result = mutableSetOf(selectedKey)

    fun append(parentKey: String) {
        children[parentKey].orEmpty().forEach { child ->
            if (result.add(child.key)) append(child.key)
        }
    }

    append(selectedKey)
    return result
}

private fun libraryFolderScope(
    selectedId: String?,
    folders: List<LibraryEntry>,
): Set<String> {
    if (selectedId == null) return emptySet()
    val children = folders.groupBy { it.parentId }
    val result = mutableSetOf(selectedId)

    fun append(parentId: String) {
        children[parentId].orEmpty().forEach { child ->
            if (result.add(child.id)) append(child.id)
        }
    }

    append(selectedId)
    return result
}

@Composable
private fun PaperCard(paper: Paper, selected: Boolean = false, onClick: () -> Unit) {
    NativeCard(selected = selected, onClick = onClick) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
            NativeGlyph(NativeIcon.Book, tint = nativeAccent)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                NativeCardTitle(paper.title)
                NativeMeta(paper.authors.take(3).joinToString(", ").ifBlank { "Unknown authors" })
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    paper.year?.let { NativeChip(it.toString()) }
                    paper.source.takeIf(String::isNotBlank)?.let { NativeChip(it) }
                }
            }
        }
    }
}

@Composable
private fun PaperDetails(paper: Paper, onOpen: (Paper) -> Unit, modifier: Modifier) {
    Column(modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        NativeHeading(paper.title, paper.authors.joinToString(", ").ifBlank { "Unknown authors" })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DefaultButton(onClick = { onOpen(paper) }, enabled = !paper.pdfPath.isNullOrBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    NativeGlyph(NativeIcon.File)
                    Text(if (paper.pdfPath.isNullOrBlank()) "PDF unavailable" else "Open PDF")
                }
            }
            paper.completed.takeIf { it }?.let { NativeChip("Completed", NativeIcon.Check, Color(0xFF3FA36C)) }
            paper.year?.let { NativeChip(it.toString()) }
        }
        NativeDivider()
        MetadataRow("Source", paper.source)
        paper.doi?.let { MetadataRow("DOI", it) }
        paper.pdfPath?.let { MetadataRow("PDF", it) }
        if (paper.tags.isNotEmpty()) {
            NativeSectionLabel("Tags")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                paper.tags.forEach { NativeChip(it) }
            }
        }
        NativeSectionLabel("Abstract")
        Text(paper.abstractText?.takeIf(String::isNotBlank) ?: "No abstract was saved for this paper.", color = nativeForeground.copy(alpha = .82f))
    }
}

@Composable
private fun MetadataRow(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        NativeMeta(label, Modifier.width(72.dp))
        Text(value, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ReaderScreen(
    features: NativeFeatures,
) {
    if (features.paper == null) {
        NativeEmptyState("Select a paper before opening the reader.")
        return
    }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        features.error?.let {
            Text(it, color = Color(0xFFE5534B))
            OutlinedButton(onClick = { features.paper?.let(features::open) }) { Text("Retry opening PDF") }
        }
        if (features.openingPdf) Text("Opening PDF…")
        Box(Modifier.weight(1f).fillMaxWidth()) {
            key(features.paper?.id, features.paper?.pdfPath) {
                NativePdfWorkspace(features)
            }
        }
    }
}

@Composable
private fun GraphDetails(
    graph: ResearchGraph,
    papers: List<Paper>,
    onPaperClick: (Paper) -> Unit,
    modifier: Modifier,
) {
    val graphPapers = graph.paperIds.mapNotNull { id -> papers.firstOrNull { it.id == id } }
    Column(modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        NativeHeading(graph.name, "Updated ${graph.updatedAt?.let(::formatInstant) ?: "not recorded"}")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NativeChip("${graphPapers.size} papers", NativeIcon.Library)
            NativeChip("${graph.connections.size} connections", NativeIcon.Graph)
        }
        NativeDivider()
        NativeSectionLabel("Graph papers")
        if (graphPapers.isEmpty()) NativeEmptyState("This graph does not reference saved papers.", Modifier.fillMaxWidth())
        else graphPapers.forEach { paper -> PaperCard(paper, onClick = { onPaperClick(paper) }) }
    }
}

@Composable
private fun AssistantScreen(
    snapshot: PluginWorkspaceSnapshot,
    selectedSessionId: String?,
    onSelect: (AssistantSession) -> Unit,
) {
    val selected = snapshot.assistantSessions.firstOrNull { it.id == selectedSessionId } ?: snapshot.assistantSessions.firstOrNull()
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(Modifier.width(300.dp).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (snapshot.assistantSessions.isEmpty()) NativeEmptyState("No assistant sessions yet.", Modifier.fillMaxWidth().weight(1f))
            else snapshot.assistantSessions.forEach { session ->
                NativeCard(selected = session.id == selected?.id, onClick = { onSelect(session) }) {
                    NativeCardTitle(session.title)
                    NativeMeta("${session.messages.size} messages")
                }
            }
        }
        if (selected == null) NativeEmptyState("Select a session to read the conversation.", Modifier.weight(1f))
        else AssistantDetails(selected, Modifier.weight(1f))
    }
}

@Composable
private fun AssistantDetails(session: AssistantSession, modifier: Modifier) {
    Column(modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        NativeHeading(session.title, session.updatedAt?.let { "Updated ${formatInstant(it)}" })
        NativeDivider()
        if (session.messages.isEmpty()) NativeEmptyState("This session has no messages.", Modifier.fillMaxWidth())
        else session.messages.forEach { message -> MessageCard(message) }
    }
}

@Composable
private fun MessageCard(message: AssistantMessage) {
    NativeCard(selected = message.role == AssistantRole.ASSISTANT) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
            NativeGlyph(if (message.role == AssistantRole.ASSISTANT) NativeIcon.Assistant else NativeIcon.Idea, tint = nativeAccent)
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                NativeMeta(if (message.role == AssistantRole.ASSISTANT) "Assistant" else "You")
                Text(message.content)
                message.sentAt?.let { NativeMeta(formatInstant(it)) }
            }
        }
    }
}

@Composable
private fun HistoryScreen(
    papers: List<Paper>,
    onOpen: (Paper) -> Unit,
    onSelect: (Paper) -> Unit,
) {
    val recent = papers.filter { it.lastOpenedAt > 0L }.sortedByDescending { it.lastOpenedAt }
    if (recent.isEmpty()) {
        NativeEmptyState("Opened papers will appear here after you read them in the desktop app.")
        return
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        recent.forEach { paper ->
            NativeCard(onClick = { onSelect(paper) }) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NativeGlyph(NativeIcon.History, tint = nativeAccent)
                    Column(Modifier.weight(1f)) {
                        NativeCardTitle(paper.title)
                        NativeMeta("Opened ${formatMillis(paper.lastOpenedAt)}")
                    }
                    NativeToolButton("Open PDF", NativeIcon.File, enabled = !paper.pdfPath.isNullOrBlank(), compact = true) {
                        onOpen(paper)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    snapshot: PluginWorkspaceSnapshot,
    error: String?,
    onRefresh: () -> Unit,
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        NativeHeading("Local workspace", "The IntelliJ plugin reads the same local-first data as the PaperConnekt app.")
        NativeCard {
            NativeSectionLabel("Workspace path")
            Text(
                System.getProperty("paperconnekt.data.dir")
                    ?: File(System.getProperty("user.home"), ".paperconnekt-contest").absolutePath,
            )
            NativeMeta("workspace.json is read-only from the plugin; the app remains the source of truth.")
        }
        NativeCard {
            NativeSectionLabel("Indexed content")
            MetadataRow("Papers", snapshot.papers.size.toString())
            MetadataRow("Graphs", snapshot.graphs.size.toString())
            MetadataRow("Sessions", snapshot.assistantSessions.size.toString())
            MetadataRow("Library entries", snapshot.libraryEntries.size.toString())
        }
        if (error != null) NativeChip(error, NativeIcon.Close, Color(0xFFC75450))
        OutlinedButton(onClick = onRefresh) {
            NativeGlyph(NativeIcon.Refresh)
            Spacer(Modifier.width(6.dp))
            Text("Reload workspace")
        }
    }
}

private fun formatInstant(value: Instant): String =
    value.toString().replace('T', ' ').substringBefore('.').removeSuffix("Z")

private fun formatMillis(value: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(value))

private fun ZoteroItem.toContestPaper(groupId: Int?): Paper {
    val authors = data.creators.mapNotNull { creator ->
        creator.name ?: listOfNotNull(creator.firstName, creator.lastName)
            .joinToString(" ")
            .takeIf(String::isNotBlank)
    }
    return Paper(
        id = "zotero:${groupId ?: "personal"}:$key",
        title = data.title?.takeIf(String::isNotBlank) ?: "Untitled Zotero item",
        authors = authors,
        abstractText = data.abstractNote,
        year = data.date?.take(4)?.toIntOrNull(),
        doi = data.doi,
        tags = data.tags.map { it.tag },
        source = "Zotero",
        sourceUrl = data.url,
    )
}
