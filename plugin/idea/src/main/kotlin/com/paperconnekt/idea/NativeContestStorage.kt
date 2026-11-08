package com.paperconnekt.idea

import com.paperconnekt.storage.PersistentResearchStore
import com.paperconnekt.storage.WorkspaceStorage
import com.paperconnekt.storage.InMemoryResearchStore
import com.paperconnekt.storage.writePrivateFileAtomically
import com.paperconnekt.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText

internal class NativeContestStore(
    private val directory: Path = configuredWorkspaceDirectory(),
) : InMemoryResearchStore() {
    private suspend fun transact(change: suspend PersistentResearchStore.() -> Unit) = mutex.withLock {
        val current = PersistentResearchStore(NativeContestWorkspaceStorage(directory))
        current.initialize()
        current.change()
        settingsState.value = current.settings.value
        papersState.value = current.papers.value
        graphsState.value = current.graphs.value
        assistantSessionsState.value = current.assistantSessions.value
        libraryEntriesState.value = current.libraryEntries.value
    }

    suspend fun initialize() = transact { }
    override suspend fun updateSettings(transform: (AppSettings) -> AppSettings) = transact { updateSettings(transform) }
    override suspend fun savePaper(paper: Paper) = transact { savePaper(paper) }
    override suspend fun updatePaper(paperId: String, transform: (Paper) -> Paper) = transact { updatePaper(paperId, transform) }
    override suspend fun removePaper(paperId: String) = transact { removePaper(paperId) }
    override suspend fun saveGraph(graph: ResearchGraph) = transact { saveGraph(graph) }
    override suspend fun removeGraph(graphId: String) = transact { removeGraph(graphId) }
    override suspend fun saveAssistantSession(session: AssistantSession) = transact { saveAssistantSession(session) }
    override suspend fun removeAssistantSession(sessionId: String) = transact { removeAssistantSession(sessionId) }
    override suspend fun saveLibraryEntry(entry: LibraryEntry) = transact { saveLibraryEntry(entry) }
    override suspend fun removeLibraryEntry(entryId: String) = transact { removeLibraryEntry(entryId) }

    suspend fun savePositions(graphId: String, positions: Map<String, GraphNodePosition>) = transact {
        val graph = graphs.value.firstOrNull { it.id == graphId }
            ?: error("This graph was removed. Refresh the workspace.")
        saveGraph(graph.copy(
            nodePositions = (graph.nodePositions + positions).filterKeys { it in graph.paperIds },
            updatedAt = kotlinx.datetime.Clock.System.now(),
        ))
    }

    suspend fun editGraph(graphId: String, change: (ResearchGraph) -> ResearchGraph) = transact {
        val graph = graphs.value.firstOrNull { it.id == graphId }
            ?: error("This graph was removed. Refresh the workspace.")
        val updated = change(graph)
        require(updated.id == graphId) { "A graph update cannot change its ID." }
        saveGraph(updated.copy(updatedAt = kotlinx.datetime.Clock.System.now()))
    }

    private companion object {
        val mutex = Mutex()
    }
}

private class NativeContestWorkspaceStorage(
    private val directory: Path,
) : WorkspaceStorage {
    private val dataFile = directory.resolve("workspace.json")
    private val corruptFile = directory.resolve("workspace.json.corrupt")
    private var original: String? = null

    override suspend fun read(): String? = withContext(Dispatchers.IO) {
        dataFile.takeIf { it.exists() && it.isRegularFile() }?.readText().also { original = it }
    }

    override suspend fun write(content: String) = withContext(Dispatchers.IO) {
        val latest = dataFile.takeIf { it.exists() && it.isRegularFile() }?.readText()
        check(latest == original) {
            "The workspace changed in another app. Refresh and retry; no changes were overwritten."
        }
        writePrivateFileAtomically(dataFile, content.toByteArray(Charsets.UTF_8))
        original = content
        Unit
    }

    override suspend fun quarantineCorruptData() = withContext(Dispatchers.IO) {
        if (dataFile.exists()) {
            Files.deleteIfExists(corruptFile)
            Files.move(dataFile, corruptFile, REPLACE_EXISTING)
        }
    }
}

private fun configuredWorkspaceDirectory(): Path =
    System.getProperty("paperconnekt.data.dir")?.takeIf(String::isNotBlank)?.let(::Path)
        ?: Path(System.getProperty("user.home"), ".paperconnekt-contest")
