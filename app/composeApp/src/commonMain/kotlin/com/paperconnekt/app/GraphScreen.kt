package com.paperconnekt.app

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.paperconnekt.domain.model.ResearchGraph
import com.paperconnekt.storage.LocalResearchStore
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlin.random.Random

@Composable
fun GraphScreen(
    store: LocalResearchStore,
    paddingValues: PaddingValues,
) {
    val graphs by store.graphs.collectAsState()
    val papers by store.papers.collectAsState()
    var selectedGraphId by remember { mutableStateOf<String?>(null) }
    var graphQuery by remember { mutableStateOf("") }
    var sortOrder by remember { mutableStateOf(GraphSortOrder.RECENT) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var graphBeingRenamed by remember { mutableStateOf<ResearchGraph?>(null) }
    var graphBeingDeleted by remember { mutableStateOf<ResearchGraph?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val selectedGraph = graphs.firstOrNull { it.id == selectedGraphId }
    val normalizedQuery = graphQuery.trim().lowercase()
    val visibleGraphs = graphs
        .filter { normalizedQuery.isEmpty() || it.name.lowercase().contains(normalizedQuery) }
        .let { current ->
            when (sortOrder) {
                GraphSortOrder.RECENT -> current.sortedByDescending { it.updatedAt }
                GraphSortOrder.NAME -> current.sortedBy { it.name.lowercase() }
                GraphSortOrder.PAPERS -> current.sortedByDescending { it.paperIds.size }
            }
        }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
    ) {
        if (selectedGraph != null) {
            GraphWorkspace(
                graph = selectedGraph,
                papers = papers,
                compact = maxWidth < 860.dp,
                isSaving = isSaving,
                errorMessage = errorMessage,
                onBack = { selectedGraphId = null },
                onRename = { graphBeingRenamed = selectedGraph },
                onSave = { updatedGraph ->
                    isSaving = true
                    scope.launch {
                        runCatching { store.saveGraph(updatedGraph) }
                            .onFailure { errorMessage = it.message ?: "Could not save graph changes." }
                            .also { isSaving = false }
                    }
                },
                onError = {
                    errorMessage = it
                    isSaving = false
                },
            )
        } else {
            GraphList(
                graphs = visibleGraphs,
                query = graphQuery,
                sortOrder = sortOrder,
                errorMessage = errorMessage,
                onQueryChange = {
                    graphQuery = it
                    errorMessage = null
                },
                onSortOrderChange = { sortOrder = it },
                onCreate = { showCreateDialog = true },
                onOpen = { selectedGraphId = it },
                onRename = { graphBeingRenamed = it },
                onDelete = { graphBeingDeleted = it },
            )
        }
    }

    if (showCreateDialog) {
        GraphNameDialog(
            title = "Create research graph",
            confirmLabel = "Create graph",
            onDismiss = { showCreateDialog = false },
            onConfirm = { name ->
                val graph = ResearchGraph(
                    id = "graph-${Random.nextLong()}",
                    name = name,
                    updatedAt = Clock.System.now(),
                )
                scope.launch {
                    runCatching { store.saveGraph(graph) }
                        .onSuccess {
                            selectedGraphId = graph.id
                            showCreateDialog = false
                        }
                        .onFailure {
                            errorMessage = it.message ?: "Could not create graph."
                        }
                }
            },
        )
    }

    graphBeingRenamed?.let { graph ->
        GraphNameDialog(
            title = "Rename graph",
            confirmLabel = "Save name",
            initialName = graph.name,
            onDismiss = { graphBeingRenamed = null },
            onConfirm = { name ->
                scope.launch {
                    runCatching {
                        store.saveGraph(graph.copy(name = name, updatedAt = Clock.System.now()))
                    }.onFailure {
                        errorMessage = it.message ?: "Could not rename graph."
                    }
                    graphBeingRenamed = null
                }
            },
        )
    }

    graphBeingDeleted?.let { graph ->
        AlertDialog(
            onDismissRequest = { graphBeingDeleted = null },
            title = { Text("Delete ${graph.name}?") },
            text = { Text("This removes the graph and its local connections. Saved papers are not deleted.") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            runCatching { store.removeGraph(graph.id) }
                                .onFailure {
                                    errorMessage = it.message ?: "Could not delete graph."
                                }
                            graphBeingDeleted = null
                        }
                    },
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { graphBeingDeleted = null }) {
                    Text("Cancel")
                }
            },
        )
    }
}

