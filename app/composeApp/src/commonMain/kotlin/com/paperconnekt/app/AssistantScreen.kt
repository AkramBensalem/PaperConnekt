package com.paperconnekt.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.paperconnekt.domain.model.AssistantMessage
import com.paperconnekt.domain.model.AssistantRole
import com.paperconnekt.domain.model.AssistantSession
import com.paperconnekt.domain.model.Paper
import com.paperconnekt.storage.LocalResearchStore
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlin.random.Random

@Composable
fun AssistantScreen(
    store: LocalResearchStore,
    paddingValues: androidx.compose.foundation.layout.PaddingValues,
) {
    val sessions by store.assistantSessions.collectAsState()
    val papers by store.papers.collectAsState()
    var selectedSessionId by remember { mutableStateOf<String?>(null) }
    var draft by remember { mutableStateOf("") }
    var openPdf by remember { mutableStateOf<Paper?>(null) }
    val scope = rememberCoroutineScope()
    val selectedSession = sessions.firstOrNull { it.id == selectedSessionId }

    if (openPdf != null) {
        PdfWorkspaceScreen(
            paper = openPdf!!,
            onBack = { openPdf = null },
            paddingValues = paddingValues,
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("AI assistance")
        Text("Keep local conversation history and open a saved paper in the PDF workspace.")

        Button(
            onClick = {
                val session = AssistantSession(
                    id = "assistant-${Random.nextLong()}",
                    title = "New research conversation",
                    updatedAt = Clock.System.now(),
                )
                scope.launch {
                    store.saveAssistantSession(session)
                    selectedSessionId = session.id
                }
            },
        ) {
            Text("New conversation")
        }

        if (sessions.isNotEmpty()) {
            Text("History")
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(sessions, key = AssistantSession::id) { session ->
                    TextButton(onClick = { selectedSessionId = session.id }) {
                        Text(
                            text = if (session.id == selectedSessionId) {
                                "• ${session.title}"
                            } else {
                                session.title
                            },
                        )
                    }
                }
            }
        } else {
            Text("No conversations yet.")
        }

        selectedSession?.let { session ->
            ConversationPanel(
                session = session,
                draft = draft,
                onDraftChanged = { draft = it },
                onSend = {
                    val message = draft.trim()
                    if (message.isNotEmpty()) {
                        val updated = session.copy(
                            title = session.title.takeUnless {
                                it == "New research conversation"
                            } ?: message.take(48),
                            messages = session.messages + listOf(
                                AssistantMessage(AssistantRole.USER, message),
                                AssistantMessage(
                                    AssistantRole.ASSISTANT,
                                    "Your local assistant session is ready for an AI provider.",
                                ),
                            ),
                            updatedAt = Clock.System.now(),
                        )
                        scope.launch { store.saveAssistantSession(updated) }
                        draft = ""
                    }
                },
            )
        }

        if (papers.any { it.pdfPath != null }) {
            Text("Saved PDFs")
            papers.filter { it.pdfPath != null }.forEach { paper ->
                TextButton(onClick = { openPdf = paper }) {
                    Text(paper.title)
                }
            }
        }
    }
}

@Composable
private fun ConversationPanel(
    session: AssistantSession,
    draft: String,
    onDraftChanged: (String) -> Unit,
    onSend: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            session.messages.forEach { message ->
                Text("${message.role.name.lowercase().replaceFirstChar { it.uppercase() }}: ${message.content}")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    modifier = Modifier.weight(1f),
                    value = draft,
                    onValueChange = onDraftChanged,
                    label = { Text("Ask about your research") },
                )
                Button(onClick = onSend) {
                    Text("Send")
                }
            }
        }
    }
}

@Composable
private fun PdfWorkspaceScreen(
    paper: Paper,
    onBack: () -> Unit,
    paddingValues: androidx.compose.foundation.layout.PaddingValues,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextButton(onClick = onBack) {
            Text("Back to assistant")
        }
        Text(paper.title)
        Text("PDF workspace")
        Text(paper.pdfPath ?: "No PDF path configured.")
        Text("Native PDF rendering and annotations will be added behind this shared workspace contract.")
    }
}

