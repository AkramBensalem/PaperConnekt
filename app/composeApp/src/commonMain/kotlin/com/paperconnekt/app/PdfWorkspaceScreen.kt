package com.paperconnekt.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.CompositionLocalProvider
import com.akram.bensalem.paper.connekt.feature.assistant.AssistantRepository
import com.akram.bensalem.paper.connekt.feature.assistant.LOCAL_RESEARCH_ACCOUNT
import com.akram.bensalem.paper.connekt.feature.assistant.AssistantViewModel
import com.akram.bensalem.paper.connekt.feature.assistant.PdfWorkspaceViewModel
import com.akram.bensalem.paper.connekt.feature.assistant.StoredLocalPdfAnnotations
import com.akram.bensalem.paper.connekt.feature.assistant.ZoteroPdfTarget
import com.akram.bensalem.paper.connekt.feature.assistant.component.Workplace
import com.akram.bensalem.paper.connekt.models.ZoteroConfig
import com.akram.bensalem.paper.connekt.models.Paper as AssistantPaper
import com.paperconnekt.domain.model.Paper
import com.paperconnekt.domain.model.SavedPdfReadingPosition
import com.paperconnekt.secrets.SecretKey
import com.paperconnekt.secrets.SecretStore
import com.paperconnekt.storage.LocalResearchStore
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import com.paperconnekt.app.resources.Res
import org.jetbrains.compose.resources.stringResource
import com.paperconnekt.app.resources.back_to_assistant
import com.paperconnekt.app.resources.back_to_library
import com.paperconnekt.app.resources.discard_and_exit
import com.paperconnekt.app.resources.discard_changes_message
import com.paperconnekt.app.resources.discard_unsaved_changes
import com.paperconnekt.app.resources.discard_zotero_changes_message
import com.paperconnekt.app.resources.keep_editing
import com.paperconnekt.app.resources.try_again
import com.paperconnekt.app.resources.unable_to_open_pdf

internal class PdfNavigationGuard {
    var beforeNavigate: ((() -> Unit) -> Unit)? = null

    fun navigate(action: () -> Unit) {
        val guard = beforeNavigate
        if (guard == null) action() else guard(action)
    }
}

internal val LocalPdfNavigationGuard = staticCompositionLocalOf<PdfNavigationGuard?> { null }

@Composable
fun PdfWorkspaceScreen(
    paper: Paper,
    store: LocalResearchStore,
    paddingValues: PaddingValues,
    onBack: () -> Unit,
    secretStore: SecretStore? = null,
) {
    val owner = remember(paper.id, paper.pdfPath) {
        object : ViewModelStoreOwner {
            override val viewModelStore = ViewModelStore()
        }
    }
    DisposableEffect(owner) {
        onDispose { owner.viewModelStore.clear() }
    }
    CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
        key(owner) {
            LocalPaperWorkspace(paper, store, paddingValues, onBack, secretStore)
        }
    }
}

