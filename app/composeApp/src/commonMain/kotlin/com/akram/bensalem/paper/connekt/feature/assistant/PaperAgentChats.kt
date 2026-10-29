package com.akram.bensalem.paper.connekt.feature.assistant

import com.akram.bensalem.paper.connekt.core.aiassistant.AgentRole
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.random.Random


/** Slash commands understood by the agent chat input. */
enum class PaperAgentCommand(val command: String, val description: String) {
    New("/new", "Start a new chat (the current one is kept in history)"),
    History("/history", "Browse and reopen earlier chats about this paper"),
    Compact("/compact", "Summarize earlier messages to keep the context small"),
    Delete("/delete", "Delete the current chat");

    companion object {
        fun parse(text: String): PaperAgentCommand? = text.trim().lowercase().let { input -> entries.firstOrNull { it.command == input } }
        fun matching(text: String): List<PaperAgentCommand> {
            val input = text.trimStart().lowercase()
            if (!input.startsWith("/") || input.contains(' ')) return emptyList()
            return entries.filter { it.command.startsWith(input) }
        }
    }
}

internal const val COMPACTION_SYSTEM_PROMPT = """You compact a conversation between a researcher and an AI research assistant about one scientific paper.
Write a faithful, dense summary that lets the assistant continue the conversation without the original messages.
Keep: the user's goals and preferences, questions asked and the key answers (with page numbers), definitions, formulas (LaTeX), decisions, and every change made to the PDF (annotations, highlights, notes, ideas, drawings with their pages and colors).
Drop pleasantries and repetition. Use concise Markdown bullet points grouped under short bold labels. Do not add anything that was not said."""

internal fun compactionRequest(previousSummary: String?, messages: List<PaperAgentMessage>, maxChars: Int = 60_000): String = buildString {
    previousSummary?.takeIf { it.isNotBlank() }?.let {
        appendLine("Summary of the even earlier conversation:")
        appendLine(it.trim())
        appendLine()
    }
    appendLine("Messages to compact:")
    val transcript = messages.filterNot { it.isError }.joinToString("\n") { message ->
        val who = if (message.role == AgentRole.USER) "User" else "Assistant"
        val about = message.quote?.let { " (about: \"${it.take(300)}\")" }.orEmpty()
        val changes = message.actions.takeIf { it.isNotEmpty() }
            ?.joinToString(prefix = "\n  [PDF changes: ", postfix = "]") { "p. ${it.pageIndex + 1} ${it.label}" }.orEmpty()
        "$who$about: ${message.text}$changes"
    }
    appendLine(if (transcript.length > maxChars) "…" + transcript.takeLast(maxChars) else transcript)
    appendLine()
    append("Write the updated summary now. Reply with the summary only.")
}

/** Rough size of what a send would include, used to compact automatically before the context grows too large. */
internal fun PaperAgentConversation.promptChars(): Int =
    (summary?.length ?: 0) + activeMessages.sumOf { it.text.length + (it.quote?.length ?: 0) }
