package com.paperconnekt.idea

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import com.akram.bensalem.paper.connekt.feature.assistant.*
import com.akram.bensalem.paper.connekt.models.Paper as AssistantPaper
import com.paperconnekt.domain.model.Paper
import com.paperconnekt.domain.model.SavedPdfReadingPosition
import com.paperconnekt.secrets.SecretKey
import com.paperconnekt.storage.DesktopPdfAttachmentStorage
import com.paperconnekt.storage.LocalResearchStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.CoroutineStart
import java.io.File
import java.util.UUID

internal class NativeFeatures(
    private val store: LocalResearchStore,
    private val scope: CoroutineScope,
) : AutoCloseable {
    private val models = ViewModelStore()
    val secrets = IdeaSecretStore()
    private val attachments = DesktopPdfAttachmentStorage()
    private fun <T : ViewModel> own(key: String, model: T): T = model.also { models.put(key, it) }
    val assistant = own("assistant", AssistantViewModel(AssistantRepository()))
    val reader = own("reader", PdfWorkspaceViewModel(
        localAnnotationStore = StoredLocalPdfAnnotations(attachments),
        mistralApiKey = { secrets.read(SecretKey.MISTRAL).orEmpty() },
        usageRecorder = assistant::recordUsage,
    ))
    var agent by mutableStateOf(createAgent())
        private set
    private fun createAgent() = own("agent", PaperAgentViewModel(
        workspace = reader,
        ideaBoard = assistant,
        usageRecorder = assistant::recordUsage,
        secretStore = secrets,
    )).also { it.semanticSearchEnabled = store.settings.value.semanticSearchEnabled }
    private var document: PdfDocument? = null
    private var opening: Job? = null
    private var documentVersion = 0
    var paper by mutableStateOf<Paper?>(null)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var openingPdf by mutableStateOf(false)
        private set

    suspend fun zoteroUserId(): String = store.settings.value.zoteroUserId

    suspend fun saveZoteroUserId(userId: String) {
        store.updateSettings { it.copy(zoteroUserId = userId) }
    }

    fun open(paper: Paper): Boolean {
        if (reader.state.draft != null || reader.state.pendingWrites > 0) {
            if (!confirmNative("Discard the unsaved annotation draft and pending changes before opening another PDF?")) return false
        }
        val version = ++documentVersion
        flushReadingPosition()
        opening?.cancel()
        this.paper = paper
        error = null
        agent.stop()
        agent = createAgent()
        reader.close()
        document?.close()
        document = null
        openingPdf = true
        opening = scope.launch {
            var opened: PdfDocument? = null
            try {
                val path = requireNotNull(paper.pdfPath) { "Attach a PDF before opening the reader." }
                val bytes = attachments.readPdf(path)
                val position = store.settings.value.pdfReadingPositions["pdf/local/${paper.id}"]
                    ?: SavedPdfReadingPosition()
                // Keep ownership even when cancellation arrives as PDFBox opens the file.
                withContext(kotlinx.coroutines.NonCancellable) { opened = openJvmPdfDocument(bytes) }
                ensureActive()
                val activeDocument = checkNotNull(opened)
                document = opened
                reader.openLocal(
                    bytes, paper.title, "local/${paper.id}/$path",
                    PdfReadingPosition(position.pageIndex, position.fraction), position.zoom,
                )
                reader.attachPageProvider(activeDocument.pageCount) { index ->
                    activeDocument.render(index, maxOf(1.5f, reader.state.zoom * 1.5f))
                }
                assistant.updateHistoryAccount(LOCAL_RESEARCH_ACCOUNT)
                assistant.selectPaper(AssistantPaper(
                    id = paper.id, title = paper.title, authors = paper.authors,
                    abstract = paper.abstractText, doi = paper.doi, tags = paper.tags,
                    publicationDate = paper.year?.toString(), url = paper.sourceUrl,
                    filePath = path, createdAt = 0L, updatedAt = 0L,
                    visitedAt = System.currentTimeMillis(),
                ))
                store.updatePaper(paper.id) { it.copy(lastOpenedAt = System.currentTimeMillis()) }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                opened?.close()
                throw cancelled
            } catch (exception: Exception) {
                com.intellij.openapi.diagnostic.Logger.getInstance(NativeFeatures::class.java)
                    .warn("Could not open the local PDF", exception)
                error = exception.message ?: "Could not open the local PDF."
            } finally {
                if (version == documentVersion) openingPdf = false
            }
        }
        return true
    }

    suspend fun importPdf(source: String): Paper {
        val path = attachments.importPdf(source)
        val paper = Paper(
            id = UUID.randomUUID().toString(),
            title = File(source).nameWithoutExtension,
            pdfPath = path,
        )
        store.savePaper(paper)
        return paper
    }

    fun closeReader(): Boolean {
        if (reader.state.draft != null || reader.state.pendingWrites > 0) {
            if (!confirmNative("Discard the unsaved annotation draft and pending changes before closing the reader?")) return false
        }
        flushReadingPosition()
        documentVersion++
        opening?.cancel()
        agent.stop()
        reader.close()
        assistant.exitWorkplace()
        document?.close()
        document = null
        paper = null
        openingPdf = false
        return true
    }

    suspend fun saveReadingPosition() {
        val current = paper ?: return
        if (reader.state.pageCount == 0) return
        val state = reader.state
        store.updateSettings {
            it.copy(pdfReadingPositions = (it.pdfReadingPositions + (
                "pdf/local/${current.id}" to SavedPdfReadingPosition(
                    state.readingPosition.pageIndex, state.readingPosition.fraction, state.zoom,
                )
            )).toList().takeLast(200).toMap())
        }
    }

    private fun flushReadingPosition() {
        scope.launch(NonCancellable, start = CoroutineStart.UNDISPATCHED) {
            try {
                saveReadingPosition()
            } catch (exception: Exception) {
                reportError("Could not save reading position", exception)
            }
        }
    }

    fun reportError(message: String, exception: Exception) {
        error = "$message: ${exception.message ?: "Unknown error"}"
        com.intellij.openapi.diagnostic.Logger.getInstance(NativeFeatures::class.java).warn(message, exception)
    }

    fun createIdea(title: String, content: String, color: String) {
        val paperId = assistant.state.selectedPaper?.id ?: return
        scope.launch {
            try {
                val idea = assistant.createIdeaNote(paperId, title, content)
                assistant.updateIdeaNote(paperId, idea.id, null, null, color)
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                error = exception.message ?: "Could not save the idea."
                com.intellij.openapi.diagnostic.Logger.getInstance(NativeFeatures::class.java)
                    .warn("Could not save idea", exception)
            }
        }
    }

    override fun close() {
        flushReadingPosition()
        documentVersion++
        opening?.cancel()
        models.clear()
        document?.close()
        document = null
    }
}
