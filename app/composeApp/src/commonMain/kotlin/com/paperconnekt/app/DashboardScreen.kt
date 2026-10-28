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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.paperconnekt.domain.model.Paper
import com.paperconnekt.storage.LocalFirstPaperSearchProvider
import com.paperconnekt.storage.LocalPaperSearchProvider
import com.paperconnekt.storage.LocalResearchStore
import com.paperconnekt.storage.OpenAlexPaperSearchProvider
import io.ktor.client.HttpClient
import kotlinx.coroutines.launch
import kotlin.random.Random

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    store: LocalResearchStore,
    paddingValues: PaddingValues,
    onOpenAssistant: () -> Unit = {},
    onOpenLibrary: () -> Unit = {},
    onOpenPaper: (Paper) -> Unit = {},
    onOpenGraph: (Paper) -> Unit = {},
) {
    val papers by store.papers.collectAsState()
    val graphs by store.graphs.collectAsState()
    val libraryEntries by store.libraryEntries.collectAsState()
    val settings by store.settings.collectAsState()
    var query by remember { mutableStateOf("") }
    var includeRemote by remember(settings.openAlexDiscoveryEnabled) {
        mutableStateOf(settings.openAlexDiscoveryEnabled)
    }
    var sortOrder by remember { mutableStateOf(PaperSortOrder.RECENT) }
    var searchResults by remember { mutableStateOf<List<Paper>?>(null) }
    var isSearching by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var showAddPaperDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val searchProvider = remember {
        LocalFirstPaperSearchProvider(
            localProvider = LocalPaperSearchProvider(store),
            optionalRemoteProvider = OpenAlexPaperSearchProvider(HttpClient()),
        )
    }

    val localResults = remember(papers, query) {
        val normalizedQuery = query.trim().lowercase()
        if (normalizedQuery.isEmpty()) {
            papers
        } else {
            papers.filter { paper ->
                paper.title.lowercase().contains(normalizedQuery) ||
                    paper.authors.any { it.lowercase().contains(normalizedQuery) } ||
                    paper.doi?.lowercase()?.contains(normalizedQuery) == true ||
                    paper.tags.any { it.lowercase().contains(normalizedQuery) }
            }
        }
    }
    val displayedPapers = (searchResults ?: localResults).let { results ->
        when (sortOrder) {
            PaperSortOrder.RECENT -> results.asReversed()
            PaperSortOrder.A_TO_Z -> results.sortedBy { it.title.lowercase() }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
    ) {
        val compact = maxWidth < 720.dp
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = if (compact) 16.dp else 32.dp,
                end = if (compact) 16.dp else 32.dp,
                top = 24.dp,
                bottom = 32.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                DashboardHeader(
                    paperCount = papers.size,
                    includeRemote = includeRemote,
                    onRefresh = {
                        searchResults = null
                        notice = "Showing your latest local collection."
                    },
                )
            }
            item {
                DashboardQuickActions(
                    compact = compact,
                    onDiscover = {
                        includeRemote = true
                        scope.launch {
                            store.updateSettings { it.copy(openAlexDiscoveryEnabled = true) }
                        }
                        notice = "Optional discovery enabled. Search to query OpenAlex."
                    },
                    onAssistant = onOpenAssistant,
                    onLibrary = onOpenLibrary,
                )
            }
            item {
                DashboardMetrics(
                    paperCount = papers.size,
                    graphCount = graphs.size,
                    libraryCount = libraryEntries.size,
                    authorCount = papers.flatMap { it.authors }.distinct().size,
                    compact = compact,
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Your research collection",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            modifier = Modifier.weight(1f),
                            value = query,
                            onValueChange = {
                                query = it
                                searchResults = null
                                errorMessage = null
                            },
                            label = { Text("Search title, author, tag, or DOI") },
                            singleLine = true,
                        )
                        if (!compact) Spacer(Modifier.width(10.dp))
                        Button(
                            enabled = !isSearching,
                            onClick = {
                                scope.launch {
                                    isSearching = true
                                    errorMessage = null
                                    try {
                                        searchResults = searchProvider.search(query, includeRemote)
                                        notice = if (includeRemote && query.isNotBlank()) {
                                            "Local results combined with optional OpenAlex discovery."
                                        } else {
                                            "Search updated from your local collection."
                                        }
                                    } catch (exception: Exception) {
                                        errorMessage = exception.message ?: "Paper search failed."
                                    } finally {
                                        isSearching = false
                                    }
                                }
                            },
                        ) {
                            Text("Search")
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FilterChip(
                            selected = includeRemote,
                            onClick = {
                                includeRemote = !includeRemote
                                scope.launch {
                                    store.updateSettings {
                                        it.copy(openAlexDiscoveryEnabled = includeRemote)
                                    }
                                }
                            },
                            label = { Text("Optional online discovery") },
                        )
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = {
                            sortOrder = PaperSortOrder.RECENT
                            searchResults = null
                        }) {
                            Text("Recent")
                        }
                        TextButton(onClick = {
                            sortOrder = PaperSortOrder.A_TO_Z
                            searchResults = null
                        }) {
                            Text("A-Z")
                        }
                    }
                }
            }
            if (isSearching) {
                item {
                    DashboardStatusCard(
                        message = "Searching your collection${if (includeRemote) " and OpenAlex" else ""}…",
                        showProgress = true,
                    )
                }
            }
            errorMessage?.let { message ->
                item {
                    DashboardStatusCard(message = message, isError = true)
                }
            }
            notice?.let { message ->
                item {
                    DashboardStatusCard(message = message)
                }
            }
            if (!isSearching && displayedPapers.isEmpty()) {
                item {
                    DashboardEmptyState(
                        hasQuery = query.isNotBlank(),
                        onAddPaper = { showAddPaperDialog = true },
                        onDiscover = {
                            includeRemote = true
                            notice = "Enable optional discovery, then search for a topic."
                        },
                    )
                }
            } else if (!isSearching) {
                item {
                    Text(
                        text = "${displayedPapers.size} paper${if (displayedPapers.size == 1) "" else "s"}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                item {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        maxItemsInEachRow = if (compact) 1 else 2,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        displayedPapers.forEach { paper ->
                            DashboardPaperCard(
                                modifier = if (compact) {
                                    Modifier.fillMaxWidth()
                                } else {
                                    Modifier.weight(1f)
                                },
                                paper = paper,
                                isSaved = papers.any { it.id == paper.id },
                                compact = compact,
                                onOpen = { onOpenPaper(paper) },
                                onOpenGraph = { onOpenGraph(paper) },
                                onSave = {
                                    scope.launch {
                                        store.savePaper(paper)
                                        notice = "Saved “${paper.title}” locally."
                                    }
                                },
                                onRemove = {
                                    scope.launch {
                                        store.removePaper(paper.id)
                                        notice = "Removed “${paper.title}” from your local collection."
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddPaperDialog) {
        AddPaperDialog(
            onDismiss = { showAddPaperDialog = false },
            onSave = { paper ->
                scope.launch {
                    store.savePaper(paper)
                    showAddPaperDialog = false
                    searchResults = null
                    notice = "Paper added to your local collection."
                }
            },
        )
    }
}

private enum class PaperSortOrder {
    RECENT,
    A_TO_Z,
}

@Composable
private fun AddPaperDialog(
    onDismiss: () -> Unit,
    onSave: (Paper) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var authors by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var doi by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }
    var abstractText by remember { mutableStateOf("") }
    var pdfPath by remember { mutableStateOf("") }

    PaperMetadataDialog(
        title = "Add paper manually",
        confirmLabel = "Save locally",
        titleValue = title,
        onTitleChange = { title = it },
        authorsValue = authors,
        onAuthorsChange = { authors = it },
        yearValue = year,
        onYearChange = { year = it },
        doiValue = doi,
        onDoiChange = { doi = it },
        tagsValue = tags,
        onTagsChange = { tags = it },
        abstractValue = abstractText,
        onAbstractChange = { abstractText = it },
        pdfPathValue = pdfPath,
        onPdfPathChange = { pdfPath = it },
        onDismiss = onDismiss,
        enabled = title.isNotBlank(),
        onConfirm = {
            onSave(
                Paper(
                    id = "manual-${Random.nextLong()}",
                    title = title.trim(),
                    authors = authors.split(',').map(String::trim).filter(String::isNotEmpty),
                    year = year.toIntOrNull(),
                    doi = doi.trim().ifEmpty { null },
                    tags = tags.split(',').map(String::trim).filter(String::isNotEmpty),
                    abstractText = abstractText.trim().ifEmpty { null },
                    pdfPath = pdfPath.trim().ifEmpty { null },
                ),
            )
        },
    )
}
