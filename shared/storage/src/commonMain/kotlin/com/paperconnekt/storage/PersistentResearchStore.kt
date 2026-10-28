package com.paperconnekt.storage

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import com.paperconnekt.domain.model.AppSettings
import com.paperconnekt.domain.model.AssistantSession
import com.paperconnekt.domain.model.LibraryEntry
import com.paperconnekt.domain.model.Paper
import com.paperconnekt.domain.model.ResearchGraph

class PersistentResearchStore(
    private val storage: WorkspaceStorage,
    private val json: Json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    },
) : InMemoryResearchStore(initialized = false) {
    private val mutex = Mutex()
    private var initialized = false

    suspend fun initialize() = mutex.withLock {
        if (initialized) return

        val content = storage.read()
        if (content != null) {
            try {
                restore(json.decodeFromString<WorkspaceSnapshot>(content))
            } catch (error: SerializationException) {
                storage.quarantineCorruptData()
            } catch (error: IllegalArgumentException) {
                storage.quarantineCorruptData()
            }
        }
        initialized = true
        markInitialized()
    }

    override suspend fun updateSettings(transform: (AppSettings) -> AppSettings) =
        mutate { super.updateSettings(transform) }

    override suspend fun savePaper(paper: Paper) =
        mutate { super.savePaper(paper) }

    override suspend fun removePaper(paperId: String) = mutate { super.removePaper(paperId) }

    override suspend fun saveGraph(graph: ResearchGraph) =
        mutate { super.saveGraph(graph) }

    override suspend fun removeGraph(graphId: String) = mutate { super.removeGraph(graphId) }

    override suspend fun saveAssistantSession(session: AssistantSession) =
        mutate { super.saveAssistantSession(session) }

    override suspend fun removeAssistantSession(sessionId: String) =
        mutate { super.removeAssistantSession(sessionId) }

    override suspend fun saveLibraryEntry(entry: LibraryEntry) =
        mutate { super.saveLibraryEntry(entry) }

    override suspend fun removeLibraryEntry(entryId: String) =
        mutate { super.removeLibraryEntry(entryId) }

    private suspend fun mutate(operation: suspend () -> Unit) = mutex.withLock {
        check(initialized) { "Research store must be initialized before it is used" }
        operation()
        storage.write(json.encodeToString(snapshot()))
    }
}
