package com.akram.bensalem.paper.connekt.feature.assistant

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akram.bensalem.paper.connekt.core.aiassistant.*
import io.github.aakira.napier.Napier
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlin.random.Random
import kotlin.coroutines.CoroutineContext
import com.akram.bensalem.paper.connekt.core.settings.ApiKeysStore
import com.akram.bensalem.paper.connekt.core.settings.createApiKeysStore
import kotlinx.coroutines.flow.first
import com.paperconnekt.secrets.SecretKey
import com.paperconnekt.secrets.SecretStore


/** A passage the user explicitly asked the agent about, kept even if the PDF selection is cleared before sending. */
data class PinnedPassage(val text: String, val pageIndex: Int)

enum class AnnotationAiAction(val label: String, val instruction: String) {
    Describe("Describe", "Describe what the selected passage says and means in clear language."),
    ExpandAbbreviation("What does it stand for?", "Identify and expand any abbreviation or acronym in the selected passage, then explain it briefly."),
    Rephrase("Rephrase", "Rephrase the existing comment so it is clearer and more natural while preserving its meaning."),
    Correct("Correct", "Correct grammar, spelling, structure, and factual wording in the existing comment without changing its intended meaning."),
    AddDetails("Add details", "Improve the existing comment by adding useful details grounded in the selected passage and supplied context."),
}

