package com.paperconnekt.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.random.Random
import com.paperconnekt.app.resources.Res
import com.paperconnekt.app.resources.filled_add
import com.paperconnekt.app.resources.lightbulb

@Composable
fun AssistantScreen(
    store: LocalResearchStore,
    paddingValues: PaddingValues,
    initialPaper: Paper? = null,
    onInitialPaperConsumed: () -> Unit = {},
    secretStore: SecretStore? = null,
    initialZoteroTarget: ZoteroPdfTarget? = null,
    onInitialZoteroTargetConsumed: () -> Unit = {},
    onPdfWorkspaceClosed: () -> Unit = {},
) {
    val sessions by store.assistantSessions.collectAsState()
    val papers by store.papers.collectAsState()
    val settings by store.settings.collectAsState()
    var selectedSessionId by remember { mutableStateOf<String?>(null) }
    var openPaper by remember { mutableStateOf<Paper?>(null) }
    var openZoteroTarget by remember { mutableStateOf<ZoteroPdfTarget?>(null) }
    LaunchedEffect(initialPaper) {
        if (initialPaper != null) {
            openPaper = initialPaper
            onInitialPaperConsumed()
        }
    }
    LaunchedEffect(initialZoteroTarget) {
        if (initialZoteroTarget != null) {
            openZoteroTarget = initialZoteroTarget
            onInitialZoteroTargetConsumed()
        }
    }
    var isCreating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val selectedSession = sessions.firstOrNull { it.id == selectedSessionId }
        ?: sessions.firstOrNull()
    val activeSessionId = selectedSession?.id

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
        val compact = maxWidth < 900.dp
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (compact) 16.dp else 28.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AssistantHeader(
                sessionCount = sessions.size,
                onNewConversation = {
                    if (!isCreating) {
                        isCreating = true
                        val session = newAssistantSession()
                        scope.launch {
                            store.saveAssistantSession(session)
                            selectedSessionId = session.id
                            isCreating = false
                        }
                    }
                },
                isCreating = isCreating,
            )

            if (compact) {
                SessionStrip(
                    sessions = sessions,
                    selectedSessionId = activeSessionId,
                    onSelect = { selectedSessionId = it },
                    onNewConversation = {
                        if (!isCreating) {
                            isCreating = true
                            val session = newAssistantSession()
                            scope.launch {
                                store.saveAssistantSession(session)
                                selectedSessionId = session.id
                                isCreating = false
                            }
                        }
                    },
                )
                AssistantMainContent(
                    modifier = Modifier.weight(1f),
                    session = selectedSession,
                    papers = papers,
                    onOpenPaper = { openPaper = it },
                    onSessionUpdated = { updated ->
                        scope.launch { store.saveAssistantSession(updated) }
                    },
                    onSessionDeleted = {
                        scope.launch {
                            store.removeAssistantSession(it)
                            selectedSessionId = sessions.firstOrNull { session -> session.id != it }?.id
                        }
                    },
                    onCreateSession = {
                        val session = newAssistantSession()
                        scope.launch {
                            store.saveAssistantSession(session)
                            selectedSessionId = session.id
                        }
                    },
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    SessionSidebar(
                        modifier = Modifier.width(270.dp),
                        sessions = sessions,
                        selectedSessionId = activeSessionId,
                        onSelect = { selectedSessionId = it },
                        onNewConversation = {
                            if (!isCreating) {
                                isCreating = true
                                val session = newAssistantSession()
                                scope.launch {
                                    store.saveAssistantSession(session)
                                    selectedSessionId = session.id
                                    isCreating = false
                                }
                            }
                        },
                    )
                    AssistantMainContent(
                        modifier = Modifier.weight(1f),
                        session = selectedSession,
                        papers = papers,
                        onOpenPaper = { openPaper = it },
                        onSessionUpdated = { updated ->
                            scope.launch { store.saveAssistantSession(updated) }
                        },
                        onSessionDeleted = {
                            scope.launch {
                                store.removeAssistantSession(it)
                                selectedSessionId = sessions.firstOrNull { session -> session.id != it }?.id
                            }
                        },
                        onCreateSession = {
                            val session = newAssistantSession()
                            scope.launch {
                                store.saveAssistantSession(session)
                                selectedSessionId = session.id
                            }
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
    onNewConversation: () -> Unit,
    isCreating: Boolean,
) {
    WorkspaceHeader(
        title = "Assistant",
        subtitle = "Explore questions, read deeply, and turn papers into understanding.",
        icon = Res.drawable.lightbulb,
        summaries = listOf(
            "$sessionCount conversation${if (sessionCount == 1) "" else "s"}",
            "History saved locally",
        ),
        actions = {
            HeaderAction(
                label = if (isCreating) "Creating…" else "New conversation",
                icon = Res.drawable.filled_add,
                onClick = onNewConversation,
                enabled = !isCreating,
            )
        },
    )
}

@Composable
private fun SessionSidebar(
    modifier: Modifier,
    sessions: List<AssistantSession>,
    selectedSessionId: String?,
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
            if (sessions.isEmpty()) {
                EmptyHistory()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(sessions.sortedByDescending { it.updatedAt }, key = AssistantSession::id) { session ->
                        SessionListItem(
                            session = session,
                            selected = session.id == selectedSessionId,
                            onClick = { onSelect(session.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionStrip(
    sessions: List<AssistantSession>,
    selectedSessionId: String?,
    onSelect: (String) -> Unit,
    onNewConversation: () -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            OutlinedButton(onClick = onNewConversation) { Text("+ New") }
        }
        items(sessions.sortedByDescending { it.updatedAt }, key = AssistantSession::id) { session ->
            FilterChip(
                selected = session.id == selectedSessionId,
                onClick = { onSelect(session.id) },
                label = { Text(session.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            )
        }
    }
}

@Composable
private fun SessionListItem(
    session: AssistantSession,
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
                session.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                session.messages.lastOrNull()?.content ?: "No messages yet",
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
    session: AssistantSession?,
    papers: List<Paper>,
    onOpenPaper: (Paper) -> Unit,
    onSessionUpdated: (AssistantSession) -> Unit,
    onSessionDeleted: (String) -> Unit,
    onCreateSession: () -> Unit,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (session == null) {
            WelcomePanel(onCreate = onCreateSession)
        } else {
            ConversationPanel(
                session = session,
                onSessionUpdated = onSessionUpdated,
                onSessionDeleted = onSessionDeleted,
            )
        }
        PaperShelf(
            papers = papers,
            onOpenPaper = onOpenPaper,
        )
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
                        val now = Clock.System.now()
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
                onSessionUpdated(session.copy(title = title, updatedAt = Clock.System.now()))
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Saved papers and PDFs", style = MaterialTheme.typography.titleMedium)
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
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(papers, key = Paper::id) { paper ->
                        PaperShelfCard(paper = paper, onOpen = { onOpenPaper(paper) })
                    }
                }
            }
        }
    }
}

@Composable
private fun PaperShelfCard(
    paper: Paper,
    onOpen: () -> Unit,
) {
    Card(
        modifier = Modifier.width(230.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
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
            OutlinedButton(onClick = onOpen, enabled = paper.pdfPath != null) {
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
    updatedAt = Clock.System.now(),
)

private fun formatMessageTime(instant: Instant): String =
    instant.toString().replace('T', ' ').substringBefore('.').removeSuffix("Z")
