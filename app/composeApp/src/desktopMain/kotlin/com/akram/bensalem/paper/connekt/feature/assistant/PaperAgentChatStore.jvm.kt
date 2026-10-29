package com.akram.bensalem.paper.connekt.feature.assistant

import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path
import com.akram.bensalem.paper.connekt.shared.utils.paperConnektDataDirectory
import java.nio.file.StandardCopyOption

/** Stores each paper's chats in `~/.paperconnekt/agent-chats/<user>/<attachment>.json`. */
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
        runCatching { paperAgentChatJson.decodeFromString<StoredPaperAgentChats>(Files.readString(file)).conversations }
            .onFailure { Napier.e("Could not read agent chats from $file", it) }
            .getOrDefault(emptyList())
    }

    override suspend fun save(key: String, conversations: List<PaperAgentConversation>) = withContext(Dispatchers.IO) {
        val file = file(key)
        if (conversations.isEmpty()) {
            Files.deleteIfExists(file)
            return@withContext
        }
        Files.createDirectories(file.parent)
        val temporary = Files.createTempFile(file.parent, "chats", ".json.tmp")
        try {
            Files.writeString(temporary, paperAgentChatJson.encodeToString(StoredPaperAgentChats(conversations = conversations)))
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } finally {
            Files.deleteIfExists(temporary)
        }
    }
}

private val sharedChatStore by lazy { FilePaperAgentChatStore() }

actual fun defaultPaperAgentChatStore(): PaperAgentChatStore = sharedChatStore
