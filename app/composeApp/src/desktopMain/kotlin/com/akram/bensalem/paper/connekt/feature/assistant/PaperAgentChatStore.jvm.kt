package com.akram.bensalem.paper.connekt.feature.assistant

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path
import com.akram.bensalem.paper.connekt.shared.utils.paperConnektDataDirectory
import com.paperconnekt.storage.writePrivateFileAtomically

/** Stores chats in the contest app's private, attachment-scoped data directory. */
class FilePaperAgentChatStore(
    private val root: Path = paperConnektDataDirectory().resolve("agent-chats"),
) : PaperAgentChatStore {
    private fun file(key: String): Path {
        val segments = key.split('/').filter { it.isNotBlank() }.map(::safePathSegment).ifEmpty { listOf("_") }
        return segments.dropLast(1).fold(root) { path, segment -> path.resolve(segment) }.resolve(segments.last() + ".json")
    }

    override suspend fun load(key: String): List<PaperAgentConversation> = withContext(Dispatchers.IO) {
        val file = file(key)
        if (!Files.isRegularFile(file)) return@withContext emptyList()
        paperAgentChatJson.decodeFromString<StoredPaperAgentChats>(Files.readString(file)).conversations
    }

    override suspend fun save(key: String, conversations: List<PaperAgentConversation>) = withContext(Dispatchers.IO) {
        val file = file(key)
        if (conversations.isEmpty()) {
            Files.deleteIfExists(file)
            return@withContext
        }
        writePrivateFileAtomically(
            file,
            paperAgentChatJson.encodeToString(StoredPaperAgentChats(conversations = conversations)).toByteArray(Charsets.UTF_8),
        )
    }
}

private val sharedChatStore by lazy { FilePaperAgentChatStore() }

actual fun defaultPaperAgentChatStore(): PaperAgentChatStore = sharedChatStore
