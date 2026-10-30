package com.akram.bensalem.paper.connekt.feature.assistant.component

import com.paperconnekt.app.resources.Res
import com.paperconnekt.app.resources.filled_add_comment
import com.paperconnekt.app.resources.filled_article
import com.paperconnekt.app.resources.filled_bookmark
import com.paperconnekt.app.resources.filled_chat_bubble_outline
import com.paperconnekt.app.resources.filled_content_copy
import com.paperconnekt.app.resources.filled_delete_outline
import com.paperconnekt.app.resources.filled_description
import com.paperconnekt.app.resources.filled_error_outline
import com.paperconnekt.app.resources.filled_format_quote
import com.paperconnekt.app.resources.filled_history
import com.paperconnekt.app.resources.filled_hourglass_top
import com.paperconnekt.app.resources.filled_key
import com.paperconnekt.app.resources.filled_more_vert
import com.paperconnekt.app.resources.filled_send
import com.paperconnekt.app.resources.filled_stop
import com.paperconnekt.app.resources.filled_text_fields
import org.jetbrains.compose.resources.painterResource
import androidx.compose.foundation.background
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.imePadding
import com.akram.bensalem.bistosh.core.designsystem.component.clipChipInteraction
import com.akram.bensalem.bistosh.core.designsystem.component.interactivePointer
import com.akram.bensalem.paper.connekt.core.aiassistant.AgentRole
import com.akram.bensalem.paper.connekt.core.aiassistant.ResearchAgentModels
import com.akram.bensalem.paper.connekt.feature.assistant.*
import com.paperconnekt.app.resources.filled_auto_awesome
import com.paperconnekt.app.resources.filled_compress
import com.paperconnekt.app.resources.filled_expand_less
import com.paperconnekt.app.resources.filled_expand_more
import com.paperconnekt.app.resources.filled_tune
import com.paperconnekt.app.resources.filled_check
import com.paperconnekt.app.resources.filled_close
import kotlinx.coroutines.launch



