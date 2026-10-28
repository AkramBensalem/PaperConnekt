package com.paperconnekt.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.paperconnekt.domain.model.ConnectionType
import com.paperconnekt.domain.model.GraphConnection
import com.paperconnekt.domain.model.Paper
import com.paperconnekt.domain.model.ResearchGraph
import com.paperconnekt.storage.GraphExpansionDirection
import com.paperconnekt.storage.OpenAlexGraphProvider
import io.ktor.client.HttpClient
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

@Composable
fun GraphWorkspace(
    graph: ResearchGraph,
    papers: List<Paper>,
    compact: Boolean,
    isSaving: Boolean,
    errorMessage: String?,
    onBack: () -> Unit,
    onRename: () -> Unit,
    onSave: (ResearchGraph) -> Unit,
    onSavePaper: (Paper) -> Unit,
    onError: (String) -> Unit,
) {
    val graphPapers = papers.filter { it.id in graph.paperIds }
    val availablePapers = papers.filterNot { it.id in graph.paperIds }
    var selectedPaperId by remember(graph.id) { mutableStateOf<String?>(null) }
    var paperQuery by remember(graph.id) { mutableStateOf("") }
    var sortByTitle by remember(graph.id) { mutableStateOf(false) }
    var zoom by remember(graph.id) { mutableFloatStateOf(1f) }
    var showAddPaperDialog by remember(graph.id) { mutableStateOf(false) }
    var showConnectionDialog by remember(graph.id) { mutableStateOf(false) }
    var connectionToRemove by remember { mutableStateOf<GraphConnection?>(null) }
    var isExpanding by remember(graph.id) { mutableStateOf(false) }
    var autoExpansionAttempted by remember(graph.id) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val graphProvider = remember { OpenAlexGraphProvider(HttpClient()) }

    val selectedPaper = graphPapers.firstOrNull { it.id == selectedPaperId }
    val visiblePapers = graphPapers
        .filter {
            val query = paperQuery.trim().lowercase()
            query.isEmpty() ||
                it.title.lowercase().contains(query) ||
                it.authors.any { author -> author.lowercase().contains(query) }
        }
        .let { current -> if (sortByTitle) current.sortedBy { it.title.lowercase() } else current }

    val saveUpdated: (ResearchGraph) -> Unit = { updated ->
        onSave(updated.copy(updatedAt = Clock.System.now()))
    }
    val expandGraph: (GraphExpansionDirection) -> Unit = { direction ->
        val source = selectedPaper ?: graphPapers.firstOrNull()
        if (source == null) {
            onError("Add a paper to the graph before loading connections.")
        } else if (!isExpanding) {
            isExpanding = true
            scope.launch {
                runCatching { graphProvider.expand(source.id, direction) }
                    .onSuccess { expansion ->
                        expansion.papers.forEach(onSavePaper)
                        saveUpdated(
                            graph.copy(
                                paperIds = (graph.paperIds + expansion.papers.map(Paper::id)).distinct(),
                                connections = (graph.connections + expansion.connections).distinct(),
                            ),
                        )
                        if (expansion.papers.isEmpty()) {
                            onError("No ${direction.name.lowercase()} were found for this paper.")
                        }
                    }
                    .onFailure { onError(it.message ?: "Could not load graph connections.") }
                isExpanding = false
            }
        }
    }

    LaunchedEffect(graph.id) {
        if (!autoExpansionAttempted && graphPapers.size == 1 && graph.connections.isEmpty()) {
            autoExpansionAttempted = true
            val seed = graphPapers.single()
            if (seed.id.startsWith("W")) {
                isExpanding = true
                scope.launch {
                    var expandedGraph = graph
                    runCatching {
                        listOf(
                            GraphExpansionDirection.REFERENCES,
                            GraphExpansionDirection.CITATIONS,
                        ).forEach { direction ->
                            val expansion = graphProvider.expand(seed.id, direction)
                            expansion.papers.forEach(onSavePaper)
                            expandedGraph = expandedGraph.copy(
                                paperIds = (expandedGraph.paperIds + expansion.papers.map(Paper::id)).distinct(),
                                connections = (expandedGraph.connections + expansion.connections).distinct(),
                            )
                        }
                    }.onSuccess {
                        saveUpdated(expandedGraph)
                    }.onFailure {
                        onError(it.message ?: "Could not load the paper's citation graph.")
                    }
                    isExpanding = false
                }
            } else {
                onError("This local paper has no OpenAlex work id, so references and citations are unavailable.")
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = if (compact) 16.dp else 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack) { Text("Back to graphs") }
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(graph.name, style = MaterialTheme.typography.headlineSmall)
                Text(
                    "${graphPapers.size} papers  •  ${graph.connections.size} connections",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedButton(onClick = onRename) { Text("Rename") }
        }

        if (isSaving) {
            GraphStatusCard(message = "Saving graph changes locally…", isError = false)
        }
        errorMessage?.let { message ->
            GraphStatusCard(message = message, isError = true)
        }

        if (compact) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                GraphCanvasPanel(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(360.dp),
                    graph = graph,
                    papers = graphPapers,
                    selectedPaperId = selectedPaperId,
                    zoom = zoom,
                    onZoomChange = { zoom = it },
                    onSelectPaper = { selectedPaperId = it },
                )
                GraphDetailsPanel(
                    graph = graph,
                    papers = visiblePapers,
                    allPapers = graphPapers,
                    selectedPaper = selectedPaper,
                    paperQuery = paperQuery,
                    sortByTitle = sortByTitle,
                    onPaperQueryChange = { paperQuery = it },
                    onSortChange = { sortByTitle = !sortByTitle },
                    onSelectPaper = { selectedPaperId = it },
                    onAddPaper = { showAddPaperDialog = true },
                    onRemovePaper = { paperId ->
                        selectedPaperId = null
                        saveUpdated(
                            graph.copy(
                                paperIds = graph.paperIds.filterNot { it == paperId },
                                connections = graph.connections.filter {
                                    it.fromPaperId != paperId && it.toPaperId != paperId
                                },
                            ),
                        )
                    },
                    onConnect = { showConnectionDialog = true },
                    onExpand = expandGraph,
                    isExpanding = isExpanding,
                    onRemoveConnection = { connectionToRemove = it },
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                GraphCanvasPanel(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    graph = graph,
                    papers = graphPapers,
                    selectedPaperId = selectedPaperId,
                    zoom = zoom,
                    onZoomChange = { zoom = it },
                    onSelectPaper = { selectedPaperId = it },
                )
                GraphDetailsPanel(
                    modifier = Modifier.width(320.dp).fillMaxHeight(),
                    graph = graph,
                    papers = visiblePapers,
                    allPapers = graphPapers,
                    selectedPaper = selectedPaper,
                    paperQuery = paperQuery,
                    sortByTitle = sortByTitle,
                    onPaperQueryChange = { paperQuery = it },
                    onSortChange = { sortByTitle = !sortByTitle },
                    onSelectPaper = { selectedPaperId = it },
                    onAddPaper = { showAddPaperDialog = true },
                    onRemovePaper = { paperId ->
                        selectedPaperId = null
                        saveUpdated(
                            graph.copy(
                                paperIds = graph.paperIds.filterNot { it == paperId },
                                connections = graph.connections.filter {
                                    it.fromPaperId != paperId && it.toPaperId != paperId
                                },
                            ),
                        )
                    },
                    onConnect = { showConnectionDialog = true },
                    onExpand = expandGraph,
                    isExpanding = isExpanding,
                    onRemoveConnection = { connectionToRemove = it },
                )
            }
        }
    }

    if (showAddPaperDialog) {
        AddGraphPapersDialog(
            papers = availablePapers,
            onDismiss = { showAddPaperDialog = false },
            onAdd = { paper ->
                saveUpdated(graph.copy(paperIds = (graph.paperIds + paper.id).distinct()))
                selectedPaperId = paper.id
                showAddPaperDialog = false
            },
        )
    }

    if (showConnectionDialog) {
        CreateConnectionDialog(
            papers = graphPapers,
            sourcePaper = selectedPaper,
            existingConnections = graph.connections,
            onDismiss = { showConnectionDialog = false },
            onCreate = { targetId, type ->
                val sourceId = selectedPaperId
                if (sourceId == null || sourceId == targetId) {
                    onError("Select one paper and a different target to create a connection.")
                } else {
                    saveUpdated(
                        graph.copy(
                            connections = (
                                graph.connections + GraphConnection(
                                    fromPaperId = sourceId,
                                    toPaperId = targetId,
                                    type = type,
                                )
                            ).distinct(),
                        ),
                    )
                }
                showConnectionDialog = false
            },
        )
    }

    connectionToRemove?.let { connection ->
        AlertDialog(
            onDismissRequest = { connectionToRemove = null },
            title = { Text("Remove connection?") },
            text = { Text("This only removes the relationship from this graph.") },
            confirmButton = {
                Button(
                    onClick = {
                        saveUpdated(graph.copy(connections = graph.connections - connection))
                        connectionToRemove = null
                    },
                ) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { connectionToRemove = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun GraphDetailsPanel(
    modifier: Modifier = Modifier,
    graph: ResearchGraph,
    papers: List<Paper>,
    allPapers: List<Paper>,
    selectedPaper: Paper?,
    paperQuery: String,
    sortByTitle: Boolean,
    onPaperQueryChange: (String) -> Unit,
    onSortChange: () -> Unit,
    onSelectPaper: (String) -> Unit,
    onAddPaper: () -> Unit,
    onRemovePaper: (String) -> Unit,
    onConnect: () -> Unit,
    onExpand: (GraphExpansionDirection) -> Unit,
    isExpanding: Boolean,
    onRemoveConnection: (GraphConnection) -> Unit,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text("Graph guide", style = MaterialTheme.typography.titleMedium)
                Text(
                    graphGuide(graph, allPapers),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        modifier = Modifier.weight(1f),
                        value = paperQuery,
                        onValueChange = onPaperQueryChange,
                        label = { Text("Filter papers") },
                        singleLine = true,
                    )
                    TextButton(onClick = onSortChange) {
                        Text(if (sortByTitle) "A-Z" else "Recent")
                    }
                }
            }
            item {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onAddPaper,
                ) {
                    Text("Add saved paper")
                }
            }
            if (papers.isEmpty()) {
                item {
                    Text(
                        if (allPapers.isEmpty()) {
                            "No papers in this graph yet. Add one from your saved collection."
                        } else {
                            "No papers match this filter."
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                item {
                    Text("Papers", style = MaterialTheme.typography.titleSmall)
                }
                items(papers, key = Paper::id) { paper ->
                    GraphPaperRow(
                        paper = paper,
                        selected = paper.id == selectedPaper?.id,
                        onSelect = { onSelectPaper(paper.id) },
                        onRemove = { onRemovePaper(paper.id) },
                    )
                }
            }
            if (selectedPaper != null) {
                item {
                    SelectedPaperCard(
                        paper = selectedPaper,
                        graph = graph,
                        onConnect = onConnect,
                        onExpand = onExpand,
                        isExpanding = isExpanding,
                    )
                }
            }
            item {
                Text("Connections", style = MaterialTheme.typography.titleSmall)
            }
            if (graph.connections.isEmpty()) {
                item {
                    Text(
                        "Connections will appear here as you map citations, shared references, or related topics.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(graph.connections) { connection ->
                    ConnectionRow(
                        connection = connection,
                        papers = allPapers,
                        onRemove = { onRemoveConnection(connection) },
                    )
                }
            }
        }
    }
}

@Composable
private fun GraphPaperRow(
    paper: Paper,
    selected: Boolean,
    onSelect: () -> Unit,
    onRemove: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(paper.title, style = MaterialTheme.typography.bodyMedium)
                Text(
                    paper.authors.take(2).joinToString().ifEmpty { "Author details unavailable" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onSelect) { Text(if (selected) "Selected" else "Inspect") }
            TextButton(onClick = onRemove) { Text("Remove") }
        }
    }
}

@Composable
private fun SelectedPaperCard(
    paper: Paper,
    graph: ResearchGraph,
    onConnect: () -> Unit,
    onExpand: (GraphExpansionDirection) -> Unit,
    isExpanding: Boolean,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("Selected paper", style = MaterialTheme.typography.labelLarge)
            Text(paper.title, style = MaterialTheme.typography.titleSmall)
            Text(
                listOfNotNull(paper.year?.toString(), paper.doi).joinToString("  •  ")
                    .ifEmpty { "Local metadata only" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                listOfNotNull(
                    paper.citationCount?.let { "$it citations" },
                    paper.referenceCount?.let { "$it references" },
                ).ifEmpty { listOf("Citation counts unavailable") }.joinToString("  •  "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Button(onClick = onConnect) { Text("Create connection") }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(
                    onClick = { onExpand(GraphExpansionDirection.REFERENCES) },
                    enabled = !isExpanding,
                ) { Text("References") }
                TextButton(
                    onClick = { onExpand(GraphExpansionDirection.CITATIONS) },
                    enabled = !isExpanding,
                ) { Text("Citations") }
            }
            if (isExpanding) {
                Text("Loading citation connections…", style = MaterialTheme.typography.bodySmall)
            }
            if (graph.connections.none { it.fromPaperId == paper.id || it.toPaperId == paper.id }) {
                Text(
                    "Insight: this paper has no mapped relationships yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun ConnectionRow(
    connection: GraphConnection,
    papers: List<Paper>,
    onRemove: () -> Unit,
) {
    val from = papers.firstOrNull { it.id == connection.fromPaperId }?.title ?: connection.fromPaperId
    val to = papers.firstOrNull { it.id == connection.toPaperId }?.title ?: connection.toPaperId
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("${from.take(26)} -> ${to.take(26)}", style = MaterialTheme.typography.bodySmall)
            Text(
                connection.type.label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        TextButton(onClick = onRemove) { Text("Remove") }
    }
}

@Composable
private fun AddGraphPapersDialog(
    papers: List<Paper>,
    onDismiss: () -> Unit,
    onAdd: (Paper) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add saved papers") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (papers.isEmpty()) {
                    item { Text("All saved papers are already in this graph.") }
                } else {
                    items(papers, key = Paper::id) { paper ->
                        TextButton(onClick = { onAdd(paper) }) {
                            Text(paper.title)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
private fun CreateConnectionDialog(
    papers: List<Paper>,
    sourcePaper: Paper?,
    existingConnections: List<GraphConnection>,
    onDismiss: () -> Unit,
    onCreate: (String, ConnectionType) -> Unit,
) {
    var targetId by remember(sourcePaper?.id) { mutableStateOf(papers.firstOrNull { it.id != sourcePaper?.id }?.id) }
    var type by remember { mutableStateOf(ConnectionType.RELATED_TOPIC) }
    var typeMenuExpanded by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create connection") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(sourcePaper?.title ?: "Select a source paper first")
                if (papers.none { it.id != sourcePaper?.id }) {
                    Text("Add another paper to this graph before creating a connection.")
                } else {
                    papers.filter { it.id != sourcePaper?.id }.forEach { paper ->
                        FilterChip(
                            selected = targetId == paper.id,
                            onClick = { targetId = paper.id },
                            label = { Text(paper.title.take(34)) },
                        )
                    }
                    Box {
                        OutlinedButton(onClick = { typeMenuExpanded = true }) {
                            Text(type.label)
                        }
                        DropdownMenu(
                            expanded = typeMenuExpanded,
                            onDismissRequest = { typeMenuExpanded = false },
                        ) {
                            ConnectionType.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.label) },
                                    onClick = {
                                        type = option
                                        typeMenuExpanded = false
                                    },
                                )
                            }
                        }
                    }
                    if (targetId != null && existingConnections.any {
                            it.fromPaperId == sourcePaper?.id && it.toPaperId == targetId
                        }) {
                        Text(
                            "This relationship already exists.",
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = sourcePaper != null && targetId != null && papers.any { it.id != sourcePaper.id },
                onClick = { targetId?.let { onCreate(it, type) } },
            ) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private val ConnectionType.label: String
    get() = when (this) {
        ConnectionType.CITATION -> "Citation"
        ConnectionType.SHARED_REFERENCE -> "Shared reference"
        ConnectionType.RELATED_TOPIC -> "Related topic"
    }

private fun graphGuide(graph: ResearchGraph, papers: List<Paper>): String {
    return when {
        papers.isEmpty() -> "Add saved papers to build a visual map. Graphs stay local and can be shaped around any research question."
        graph.connections.isEmpty() -> "Select a node, inspect its metadata, then connect it to another paper with a relationship type."
        else -> {
            val connectionTypes = graph.connections.map { it.type }.distinct().size
            "You have mapped ${graph.connections.size} relationship${if (graph.connections.size == 1) "" else "s"} across $connectionTypes type${if (connectionTypes == 1) "" else "s"}. Follow the most connected paper to find a useful reading path."
        }
    }
}
