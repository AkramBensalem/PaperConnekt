package com.paperconnekt.idea

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.intellij.ide.BrowserUtil
import com.paperconnekt.app.legacygraph.*
import com.paperconnekt.app.legacygraph.components.GraphCanvas
import com.paperconnekt.app.legacygraph.storage.SavedGraphNodePosition
import com.paperconnekt.domain.model.ConnectionType
import com.paperconnekt.domain.model.GraphConnection
import com.paperconnekt.domain.model.GraphNodePosition
import com.paperconnekt.domain.model.Paper
import com.paperconnekt.domain.model.ResearchGraph
import com.paperconnekt.plugins.common.PluginWorkspaceSnapshot
import com.paperconnekt.storage.GraphExpansionDirection
import com.paperconnekt.storage.OpenAlexGraphProvider
import com.paperconnekt.storage.OpenAlexPaperSearchProvider
import com.paperconnekt.secrets.SecretKey
import com.paperconnekt.secrets.SecretStore
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import org.jetbrains.jewel.ui.component.DefaultButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField
import java.util.UUID

/** Reference native workspace controls over the app's graph engine and local graph contract. */
@Composable
@OptIn(FlowPreview::class)
internal fun NativeGraphWorkspace(
    snapshot: PluginWorkspaceSnapshot,
    store: NativeContestStore,
    secrets: SecretStore,
    selectedGraphId: String?,
    onSelectGraph: (String) -> Unit,
    onOpenPaper: (Paper) -> Unit,
    onPersistPositions: (String, Map<String, GraphNodePosition>) -> Unit,
    onError: (String, Exception) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val client = remember { HttpClient(CIO) { expectSuccess = true } }
    DisposableEffect(client) { onDispose { client.close() } }
    var history by remember { mutableStateOf(false) }
    var picking by remember { mutableStateOf<String?>(null) }
    var renaming by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var connecting by remember { mutableStateOf(false) }
    val selected = snapshot.graphs.firstOrNull { it.id == selectedGraphId } ?: snapshot.graphs.firstOrNull()
    fun act(label: String, action: suspend () -> Unit) {
        scope.launch {
            busy = true
            try {
                action()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                onError(label, exception)
            } finally {
                busy = false
            }
        }
    }

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        NativeActions {
            NativeToolButton("New graph", NativeIcon.Add, enabled = !busy, showLabel = true) { picking = "Create" }
            NativeToolButton("Saved graphs", NativeIcon.History, selected = history, showLabel = true) { history = !history }
            selected?.let { graph ->
                Text(graph.name, Modifier.weight(1f), maxLines = 1)
                NativeMenu("Graph actions", listOf(
                    NativeMenuAction("Rename graph", NativeIcon.Edit, enabled = !busy) { renaming = true },
                    NativeMenuAction("Add a paper", NativeIcon.Add, enabled = !busy) { picking = "Add" },
                    NativeMenuAction("Connect papers", NativeIcon.Graph, enabled = !busy && graph.paperIds.size > 1) { connecting = true },
                    NativeMenuAction("Delete graph", NativeIcon.Trash, enabled = !busy) {
                        if (confirmNative("Delete \"${graph.name}\"? The Library papers will be kept.")) {
                            act("Could not delete graph") { store.removeGraph(graph.id) }
                        }
                    },
                ))
            }
        }
        if (busy) NativeMeta("Updating graph…")
        when {
            history -> LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(snapshot.graphs.sortedByDescending { it.updatedAt }, key = { it.id }) { graph ->
                    NativeCard(selected = graph.id == selected?.id, onClick = { onSelectGraph(graph.id); history = false }) {
                        NativeCardTitle(graph.name)
                        NativeMeta("${graph.paperIds.size} papers · ${graph.connections.size} connections")
                    }
                }
            }
            selected == null -> NativeEmptyState(
                "Start a graph with a Library paper or search OpenAlex for a seed.",
                Modifier.weight(1f).fillMaxWidth(),
            ) {
                DefaultButton(onClick = { picking = "Create" }) { Text("New graph") }
            }
            else -> key(selected.id) {
                val papers = (snapshot.papers + selected.discoveredPapers).distinctBy(Paper::id)
                val graph = remember(selected.paperIds, selected.connections, papers) { selected.toLegacyGraph(papers) }
                var nodeId by remember { mutableStateOf<String?>(null) }
                var cited by remember { mutableStateOf(true) }
                var citing by remember { mutableStateOf(true) }
                var interconnections by remember { mutableStateOf(true) }
                var minimumCitations by remember { mutableIntStateOf(0) }
                var sort by remember { mutableStateOf(GraphNodeSortOrder.DEFAULT) }
                var hidden by remember { mutableStateOf(emptySet<String>()) }
                var positions by remember { mutableStateOf(selected.nodePositions) }
                val initialPositions = remember { selected.nodePositions.mapValues { SavedGraphNodePosition(it.value.x, it.value.y) } }
                val node = graph.nodes.firstOrNull { it.paper.id == nodeId }
                val visible = remember(graph, cited, citing, interconnections, minimumCitations, sort, hidden) {
                    filterGraph(graph, citing, cited, minimumCitations, Int.MAX_VALUE, 0,
                        interconnections, false, hidden, sort)
                }
                val persist by rememberUpdatedState(onPersistPositions)
                LaunchedEffect(selected.id) {
                    snapshotFlow { positions }.debounce(700).collect { updated ->
                        if (updated != selected.nodePositions) persist(selected.id, updated)
                    }
                }
                DisposableEffect(selected.id) {
                    onDispose { if (positions != selected.nodePositions) persist(selected.id, positions) }
                }
                NativeActions {
                    NativeToolButton("References", NativeIcon.ArrowRight, selected = cited, showLabel = true) { cited = !cited }
                    NativeToolButton("Citations", NativeIcon.ArrowLeft, selected = citing, showLabel = true) { citing = !citing }
                    NativeToolButton("Interconnections", NativeIcon.Graph, selected = interconnections) { interconnections = !interconnections }
                    NativeMenu("Minimum citations: $minimumCitations", listOf(0, 10, 50, 100, 500).map { count ->
                        NativeMenuAction(count.toString(), NativeIcon.Filter, selected = minimumCitations == count) { minimumCitations = count }
                    }, icon = NativeIcon.Filter)
                    NativeMenu("Sort papers", GraphNodeSortOrder.entries.map { order ->
                        NativeMenuAction(order.name.lowercase().replaceFirstChar(Char::uppercase), NativeIcon.Filter,
                            selected = sort == order) { sort = order }
                    }, icon = NativeIcon.Filter)
                    if (hidden.isNotEmpty()) NativeToolButton("Show hidden papers", NativeIcon.Refresh, showLabel = true) { hidden = emptySet() }
                    NativeMeta("${visible.nodes.size} papers · ${visible.edges.size} connections")
                }
                GraphCanvas(
                    graph = visible,
                    onNodeClick = { nodeId = it?.paper?.id },
                    selectedNode = node,
                    disqualifiedPaperIds = hidden,
                    initialNodePositions = initialPositions,
                    onPositionsChanged = { values ->
                        positions = positions + values.mapValues { GraphNodePosition(it.value.x, it.value.y) }
                    },
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
                node?.let { picked ->
                    NativeCard {
                        NativeCardTitle(picked.paper.title)
                        NativeMeta(picked.paper.authors.joinToString(", "))
                        NativeActions {
                            NativeToolButton("Read PDF", NativeIcon.Book, enabled = picked.paper.pdfPath != null, showLabel = true) {
                                onOpenPaper(picked.paper)
                            }
                            NativeToolButton("Save to Library", NativeIcon.Library, enabled = !busy, showLabel = true) {
                                act("Could not save paper") { store.savePaper(picked.paper) }
                            }
                            NativeToolButton("Paper website", NativeIcon.External, enabled = picked.paper.sourceUrl != null) {
                                picked.paper.sourceUrl?.let(BrowserUtil::browse)
                            }
                            val canExpand = !busy && picked.paper.id.substringAfterLast('/').startsWith("W")
                            GraphExpansionDirection.entries.forEach { direction ->
                                NativeToolButton("Expand ${direction.name.lowercase()}", NativeIcon.Add, enabled = canExpand, showLabel = true) {
                                    act("Could not expand the graph") {
                                        val expansion = OpenAlexGraphProvider(client, secrets.read(SecretKey.OPENALEX))
                                            .expand(picked.paper.id, direction)
                                        store.editGraph(selected.id) { current ->
                                            current.copy(
                                                paperIds = (current.paperIds + expansion.papers.map(Paper::id)).distinct(),
                                                discoveredPapers = (current.discoveredPapers + expansion.papers).distinctBy(Paper::id),
                                                connections = (current.connections + expansion.connections).distinct(),
                                            )
                                        }
                                    }
                                }
                            }
                            NativeMenu("Paper actions", listOf(
                                NativeMenuAction("Hide paper", NativeIcon.Close, enabled = picked.connectionType != com.paperconnekt.app.legacygraph.ConnectionType.CENTRAL) {
                                    hidden = hidden + picked.paper.id
                                    nodeId = null
                                },
                                NativeMenuAction("Remove from graph", NativeIcon.Trash, enabled = !busy) {
                                    act("Could not remove paper") {
                                        store.editGraph(selected.id) { current ->
                                            current.copy(
                                                paperIds = current.paperIds - picked.paper.id,
                                                seedPaperIds = current.seedPaperIds - picked.paper.id,
                                                connections = current.connections.filterNot { it.fromPaperId == picked.paper.id || it.toPaperId == picked.paper.id },
                                                discoveredPapers = current.discoveredPapers.filterNot { it.id == picked.paper.id },
                                                nodePositions = current.nodePositions - picked.paper.id,
                                            )
                                        }
                                    }
                                },
                            ))
                        }
                        if (!picked.paper.id.substringAfterLast('/').startsWith("W")) {
                            NativeMeta("Citation expansion needs an OpenAlex paper. Local papers can be connected manually.")
                        }
                    }
                }
            }
        }
    }

    picking?.let { mode ->
        NativeGraphPaperPicker(snapshot.papers, client, secrets, onClose = { picking = null }) { paper ->
            picking = null
            act("Could not add graph seed") {
                if (mode == "Create") {
                    val graph = ResearchGraph(
                        id = UUID.randomUUID().toString(),
                        name = paper.title,
                        paperIds = listOf(paper.id),
                        seedPaperIds = listOf(paper.id),
                        discoveredPapers = listOf(paper),
                        updatedAt = kotlinx.datetime.Clock.System.now(),
                    )
                    store.saveGraph(graph)
                    onSelectGraph(graph.id)
                    history = false
                } else selected?.let { graph ->
                    store.editGraph(graph.id) { current ->
                        current.copy(
                            paperIds = (current.paperIds + paper.id).distinct(),
                            seedPaperIds = (current.seedPaperIds.ifEmpty { current.paperIds.take(1) } + paper.id).distinct(),
                            discoveredPapers = (current.discoveredPapers + paper).distinctBy(Paper::id),
                        )
                    }
                }
            }
        }
    }
    if (renaming && selected != null) {
        val name = rememberTextFieldState(selected.name)
        NativeDialog("Rename graph", onClose = { renaming = false }) {
            TextField(name, Modifier.fillMaxWidth())
            DefaultButton(enabled = name.text.isNotBlank(), onClick = {
                renaming = false
                act("Could not rename graph") { store.editGraph(selected.id) { it.copy(name = name.text.toString().trim()) } }
            }) { Text("Save") }
        }
    }
    if (connecting && selected != null) {
        val papers = (snapshot.papers + selected.discoveredPapers).distinctBy(Paper::id).filter { it.id in selected.paperIds }
        NativeGraphConnectionDialog(papers, onClose = { connecting = false }) { connection ->
            connecting = false
            act("Could not connect papers") {
                store.editGraph(selected.id) { current ->
                    require(connection.fromPaperId in current.paperIds && connection.toPaperId in current.paperIds) { "A selected paper was removed." }
                    current.copy(connections = (current.connections + connection).distinct())
                }
            }
        }
    }
}

