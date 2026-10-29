package com.paperconnekt.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.BorderStroke
import com.paperconnekt.domain.model.ResearchGraph
import com.paperconnekt.app.resources.Res
import com.paperconnekt.app.resources.outlined_account_tree
import com.paperconnekt.app.resources.outlined_add
import com.paperconnekt.app.resources.outlined_auto_graph
import com.paperconnekt.app.resources.outlined_delete_outline
import com.paperconnekt.app.resources.outlined_description
import com.paperconnekt.app.resources.outlined_edit
import com.paperconnekt.app.resources.outlined_open_in_new
import com.paperconnekt.app.resources.outlined_search
import com.paperconnekt.app.resources.outlined_sort
import com.paperconnekt.app.resources.research_graph_empty
import com.paperconnekt.storage.LocalResearchStore
import com.paperconnekt.app.legacygraph.components.GraphRenameDialog
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.jetbrains.compose.resources.painterResource
import kotlin.random.Random

@Composable
fun GraphScreen(
    store: LocalResearchStore,
    paddingValues: PaddingValues,
    onOpenCanvas: (String) -> Unit = {},
) {
    val graphs by store.graphs.collectAsState()
    var graphQuery by remember { mutableStateOf("") }
    var sortOrder by remember { mutableStateOf(GraphSortOrder.RECENT) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var graphBeingRenamed by remember { mutableStateOf<ResearchGraph?>(null) }
    var graphBeingDeleted by remember { mutableStateOf<ResearchGraph?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

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
            onOpen = onOpenCanvas,
            onRename = { graphBeingRenamed = it },
            onDelete = { graphBeingDeleted = it },
        )
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
                    seedPaperIds = emptyList(),
                    updatedAt = Clock.System.now(),
                )
                scope.launch {
                    runCatching { store.saveGraph(graph) }
                        .onSuccess {
                            showCreateDialog = false
                            onOpenCanvas(graph.id)
                        }
                        .onFailure {
                            errorMessage = it.message ?: "Could not create graph."
                        }
                }
            },
        )
    }

    graphBeingRenamed?.let { graph ->
        GraphRenameDialog(
            title = graph.name,
            onDismiss = { graphBeingRenamed = null },
            onSave = { name ->
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

@Composable
fun GraphCanvasScreen(
    store: LocalResearchStore,
    paddingValues: PaddingValues,
    graphId: String?,
    onBack: () -> Unit,
) {
    val graphs by store.graphs.collectAsState()
    val papers by store.papers.collectAsState()
    val graph = graphs.firstOrNull { it.id == graphId }
    var graphBeingRenamed by remember(graphId) { mutableStateOf<ResearchGraph?>(null) }
    var errorMessage by remember(graphId) { mutableStateOf<String?>(null) }
    var isSaving by remember(graphId) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    if (graph == null) {
        onBack()
        return
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
    ) {
        GraphWorkspace(
            graph = graph,
            papers = papers,
            compact = maxWidth < 860.dp,
            isSaving = isSaving,
            errorMessage = errorMessage,
            onBack = onBack,
            onRename = { graphBeingRenamed = graph },
            onSave = { updatedGraph ->
                isSaving = true
                scope.launch {
                    runCatching { store.saveGraph(updatedGraph) }
                        .onFailure { errorMessage = it.message ?: "Could not save graph changes." }
                        .also { isSaving = false }
                }
            },
            onSavePaper = { paper ->
                scope.launch { store.savePaper(paper) }
            },
            onError = {
                errorMessage = it
                isSaving = false
            },
        )
    }

    graphBeingRenamed?.let { renamedGraph ->
        GraphRenameDialog(
            title = renamedGraph.name,
            onDismiss = { graphBeingRenamed = null },
            onSave = { name ->
                scope.launch {
                    runCatching {
                        store.saveGraph(renamedGraph.copy(name = name, updatedAt = Clock.System.now()))
                    }.onFailure {
                        errorMessage = it.message ?: "Could not rename graph."
                    }
                    graphBeingRenamed = null
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
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 320.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val compactHeader = maxWidth < 1100.dp
                if (compactHeader) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        GraphPageTitle()
                        GraphSearchControls(
                            query = query,
                            onQueryChange = onQueryChange,
                            onSortClick = { sortMenuExpanded = true },
                            sortMenuExpanded = sortMenuExpanded,
                            onSortMenuDismiss = { sortMenuExpanded = false },
                            onSortOrderChange = onSortOrderChange,
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    ) {
                        GraphPageTitle(
                            modifier = Modifier.weight(1f),
                        )
                        GraphSearchControls(
                            modifier = Modifier.width(560.dp),
                            query = query,
                            onQueryChange = onQueryChange,
                            onSortClick = { sortMenuExpanded = true },
                            sortMenuExpanded = sortMenuExpanded,
                            onSortMenuDismiss = { sortMenuExpanded = false },
                            onSortOrderChange = onSortOrderChange,
                        )
                    }
                }
            }
        }
        errorMessage?.let { message ->
            item(span = { GridItemSpan(maxLineSpan) }) {
                GraphStatusCard(message = message, isError = true)
            }
        }
        if (graphs.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                GraphEmptyState(hasQuery = query.isNotBlank(), onCreate = onCreate)
            }
        } else {
            item(span = { GridItemSpan(maxLineSpan) }) {
                GraphOrientationCard(
                    graphCount = graphs.size,
                    paperCount = graphs.sumOf { it.paperIds.size },
                    connectionCount = graphs.sumOf { it.connections.size },
                )
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                GraphWorkflowGuide()
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    "Your research maps",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                GraphCreateCard(onCreate = onCreate)
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(208.dp),
        onClick = onOpen,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.Top,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.Top,
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                CircleShape,
                            ),
                        contentAlignment = androidx.compose.ui.Alignment.Center,
                    ) {
                        Icon(
                            painterResource(Res.drawable.outlined_auto_graph),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    Text(
                        graph.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 3,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = onOpen) {
                    Icon(painterResource(Res.drawable.outlined_open_in_new), contentDescription = "Open ${graph.name}")
                }
            }
            Row(
                modifier = Modifier.height(24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                GraphMetric(painterResource(Res.drawable.outlined_description), "${graph.paperIds.size} papers")
                GraphMetric(painterResource(Res.drawable.outlined_account_tree), "${graph.connections.size} connections")
            }
            Spacer(Modifier.weight(1f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Text(
                    graphInsight(graph),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                Row {
                    IconButton(onClick = onRename) {
                        Icon(painterResource(Res.drawable.outlined_edit), contentDescription = "Rename ${graph.name}")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(painterResource(Res.drawable.outlined_delete_outline), contentDescription = "Delete ${graph.name}")
                    }
                }
            }
        }
    }
}

@Composable
private fun GraphWorkflowGuide() {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val steps = listOf(
            Triple(painterResource(Res.drawable.outlined_description), "Start with a question", "Name the problem, chapter, or claim you want to understand."),
            Triple(painterResource(Res.drawable.outlined_account_tree), "Connect evidence", "Link papers by method, citation, result, or research direction."),
            Triple(painterResource(Res.drawable.outlined_auto_graph), "Find the next gap", "Use the shape of the map to spot clusters, edges, and open paths."),
        )
        if (maxWidth < 760.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                steps.forEach { (icon, title, description) ->
                    WorkflowStepCard(icon, title, description, Modifier.fillMaxWidth())
                }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                steps.forEach { (icon, title, description) ->
                    WorkflowStepCard(icon, title, description, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun WorkflowStepCard(
    icon: androidx.compose.ui.graphics.painter.Painter,
    title: String,
    description: String,
    modifier: Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = androidx.compose.ui.Alignment.Top,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.labelLarge)
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun GraphCreateCard(
    onCreate: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(208.dp),
        onClick = onCreate,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f),
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = androidx.compose.ui.Alignment.Center,
            ) {
                Icon(
                    painterResource(Res.drawable.outlined_add),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
            Spacer(Modifier.height(12.dp))
            Text("New research graph", style = MaterialTheme.typography.titleMedium)
            Text(
                "Start mapping a question",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun GraphOrientationCard(
    graphCount: Int,
    paperCount: Int,
    connectionCount: Int,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(22.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = androidx.compose.ui.Alignment.Center,
            ) {
                Icon(
                    painterResource(Res.drawable.outlined_auto_graph),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    "Build your state-of-the-art map",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    "Organize evidence around a question, reveal relationships, and see where the literature is dense or still open.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                OrientationMetric("$graphCount", "maps")
                OrientationMetric("$paperCount", "papers")
                OrientationMetric("$connectionCount", "links")
            }
        }
    }
}

@Composable
private fun OrientationMetric(
    value: String,
    label: String,
) {
    Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
        )
    }
}

@Composable
private fun GraphPageTitle(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Research graphs", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Map the papers behind a question, chapter, or research direction.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun GraphSearchControls(
    modifier: Modifier = Modifier,
    query: String,
    onQueryChange: (String) -> Unit,
    onSortClick: () -> Unit,
    sortMenuExpanded: Boolean,
    onSortMenuDismiss: () -> Unit,
    onSortOrderChange: (GraphSortOrder) -> Unit,
) {
    OutlinedTextField(
        modifier = modifier.fillMaxWidth(),
        value = query,
        onValueChange = onQueryChange,
        leadingIcon = { Icon(painterResource(Res.drawable.outlined_search), contentDescription = null) },
        placeholder = { Text("Search graphs") },
        trailingIcon = {
            Box {
                IconButton(onClick = onSortClick) {
                    Icon(painterResource(Res.drawable.outlined_sort), contentDescription = "Sort graphs")
                }
                DropdownMenu(
                    expanded = sortMenuExpanded,
                    onDismissRequest = onSortMenuDismiss,
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
                                onSortMenuDismiss()
                            },
                        )
                    }
                }
            }
        },
        shape = RoundedCornerShape(28.dp),
        singleLine = true,
    )
}

@Composable
private fun GraphMetric(
    icon: androidx.compose.ui.graphics.painter.Painter,
    label: String,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(17.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun GraphEmptyState(
    hasQuery: Boolean,
    onCreate: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
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
                    Button(onClick = onCreate) {
                        Icon(painterResource(Res.drawable.outlined_add), contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Create your first graph")
                    }
                }
            }
            if (!hasQuery) {
                androidx.compose.foundation.Image(
                    painter = painterResource(Res.drawable.research_graph_empty),
                    contentDescription = "Connected research papers illustration",
                    modifier = Modifier
                        .size(180.dp)
                        .alpha(0.9f),
                )
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
