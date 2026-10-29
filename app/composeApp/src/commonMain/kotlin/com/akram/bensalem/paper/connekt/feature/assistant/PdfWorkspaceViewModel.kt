package com.akram.bensalem.paper.connekt.feature.assistant

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akram.bensalem.paper.connekt.core.aiassistant.ResearchAgent
import com.akram.bensalem.paper.connekt.core.aiassistant.createReadingAgent
import com.akram.bensalem.paper.connekt.core.settings.createApiKeysStore
import com.akram.bensalem.paper.connekt.models.ZoteroConfig
import com.akram.bensalem.paper.connekt.shared.zotero.model.zotero.ZoteroAnnotation
import io.github.aakira.napier.Napier
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

val inkWidthPresets = listOf(0.5f, 1f, 2f, 3f, 5f, 8f, 12f)
val textSizePresets = listOf(8f, 10f, 12f, 14f, 18f, 24f, 32f)

private fun nextPreset(presets: List<Float>, current: Float, larger: Boolean): Float? =
    if (larger) presets.firstOrNull { it > current + 0.01f } else presets.lastOrNull { it < current - 0.01f }

/** Display-space box that fits [text] at [fontSize], anchored at its top-left corner. */
val movableAnnotationTypes = setOf("ink", "image", "text", "note")
val scalableAnnotationTypes = setOf("ink", "image")

fun textBoxRect(left: Float, top: Float, text: String, fontSize: Float, maxRight: Float): Rect {
    val lines = text.ifBlank { " " }.lines()
    val width = (lines.maxOf { it.length } * fontSize * 0.55f).coerceAtLeast(fontSize * 2f)
    val height = lines.size * fontSize * 1.25f + fontSize * 0.3f
    return Rect(left, top, minOf(left + width, maxRight).coerceAtLeast(left + 1f), top + height)
}

data class AnnotationDraft(
    val type: String,
    val pageIndex: Int,
    val rects: List<List<Float>>,
    val text: String = "",
    val comment: String = "",
    val color: String = "#ffd400",
    val paths: List<List<Float>> = emptyList(),
    val width: Float = 2f,
    val fontSize: Float = 12f,
    val editing: ZoteroAnnotation? = null,
    val key: String = (1..8).map { "23456789ABCDEFGHIJKLMNPQRSTUVWXYZ".random() }.joinToString(""),
    val isIdea: Boolean = false,
    val aiGenerated: Boolean = false
)

enum class ReaderViewMode { Pdf, Markdown }

data class PdfMarkdownState(
    val content: String? = null,
    val loading: Boolean = false,
    val converting: Boolean = false,
    val error: String? = null,
    val syncError: String? = null,
    val missingApiKey: Boolean = false
) {
    val busy: Boolean get() = loading || converting
    val pages: List<String> by lazy { content?.let(::markdownPages).orEmpty() }
    val hasPageBoundaries: Boolean get() = content?.let(::hasMarkdownPages) == true
}

/** AI-cleaned reading script (`reading.md`): what Read mode speaks, page by page. */
data class ReadingScriptState(
    val content: String? = null,
    val preparing: Boolean = false,
    val error: String? = null,
) {
    val pages: Map<Int, List<ReadingBlock>> by lazy { content?.let(::parseReadingScript).orEmpty() }
}

data class PdfWorkspaceState(
    val target: ZoteroPdfTarget? = null,
    val localTitle: String? = null,
    val localStorageKey: String? = null,
    val pageCount: Int = 0,
    val pageIndex: Int = 0,
    val pageNavigationId: Int = 0,
    val readingPosition: PdfReadingPosition = PdfReadingPosition(),
    val scrollSource: PdfScrollSource = PdfScrollSource.Navigation,
    val pageGeometries: Map<Int, PdfGeometry> = emptyMap(),
    val displayedPageWidth: Float = 500f,
    val page: PdfPage? = null,
    val zoom: Float = 1f,
    val annotations: List<ZoteroAnnotation> = emptyList(),
    val draft: AnnotationDraft? = null,
    val selectedKey: String? = null,
    val textSelection: PdfTextSelection? = null,
    val annotationColor: String = "#ffd400",
    val inkWidth: Float = 2f,
    val textSize: Float = 12f,
    val adjustingTextRange: Boolean = false,
    val loading: Boolean = false,
    val rendering: Boolean = false,
    val syncing: Boolean = false,
    val needsRefresh: Boolean = false,
    val pendingWrites: Int = 0,
    val error: String? = null,
    val message: String? = null,
    val viewMode: ReaderViewMode = ReaderViewMode.Pdf,
    val markdown: PdfMarkdownState = PdfMarkdownState(),
    val readingScript: ReadingScriptState = ReadingScriptState(),
) {
    val canWrite: Boolean get() = !loading && page != null && localTitle == null
    val readerPageCount: Int get() = if (viewMode == ReaderViewMode.Markdown) markdown.pages.size else pageCount
}