private val agentSuggestions = listOf(
    "Summarize this paper",
    "Explain the current page",
    "Highlight the key contributions",
    "Highlight the key terms on this page",
    "Add notes on the limitations",
    "Suggest research ideas from this paper"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PaperAgentPanel(agent: PaperAgentViewModel, workspace: PdfWorkspaceViewModel, modifier: Modifier = Modifier,
    headerCollapsed: Boolean = false, onOpenIdea: (String) -> Unit = {}) {
    val state = workspace.state
    val conversation = agent.conversation
    val openAction: (PaperAgentAction) -> Unit = { action ->
        action.ideaId?.let(onOpenIdea) ?: agent.open(action)
    }
    var showEarlier by remember(conversation.id) { mutableStateOf(false) }
    var previewInput by remember { mutableStateOf(false) }
    var lastReaderPage by remember { mutableStateOf(state.pageIndex) }
    var contextInitialized by remember { mutableStateOf(false) }
    val hiddenEarlier = if (showEarlier) 0 else conversation.compactedCount.coerceAtMost(conversation.messages.size)
    val itemCount = agent.messages.size - hiddenEarlier + (if (conversation.compactedCount > 0) 1 else 0) + if (agent.running) 1 else 0
    val storageKey = workspace.paperStorageKey()
    LaunchedEffect(storageKey) { agent.load() }
    val listState = remember(conversation.id, agent.showHistory) { LazyListState((itemCount - 1).coerceAtLeast(0)) }
    LaunchedEffect(itemCount, agent.liveActions.size) { if (itemCount > 0 && !agent.showHistory) listState.animateScrollToItem(itemCount - 1) }
    LaunchedEffect(state.pageIndex, state.readerPageCount) {
        if (!contextInitialized || state.pageIndex != lastReaderPage) {
            agent.pageContextStart = state.pageIndex
            agent.pageContextEnd = state.pageIndex
            lastReaderPage = state.pageIndex
            contextInitialized = true
        }
    }

    Column(modifier.fillMaxSize().imePadding().testTag("paper-agent-panel")) {
        agent.storageError?.let { error ->
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = agent::retryStorage) { Text("Retry chat storage") }
        }
        if (!agent.providerKeyAvailable) {
            val scope = rememberCoroutineScope()
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        agent.providerKeyMessage,
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    TextButton(onClick = { scope.launch { agent.refreshProviderKey() } }) {
                        Text("Recheck")
                    }
                }
            }
        }
        if (!agent.storageReady && agent.storageError == null) {
            Text("Loading saved chats...", style = MaterialTheme.typography.bodySmall)
        }
        AnimatedVisibility(visible = !headerCollapsed) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(painterResource(Res.drawable.filled_auto_awesome), null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                Text(if (agent.showHistory) "Chat history" else if (agent.messages.isEmpty()) "Research agent" else conversation.title,
                    style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                var modelMenu by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(
                        onClick = { if (!agent.running) modelMenu = true },
                        enabled = !agent.running,
                        contentPadding = PaddingValues(horizontal = 9.dp, vertical = 5.dp),
                        modifier = Modifier.height(34.dp).interactivePointer(enabled = !agent.running),
                    ) {
                        Icon(painterResource(Res.drawable.filled_tune), null, Modifier.size(15.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(agent.selectedModel.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Icon(painterResource(Res.drawable.filled_expand_more), null, Modifier.size(17.dp))
                    }
                    DropdownMenu(expanded = modelMenu, onDismissRequest = { modelMenu = false }) {
                        ResearchAgentModels.all.forEach { model ->
                            DropdownMenuItem(
                                text = { Text(model.label) },
                                leadingIcon = {
                                    if (model == agent.selectedModel) Icon(painterResource(Res.drawable.filled_check), null)
                                },
                                onClick = { modelMenu = false; agent.selectModel(model) },
                                modifier = Modifier.interactivePointer(),
                            )
                        }
                    }
                }
                var menu by remember { mutableStateOf(false) }
                Surface(
                    shape = RoundedCornerShape(11.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(11.dp)),
                ) {
                    Row {
                        HeaderButton(painterResource(Res.drawable.filled_add_comment), "New chat",
                            enabled = !agent.running && agent.messages.isNotEmpty(), onClick = agent::newChat)
                        HeaderButton(painterResource(Res.drawable.filled_history), if (agent.showHistory) "Back to chat" else "Chat history",
                            enabled = !agent.running, selected = agent.showHistory,
                            onClick = { agent.run(PaperAgentCommand.History) })
                        HeaderButton(painterResource(Res.drawable.filled_more_vert), "More chat actions",
                            enabled = !agent.running && agent.messages.isNotEmpty()) { menu = true }
                    }
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Compact conversation") }, enabled = agent.canCompact,
                        leadingIcon = { Icon(painterResource(Res.drawable.filled_compress), null, Modifier.size(18.dp)) },
                        onClick = { menu = false; agent.compact() }, modifier = Modifier.interactivePointer(enabled = agent.canCompact))
                    DropdownMenuItem(text = { Text("Delete conversation") },
                        leadingIcon = { Icon(painterResource(Res.drawable.filled_delete_outline), null, Modifier.size(18.dp)) },
                        onClick = { menu = false; agent.deleteConversation(conversation.id) }, modifier = Modifier.interactivePointer())
                }
            }
        }
        if (agent.showHistory) {
            ChatHistory(agent, Modifier.weight(1f))
        } else Column(Modifier.weight(1f).fillMaxWidth()) {
            LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = listState,
                verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
                if (agent.messages.isEmpty() && !agent.running) item {
                    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        Text("Ask about the paper, annotate it, or create and fill Markdown notes in the Idea Board. I read the paper's Markdown version and see your current page and selection.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Type / for commands such as /new, /history or /compact.",
                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            modifier = Modifier.padding(top = 4.dp))
                        Spacer(Modifier.height(12.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            agentSuggestions.forEach { suggestion ->
                                SuggestionChip(onClick = { agent.send(suggestion) }, enabled = agent.storageReady && agent.providerKeyAvailable, label = { Text(suggestion) },
                                    modifier = Modifier.clipChipInteraction().interactivePointer())
                            }
                        }
                    }
                }
                if (conversation.compactedCount > 0) {
                    val earlier = conversation.messages.take(conversation.compactedCount)
                    if (showEarlier) items(earlier, key = { it.id }) { message ->
                        Box(Modifier.alpha(0.6f)) { AgentMessage(message, onAction = openAction) }
                    }
                    item(key = "compacted-${conversation.compactedCount}") {
                        CompactedDivider(conversation.summary.orEmpty(), earlier.size, showEarlier) { showEarlier = !showEarlier }
                    }
                }
                items(conversation.messages.drop(conversation.compactedCount), key = { it.id }) { message -> AgentMessage(message, onAction = openAction) }
                if (agent.running) item(key = "running") {
                    AgentBubble {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text(when {
                                agent.compacting -> "Compacting the conversation…"
                                state.markdown.converting -> "Creating the Markdown version of the PDF…"
                                agent.liveActions.isNotEmpty() -> "Updating the workspace…"
                                else -> "Thinking…"
                            }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        ActionChips(agent.liveActions, openAction)
                    }
                }
            }

            agent.pinned?.let { pinned ->
                Row(Modifier.fillMaxWidth().padding(bottom = 6.dp).clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer).padding(start = 10.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(Res.drawable.filled_format_quote), null, Modifier.size(16.dp))
                    Text("p. ${pinned.pageIndex + 1} · ${pinned.text}", Modifier.weight(1f).padding(horizontal = 6.dp),
                        style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    IconButton(onClick = agent::unpin, modifier = Modifier.size(32.dp).interactivePointer()) {
                        Icon(painterResource(Res.drawable.filled_close), "Remove passage", Modifier.size(16.dp))
                    }
                }
            }
            val commands = PaperAgentCommand.matching(agent.input)
            if (commands.isNotEmpty()) CommandSuggestions(commands) { agent.input = ""; agent.run(it) }
            AgentComposer(
                agent = agent,
                state = state,
                preview = previewInput,
                onPreviewChange = { previewInput = it },
            )
        }
    }
}