@Composable
private fun NativeGraphPaperPicker(library: List<Paper>, client: HttpClient, secrets: SecretStore, onClose: () -> Unit, onPick: (Paper) -> Unit) {
    val query = rememberTextFieldState()
    val scope = rememberCoroutineScope()
    var results by remember { mutableStateOf(emptyList<Paper>()) }
    var searching by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    NativeDialog("Choose a graph seed", onClose = onClose, maxDialogWidth = 680.dp, maxDialogHeight = 600.dp) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextField(query, Modifier.weight(1f), placeholder = { Text("Search the Library or OpenAlex") })
            DefaultButton(enabled = !searching && query.text.isNotBlank(), onClick = {
                scope.launch {
                    searching = true
                    error = null
                    try {
                        val requested = query.text.toString().trim()
                        val found = OpenAlexPaperSearchProvider(client, secrets.read(SecretKey.OPENALEX)).search(requested)
                        if (query.text.toString().trim() == requested) results = found
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (exception: Exception) {
                        error = exception.message ?: "OpenAlex search failed."
                        com.intellij.openapi.diagnostic.Logger.getInstance("PaperConnekt.Graph").warn("OpenAlex search failed", exception)
                    } finally {
                        searching = false
                    }
                }
            }) { Text(if (searching) "Searching…" else "Search OpenAlex") }
        }
        NativeMeta("Library filtering is local. Search OpenAlex sends only the query to OpenAlex.")
        error?.let { Text(it) }
        val text = query.text.toString().trim()
        val local = library.filter { text.isBlank() || it.title.contains(text, true) || it.authors.any { author -> author.contains(text, true) } }
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items((local + results).distinctBy(Paper::id), key = Paper::id) { paper ->
                NativeCard(onClick = { onPick(paper) }) {
                    NativeCardTitle(paper.title)
                    NativeMeta("${paper.year ?: ""} ${paper.authors.joinToString(", ")}")
                }
            }
        }
    }
}

