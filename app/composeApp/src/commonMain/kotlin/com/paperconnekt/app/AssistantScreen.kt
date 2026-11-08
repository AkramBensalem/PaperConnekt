package com.paperconnekt.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.paperconnekt.domain.model.AssistantMessage
import com.paperconnekt.domain.model.AssistantRole
import com.paperconnekt.domain.model.AssistantSession
import com.paperconnekt.domain.model.Paper
import com.paperconnekt.storage.LocalResearchStore
import com.paperconnekt.secrets.SecretStore
import com.akram.bensalem.paper.connekt.feature.assistant.ZoteroPdfTarget
import com.akram.bensalem.paper.connekt.feature.assistant.AssistantRepository
import com.akram.bensalem.paper.connekt.feature.assistant.AssistantViewModel
import kotlin.random.Random
import com.paperconnekt.app.resources.Res
import com.paperconnekt.app.resources.filled_arrow_back
import com.paperconnekt.app.resources.filled_arrow_forward
import com.paperconnekt.app.resources.lightbulb
import androidx.lifecycle.viewmodel.compose.viewModel
import com.akram.bensalem.paper.connekt.core.aiassistant.AgentRole
import com.akram.bensalem.paper.connekt.feature.assistant.PaperAgentConversation
import com.akram.bensalem.paper.connekt.feature.assistant.PaperAgentMessage
import com.akram.bensalem.paper.connekt.feature.assistant.component.ChatMarkdown
import com.paperconnekt.app.resources.filled_auto_awesome
import com.paperconnekt.app.resources.filled_content_copy
import com.paperconnekt.app.resources.filled_stop
import org.jetbrains.compose.resources.painterResource
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

