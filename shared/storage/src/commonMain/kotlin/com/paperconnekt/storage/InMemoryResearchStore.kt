package com.paperconnekt.storage

import com.paperconnekt.domain.model.AppSettings
import com.paperconnekt.domain.model.AssistantSession
import com.paperconnekt.domain.model.LibraryEntry
import com.paperconnekt.domain.model.Paper
import com.paperconnekt.domain.model.ResearchGraph
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class InMemoryResearchStore : LocalResearchStore {
    private val settingsState = MutableStateFlow(AppSettings())
    private val papersState = MutableStateFlow<List<Paper>>(emptyList())
    private val graphsState = MutableStateFlow<List<ResearchGraph>>(emptyList())
    private val assistantSessionsState = MutableStateFlow<List<AssistantSession>>(emptyList())
    private val libraryEntriesState = MutableStateFlow<List<LibraryEntry>>(emptyList())

    override val settings: StateFlow<AppSettings> = settingsState.asStateFlow()
    override val papers: StateFlow<List<Paper>> = papersState.asStateFlow()
    override val graphs: StateFlow<List<ResearchGraph>> = graphsState.asStateFlow()
    override val assistantSessions: StateFlow<List<AssistantSession>> =
        assistantSessionsState.asStateFlow()
    override val libraryEntries: StateFlow<List<LibraryEntry>> =
        libraryEntriesState.asStateFlow()

    override suspend fun updateSettings(transform: (AppSettings) -> AppSettings) {
        settingsState.update(transform)
    }

    override suspend fun savePaper(paper: Paper) {
        papersState.update { current -> (current.filterNot { it.id == paper.id } + paper) }
    }

    override suspend fun removePaper(paperId: String) {
        papersState.update { current -> current.filterNot { it.id == paperId } }
    }

    override suspend fun saveGraph(graph: ResearchGraph) {
        graphsState.update { current -> (current.filterNot { it.id == graph.id } + graph) }
    }

    override suspend fun removeGraph(graphId: String) {
        graphsState.update { current -> current.filterNot { it.id == graphId } }
    }

    override suspend fun saveAssistantSession(session: AssistantSession) {
        assistantSessionsState.update { current ->
            (current.filterNot { it.id == session.id } + session)
        }
    }

    override suspend fun saveLibraryEntry(entry: LibraryEntry) {
        libraryEntriesState.update { current ->
            (current.filterNot { it.id == entry.id } + entry)
        }
    }

    override suspend fun removeLibraryEntry(entryId: String) {
        libraryEntriesState.update { current ->
            val removedIds = mutableSetOf(entryId)
            var changed: Boolean
            do {
                changed = false
                current.forEach { entry ->
                    if (entry.parentId != null &&
                        entry.parentId in removedIds &&
                        removedIds.add(entry.id)
                    ) {
                        changed = true
                    }
                }
            } while (changed)
            current.filterNot { it.id in removedIds }
        }
    }
}
