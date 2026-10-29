package com.akram.bensalem.paper.connekt.feature.assistant

import com.akram.bensalem.bistosh.core.designsystem.component.interactivePointer

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.akram.bensalem.paper.connekt.feature.assistant.component.ResearchDashboard
import com.akram.bensalem.paper.connekt.feature.assistant.component.Workplace
import com.akram.bensalem.paper.connekt.core.remote.functions.TextToSpeechRemoteDataSource
import androidx.compose.runtime.DisposableEffect
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantScreen(
    modifier: Modifier = Modifier,
    viewModel: AssistantViewModel,
    pdfWorkspace: PdfWorkspaceViewModel? = null,
    textToSpeech: TextToSpeechRemoteDataSource? = null,
    onOpenZotero: () -> Unit = {},
    onOpenPaper: (ZoteroPdfTarget) -> Unit = {},
    onExitWorkspace: (() -> Unit)? = null
) {
    val state = viewModel.state
    var confirmExit by remember { mutableStateOf(false) }
    val exitWorkspace = {
        pdfWorkspace?.close()
        viewModel.exitWorkplace()
        confirmExit = false
    }
    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            title = { Text("Discard unsynchronized changes?") },
            text = { Text("Your annotation draft or queued changes have not finished synchronizing with Zotero. Leaving the workspace will discard local changes that have not reached Zotero.") },
            confirmButton = { TextButton(onClick = exitWorkspace, modifier = Modifier.interactivePointer()) { Text("Discard and exit") } },
            dismissButton = { TextButton(onClick = { confirmExit = false }, modifier = Modifier.interactivePointer()) { Text("Keep editing") } }
        )
    }
    val paper = state.selectedPaper
    if (state.isWorkplaceActive && paper != null) {
        val activeTarget = pdfWorkspace?.state?.target
        val activeStudy = activeTarget?.let { target ->
            state.history.papers.firstOrNull {
                it.target.attachment.key == target.attachment.key && it.target.groupId == target.groupId
            }
        }
        val usagePaperId = activeStudy?.id ?: activeTarget?.let { target ->
            target.groupId?.let { "group:$it:${target.attachment.key}" } ?: "personal:${target.attachment.key}"
        }
        val today = kotlinx.datetime.Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
        val readAloud = textToSpeech?.let { remote ->
            remember(remote, paper.id, pdfWorkspace?.state?.target?.attachment?.key) {
                ReadAloudController(remote) { event ->
                    viewModel.recordUsage(usagePaperId, event)
                }
            }
        }
        readAloud?.let { controller ->
            DisposableEffect(controller) {
                onDispose { controller.close() }
            }
        }
        Box(modifier.fillMaxSize()) {
            Workplace(
                paper = paper, notes = state.notes, chatMessages = state.chatMessages, ideas = state.ideas,
                onAddNote = viewModel::addNote, onDeleteNote = viewModel::deleteNote,
                onSendMessage = viewModel::sendMessage, onAddIdea = viewModel::addIdea,
                onUpdateIdea = viewModel::updateIdea,
                ideasLoading = state.ideasLoading,
                ideaError = state.ideaError,
                ideaBoard = viewModel,
                usageRecorder = viewModel::recordUsage,
                pdfWorkspace = pdfWorkspace,
                readAloud = readAloud,
                studyTimeToday = activeStudy?.dailyStudyMillis?.get(today) ?: 0L,
                totalStudyTime = activeStudy?.totalStudyMillis ?: 0L,
                onExit = onExitWorkspace ?: {
                    if (pdfWorkspace?.state?.draft != null || (pdfWorkspace?.state?.pendingWrites ?: 0) > 0) confirmExit = true else exitWorkspace()
                }
            )
        }
        return
    }

    Scaffold(
        modifier = modifier,
    ) { padding ->
        ResearchDashboard(
            state = state,
            onOpenPaper = onOpenPaper,
            onOpenZotero = onOpenZotero,
            onRetry = viewModel::refreshHistory,
            modifier = Modifier.padding(padding).fillMaxSize()
        )
    }
}