@Composable
private fun LocalPaperWorkspace(
    paper: Paper,
    store: LocalResearchStore,
    paddingValues: PaddingValues,
    onBack: () -> Unit,
    secretStore: SecretStore?,
) {
    val storage = LocalPdfAttachments.current.storage
    val settings by store.settings.collectAsState()
    val positionKey = "pdf/local/${paper.id}"
    val savedPosition = remember(positionKey) { settings.pdfReadingPositions[positionKey] ?: SavedPdfReadingPosition() }
    val assistant = viewModel { AssistantViewModel(AssistantRepository()) }
    val workspace = viewModel {
        PdfWorkspaceViewModel(
            mistralApiKey = { secretStore?.read(SecretKey.MISTRAL).orEmpty() },
            usageRecorder = assistant::recordUsage,
            localAnnotationStore = StoredLocalPdfAnnotations(storage),
        )
    }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var attempt by remember { mutableStateOf(0) }
    var pendingExit by remember { mutableStateOf<(() -> Unit)?>(null) }
    var pendingPosition by remember { mutableStateOf<SavedPdfReadingPosition?>(null) }
    val persistenceScope = rememberCoroutineScope()
    suspend fun persistPosition(position: SavedPdfReadingPosition) {
        store.updateSettings { current ->
            val positions = (current.pdfReadingPositions + (positionKey to position))
                .toList()
                .takeLast(200)
                .toMap()
            current.copy(pdfReadingPositions = positions)
        }
    }
    LaunchedEffect(pendingPosition) {
        val position = pendingPosition ?: return@LaunchedEffect
        delay(500)
        if (pendingPosition == position) {
            try {
                persistPosition(position)
                pendingPosition = null
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Napier.e("Could not save local PDF position", error)
            }
        }
    }
    val navigation = LocalPdfNavigationGuard.current
    val requestExit: (() -> Unit) -> Unit = { action ->
        if (workspace.state.draft != null || workspace.state.pendingWrites > 0) pendingExit = action else action()
    }
    DisposableEffect(navigation, workspace) {
        navigation?.beforeNavigate = requestExit
        onDispose { navigation?.beforeNavigate = null }
    }
    val assistantPaper = remember(paper) {
        AssistantPaper(
            id = paper.id,
            title = paper.title,
            authors = paper.authors,
            abstract = paper.abstractText,
            publicationDate = paper.year?.toString(),
            doi = paper.doi,
            tags = paper.tags,
            publicationName = paper.source.takeIf { it.isNotBlank() && it != "Local" },
            url = paper.sourceUrl,
            filePath = paper.pdfPath,
            createdAt = 0L,
            updatedAt = 0L,
            visitedAt = kotlin.time.Clock.System.now().toEpochMilliseconds(),
        )
    }
    LaunchedEffect(assistant, assistantPaper.id) {
        store.updatePaper(paper.id) { it.copy(lastOpenedAt = kotlin.time.Clock.System.now().toEpochMilliseconds()) }
        assistant.updateHistoryAccount(LOCAL_RESEARCH_ACCOUNT)
        assistant.selectPaper(assistantPaper)
    }
    LaunchedEffect(workspace, attempt) {
        loading = true
        error = null
        try {
            assistant.updateHistoryAccount(LOCAL_RESEARCH_ACCOUNT)
            val path = requireNotNull(paper.pdfPath) { "Attach a PDF to this item before opening the viewer." }
            val bytes = storage.readPdf(path)
            workspace.openLocal(
                bytes,
                paper.title,
                storageKey = "local/${paper.id}/$path",
                initialPosition = com.akram.bensalem.paper.connekt.feature.assistant.PdfReadingPosition(savedPosition.pageIndex, savedPosition.fraction),
                initialZoom = savedPosition.zoom,
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (exception: Exception) {
            Napier.e("Could not read attached PDF", exception)
            error = exception.message ?: "Could not read the attached PDF."
        } finally {
            loading = false
        }
    }
    if (pendingExit != null) {
        AlertDialog(
            onDismissRequest = { pendingExit = null },
            title = { Text(stringResource(Res.string.discard_unsaved_changes)) },
            text = { Text(stringResource(Res.string.discard_changes_message)) },
            confirmButton = { TextButton(onClick = {
                val exit = pendingExit
                pendingExit = null
                exit?.invoke()
            }) { Text(stringResource(Res.string.discard_and_exit)) } },
            dismissButton = { TextButton(onClick = { pendingExit = null }) { Text(stringResource(Res.string.keep_editing)) } },
        )
    }
    Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
        when {
            loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            error != null -> Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(stringResource(Res.string.unable_to_open_pdf), style = MaterialTheme.typography.titleLarge)
                Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
                Button(onClick = { attempt++ }) { Text(stringResource(Res.string.try_again)) }
                TextButton(onClick = onBack) { Text(stringResource(Res.string.back_to_assistant)) }
            }
            else -> {
                val state = assistant.state
                Workplace(
                    paper = assistantPaper,
                    notes = state.notes,
                    chatMessages = state.chatMessages,
                    ideas = state.ideas,
                    onAddNote = assistant::addNote,
                    onDeleteNote = assistant::deleteNote,
                    onSendMessage = assistant::sendMessage,
                    onAddIdea = assistant::addIdea,
                    onUpdateIdea = assistant::updateIdea,
                    ideasLoading = state.ideasLoading,
                    ideaError = state.ideaError,
                    ideaBoard = assistant,
                    usageRecorder = assistant::recordUsage,
                    pdfWorkspace = workspace,
                    secretStore = secretStore,
                    semanticSearchEnabled = settings.semanticSearchEnabled,
                    onReadingPositionChanged = { position, zoom ->
                        pendingPosition = SavedPdfReadingPosition(position.pageIndex, position.fraction, zoom)
                    },
                    onExit = {
                        requestExit {
                            val position = SavedPdfReadingPosition(
                                workspace.state.readingPosition.pageIndex,
                                workspace.state.readingPosition.fraction,
                                workspace.state.zoom,
                            )
                            persistenceScope.launch(NonCancellable) { persistPosition(position) }
                            onBack()
                        }
                    },
                )
            }
        }
    }
}