@Composable
fun AssistantScreen(
    store: LocalResearchStore,
    paddingValues: PaddingValues,
    initialPaper: Paper? = null,
    onInitialPaperConsumed: () -> Unit = {},
    secretStore: SecretStore? = null,
    initialZoteroTarget: ZoteroPdfTarget? = null,
    onInitialZoteroTargetConsumed: () -> Unit = {},
    onPdfWorkspaceOpened: () -> Unit = {},
    onPdfWorkspaceClosed: () -> Unit = {},
    onConversationFocusedChanged: (Boolean) -> Unit = {},
    focusedConversationPage: Boolean = false,
    onOpenPaper: (Paper) -> Unit = {},
    onOpenZoteroTarget: (ZoteroPdfTarget) -> Unit = {},
) {
    val papers by store.papers.collectAsState()
    val settings by store.settings.collectAsState()
    val historyAgent = viewModel { AssistantViewModel(AssistantRepository()) }
    val globalAgent = viewModel {
        GlobalResearchAgentViewModel(store = store, secretStore = secretStore)
    }
    LaunchedEffect(settings.zoteroUserId) {
        historyAgent.updateHistoryAccount(settings.zoteroUserId.takeIf(String::isNotBlank))
    }
    LaunchedEffect(globalAgent) {
        globalAgent.load()
        globalAgent.refreshProviderKey()
    }
    var openedConversationId by remember { mutableStateOf<String?>(null) }
    var openPaper by remember { mutableStateOf<Paper?>(null) }
    var openZoteroTarget by remember { mutableStateOf<ZoteroPdfTarget?>(null) }
    SideEffect {
        onConversationFocusedChanged(openedConversationId != null)
    }
    LaunchedEffect(openedConversationId) {
        globalAgent.refreshProviderKey()
    }
    LaunchedEffect(initialPaper) {
        if (initialPaper != null) {
            onPdfWorkspaceOpened()
            openPaper = initialPaper
            onInitialPaperConsumed()
        }
    }
    LaunchedEffect(initialZoteroTarget) {
        if (initialZoteroTarget != null) {
            onPdfWorkspaceOpened()
            openZoteroTarget = initialZoteroTarget
            onInitialZoteroTargetConsumed()
        }
    }
    var isCreating by remember { mutableStateOf(false) }
    fun openConversation(id: String?) {
        openedConversationId = id
        onConversationFocusedChanged(id != null)
    }
    if (openPaper != null) {
        PdfWorkspaceScreen(
            paper = openPaper!!,
            store = store,
            paddingValues = paddingValues,
            onBack = {
                openPaper = null
                onPdfWorkspaceClosed()
            },
            secretStore = secretStore,
        )
        return
    }
    if (openZoteroTarget != null) {
        ZoteroPdfWorkspaceScreen(
            target = openZoteroTarget!!,
            store = store,
            paddingValues = paddingValues,
            secretStore = secretStore,
            zoteroUserId = settings.zoteroUserId,
            onBack = {
                openZoteroTarget = null
                onPdfWorkspaceClosed()
            },
        )
        return
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .background(MaterialTheme.colorScheme.surfaceContainer),
    ) {
        val compact = maxWidth < 1120.dp
        val narrow = maxWidth < 420.dp
        if (compact && openedConversationId == null) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = if (narrow) 12.dp else 16.dp,
                        vertical = 14.dp,
                    ),
                contentPadding = PaddingValues(bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    AssistantHeader(
                        sessionCount = globalAgent.conversations.size,
                    )
                }
                item {
                    SessionStrip(
                        conversations = globalAgent.conversations,
                        selectedConversationId = openedConversationId,
                        onSelect = {
                            globalAgent.selectConversation(it)
                            openConversation(it)
                        },
                        onNewConversation = {
                            if (!isCreating) {
                                isCreating = true
                                globalAgent.newChat()
                                openConversation(globalAgent.conversation.id)
                                isCreating = false
                            }
                        },
                    )
                }
                item {
                    RecentResearchShelf(
                        localPapers = papers,
                        history = historyAgent.state.history,
                        onOpenPaper = {
                            onOpenPaper(it)
                        },
                        onOpenZotero = {
                            onOpenZoteroTarget(it)
                        },
                    )
                }
                item {
                    PaperShelf(
                        papers = papers,
                        onOpenPaper = {
                            onOpenPaper(it)
                        },
                    )
                }
            }
        } else if (compact) {
            AssistantMainContent(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = if (narrow) 8.dp else 12.dp, vertical = 8.dp),
                papers = papers,
                globalAgent = globalAgent,
                recentHistory = historyAgent.state.history,
                conversationOpened = true,
                focusedConversationPage = focusedConversationPage,
                onBackToHistory = { openConversation(null) },
                onOpenZotero = {
                    onOpenZoteroTarget(it)
                },
                onOpenPaper = {
                    onOpenPaper(it)
                },
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = if (narrow) 12.dp else if (compact) 16.dp else 28.dp,
                        vertical = if (compact) 14.dp else 20.dp,
                    ),
                verticalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 16.dp),
            ) {
                AnimatedVisibility(
                    visible = openedConversationId == null,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    AssistantHeader(
                        sessionCount = globalAgent.conversations.size,
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    SessionSidebar(
                        modifier = Modifier.width(270.dp),
                        conversations = globalAgent.conversations,
                        selectedConversationId = openedConversationId,
                        onSelect = {
                            globalAgent.selectConversation(it)
                            openConversation(it)
                        },
                        onNewConversation = {
                            if (!isCreating) {
                                isCreating = true
                                globalAgent.newChat()
                                openConversation(globalAgent.conversation.id)
                                isCreating = false
                            }
                        },
                    )
                    AssistantMainContent(
                        modifier = Modifier.weight(1f),
                        papers = papers,
                        globalAgent = globalAgent,
                        recentHistory = historyAgent.state.history,
                        conversationOpened = openedConversationId != null,
                        focusedConversationPage = focusedConversationPage,
                        onBackToHistory = { openConversation(null) },
                        onOpenZotero = {
                            onOpenZoteroTarget(it)
                        },
                        onOpenPaper = {
                            onOpenPaper(it)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun AssistantHeader(
    sessionCount: Int,
) {
    WorkspaceHeader(
        title = "Assistant",
        subtitle = "Explore questions, read deeply, and turn papers into understanding.",
        icon = Res.drawable.lightbulb,
        summaries = listOf(
            "$sessionCount conversation${if (sessionCount == 1) "" else "s"}",
            "History saved locally",
        ),
    )
}

@Composable
private fun SessionSidebar(
    modifier: Modifier,
    conversations: List<PaperAgentConversation>,
    selectedConversationId: String?,
    onSelect: (String) -> Unit,
    onNewConversation: () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxHeight(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Research history", style = MaterialTheme.typography.titleMedium)
                IconButton(
                    onClick = onNewConversation,
                    modifier = Modifier.semantics { contentDescription = "Create conversation" },
                ) {
                    Text("+", style = MaterialTheme.typography.titleLarge)
                }
            }
            Divider()
            if (conversations.isEmpty()) {
                Text(
                    "No conversations yet. Your recently opened PDFs are shown beside this history.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(conversations.sortedByDescending { it.updatedAt }, key = PaperAgentConversation::id) { conversation ->
                        ConversationListItem(
                            conversation = conversation,
                            selected = conversation.id == selectedConversationId,
                            onClick = { onSelect(conversation.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionStrip(
    conversations: List<PaperAgentConversation>,
    selectedConversationId: String?,
    onSelect: (String) -> Unit,
    onNewConversation: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Chat history", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (conversations.isEmpty()) {
                            "Your saved research conversations will appear here."
                        } else {
                            "Select a conversation to continue your research."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedButton(
                    onClick = onNewConversation,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp),
                ) {
                    Text("+ New")
                }
            }
            if (conversations.isEmpty()) {
                Text(
                    "No chats yet",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(
                        conversations.sortedByDescending { it.updatedAt },
                        key = PaperAgentConversation::id,
                    ) { conversation ->
                        FilterChip(
                            selected = conversation.id == selectedConversationId,
                            onClick = { onSelect(conversation.id) },
                            label = {
                                Text(
                                    conversation.title,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationListItem(
    conversation: PaperAgentConversation,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        color = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                conversation.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                conversation.messages.lastOrNull()?.text ?: "No messages yet",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun AssistantMainContent(
    modifier: Modifier,
    papers: List<Paper>,
    globalAgent: GlobalResearchAgentViewModel,
    recentHistory: com.akram.bensalem.paper.connekt.feature.assistant.ResearchHistory,
    conversationOpened: Boolean,
    focusedConversationPage: Boolean,
    onBackToHistory: () -> Unit,
    onOpenZotero: (ZoteroPdfTarget) -> Unit,
    onOpenPaper: (Paper) -> Unit,
) {
    if (conversationOpened) {
        GlobalConversationPanel(
            modifier = modifier,
            agent = globalAgent,
            onBackToHistory = onBackToHistory,
            focusedPage = focusedConversationPage,
        )
    } else {
        LazyColumn(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 4.dp),
        ) {
            item {
                RecentResearchShelf(
                    localPapers = papers,
                    history = recentHistory,
                    onOpenPaper = onOpenPaper,
                    onOpenZotero = onOpenZotero,
                )
            }
            item {
                PaperShelf(
                    papers = papers,
                    onOpenPaper = onOpenPaper,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GlobalConversationPanel(
    modifier: Modifier,
    agent: GlobalResearchAgentViewModel,
    onBackToHistory: () -> Unit,
    focusedPage: Boolean,
) {
    val scrollState = rememberLazyListState()
    val headerCollapsed by remember {
        derivedStateOf {
            scrollState.firstVisibleItemIndex > 0 ||
                scrollState.firstVisibleItemScrollOffset > 32
        }
    }
    var draft by remember { mutableStateOf("") }
    LaunchedEffect(agent.conversation.id, agent.conversation.messages.size, agent.running) {
        val count = agent.conversation.messages.size + if (agent.running) 1 else 0
        if (count > 0) scrollState.animateScrollToItem(count - 1)
    }
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(if (focusedPage) 0.dp else 16.dp),
        border = if (focusedPage) {
            null
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        },
    ) {
        Column(Modifier.fillMaxSize()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = if (headerCollapsed) 3.dp else 0.dp,
                shadowElevation = if (headerCollapsed) 2.dp else 0.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Row(
                    Modifier.padding(
                        start = if (headerCollapsed) 10.dp else 18.dp,
                        top = if (headerCollapsed) 6.dp else 14.dp,
                        end = 10.dp,
                        bottom = if (headerCollapsed) 6.dp else 10.dp,
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    IconButton(onClick = onBackToHistory) {
                        Icon(
                            painterResource(Res.drawable.filled_arrow_back),
                            contentDescription = "Back to chat history",
                        )
                    }
                    AnimatedVisibility(visible = !headerCollapsed) {
                        Icon(
                            painterResource(Res.drawable.filled_auto_awesome),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            agent.conversation.title,
                            style = if (headerCollapsed) {
                                MaterialTheme.typography.titleMedium
                            } else {
                                MaterialTheme.typography.titleLarge
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    IconButton(onClick = agent::stop, enabled = agent.running) {
                        Icon(painterResource(Res.drawable.filled_stop), "Stop")
                    }
                }
            }
            agent.storageError?.let {
                Surface(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text(it, Modifier.padding(10.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
            if (!agent.providerKeyAvailable && agent.storageReady) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        agent.providerKeyMessage,
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    TextButton(
                        onClick = agent::recheckProviderKey,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text("Recheck")
                    }
                }
            }
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                state = scrollState,
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = if (headerCollapsed) 8.dp else 12.dp,
                    bottom = 12.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (agent.conversation.messages.isEmpty() && !agent.running) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Ask across your research library", style = MaterialTheme.typography.headlineSmall)
                            Text(
                                "Compare a set of papers, search converted PDF text, inspect Idea Board Markdown, or extract connections from a research graph.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(
                                    "Compare the methods in my latest papers",
                                    "Find papers about retrieval augmented generation",
                                    "Summarize the papers in my latest graph",
                                ).forEach { prompt ->
                                    SuggestionChip(
                                        onClick = { agent.send(prompt) },
                                        enabled = agent.storageReady && agent.providerKeyAvailable,
                                        label = { Text(prompt) },
                                    )
                                }
                            }
                        }
                    }
                }
                items(agent.conversation.messages, key = { it.id }) { message ->
                    GlobalMessageBubble(message)
                }
                if (agent.running) {
                    item {
                        Column(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (agent.streamingText.isNotBlank()) ChatMarkdown(agent.streamingText)
                            agent.liveSteps.forEach { step ->
                                Text(
                                    "${if (step.done) "✓" else "…"} ${step.label}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (agent.streamingText.isBlank() && agent.liveSteps.isEmpty()) {
                                Text("Thinking…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 3.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = draft,
                        onValueChange = { draft = it },
                        enabled = agent.storageReady && !agent.running,
                        label = { Text("Ask your research library") },
                        placeholder = {
                            Text("Compare papers, inspect a graph, or save an Idea Board note…")
                        },
                        minLines = 2,
                        maxLines = 6,
                        shape = RoundedCornerShape(16.dp),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        Button(
                            enabled = draft.isNotBlank() &&
                                agent.storageReady &&
                                agent.providerKeyAvailable &&
                                !agent.running,
                            onClick = {
                                agent.send(draft)
                                draft = ""
                            },
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                        ) {
                            Text("Send")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GlobalMessageBubble(message: PaperAgentMessage) {
    val isUser = message.role == AgentRole.USER
    val clipboard = LocalClipboardManager.current
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            Modifier.fillMaxWidth(0.86f),
            shape = RoundedCornerShape(14.dp),
            color = if (isUser) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainerHighest,
        ) {
            Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    if (isUser) "You" else "PaperConnekt assistant",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                SelectionContainer {
                    if (isUser) Text(message.text) else ChatMarkdown(message.text)
                }
                if (message.isError) {
                    Text(
                        "The response failed; you can retry the same question.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                if (!isUser && !message.isError && message.text.isNotBlank()) {
                    IconButton(
                        onClick = { clipboard.setText(AnnotatedString(message.text)) },
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            painterResource(Res.drawable.filled_content_copy),
                            contentDescription = "Copy reply",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomePanel(onCreate: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Start a research conversation", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Ask questions about your saved papers, capture a direction, or open a paper in the reading workspace. Conversations stay local until you connect a provider.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onCreate) { Text("Create conversation") }
        }
    }
}

@Composable
private fun ConversationPanel(
    session: AssistantSession,
    onSessionUpdated: (AssistantSession) -> Unit,
    onSessionDeleted: (String) -> Unit,
) {
    var draft by remember(session.id) { mutableStateOf("") }
    var renameDialog by remember { mutableStateOf(false) }
    var deleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, top = 14.dp, end = 10.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(session.title, style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Local provider-ready session",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                TextButton(onClick = { renameDialog = true }) { Text("Rename") }
                TextButton(onClick = { deleteDialog = true }) { Text("Delete") }
            }
            Divider()
            if (session.messages.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text("What are you investigating?", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Draft a question below. The local assistant shell will preserve it and show where a provider can respond.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    itemsIndexed(session.messages) { _, message ->
                        MessageBubble(message)
                    }
                }
            }
            Divider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    modifier = Modifier.weight(1f),
                    value = draft,
                    onValueChange = { draft = it },
                    label = { Text("Ask about your research") },
                    placeholder = { Text("e.g. Compare the methods in my saved papers") },
                    minLines = 2,
                )
                Button(
                    enabled = draft.isNotBlank(),
                    onClick = {
                        val message = draft.trim()
                        val now = kotlinx.datetime.Clock.System.now()
                        onSessionUpdated(
                            session.copy(
                                title = if (session.title == "New research conversation") {
                                    message.take(48)
                                } else {
                                    session.title
                                },
                                messages = session.messages + listOf(
                                    AssistantMessage(AssistantRole.USER, message, now),
                                    AssistantMessage(
                                        AssistantRole.ASSISTANT,
                                        "Saved locally. Connect an assistant provider to generate a response.",
                                        now,
                                    ),
                                ),
                                updatedAt = now,
                            ),
                        )
                        draft = ""
                    },
                ) {
                    Text("Send")
                }
            }
        }
    }

    if (renameDialog) {
        RenameSessionDialog(
            initialTitle = session.title,
            onDismiss = { renameDialog = false },
            onSave = { title ->
                onSessionUpdated(session.copy(title = title, updatedAt = kotlinx.datetime.Clock.System.now()))
                renameDialog = false
            },
        )
    }
    if (deleteDialog) {
        AlertDialog(
            onDismissRequest = { deleteDialog = false },
            title = { Text("Delete conversation?") },
            text = { Text("This removes “${session.title}” from local history.") },
            confirmButton = {
                Button(onClick = {
                    onSessionDeleted(session.id)
                    deleteDialog = false
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleteDialog = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun MessageBubble(message: AssistantMessage) {
    val isUser = message.role == AssistantRole.USER
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.82f),
            shape = RoundedCornerShape(14.dp),
            color = if (isUser) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHighest
            },
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    if (isUser) "You" else "PaperConnekt assistant",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(message.content)
                message.sentAt?.let {
                    Text(
                        formatMessageTime(it),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun PaperShelf(
    papers: List<Paper>,
    onOpenPaper: (Paper) -> Unit,
) {
    var pageIndex by remember(papers.map { it.id }) { mutableStateOf(0) }
    var pageCount by remember { mutableStateOf(1) }
    LaunchedEffect(papers.size) {
        pageIndex = pageIndex.coerceAtLeast(0)
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "Saved papers and PDFs",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                if (papers.isNotEmpty()) {
                    IconButton(
                        onClick = { pageIndex = (pageIndex - 1).coerceAtLeast(0) },
                        enabled = pageIndex > 0,
                    ) {
                        Icon(
                            painterResource(Res.drawable.filled_arrow_back),
                            contentDescription = "Previous saved paper",
                        )
                    }
                    Text(
                        "Page ${pageIndex + 1}/$pageCount",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    IconButton(
                        onClick = {
                            pageIndex = (pageIndex + 1).coerceAtMost(pageCount - 1)
                        },
                        enabled = pageIndex < pageCount - 1,
                    ) {
                        Icon(
                            painterResource(Res.drawable.filled_arrow_forward),
                            contentDescription = "Next saved paper",
                        )
                    }
                }
            }
            if (papers.isEmpty()) {
                Text(
                    "No saved papers yet. Add one from the dashboard to make it available here.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                if (papers.none { it.pdfPath != null }) {
                    Text(
                        "Your saved papers do not have PDFs yet. Open an item's details in Library to browse or drop a PDF.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val cardsPerPage = when {
                        maxWidth >= 980.dp -> 3
                        maxWidth >= 640.dp -> 2
                        else -> 1
                    }
                    val calculatedPageCount = (papers.size + cardsPerPage - 1) / cardsPerPage
                    LaunchedEffect(cardsPerPage, calculatedPageCount) {
                        pageCount = calculatedPageCount.coerceAtLeast(1)
                        pageIndex = pageIndex.coerceIn(0, calculatedPageCount - 1)
                    }
                    val cardWidth = ((maxWidth - 12.dp * (cardsPerPage - 1)) / cardsPerPage)
                        .coerceAtLeast(220.dp)
                    AnimatedContent(
                        targetState = pageIndex,
                        transitionSpec = {
                            (slideInHorizontally { it / 3 } + fadeIn()) togetherWith
                                (slideOutHorizontally { -it / 3 } + fadeOut())
                        },
                        label = "saved papers carousel",
                    ) { page ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            papers.drop(page * cardsPerPage).take(cardsPerPage).forEach { paper ->
                                PaperShelfCard(
                                    paper = paper,
                                    onOpen = { onOpenPaper(paper) },
                                    modifier = Modifier.width(cardWidth),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentResearchShelf(
    localPapers: List<Paper>,
    history: com.akram.bensalem.paper.connekt.feature.assistant.ResearchHistory,
    onOpenPaper: (Paper) -> Unit,
    onOpenZotero: (ZoteroPdfTarget) -> Unit,
) {
    val recentLocal = localPapers
        .filter { it.lastOpenedAt > 0L && !it.archived }
        .sortedByDescending { it.lastOpenedAt }
    val recentZotero = history.papers
        .sortedByDescending { it.lastOpenedAt }
    if (recentLocal.isEmpty() && recentZotero.isEmpty()) return
    val recentPapers = buildList {
        recentLocal.take(4).forEach { paper ->
            add(
                RecentPaperEntry(
                    key = "local-${paper.id}",
                    title = paper.title,
                    subtitle = paper.authors.take(2).joinToString().ifBlank { "Local library" },
                    source = "Local",
                    enabled = paper.pdfPath != null,
                    onOpen = { onOpenPaper(paper) },
                )
            )
        }
        recentZotero.take(6).forEach { paper ->
            add(
                RecentPaperEntry(
                    key = "zotero-${paper.id}",
                    title = paper.title,
                    subtitle = paper.authors.ifBlank { "Zotero library" },
                    source = "Zotero",
                    enabled = true,
                    onOpen = { onOpenZotero(paper.target) },
                )
            )
        }
    }
    var pageIndex by remember(recentPapers.map { it.key }) { mutableStateOf(0) }
    var pageCount by remember { mutableStateOf(1) }
    LaunchedEffect(recentPapers.size) {
        pageIndex = pageIndex.coerceAtLeast(0)
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Recently visited", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Your latest reading activity from local PDFs and Zotero.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(
                    onClick = { pageIndex = (pageIndex - 1).coerceAtLeast(0) },
                    enabled = pageIndex > 0,
                ) {
                    Icon(
                        painterResource(Res.drawable.filled_arrow_back),
                        contentDescription = "Previous recently visited paper",
                    )
                }
                Text(
                    "Page ${pageIndex + 1}/$pageCount",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                IconButton(
                    onClick = {
                        pageIndex = (pageIndex + 1).coerceAtMost(pageCount - 1)
                    },
                    enabled = pageIndex < pageCount - 1,
                ) {
                    Icon(
                        painterResource(Res.drawable.filled_arrow_forward),
                        contentDescription = "Next recently visited paper",
                    )
                }
            }
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val cardsPerPage = when {
                    maxWidth >= 980.dp -> 3
                    maxWidth >= 640.dp -> 2
                    else -> 1
                }
                val calculatedPageCount = (recentPapers.size + cardsPerPage - 1) / cardsPerPage
                LaunchedEffect(cardsPerPage, calculatedPageCount) {
                    pageCount = calculatedPageCount.coerceAtLeast(1)
                    pageIndex = pageIndex.coerceIn(0, calculatedPageCount - 1)
                }
                val cardWidth = ((maxWidth - 12.dp * (cardsPerPage - 1)) / cardsPerPage)
                    .coerceAtLeast(220.dp)
                AnimatedContent(
                    targetState = pageIndex,
                    transitionSpec = {
                        (slideInHorizontally { it / 3 } + fadeIn()) togetherWith
                            (slideOutHorizontally { -it / 3 } + fadeOut())
                    },
                    label = "recent papers carousel",
                ) { page ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        recentPapers.drop(page * cardsPerPage).take(cardsPerPage).forEach { paper ->
                            RecentPaperCard(
                                title = paper.title,
                                subtitle = paper.subtitle,
                                source = paper.source,
                                enabled = paper.enabled,
                                onOpen = paper.onOpen,
                                modifier = Modifier.width(cardWidth),
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class RecentPaperEntry(
    val key: String,
    val title: String,
    val subtitle: String,
    val source: String,
    val enabled: Boolean,
    val onOpen: () -> Unit,
)

@Composable
private fun RecentPaperCard(
    title: String,
    subtitle: String,
    source: String,
    enabled: Boolean,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.height(190.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            Modifier.fillMaxSize().padding(13.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(source.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.weight(1f))
            OutlinedButton(
                onClick = onOpen,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (enabled) "Resume reading" else "PDF unavailable")
            }
        }
    }
}

@Composable
private fun PaperShelfCard(
    paper: Paper,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.height(190.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Text(
                "SAVED PAPER",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                paper.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                paper.authors.take(2).joinToString().ifBlank { "Author details not added" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                if (paper.pdfPath != null) "PDF available" else "Metadata only",
                style = MaterialTheme.typography.labelSmall,
                color = if (paper.pdfPath != null) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            Spacer(Modifier.weight(1f))
            OutlinedButton(
                onClick = onOpen,
                enabled = paper.pdfPath != null,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (paper.pdfPath != null) "Open PDF" else "No PDF")
            }
        }
    }
}

@Composable
private fun EmptyHistory() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("No conversations yet", style = MaterialTheme.typography.titleSmall)
        Text(
            "Create one to keep a local trail of your research questions.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RenameSessionDialog(
    initialTitle: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var title by remember { mutableStateOf(initialTitle) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename conversation") },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Conversation name") },
                singleLine = true,
            )
        },
        confirmButton = {
            Button(
                enabled = title.isNotBlank(),
                onClick = { onSave(title.trim()) },
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun newAssistantSession(): AssistantSession = AssistantSession(
    id = "assistant-${Random.nextLong()}",
    title = "New research conversation",
    updatedAt = kotlinx.datetime.Clock.System.now(),
)

private fun formatMessageTime(instant: Instant): String =
    instant.toString().replace('T', ' ').substringBefore('.').removeSuffix("Z")
