package com.akram.bensalem.paper.connekt.feature.assistant.component

import com.paperconnekt.app.resources.Res
import com.paperconnekt.app.resources.filled_arrow_back
import com.paperconnekt.app.resources.filled_lightbulb
import com.paperconnekt.app.resources.filled_note
import com.paperconnekt.app.resources.filled_smart_toy
import org.jetbrains.compose.resources.painterResource
import com.akram.bensalem.bistosh.core.designsystem.component.interactivePointer

import androidx.compose.foundation.layout.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.akram.bensalem.paper.connekt.feature.assistant.ChatMessage
import com.akram.bensalem.paper.connekt.feature.assistant.Idea
import com.akram.bensalem.paper.connekt.feature.assistant.Note
import com.akram.bensalem.paper.connekt.feature.assistant.PdfWorkspaceViewModel
import com.akram.bensalem.paper.connekt.feature.assistant.PdfReadingPosition
import com.akram.bensalem.paper.connekt.feature.assistant.PaperAgentViewModel
import com.akram.bensalem.paper.connekt.feature.assistant.AssistantViewModel
import com.akram.bensalem.paper.connekt.feature.assistant.AiUsageEvent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.akram.bensalem.paper.connekt.models.Paper
import com.paperconnekt.app.resources.filled_article
import com.paperconnekt.secrets.SecretStore

