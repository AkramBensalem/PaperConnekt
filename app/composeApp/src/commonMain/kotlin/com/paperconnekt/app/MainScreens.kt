package com.paperconnekt.app

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
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
import com.paperconnekt.domain.model.Paper
import com.paperconnekt.storage.LocalResearchStore
import com.paperconnekt.storage.LocalFirstPaperSearchProvider
import com.paperconnekt.storage.LocalPaperSearchProvider
import com.paperconnekt.storage.OpenAlexPaperSearchProvider
import io.ktor.client.HttpClient
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun DashboardScreen(
    store: LocalResearchStore,
    paddingValues: PaddingValues,
) {
    val papers by store.papers.collectAsState()
    var query by remember { mutableStateOf("") }
    var includeRemote by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf(papers) }
    var isSearching by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showAddPaperDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val searchProvider = remember {
        LocalFirstPaperSearchProvider(
            localProvider = LocalPaperSearchProvider(store),
            optionalRemoteProvider = OpenAlexPaperSearchProvider(HttpClient()),
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Research dashboard")
        Text("Search locally saved papers first, then optionally discover new papers through OpenAlex.")
        Button(onClick = { showAddPaperDialog = true }) {
            Text("Add paper manually")
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                modifier = Modifier.weight(1f),
                value = query,
                onValueChange = { query = it },
                label = { Text("Search papers") },
                singleLine = true,
            )
            Button(
                enabled = !isSearching,
                onClick = {
                    scope.launch {
                        isSearching = true
                        errorMessage = null
                        try {
                            results = searchProvider.search(query, includeRemote)
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

        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Checkbox(
                checked = includeRemote,
                onCheckedChange = { includeRemote = it },
            )
            Text("Include optional OpenAlex search")
        }

        errorMessage?.let { message ->
            Text(message)
        }

        if (isSearching) {
            CircularProgressIndicator()
        } else if (results.isEmpty()) {
            Text("No papers yet. Search OpenAlex or add papers in the next dashboard iteration.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(results, key = Paper::id) { paper ->
                    PaperCard(
                        paper = paper,
                        isSaved = papers.any { it.id == paper.id },
                        onSave = {
                            scope.launch { store.savePaper(paper) }
                        },
                        onRemove = {
                            scope.launch { store.removePaper(paper.id) }
                        },
                    )
                }
            }

            if (showAddPaperDialog) {
                AddPaperDialog(
                    onDismiss = { showAddPaperDialog = false },
                    onSave = { title, authors, year, pdfPath ->
                        scope.launch {
                            store.savePaper(
                                Paper(
                                    id = "manual-${Random.nextLong()}",
                                    title = title,
                                    authors = authors,
                                    year = year,
                                    pdfPath = pdfPath,
                                ),
                            )
                            results = store.papers.value
                            showAddPaperDialog = false
                        }
                    },
                )
            }
        }
    }
}

@Composable
fun GraphScreen(paddingValues: PaddingValues) {
    PlaceholderScreen("Research graphs", "Explore saved connections between papers.", paddingValues)
}

@Composable
fun LibraryScreen(paddingValues: PaddingValues) {
    PlaceholderScreen("Library", "Organize papers in a local Zotero-style hierarchy.", paddingValues)
}

@Composable
fun SettingsScreen(
    store: LocalResearchStore,
    paddingValues: PaddingValues,
) {
    PlaceholderScreen("Settings", "Configure secure local AI and Zotero keys.", paddingValues)
}

@Composable
private fun PaperCard(
    paper: Paper,
    isSaved: Boolean,
    onSave: () -> Unit,
    onRemove: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(paper.title)
            if (paper.authors.isNotEmpty()) {
                Text(paper.authors.joinToString())
            }

            @Composable
            private fun AddPaperDialog(
                onDismiss: () -> Unit,
                onSave: (title: String, authors: List<String>, year: Int?, pdfPath: String?) -> Unit,
            ) {
                var title by remember { mutableStateOf("") }
                var authors by remember { mutableStateOf("") }
                var year by remember { mutableStateOf("") }
                var pdfPath by remember { mutableStateOf("") }

                AlertDialog(
                    onDismissRequest = onDismiss,
                    title = { Text("Add paper") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = title,
                                onValueChange = { title = it },
                                label = { Text("Title") },
                                singleLine = true,
                            )
                            OutlinedTextField(
                                value = authors,
                                onValueChange = { authors = it },
                                label = { Text("Authors, comma separated") },
                                singleLine = true,
                            )
                            OutlinedTextField(
                                value = year,
                                onValueChange = { year = it },
                                label = { Text("Year") },
                                singleLine = true,
                            )
                            OutlinedTextField(
                                value = pdfPath,
                                onValueChange = { pdfPath = it },
                                label = { Text("PDF path (optional)") },
                                singleLine = true,
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            enabled = title.isNotBlank(),
                            onClick = {
                                onSave(
                                    title.trim(),
                                    authors.split(',').map(String::trim).filter(String::isNotEmpty),
                                    year.toIntOrNull(),
                                    pdfPath.trim().ifEmpty { null },
                                )
                            },
                        ) {
                            Text("Save locally")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel")
                        }
                    },
                )
            }
            paper.year?.let { Text(it.toString()) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isSaved) {
                    TextButton(onClick = onRemove) {
                        Text("Remove")
                    }
                } else {
                    Button(onClick = onSave) {
                        Text("Save locally")
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaceholderScreen(
    title: String,
    description: String,
    paddingValues: PaddingValues,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(title)
        Text(description)
    }
}
