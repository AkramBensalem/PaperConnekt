package com.paperconnekt.idea

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.akram.bensalem.paper.connekt.core.aiassistant.AgentRole
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akram.bensalem.paper.connekt.core.aiassistant.ResearchAgentModels
import com.akram.bensalem.paper.connekt.feature.assistant.*
import com.akram.bensalem.paper.connekt.feature.assistant.component.ChatMarkdown
import com.mikepenz.markdown.coil3.Coil3ImageTransformerImpl
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextArea
import org.jetbrains.jewel.ui.component.TextField

private enum class AgentView { Chat, Conversations }

private val agentSuggestions = listOf(
    "Summarize this paper" to NativeIcon.Note,
    "What are the key contributions?" to NativeIcon.Assistant,
    "Highlight the main findings" to NativeIcon.Highlight,
    "What are the limitations?" to NativeIcon.Search,
)

/** Floating, non-modal AI agent panel shown over the reader, so the paper stays visible while chatting. */
@Composable
internal fun NativeAssistantPanel(features: NativeFeatures, onClose: () -> Unit) {
    val agent = features.agent
    val reader = features.reader
    val question = rememberTextFieldState()
    var view by remember { mutableStateOf(AgentView.Chat) }
    LaunchedEffect(agent, reader.paperStorageKey()) { agent.load() }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (view == AgentView.Conversations) NativeToolButton("Back to chat", NativeIcon.Back) { view = AgentView.Chat }
            else NativeAgentAvatar(32.dp, agent.running)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(if (view == AgentView.Chat) "AI agent" else "Conversations", fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                NativeMeta(when {
                    view == AgentView.Conversations -> "${agent.conversations.size} saved for this paper"
                    agent.running -> if (agent.compacting) "Compacting earlier messages..." else "Thinking..."
                    else -> reader.state.target?.item?.data?.title ?: "Research assistant"
                })
            }
            NativeToolButton("New conversation", NativeIcon.Add, enabled = !agent.running && (reader.state.target != null || reader.state.localStorageKey != null)) {
                agent.newChat(); view = AgentView.Chat
            }
            NativeToolButton("Conversation history", NativeIcon.History, enabled = (reader.state.target != null || reader.state.localStorageKey != null),
                selected = view == AgentView.Conversations) {
                view = if (view == AgentView.Conversations) AgentView.Chat else AgentView.Conversations
            }
            NativeToolButton("Close AI agent (Escape)", NativeIcon.Close, onClick = onClose)
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(nativeForeground.copy(alpha = .12f)))
        if (reader.state.target == null && reader.state.localStorageKey == null) {
            NativeEmptyState("Open a PDF to chat with the AI agent about it.", Modifier.weight(1f).fillMaxWidth())
            return@Column
        }
        if (view == AgentView.Conversations) {
            if (agent.conversations.isEmpty()) {
                NativeEmptyState("No saved conversation for this paper yet.", Modifier.weight(1f).fillMaxWidth()) {
                    OutlinedButton(onClick = { view = AgentView.Chat }) { Text("Start chatting") }
                }
                return@Column
            }
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(agent.conversations, key = { it.id }) { conversation ->
                    val open: (() -> Unit)? = if (agent.running) null else {
                        { agent.openConversation(conversation.id); view = AgentView.Chat }
                    }
                    NativeCard(selected = conversation.id == agent.conversation.id, onClick = open) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            NativeGlyph(NativeIcon.Chat, tint = nativeAccent)
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                NativeCardTitle(conversation.title, maxLines = 1)
                                NativeMeta("${conversation.messages.size} messages" +
                                    if (conversation.summary != null) " · compacted" else "")
                            }
                            NativeToolButton("Delete conversation", NativeIcon.Trash, enabled = !agent.running, compact = true) {
                                if (confirmNative("Delete this paper conversation?")) agent.deleteConversation(conversation.id)
                            }
                        }
                    }
                }
            }
            return@Column
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            NativeMenu(agent.selectedModel.label, ResearchAgentModels.all.map { model ->
                NativeMenuAction(model.label, NativeIcon.Assistant, enabled = !agent.running, selected = model == agent.selectedModel) {
                    agent.selectModel(model)
                }
            }, icon = NativeIcon.Assistant, showLabel = true)
            val contextLabel = when {
                agent.includePageContext && agent.includePaperContext -> "Page + paper"
                agent.includePageContext -> "Current page"
                agent.includePaperContext -> "Whole paper"
                else -> "No context"
            }
            NativeMenu(contextLabel, listOf(
                NativeMenuAction("Include current page", NativeIcon.File, selected = agent.includePageContext) {
                    agent.includePageContext = !agent.includePageContext
                },
                NativeMenuAction("Include whole paper", NativeIcon.Book, selected = agent.includePaperContext) {
                    agent.includePaperContext = !agent.includePaperContext
                },
            ), icon = NativeIcon.Context, showLabel = true)
            Spacer(Modifier.weight(1f))
            NativeMenu("More chat actions", listOf(
                NativeMenuAction("Compact context", NativeIcon.Filter, enabled = agent.canCompact, action = agent::compact),
                NativeMenuAction("Conversation history", NativeIcon.History) { view = AgentView.Conversations },
            ))
        }
        val send: (String) -> Unit = { text ->
            if (!agent.running && text.isNotBlank()) {
                agent.send(text)
                question.setTextAndPlaceCursorAtEnd("")
                if (agent.showHistory) { view = AgentView.Conversations; agent.showHistory = false }
            }
        }
        val list = androidx.compose.foundation.lazy.rememberLazyListState()
        LaunchedEffect(agent.messages.size) { if (agent.messages.isNotEmpty()) list.animateScrollToItem(agent.messages.lastIndex) }
        if (agent.messages.isEmpty()) {
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically)) {
                NativeAgentAvatar(48.dp, false)
                Text("How can I help with this paper?", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                NativeMeta("Ask questions, or let the agent add highlights and notes for you.", maxLines = 2)
                Spacer(Modifier.height(4.dp))
                agentSuggestions.forEach { (text, icon) -> NativeSuggestion(text, icon, enabled = !agent.running) { send(text) } }
            }
        } else {
            LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = list, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                items(agent.messages, key = { it.id }) { message -> NativeChatBubble(message, onAction = agent::open) }
                if (agent.running) item(key = "typing") { NativeTypingRow(agent.liveActions.map { it.label }, agent::open, agent.liveActions) }
            }
        }
        agent.pinned?.let { pinned ->
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(nativeAccent.copy(alpha = .08f))
                .padding(start = 0.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(3.dp).height(40.dp).background(nativeAccent))
                Column(Modifier.weight(1f).padding(horizontal = 10.dp, vertical = 6.dp)) {
                    NativeMeta("Pinned passage · page ${pinned.pageIndex + 1}")
                    Text(pinned.text, maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 12.sp)
                }
                NativeToolButton("Unpin passage", NativeIcon.Close, compact = true, onClick = agent::unpin)
            }
        }
        val composerShape = RoundedCornerShape(10.dp)
        Row(Modifier.fillMaxWidth().clip(composerShape).background(nativeForeground.copy(alpha = .04f))
            .border(1.dp, nativeForeground.copy(alpha = .18f), composerShape).padding(6.dp),
            verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TextArea(question, Modifier.weight(1f).height(64.dp).onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.Enter && (event.isCtrlPressed || event.isMetaPressed)) {
                    send(question.text.toString()); true
                } else false
            }, placeholder = { Text("Ask about this paper...") })
            if (agent.running) NativeRoundAction("Stop generating", NativeIcon.Stop, Color(0xFFE5534B), onClick = agent::stop)
            else NativeRoundAction("Send (Ctrl+Enter)", NativeIcon.Send, nativeAccent, enabled = question.text.isNotBlank()) {
                send(question.text.toString())
            }
        }
        NativeMeta("Ctrl+Enter to send · context is shared with ${agent.selectedModel.label}", Modifier.align(Alignment.CenterHorizontally))
    }
}

