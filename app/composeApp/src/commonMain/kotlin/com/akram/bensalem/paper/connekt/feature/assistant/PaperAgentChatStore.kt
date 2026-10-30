package com.akram.bensalem.paper.connekt.feature.assistant

import com.akram.bensalem.paper.connekt.core.aiassistant.AgentRole
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.random.Random

@Serializable
data class PaperAgentMessage(
    val role: AgentRole,
    val text: String,
    val quote: String? = null,
    val actions: List<PaperAgentAction> = emptyList(),
    val isError: Boolean = false,
    val id: Long = Random.nextLong(),
    /** Passages the agent retrieved for this reply; [PaperAgentSource.cited] marks the ones the reply relies on. */
    val sources: List<PaperAgentSource> = emptyList(),
    /** Tools the agent ran for this reply, in order, e.g. `search_paper · "contrastive loss"`. */
    val steps: List<String> = emptyList(),
    /** Chunk IDs the reply cited that do not exist in the paper; non-empty means the answer needs a careful read. */
    val unverifiedCitations: List<String> = emptyList(),
)

/** A paper passage the agent read; links the reply to its evidence by page and section. */
@Serializable
data class PaperAgentSource(
    val chunkId: String,
    val firstPage: Int,
    val lastPage: Int = firstPage,
    val section: String,
    val kind: String,
    val excerpt: String,
    /** How the agent found it: keyword, meaning, keyword + meaning, exact, read or cited (older chats: relevant). */
    val via: String,
    /** BM25 keyword score, when found by keyword search alone. */
    val score: Double? = null,
    /** 0–1 fused keyword and meaning relevance, when found by hybrid search (1 = top of both rankings). */
    val relevance: Double? = null,
    val cited: Boolean = false,
) {
    val pageLabel: String get() = if (firstPage == lastPage) "p. $firstPage" else "pp. $firstPage–$lastPage"
}

/** Something the agent changed in the workspace; rendered as a clickable chip under its reply. */
@kotlinx.serialization.Serializable
data class PaperAgentAction(
    val label: String, val pageIndex: Int, val annotationKey: String? = null, val ideaId: String? = null
)

/** One conversation with the research agent about a paper. Messages before [compactedCount] are replaced by [summary] when prompting. */
@Serializable
data class PaperAgentConversation(
    val id: String = newConversationId(),
    val messages: List<PaperAgentMessage> = emptyList(),
    val summary: String? = null,
    val compactedCount: Int = 0,
    val createdAt: Long = nowMillis(),
    val updatedAt: Long = createdAt,
) {
    val title: String
        get() = messages.firstOrNull { it.role == AgentRole.USER }?.text?.lineSequence()?.firstOrNull { it.isNotBlank() }
            ?.trim()?.let { if (it.length > 60) it.take(57).trimEnd() + "…" else it } ?: "New chat"

    /** Messages still sent verbatim to the model. */
    val activeMessages: List<PaperAgentMessage> get() = messages.drop(compactedCount.coerceIn(0, messages.size))
}

/** Persists the conversations of each paper so they can be resumed and revisited. */
interface PaperAgentChatStore {
    suspend fun load(key: String): List<PaperAgentConversation>
    suspend fun save(key: String, conversations: List<PaperAgentConversation>)
}

class InMemoryPaperAgentChatStore : PaperAgentChatStore {
    private val saved = mutableMapOf<String, List<PaperAgentConversation>>()
    override suspend fun load(key: String) = saved[key].orEmpty()
    override suspend fun save(key: String, conversations: List<PaperAgentConversation>) { saved[key] = conversations }
}

expect fun defaultPaperAgentChatStore(): PaperAgentChatStore

@Serializable
data class StoredPaperAgentChats(val version: Int = 1, val conversations: List<PaperAgentConversation> = emptyList())

val paperAgentChatJson = Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = false }

fun nowMillis(): Long = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()

fun newConversationId(): String = nowMillis().toString(36) + "-" + Random.nextLong().toULong().toString(36)