class PdfWorkspaceViewModel(
    private val repository: PdfWorkspaceRepository = ZoteroWorkspaceRepository(),
    private val markdownConverter: PdfMarkdownConverter = MistralOcrConverter(),
    private val markdownStore: PdfMarkdownStore = defaultPdfMarkdownStore(),
    private val mistralApiKey: suspend () -> String = { createApiKeysStore().apiKeys.first().mistral },
    private val readingAgent: () -> ResearchAgent = ::createReadingAgent,
    private val syncDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val usageRecorder: suspend (String?, AiUsageEvent) -> Unit = { _, _ -> },
    private val openDocument: suspend (ByteArray) -> PdfDocument = ::openPdfDocument,
) : ViewModel() {
    var state by mutableStateOf(PdfWorkspaceState())
        private set
    private var config: ZoteroConfig? = null
    private var document: PdfDocument? = null
    private var session = 0
    private var loadJob: Job? = null
    private var renderJob: Job? = null
    private var syncJob: Job? = null
    private val syncMutex = Mutex()
    private class PendingWrite(
        val overlay: (List<ZoteroAnnotation>) -> List<ZoteroAnnotation>,
        val action: suspend (ZoteroConfig, List<ZoteroAnnotation>) -> Unit,
        var saved: Boolean = false,
    )
    private val pendingWrites = mutableListOf<PendingWrite>()
    private var remoteAnnotations = emptyList<ZoteroAnnotation>()
    private var markdownJob: Job? = null
    private var readingJob: Job? = null
    private var pdfBytes: ByteArray? = null
    private var renderScale = 1.5f
    private val continuousPages = mutableMapOf<Pair<Int, Int>, PdfPage>()
    private val pageTexts = mutableMapOf<Int, PdfPageText>()

    suspend fun loadContinuousPage(index: Int, widthPixels: Int): PdfPage? {
        val pdf = document ?: return null
        val token = session
        val key = index to widthPixels
        continuousPages[key]?.let { return it }
        return try {
            val initial = pdf.render(index, 1f)
            val scale = minOf(widthPixels / initial.geometry.displayWidth, 8f,
                4096f / maxOf(initial.geometry.displayWidth, initial.geometry.displayHeight))
            val page = if (kotlin.math.abs(initial.image.width - widthPixels) < 32) initial else pdf.render(index, scale)
            currentCoroutineContext().ensureActive()
            if (token != session) null else {
                if (continuousPages.size >= 6) continuousPages.remove(continuousPages.keys.first())
                continuousPages[key] = page
                state = state.copy(pageGeometries = state.pageGeometries + (index to page.geometry))
                page
            }
        } catch (exception: Exception) {
            if (exception is CancellationException) throw exception
            if (token == session) report(exception, "Could not render page ${index + 1}")
            null
        }
    }

    fun activateContinuousPage(index: Int, page: PdfPage) {
        if (state.draft != null || index == state.pageIndex && state.page != null) return
        renderJob?.cancel()
        state = state.copy(pageIndex = index, page = page, rendering = false, textSelection = null)
    }

    /** Stable per-user, per-attachment key for data saved alongside the open paper (Markdown, agent chats). */
    fun paperStorageKey(): String? {
        state.localStorageKey?.let { return it }
        val account = config ?: return null
        val target = state.target ?: return null
        return paperStorageKey(account, target)
    }

    private fun paperStorageKey(account: ZoteroConfig, target: ZoteroPdfTarget) =
        "${account.userId}/${account.groupId?.let { "groups/$it/" }.orEmpty()}${target.attachment.key}"

    fun updateConfig(value: ZoteroConfig?): Boolean {
        if (config == value) return false
        close()
        config = value
        return true
    }

    fun open(target: ZoteroPdfTarget) {
        close()
        val account = config
        state = PdfWorkspaceState(target = target, loading = account != null, needsRefresh = true,
            error = if (account == null) "Configure Zotero before opening a PDF." else null)
        if (account == null) return
        val token = session
        loadJob = viewModelScope.launch {
            var opened: PdfDocument? = null
            try {
                val bytes = repository.download(account, target.attachment)
                if (token == session) pdfBytes = bytes
                opened = openDocument(bytes)
                ensureActive()
                if (token != session) return@launch
                document = opened
                opened = null
                state = state.copy(pageCount = document!!.pageCount, loading = false)
                renderPage()
                refreshAnnotations()
            } catch (exception: Exception) {
                if (exception is CancellationException) throw exception
                if (token == session) {
                    report(exception, "Could not open PDF")
                    state = state.copy(loading = false)
                }

            } finally {
                opened?.close()
            }
        }
    }

    /** Local files use the same renderer and text geometry, without Zotero annotation writes. */
    fun openLocal(bytes: ByteArray, title: String, storageKey: String? = null) {
        close()
        pdfBytes = bytes
        state = PdfWorkspaceState(localTitle = title, localStorageKey = storageKey, loading = true)
        val token = session
        loadJob = viewModelScope.launch {
            var opened: PdfDocument? = null
            try {
                opened = openDocument(bytes)
                ensureActive()
                if (token != session) return@launch
                document = opened
                opened = null
                state = state.copy(pageCount = requireNotNull(document).pageCount, loading = false)
                renderPage()
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                if (token == session) {
                    report(exception, "Could not open local PDF")
                    state = state.copy(loading = false)
                }
            } finally {
                opened?.close()
            }
        }
    }

    fun retryOpen() {
        val target = state.target
        val title = state.localTitle
        val bytes = pdfBytes
        when {
            target != null -> open(target)
            title != null && bytes != null -> openLocal(bytes, title, state.localStorageKey)
            else -> state = state.copy(error = "The PDF is no longer loaded. Close the workspace and reopen the item.")
        }
    }

    fun close() {
        session++
        loadJob?.cancel()
        renderJob?.cancel()
        syncJob?.cancel()
        pendingWrites.clear()
        remoteAnnotations = emptyList()
        markdownJob?.cancel()
        readingJob?.cancel()
        document?.close()
        document = null
        pdfBytes = null
        continuousPages.clear()
        pageTexts.clear()
        renderScale = 1.5f
        state = PdfWorkspaceState()
    }

    /** Shows the hidden Markdown version, converting with Mistral OCR only when none is saved yet. */
    fun showMarkdown() {
        if (paperStorageKey() == null || state.draft != null) return
        state = state.copy(viewMode = ReaderViewMode.Markdown, selectedKey = null, textSelection = null)
        if (state.markdown.content == null && markdownJob?.isActive != true) loadMarkdown(force = false)
    }

    fun showPdf() {
        state = state.copy(viewMode = ReaderViewMode.Pdf)
        if (state.pageIndex >= state.pageCount && state.pageCount > 0) goToPage(state.pageCount - 1)
        else if (state.page == null && state.pageCount > 0) renderPage()
    }

    /** Loads (or converts once) the OCR Markdown used as the read-aloud script, without retrying after errors. */
    fun ensureMarkdown() {
        val markdown = state.markdown
        if (markdown.content == null && !markdown.busy && markdown.error == null && markdownJob?.isActive != true) {
            loadMarkdown(force = false)
        }
    }

    fun retryMarkdown() {
        if (markdownJob?.isActive != true) loadMarkdown(force = false)
    }

    fun regenerateMarkdown() {
        if (markdownJob?.isActive != true) loadMarkdown(force = true)
    }

    /**
     * Prepares the reading script once per paper: OCR blocks are classified by the AI assistant (OpenAI, or
     * Mistral as fallback) into readable article text versus headers, footers, authors, references…
     * Read mode falls back to the OCR Markdown heuristics while this runs or when no model is configured.
     */
    fun ensureReadingScript() {
        val reading = state.readingScript
        if (reading.content != null || reading.preparing || reading.error != null || readingJob?.isActive == true) return
        prepareReadingScript(force = false)
    }

    /** Re-runs the AI cleanup, e.g. after the Markdown was regenerated with positioned OCR blocks. */
    fun regenerateReadingScript() {
        readingJob?.cancel()
        state = state.copy(readingScript = ReadingScriptState())
        prepareReadingScript(force = true)
    }

    private fun prepareReadingScript(force: Boolean) {
        val pages = state.markdown.pages.takeIf { state.markdown.hasPageBoundaries && it.isNotEmpty() } ?: return
        val key = paperStorageKey() ?: return
        val token = session
        state = state.copy(readingScript = ReadingScriptState(preparing = true))
        readingJob = viewModelScope.launch {
            fun update(value: ReadingScriptState) {
                if (token == session) state = state.copy(readingScript = value)
            }
            try {
                val saved = if (force) null else markdownStore.loadReadingScript(key)?.takeIf { parseReadingScript(it).isNotEmpty() }
                if (saved != null) return@launch update(ReadingScriptState(content = saved))
                val candidates = readingCandidates(markdownStore.loadLayout(key), pages)
                val cleaned = ReadingScriptCleaner(readingAgent()).clean(candidates)
                val script = renderReadingScript(cleaned)
                ensureActive()
                if (token != session) return@launch
                markdownStore.saveReadingScript(key, script)
                usageRecorder(
                    usagePaperId(state.target),
                    AiUsageEvent(
                        service = AiUsageService.READING_AI,
                        timestamp = kotlinx.datetime.Clock.System.now().toEpochMilliseconds(),
                        inputUnits = candidates.sumOf { it.text.length.toLong() } / 4L,
                        outputUnits = script.length.toLong() / 4L,
                        model = "gpt-6-luna",
                    )
                )
                update(ReadingScriptState(content = script))
            } catch (exception: Exception) {
                if (exception is CancellationException) throw exception
                Napier.w("Could not prepare the reading script", exception)
                update(ReadingScriptState(error = exception.message ?: "Could not prepare the reading script"))
            }
        }
    }

    private fun loadMarkdown(force: Boolean) {
        val target = state.target
        val account = config
        val token = session
        val key = paperStorageKey() ?: return
        state = state.copy(markdown = state.markdown.copy(loading = true, converting = false, error = null, missingApiKey = false))
        markdownJob = viewModelScope.launch {
            fun update(block: PdfMarkdownState.() -> PdfMarkdownState) {
                if (token == session) state = state.copy(markdown = state.markdown.block())
            }
            try {
                val saved = if (force) null else markdownStore.load(key)
                if (saved != null) {
                    if (token == session && state.markdown.content != saved) {
                        readingJob?.cancel()
                        state = state.copy(readingScript = ReadingScriptState())
                    }
                    update { PdfMarkdownState(content = saved, syncError = markdownStore.syncError) }
                    alignMarkdownPage()
                    return@launch
                }
                val apiKey = mistralApiKey().trim()
                if (apiKey.isEmpty()) {
                    update { copy(loading = false, missingApiKey = true,
                        error = "Add your Mistral API key in Settings → API keys to create a Markdown version of this PDF.") }
                    return@launch
                }
                update { copy(loading = false, converting = true) }
                val bytes = pdfBytes ?: repository.download(
                    requireNotNull(account) { "No local PDF is loaded and Zotero is not configured." },
                    requireNotNull(target) { "No PDF is open." }.attachment,
                ).also { if (token == session) pdfBytes = it }
                val converted = markdownConverter.convert(bytes, target?.attachment?.data?.filename ?: "paper.pdf", apiKey)
                ensureActive()
                if (token != session) return@launch
                val markdown = markdownStore.save(key, converted)
                usageRecorder(
                    usagePaperId(target),
                    AiUsageEvent(
                        service = AiUsageService.OCR,
                        timestamp = kotlinx.datetime.Clock.System.now().toEpochMilliseconds(),
                        inputUnits = converted.layout?.pages?.size?.toLong()
                            ?: markdownPages(converted.markdown).size.toLong(),
                        model = converted.layout?.model ?: MISTRAL_OCR_MODEL,
                    )
                )
                readingJob?.cancel()
                update { PdfMarkdownState(content = markdown, syncError = markdownStore.syncError) }
                if (token == session) state = state.copy(readingScript = ReadingScriptState())
                alignMarkdownPage()
                if (force) state = state.copy(message = "Markdown version regenerated")
            } catch (exception: Exception) {
                if (exception is CancellationException) throw exception
                Napier.e("Could not create Markdown version", exception)
                update { copy(loading = false, converting = false,
                    error = (exception as? PdfMarkdownException)?.message ?: "Could not create the Markdown version: ${exception.message ?: "unknown error"}") }
            }

        }
    }

    private fun alignMarkdownPage() {
        if (state.viewMode == ReaderViewMode.Markdown && state.readerPageCount > 0 &&
            state.pageIndex >= state.readerPageCount) goToPage(state.readerPageCount - 1)
    }

    fun goToPage(index: Int, fromScroll: Boolean = false) {
        if (index !in 0 until state.readerPageCount || state.draft != null) return
        if (index == state.pageIndex && (state.page != null || fromScroll)) return
        if (state.viewMode == ReaderViewMode.Markdown) renderJob?.cancel()
        state = state.copy(pageIndex = index, page = null, selectedKey = null, textSelection = null,
            rendering = if (state.viewMode == ReaderViewMode.Markdown) false else state.rendering,
            readingPosition = PdfReadingPosition(index), scrollSource = PdfScrollSource.Navigation,
            pageNavigationId = state.pageNavigationId + if (fromScroll) 0 else 1)
        if (state.viewMode == ReaderViewMode.Pdf) renderPage()
    }

    fun trackReadingPosition(position: PdfReadingPosition, source: PdfScrollSource) {
        if (state.draft != null || position.pageIndex !in 0 until state.readerPageCount) return
        require(position.fraction.isFinite())
        val bounded = position.copy(fraction = position.fraction.coerceIn(0f, 1f))
        if (bounded == state.readingPosition && source == state.scrollSource) return
        goToPage(bounded.pageIndex, fromScroll = true)
        state = state.copy(readingPosition = bounded, scrollSource = source)
    }

    fun navigateToPosition(position: PdfReadingPosition) {
        if (state.draft != null || position.pageIndex !in 0 until state.pageCount) return
        trackReadingPosition(position, PdfScrollSource.Sidebar)
        state = state.copy(pageNavigationId = state.pageNavigationId + 1)
    }

    fun recordDisplayedPageWidth(width: Float) {
        if (width > 0f && state.displayedPageWidth != width) state = state.copy(displayedPageWidth = width)
    }

    fun zoom(value: Float) {
        state = state.copy(zoom = value.coerceIn(0.5f, 3f))
    }

    fun ensurePageResolution(widthPixels: Int) {
        val page = state.page ?: return
        if (widthPixels <= 0 || state.rendering) return
        val scale = minOf(widthPixels / page.geometry.displayWidth, 8f,
            4096f / maxOf(page.geometry.displayWidth, page.geometry.displayHeight))
        val targetWidth = (page.geometry.displayWidth * scale).toInt()
        if (kotlin.math.abs(page.image.width - targetWidth) < 32) return
        renderScale = scale
        renderPage()
    }

    private fun renderPage() {
        val pdf = document ?: return
        val token = session
        val index = state.pageIndex
        val scale = renderScale
        renderJob?.cancel()
        state = state.copy(rendering = true)
        renderJob = viewModelScope.launch {
            try {
                val page = pdf.render(index, scale)
                ensureActive()
                if (token == session) state = state.copy(page = page, rendering = false,
                    pageGeometries = state.pageGeometries + (index to page.geometry))
            } catch (exception: Exception) {
                if (exception is CancellationException) throw exception
                if (token == session) {
                    report(exception, "Could not render page")
                    state = state.copy(rendering = false)
                }
            }
        }
    }

    fun refreshAnnotations() {
        val account = config ?: return
        val target = state.target ?: return
        if (state.syncing || state.loading) return
        val token = session
        state = state.copy(syncing = true, error = null)
        syncJob = viewModelScope.launch {
            syncMutex.withLock {
                try {
                    val annotations = withContext(syncDispatcher) {
                        validatedAnnotations(repository.annotations(account, target.attachment.key))
                    }
                    if (token != session) return@withLock
                    remoteAnnotations = annotations
                    val draft = state.draft
                    val visible = visibleAnnotations()
                    val current = draft?.editing?.let { previous -> visible.find { it.key == previous.key } }
                    val deleted = draft?.editing != null && current == null
                    state = state.copy(annotations = visible, needsRefresh = false,
                        draft = if (current != null) draft.copy(editing = current) else draft,
                        error = if (deleted) "This annotation was deleted in Zotero. Copy your draft, cancel it, then create a new annotation." else null)
                    drainWrites(account, target, token)
                } catch (exception: Exception) {
                    if (exception is CancellationException) throw exception
                    if (token == session) {
                        report(exception, "Could not synchronize Zotero annotations; local changes are retained")
                        state = state.copy(syncing = false, needsRefresh = true)
                    }
                }
            }
        }
    }

    private fun visibleAnnotations(): List<ZoteroAnnotation> =
        pendingWrites.fold(remoteAnnotations) { annotations, pending -> pending.overlay(annotations) }
            .sortedWith(compareBy({ it.position.pageIndex }, { it.data["annotationSortIndex"].toString() }))

    private fun currentAnnotation(annotations: List<ZoteroAnnotation>, key: String): ZoteroAnnotation =
        annotations.find { it.key == key } ?: error("Annotation $key was deleted in Zotero. Your local change is retained.")

    private fun replaceAnnotation(annotation: ZoteroAnnotation): (List<ZoteroAnnotation>) -> List<ZoteroAnnotation> =
        { annotations -> annotations.filterNot { it.key == annotation.key } + annotation }

    private suspend fun drainWrites(account: ZoteroConfig, target: ZoteroPdfTarget, token: Int) {
        while (token == session && pendingWrites.isNotEmpty()) {
            val pending = pendingWrites.first()
            if (!pending.saved) {
                val snapshot = remoteAnnotations
                withContext(syncDispatcher) { pending.action(account, snapshot) }
                if (token != session) return
                pending.saved = true
            }
            val annotations = withContext(syncDispatcher) {
                validatedAnnotations(repository.annotations(account, target.attachment.key))
            }
            if (token != session) return
            remoteAnnotations = annotations
            pendingWrites.removeAt(0)
            state = state.copy(annotations = visibleAnnotations(), pendingWrites = pendingWrites.size)
        }
        if (token == session) state = state.copy(syncing = false, needsRefresh = false)
    }

    private fun validatedAnnotations(values: List<ZoteroAnnotation>): List<ZoteroAnnotation> {
        values.forEach { annotation ->
            val position = annotation.position
            require(position.pageIndex >= 0) { "Invalid Zotero annotation page." }
            require(position.rects.all { it.size == 4 && it.all(Float::isFinite) }) { "Invalid Zotero annotation rectangle." }
            require(position.paths.all { path -> path.size >= 2 && path.size % 2 == 0 && path.all(Float::isFinite) }) {
                "Invalid Zotero ink path."
            }
            require(position.width.isFinite() && position.width > 0f) { "Invalid Zotero ink width." }
            require(annotation.color.matches(Regex("#[0-9a-fA-F]{6}"))) { "Invalid Zotero annotation color." }
        }
        return values.sortedWith(compareBy({ it.position.pageIndex }, { it.data["annotationSortIndex"].toString() }))
    }

    fun beginHighlight(selection: Rect) {
        val page = state.page ?: return
        if (!state.canWrite || state.draft != null) return
        val glyphs = page.glyphs.filter { selection.contains(it.bounds.center) }
        if (glyphs.isEmpty()) {
            state = state.copy(error = if (page.glyphs.isEmpty()) missingPdfTextMessage()
                else "No selectable PDF text in this area. Select an area containing text, or add a page note.")
            return
        }
        state = state.copy(textSelection = page.selectionFor(glyphs))
        highlightSelectedText()
    }

    fun selectText(start: Offset, end: Offset) {
        val page = state.page ?: return
        if (state.draft != null && !state.adjustingTextRange) return
        updateTextSelection(page, page.selectText(start, end))
    }

    fun selectWord(point: Offset) {
        val page = state.page ?: return
        if (state.draft != null) return
        updateTextSelection(page, page.selectWord(point))
    }

    private fun updateTextSelection(page: PdfPage, selection: PdfTextSelection?) {
        state = state.copy(textSelection = selection, selectedKey = null,
            draft = if (state.adjustingTextRange && selection != null) state.draft?.copy(
                rects = selection.rects.map(page.geometry::pdfRect), text = selection.text) else state.draft,
            error = if (selection == null) missingPdfTextMessage() else state.error)
    }

    fun clearTextSelection() { state = state.copy(textSelection = null) }

    fun selectAllText() {
        val page = state.page ?: return
        if (state.draft != null) return
        if (page.glyphs.isEmpty()) {
            state = state.copy(error = missingPdfTextMessage())
            return
        }
        state = state.copy(textSelection = page.selectionFor(page.glyphs), selectedKey = null)
    }

    private fun missingPdfTextMessage(): String =
        "This page has no selectable PDF text. " +
            (if (state.markdown.content != null) "Your OCR Markdown is available for reading and copying in Markdown view. "
            else "Use Markdown view to load or create an OCR copy for reading and copying. ") +
            "OCR Markdown does not add a selectable text layer to the PDF; page notes are still available."

    fun highlightSelectedText() = annotateSelectedText("highlight")

    fun underlineSelectedText() = annotateSelectedText("underline")

    private fun annotateSelectedText(type: String) {
        val page = state.page ?: return
        val selection = state.textSelection ?: return
        if (!state.canWrite || state.draft != null || selection.rects.isEmpty()) return
        state = state.copy(draft = AnnotationDraft(type, state.pageIndex, selection.rects.map(page.geometry::pdfRect),
            text = selection.text, color = state.annotationColor), error = null, message = null)
    }

    fun setAnnotationColor(color: String) {
        require(color.matches(Regex("#[0-9a-fA-F]{6}"))) { "Invalid annotation color." }
        state = state.copy(annotationColor = color,
            draft = state.draft?.takeIf { it.type == "ink" }?.copy(color = color) ?: state.draft)
    }

    fun setInkWidth(width: Float) {
        require(width.isFinite() && width > 0f) { "Invalid pen width." }
        state = state.copy(inkWidth = width,
            draft = state.draft?.takeIf { it.type == "ink" && it.editing == null }?.copy(width = width) ?: state.draft)
    }

    fun setTextSize(size: Float) {
        require(size.isFinite() && size > 0f) { "Invalid text size." }
        state = state.copy(textSize = size,
            draft = state.draft?.takeIf { it.type == "text" && it.editing == null }?.copy(fontSize = size) ?: state.draft)
    }

    fun beginText(point: Offset) {
        val page = state.page ?: return
        if (!state.canWrite || state.draft != null) return
        val rect = textBoxRect(point.x, point.y, "", state.textSize, page.geometry.displayWidth)
        state = state.copy(draft = AnnotationDraft("text", state.pageIndex, listOf(page.geometry.pdfRect(rect)),
            color = state.annotationColor, fontSize = state.textSize), textSelection = null, error = null, message = null)
    }

    fun beginRectangle(rect: Rect) {
        val page = state.page ?: return
        if (!state.canWrite || state.draft != null || rect.width < 2f || rect.height < 2f) return
        state = state.copy(draft = AnnotationDraft("image", state.pageIndex, listOf(page.geometry.pdfRect(rect)),
            color = state.annotationColor), textSelection = null, error = null, message = null)
    }

    private fun fittedTextRects(draft: AnnotationDraft): List<List<Float>> {
        val geometry = state.pageGeometries[draft.pageIndex] ?: return draft.rects
        val current = geometry.displayRect(draft.rects.first())
        return listOf(geometry.pdfRect(textBoxRect(current.left, current.top, draft.comment, draft.fontSize, geometry.displayWidth)))
    }

    fun resizeAnnotation(annotation: ZoteroAnnotation, larger: Boolean) {
        if (!state.canWrite || state.draft != null) return
        val position = when (annotation.type) {
            "ink" -> {
                val width = nextPreset(inkWidthPresets, annotation.position.width, larger) ?: return
                val base = if (annotation.needsInkPositionRepair) repairedInkPosition(annotation) else annotation.positionData
                JsonObject(base + ("width" to JsonPrimitive(width)))
            }
            "text" -> {
                val size = nextPreset(textSizePresets, annotation.position.fontSize, larger) ?: return
                val draft = AnnotationDraft("text", annotation.position.pageIndex, annotation.position.rects,
                    comment = annotation.comment, fontSize = size)
                JsonObject(annotation.positionData + mapOf("fontSize" to JsonPrimitive(size),
                    "rects" to JsonArray(fittedTextRects(draft).map { JsonArray(it.map(::JsonPrimitive)) })))
            }
            else -> return
        }
        val local = annotation.copy(data = JsonObject(annotation.data + ("annotationPosition" to JsonPrimitive(position.toString()))))
        write(replaceAnnotation(local)) { account, annotations ->
            val current = currentAnnotation(annotations, annotation.key)
            repository.updatePosition(account, current, current.comment, position)
        }
    }

    fun transformAnnotation(annotation: ZoteroAnnotation, pageIndex: Int, from: Rect, to: Rect) {
        if (!state.canWrite || state.draft != null || annotation.type !in movableAnnotationTypes) return
        if (annotation.position.pageIndex != pageIndex || annotation.position.nextPageRects.isNotEmpty()) return
        val geometry = state.pageGeometries[pageIndex] ?: return
        if (from.width <= 0f || from.height <= 0f || to.width <= 0f || to.height <= 0f || to == from) return
        if (listOf(to.left, to.top, to.right, to.bottom).any { !it.isFinite() }) return
        fun map(point: Offset) = Offset(to.left + (point.x - from.left) * to.width / from.width,
            to.top + (point.y - from.top) * to.height / from.height)
        val position = if (annotation.type == "ink") {
            val base = if (annotation.needsInkPositionRepair) repairedInkPosition(annotation) else annotation.positionData
            JsonObject(base + ("paths" to JsonArray(annotation.position.paths.map { path ->
                JsonArray(path.chunked(2).flatMap { pair ->
                    val point = geometry.toPdf(map(geometry.toDisplay(Offset(pair[0], pair[1]))))
                    listOf(JsonPrimitive(point.x), JsonPrimitive(point.y))
                })
            })))
        } else JsonObject(annotation.positionData + ("rects" to JsonArray(annotation.position.rects.map { rect ->
            val display = geometry.displayRect(rect)
            JsonArray(geometry.pdfRect(Rect(map(display.topLeft), map(display.bottomRight))).map(::JsonPrimitive))
        })))
        val local = annotation.copy(data = JsonObject(annotation.data + ("annotationPosition" to JsonPrimitive(position.toString()))))
        write(replaceAnnotation(local)) { account, annotations ->
            val current = currentAnnotation(annotations, annotation.key)
            repository.updatePosition(account, current, current.comment, position)
        }
    }

    fun adjustTextRange() {
        val draft = state.draft ?: return
        if (!state.canWrite || draft.type !in listOf("highlight", "underline")) return
        if (draft.editing?.position?.nextPageRects?.isNotEmpty() == true) {
            state = state.copy(error = "This annotation spans two pages. Adjust its range in Zotero to preserve both pages.")
            return
        }
        val page = state.page ?: return
        state = state.copy(adjustingTextRange = true,
            textSelection = PdfTextSelection(draft.text, draft.rects.map(page.geometry::displayRect)))
    }

    fun finishTextRange() { state = state.copy(adjustingTextRange = false) }

    fun dismissMessage() { state = state.copy(message = null) }

    fun beginNote(rect: Rect? = null) {
        val page = state.page ?: return
        if (!state.canWrite || state.draft != null) return
        val position = rect ?: Rect(24f, 24f, 46f, 46f)
        state = state.copy(draft = AnnotationDraft("note", state.pageIndex, listOf(page.geometry.pdfRect(position)),
            color = state.annotationColor), textSelection = null,
            error = null, message = null)
    }

    fun beginIdea(rect: Rect) {
        if (state.draft != null) return
        beginNote(rect)
        state.draft?.takeIf { it.editing == null && it.type == "note" }?.let {
            state = state.copy(draft = it.copy(isIdea = true))
        }
    }

    fun addInkPath(points: List<Offset>, color: String) {
        val page = state.page ?: return
        if (!state.canWrite || points.size < 2 || state.draft?.type?.let { it != "ink" } == true) return
        require(color.matches(Regex("#[0-9a-fA-F]{6}"))) { "Invalid annotation color." }
        val pdfPoints = points.map(page.geometry::toPdf)
        if (pdfPoints.any { !it.x.isFinite() || !it.y.isFinite() }) return
        val path = pdfPoints.flatMap { listOf(it.x, it.y) }
        val allPoints = (state.draft?.paths.orEmpty() + listOf(path)).flatMap { it.chunked(2) }
        val left = allPoints.minOf { it[0] }
        val bottom = allPoints.minOf { it[1] }
        val right = maxOf(allPoints.maxOf { it[0] }, left + 0.01f)
        val top = maxOf(allPoints.maxOf { it[1] }, bottom + 0.01f)
        val rect = listOf(left, bottom, right, top)
        val currentDraft = state.draft
        state = state.copy(
            draft = if (currentDraft == null) AnnotationDraft(
                type = "ink",
                pageIndex = state.pageIndex,
                rects = listOf(rect),
                color = color,
                width = state.inkWidth,
                paths = listOf(path)
            ) else currentDraft.copy(
                rects = listOf(rect),
                paths = currentDraft.paths + listOf(path)
            ),
            textSelection = null,
            error = null,
            message = null
        )
    }

    fun selectAnnotation(annotation: ZoteroAnnotation, pageIndex: Int = annotation.position.pageIndex, reveal: Boolean = true) {
        if (state.draft != null) return
        if (!reveal) {
            state = state.copy(selectedKey = annotation.key, textSelection = null)
            return
        }
        goToPage(pageIndex)
        val geometry = state.pageGeometries[pageIndex]
        val rects = if (pageIndex == annotation.position.pageIndex) annotation.position.boundingRects else annotation.position.nextPageRects
        val fraction = if (geometry != null) rects.minOfOrNull { geometry.displayRect(it).top / geometry.displayHeight } ?: 0f else 0f
        state = state.copy(selectedKey = annotation.key, textSelection = null,
            readingPosition = PdfReadingPosition(pageIndex, (fraction - 0.04f).coerceAtLeast(0f)),
            scrollSource = PdfScrollSource.Navigation, pageNavigationId = state.pageNavigationId + 1)
    }

    fun clearSelectedAnnotation() {
        if (state.draft == null && state.selectedKey != null) state = state.copy(selectedKey = null)
    }

    fun adjustAnnotationRange(annotation: ZoteroAnnotation) {
        if (!state.canWrite || state.draft != null || annotation.type !in listOf("highlight", "underline")) return
        if (state.page == null || state.pageIndex != annotation.position.pageIndex) {
            state = state.copy(error = "Open page ${annotation.position.pageIndex + 1} to adjust this text range.")
            return
        }
        editAnnotation(annotation)
        adjustTextRange()
        if (!state.adjustingTextRange) state = state.copy(draft = null)
    }

    fun editAnnotation(annotation: ZoteroAnnotation) {
        if (!state.canWrite || state.draft != null) return
        selectAnnotation(annotation)
        state = state.copy(draft = AnnotationDraft(annotation.type, annotation.position.pageIndex,
            annotation.position.boundingRects, annotation.text, annotation.comment, annotation.color,
            paths = annotation.position.paths, width = annotation.position.width, fontSize = annotation.position.fontSize, editing = annotation, isIdea = annotation.isIdea))
    }

    fun changeDraft(comment: String, color: String) {
        state = state.copy(draft = state.draft?.copy(comment = comment, color = color))
    }

    fun cancelDraft() {
        state = state.copy(draft = null, adjustingTextRange = false, textSelection = null)
    }

    fun saveDraft() {
        val draft = state.draft ?: return
        if (!state.canWrite || (draft.type in listOf("note", "text") && draft.comment.isBlank()) ||
            (draft.type == "ink" && draft.paths.isEmpty())) return
        val attachment = state.target?.attachment?.key ?: return
        val fitted = if (draft.type == "text") draft.copy(rects = fittedTextRects(draft)) else draft
        val payload = annotationPayload(attachment, fitted)
        val key = draft.editing?.key ?: draft.key
        val rangeChanged = draft.editing != null && draft.type in listOf("highlight", "underline") &&
            (draft.rects != draft.editing.position.rects || draft.text != draft.editing.text)
        val textChanged = draft.editing != null && draft.type == "text" && draft.comment != draft.editing.comment
        val edits = buildMap {
            put("annotationComment", JsonPrimitive(draft.comment))
            put("annotationColor", JsonPrimitive(draft.color))
            if (rangeChanged && draft.editing != null) {
                put("annotationText", JsonPrimitive(draft.text))
                val position = JsonObject(draft.editing.positionData - "nextPageRects" +
                    Json.parseToJsonElement(payload.getValue("annotationPosition").jsonPrimitive.content).jsonObject)
                put("annotationPosition", JsonPrimitive(position.toString()))
            } else if (textChanged && draft.editing != null) {
                val position = JsonObject(draft.editing.positionData +
                    ("rects" to JsonArray(fitted.rects.map { rect -> JsonArray(rect.map(::JsonPrimitive)) })))
                put("annotationPosition", JsonPrimitive(position.toString()))
            }
        }
        val local = ZoteroAnnotation(key, draft.editing?.version ?: 0,
            if (draft.editing == null) payload else JsonObject(draft.editing.data + edits))
        write(replaceAnnotation(local)) { account, annotations ->
            if (draft.editing == null) {
                // A lost create response may already have committed this stable Zotero key.
                if (annotations.none { it.key == key }) repository.create(account, payload)
            } else {
                val current = currentAnnotation(annotations, key)
                when {
                    textChanged -> {
                        repository.updatePosition(account, current, draft.comment, JsonObject(current.positionData +
                            ("rects" to JsonArray(fitted.rects.map { r -> JsonArray(r.map(::JsonPrimitive)) }))))
                        if (draft.color != current.color) {
                            val fresh = repository.annotations(account, attachment).first { it.key == key }
                            repository.update(account, fresh, draft.comment, draft.color)
                        }
                    }
                    rangeChanged -> repository.updateTextRange(account, current, draft)
                    else -> repository.update(account, current, draft.comment, draft.color)
                }
            }
        }
        state = state.copy(draft = null, textSelection = null, adjustingTextRange = false)
    }

    fun recolorAnnotation(annotation: ZoteroAnnotation, color: String) {
        require(color.matches(Regex("#[0-9a-fA-F]{6}"))) { "Invalid annotation color." }
        if (!state.canWrite || state.draft != null || annotation.color.equals(color, true)) return
        val local = annotation.copy(data = JsonObject(annotation.data + ("annotationColor" to JsonPrimitive(color))))
        write(replaceAnnotation(local)) { account, annotations ->
            val current = currentAnnotation(annotations, annotation.key)
            repository.update(account, current, current.comment, color)
        }
    }

    fun deleteAnnotation(annotation: ZoteroAnnotation) {
        if (!state.canWrite || state.draft != null) return
        write({ annotations -> annotations.filterNot { it.key == annotation.key } }) { account, annotations ->
            annotations.find { it.key == annotation.key }?.let { repository.delete(account, it) }
        }
    }

    fun repairInkAnnotations() {
        if (!state.canWrite || state.draft != null) return
        val broken = state.annotations.filter { it.needsInkPositionRepair }
        if (broken.isEmpty()) return
        write({ annotations -> annotations.map { annotation ->
            if (annotation.needsInkPositionRepair && broken.any { it.key == annotation.key }) annotation.copy(data = JsonObject(annotation.data +
                ("annotationPosition" to JsonPrimitive(repairedInkPosition(annotation).toString())))) else annotation
        } }) { account, annotations ->
            broken.forEach { annotation ->
                val current = currentAnnotation(annotations, annotation.key)
                if (current.needsInkPositionRepair) repository.repairInkPosition(account, current)
            }
        }
    }

    private fun write(
        overlay: (List<ZoteroAnnotation>) -> List<ZoteroAnnotation>,
        action: suspend (ZoteroConfig, List<ZoteroAnnotation>) -> Unit,
    ) {
        val account = config ?: return
        val target = state.target ?: return
        val token = session
        pendingWrites += PendingWrite(overlay, action)
        state = state.copy(annotations = visibleAnnotations(), pendingWrites = pendingWrites.size, message = null)
        if (state.syncing || state.needsRefresh) return
        state = state.copy(syncing = true, error = null)
        syncJob = viewModelScope.launch {
            syncMutex.withLock {
                try {
                    drainWrites(account, target, token)
                } catch (exception: Exception) {
                    if (exception is CancellationException) throw exception
                    if (token == session) {
                        report(exception, "Zotero sync failed; your local changes are retained")
                        state = state.copy(syncing = false, needsRefresh = true)
                    }
                }
            }
        }
    }

    /** Loads or creates the hidden Markdown version without switching the reader; the agent only reads Markdown. */
    suspend fun awaitMarkdown(): PdfMarkdownState {
        if (paperStorageKey() == null) return state.markdown
        if (state.markdown.content == null && markdownJob?.isActive != true) loadMarkdown(force = false)
        markdownJob?.join()
        return state.markdown
    }

    /** Text layer of any page, used to anchor agent annotations on pages that are not currently rendered. */
    suspend fun pageText(index: Int): PdfPageText? {
        if (index !in 0 until state.pageCount) return null
        state.page?.takeIf { state.pageIndex == index }?.let { return PdfPageText(index, it.geometry, it.glyphs) }
        pageTexts[index]?.let { return it }
        val pdf = document ?: return null
        val token = session
        val page = pdf.render(index, 0.2f)
        if (token != session) return null
        if (pageTexts.size >= 64) pageTexts.clear()
        return PdfPageText(index, page.geometry, page.glyphs).also {
            pageTexts[index] = it
            state = state.copy(pageGeometries = state.pageGeometries + (index to page.geometry))
        }
    }

    /** Creates annotations on the agent's behalf without touching the user's in-progress draft. */
    suspend fun agentCreateAnnotations(drafts: List<AnnotationDraft>, reveal: Boolean = true): List<String> {
        if (drafts.isEmpty()) return emptyList()
        val attachment = state.target?.attachment?.key ?: error("No PDF is open.")
        val payloads = drafts.map { draft ->
            annotationPayload(attachment, draft.copy(aiGenerated = true,
                rects = if (draft.type == "text") fittedTextRects(draft) else draft.rects))
        }
        val keys = agentWrite { account -> payloads.map { repository.create(account, it) } }
        if (reveal && state.viewMode == ReaderViewMode.Pdf) state.annotations.find { it.key == keys.first() }?.let {
            if (state.draft == null) selectAnnotation(it)
        }
        return keys
    }

    suspend fun agentUpdateAnnotation(key: String, comment: String?, color: String?) {
        check(state.annotations.any { it.key == key }) { "No annotation with key $key." }
        check(state.draft?.editing?.key != key) { "The user is editing this annotation right now." }
        color?.let { require(it.matches(Regex("#[0-9a-fA-F]{6}"))) { "Invalid annotation color." } }
        agentWrite { account ->
            check(state.draft?.editing?.key != key) { "The user is editing this annotation right now." }
            val current = currentAnnotation(remoteAnnotations, key)
            val newComment = comment ?: current.comment
            val newColor = color ?: current.color
            if (current.type == "text" && newComment != current.comment) {
                require(newComment.isNotBlank()) { "Text annotations cannot be empty." }
                val draft = AnnotationDraft("text", current.position.pageIndex, current.position.rects,
                    comment = newComment, fontSize = current.position.fontSize)
                repository.updatePosition(account, current, newComment, JsonObject(current.positionData +
                    ("rects" to JsonArray(fittedTextRects(draft).map { r -> JsonArray(r.map(::JsonPrimitive)) }))))
                if (!newColor.equals(current.color, true)) {
                    val fresh = repository.annotations(account, state.target!!.attachment.key).first { it.key == key }
                    repository.update(account, fresh, newComment, newColor)
                }
            } else repository.update(account, current, newComment, newColor)
        }
    }

    suspend fun agentDeleteAnnotation(key: String) {
        val annotation = state.annotations.find { it.key == key } ?: error("No annotation with key $key.")
        check(state.draft?.editing?.key != key) { "The user is editing this annotation right now." }
        agentWrite {
            check(state.draft?.editing?.key != key) { "The user is editing this annotation right now." }
            repository.delete(it, currentAnnotation(remoteAnnotations, annotation.key))
        }
        if (state.selectedKey == key) state = state.copy(selectedKey = null)
    }

    /** Shows an annotation in the PDF, switching from the Markdown view when necessary. */
    fun revealAnnotation(key: String) {
        val annotation = state.annotations.find { it.key == key } ?: return
        if (state.draft != null) return
        if (state.viewMode != ReaderViewMode.Pdf) showPdf()
        selectAnnotation(annotation)
    }

    private suspend fun <T> agentWrite(action: suspend (ZoteroConfig) -> T): T {
        val account = config ?: error("Zotero is not configured.")
        val target = state.target ?: error("No PDF is open.")
        val token = session
        loadJob?.join()
        syncJob?.join()
        check(token == session) { "The PDF was closed." }
        return syncMutex.withLock {
            check(token == session) { "The PDF was closed." }
            check(!state.needsRefresh) { "Retry Zotero sync before saving agent changes." }
            state = state.copy(syncing = true, error = null)
            try {
                val result = action(account)
                val annotations = withContext(syncDispatcher) {
                    validatedAnnotations(repository.annotations(account, target.attachment.key))
                }
                check(token == session) { "The PDF was closed." }
                remoteAnnotations = annotations
                state = state.copy(annotations = visibleAnnotations())
                drainWrites(account, target, token)
                result
            } catch (exception: Exception) {
                if (token == session) {
                    if (exception !is CancellationException) report(exception, "Could not synchronize agent changes")
                    state = state.copy(syncing = false, needsRefresh = true)
                }
                throw exception
            }
        }
    }

    private fun report(exception: Exception, context: String) {
        Napier.e(context, exception)
        state = state.copy(error = "$context: ${exception.message ?: "Unknown error"}. Refresh before retrying.")
    }

    override fun onCleared() {
        close()
        super.onCleared()
    }
}
