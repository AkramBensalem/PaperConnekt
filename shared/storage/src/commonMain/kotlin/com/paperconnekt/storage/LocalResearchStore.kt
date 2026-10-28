package com.paperconnekt.storage

import com.paperconnekt.domain.model.AppSettings
import com.paperconnekt.domain.model.AssistantSession
import com.paperconnekt.domain.model.LibraryEntry
import com.paperconnekt.domain.model.Paper
import com.paperconnekt.domain.model.ResearchGraph
import kotlinx.coroutines.flow.StateFlow

interface LocalResearchStore {
    val settings: StateFlow<AppSettings>
    val papers: StateFlow<List<Paper>>
    val graphs: StateFlow<List<ResearchGraph>>
    val assistantSessions: StateFlow<List<AssistantSession>>
    val libraryEntries: StateFlow<List<LibraryEntry>>

    suspend fun updateSettings(transform: (AppSettings) -> AppSettings)
    suspend fun savePaper(paper: Paper)
    suspend fun removePaper(paperId: String)
    suspend fun saveGraph(graph: ResearchGraph)
    suspend fun saveAssistantSession(session: AssistantSession)
    suspend fun saveLibraryEntry(entry: LibraryEntry)
}
