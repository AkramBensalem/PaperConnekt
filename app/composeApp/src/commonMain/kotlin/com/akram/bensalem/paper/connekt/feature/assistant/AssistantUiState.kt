package com.akram.bensalem.paper.connekt.feature.assistant

import com.akram.bensalem.paper.connekt.models.Paper

data class AssistantUiState(
    val history: ResearchHistory = ResearchHistory(),
    val historyLoading: Boolean = false,
    val historyError: String? = null,
    val historyAccountId: String? = null,
    val selectedPaper: Paper? = null,
    val notes: List<Note> = emptyList(),
    val chatMessages: List<ChatMessage> = emptyList(),
    val chatError: String? = null,
    val ideas: List<Idea> = emptyList(),
    val ideasLoading: Boolean = false,
    val ideaError: String? = null,
    val isWorkplaceActive: Boolean = false
) {
    val historyAvailable: Boolean get() = historyAccountId != null
}

data class Note(
    val id: String,
    val content: String,
    val pageNumber: Int? = null,
    val timestamp: Long = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
)

data class ChatMessage(
    val id: String,
    val sender: MessageSender,
    val content: String,
    val timestamp: Long = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
)

enum class MessageSender {
    USER, AI_AGENT
}
