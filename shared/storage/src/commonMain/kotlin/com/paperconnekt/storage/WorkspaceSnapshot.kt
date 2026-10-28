package com.paperconnekt.storage

import com.paperconnekt.domain.model.AppSettings
import com.paperconnekt.domain.model.AssistantSession
import com.paperconnekt.domain.model.LibraryEntry
import com.paperconnekt.domain.model.Paper
import com.paperconnekt.domain.model.ResearchGraph
import kotlinx.serialization.Serializable

@Serializable
internal data class WorkspaceSnapshot(
    val version: Int = CURRENT_VERSION,
    val settings: AppSettings = AppSettings(),
    val papers: List<Paper> = emptyList(),
    val graphs: List<ResearchGraph> = emptyList(),
    val assistantSessions: List<AssistantSession> = emptyList(),
    val libraryEntries: List<LibraryEntry> = emptyList(),
) {
    companion object {
        const val CURRENT_VERSION = 1
    }
}
