package com.paperconnekt.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akram.bensalem.paper.connekt.core.aiassistant.AgentEvent
import com.akram.bensalem.paper.connekt.core.aiassistant.AgentRequest
import com.akram.bensalem.paper.connekt.core.aiassistant.AgentRole
import com.akram.bensalem.paper.connekt.core.aiassistant.AgentTurn
import com.akram.bensalem.paper.connekt.core.aiassistant.AgentTool
import com.akram.bensalem.paper.connekt.core.aiassistant.AgentToolParameter
import com.akram.bensalem.paper.connekt.core.aiassistant.ResearchAgent
import com.akram.bensalem.paper.connekt.core.aiassistant.ResearchAgentModel
import com.akram.bensalem.paper.connekt.core.aiassistant.ResearchAgentModels
import com.akram.bensalem.paper.connekt.core.aiassistant.AgentNotConfiguredException
import com.akram.bensalem.paper.connekt.core.aiassistant.createResearchAgent
import com.akram.bensalem.paper.connekt.core.settings.ApiKeysStore
import com.akram.bensalem.paper.connekt.core.settings.createApiKeysStore
import com.akram.bensalem.paper.connekt.feature.assistant.PaperAgentChatStore
import com.akram.bensalem.paper.connekt.feature.assistant.PaperAgentConversation
import com.akram.bensalem.paper.connekt.feature.assistant.PaperAgentLiveStep
import com.akram.bensalem.paper.connekt.feature.assistant.PaperAgentMessage
import com.akram.bensalem.paper.connekt.feature.assistant.defaultIdeaNoteStore
import com.akram.bensalem.paper.connekt.feature.assistant.defaultPaperAgentChatStore
import com.akram.bensalem.paper.connekt.feature.assistant.toolStepLabel
import com.akram.bensalem.paper.connekt.feature.mcp.PaperLibrary
import com.akram.bensalem.paper.connekt.feature.mcp.paperLibraryTools
import com.akram.bensalem.paper.connekt.models.ZoteroConfig
import com.akram.bensalem.paper.connekt.shared.zotero.KtorZoteroClient
import com.paperconnekt.domain.model.ResearchGraph
import com.paperconnekt.secrets.SecretKey
import com.paperconnekt.secrets.SecretStore
import com.paperconnekt.storage.LocalResearchStore
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

private const val GLOBAL_CHAT_KEY = "global-research-chat"