@Composable
private fun AgentComposer(
    agent: PaperAgentViewModel,
    state: PdfWorkspaceState,
    preview: Boolean,
    onPreviewChange: (Boolean) -> Unit,
) {
    val pageCount = state.readerPageCount.coerceAtLeast(1)
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val compact = maxWidth < 520.dp
        val constrained = compact || maxHeight < 620.dp
        var composerExpanded by remember { mutableStateOf(false) }
        val send = {
            agent.send()
            if (constrained) composerExpanded = false
        }
        if (constrained && !composerExpanded && agent.input.isBlank() && !preview) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                onClick = { composerExpanded = true },
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painterResource(Res.drawable.filled_chat_bubble_outline),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        "Ask about this paper…",
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                    )
                    Icon(
                        painterResource(Res.drawable.filled_expand_more),
                        contentDescription = "Expand message composer",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
        val editorMinHeight = when {
            compact -> 76.dp
            maxHeight < 420.dp -> 96.dp
            maxWidth < 400.dp -> 116.dp
            else -> 150.dp
        }
        val editorMaxHeight = when {
            compact -> 132.dp
            maxHeight < 420.dp -> 170.dp
            maxWidth < 400.dp -> 210.dp
            else -> 260.dp
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!compact) {
                Text("Context", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
            }
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ContextChip(painterResource(Res.drawable.filled_description), "Page ${state.pageIndex + 1}/$pageCount")
                Spacer(Modifier.weight(1f))
                PageRangeSelector(agent, pageCount, state.pageIndex)
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                val markdown = state.markdown
                when {
                    markdown.content != null -> ContextChip(painterResource(Res.drawable.filled_article), "Markdown ready")
                    markdown.busy -> ContextChip(painterResource(Res.drawable.filled_hourglass_top), if (markdown.converting) "Creating Markdown…" else "Loading Markdown…")
                    markdown.missingApiKey -> ContextChip(painterResource(Res.drawable.filled_key), "Mistral key needed", error = true)
                    markdown.error != null -> ContextChip(painterResource(Res.drawable.filled_error_outline), "Markdown unavailable", error = true)
                    else -> ContextChip(painterResource(Res.drawable.filled_article), "Markdown on first question")
                }
                state.textSelection?.text?.takeIf { it.isNotBlank() && agent.pinned == null }?.let {
                    ContextChip(painterResource(Res.drawable.filled_text_fields), "Selection: “${it.take(28)}${if (it.length > 28) "…" else ""}”")
                }
                state.annotations.find { it.key == state.selectedKey }?.let {
                    ContextChip(painterResource(Res.drawable.filled_bookmark), "Selected ${if (it.isIdea) "idea" else it.type}")
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!compact) {
                    Text("Message", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                } else {
                    Spacer(Modifier.weight(1f))
                }
                TextButton(
                    onClick = { onPreviewChange(!preview) },
                    modifier = Modifier.height(32.dp).interactivePointer(),
                ) {
                    Text(if (preview) "Edit Markdown" else "Preview Markdown")
                }
            }
            if (preview) {
                Surface(
                    modifier = Modifier.fillMaxWidth().heightIn(min = editorMinHeight, max = editorMaxHeight),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    if (agent.input.isBlank()) {
                        Text(
                            "Your Markdown preview will appear here.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(14.dp),
                        )
                    } else {
                        ChatMarkdown(
                            agent.input,
                            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(14.dp),
                        )
                    }
                }
            } else {
                OutlinedTextField(
                    value = agent.input,
                    onValueChange = { agent.input = it },
                    enabled = agent.providerKeyAvailable,
                    modifier = Modifier.fillMaxWidth().heightIn(min = editorMinHeight, max = editorMaxHeight)
                        .testTag("paper-agent-input")
                        .onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown && event.key == Key.Enter &&
                                (event.isCtrlPressed || event.isMetaPressed)
                            ) {
                                send()
                                true
                            } else false
                        },
                    placeholder = { Text("Write your question in Markdown…") },
                    shape = RoundedCornerShape(14.dp),
                    maxLines = 12,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!compact) {
                    Text(
                        "Markdown supported · Ctrl/⌘ + Enter to send",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }
                if (agent.running) {
                    FilledTonalButton(onClick = agent::stop, modifier = Modifier.interactivePointer()) {
                        Icon(painterResource(Res.drawable.filled_stop), "Stop", Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Stop")
                    }
                } else {
                    Button(
                        onClick = send,
                        enabled = agent.input.isNotBlank() && agent.storageReady && agent.providerKeyAvailable,
                        modifier = Modifier.interactivePointer(
                            enabled = agent.input.isNotBlank() && agent.storageReady && agent.providerKeyAvailable
                        ).testTag("paper-agent-send"),
                    ) {
                        Icon(painterResource(Res.drawable.filled_send), "Send", Modifier.size(18.dp))
                        if (!compact) {
                            Spacer(Modifier.width(6.dp))
                            Text("Send")
                        }
                        }
                    }
                }
            }
            }
        }
    }
}