@Composable
fun ZoteroPdfWorkspaceScreen(
    target: ZoteroPdfTarget,
    store: LocalResearchStore,
    zoteroUserId: String,
    paddingValues: PaddingValues,
    onBack: () -> Unit,
    secretStore: SecretStore? = null,
) {
    val owner = remember(target.item.key, target.attachment.key, target.groupId) {
        object : ViewModelStoreOwner {
            override val viewModelStore = ViewModelStore()
        }
    }
    DisposableEffect(owner) {
        onDispose { owner.viewModelStore.clear() }
    }
    CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
        key(owner) {
            ZoteroPaperWorkspace(target, zoteroUserId, store, paddingValues, onBack, secretStore)
        }
    }
}

@Composable
private fun ZoteroPaperWorkspace(
    target: ZoteroPdfTarget,
    zoteroUserId: String,
    store: LocalResearchStore,
    paddingValues: PaddingValues,
    onBack: () -> Unit,
    secretStore: SecretStore?,
) {
    val storage = LocalPdfAttachments.current.storage
    val settings by store.settings.collectAsState()
    val positionKey = "pdf/zotero/$zoteroUserId/${target.groupId ?: "personal"}/${target.attachment.key}"
    val savedPosition = remember(positionKey) { settings.pdfReadingPositions[positionKey] ?: SavedPdfReadingPosition() }
    val assistant = viewModel { AssistantViewModel(AssistantRepository()) }
    val workspace = viewModel {
        PdfWorkspaceViewModel(
            mistralApiKey = { secretStore?.read(SecretKey.MISTRAL).orEmpty() },
            usageRecorder = assistant::recordUsage,
            pdfCache = storage,
        )
    }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var pendingExit by remember { mutableStateOf<(() -> Unit)?>(null) }
    var pendingPosition by remember { mutableStateOf<SavedPdfReadingPosition?>(null) }
    val persistenceScope = rememberCoroutineScope()
    suspend fun persistPosition(position: SavedPdfReadingPosition) {
        store.updateSettings { current ->
            val positions = (current.pdfReadingPositions + (positionKey to position))
                .toList()
                .takeLast(200)
                .toMap()
            current.copy(pdfReadingPositions = positions)
        }
    }
    LaunchedEffect(pendingPosition) {
        val position = pendingPosition ?: return@LaunchedEffect
        delay(500)
        if (pendingPosition == position) {
            try {
                persistPosition(position)
                pendingPosition = null
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Napier.e("Could not save Zotero PDF position", error)
            }
        }
    }
    val navigation = LocalPdfNavigationGuard.current
    val requestExit: (() -> Unit) -> Unit = { action ->
        if (workspace.state.draft != null || workspace.state.pendingWrites > 0) pendingExit = action else action()
    }
    DisposableEffect(navigation, workspace) {
        navigation?.beforeNavigate = requestExit
        onDispose { navigation?.beforeNavigate = null }
    }
    LaunchedEffect(workspace, assistant, target, zoteroUserId) {
        loading = true
        error = null
        try {
            val apiKey = requireNotNull(secretStore?.read(SecretKey.ZOTERO)?.takeIf { it.isNotBlank() }) {
                "Connect Zotero in Settings before opening this PDF."
            }
            require(zoteroUserId.isNotBlank()) { "Your Zotero user ID is missing." }
            workspace.updateConfig(
                ZoteroConfig(apiKey = apiKey, userId = zoteroUserId, groupId = target.groupId),
            )
            assistant.updateHistoryAccount(zoteroUserId.trim())
            assistant.selectZoteroPaper(target)
            workspace.open(
                target,
                initialPosition = com.akram.bensalem.paper.connekt.feature.assistant.PdfReadingPosition(
                    savedPosition.pageIndex,
                    savedPosition.fraction,
                ),
                initialZoom = savedPosition.zoom,
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (exception: Exception) {
            Napier.e("Could not open Zotero PDF", exception)
            error = exception.message ?: "Could not open the Zotero PDF."
        } finally {
            loading = false
        }
    }
    if (pendingExit != null) {
        AlertDialog(
            onDismissRequest = { pendingExit = null },
            title = { Text(stringResource(Res.string.discard_unsaved_changes)) },
            text = { Text(stringResource(Res.string.discard_zotero_changes_message)) },
            confirmButton = {
                TextButton(onClick = {
                    val exit = pendingExit
                    pendingExit = null
                    exit?.invoke()
                }) { Text(stringResource(Res.string.discard_and_exit)) }
            },
            dismissButton = { TextButton(onClick = { pendingExit = null }) { Text(stringResource(Res.string.keep_editing)) } },
        )
    }
    Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
        when {
            loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            error != null -> Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(stringResource(Res.string.unable_to_open_pdf), style = MaterialTheme.typography.titleLarge)
                Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
                TextButton(onClick = onBack) { Text(stringResource(Res.string.back_to_library)) }
            }
            else -> {
                val state = assistant.state
                val activeStudy = state.history.papers.firstOrNull {
                    it.target.attachment.key == target.attachment.key &&
                        it.target.groupId == target.groupId
                }
                val today = kotlinx.datetime.Clock.System.now()
                    .toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
                state.selectedPaper?.let { paper ->
                    Workplace(
                        paper = paper,
                        notes = state.notes,
                        chatMessages = state.chatMessages,
                        ideas = state.ideas,
                        onAddNote = assistant::addNote,
                        onDeleteNote = assistant::deleteNote,
                        onSendMessage = assistant::sendMessage,
                        onAddIdea = assistant::addIdea,
                        onUpdateIdea = assistant::updateIdea,
                        ideasLoading = state.ideasLoading,
                        ideaError = state.ideaError,
                        ideaBoard = assistant,
                        usageRecorder = assistant::recordUsage,
                        pdfWorkspace = workspace,
                        studyTimeToday = activeStudy?.dailyStudyMillis?.get(today) ?: 0L,
                        totalStudyTime = activeStudy?.totalStudyMillis ?: 0L,
                        secretStore = secretStore,
                        semanticSearchEnabled = settings.semanticSearchEnabled,
                        onReadingPositionChanged = { position, zoom ->
                            pendingPosition = SavedPdfReadingPosition(position.pageIndex, position.fraction, zoom)
                        },
                        onExit = {
                            requestExit {
                                val position = SavedPdfReadingPosition(
                                    workspace.state.readingPosition.pageIndex,
                                    workspace.state.readingPosition.fraction,
                                    workspace.state.zoom,
                                )
                                persistenceScope.launch(NonCancellable) { persistPosition(position) }
                                onBack()
                            }
                        },
                    )
                }
            }
        }
    }
}