class PaperAgentViewModel(
    private val workspace: PdfWorkspaceViewModel,
    private val secretStore: SecretStore? = null,
    private val agent: ResearchAgent = createResearchAgent(secretStore),
    private val ui: CoroutineContext = Dispatchers.Main,
    private val agentContext: CoroutineContext = Dispatchers.Default,
    private val store: PaperAgentChatStore = syncedPaperAgentChatStore(),
    private val autoCompactChars: Int = 24_000,
    private val autoCompactMessages: Int = 16,
    private val ideaBoard: AssistantViewModel? = null,
    private val usageRecorder: suspend (String?, AiUsageEvent) -> Unit = { _, _ -> },
    private val apiKeysStore: ApiKeysStore = createApiKeysStore(),
) : ViewModel() {
    /** The open conversation. */
    var conversation by mutableStateOf(PaperAgentConversation())
        private set
    /** Saved conversations about this paper (newest first), including the open one once it has messages. */
    var conversations by mutableStateOf(listOf<PaperAgentConversation>())
        private set
    val messages: List<PaperAgentMessage> get() = conversation.messages
    var showHistory by mutableStateOf(false)
    var input by mutableStateOf("")
    var pinned by mutableStateOf<PinnedPassage?>(null)
        private set
    var running by mutableStateOf(false)
        private set
    var compacting by mutableStateOf(false)
        private set
    var includePageContext by mutableStateOf(true)
    var includePaperContext by mutableStateOf(true)
    var pageContextStart by mutableStateOf(0)
    var pageContextEnd by mutableStateOf(0)
    var annotationAiRunning by mutableStateOf(false)
        private set
    var annotationAiError by mutableStateOf<String?>(null)
        private set
    var selectedModel by mutableStateOf(ResearchAgentModels.GPT6_LUNA)
        private set
    /** Changes made during the current run, shown live while the agent works. */
    val liveActions = mutableStateListOf<PaperAgentAction>()
    private var job: Job? = null
    private var loadedKey: String? = null
    private var modelLoaded = false
    private val saveLock = Mutex()
    var storageReady by mutableStateOf(false)
        private set
    var storageError by mutableStateOf<String?>(null)
        private set
    var providerKeyMessage by mutableStateOf("Checking provider key…")
        private set
    var providerKeyAvailable by mutableStateOf(false)
        private set

    /** Loads the saved chats of the open paper once its storage key is known, resuming the most recent one. */
    suspend fun load() {
        if (!modelLoaded) {
            selectedModel = ResearchAgentModels.fromId(apiKeysStore.apiKeys.first().researchModel)
            modelLoaded = true
        }
        refreshProviderKey()
        val key = workspace.paperStorageKey() ?: return
        saveLock.withLock {
            if (key == loadedKey && storageReady) return
            storageReady = false
            try {
                val saved = store.load(key).sortedByDescending { it.updatedAt }
                conversations = saved
                conversation = saved.firstOrNull() ?: PaperAgentConversation()
                loadedKey = key
                storageReady = true
                storageError = null
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Napier.e("Could not load agent chats", exception)
                storageError = "Could not load saved chats: ${exception.message}. Retry before editing."
            }
        }
    }

    fun askAbout(selection: PdfTextSelection?, pageIndex: Int) {
        selection?.text?.takeIf { it.isNotBlank() }?.let { pinned = PinnedPassage(it.trim(), pageIndex) }
    }

    fun unpin() { pinned = null }

    fun selectModel(model: ResearchAgentModel) {
        if (running || model == selectedModel) return
        selectedModel = model
        viewModelScope.launch {
            runCatching {
                val keys = apiKeysStore.apiKeys.first()
                apiKeysStore.save(keys.copy(researchModel = model.id))
            }.onFailure { Napier.e("Could not save research agent model.", it) }
            refreshProviderKey()
        }
    }

    suspend fun refreshProviderKey() {
        val provider = providerName(selectedModel)
        try {
            val key = secretStore?.read(providerSecretKey(selectedModel))?.trim()
            providerKeyAvailable = !key.isNullOrEmpty()
            providerKeyMessage = if (providerKeyAvailable) {
                ""
            } else {
                "Add your $provider API key in Settings → API keys before using AI chat."
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Napier.e("Could not check the $provider API key", exception)
            providerKeyAvailable = false
            providerKeyMessage = "Could not check the saved $provider key. Retry after checking Settings."
        }
    }

    fun generateAnnotationComment(
        selectedText: String,
        existingComment: String,
        pageIndex: Int,
        includePage: Boolean,
        includePaper: Boolean,
        action: AnnotationAiAction,
        brief: Boolean,
        onResult: (String) -> Unit,
    ) {
        if (annotationAiRunning || selectedText.isBlank()) return
        annotationAiError = null
        if (!providerKeyAvailable) {
            annotationAiError = providerKeyMessage
            return
        }
        annotationAiRunning = true
        viewModelScope.launch {
            try {
                if (includePageContext || includePaperContext) workspace.awaitMarkdown()
                val target = workspace.state.target
                val context = buildString {
                    appendLine("Selected text (always relevant):")
                    appendLine(selectedText.trim())
                    appendLine()
                    appendLine("Existing comment (always relevant; preserve or improve it according to the requested action):")
                    appendLine(existingComment.ifBlank { "(empty)" })
                    if (includePage) {
                        val pages = workspace.state.markdown.pages
                        val first = minOf(pageContextStart, pageContextEnd)
                            .coerceIn(0, (pages.size - 1).coerceAtLeast(0))
                        val last = maxOf(pageContextStart, pageContextEnd)
                            .coerceIn(first, (pages.size - 1).coerceAtLeast(first))
                        appendLine()
                        appendLine("Pages ${first + 1}-${last + 1} content:")
                        if (pages.isEmpty()) {
                            appendLine("(Page content is not available yet.)")
                        } else {
                            pages.subList(first, last + 1)
                                .mapIndexed { offset, content -> "Page ${first + offset + 1}:\n$content" }
                                .joinToString("\n\n")
                                .take(16_000)
                                .let(::appendLine)
                        }
                    }
                    if (includePaper) {
                        appendLine()
                        appendLine("Paper context:")
                        appendLine("Title: ${workspace.state.localTitle ?: target?.item?.data?.title ?: "Untitled"}")
                        target?.item?.data?.abstractNote?.takeIf { it.isNotBlank() }?.let {
                            appendLine("Abstract:")
                            appendLine(it.take(8_000))
                        }
                        workspace.state.markdown.content?.take(40_000)?.let {
                            appendLine("Paper Markdown:")
                            appendLine(it)
                        }
                    }
                }
                val request = AgentRequest(
                    systemPrompt = ANNOTATION_COMMENT_SYSTEM_PROMPT,
                    history = emptyList(),
                    message = "${action.instruction} ${if (brief) "Keep the result brief and concise." else "Give enough detail to be useful."}\nReturn only the resulting Markdown note or comment, without a preamble or labels.\n\n$context",
                    tools = emptyList(),
                )
                val answer = withContext(agentContext) {
                    agent.respond(request)
                }.trim()
                recordAgentUsage(request, answer, AiUsageService.CHAT, usagePaperId(target))
                onResult(answer.ifBlank { "No comment was generated." })
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: AgentNotConfiguredException) {
                val message = exception.message ?: "AI responses are not available in this build."
                blockProvider(message)
                annotationAiError = message
            } catch (exception: Exception) {
                Napier.e("Annotation comment generation failed", exception)
                val providerError = providerFailureMessage(exception, providerName(selectedModel))
                if (providerError != null) blockProvider(providerError)
                annotationAiError = providerError ?: "Could not generate an AI comment. Try again."
            } finally {
                annotationAiRunning = false
            }
        }
    }

    fun send(text: String = input) {
        val message = text.trim()
        if (message.isEmpty() || running || !storageReady) return
        PaperAgentCommand.parse(message)?.let {
            input = ""
            run(it)
            return
        }
        if (!providerKeyAvailable) return
        val quote = pinned
        val ideaPaperId = ideaBoard?.state?.selectedPaper?.id
        showHistory = false
        update { copy(messages = messages + PaperAgentMessage(AgentRole.USER, message, quote?.text)) }
        input = ""
        pinned = null
        running = true
        liveActions.clear()
        val actions = mutableListOf<PaperAgentAction>()
        job = viewModelScope.launch {
            try {
                load()
                val pending = conversation.messages.size - 1
                if (conversation.copy(messages = conversation.messages.take(pending)).needsCompaction()) {
                    compactNow(upTo = (pending - KEEP_AFTER_AUTO_COMPACT).coerceAtLeast(conversation.compactedCount))
                }
                workspace.awaitMarkdown()
                var ideaBoardError: String? = null
                val ideas = try {
                    ideaPaperId?.let { ideaBoard?.awaitIdeaNotes(it) }
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Exception) {
                    Napier.e("Could not load the agent's Idea Board context", exception)
                    ideaBoardError = exception.message ?: exception::class.simpleName
                    null
                }
                val toolbox = PaperAgentToolbox(workspace, ui, ideaBoard, ideaPaperId) { action ->
                    actions += action
                    liveActions += action
                }
                val history = conversation.messages.subList(conversation.compactedCount.coerceAtMost(pending), pending)
                    .filterNot { it.isError }
                    .map { turn -> AgentTurn(turn.role, turn.quote?.let { "About: \"${it.take(600)}\"\n${turn.text}" } ?: turn.text) }
                val request = AgentRequest(
                    systemPrompt = paperAgentSystemPrompt(
                        workspace.state,
                        includePageContext = includePageContext,
                        includePaperContext = includePaperContext,
                        pageContextStart = pageContextStart,
                        pageContextEnd = pageContextEnd,
                        ideas = ideas,
                        ideaBoardError = ideaBoardError,
                    ),
                    history = history,
                    message = quote?.let { "About this passage on page ${it.pageIndex + 1}:\n> ${it.text.replace("\n", "\n> ")}\n\n$message" } ?: message,
                    tools = toolbox.tools,
                    summary = conversation.summary
                )
                val reply = withContext(agentContext) { agent.respond(request) }
                recordAgentUsage(request, reply, AiUsageService.CHAT)
                append(PaperAgentMessage(AgentRole.ASSISTANT, reply.ifBlank { "Done." }, actions = actions.toList()))
            } catch (exception: CancellationException) {
                append(PaperAgentMessage(AgentRole.ASSISTANT, "Stopped.", actions = actions.toList(), isError = true))
                throw exception
            } catch (exception: AgentNotConfiguredException) {
                val message = exception.message ?: "AI responses are not available in this build."
                blockProvider(message)
                append(PaperAgentMessage(AgentRole.ASSISTANT, message, isError = true))
            } catch (exception: Exception) {
                Napier.e("Research agent failed", exception)
                val providerError = providerFailureMessage(exception, providerName(selectedModel))
                if (providerError != null) blockProvider(providerError)
                append(PaperAgentMessage(AgentRole.ASSISTANT,
                    providerError ?: "Something went wrong while preparing your assistant request. Try again.",
                    actions = actions.toList(), isError = true))
            } finally {
                running = false
                compacting = false
                liveActions.clear()
            }
        }
    }

    fun run(command: PaperAgentCommand) {
        when (command) {
            PaperAgentCommand.New -> newChat()
            PaperAgentCommand.History -> if (!running) showHistory = !showHistory
            PaperAgentCommand.Compact -> compact()
            PaperAgentCommand.Delete -> deleteConversation(conversation.id)
        }
    }

    val canCompact: Boolean get() = providerKeyAvailable && !running &&
        conversation.activeMessages.count { !it.isError } >= 2

    /** Summarizes everything said so far; only the summary is sent to the model from now on, while messages stay visible. */
    fun compact() {
        if (!canCompact) return
        showHistory = false
        running = true
        job = viewModelScope.launch {
            try {
                compactNow(upTo = conversation.messages.size)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Napier.e("Compacting the agent conversation failed", exception)
                val providerError = providerFailureMessage(exception, providerName(selectedModel))
                if (providerError != null) blockProvider(providerError)
                append(PaperAgentMessage(AgentRole.ASSISTANT,
                    providerError ?: "Could not compact the conversation. Try again.", isError = true))
            } finally {
                running = false
                compacting = false
            }
        }
    }

    private suspend fun compactNow(upTo: Int) {
        val start = conversation.compactedCount
        val end = upTo.coerceIn(start, conversation.messages.size)
        val older = conversation.messages.subList(start, end)
        if (older.none { !it.isError }) return
        compacting = true
        val id = conversation.id
        val request = AgentRequest(COMPACTION_SYSTEM_PROMPT, emptyList(), compactionRequest(conversation.summary, older), emptyList())
        val summary = withContext(agentContext) { agent.respond(request) }.trim()
        recordAgentUsage(request, summary, AiUsageService.CHAT)
        compacting = false
        if (summary.isEmpty() || conversation.id != id) return
        update { copy(summary = summary, compactedCount = end) }
    }

    private fun PaperAgentConversation.needsCompaction() =
        activeMessages.size > autoCompactMessages || promptChars() > autoCompactChars

    /** Starts a fresh chat; the current one stays in the history. */
    fun newChat() {
        if (running || !storageReady) return
        showHistory = false
        pinned = null
        if (conversation.messages.isNotEmpty()) conversation = PaperAgentConversation()
    }

    fun openConversation(id: String) {
        if (running) return
        conversations.firstOrNull { it.id == id }?.let { conversation = it }
        showHistory = false
    }

    fun deleteConversation(id: String) {
        if (running || !storageReady) return
        conversations = conversations.filterNot { it.id == id }
        if (conversation.id == id) conversation = PaperAgentConversation()
        persist()
    }

    fun stop() { job?.cancel() }

    fun open(action: PaperAgentAction) {
        val key = action.annotationKey
        if (key != null && workspace.state.annotations.any { it.key == key }) workspace.revealAnnotation(key)
        else {
            if (workspace.state.viewMode != ReaderViewMode.Pdf) workspace.showPdf()
            workspace.goToPage(action.pageIndex)
        }
    }

    private fun append(message: PaperAgentMessage) = update { copy(messages = messages + message) }

    private fun update(block: PaperAgentConversation.() -> PaperAgentConversation) {
        val updated = conversation.block().copy(updatedAt = nowMillis())
        conversation = updated
        conversations = listOf(updated) + conversations.filterNot { it.id == updated.id }
        persist()
    }

    private fun persist() {
        if (!storageReady) return
        val key = loadedKey ?: return
        val snapshot = conversations.filter { it.messages.isNotEmpty() }
        viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            withContext(NonCancellable) {
                saveLock.withLock {
                    try {
                        store.save(key, snapshot)
                        storageError = null
                    } catch (exception: CancellationException) {
                        throw exception
                    } catch (exception: Exception) {
                        Napier.e("Could not save agent chats", exception)
                        storageError = "Chat changes have not been saved: ${exception.message}"
                    }
                }
            }
        }
    }

    fun retryStorage() {
        if (storageReady) persist() else viewModelScope.launch { load() }
    }

    private suspend fun recordAgentUsage(
        request: AgentRequest,
        response: String,
        service: AiUsageService,
        paperId: String? = usagePaperId(workspace.state.target),
    ) {
        val input = buildString {
            append(request.systemPrompt)
            request.summary?.let(::append)
            request.history.forEach { append(it.text) }
            append(request.message)
        }
        usageRecorder(
            paperId,
            AiUsageEvent(
                service = service,
                timestamp = nowMillis(),
                inputUnits = input.length.toLong() / 4L,
                outputUnits = response.length.toLong() / 4L,
                model = selectedModel.id,
            )
        )
    }

    private companion object {
        const val KEEP_AFTER_AUTO_COMPACT = 4
        const val ANNOTATION_COMMENT_SYSTEM_PROMPT = """You write concise Markdown comments for annotations in a scientific PDF.
Use the selected text as the central subject. Use page or paper context only when supplied and relevant.
Return only the note/comment content, without a preamble, quotation marks, or labels. Do not invent facts. Prefer a short explanation, definition, implication, or connection that helps the researcher understand the passage."""
    }

    private fun providerSecretKey(model: ResearchAgentModel): SecretKey =
        if (model == ResearchAgentModels.MISTRAL_MEDIUM_31) SecretKey.MISTRAL else SecretKey.OPENAI

    private fun providerName(model: ResearchAgentModel): String =
        if (model == ResearchAgentModels.MISTRAL_MEDIUM_31) "Mistral" else "OpenAI"

    private fun blockProvider(message: String) {
        providerKeyAvailable = false
        providerKeyMessage = message
    }
}

internal fun usagePaperId(target: ZoteroPdfTarget?): String? =
    target?.groupId?.let { "group:$it:${target.attachment.key}" }
        ?: target?.attachment?.key?.let { "personal:$it" }
