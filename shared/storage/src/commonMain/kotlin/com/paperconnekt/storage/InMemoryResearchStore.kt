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

open class InMemoryResearchStore(initialized: Boolean = true) : LocalResearchStore {
    private val initializedState = MutableStateFlow(initialized)
    protected val settingsState = MutableStateFlow(AppSettings())
    protected val papersState = MutableStateFlow<List<Paper>>(emptyList())
    protected val graphsState = MutableStateFlow<List<ResearchGraph>>(emptyList())
    protected val assistantSessionsState = MutableStateFlow<List<AssistantSession>>(emptyList())
    protected val libraryEntriesState = MutableStateFlow<List<LibraryEntry>>(emptyList())

    override val isInitialized: StateFlow<Boolean> = initializedState.asStateFlow()
    override val settings: StateFlow<AppSettings> = settingsState.asStateFlow()
    override val papers: StateFlow<List<Paper>> = papersState.asStateFlow()
    override val graphs: StateFlow<List<ResearchGraph>> = graphsState.asStateFlow()
    override val assistantSessions: StateFlow<List<AssistantSession>> =
        assistantSessionsState.asStateFlow()
    override val libraryEntries: StateFlow<List<LibraryEntry>> =
        libraryEntriesState.asStateFlow()

    override open suspend fun updateSettings(transform: (AppSettings) -> AppSettings) {
        settingsState.update(transform)
    }

    override open suspend fun savePaper(paper: Paper) {
        papersState.update { current -> (current.filterNot { it.id == paper.id } + paper) }
    }

    override open suspend fun removePaper(paperId: String) {
        papersState.update { current -> current.filterNot { it.id == paperId } }
    }

    override open suspend fun saveGraph(graph: ResearchGraph) {
        graphsState.update { current -> (current.filterNot { it.id == graph.id } + graph) }
    }

    override open suspend fun removeGraph(graphId: String) {
        graphsState.update { current -> current.filterNot { it.id == graphId } }
    }

    override open suspend fun saveAssistantSession(session: AssistantSession) {
        assistantSessionsState.update { current ->
            (current.filterNot { it.id == session.id } + session)
        }
    }

    override open suspend fun removeAssistantSession(sessionId: String) {
        assistantSessionsState.update { current -> current.filterNot { it.id == sessionId } }
    }

    override open suspend fun saveLibraryEntry(entry: LibraryEntry) {
        libraryEntriesState.update { current ->
            (current.filterNot { it.id == entry.id } + entry)
        }
    }

    override open suspend fun removeLibraryEntry(entryId: String) {
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

    internal fun snapshot(): WorkspaceSnapshot = WorkspaceSnapshot(
        settings = settingsState.value,
        papers = papersState.value,
        graphs = graphsState.value,
        assistantSessions = assistantSessionsState.value,
        libraryEntries = libraryEntriesState.value,
    )

    internal fun restore(snapshot: WorkspaceSnapshot) {
        require(snapshot.version == WorkspaceSnapshot.CURRENT_VERSION) {
            "Unsupported workspace data version: ${snapshot.version}"
        }
        settingsState.value = snapshot.settings
        papersState.value = snapshot.papers
        graphsState.value = snapshot.graphs
        assistantSessionsState.value = snapshot.assistantSessions
        libraryEntriesState.value = snapshot.libraryEntries
    }

    internal fun markInitialized() {
        initializedState.value = true
    }
}