@Composable
internal fun PageRangeSelector(
    agent: PaperAgentViewModel,
    pageCount: Int,
    currentPage: Int,
    targetLabel: String = "next question",
) {
    var menu by remember { mutableStateOf(false) }
    var startText by remember { mutableStateOf("") }
    var endText by remember { mutableStateOf("") }
    var draftEnabled by remember { mutableStateOf(true) }
    var draftAllPaper by remember { mutableStateOf(false) }
    val start = agent.pageContextStart.coerceIn(0, pageCount - 1)
    val end = agent.pageContextEnd.coerceIn(start, pageCount - 1)
    val contextEnabled = agent.includePageContext || agent.includePaperContext
    val allPaper = agent.includePaperContext && !agent.includePageContext
    Box {
        Surface(
            onClick = {
                startText = (start + 1).toString()
                endText = (end + 1).toString()
                menu = true
            },
            shape = RoundedCornerShape(9.dp),
            color = when {
                allPaper -> MaterialTheme.colorScheme.primaryContainer
                agent.includePageContext -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            },
            contentColor = if (contextEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (contextEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            ),
            modifier = Modifier.height(26.dp).interactivePointer(),
        ) {
            Row(
                Modifier.padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    painterResource(if (contextEnabled) Res.drawable.filled_description else Res.drawable.filled_close),
                    null,
                    Modifier.size(12.dp),
                )
                Text(
                    when {
                        allPaper -> "All paper context"
                        start == end && start == currentPage -> "Current page"
                        start == end -> "Page ${start + 1}"
                        else -> "Pages ${start + 1}–${end + 1}"
                    }.let { if (contextEnabled) it else "No context" },
                    style = MaterialTheme.typography.labelSmall,
                )
                if (contextEnabled) {
                    Icon(painterResource(Res.drawable.filled_check), "Context enabled", Modifier.size(13.dp))
                }
                Icon(painterResource(Res.drawable.filled_expand_more), null, Modifier.size(13.dp))
            }
        }
        DropdownMenu(
            expanded = menu,
            onDismissRequest = { menu = false },
            shape = RoundedCornerShape(18.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
        ) {
            Column(
                Modifier.width(292.dp).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                LaunchedEffect(menu) {
                    if (menu) {
                        draftEnabled = contextEnabled
                        draftAllPaper = allPaper
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Page context", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Choose the pages the agent should focus on.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { menu = false }, modifier = Modifier.size(32.dp).interactivePointer()) {
                        Icon(painterResource(Res.drawable.filled_close), "Close page context")
                    }
                }
                Text("Quick range", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    AssistChip(
                        onClick = {
                            startText = (currentPage.coerceIn(0, pageCount - 1) + 1).toString()
                            endText = startText
                            draftEnabled = true
                            draftAllPaper = false
                        },
                        label = { Text("Current") },
                        modifier = Modifier.height(30.dp).interactivePointer(),
                    )
                    AssistChip(
                        onClick = {
                            val current = agent.pageContextStart.coerceIn(0, pageCount - 1)
                            startText = (maxOf(0, current - 2) + 1).toString()
                            endText = (minOf(pageCount - 1, current + 2) + 1).toString()
                            draftEnabled = true
                            draftAllPaper = false
                        },
                        label = { Text("Nearby ±2") },
                        modifier = Modifier.height(30.dp).interactivePointer(),
                    )
                    AssistChip(
                        onClick = {
                            startText = "1"
                            endText = pageCount.toString()
                            draftEnabled = true
                            draftAllPaper = true
                        },
                        label = { Text("All paper") },
                        modifier = Modifier.height(30.dp).interactivePointer(),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startText,
                        onValueChange = {
                            startText = it.filter(Char::isDigit).take(5)
                            draftAllPaper = false
                        },
                        label = { Text("From page") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = endText,
                        onValueChange = {
                            endText = it.filter(Char::isDigit).take(5)
                            draftAllPaper = false
                        },
                        label = { Text("To page") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(
                        if (draftEnabled) MaterialTheme.colorScheme.surfaceContainerLow
                        else MaterialTheme.colorScheme.surfaceVariant
                    ).padding(start = 10.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            when {
                                !draftEnabled -> "Context disabled"
                                draftAllPaper -> "All paper context"
                                else -> "Use page context"
                            },
                            style = MaterialTheme.typography.labelMedium,
                        )
                        Text(
                            when {
                                !draftEnabled -> "Turn this on to include the saved selection"
                                draftAllPaper -> "The complete paper will be included"
                                else -> "Include this range in the $targetLabel"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = draftEnabled,
                        onCheckedChange = { draftEnabled = it },
                    )
                }
                if (!draftEnabled) {
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.errorContainer)
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(painterResource(Res.drawable.filled_close), null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "No page or paper context will be included with the $targetLabel.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { menu = false }, modifier = Modifier.interactivePointer()) { Text("Cancel") }
                    Button(
                        onClick = {
                            if (!draftEnabled) {
                                agent.includePageContext = false
                                agent.includePaperContext = false
                                menu = false
                            } else {
                                val from = startText.toIntOrNull()?.coerceIn(1, pageCount)
                                val to = endText.toIntOrNull()?.coerceIn(1, pageCount)
                                if (from != null && to != null) {
                                    val allPages = draftAllPaper || (from == 1 && to == pageCount)
                                    agent.pageContextStart = minOf(from, to) - 1
                                    agent.pageContextEnd = maxOf(from, to) - 1
                                    agent.includePageContext = !allPages
                                    agent.includePaperContext = allPages
                                    menu = false
                                }
                            }
                        },
                        enabled = !draftEnabled || (startText.toIntOrNull() != null && endText.toIntOrNull() != null),
                        modifier = Modifier.interactivePointer(
                            enabled = !draftEnabled || (startText.toIntOrNull() != null && endText.toIntOrNull() != null)
                        ),
                    ) { Text(if (draftEnabled) "Save range" else "Save") }
                }
            }
        }
    }
}

@Composable
private fun ContextChip(icon: androidx.compose.ui.graphics.painter.Painter, label: String, error: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    val color = if (error) colors.onErrorContainer else colors.onSurface
    val background = if (error) colors.errorContainer else colors.surfaceContainerHigh
    Row(Modifier.clip(RoundedCornerShape(8.dp)).background(background).border(1.dp, if (error) colors.error else colors.outline, RoundedCornerShape(8.dp))
        .padding(horizontal = 5.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(11.dp), tint = color)
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = color, maxLines = 1)
    }
}

private val userBubbleShape = RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
private val agentBubbleShape = RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp)

@Composable
private fun AgentBubble(
    modifier: Modifier = Modifier,
    container: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.padding(end = 24.dp).clip(agentBubbleShape).background(container).padding(horizontal = 12.dp, vertical = 10.dp),
        content = content)
}

@Composable
private fun AgentMessage(message: PaperAgentMessage, onAction: (PaperAgentAction) -> Unit) {
    val user = message.role == AgentRole.USER
    Column(Modifier.fillMaxWidth(), horizontalAlignment = if (user) Alignment.End else Alignment.Start) {
        if (user) {
            Column(Modifier.padding(start = 32.dp).clip(userBubbleShape)
                .background(MaterialTheme.colorScheme.primaryContainer).padding(horizontal = 12.dp, vertical = 10.dp)) {
                message.quote?.let {
                    Row(Modifier.height(IntrinsicSize.Min)) {
                        Box(Modifier.width(3.dp).fillMaxHeight().clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.35f)))
                        Spacer(Modifier.width(8.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic, maxLines = 3,
                            overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f))
                    }
                    Spacer(Modifier.height(6.dp))
                }
                SelectionContainer {
                    Text(message.text, color = MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            val clipboard = LocalClipboardManager.current
            if (message.isError) AgentBubble(Modifier.testTag("paper-agent-reply"), container = MaterialTheme.colorScheme.errorContainer) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(painterResource(Res.drawable.filled_error_outline), null, Modifier.size(16.dp).padding(top = 2.dp), tint = MaterialTheme.colorScheme.onErrorContainer)
                    Spacer(Modifier.width(8.dp))
                    Text(message.text, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall)
                }
            } else AgentBubble(Modifier.testTag("paper-agent-reply")) {
                SelectionContainer { ChatMarkdown(message.text, Modifier.fillMaxWidth()) }
                ActionChips(message.actions, onAction)
            }
            if (!message.isError && message.text.isNotBlank()) {
                IconButton(onClick = { clipboard.setText(AnnotatedString(message.text)) },
                    modifier = Modifier.padding(start = 2.dp).size(28.dp).interactivePointer()) {
                    Icon(painterResource(Res.drawable.filled_content_copy), "Copy reply", Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActionChips(actions: List<PaperAgentAction>, onAction: (PaperAgentAction) -> Unit) {
    if (actions.isEmpty()) return
    FlowRow(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        actions.take(30).forEach { action ->
            AssistChip(onClick = { onAction(action) }, modifier = Modifier.clipChipInteraction().interactivePointer(),
                label = { Text(if (action.ideaId != null) action.label else "p. ${action.pageIndex + 1} · ${action.label}",
                    maxLines = 1, overflow = TextOverflow.Ellipsis) },
                leadingIcon = { Icon(painterResource(Res.drawable.filled_auto_awesome), null, Modifier.size(14.dp)) })
        }
        if (actions.size > 30) Text("+${actions.size - 30} more", style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun HeaderButton(
    icon: androidx.compose.ui.graphics.painter.Painter, label: String, enabled: Boolean = true, selected: Boolean = false,
    onClick: () -> Unit,
) {
    TooltipIconButton(label) {
        IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(32.dp).interactivePointer(enabled = enabled),
            colors = if (selected) IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            else IconButtonDefaults.iconButtonColors()) {
            Icon(icon, label, Modifier.size(18.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TooltipIconButton(label: String, content: @Composable () -> Unit) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(label) } },
        state = rememberTooltipState(),
        content = content,
    )
}

@Composable
private fun CompactedDivider(summary: String, count: Int, expanded: Boolean, onToggle: () -> Unit) {
    var showSummary by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().testTag("paper-agent-compacted")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            HorizontalDivider(Modifier.weight(1f))
            Row(Modifier.padding(horizontal = 6.dp).clip(RoundedCornerShape(12.dp)).clickable(onClick = { showSummary = !showSummary })
                .interactivePointer().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(Res.drawable.filled_compress), null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(4.dp))
                Text("$count earlier message${if (count == 1) "" else "s"} summarized", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Icon(if (showSummary) painterResource(Res.drawable.filled_expand_less) else painterResource(Res.drawable.filled_expand_more), if (showSummary) "Hide summary" else "Show summary",
                    Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            HorizontalDivider(Modifier.weight(1f))
        }
        if (showSummary && summary.isNotBlank()) {
            Column(Modifier.fillMaxWidth().padding(top = 6.dp).clip(RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)).padding(10.dp)) {
                Text("Summary used as context", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(4.dp))
                SelectionContainer { ChatMarkdown(summary, Modifier.fillMaxWidth()) }
            }
        }
        TextButton(onClick = onToggle, modifier = Modifier.align(Alignment.CenterHorizontally).interactivePointer()) {
            Text(if (expanded) "Hide earlier messages" else "Show earlier messages", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun ChatHistory(agent: PaperAgentViewModel, modifier: Modifier = Modifier) {
    LazyColumn(modifier.fillMaxWidth().testTag("paper-agent-history"), verticalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(vertical = 8.dp)) {
        item(key = "new") {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = agent::newChat).interactivePointer()
                .padding(horizontal = 10.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(Res.drawable.filled_add_comment), null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Text("New chat", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
        if (agent.conversations.isEmpty()) item(key = "empty") {
            Text("No earlier chats about this paper yet.", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(10.dp))
        }
        items(agent.conversations, key = { it.id }) { chat ->
            val current = chat.id == agent.conversation.id
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .background(if (current) MaterialTheme.colorScheme.secondaryContainer else androidx.compose.ui.graphics.Color.Transparent)
                .clickable(onClick = { agent.openConversation(chat.id) }).interactivePointer()
                .padding(start = 10.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(Res.drawable.filled_chat_bubble_outline), null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(chat.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        fontWeight = if (current) FontWeight.SemiBold else FontWeight.Normal)
                    val count = chat.messages.size
                    Text(buildString {
                        append(relativeTime(chat.updatedAt)).append(" · ").append(count).append(if (count == 1) " message" else " messages")
                        if (chat.compactedCount > 0) append(" · compacted")
                    }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { agent.deleteConversation(chat.id) }, modifier = Modifier.size(32.dp).interactivePointer()) {
                    Icon(painterResource(Res.drawable.filled_delete_outline), "Delete chat ${chat.title}", Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun CommandSuggestions(commands: List<PaperAgentCommand>, onRun: (PaperAgentCommand) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(bottom = 6.dp).clip(RoundedCornerShape(12.dp))
        .background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(vertical = 4.dp).testTag("paper-agent-commands")) {
        commands.forEach { command ->
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                .clickable(onClick = { onRun(command) }).interactivePointer()
                .padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(command.command, style = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(76.dp))
                Text(command.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

private fun relativeTime(millis: Long): String {
    val minutes = (nowMillis() - millis).coerceAtLeast(0) / 60_000
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "${minutes} min ago"
        minutes < 24 * 60 -> "${minutes / 60} h ago"
        minutes < 7 * 24 * 60 -> "${minutes / (24 * 60)} d ago"
        else -> "${minutes / (7 * 24 * 60)} w ago"
    }
}
