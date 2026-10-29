package com.paperconnekt.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.paperconnekt.domain.model.Paper
import com.paperconnekt.storage.OpenAlexPaperSearchProvider
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
internal fun LibraryPaperSearchDialog(
    savedPapers: List<Paper>,
    onSave: suspend (Paper) -> Unit,
    onDismiss: () -> Unit,
) {
    val client = remember { HttpClient { expectSuccess = true } }
    DisposableEffect(client) { onDispose { client.close() } }
    val provider = remember(client) { OpenAlexPaperSearchProvider(client) }
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var submittedQuery by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Paper>>(emptyList()) }
    var page by remember { mutableStateOf(0) }
    var hasNextPage by remember { mutableStateOf(false) }
    var searching by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var hasSearched by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun search(requestedPage: Int, searchQuery: String = submittedQuery) {
        if (searching || saving || searchQuery.isBlank() || requestedPage < 0) return
        searching = true
        error = null
        scope.launch {
            try {
                val found = provider.search(searchQuery.trim(), page = requestedPage, perPage = 10)
                results = found
                page = requestedPage
                submittedQuery = searchQuery.trim()
                hasNextPage = found.size == 10
                hasSearched = true
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                Napier.e("Could not search OpenAlex for Library papers", exception)
                error = exception.message ?: "OpenAlex search failed. Please retry."
            } finally {
                searching = false
            }
        }
    }

    PaperSearchPickerDialog(
        seeds = emptyList(),
        query = query,
        results = results,
        isSearching = searching,
        isAdding = saving,
        error = error,
        page = page,
        hasNextPage = hasNextPage,
        onQueryChange = {
            query = it
            results = emptyList()
            page = 0
            hasNextPage = false
            hasSearched = false
            submittedQuery = ""
            error = null
        },
        onSearch = { search(0, query) },
        onPreviousPage = { search(page - 1) },
        onNextPage = { search(page + 1) },
        onAdd = { paper ->
            if (!saving && savedPapers.none { it.id == paper.id }) {
                saving = true
                error = null
                scope.launch {
                    try {
                        onSave(paper.copy(pdfPath = null))
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (exception: Exception) {
                        Napier.e("Could not save OpenAlex paper", exception)
                        error = exception.message ?: "Could not save this paper. Please retry."
                    } finally {
                        saving = false
                    }
                }
            }
        },
        onRemove = {},
        onDismiss = { if (!saving) onDismiss() },
        libraryMode = true,
        savedPaperIds = savedPapers.map(Paper::id).toSet(),
        hasSearched = hasSearched,
    )
}