@Composable
private fun NativeAgentAvatar(size: Dp, running: Boolean) {
    Box(Modifier.size(size).clip(RoundedCornerShape(50)).background(nativeAccent.copy(alpha = .16f))
        .border(1.dp, nativeAccent.copy(alpha = if (running) .9f else .4f), RoundedCornerShape(50)), contentAlignment = Alignment.Center) {
        NativeGlyph(NativeIcon.Assistant, Modifier.size(size * .55f), tint = nativeAccent)
    }
}

@Composable
private fun NativeSuggestion(text: String, icon: NativeIcon, enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Row(Modifier.widthIn(max = 340.dp).fillMaxWidth().clip(shape)
        .background(if (hovered && enabled) nativeAccent.copy(alpha = .12f) else nativeForeground.copy(alpha = .04f))
        .border(1.dp, if (hovered && enabled) nativeAccent.copy(alpha = .5f) else nativeForeground.copy(alpha = .14f), shape)
        .hoverable(interaction).clickable(enabled = enabled, role = Role.Button, onClick = onClick)
        .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        NativeGlyph(icon, Modifier.size(16.dp), tint = nativeAccent)
        Text(text, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        NativeGlyph(NativeIcon.Next, Modifier.size(14.dp), tint = nativeForeground.copy(alpha = .5f))
    }
}

@Composable
private fun NativeChatBubble(message: PaperAgentMessage, onAction: (PaperAgentAction) -> Unit) {
    val user = message.role == AgentRole.USER
    val error = Color(0xFFE5534B)
    val shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = if (user) 12.dp else 3.dp, bottomEnd = if (user) 3.dp else 12.dp)
    Column(Modifier.fillMaxWidth(), horizontalAlignment = if (user) Alignment.End else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(5.dp)) {
        if (!user) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            NativeAgentAvatar(20.dp, false)
            Text(if (message.isError) "Something went wrong" else "Assistant", fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                color = if (message.isError) error else nativeForeground.copy(alpha = .75f))
        }
        SelectionContainer {
            Column(
                (if (user) Modifier.widthIn(max = 360.dp) else Modifier.fillMaxWidth()).clip(shape)
                    .background(when {
                        message.isError -> error.copy(alpha = .1f)
                        user -> nativeAccent.copy(alpha = .16f)
                        else -> nativeForeground.copy(alpha = .045f)
                    })
                    .border(1.dp, when {
                        message.isError -> error.copy(alpha = .45f)
                        user -> nativeAccent.copy(alpha = .35f)
                        else -> nativeForeground.copy(alpha = .1f)
                    }, shape)
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                message.quote?.let { quote ->
                    Row(Modifier.height(IntrinsicSize.Min)) {
                        Box(Modifier.width(3.dp).fillMaxHeight().background(nativeAccent.copy(alpha = .7f)))
                        Text(quote, Modifier.padding(start = 8.dp), fontSize = 12.sp, maxLines = 4, overflow = TextOverflow.Ellipsis,
                            color = nativeForeground.copy(alpha = .7f))
                    }
                }
                if (user) Text(message.text) else ChatMarkdown(message.text, imageTransformer = Coil3ImageTransformerImpl)
            }
        }
        if (message.actions.isNotEmpty()) NativeActions {
            message.actions.forEach { action ->
                NativeActionChip("p${action.pageIndex + 1} · ${action.label}") { onAction(action) }
            }
        }
    }
}

