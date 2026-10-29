package com.paperconnekt.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.Image
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.jetbrains.compose.resources.painterResource
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.paperconnekt.domain.model.ConnectionType
import com.paperconnekt.domain.model.GraphConnection
import com.paperconnekt.domain.model.Paper
import com.paperconnekt.domain.model.ResearchGraph
import com.paperconnekt.app.legacygraph.LegacyGraphCanvasPanel
import com.paperconnekt.app.legacygraph.ConnectionType as LegacyConnectionType
import com.paperconnekt.app.legacygraph.GraphNodeSortOrder
import com.paperconnekt.app.legacygraph.PaperNode
import com.paperconnekt.app.legacygraph.GuidedReviewDirection
import com.paperconnekt.app.legacygraph.GuidedReviewState
import com.paperconnekt.app.legacygraph.GuidedSuggestion
import com.paperconnekt.app.legacygraph.GuidedCandidate
import com.paperconnekt.app.legacygraph.GuidedRelation
import com.paperconnekt.app.legacygraph.GuidedAiVerdict
import com.paperconnekt.app.legacygraph.rankGuidedCandidates
import com.paperconnekt.app.legacygraph.includes
import com.paperconnekt.app.legacygraph.filterGraph
import com.paperconnekt.app.legacygraph.components.PaperSidebar
import com.paperconnekt.app.legacygraph.components.GraphControls
import com.paperconnekt.app.legacygraph.components.GraphFilterMenu
import com.paperconnekt.app.legacygraph.components.GuidedReviewPanel
import com.paperconnekt.app.legacygraph.components.GuidedInsightsDialog
import com.paperconnekt.app.legacygraph.toLegacyGraph
import com.paperconnekt.storage.GraphExpansionDirection
import com.paperconnekt.storage.OpenAlexGraphProvider
import com.paperconnekt.storage.OpenAlexPaperSearchProvider
import com.paperconnekt.app.resources.Res
import com.paperconnekt.app.resources.filled_close
import com.paperconnekt.app.resources.filled_stars
import com.paperconnekt.app.resources.arrow_back
import com.paperconnekt.app.resources.research_graph_empty
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
    val graphPapers = (papers + graph.discoveredPapers).distinctBy(Paper::id)
        .filter { it.id in graph.paperIds }
    val availablePapers = papers.filterNot { it.id in graph.paperIds }
    var selectedPaperId by remember(graph.id) { mutableStateOf<String?>(null) }
    var paperQuery by remember(graph.id) { mutableStateOf("") }
    var sortByTitle by remember(graph.id) { mutableStateOf(false) }
    var showAddPaperDialog by remember(graph.id) { mutableStateOf(false) }
    var showSeedPicker by remember(graph.id) { mutableStateOf(graph.paperIds.isEmpty()) }
    var seedQuery by remember(graph.id) { mutableStateOf("") }
    var seedResults by remember(graph.id) { mutableStateOf<List<Paper>>(emptyList()) }
    var seedSearchPage by remember(graph.id) { mutableStateOf(0) }
    var seedHasNextPage by remember(graph.id) { mutableStateOf(false) }
    var isSearchingSeeds by remember(graph.id) { mutableStateOf(false) }
    var seedPickerError by remember(graph.id) { mutableStateOf<String?>(null) }
    var isAddingSeed by remember(graph.id) { mutableStateOf(false) }
    var showConnectionDialog by remember(graph.id) { mutableStateOf(false) }
    var connectionToRemove by remember { mutableStateOf<GraphConnection?>(null) }
    var isExpanding by remember(graph.id) { mutableStateOf(false) }
    var isDiscoveringConnections by remember(graph.id) { mutableStateOf(false) }
    var connectionDiscoveryProgress by remember(graph.id) { mutableStateOf(0f) }
    var connectionDiscoveryComplete by remember(graph.id) { mutableStateOf(false) }
    var autoExpansionAttempted by remember(graph.id) { mutableStateOf(false) }
    var showReferences by remember(graph.id) { mutableStateOf(true) }
    var showCitations by remember(graph.id) { mutableStateOf(true) }
    var hiddenReferencePaperIds by remember(graph.id) { mutableStateOf<Set<String>>(emptySet()) }
    var hiddenCitationPaperIds by remember(graph.id) { mutableStateOf<Set<String>>(emptySet()) }
    var showInterconnections by remember(graph.id) { mutableStateOf(true) }
    var minimumCitations by remember(graph.id) { mutableStateOf(0) }
    var maximumCitations by remember(graph.id) { mutableStateOf(Int.MAX_VALUE) }
    var minimumSimilarityPercent by remember(graph.id) { mutableStateOf(0) }
    var showFilters by remember(graph.id) { mutableStateOf(false) }
    var guidedKeptIds by remember(graph.id) { mutableStateOf<Set<String>>(emptySet()) }
    var guidedSkippedIds by remember(graph.id) { mutableStateOf<Set<String>>(emptySet()) }
    var reviewDirection by remember(graph.id, selectedPaperId) { mutableStateOf(GuidedReviewDirection.BOTH) }
    var reviewBatchSize by remember(graph.id, selectedPaperId) { mutableStateOf(5) }
    var showReviewDialog by remember(graph.id) { mutableStateOf(false) }
    var showInsightsDialog by remember(graph.id) { mutableStateOf(false) }
    var persistedNodePositions by remember(graph.id) { mutableStateOf(graph.nodePositions) }
    val scope = rememberCoroutineScope()
    val graphProvider = remember { OpenAlexGraphProvider(HttpClient()) }
    val paperSearchProvider = remember { OpenAlexPaperSearchProvider(HttpClient()) }

    val selectedPaper = graphPapers.firstOrNull { it.id == selectedPaperId }
    val selectedNode = selectedPaper?.let { paper ->
        val seedIds = graph.seedPaperIds.ifEmpty { graph.paperIds.firstOrNull()?.let(::listOf).orEmpty() }
        PaperNode(
            paper = paper,
            citationCount = paper.citationCount ?: 0,
            connectionType = when {
                paper.id in seedIds -> LegacyConnectionType.CENTRAL
                graph.connections.any { it.fromPaperId in seedIds && it.toPaperId == paper.id } -> LegacyConnectionType.CITED_BY_CENTRAL
                else -> LegacyConnectionType.CITES_CENTRAL
            },
            referenceCount = paper.referenceCount ?: graph.connections.count { it.fromPaperId == paper.id },
            citingCount = paper.citingCount ?: graph.connections.count { it.toPaperId == paper.id },
        )
    }
    val legacyReferenceNodes = graphPapers
        .filter { paper ->
            graph.connections.any {
                it.fromPaperId in (graph.seedPaperIds.ifEmpty { graph.paperIds.firstOrNull()?.let(::listOf).orEmpty() }) &&
                    it.toPaperId == paper.id
            }
        }
        .map { paper -> PaperNode(paper, paper.citationCount ?: 0, LegacyConnectionType.CITED_BY_CENTRAL) }
    val guidedReviewState = selectedPaper?.let { paper ->
        val candidates = rankGuidedCandidates(paper.id, graph.toLegacyGraph(graphPapers))
            .filter { reviewDirection.includes(it.relation) }
        GuidedReviewState(
            direction = reviewDirection,
            batchSize = reviewBatchSize,
            isQueueLoaded = true,
                batch = candidates
                    .filter { it.node.paper.id !in guidedKeptIds && it.node.paper.id !in guidedSkippedIds }
                    .take(reviewBatchSize)
                    .map { candidate ->
                GuidedSuggestion(
                    candidate = candidate,
                    reason = "Ranked from this paper's loaded references and citations.",
                    aiVerdict = null,
                )
            },
            keptIds = guidedKeptIds,
            skippedIds = guidedSkippedIds,
            candidates = candidates,
            referenceCount = candidates.count { it.relation == GuidedRelation.REFERENCE },
            citationCount = candidates.count { it.relation == GuidedRelation.CITATION },
            remainingCount = candidates.count { it.node.paper.id !in guidedKeptIds && it.node.paper.id !in guidedSkippedIds },
        )
    }
    val visiblePapers = graphPapers
        .filter {
            val query = paperQuery.trim().lowercase()
            query.isEmpty() ||
                it.title.lowercase().contains(query) ||
                it.authors.any { author -> author.lowercase().contains(query) }
        }
        .let { current -> if (sortByTitle) current.sortedBy { it.title.lowercase() } else current }
    val seedId = graph.seedPaperIds.firstOrNull() ?: graph.paperIds.firstOrNull()
    val legacyFullGraph = graph.toLegacyGraph(graphPapers)
    val graphInsightsReview = remember(legacyFullGraph, guidedKeptIds, guidedSkippedIds) {
        val candidates = seedId?.let { rankGuidedCandidates(it, legacyFullGraph) }.orEmpty()
        GuidedReviewState(
            isQueueLoaded = true,
            keptIds = guidedKeptIds,
            skippedIds = guidedSkippedIds,
            candidates = candidates,
            referenceCount = candidates.count { it.relation == GuidedRelation.REFERENCE },
            citationCount = candidates.count { it.relation == GuidedRelation.CITATION },
            remainingCount = candidates.count { it.node.paper.id !in guidedKeptIds && it.node.paper.id !in guidedSkippedIds },
        )
    }
    val baseFilteredLegacyGraph = filterGraph(
        graph = legacyFullGraph,
        includeCiting = showCitations,
        includeCited = showReferences,
        minCitations = minimumCitations,
        maxCitations = maximumCitations,
        minimumSimilarityPercent = minimumSimilarityPercent,
        showInterconnections = showInterconnections,
        showDisqualified = false,
        disqualifiedPaperIds = guidedSkippedIds,
    )
    val selectedHiddenIds = buildSet {
        graph.connections
            .filter { it.fromPaperId in hiddenReferencePaperIds }
            .forEach { add(it.toPaperId) }
        graph.connections
            .filter { it.toPaperId in hiddenCitationPaperIds }
            .forEach { add(it.fromPaperId) }
        selectedPaperId?.let(::remove)
    }
    val filteredLegacyGraph = if (selectedHiddenIds.isEmpty()) {
        baseFilteredLegacyGraph
    } else {
        baseFilteredLegacyGraph.copy(
            nodes = baseFilteredLegacyGraph.nodes.filterNot { it.paper.id in selectedHiddenIds },
            edges = baseFilteredLegacyGraph.edges.filter {
                it.fromId !in selectedHiddenIds && it.toId !in selectedHiddenIds
            },
        )
    }
    val filteredGraphPapers = graphPapers.filter { it.id in filteredLegacyGraph.nodes.map { node -> node.paper.id }.toSet() }
    val visibleGraphPapers = visiblePapers.filter { paper -> paper.id in filteredGraphPapers.map { it.id } }

    val saveUpdated: (ResearchGraph) -> Unit = { updated ->
        onSave(
            updated.copy(
                // The workspace copy is authoritative; the incoming graph can
                // be an older snapshot captured before the latest drag.
                nodePositions = updated.nodePositions + persistedNodePositions,
                updatedAt = Clock.System.now(),
            )
        )
    }
    val keepPaper: (String) -> Unit = { id ->
        graphPapers.firstOrNull { it.id == id }?.let(onSavePaper)
        guidedKeptIds = guidedKeptIds + id
        guidedSkippedIds = guidedSkippedIds - id
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
                        saveUpdated(
                            graph.copy(
                                discoveredPapers = (graph.discoveredPapers + expansion.papers).distinctBy(Paper::id),
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

    val discoverConnections: () -> Unit = {
        val source = selectedPaper ?: graphPapers.firstOrNull()
        if (source == null) {
            onError("Add a paper to the graph before loading connections.")
        } else if (!isDiscoveringConnections && !isExpanding) {
            isDiscoveringConnections = true
            connectionDiscoveryProgress = 0f
            connectionDiscoveryComplete = false
            scope.launch {
                var discoveredGraph = graph
                runCatching {
                    listOf(
                        GraphExpansionDirection.REFERENCES,
                        GraphExpansionDirection.CITATIONS,
                    ).forEachIndexed { index, direction ->
                        val visibleIds = filteredGraphPapers.map(Paper::id).toSet() + source.id
                        val expansion = graphProvider.expand(source.id, direction)
                        val visiblePapers = expansion.papers.filter { it.id in visibleIds }
                        val visibleConnections = expansion.connections.filter {
                            it.fromPaperId in visibleIds && it.toPaperId in visibleIds
                        }
                        discoveredGraph = discoveredGraph.copy(
                            discoveredPapers = (discoveredGraph.discoveredPapers + visiblePapers).distinctBy(Paper::id),
                            paperIds = (discoveredGraph.paperIds + visiblePapers.map(Paper::id)).distinct(),
                            connections = (discoveredGraph.connections + visibleConnections).distinct(),
                        )
                        connectionDiscoveryProgress = (index + 1) / 2f
                        saveUpdated(discoveredGraph)
                    }
                }.onSuccess {
                    connectionDiscoveryComplete = true
                }.onFailure { onError(it.message ?: "Could not discover graph connections.") }
                isDiscoveringConnections = false
                connectionDiscoveryProgress = 1f
            }
        }
    }

    val searchSeeds: (Int) -> Unit = { requestedPage ->
        val query = seedQuery.trim()
        if (query.isNotEmpty() && !isSearchingSeeds) {
            isSearchingSeeds = true
            seedPickerError = null
            scope.launch {
                runCatching {
                    val normalized = query.lowercase()
                    val local = if (requestedPage == 0) papers.filter {
                        it.title.lowercase().contains(normalized) ||
                            it.authors.any { author -> author.lowercase().contains(normalized) } ||
                            it.doi?.lowercase()?.contains(normalized) == true
                    } else emptyList()
                    val remote = paperSearchProvider.search(query, page = requestedPage, perPage = 10)
                    local + remote.filterNot { remotePaper -> local.any { it.id == remotePaper.id } }
                }.onSuccess {
                    seedResults = it
                    seedSearchPage = requestedPage
                    seedHasNextPage = it.size >= 10
                }
                    .onFailure { seedPickerError = it.message ?: "Paper search failed." }
                isSearchingSeeds = false
            }
        }
    }

    val addSeed: (Paper) -> Unit = { paper ->
        if (!isAddingSeed && paper.id !in graph.seedPaperIds &&
            !(graph.seedPaperIds.isEmpty() && graph.paperIds.firstOrNull() == paper.id)) {
            isAddingSeed = true
            seedPickerError = null
            scope.launch {
                runCatching {
                    var updated = graph.copy(
                        discoveredPapers = (graph.discoveredPapers + paper).distinctBy(Paper::id),
                        paperIds = (graph.paperIds + paper.id).distinct(),
                        seedPaperIds = ((graph.seedPaperIds.ifEmpty { graph.paperIds.firstOrNull()?.let(::listOf).orEmpty() }) + paper.id).distinct(),
                    )
                    if (paper.id.startsWith("W")) {
                        listOf(GraphExpansionDirection.REFERENCES, GraphExpansionDirection.CITATIONS).forEach { direction ->
                            val expansion = graphProvider.expand(paper.id, direction)
                            updated = updated.copy(
                                discoveredPapers = (updated.discoveredPapers + expansion.papers).distinctBy(Paper::id),
                                paperIds = (updated.paperIds + expansion.papers.map(Paper::id)).distinct(),
                                connections = (updated.connections + expansion.connections).distinct(),
                            )
                        }
                    }
                    saveUpdated(updated)
                    selectedPaperId = paper.id
                    showSeedPicker = false
                }.onFailure { seedPickerError = it.message ?: "Could not add this seed paper." }
                isAddingSeed = false
            }
        }
    }

    val removeSeed: (String) -> Unit = { paperId ->
        val seedIds = graph.seedPaperIds.ifEmpty { graph.paperIds.firstOrNull()?.let(::listOf).orEmpty() }
        if (seedIds.size > 1 && paperId in seedIds) {
            val relatedPaperIds = graph.connections
                .filter { it.fromPaperId == paperId || it.toPaperId == paperId }
                .flatMap { listOf(it.fromPaperId, it.toPaperId) }
                .filterNot { it == paperId }
                .toSet()
            val remainingConnections = graph.connections.filter {
                it.fromPaperId != paperId && it.toPaperId != paperId
            }
            val orphanedRelatedPaperIds = relatedPaperIds.filter { relatedId ->
                remainingConnections.none {
                    it.fromPaperId == relatedId || it.toPaperId == relatedId
                }
            }.toSet()
            val removedPaperIds = orphanedRelatedPaperIds + paperId
            persistedNodePositions = persistedNodePositions.filterKeys { it !in removedPaperIds }
            saveUpdated(
                graph.copy(
                    paperIds = graph.paperIds.filterNot { it in removedPaperIds },
                    seedPaperIds = seedIds - paperId,
                    connections = remainingConnections,
                    nodePositions = graph.nodePositions.filterKeys { it !in removedPaperIds },
                    discoveredPapers = graph.discoveredPapers.filterNot { it.id in removedPaperIds },
                ),
            )
            if (selectedPaperId == paperId) selectedPaperId = null
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
                            expandedGraph = expandedGraph.copy(
                                discoveredPapers = (expandedGraph.discoveredPapers + expansion.papers).distinctBy(Paper::id),
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
            IconButton(onClick = onBack) {
                Icon(
                    painter = org.jetbrains.compose.resources.painterResource(Res.drawable.arrow_back),
                    contentDescription = "Back to graphs",
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = graph.name,
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${filteredGraphPapers.size} papers  •  ${graph.connections.size} connections",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { showSeedPicker = true }) { Text("Add seed") }
                TextButton(onClick = { showInsightsDialog = true }) { Text("Insights") }
                GraphFilterMenu(
                    expanded = showFilters,
                    onOpen = { showFilters = true },
                    onDismiss = { showFilters = false },
                ) {
                    GraphControls(
                        filterCiting = showCitations,
                        onToggleCiting = { showCitations = !showCitations },
                        filterCited = showReferences,
                        onToggleCited = { showReferences = !showReferences },
                        minCitations = minimumCitations,
                        onMinCitationsChanged = { minimumCitations = it },
                        maxCitations = maximumCitations,
                        onMaxCitationsChanged = { maximumCitations = it },
                        minimumSimilarityPercent = minimumSimilarityPercent,
                        onMinimumSimilarityChanged = { minimumSimilarityPercent = it },
                        showInterconnections = showInterconnections,
                        onToggleInterconnections = { showInterconnections = !showInterconnections },
                        initiallyExpanded = true,
                        embedded = true,
                    )
                }
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
                LegacyGraphCanvasPanel(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(360.dp),
                    graph = graph,
                    papers = filteredGraphPapers,
                    selectedPaperId = selectedPaperId,
                    onSelectPaper = { selectedPaperId = it },
                    initialNodePositions = persistedNodePositions,
                    onPositionsChanged = { positions ->
                        val mergedPositions = persistedNodePositions + positions
                        if (mergedPositions != persistedNodePositions) {
                            persistedNodePositions = mergedPositions
                            saveUpdated(graph.copy(nodePositions = mergedPositions))
                        }
                    },
                )
                if (graphPapers.isEmpty()) {
                    EmptyGraphStateCard(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        onClick = { showSeedPicker = true },
                    )
                }
                androidx.compose.animation.AnimatedVisibility(
                    visible = selectedPaper != null,
                    enter = fadeIn() + slideInHorizontally { it / 4 },
                    exit = fadeOut() + slideOutHorizontally { it / 4 },
                ) {
                    selectedNode?.let { node ->
                            PaperSidebar(
                                node = node,
                                isSavedToLibrary = papers.any { it.id == node.paper.id },
                                onSaveToLibrary = { onSavePaper(node.paper) },
                                onClose = { selectedPaperId = null },
                                onExpand = expandGraph,
                                onRefresh = { expandGraph(com.paperconnekt.storage.GraphExpansionDirection.REFERENCES) },
                                isRefreshing = isExpanding,
                                onLoadConnections = discoverConnections,
                                isLoadingConnections = isDiscoveringConnections,
                                connectionProgress = connectionDiscoveryProgress,
                                connectionDiscoveryComplete = connectionDiscoveryComplete,
                                showReferences = selectedPaperId?.let { it !in hiddenReferencePaperIds } ?: true,
                                showCitations = selectedPaperId?.let { it !in hiddenCitationPaperIds } ?: true,
                                onToggleReferences = {
                                    selectedPaperId?.let { id ->
                                        hiddenReferencePaperIds = if (id in hiddenReferencePaperIds) {
                                            hiddenReferencePaperIds - id
                                        } else {
                                            hiddenReferencePaperIds + id
                                        }
                                    }
                                },
                                onToggleCitations = {
                                    selectedPaperId?.let { id ->
                                        hiddenCitationPaperIds = if (id in hiddenCitationPaperIds) {
                                            hiddenCitationPaperIds - id
                                        } else {
                                            hiddenCitationPaperIds + id
                                        }
                                    }
                                },
                                onOpenReview = { showReviewDialog = true },
                                onUseAsSeed = { addSeed(node.paper) },
                                canUseAsSeed = node.paper.id !in (graph.seedPaperIds.ifEmpty { graph.paperIds.firstOrNull()?.let(::listOf).orEmpty() }),
                                citedPapers = legacyReferenceNodes,
                                nodeSortOrder = GraphNodeSortOrder.DEFAULT,
                                modifier = Modifier.fillMaxWidth(),
                            )
                    }
                    }
                }
        } else {
            // Keep the canvas mounted at one stable size while the paper panel
            // opens. Resizing/replacing the canvas here used to restart its
            // layout and made the clicked node disappear briefly.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                LegacyGraphCanvasPanel(
                    modifier = Modifier.fillMaxSize(),
                    graph = graph,
                    papers = filteredGraphPapers,
                    selectedPaperId = selectedPaperId,
                    onSelectPaper = { selectedPaperId = it },
                    initialNodePositions = persistedNodePositions,
                    onPositionsChanged = { positions ->
                        val mergedPositions = persistedNodePositions + positions
                        if (mergedPositions != persistedNodePositions) {
                            persistedNodePositions = mergedPositions
                            saveUpdated(graph.copy(nodePositions = mergedPositions))
                        }
                    },
                )
                if (graphPapers.isEmpty()) {
                    EmptyGraphStateCard(
                        modifier = Modifier.align(Alignment.Center),
                        onClick = { showSeedPicker = true },
                    )
                }
                androidx.compose.animation.AnimatedVisibility(
                    visible = selectedPaper != null,
                    enter = fadeIn() + slideInHorizontally { it / 3 },
                    exit = fadeOut() + slideOutHorizontally { it / 3 },
                    modifier = Modifier.align(Alignment.CenterEnd),
                ) {
                    selectedNode?.let { node ->
                        PaperSidebar(
                            modifier = Modifier
                                .width(360.dp)
                                .fillMaxHeight(),
                            node = node,
                            isSavedToLibrary = papers.any { it.id == node.paper.id },
                            onSaveToLibrary = { onSavePaper(node.paper) },
                            onClose = { selectedPaperId = null },
                            onExpand = expandGraph,
                            onRefresh = { expandGraph(com.paperconnekt.storage.GraphExpansionDirection.REFERENCES) },
                            isRefreshing = isExpanding,
                            onLoadConnections = discoverConnections,
                            isLoadingConnections = isDiscoveringConnections,
                            connectionProgress = connectionDiscoveryProgress,
                            connectionDiscoveryComplete = connectionDiscoveryComplete,
                            showReferences = selectedPaperId?.let { it !in hiddenReferencePaperIds } ?: true,
                            showCitations = selectedPaperId?.let { it !in hiddenCitationPaperIds } ?: true,
                            onToggleReferences = {
                                selectedPaperId?.let { id ->
                                    hiddenReferencePaperIds = if (id in hiddenReferencePaperIds) {
                                        hiddenReferencePaperIds - id
                                    } else {
                                        hiddenReferencePaperIds + id
                                    }
                                }
                            },
                            onToggleCitations = {
                                selectedPaperId?.let { id ->
                                    hiddenCitationPaperIds = if (id in hiddenCitationPaperIds) {
                                        hiddenCitationPaperIds - id
                                    } else {
                                        hiddenCitationPaperIds + id
                                    }
                                }
                            },
                            onOpenReview = { showReviewDialog = true },
                            onUseAsSeed = { addSeed(node.paper) },
                            canUseAsSeed = node.paper.id !in (graph.seedPaperIds.ifEmpty { graph.paperIds.firstOrNull()?.let(::listOf).orEmpty() }),
                            citedPapers = legacyReferenceNodes,
                            nodeSortOrder = GraphNodeSortOrder.DEFAULT,
                        )
                    }
                }
                    /* GraphDetailsPanel(
                        graph = graph,
                        papers = visibleGraphPapers,
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
                        showReferences = showReferences,
                        onToggleReferences = { showReferences = !showReferences },
                        showCitations = showCitations,
                        onToggleCitations = { showCitations = !showCitations },
                        showInterconnections = showInterconnections,
                        onToggleInterconnections = { showInterconnections = !showInterconnections },
                        minimumCitations = minimumCitations,
                        onMinimumCitationsChange = { minimumCitations = it },
                    ) */
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

    if (showSeedPicker) {
        PaperSearchPickerDialog(
            seeds = graphPapers.filter { it.id in (graph.seedPaperIds.ifEmpty { graph.paperIds.firstOrNull()?.let(::listOf).orEmpty() }) },
            query = seedQuery,
            results = seedResults,
            isSearching = isSearchingSeeds,
            isAdding = isAddingSeed,
            error = seedPickerError,
            onQueryChange = { seedQuery = it },
            onSearch = { searchSeeds(0) },
            page = seedSearchPage,
            hasNextPage = seedHasNextPage,
            onPreviousPage = { searchSeeds(seedSearchPage - 1) },
            onNextPage = { searchSeeds(seedSearchPage + 1) },
            onAdd = addSeed,
            onRemove = removeSeed,
            onDismiss = { showSeedPicker = false },
        )
    }

    if (showReviewDialog && selectedNode != null && guidedReviewState != null) {
        Dialog(
            onDismissRequest = { showReviewDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            GuidedReviewPanel(
                modifier = Modifier.width(if (compact) 520.dp else 640.dp).heightIn(max = 760.dp),
                node = selectedNode,
                review = guidedReviewState,
                isEvaluating = false,
                keptPapers = graphPapers.filter { it.id in guidedKeptIds }.map { PaperNode(it) },
                onClose = { showReviewDialog = false },
                onShowDetails = { showReviewDialog = false },
                onShowInsights = { showReviewDialog = false; showInsightsDialog = true },
                onBatchSizeChanged = { reviewBatchSize = it },
                onDirectionChanged = { reviewDirection = it },
                onShowNext = {},
                onKeep = keepPaper,
                onSkip = { id -> guidedSkippedIds = guidedSkippedIds + id; guidedKeptIds = guidedKeptIds - id },
                onUndo = { id -> guidedKeptIds = guidedKeptIds - id; guidedSkippedIds = guidedSkippedIds - id },
                onRevisitSkipped = { guidedSkippedIds = emptySet() },
                onReviewPaper = { selectedPaperId = it },
                onDismissError = {},
                researchTargetContent = {},
            )
        }
    }

    if (showInsightsDialog) {
        GuidedInsightsDialog(
            node = null,
            review = graphInsightsReview,
            keptElsewhere = emptySet(),
            onLoad = {},
            onKeep = keepPaper,
            onSkip = { guidedSkippedIds = guidedSkippedIds + it },
            onUndo = { guidedKeptIds = guidedKeptIds - it; guidedSkippedIds = guidedSkippedIds - it },
            onDismiss = { showInsightsDialog = false },
            graphTitle = graph.name,
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
    showReferences: Boolean,
    onToggleReferences: () -> Unit,
    showCitations: Boolean,
    onToggleCitations: () -> Unit,
    showInterconnections: Boolean,
    onToggleInterconnections: () -> Unit,
    minimumCitations: Int,
    onMinimumCitationsChange: (Int) -> Unit,
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
                Text("Graph filters", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = showReferences,
                        onClick = onToggleReferences,
                        label = { Text("References") },
                    )
                    FilterChip(
                        selected = showCitations,
                        onClick = onToggleCitations,
                        label = { Text("Citations") },
                    )
                }
                FilterChip(
                    selected = showInterconnections,
                    onClick = onToggleInterconnections,
                    label = { Text("Interconnections") },
                )
                OutlinedTextField(
                    value = minimumCitations.toString(),
                    onValueChange = { onMinimumCitationsChange(it.toIntOrNull()?.coerceAtLeast(0) ?: 0) },
                    label = { Text("Minimum citations") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
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
    val seedIds = graph.seedPaperIds.ifEmpty { graph.paperIds.firstOrNull()?.let(::listOf).orEmpty() }
    val isSeed = paper.id in seedIds
    val isReference = graph.connections.any { it.fromPaperId in seedIds && it.toPaperId == paper.id }
    val role = when {
        isSeed -> "Seed paper"
        isReference -> "Reference"
        else -> "Citation"
    }
    val roleColor = when {
        isSeed -> MaterialTheme.colorScheme.primaryContainer
        isReference -> Color(0xFFCCFBF1)
        else -> Color(0xFFFEF3C7)
    }
    val roleTextColor = when {
        isSeed -> MaterialTheme.colorScheme.onPrimaryContainer
        isReference -> Color(0xFF0F766E)
        else -> Color(0xFFB45309)
    }
    val references = graph.connections.count { it.fromPaperId == paper.id }
    val citations = graph.connections.count { it.toPaperId == paper.id }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = roleColor,
                ) {
                    Text(
                        role,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = roleTextColor,
                    )
                }
                Text(
                    "${paper.citationCount ?: 0} citations",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                paper.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            if (paper.authors.isNotEmpty()) {
                Text(
                    paper.authors.joinToString(", "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                listOfNotNull(
                    paper.year?.let { "Year $it" },
                    paper.doi?.let { "DOI $it" },
                ).joinToString("  •  ").ifEmpty { "Local metadata only" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                listOfNotNull(
                    "${paper.referenceCount ?: references} references",
                    "$citations citing papers",
                ).joinToString("  •  "),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            if (!paper.abstractText.isNullOrBlank()) {
                Text(
                    paper.abstractText!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Graph actions", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onExpand(GraphExpansionDirection.CITATIONS) }, modifier = Modifier.weight(1f)) {
                            Text("Citations", style = MaterialTheme.typography.labelSmall)
                        }
                        OutlinedButton(onClick = { onExpand(GraphExpansionDirection.REFERENCES) }, modifier = Modifier.weight(1f)) {
                            Text("References", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Button(onClick = onConnect, modifier = Modifier.fillMaxWidth()) {
                        Text("Create connection")
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(
                    onClick = { onExpand(GraphExpansionDirection.REFERENCES) },
                    enabled = !isExpanding,
                ) { Text("Load references") }
                TextButton(
                    onClick = { onExpand(GraphExpansionDirection.CITATIONS) },
                    enabled = !isExpanding,
                ) { Text("Load citations") }
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
private fun EmptyGraphStateCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .widthIn(min = 280.dp, max = 420.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Image(
                painter = painterResource(Res.drawable.research_graph_empty),
                contentDescription = null,
                modifier = Modifier.widthIn(max = 220.dp).fillMaxWidth(),
            )
            Text(
                text = "Your graph is empty",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Add a seed paper to start exploring its references and citations.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Text(
                text = "Click to add a paper",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
internal fun PaperSearchPickerDialog(
    seeds: List<Paper>,
    query: String,
    results: List<Paper>,
    isSearching: Boolean,
    isAdding: Boolean,
    error: String?,
    page: Int,
    hasNextPage: Boolean,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onAdd: (Paper) -> Unit,
    onRemove: (String) -> Unit,
    onDismiss: () -> Unit,
    libraryMode: Boolean = false,
    savedPaperIds: Set<String> = emptySet(),
    hasSearched: Boolean = false,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BoxWithConstraints(
            Modifier.fillMaxSize().safeDrawingPadding(),
            contentAlignment = Alignment.Center,
        ) {
            val compact = maxWidth < 640.dp
            Surface(
                modifier = Modifier
                    .padding(if (compact) 0.dp else 24.dp)
                    .widthIn(max = 1100.dp)
                    .fillMaxWidth()
                    .fillMaxHeight(if (compact) 1f else 0.94f),
                shape = RoundedCornerShape(if (compact) 0.dp else 24.dp),
                color = MaterialTheme.colorScheme.background,
                tonalElevation = 6.dp,
            ) {
                Column(Modifier.padding(if (compact) 12.dp else 24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(painterResource(Res.drawable.filled_stars), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            if (libraryMode) "Find papers on OpenAlex" else "Seed papers (${seeds.size})",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(painterResource(Res.drawable.filled_close), contentDescription = "Close paper picker")
                        }
                    }
                    Text(
                        if (libraryMode) "Search academic literature, then save only the papers you choose to your local Library."
                        else "Add papers to explore direct citations and shared references. Removing a seed keeps its papers and connections.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(seeds, key = Paper::id) { seed ->
                            InputChip(
                                selected = true,
                                onClick = {},
                                label = {
                                    Text(
                                        seed.title,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.widthIn(max = 220.dp),
                                    )
                                },
                                trailingIcon = if (seeds.size > 1) ({
                                    IconButton(onClick = { onRemove(seed.id) }, modifier = Modifier.size(40.dp)) {
                                        Icon(painterResource(Res.drawable.filled_close), contentDescription = "Remove seed: ${seed.title}", modifier = Modifier.size(18.dp))
                                    }
                                }) else null,
                            )
                        }
                    }
                    if (isAdding) {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                        Text(
                            if (libraryMode) "Saving paper locally…" else "Finding citation connections…",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    }
                    error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp))
                    }
                    Box(Modifier.weight(1f)) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                OutlinedTextField(
                                    modifier = Modifier.weight(1f),
                                    value = query,
                                    onValueChange = onQueryChange,
                                    enabled = !isSearching && !isAdding,
                                    label = { Text("Search title, author, or DOI") },
                                    singleLine = true,
                                )
                                Button(
                                    onClick = onSearch,
                                    enabled = query.isNotBlank() && !isSearching && !isAdding,
                                ) { Text(if (isSearching) "Searching…" else "Search") }
                            }
                            if (results.isNotEmpty() || page > 0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        if (results.isEmpty()) "No matching papers" else
                                            "${page * 10 + 1}-${page * 10 + results.size} results",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.weight(1f),
                                    )
                                    SeedSearchPager(
                                        page = page,
                                        hasNext = hasNextPage,
                                        enabled = !isSearching && !isAdding,
                                        onPrevious = onPreviousPage,
                                        onNext = onNextPage,
                                    )
                                }
                            }
                            if (!isSearching && results.isEmpty()) {
                                Box(
                                    modifier = Modifier.weight(1f).fillMaxWidth(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(16.dp),
                                    ) {
                                        Image(
                                            painter = painterResource(Res.drawable.research_graph_empty),
                                            contentDescription = "Search for research papers",
                                            modifier = Modifier.widthIn(max = 280.dp).fillMaxWidth(),
                                        )
                                        Spacer(Modifier.height(12.dp))
                                        Text(
                                            if (libraryMode && !hasSearched) "Search for a paper to save"
                                            else if (query.isBlank()) "Search for a paper to add a seed"
                                            else "No research papers found for \"$query\"",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            if (libraryMode && !hasSearched) "Search OpenAlex by title, author, keywords, or DOI."
                                            else if (query.isBlank()) "Search across academic literature to start building this graph."
                                            else "Try adjusting your query, keywords, or DOI.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    items(results, key = Paper::id) { paper ->
                                        Card(modifier = Modifier.fillMaxWidth()) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(paper.title, fontWeight = FontWeight.SemiBold)
                                                    Text(
                                                        listOfNotNull(
                                                            paper.year?.toString(),
                                                            paper.authors.take(3).joinToString(", ").takeIf { it.isNotBlank() },
                                                        ).joinToString(" · "),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis,
                                                    )
                                                    Row(
                                                        modifier = Modifier.padding(top = 8.dp),
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    ) {
                                                        paper.year?.let {
                                                            Text("$it", style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                        }
                                                        paper.citationCount?.let {
                                                            Text("⭐ $it citations", style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.tertiary)
                                                        }
                                                        paper.referenceCount?.let {
                                                            Text("$it references", style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.primary)
                                                        }
                                                    }
                                                }
                                                TextButton(
                                                    onClick = { onAdd(paper) },
                                                    enabled = !isAdding && (!libraryMode || paper.id !in savedPaperIds),
                                                ) {
                                                    Text(if (!libraryMode) "Add seed" else if (paper.id in savedPaperIds) "Saved" else "Save locally")
                                                }
                                            }
                                        }
                                    }
                                }
                                SeedSearchPager(
                                    page = page,
                                    hasNext = hasNextPage,
                                    enabled = !isSearching && !isAdding,
                                    onPrevious = onPreviousPage,
                                    onNext = onNextPage,
                                )
                            }
                        }
                    }
                    TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                        Text("Done")
                    }
                }
            }
        }
    }
}

@Composable
private fun SeedSearchPager(
    page: Int,
    hasNext: Boolean,
    enabled: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(
            onClick = onPrevious,
            enabled = enabled && page > 0,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
        ) { Text("Previous", style = MaterialTheme.typography.labelSmall) }
        Text(
            "Page ${page + 1}",
            modifier = Modifier.padding(horizontal = 10.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
        OutlinedButton(
            onClick = onNext,
            enabled = enabled && hasNext,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
        ) { Text("Next", style = MaterialTheme.typography.labelSmall) }
    }
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
