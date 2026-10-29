package com.akram.bensalem.paper.connekt.feature.assistant

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path
import com.akram.bensalem.paper.connekt.shared.utils.paperConnektDataDirectory
import com.paperconnekt.storage.writePrivateFileAtomically

class FileResearchHistoryStore(
    private val root: Path = paperConnektDataDirectory().resolve("research-history")
) : ResearchHistoryStore {
    private val json = Json { ignoreUnknownKeys = true }

    private fun file(userId: String): Path {
        require(userId.isNotBlank()) { "A Zotero user is required to store research history." }
        val name = userId.toByteArray(Charsets.UTF_8).joinToString("") {
            (it.toInt() and 0xff).toString(16).padStart(2, '0')
        }
        return root.resolve("$name.json")
    }

    override suspend fun load(userId: String): ResearchHistory = withContext(Dispatchers.IO) {
        val file = file(userId)
        if (!Files.exists(file)) ResearchHistory()
        else json.decodeFromString<ResearchHistory>(Files.readString(file))
    }

    override suspend fun save(userId: String, history: ResearchHistory): Unit = withContext(Dispatchers.IO) {
        val file = file(userId)
        writePrivateFileAtomically(file, json.encodeToString(history).toByteArray(Charsets.UTF_8))
    }
}

private val sharedResearchHistoryStore by lazy { FileResearchHistoryStore() }

actual fun defaultResearchHistoryStore(): ResearchHistoryStore = sharedResearchHistoryStore