@Composable
private fun NativeGraphConnectionDialog(papers: List<Paper>, onClose: () -> Unit, onConnect: (GraphConnection) -> Unit) {
    var from by remember { mutableStateOf(papers.firstOrNull()?.id) }
    var to by remember { mutableStateOf(papers.drop(1).firstOrNull()?.id) }
    var type by remember { mutableStateOf(ConnectionType.CITATION) }
    NativeDialog("Connect papers", onClose = onClose) {
        Text("From")
        NativeMenu(papers.firstOrNull { it.id == from }?.title ?: "Choose a paper", papers.map { paper ->
            NativeMenuAction(paper.title, NativeIcon.Book, selected = paper.id == from) { from = paper.id }
        }, showLabel = true)
        Text("To")
        NativeMenu(papers.firstOrNull { it.id == to }?.title ?: "Choose a paper", papers.map { paper ->
            NativeMenuAction(paper.title, NativeIcon.Book, selected = paper.id == to) { to = paper.id }
        }, showLabel = true)
        NativeMenu(type.name, ConnectionType.entries.map { value ->
            NativeMenuAction(value.name, NativeIcon.Graph, selected = value == type) { type = value }
        }, showLabel = true)
        DefaultButton(enabled = from != null && to != null && from != to, onClick = {
            onConnect(GraphConnection(checkNotNull(from), checkNotNull(to), type))
        }) { Text("Connect") }
    }
}