@Composable
private fun NativeTypingRow(labels: List<String>, onAction: (PaperAgentAction) -> Unit, actions: List<PaperAgentAction>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            NativeAgentAvatar(20.dp, true)
            NativeMeta(if (labels.isEmpty()) "Thinking..." else "Working on the paper...")
        }
        if (actions.isNotEmpty()) NativeActions {
            actions.forEach { action -> NativeActionChip(action.label) { onAction(action) } }
        }
    }
}

@Composable
private fun NativeActionChip(text: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Row(Modifier.clip(shape).background(nativeAccent.copy(alpha = .1f)).border(1.dp, nativeAccent.copy(alpha = .4f), shape)
        .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        NativeGlyph(NativeIcon.Locate, Modifier.size(13.dp), tint = nativeAccent)
        Text(text, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 260.dp))
    }
}

@Composable
private fun NativeRoundAction(label: String, icon: NativeIcon, color: Color, enabled: Boolean = true, onClick: () -> Unit) {
    NativeTooltip(label) {
        Box(Modifier.size(34.dp).clip(RoundedCornerShape(50)).background(color.copy(alpha = if (enabled) 1f else .3f))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick).semantics { contentDescription = label },
            contentAlignment = Alignment.Center) {
            NativeGlyph(icon, Modifier.size(16.dp), tint = Color.White)
        }
    }
}

