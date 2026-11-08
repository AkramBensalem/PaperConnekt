package com.paperconnekt.plugins.common

import com.paperconnekt.storage.PersistentResearchStore
import com.paperconnekt.storage.WorkspaceStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText
import kotlin.io.path.writeText

/**
 * Read-only access to the workspace created by the Android, Desktop, or web app.
 *
 * Native hosts deliberately do not mutate this file yet: the app remains the
 * source of truth, and a plugin refresh never risks overwriting newer changes.
 */
class NativeWorkspaceReader(
    private val storage: WorkspaceStorage = FileWorkspaceStorage.default(),
) {
    suspend fun read(): PluginWorkspaceSnapshot {
        val store = PersistentResearchStore(storage)
        store.initialize()
        return PluginWorkspaceSnapshot(
            papers = store.papers.value,
            graphs = store.graphs.value,
            assistantSessions = store.assistantSessions.value,
            libraryEntries = store.libraryEntries.value,
        )
    }
}

private class FileWorkspaceStorage(
    private val directory: Path,
) : WorkspaceStorage {
    private val dataFile = directory.resolve("workspace.json")
    private val corruptFile = directory.resolve("workspace.json.corrupt")

    override suspend fun read(): String? = withContext(Dispatchers.IO) {
        dataFile.takeIf { it.exists() && it.isRegularFile() }?.readText()
    }

    override suspend fun write(content: String) = withContext(Dispatchers.IO) {
        Files.createDirectories(directory)
        val temporary = Files.createTempFile(directory, "workspace-", ".tmp")
        try {
            temporary.writeText(content)
            Files.move(temporary, dataFile, REPLACE_EXISTING)
        } finally {
            Files.deleteIfExists(temporary)
        }
        Unit
    }

    override suspend fun quarantineCorruptData() = withContext(Dispatchers.IO) {
        if (dataFile.exists()) {
            Files.deleteIfExists(corruptFile)
            Files.move(dataFile, corruptFile, REPLACE_EXISTING)
        }
    }

    companion object {
        fun default(): WorkspaceStorage {
            val configured = System.getProperty("paperconnekt.data.dir")?.takeIf(String::isNotBlank)
            val directory = configured?.let(::Path)
                ?: Path(System.getProperty("user.home"), ".paperconnekt-contest")
            return FileWorkspaceStorage(directory)
        }
    }
}