class GlobalResearchAgentViewModel(
    private val store: LocalResearchStore,
    private val secretStore: SecretStore? = null,
    private val chatStore: PaperAgentChatStore = defaultPaperAgentChatStore(),
    private val apiKeysStore: ApiKeysStore = createApiKeysStore(),
    private val agent: ResearchAgent = createResearchAgent(secretStore),
    private val agentContext: CoroutineContext = kotlinx.coroutines.Dispatchers.Default,
) : ViewModel() {
    private val library = PaperLibrary(store)
    private val zoteroClient = KtorZoteroClient()
    private val ideas = defaultIdeaNoteStore()
    private var job: Job? = null
    private var loaded = false

    var conversations by mutableStateOf(listOf<PaperAgentConversation>())
        private set
    var conversation by mutableStateOf(PaperAgentConversation())
        private set
    var input by mutableStateOf("")
    var running by mutableStateOf(false)
        private set
    var streamingText by mutableStateOf("")
        private set
    var storageReady by mutableStateOf(false)
        private set
    var storageError by mutableStateOf<String?>(null)
        private set
    var providerKeyAvailable by mutableStateOf(false)
        private set
    var providerKeyMessage by mutableStateOf("Checking provider key…")
        private set
    var selectedModel by mutableStateOf(ResearchAgentModels.GPT6_LUNA)
        private set
    val liveSteps = mutableStateListOf<PaperAgentLiveStep>()

    suspend fun load() {
        if (loaded) return
        selectedModel = ResearchAgentModels.fromId(apiKeysStore.apiKeys.first().researchModel)
        refreshProviderKey()
        try {
            conversations = chatStore.load(GLOBAL_CHAT_KEY).sortedByDescending { it.updatedAt }
            conversation = conversations.firstOrNull() ?: PaperAgentConversation()
            loaded = true
            storageReady = true
            storageError = null
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Napier.e("Could not load global research chat", exception)
            storageError = "Could not load saved research chat: ${exception.message.orEmpty()}"
        }
    }

    fun refresh() {
        viewModelScope.launch { loaded = false; load() }
    }

    fun selectConversation(id: String) {
        if (running) return
        conversations.firstOrNull { it.id == id }?.let { conversation = it }
    }

    fun newChat() {
        if (running) return
        conversation = PaperAgentConversation()
        input = ""
    }

    fun deleteConversation(id: String) {
        if (running) return
        viewModelScope.launch {
            val remaining = conversations.filterNot { it.id == id }
            conversations = remaining
            conversation = remaining.firstOrNull() ?: PaperAgentConversation()
            chatStore.save(GLOBAL_CHAT_KEY, remaining)
        }
    }

    fun selectModel(model: ResearchAgentModel) {
        if (running || model == selectedModel) return
        selectedModel = model
        viewModelScope.launch {
            runCatching {
                val keys = apiKeysStore.apiKeys.first()
                apiKeysStore.save(keys.copy(researchModel = model.id))
            }.onFailure { Napier.e("Could not save global research model", it) }
            refreshProviderKey()
        }
    }

    suspend fun refreshProviderKey() {
        providerKeyAvailable = try {
            val key = when (selectedModel.provider) {
                com.akram.bensalem.paper.connekt.core.aiassistant.ResearchAgentProvider.OPEN_AI -> SecretKey.OPENAI
                com.akram.bensalem.paper.connekt.core.aiassistant.ResearchAgentProvider.MISTRAL -> SecretKey.MISTRAL
            }
            secretStore?.read(key)?.isNotBlank() == true
        } catch (exception: Exception) {
            Napier.e("Could not check global research provider key", exception)
            false
        }
        providerKeyMessage = if (providerKeyAvailable) "" else
            "Add your ${selectedModel.provider.name.replace('_', ' ').lowercase().replaceFirstChar(Char::uppercase)} API key in Settings → API keys."
    }

    fun recheckProviderKey() {
        viewModelScope.launch { refreshProviderKey() }
    }

    fun send(text: String = input) {
        val message = text.trim()
        if (message.isEmpty() || running || !storageReady || !providerKeyAvailable) return
        input = ""
        val pending = conversation.copy(
            messages = conversation.messages + PaperAgentMessage(AgentRole.USER, message),
            updatedAt = com.akram.bensalem.paper.connekt.feature.assistant.nowMillis(),
        )
        conversation = pending
        running = true
        streamingText = ""
        liveSteps.clear()
        job = viewModelScope.launch {
            try {
                val tools = globalTools()
                val history = conversation.messages.dropLast(1)
                    .filterNot { it.isError }
                    .map { AgentTurn(it.role, it.text) }
                val request = AgentRequest(
                    systemPrompt = GLOBAL_RESEARCH_SYSTEM_PROMPT,
                    history = history,
                    message = message,
                    tools = tools,
                    summary = conversation.summary,
                )
                var reply = ""
                agent.stream(request).flowOn(agentContext).collect { event ->
                    when (event) {
                        AgentEvent.StepStarted -> streamingText = ""
                        is AgentEvent.TextDelta -> streamingText += event.text
                        is AgentEvent.ToolStarted -> liveSteps += PaperAgentLiveStep(
                            event.callId, toolStepLabel(event.name, event.arguments)
                        )
                        is AgentEvent.ToolFinished -> liveSteps.indexOfFirst { it.id == event.callId }
                            .takeIf { it >= 0 }?.let { index ->
                                liveSteps[index] = liveSteps[index].copy(done = true, ok = event.ok)
                            }
                        is AgentEvent.Completed -> reply = event.text.ifBlank { streamingText.trim() }
                        is AgentEvent.Usage -> Unit
                    }
                }
                append(PaperAgentMessage(AgentRole.ASSISTANT, reply.ifBlank { "I could not produce a response." }))
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: AgentNotConfiguredException) {
                providerKeyMessage = exception.message ?: "Configure an AI provider in Settings."
                append(PaperAgentMessage(AgentRole.ASSISTANT, providerKeyMessage, isError = true))
            } catch (exception: Exception) {
                Napier.e("Global research agent failed", exception)
                append(PaperAgentMessage(AgentRole.ASSISTANT, "I could not complete that request. ${exception.message.orEmpty()}", isError = true))
            } finally {
                running = false
                streamingText = ""
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        running = false
    }

    private suspend fun append(message: PaperAgentMessage) {
        val updated = conversation.copy(
            messages = conversation.messages + message,
            updatedAt = com.akram.bensalem.paper.connekt.feature.assistant.nowMillis(),
        )
        conversation = updated
        conversations = (conversations.filterNot { it.id == updated.id } + updated)
            .sortedByDescending { it.updatedAt }
        chatStore.save(GLOBAL_CHAT_KEY, conversations)
    }

    private fun globalTools(): List<AgentTool> =
        paperLibraryTools(library, ideas) + listOf(
            AgentTool(
                "list_zotero_group_papers",
                "Fetch and list papers directly from a named Zotero group through the connected Zotero account. " +
                    "This does not depend on which Zotero group is selected in PaperConnekt's Library.",
                listOf(
                    AgentToolParameter("group", "Zotero group name or numeric group ID.", com.akram.bensalem.paper.connekt.core.aiassistant.AgentParameterType.STRING),
                    AgentToolParameter("max_results", "Number of papers, 1-100. Defaults to 50.", com.akram.bensalem.paper.connekt.core.aiassistant.AgentParameterType.INTEGER, false),
                ),
            ) { args -> fetchZoteroGroupPapers(args.requireString("group"), args.int("max_results") ?: 50) },
            AgentTool("list_graphs", "List saved research graphs and the papers in each graph.") {
                val graphs = store.graphs.value
                if (graphs.isEmpty()) "No saved research graphs." else graphs.joinToString("\n") { graphSummary(it) }
            },
            AgentTool(
                "get_graph",
                "Read one saved research graph by ID or name, including its papers and connections.",
                listOf(AgentToolParameter("graph", "Graph ID or name.")),
            ) { args ->
                val query = args.requireString("graph")
                val graph = store.graphs.value.firstOrNull {
                    it.id == query || it.name.equals(query, ignoreCase = true)
                } ?: throw IllegalArgumentException("No graph matches '$query'.")
                graphDetails(graph)
            },
        )

    private suspend fun fetchZoteroGroupPapers(groupQuery: String, requestedLimit: Int): String {
        val apiKey = secretStore?.read(SecretKey.ZOTERO).orEmpty()
        val userId = store.settings.value.zoteroUserId
        require(apiKey.isNotBlank() && userId.isNotBlank()) {
            "Zotero is not connected. Add a Zotero API key and user ID in Settings."
        }
        val groups = zoteroClient.listGroups(ZoteroConfig(apiKey = apiKey, userId = userId))
        val normalized = groupQuery.trim()
        val group = normalized.toIntOrNull()?.let { id -> groups.firstOrNull { it.id == id } }
            ?: groups.firstOrNull { it.data.name.equals(normalized, ignoreCase = true) }
            ?: groups.firstOrNull { it.data.name.contains(normalized, ignoreCase = true) }
            ?: throw IllegalArgumentException(
                "No Zotero group matches \"$groupQuery\". Available groups: " +
                    groups.joinToString { "${it.data.name} (${it.id})" }.ifBlank { "none" },
            )
        val config = ZoteroConfig(apiKey = apiKey, userId = userId, groupId = group.id.toString())
        val papers = zoteroClient.listLibraryItems(config)
            .filter { it.data.itemType != "attachment" }
        val pdfParentKeys = zoteroClient.listAttachments(config)
            .filter { attachment ->
                attachment.data.parentItem != null &&
                    (attachment.data.contentType.equals("application/pdf", ignoreCase = true) ||
                        attachment.data.filename?.endsWith(".pdf", ignoreCase = true) == true)
            }
            .mapNotNull { it.data.parentItem }
            .toSet()
        val limit = requestedLimit.coerceIn(1, 100)
        return buildString {
            appendLine(
                "${papers.size} Zotero entr${if (papers.size == 1) "y" else "ies"} in group ${group.data.name} [${group.id}]; " +
                    "${papers.count { it.key in pdfParentKeys }} have PDF attachments" +
                    if (papers.size > limit) ", showing $limit" else "",
            )
            papers.take(limit).forEach { item ->
                val authors = item.data.creators.mapNotNull { creator ->
                    creator.name ?: listOfNotNull(creator.firstName, creator.lastName)
                        .joinToString(" ")
                        .takeIf(String::isNotBlank)
                }
                appendLine(
                    "- [zotero:${item.key}] ${item.data.title ?: "Untitled"}" +
                        " — ${authors.take(3).joinToString().ifBlank { "Unknown authors" }}" +
                        if (item.key in pdfParentKeys) " · PDF available" else " · no PDF attachment",
                )
            }
        }.trimEnd()
    }

    private fun graphSummary(graph: ResearchGraph): String =
        "- [${graph.id}] ${graph.name} · ${graph.paperIds.size} papers · ${graph.connections.size} connections"

    private fun graphDetails(graph: ResearchGraph): String = buildString {
        appendLine("${graph.name} [${graph.id}]")
        val titles = store.papers.value.associateBy { it.id } + graph.discoveredPapers.associateBy { it.id }
        appendLine("Papers:")
        graph.paperIds.forEach { id ->
            appendLine("- $id: ${titles[id]?.title ?: "metadata unavailable"}")
        }
        if (graph.seedPaperIds.isNotEmpty()) appendLine("Seeds: ${graph.seedPaperIds.joinToString()}")
        if (graph.connections.isNotEmpty()) {
            appendLine("Connections:")
            graph.connections.forEach { appendLine("- ${it.fromPaperId} -> ${it.toPaperId} (${it.type})") }
        }
        if (graph.discoveredPapers.isNotEmpty()) {
            appendLine("Discovered papers:")
            graph.discoveredPapers.forEach { appendLine("- ${it.id}: ${it.title}") }
        }
    }.trimEnd()

    override fun onCleared() {
        job?.cancel()
        library.close()
        super.onCleared()
    }
}

private const val GLOBAL_RESEARCH_SYSTEM_PROMPT = """
You are PaperConnekt's global research assistant. You work across the user's local library, cached Zotero library,
saved Idea Board Markdown notes, and research graphs. This conversation is not tied to one PDF.

Use list_zotero_context first when the user mentions a Zotero group or collection; it exposes the same cached group and
collection context selected in PaperConnekt's Library. For a request to list papers in a named Zotero group, use list_zotero_group_papers; it fetches that group directly and
does not depend on the group selected in the Library page. Use list_zotero_papers for the currently cached/selected
Zotero library and its collection filter when a collection is specified. When asked for PDF counts or availability,
call the relevant Zotero listing tool and report only the attachment data it returns; do not infer PDF availability
from metadata or a previous response.
Use list_papers or search_library to identify a set of papers before comparing them. Use get_paper, get_outline,
search_paper, read_chunks, and read_pages to ground claims in source text. Use list_graphs and get_graph when the user
asks about a graph or wants ideas extracted from graph structure. Use add_idea_note only when the user explicitly asks
to save a note. Papers without converted text can still provide metadata, but never invent full-text findings.

Answer in clean Markdown with concise headings, bullets, tables, and citations that include the paper ID, page, and
chunk ID returned by the tools. State clearly when evidence is unavailable or when a paper is only metadata.
"""
