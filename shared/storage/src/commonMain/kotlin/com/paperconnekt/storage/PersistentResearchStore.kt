package com.paperconnekt.storage

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
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
                throw IllegalStateException("The saved workspace could not be read. It was preserved as workspace.json.corrupt. Retry to start an empty workspace.", error)
            } catch (error: IllegalArgumentException) {
                storage.quarantineCorruptData()
                throw IllegalStateException("The saved workspace version is unsupported. It was preserved as workspace.json.corrupt. Retry to start an empty workspace.", error)
            }
        }
        initialized = true
        markInitialized()
    }

    override suspend fun updateSettings(transform: (AppSettings) -> AppSettings) =
        mutate { updateSettings(transform) }

    override suspend fun savePaper(paper: Paper) =
        mutate { savePaper(paper) }

    override suspend fun removePaper(paperId: String) = mutate { removePaper(paperId) }

    override suspend fun saveGraph(graph: ResearchGraph) =
        mutate { saveGraph(graph) }

    override suspend fun removeGraph(graphId: String) = mutate { removeGraph(graphId) }

    override suspend fun saveAssistantSession(session: AssistantSession) =
        mutate { saveAssistantSession(session) }

    override suspend fun removeAssistantSession(sessionId: String) =
        mutate { removeAssistantSession(sessionId) }

    override suspend fun saveLibraryEntry(entry: LibraryEntry) =
        mutate { saveLibraryEntry(entry) }

    override suspend fun removeLibraryEntry(entryId: String) =
        mutate { removeLibraryEntry(entryId) }

    override suspend fun updatePaper(paperId: String, transform: (Paper) -> Paper) =
        mutate { updatePaper(paperId, transform) }

    private suspend fun mutate(operation: suspend InMemoryResearchStore.() -> Unit) = withContext(NonCancellable) {
        mutex.withLock {
            check(initialized) { "Research store must be initialized before it is used" }
            val current = snapshot()
            val staged = InMemoryResearchStore().apply { restore(current) }
            staged.operation()
            val updated = staged.snapshot()
            storage.write(json.encodeToString(updated))
            restore(updated)
        }
    }
}
