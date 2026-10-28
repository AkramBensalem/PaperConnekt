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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import com.paperconnekt.domain.model.ConnectionType
import com.paperconnekt.domain.model.GraphConnection
import com.paperconnekt.domain.model.Paper
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
    var showCreateDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val selectedGraph = graphs.firstOrNull { it.id == selectedGraphId }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Research graphs")
        Text("Save paper relationships as local workspaces you can revisit and extend.")
        Button(onClick = { showCreateDialog = true }) {
            Text("Create graph")
        }

        if (selectedGraph == null) {
            if (graphs.isEmpty()) {
                Text("No graphs yet. Create one to start mapping your literature.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(graphs, key = ResearchGraph::id) { graph ->
                        GraphCard(
                            graph = graph,
                            paperCount = graph.paperIds.size,
                            onOpen = { selectedGraphId = graph.id },
                            onDelete = {
                                scope.launch { store.removeGraph(graph.id) }
                            },
                        )
                    }
                }
            }
        } else {
            GraphWorkspace(
                graph = selectedGraph,
                papers = papers,
                onBack = { selectedGraphId = null },
                onAddPaper = { paperId ->
                    scope.launch {
                        store.saveGraph(
                            selectedGraph.copy(
                                paperIds = (selectedGraph.paperIds + paperId).distinct(),
                                updatedAt = Clock.System.now(),
                            ),
                        )
                    }
                },
                onRemovePaper = { paperId ->
                    scope.launch {
                        store.saveGraph(
                            selectedGraph.copy(
                                paperIds = selectedGraph.paperIds.filterNot { it == paperId },
                                connections = selectedGraph.connections.filter {
                                    it.fromPaperId != paperId && it.toPaperId != paperId
                                },
                                updatedAt = Clock.System.now(),
                            ),
                        )
                    }
                },
                onConnect = { fromPaperId, toPaperId ->
                    scope.launch {
                        store.saveGraph(
                            selectedGraph.copy(
                                connections = (
                                    selectedGraph.connections +
                                        GraphConnection(
                                            fromPaperId = fromPaperId,
                                            toPaperId = toPaperId,
                                            type = ConnectionType.RELATED_TOPIC,
                                        )
                                    ).distinct(),
                                updatedAt = Clock.System.now(),
                            ),
                        )
                    }
                },
            )
        }
    }

    if (showCreateDialog) {
        CreateGraphDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name ->
                val graph = ResearchGraph(
                    id = "graph-${Random.nextLong()}",
                    name = name,
                    updatedAt = Clock.System.now(),
                )
                scope.launch {
                    store.saveGraph(graph)
                    selectedGraphId = graph.id
                    showCreateDialog = false
                }
            },
        )
    }
}

@Composable
private fun GraphCard(
    graph: ResearchGraph,
    paperCount: Int,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(graph.name)
                Text("$paperCount papers • ${graph.connections.size} connections")
            }
            Row {
                TextButton(onClick = onOpen) {
                    Text("Open")
                }
                TextButton(onClick = onDelete) {
                    Text("Delete")
                }
            }
        }
    }
}

@Composable
private fun GraphWorkspace(
    graph: ResearchGraph,
    papers: List<Paper>,
    onBack: () -> Unit,
    onAddPaper: (String) -> Unit,
    onRemovePaper: (String) -> Unit,
    onConnect: (String, String) -> Unit,
) {
    var selectedPaperForConnection by remember(graph.id) { mutableStateOf<String?>(null) }
    var expandedPaperMenu by remember { mutableStateOf<String?>(null) }
    val graphPapers = papers.filter { it.id in graph.paperIds }
    val availablePapers = papers.filterNot { it.id in graph.paperIds }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onBack) {
            Text("Back to graphs")
        }
        Text(graph.name)
        Text("Select a paper menu to add relationships or remove it from this graph.")

        if (graphPapers.isEmpty()) {
            Text("This graph is empty. Add a saved paper below.")
        } else {
            graphPapers.forEach { paper ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(paper.title)
                            Text(paper.authors.joinToString())
                        }
                        Column {
                            OutlinedButton(
                                onClick = {
                                    selectedPaperForConnection = paper.id
                                    expandedPaperMenu = paper.id
                                },
                            ) {
                                Text("Actions")
                            }
                            DropdownMenu(
                                expanded = expandedPaperMenu == paper.id,
                                onDismissRequest = { expandedPaperMenu = null },
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Remove from graph") },
                                    onClick = {
                                        onRemovePaper(paper.id)
                                        expandedPaperMenu = null
                                    },
                                )
                                graphPapers
                                    .filterNot { it.id == paper.id }
                                    .forEach { target ->
                                        DropdownMenuItem(
                                            text = { Text("Connect to ${target.title.take(24)}") },
                                            onClick = {
                                                onConnect(paper.id, target.id)
                                                expandedPaperMenu = null
                                            },
                                        )
                                    }
                            }
                        }
                    }
                }
            }
        }

        if (graph.connections.isNotEmpty()) {
            Text("Connections")
            graph.connections.forEach { connection ->
                val from = papers.firstOrNull { it.id == connection.fromPaperId }?.title
                val to = papers.firstOrNull { it.id == connection.toPaperId }?.title
                Text("${from ?: connection.fromPaperId} → ${to ?: connection.toPaperId}")
            }
        }

        Text("Saved papers")
        availablePapers.forEach { paper ->
            TextButton(onClick = { onAddPaper(paper.id) }) {
                Text("Add ${paper.title}")
            }
        }
    }
}

@Composable
private fun CreateGraphDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create graph") },
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