private enum class GraphSortOrder {
    RECENT,
    NAME,
    PAPERS,
}

@Composable
private fun GraphList(
    graphs: List<ResearchGraph>,
    query: String,
    sortOrder: GraphSortOrder,
    errorMessage: String?,
    onQueryChange: (String) -> Unit,
    onSortOrderChange: (GraphSortOrder) -> Unit,
    onCreate: () -> Unit,
    onOpen: (String) -> Unit,
    onRename: (ResearchGraph) -> Unit,
    onDelete: (ResearchGraph) -> Unit,
) {
    var sortMenuExpanded by remember { mutableStateOf(false) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Research graphs", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Map the papers behind a question, chapter, or research direction.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Button(onClick = onCreate) {
                    Text("New graph")
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    modifier = Modifier.weight(1f),
                    value = query,
                    onValueChange = onQueryChange,
                    label = { Text("Search graphs") },
                    singleLine = true,
                )
                Column {
                    OutlinedButton(onClick = { sortMenuExpanded = true }) {
                        Text("Sort")
                    }
                    DropdownMenu(
                        expanded = sortMenuExpanded,
                        onDismissRequest = { sortMenuExpanded = false },
                    ) {
                        GraphSortOrder.entries.forEach { order ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        when (order) {
                                            GraphSortOrder.RECENT -> "Recently updated"
                                            GraphSortOrder.NAME -> "Name"
                                            GraphSortOrder.PAPERS -> "Most papers"
                                        },
                                    )
                                },
                                onClick = {
                                    onSortOrderChange(order)
                                    sortMenuExpanded = false
                                },
                            )
                        }
                    }
                }
            }
        }
        errorMessage?.let { message ->
            item {
                GraphStatusCard(message = message, isError = true)
            }
        }
        if (graphs.isEmpty()) {
            item {
                GraphEmptyState(hasQuery = query.isNotBlank(), onCreate = onCreate)
            }
        } else {
            item {
                Text(
                    "${graphs.size} graph${if (graphs.size == 1) "" else "s"}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(graphs, key = ResearchGraph::id) { graph ->
                GraphListCard(
                    graph = graph,
                    onOpen = { onOpen(graph.id) },
                    onRename = { onRename(graph) },
                    onDelete = { onDelete(graph) },
                )
            }
        }
    }
}

@Composable
private fun GraphListCard(
    graph: ResearchGraph,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(graph.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${graph.paperIds.size} papers  •  ${graph.connections.size} connections",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    graphInsight(graph),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onOpen) { Text("Open") }
                TextButton(onClick = onRename) { Text("Rename") }
                TextButton(onClick = onDelete) { Text("Delete") }
            }
        }
    }
}

@Composable
private fun GraphEmptyState(
    hasQuery: Boolean,
    onCreate: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                if (hasQuery) "No matching graphs" else "Start with a research question",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                if (hasQuery) {
                    "Try a different graph name or clear the search."
                } else {
                    "Create a local graph to see how your saved papers connect. Nothing leaves this device."
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!hasQuery) {
                Button(onClick = onCreate) { Text("Create your first graph") }
            }
        }
    }
}

@Composable
private fun GraphNameDialog(
    title: String,
    confirmLabel: String,
    initialName: String = "",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Graph name") },
                singleLine = true,
            )
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank(),
                onClick = { onConfirm(name.trim()) },
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
fun GraphStatusCard(
    message: String,
    isError: Boolean,
) {
    Card(
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = if (isError) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            },
        ),
    ) {
        Text(
            message,
            modifier = Modifier.padding(14.dp),
            color = if (isError) {
                MaterialTheme.colorScheme.onErrorContainer
            } else {
                MaterialTheme.colorScheme.onSecondaryContainer
            },
        )
    }
}

private fun graphInsight(graph: ResearchGraph): String {
    return when {
        graph.paperIds.isEmpty() -> "Ready for your first saved paper"
        graph.connections.isEmpty() -> "Add a connection to reveal a research path"
        else -> "${graph.connections.size} relationship${if (graph.connections.size == 1) "" else "s"} mapped locally"
    }
}