@Composable
fun Workplace(
    paper: Paper,
    notes: List<Note>,
    chatMessages: List<ChatMessage>,
    ideas: List<Idea>,
    onAddNote: (String, Int?) -> Unit,
    onDeleteNote: (String) -> Unit,
    onSendMessage: (String) -> Unit,
    onAddIdea: (String, String) -> Unit,
    onUpdateIdea: (String, String, String, String) -> Unit,
    ideasLoading: Boolean = false,
    ideaError: String? = null,
    pdfWorkspace: PdfWorkspaceViewModel? = null,
    onExit: () -> Unit = {},
    onReadingPositionChanged: ((PdfReadingPosition, Float) -> Unit)? = null,
    onFocusModeChange: (Boolean) -> Unit = {},
    ideaBoard: AssistantViewModel? = null,
    usageRecorder: suspend (String?, AiUsageEvent) -> Unit = { _, _ -> },
    studyTimeToday: Long = 0L,
    totalStudyTime: Long = 0L,
    secretStore: SecretStore? = null,
    chatError: String? = null,
) {
    var selectedPanel by remember(paper.id) { mutableStateOf(0) }
    var focusMode by remember(paper.id) { mutableStateOf(false) }
    var ideaToOpen by remember(paper.id) { mutableStateOf<String?>(null) }
    val attachmentKey = pdfWorkspace?.paperStorageKey()
    val hasPdfAnnotations = pdfWorkspace != null &&
        (pdfWorkspace.state.target != null || pdfWorkspace.state.localTitle != null)
    val paperAgent = if (pdfWorkspace != null && attachmentKey != null)
        viewModel(key = "paper-agent-${paper.id}") {
            PaperAgentViewModel(
                pdfWorkspace,
                ideaBoard = ideaBoard,
                usageRecorder = usageRecorder,
                secretStore = secretStore,
            )
        } else null
    val panels = listOf(
        "Notes" to painterResource(Res.drawable.filled_note),
        "AI Agent" to painterResource(Res.drawable.filled_smart_toy),
        "Ideas" to painterResource(Res.drawable.filled_lightbulb)
    )
    var headerCollapsed by remember(paper.id, selectedPanel) { mutableStateOf(false) }
    val collapseThreshold = with(LocalDensity.current) { 32.dp.toPx() }
    val sidebarScroll = remember(paper.id, selectedPanel, collapseThreshold) {
        object : NestedScrollConnection {
            var distance = 0f

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput) return Offset.Zero
                val delta = consumed.y
                if (delta != 0f) {
                    if (distance * delta < 0f) distance = 0f
                    distance += delta
                    if (distance <= -collapseThreshold) {
                        headerCollapsed = true
                        distance = 0f
                    } else if (distance >= collapseThreshold) {
                        headerCollapsed = false
                        distance = 0f
                    }
                } else if (available.y > 0f) {
                    headerCollapsed = false
                    distance = 0f
                }
                return Offset.Zero
            }
        }
    }
    val supportingContent: @Composable (allowEditing: Boolean) -> Unit = { allowEditing ->
        Column(Modifier.fillMaxSize().nestedScroll(sidebarScroll)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        val detailPanels = listOf(
                            "Details" to painterResource(Res.drawable.filled_article),
                        ) + panels
                        detailPanels.forEachIndexed { index, (title, icon) ->
                            val selected = selectedPanel == index
                            val label = if (index == 1 && hasPdfAnnotations) "Annotations" else title
                            Surface(
                                selected = selected,
                                onClick = { selectedPanel = index },
                                modifier = Modifier.weight(1f).interactivePointer(),
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            ) {
                                Column(
                                    Modifier.padding(horizontal = 4.dp, vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(icon, if (headerCollapsed) label else null, Modifier.size(20.dp))
                                    AnimatedVisibility(visible = !headerCollapsed) {
                                        Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1,
                                            overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(if (headerCollapsed) 8.dp else 16.dp))
            when (selectedPanel) {
                0 -> PaperDetailsPanel(paper, Modifier.fillMaxSize())
                1 -> if (pdfWorkspace != null && hasPdfAnnotations) {
                    AnnotationList(pdfWorkspace, Modifier.fillMaxSize(), ideasOnly = null, headerCollapsed = headerCollapsed,
                        allowEditing = allowEditing)
                } else {
                    NotesManager(notes, onAddNote, onDeleteNote, headerCollapsed = headerCollapsed)
                }
                2 -> if (paperAgent != null && pdfWorkspace != null) PaperAgentPanel(
                    paperAgent, pdfWorkspace, headerCollapsed = headerCollapsed,
                    onOpenIdea = { id -> ideaToOpen = id; selectedPanel = 3 }
                )
                else AiAgentsSection(
                    chatMessages = chatMessages,
                    onSendMessage = onSendMessage,
                    errorMessage = chatError,
                )
                3 -> Box(Modifier.fillMaxSize()) {
                    IdeaOrganizer(
                        ideas = ideas,
                        onAddIdea = onAddIdea,
                        onUpdateIdea = onUpdateIdea,
                        isLoading = ideasLoading,
                        error = ideaError,
                        headerCollapsed = headerCollapsed,
                        openIdeaId = ideaToOpen,
                        onIdeaOpened = { ideaToOpen = null }
                    )
                }
            }
        }
    }
    if (pdfWorkspace != null && (pdfWorkspace.state.target != null || pdfWorkspace.state.localTitle != null)) {
        ZoteroPdfReader(pdfWorkspace, focusMode, onExit = onExit,
            onReadingPositionChanged = onReadingPositionChanged,
            supportingContent = supportingContent,
            paperAgent = paperAgent,
            studyTimeToday = studyTimeToday, totalStudyTime = totalStudyTime,
            onInspectAnnotation = {
                selectedPanel = 1
            }, onAskAi = {
                paperAgent?.newChat()
                paperAgent?.askAbout(pdfWorkspace.state.textSelection, pdfWorkspace.state.pageIndex)
                selectedPanel = 2
            }, onFocusModeChange = {
                focusMode = it
                onFocusModeChange(it)
            })
    } else {
        Column(Modifier.fillMaxSize()) {
            TextButton(onClick = onExit, modifier = Modifier.interactivePointer()) { Icon(painterResource(Res.drawable.filled_arrow_back), null); Text("Back to library") }
            BoxWithConstraints(Modifier.weight(1f)) {
                if (maxWidth >= 900.dp) Row(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(1f)) { PdfViewer(paper) }
                    Surface(Modifier.widthIn(min = 0.dp, max = 340.dp).fillMaxHeight()) { supportingContent(true) }
                } else Column {
                    Box(Modifier.weight(1f)) { PdfViewer(paper) }
                    Box(Modifier.weight(1f)) { supportingContent(true) }
                }
            }
        }
    }
}
