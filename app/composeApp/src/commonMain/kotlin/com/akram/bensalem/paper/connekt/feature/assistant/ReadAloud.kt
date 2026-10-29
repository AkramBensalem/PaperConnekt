package com.akram.bensalem.paper.connekt.feature.assistant

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.akram.bensalem.paper.connekt.core.remote.functions.TextToSpeechRemoteDataSource
import com.akram.bensalem.paper.connekt.core.remote.functions.model.TtsAudio
import com.akram.bensalem.paper.connekt.core.remote.util.RemoteResult
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.abs
import kotlinx.coroutines.isActive
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class SpeechPlan(
    val version: Int = 1,
    val source: String = "markdown",
    val segments: List<SpeechSegment>,
)

@Serializable
data class SpeechSegment(
    val index: Int,
    val kind: String,
    val text: String,
    val ssml: String,
    val skipped: Boolean = false,
    val skipReason: String? = null,
    val pageIndex: Int? = null,
    val wordOffsets: List<SpeechWord> = emptyList(),
    val anchorText: String = text,
    /** OCR block region (`left, top, right, bottom`, 0..1 of the page) the words are searched in first. */
    val region: List<Float>? = null,
)

@Serializable
data class SpeechWord(
    val word: String,
    val start: Int,
    val end: Int,
    val rects: List<List<Float>> = emptyList(),
    val anchorWord: String? = null,
)

fun SpeechPlan.withPdfPositions(page: PdfPage, pageIndex: Int): SpeechPlan {
    // Index glyphs in reconstructed reading order so sequential matching follows columns.
    val ordered = analyzePdfLayout(page).orderedGlyphs.ifEmpty { page.glyphs }
    val located = PdfPageText(pageIndex, page.geometry, ordered).index().locatePlanWords(segments)
    if (segments.none { it.region != null }) return copy(segments = located)
    val width = page.geometry.displayWidth
    val height = page.geometry.displayHeight
    // OCR blocks know where each paragraph sits, so words are matched inside that box before the whole page.
    return copy(
        segments = located.mapIndexed { index, segment ->
            val region = segments[index].region
            if (segment.skipped || region == null || region.size != 4) return@mapIndexed segment
            val margin = 0.012f
            // Raw text-layer glyphs: the block is one column, so it needs no page-wide reordering.
            val inside = page.glyphs.filter { glyph ->
                val x = glyph.bounds.center.x / width
                val y = glyph.bounds.center.y / height
                glyph.text.isNotBlank() &&
                    x in region[0] - margin..region[2] + margin && y in region[1] - margin..region[3] + margin
            }
            if (inside.isEmpty()) return@mapIndexed segment
            val regional = PdfPageText(pageIndex, page.geometry, inside.inLineOrder()).index().locatePlanWords(listOf(segments[index])).first()
            if (regional.locatedWords() * 10 >= segment.locatedWords() * 7) regional else segment
        },
    )
}

private fun SpeechSegment.locatedWords() = wordOffsets.count { it.rects.isNotEmpty() }

/**
 * Finds the readable part (position among non-skipped segments, as used by [ReadAloudController.start])
 * where a user selection begins. Word rectangles are matched first; text is used when the plan has no positions.
 */
fun SpeechPlan.readablePositionFor(selectionText: String, selectionRects: List<Rect> = emptyList()): Int? {
    val readable = segments.filterNot { it.skipped }
    if (readable.isEmpty()) return null
    if (selectionRects.isNotEmpty()) {
        val areas = selectionRects.map { it.inflate(1f) }
        readable.indexOfFirst { segment ->
            segment.wordOffsets.any { word ->
                word.rects.any { r ->
                    r.size == 4 && areas.any { it.contains(Offset((r[0] + r[2]) / 2f, (r[1] + r[3]) / 2f)) }
                }
            }
        }.takeIf { it >= 0 }?.let { return it }
    }
    val selected = selectionText.speechMatchWords()
    if (selected.isEmpty()) return null
    val texts = readable.map { (it.anchorText.ifBlank { it.text }).speechMatchWords() }
    val probe = selected.take(4)
    texts.indexOfFirst { words -> words.windowed(probe.size).any { it == probe } }
        .takeIf { it >= 0 }?.let { return it }
    val selectedSet = selected.toSet()
    return texts.withIndex()
        .map { (index, words) -> index to words.count { it in selectedSet }.toFloat() / words.size.coerceAtLeast(1) }
        .filter { it.second >= 0.5f }
        .maxByOrNull { it.second }
        ?.first
}

private fun String.speechMatchWords(): List<String> =
    lowercase().split(Regex("[^\\p{L}\\p{N}]+")).filter { it.isNotBlank() }

/**
 * An OCR block is one column, so its glyphs read top-to-bottom, left-to-right. This avoids relying on the
 * page-wide column analysis, which can interleave lines of neighbouring columns.
 */
internal fun List<PdfGlyph>.inLineOrder(): List<PdfGlyph> {
    val lines = mutableListOf<MutableList<PdfGlyph>>()
    sortedBy { it.bounds.center.y }.forEach { glyph ->
        val line = lines.lastOrNull()
        val reference = line?.let { it.sumOf { g -> g.bounds.center.y.toDouble() } / it.size }?.toFloat()
        val height = maxOf(glyph.bounds.height, line?.maxOf { it.bounds.height } ?: 0f)
        if (line != null && reference != null && kotlin.math.abs(glyph.bounds.center.y - reference) <= height * 0.45f) {
            line += glyph
        } else {
            lines += mutableListOf(glyph)
        }
    }
    return lines.flatMap { line -> line.sortedBy { it.bounds.left } }
}

/**
 * Reading script for a PDF page. The OCR Markdown (Mistral) is preferred because it already restores
 * reading order, columns and headings; without it the page layout is reconstructed geometrically.
 */
fun createPdfSpeechPlan(
    page: PdfPage,
    pageIndex: Int,
    maxCharacters: Int = 1800,
    ocrMarkdown: String? = null,
    runningLines: Set<String> = emptySet(),
): SpeechPlan {
    if (!ocrMarkdown.isNullOrBlank()) {
        return createSpeechPlan(ocrMarkdown, maxCharacters, runningLines)
            .copy(source = "pdf-page-ocr")
            .withPdfPositions(page, pageIndex)
    }
    val layout = analyzePdfLayout(page)
    val blocks = layout.blocks
    val texts = blocks.map { it.text.trim() }
    val inner = blocks.filterNot(layout::isInEdgeBand)
    val titleSize = inner.maxOfOrNull { it.size } ?: 0f
    val figureBlocks = figureFragmentBlocks(blocks, texts, layout.bodySize)
    val sources = blocks.flatMapIndexed { index, block ->
        val text = texts[index]
        val edge = layout.isInEdgeBand(block)
        val skipReason = when {
            edge && runningSignature(text) in runningLines -> if (block.box.top < layout.pageHeight / 2) "running-header" else "running-footer"
            edge && block.lines.size <= 2 && block.size < layout.bodySize * 0.95f ->
                if (block.box.top < layout.pageHeight / 2) "page-header" else "page-footer"
            index in figureBlocks -> "figure-text"
            else -> null
        }
        val numberedHeading = block.lines.size <= 3 && text.split(' ').size <= 16 &&
            !text.endsWithAny('.', ',', ';', ':') && Regex("^\\d+(\\.\\d+)*\\.?\\s+(\\|\\s*)?\\p{Lu}").containsMatchIn(text)
        val hint = when {
            block.size >= layout.bodySize * 1.15f && block.lines.size <= 4 -> SpeechHint.Heading
            numberedHeading && block.size >= layout.bodySize * 0.98f -> SpeechHint.Heading
            block.lines.size > 1 -> SpeechHint.Paragraph
            else -> SpeechHint.Auto
        }
        val previous = blocks.getOrNull(index - 1)
        val titleContinuation = hint == SpeechHint.Heading && previous != null && !layout.isInEdgeBand(previous) &&
            previous.size >= layout.bodySize * 1.15f && abs(previous.size - block.size) <= block.size * 0.03f
        val titleLevel = !edge && titleSize > layout.bodySize * 1.15f && (block.size >= titleSize * 0.97f || titleContinuation)
        listOf(SpeechSource(text, hint, skipReason, titleLevel), SpeechSource(""))
    }
    return createSpeechPlan(sources, maxCharacters)
        .copy(source = "pdf-page-layout")
        .withPdfPositions(page, pageIndex)
}

fun SpeechPlan.toJson(): String = Json {
    prettyPrint = true
}.encodeToString(this)


data class ReadAloudChunk(
    val index: Int,
    val text: String,
    val ssml: String,
    val wordPositions: List<SpeechWord> = emptyList(),
    val pageIndex: Int? = null,
    val kind: String = "paragraph",
)

enum class ReadAloudStatus {
    Idle,
    Preparing,
    Playing,
    Paused,
    Completed,
    Error,
}

data class ReadAloudState(
    val status: ReadAloudStatus = ReadAloudStatus.Idle,
    val chunks: List<ReadAloudChunk> = emptyList(),
    val currentChunk: Int = 0,
    val currentWord: Int = -1,
    val error: String? = null,
) {
    val hasContent: Boolean get() = chunks.isNotEmpty()
    val currentText: String get() = chunks.getOrNull(currentChunk)?.text.orEmpty()
    val currentWordText: String get() = chunks.getOrNull(currentChunk)?.let { chunk ->
        chunk.text.split(Regex("\\s+")).getOrNull(currentWord).orEmpty()
    }.orEmpty()
    val currentWordRects: List<List<Float>> get() =
        if (currentWord < 0) emptyList()
        else chunks.getOrNull(currentChunk)?.wordPositions?.getOrNull(currentWord)?.rects.orEmpty()
    val currentPageIndex: Int? get() = chunks.getOrNull(currentChunk)?.pageIndex
    val isActive: Boolean get() = status == ReadAloudStatus.Preparing ||
        status == ReadAloudStatus.Playing || status == ReadAloudStatus.Paused
}

/**
 * Plays bounded pieces of a paper in order. The server and this controller both
 * cache generated audio, so replaying a selection does not create another
 * Google TTS request when the same chunk already exists.
 */
class ReadAloudController(
    private val remote: TextToSpeechRemoteDataSource,
    private val player: TtsAudioPlayer = createTtsAudioPlayer(),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    private val usageRecorder: suspend (AiUsageEvent) -> Unit = {},
) {
    var state by mutableStateOf(ReadAloudState())
        private set

    private val cache = mutableMapOf<String, TtsAudio>()
    private val cacheMutex = Mutex()
    private val generationMutex = Mutex()
    private var playbackJob: Job? = null
    private var sessionPlan: SpeechPlan? = null
    private var sessionRootId: String? = null
    private var sessionExampleId: String? = null
    private var sessionLanguageCode: String = "en-US"

    fun start(
        text: String,
        rootId: String,
        exampleId: String,
        languageCode: String? = null,
    ) {
        start(createSpeechPlan(text), rootId, exampleId, languageCode)
    }

    fun start(
        plan: SpeechPlan,
        rootId: String,
        exampleId: String,
        languageCode: String? = null,
        startChunk: Int = 0,
    ) {
        val chunks = plan.segments.filterNot { it.skipped }.map {
            ReadAloudChunk(it.index, it.text, it.ssml, it.wordOffsets, it.pageIndex, it.kind)
        }
        if (chunks.isEmpty()) {
            state = ReadAloudState(status = ReadAloudStatus.Error, error = "There is no readable text here.")
            return
        }
        stop()
        val firstChunk = startChunk.coerceIn(0, chunks.lastIndex)
        val resolvedLanguageCode = languageCode ?: detectSpeechLanguage(chunks.joinToString(" ") { it.text })
        sessionPlan = plan
        sessionRootId = rootId
        sessionExampleId = exampleId
        sessionLanguageCode = resolvedLanguageCode
        state = ReadAloudState(status = ReadAloudStatus.Preparing, chunks = chunks, currentChunk = firstChunk)
        playbackJob = scope.launch {
            // Generate only the current part. This avoids paying for audio the user may never reach.
            try {
                chunks.drop(firstChunk).forEachIndexed { relativeChunkPosition, chunk ->
                    val chunkPosition = firstChunk + relativeChunkPosition
                    Napier.d("Read-aloud chunk start: index=${chunk.index} position=$chunkPosition kind=${chunk.kind} textLength=${chunk.text.length} ssmlLength=${chunk.ssml.length}")
                    state = state.copy(
                        status = ReadAloudStatus.Preparing,
                        currentChunk = chunkPosition,
                        currentWord = -1,
                        error = null,
                    )
                    val audio = audioFor(chunk, rootId, "$exampleId-${chunk.index}", resolvedLanguageCode)
                    player.preload(audio.audioUrl)
                    state = state.copy(status = ReadAloudStatus.Playing, currentChunk = chunkPosition, currentWord = -1)
                    val words = chunk.text.split(Regex("\\s+")).filter(String::isNotBlank)
                    val wordJob = launch {
                        var lastWordIndex = -1
                        var firstWordStartedAt: Float? = null
                        while (currentCoroutineContext().isActive && words.isNotEmpty()) {
                            val positionSeconds = player.positionSeconds()
                            val candidateWord = audio.timepoints
                                .takeIf { it.isNotEmpty() }
                                ?.let { timepoints ->
                                    positionSeconds?.let { position ->
                                        timepoints
                                            .filter { it.timeSeconds <= position }
                                            .maxByOrNull { it.timeSeconds }
                                            ?.markName
                                            ?.removePrefix("w")
                                            ?.toIntOrNull()
                                            ?.coerceIn(0, words.lastIndex)
                                    }
                                }
                                ?: player.progress()?.let { progress -> weightedWordIndex(words, progress) }
                            val currentWord = candidateWord?.let { candidate ->
                                    if (lastWordIndex == 0 && candidate > 0 &&
                                        chunk.kind == "heading" &&
                                        firstWordStartedAt != null &&
                                        positionSeconds != null &&
                                        positionSeconds - firstWordStartedAt!! < 0.24f
                                    ) {
                                        0
                                    } else {
                                        when {
                                            lastWordIndex < 0 -> 0
                                            candidate < lastWordIndex -> lastWordIndex
                                            else -> candidate
                                        }
                                    }
                                }
                                currentWord?.let {
                                    if (it == 0 && firstWordStartedAt == null) {
                                        firstWordStartedAt = positionSeconds
                                    }
                                    lastWordIndex = it
                                    state = state.copy(
                                    currentWord = it,
                                )
                            }
                            kotlinx.coroutines.delay(40L)
                        }
                    }
                    try {
                        player.play(audio.audioUrl)
                        Napier.d("Read-aloud chunk complete: index=${chunk.index} position=$chunkPosition")
                    } catch (exception: CancellationException) {
                        Napier.d("Read-aloud chunk cancelled: index=${chunk.index} position=$chunkPosition")
                        throw exception
                    } catch (exception: Exception) {
                        Napier.e("Read-aloud chunk playback failed: index=${chunk.index} position=$chunkPosition url=${audio.audioUrl}", exception)
                        throw exception
                    } finally {
                        wordJob.cancelAndJoin()
                    }
                }
                state = state.copy(status = ReadAloudStatus.Completed, currentChunk = chunks.lastIndex, currentWord = -1)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Throwable) {
                // Ktor browser fetch failures can be Kotlin Error rather than Exception.
                Napier.e("Read-aloud playback failed.", exception)
                player.stop()
                state = state.copy(status = ReadAloudStatus.Error, error = exception.message ?: "Could not read this text aloud.")
            }
        }
    }

    fun pause() {
        if (state.status == ReadAloudStatus.Playing) {
            player.pause()
            state = state.copy(status = ReadAloudStatus.Paused)
        }
    }

    fun resume() {
        if (state.status == ReadAloudStatus.Paused) {
            player.resume()
            state = state.copy(status = ReadAloudStatus.Playing)
        }
    }

    fun stop() {
        playbackJob?.cancel()
        playbackJob = null
        player.stop()
        if (state.status != ReadAloudStatus.Idle) state = ReadAloudState()
    }

    fun nextPart() {
        val plan = sessionPlan ?: return
        val rootId = sessionRootId ?: return
        val exampleId = sessionExampleId ?: return
        val next = state.currentChunk + 1
        if (next < state.chunks.size) start(plan, rootId, exampleId, sessionLanguageCode, next)
    }

    fun previousPart() {
        val plan = sessionPlan ?: return
        val rootId = sessionRootId ?: return
        val exampleId = sessionExampleId ?: return
        val previous = (state.currentChunk - 1).coerceAtLeast(0)
        if (state.chunks.isNotEmpty() && previous != state.currentChunk) {
            start(plan, rootId, exampleId, sessionLanguageCode, previous)
        }
    }

    fun firstPart() {
        val plan = sessionPlan ?: return
        val rootId = sessionRootId ?: return
        val exampleId = sessionExampleId ?: return
        if (state.chunks.isNotEmpty() && state.currentChunk != 0) {
            start(plan, rootId, exampleId, sessionLanguageCode, 0)
        }
    }

    fun lastPart() {
        val plan = sessionPlan ?: return
        val rootId = sessionRootId ?: return
        val exampleId = sessionExampleId ?: return
        val last = state.chunks.lastIndex
        if (last >= 0 && state.currentChunk != last) {
            start(plan, rootId, exampleId, sessionLanguageCode, last)
        }
    }

    fun close() {
        stop()
        scope.cancel()
    }

    private suspend fun audioFor(
        chunk: ReadAloudChunk,
        rootId: String,
        exampleId: String,
        languageCode: String,
    ): TtsAudio {
        val key = "$rootId|$exampleId|$languageCode|${chunk.ssml}"
        cacheMutex.withLock { cache[key] }?.let { return it }
        return generationMutex.withLock {
            cacheMutex.withLock { cache[key] }?.let { return@withLock it }
            Napier.d("TTS audio resolve: chunk=${chunk.index} textLength=${chunk.text.length} cacheKeyHash=${key.hashCode()}")
            var lastError: Throwable? = null
            repeat(3) { attempt ->
                try {
                    when (val result = remote.generate(
                        text = chunk.text,
                        languageCode = languageCode,
                        kind = "sentence",
                        rootId = rootId,
                        exampleId = exampleId,
                        ssml = chunk.ssml,
                    )) {
                        is RemoteResult.Success -> {
                            Napier.d("TTS audio resolved: chunk=${chunk.index} cached=${result.data.cached} path=${result.data.path} timepoints=${result.data.timepoints.size}")
                            cacheMutex.withLock { cache[key] = result.data }
                            if (!result.data.cached) {
                                usageRecorder(
                                    AiUsageEvent(
                                        service = AiUsageService.TTS,
                                        timestamp = nowMillis(),
                                        inputUnits = chunk.text.length.toLong(),
                                        model = "google-standard-tts",
                                    )
                                )
                            }
                            return@withLock result.data
                        }
                        is RemoteResult.Error -> throw result.exception
                    }
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Throwable) {
                    lastError = exception
                    Napier.w("TTS audio attempt ${attempt + 1}/3 failed: chunk=${chunk.index}", exception)
                    if (attempt < 2) kotlinx.coroutines.delay((attempt + 1) * 500L)
                }
            }
            throw lastError ?: IllegalStateException("Could not generate speech audio.")
        }
    }
}

interface TtsAudioPlayer {
    suspend fun play(url: String)
    fun pause()
    fun resume()
    fun stop()
    /** Downloads [url] ahead of time so [play] can start without a network round-trip. */
    suspend fun preload(url: String) = Unit
    fun progress(): Float? = null
    fun positionSeconds(): Float? = null
}


internal fun splitForSpeech(text: String, maxCharacters: Int = 1800): List<ReadAloudChunk> {
    return createSpeechPlan(text, maxCharacters).segments.filterNot { it.skipped }
        .map { ReadAloudChunk(it.index, it.text, it.ssml, it.wordOffsets, it.pageIndex, it.kind) }
}

internal enum class SpeechHint { Auto, Heading, Paragraph }

/** One source line/block of a page before speech classification; blank text separates paragraphs. */
internal data class SpeechSource(
    val text: String,
    val hint: SpeechHint = SpeechHint.Auto,
    val skipReason: String? = null,
    /** Set for blocks typeset at the title size, so title lines are never mistaken for author names. */
    val titleLevel: Boolean = false,
    val region: List<Float>? = null,
    /** Already classified as readable (AI reading script): metadata heuristics are not applied. */
    val trusted: Boolean = false,
)

/** Groups of tiny text fragments (diagram labels, rotated axis text) that are not prose. */
private fun figureFragmentBlocks(blocks: List<LayoutBlock>, texts: List<String>, bodySize: Float): Set<Int> {
    fun words(index: Int) = texts[index].split(' ').count { it.isNotBlank() }
    fun fragment(index: Int) = blocks[index].size < bodySize * 1.15f &&
        words(index).toFloat() / blocks[index].lines.size < 4f &&
        !texts[index].endsWithAny('.', '?', '!')
    fun short(index: Int) = blocks[index].size < bodySize * 1.15f && words(index) < 25 && !texts[index].endsWithAny('.', '?', '!')
    val result = mutableSetOf<Int>()
    var start = 0
    while (start < blocks.size) {
        if (!short(start) && !fragment(start)) {
            start++
            continue
        }
        var end = start
        while (end + 1 < blocks.size && (short(end + 1) || fragment(end + 1))) end++
        if ((start..end).count(::fragment) >= 3) result += start..end
        start = end + 1
    }
    return result
}

fun createSpeechPlan(text: String, maxCharacters: Int = 1800, runningLines: Set<String> = emptySet()): SpeechPlan =
    createSpeechPlan(markdownSpeechSources(text, runningLines), maxCharacters)

/** Converts (OCR) Markdown into speech sources, keeping `#` headings and dropping tables, math and figures. */
internal fun markdownSpeechSources(text: String, runningLines: Set<String> = emptySet()): List<SpeechSource> {
    val lines = text
        .replace(Regex("<!--[\\s\\S]*?-->"), "\n")
        .replace(Regex("```[\\s\\S]*?```"), "\n")
        .replace(Regex("\\$\\$[\\s\\S]*?\\$\\$"), "\n")
        .replace(Regex("!\\[[^]]*]\\([^)]*\\)"), "")
        .replace(Regex("\\[([^]]+)]\\([^)]*\\)"), "$1")
        .replace(Regex("\\$([^$\\n]{1,200})\\$")) { inlineMathSpeech(it.groupValues[1]) }
        .replace(Regex("(?i)<sup>[^<]*</sup>"), "")
        .replace(Regex("<[^>]+>"), " ")
        .lines()
    val content = lines.indices.filter { lines[it].isNotBlank() }
    val edges = (content.take(3) + content.takeLast(3)).toSet()
    return lines.mapIndexed { index, line ->
        val raw = line.trim()
        val plain = raw.replace(Regex("[*_#>`]"), "").replace(Regex("\\s+"), " ").trim()
        when {
            plain.isBlank() || plain.matches(Regex("[|:\\-\\s]+")) ->
                if (raw.startsWith("|")) SpeechSource("|", skipReason = "table") else SpeechSource("")
            raw.startsWith("|") -> SpeechSource(plain, skipReason = "table")
            raw.matches(Regex("^\\[\\^[^]]+]:.*")) -> SpeechSource(plain, skipReason = "footnote")
            index in edges && runningSignature(plain) in runningLines -> SpeechSource(plain, skipReason = "running-header-footer")
            raw.startsWith("#") -> SpeechSource(plain, SpeechHint.Heading)
            else -> SpeechSource(plain)
        }
    }
}

/** Keeps short symbols (`$k$`, `$n=3$`) and drops footnote markers and long formulas. */
private fun inlineMathSpeech(body: String): String {
    if (Regex("^\\s*(\\{\\s*\\})?\\s*\\^").containsMatchIn(body)) return ""
    val plain = body.replace(Regex("\\\\[a-zA-Z]+"), " ").replace(Regex("[{}^_\\\\]"), " ").replace(Regex("\\s+"), " ").trim()
    return if (plain.length <= 12) plain else ""
}

internal fun createSpeechPlan(sources: List<SpeechSource>, maxCharacters: Int = 1800): SpeechPlan {
    val blocks = mutableListOf<SpeechBlock>()
    var current = StringBuilder()
    var currentHeading = false
    var currentRegion: List<Float>? = null
    var skippingReferences = false
    var skippingMetadata = false
    var titleSeen = false
    var bodyStarted = false
    var skipCitationParagraph = false
    var citationContentSeen = false

    fun flush() {
        if (current.isNotBlank()) {
            val text = current.toString().trim()
            blocks += SpeechBlock(text, currentHeading, region = currentRegion)
            if (text.split(' ').size >= 30) bodyStarted = true
        }
        current = StringBuilder()
        currentHeading = false
        currentRegion = null
    }

    fun skip(text: String, reason: String, region: List<Float>? = null) {
        flush()
        val last = blocks.lastOrNull()
        if (last != null && last.skipped && last.skipReason == reason && last.region == region) {
            blocks[blocks.lastIndex] = last.copy(text = "${last.text}\n$text", anchorText = "${last.anchorText}\n$text")
        } else {
            blocks += SpeechBlock(text, heading = false, skipped = true, skipReason = reason, region = region)
        }
    }

    sources.forEach { source ->
        val line = source.text.trim().collapseLetterSpacing()
        if (line.isBlank()) {
            flush()
            skippingMetadata = false
            if (citationContentSeen) skipCitationParagraph = false
            return@forEach
        }
        source.skipReason?.let { reason ->
            if (reason != "table" || line != "|") skip(line, reason, source.region)
            return@forEach
        }
        if (source.trusted) {
            val heading = source.hint == SpeechHint.Heading
            val spokenLine = line.removeAcademicCitations()
            if (spokenLine.isBlank()) return@forEach
            if (heading) {
                flush()
                val numbered = numberedSection(line)
                blocks += if (numbered == null) {
                    SpeechBlock(spokenLine.removeHeadingAcronym().speakableHeading(), heading = true, anchorText = line, region = source.region)
                } else {
                    SpeechBlock(numbered.spokenText, heading = true, anchorText = line, anchorWords = numbered.anchorWords, region = source.region)
                }
            } else {
                if (current.isNotEmpty()) current.append(' ')
                current.append(spokenLine)
                currentRegion = currentRegion ?: source.region
            }
            return@forEach
        }
        if (isReferenceHeading(line)) {
            skip(line, "references-section")
            skippingReferences = true
            return@forEach
        }
        if (skippingReferences) {
            skip(line, "references-section")
            return@forEach
        }
        if (skippingMetadata) {
            skip(line, "article-metadata")
            return@forEach
        }
        if (skipCitationParagraph) {
            skip(line, "citation-format")
            citationContentSeen = true
            return@forEach
        }
        if (line.matches(Regex("(?i)^(acm reference format|reference format|cite this (article|paper) as|how to cite( this article)?)\\s*:?.*"))) {
            skip(line, "citation-format")
            skipCitationParagraph = true
            citationContentSeen = line.substringAfter(':', "").isNotBlank()
            return@forEach
        }
        metadataSkipReason(line)?.let { reason ->
            skip(line, reason)
            if (reason == "article-metadata") skippingMetadata = true
            return@forEach
        }
        if (!bodyStarted && titleSeen && !source.titleLevel &&
            (looksLikeAuthorLine(line) || looksLikePersonName(line) || looksLikeNameWithAffiliation(line))
        ) {
            skip(line, "authors")
            return@forEach
        }
        if (looksLikeAffiliation(line)) {
            skip(line, "affiliations")
            return@forEach
        }
        sectionLine(line)?.let { (heading, body) ->
            flush()
            blocks += SpeechBlock(heading, heading = true)
            titleSeen = true
            bodyStarted = true
            val bodySkip = metadataSkipReason(body)
            if (body.isNotBlank() && bodySkip != null) {
                skip(body, bodySkip)
            } else if (body.isNotBlank()) {
                val spokenBody = body.removeAcademicCitations()
                if (spokenBody.isNotBlank()) blocks += SpeechBlock(spokenBody, heading = false)
            }
            return@forEach
        }
        val heading = when (source.hint) {
            SpeechHint.Heading -> line.length <= 240
            SpeechHint.Paragraph -> false
            SpeechHint.Auto -> isSpeechHeading(line)
        }
        val spokenLine = line.removeAcademicCitations()
        if (spokenLine.isBlank()) return@forEach
        if (heading) {
            flush()
            val numbered = numberedSection(line)
            blocks += if (numbered == null) {
                SpeechBlock(spokenLine.removeHeadingAcronym().speakableHeading(), heading = true, anchorText = line)
            } else {
                bodyStarted = true
                SpeechBlock(
                    text = numbered.spokenText,
                    heading = true,
                    anchorText = line,
                    anchorWords = numbered.anchorWords,
                )
            }
            if (isSectionHeading(spokenLine)) bodyStarted = true
            titleSeen = true
            return@forEach
        }
        if (current.isNotEmpty()) current.append(' ')
        current.append(spokenLine)
    }
    flush()

    val pieces = mutableListOf<SpeechBlock>()
    blocks.forEach { block ->
        if (block.skipped) {
            pieces += block
            return@forEach
        }
        if (block.text.length <= maxCharacters) {
            pieces += block
        } else {
            var currentPiece = StringBuilder()
            block.text.split(Regex("(?<=[.!?])\\s+|(?<=[,;:])\\s+|\\s+")).forEach { word ->
                if (currentPiece.length + word.length + 1 > maxCharacters) {
                    if (currentPiece.isNotBlank()) pieces += SpeechBlock(currentPiece.toString(), block.heading, region = block.region)
                    currentPiece = StringBuilder()
                }
                if (currentPiece.isNotEmpty()) currentPiece.append(' ')
                currentPiece.append(word)
            }
            if (currentPiece.isNotBlank()) pieces += SpeechBlock(currentPiece.toString(), block.heading, region = block.region)
        }
    }
    return SpeechPlan(
        segments = pieces.mapIndexed { index, block ->
            SpeechSegment(
                index = index,
                kind = if (block.heading) "heading" else "paragraph",
                text = block.text,
                ssml = if (block.skipped) "" else block.toSsml(),
                skipped = block.skipped,
                skipReason = block.skipReason,
                wordOffsets = if (block.skipped) emptyList() else block.wordOffsets(),
                anchorText = block.anchorText,
                region = block.region,
            )
        },
    )
}

private fun String.wordOffsets(anchorWords: List<String?> = emptyList()): List<SpeechWord> =
    Regex("\\S+").findAll(this).mapIndexed { index, match ->
        SpeechWord(
            match.value,
            match.range.first,
            match.range.last + 1,
            anchorWord = if (anchorWords.isEmpty()) match.value else anchorWords.getOrNull(index),
        )
    }.toList()

private data class SpeechBlock(
    val text: String,
    val heading: Boolean,
    val skipped: Boolean = false,
    val skipReason: String? = null,
    val anchorText: String = text,
    val anchorWords: List<String?> = emptyList(),
    val region: List<Float>? = null,
) {
    fun wordOffsets(): List<SpeechWord> = text.wordOffsets(anchorWords)

    fun toSsml(): String {
        val escaped = text
            .replace("&", "and")
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
        val sentenceBreaks = escaped.replace(Regex("([.!?])\\s+")) {
            "${it.value.trimEnd()}<break time=\"300ms\"/> "
        }
        val headingBreaks = if (heading) {
            sentenceBreaks.replace(Regex("([\\p{L}\\d]+-[\\p{L}\\d]+)\\s+")) {
                "${it.groupValues[1]}<break time=\"120ms\"/> "
            }
        } else {
            sentenceBreaks
        }
        val prefix = if (heading) "<break time=\"250ms\"/>" else ""
        val suffix = if (heading) "<break time=\"450ms\"/>" else "<break time=\"300ms\"/>"
        return "<speak>$prefix$headingBreaks$suffix</speak>"
    }
}

private fun weightedWordIndex(words: List<String>, progress: Float): Int {
    val weights = words.map { word ->
        word.count(Char::isLetter).coerceAtLeast(1).toFloat() +
            word.count { it in ",;:" } * 0.8f +
            word.count { it in ".!?" } * 1.8f
    }
    val target = weights.sum() * progress.coerceIn(0f, 1f)
    var total = 0f
    weights.forEachIndexed { index, weight ->
        total += weight
        if (target < total) return index
    }
    return words.lastIndex
}

private data class NumberedSection(
    val spokenText: String,
    val anchorWords: List<String?>,
)

private fun numberedSection(line: String): NumberedSection? {
    val match = Regex("^(\\d+(?:\\.\\d+)*)\\.?\\s+(?:\\|\\s*)?(\\p{L}.*)$").matchEntire(line) ?: return null
    val number = match.groupValues[1]
    val title = match.groupValues[2].removeHeadingAcronym().speakableHeading()
    val titleWords = Regex("\\S+").findAll(title).map { it.value }.toList()
    return NumberedSection("Section $number: $title", listOf(null, number) + titleWords)
}

/** "ADVANCED REVIEW" → "Advanced Review" so TTS reads words instead of spelling letters; acronyms stay. */
private fun String.speakableHeading(): String {
    val cleaned = replace(Regex("\\s*\\|\\s*"), " ").replace(Regex("\\s+"), " ").trim()
    if (cleaned.any { it.isLowerCase() }) return cleaned
    return cleaned.replace(Regex("\\b\\p{Lu}{4,}\\b")) { match ->
        val word = match.value
        if (word.any { it in "AEIOUY" }) word.lowercase().replaceFirstChar { it.uppercase() } else word
    }
}

private val spacedLabelWords = listOf(
    "advanced", "review", "reviews", "original", "research", "article", "articles", "overview", "focus", "perspective",
    "opinion", "tutorial", "abstract", "keywords", "references", "introduction", "letter", "commentary", "survey", "paper",
    "short", "communication", "case", "study", "special", "issue", "full", "primer", "focus", "editorial", "methods",
    "brief", "report", "technical", "note", "invited", "position", "systematic", "software", "data", "highlights",
).sortedByDescending { it.length }

/** "A D V A N C E D R E V I E W" → "ADVANCED REVIEW"; unknown runs become one word. */
internal fun String.collapseLetterSpacing(): String =
    replace(Regex("\\b(?:\\p{Lu} ){3,}\\p{Lu}\\b")) { match ->
        val joined = match.value.replace(" ", "")
        splitLabelWords(joined.lowercase())?.joinToString(" ") { it.uppercase() } ?: joined
    }

private fun splitLabelWords(text: String): List<String>? {
    if (text.isEmpty()) return emptyList()
    spacedLabelWords.forEach { word ->
        if (text.startsWith(word)) splitLabelWords(text.removePrefix(word))?.let { return listOf(word) + it }
    }
    return null
}

private fun String.removeAcademicCitations(): String =
    replace(
        Regex("\\((?=[^()]{0,160}\\b(?:19|20)\\d{2}[a-z]?\\b)(?=[^()]*(?:et al|[A-Z][\\p{L}'-]+))[^()]{1,160}\\)"),
        "",
    ).replace(
        Regex("\\[(?:\\s*\\d+(?:\\s*[-–]\\s*\\d+)?\\s*(?:,\\s*)?)+\\]"),
        "",
    ).replace(Regex("(?i)\\b(?:https?://|www\\.)\\S+"), "")
        .replace(Regex("(?i)\\bdoi:?\\s*10\\.\\S+"), "")
        .replace(Regex("\\s+"), " ").replace(Regex("\\s+([,.!?;:])"), "$1").trim()

private fun String.removeHeadingAcronym(): String =
    replace(Regex("\\s+\\([A-Z][A-Z\\d-]{1,12}\\)$"), "").trim()

private fun isSpeechHeading(line: String): Boolean =
    line.length <= 120 &&
        line.split(' ').size <= 14 &&
        !line.endsWithAny('.', '!', '?', ';', ':', ',') &&
        (line.firstOrNull()?.isUpperCase() == true ||
            line == line.uppercase() ||
            line.matches(Regex("^\\d+(\\.\\d+)*\\.?\\s+(\\|\\s*)?\\p{Lu}.*")))

internal fun isReferenceHeading(line: String): Boolean =
    line.replace(Regex("^\\d+(\\.\\d+)*\\.?\\s+(\\|\\s*)?"), "")
        .replace(Regex("\\s+"), " ").trim()
        .matches(Regex("(?i)references?|bibliography|works cited|literature cited|r e f e r e n c e s"))

private fun isSectionHeading(line: String): Boolean =
    line.matches(Regex("(?i)^(\\d+(\\.\\d+)*\\.?\\s+)?(\\|\\s*)?(abstract|introduction|background|related work|methods?|methodology|results|discussion|conclusions?|summary)\\b.*"))

private fun sectionLine(line: String): Pair<String, String>? {
    val labelled = Regex("(?i)^(abstract|key\\s*words|keywords|introduction|background|methodology|methods|results|discussion|conclusion|acknowledgements?)\\s*[:.—–]\\s*(.*)$")
        .matchEntire(line)
        ?: Regex("^(ABSTRACT|KEYWORDS|K E Y W O R D S|A B S T R A C T)\\s+(.+)$").matchEntire(line)
        ?: return null
    val label = labelled.groupValues[1].replace(" ", "").lowercase().replaceFirstChar { it.uppercase() }
    return (if (label == "Keywords") "Keywords" else label) to labelled.groupValues[2]
}

private val emailPattern = Regex("[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+")

/** Article metadata that people skip when reading a paper aloud. */
private fun metadataSkipReason(line: String): String? = when {
    line.matches(Regex("(?i)author\\(s\\).*leave this section blank.*")) ||
        line.matches(Regex("(?i)(author|institution|email)\\s*:?\\s*$")) -> "review-author-boilerplate"
    line.matches(Regex("(?i)(page\\s*)?\\d{1,4}(\\s*(of|/)\\s*\\d{1,4})?")) -> "page-number"
    line.matches(Regex("(?i)(doi|https?://|www\\.|arxiv:)\\S+")) -> "link"
    line.matches(Regex("[^\\w\\s]{2,}")) -> "symbols"
    line.length <= 300 && Regex("(?i)^(received|revised|accepted|published( online)?|available online|first published|submitted|article history)\\b").containsMatchIn(line) &&
        Regex("\\b(19|20)\\d{2}\\b").containsMatchIn(line) -> "publication-dates"
    line.length <= 200 && Regex("(?i)^(doi|digital object identifier)\\s*:?\\s*").containsMatchIn(line) -> "doi"
    line.length <= 400 && Regex("(?i)(©|\\(c\\)\\s*(19|20)\\d{2}|copyright|all rights reserved|creative commons|cc by[- ]|license, attribution|published under the terms|periodicals, inc|elsevier (b\\.v\\.|ltd)|springer nature|issn\\s*[:\\d])").containsMatchIn(line) -> "publisher-footer"
    line.length <= 700 && (emailPattern.containsMatchIn(line) ||
        Regex("(?i)^(correspondence|corresponding author|e-?mail|orcid|funding information|present address|handling editor|edited by)\\b").containsMatchIn(line)) -> "contact-details"
    Regex("(?i)^this article is categori[sz]ed under").containsMatchIn(line) -> "article-metadata"
    Regex("(?i)^(ccs\\s+concepts|categories and subject descriptors|additional key words and phrases)\\b").containsMatchIn(line) ||
        (line.startsWith("•") && line.contains("→")) -> "classification"
    line.length <= 200 && Regex("(?i)^(these authors contributed equally|both authors contributed equally|equal contribution|\\*?\\s*corresponding author)").containsMatchIn(line) -> "contact-details"
    line.length <= 200 && Regex("(?i)^(acm isbn|isbn|issn|e-?issn)\\b").containsMatchIn(line) -> "publisher-footer"
    line.length <= 200 && Regex("(?i)^(article info|article history|journal homepage|contents lists available at|available at sciencedirect)\\b").containsMatchIn(line) -> "venue-metadata"
    line.length <= 160 && Regex("(?i)^(arxiv:|preprint|under review|accepted (manuscript|for publication)|proceedings of|vol(ume)?\\.? \\d+)").containsMatchIn(line) -> "venue-metadata"
    else -> null
}

private val nameParticles = setOf(
    "van", "de", "der", "den", "von", "da", "di", "del", "della", "le", "la", "du", "dos", "das", "bin", "ben", "al", "el", "y", "ten", "ter", "e",
)

/** "Michel van de Velden | Alfonso Iodice D'Enza | Angelos Markos" and similar author lists. */
private fun looksLikeAuthorLine(line: String): Boolean {
    if (line.length > 400 || line.endsWithAny('.', ':', '?', '!')) return false
    val chunks = line.replace(Regex("[\\d*†‡§¶∗]"), " ")
        .split(Regex("\\s*(?:\\||,|;|·|&|\\band\\b)\\s*"))
        .map { it.trim() }
        .filter { it.isNotEmpty() }
    if (chunks.size < 2) return false
    return chunks.count { it.contains(' ') } >= 1 && chunks.all { chunk ->
        val words = chunk.split(Regex("\\s+"))
        words.size in 1..5 && words.all { word ->
            word.lowercase() in nameParticles ||
                word.matches(Regex("\\p{Lu}\\.(-?\\p{Lu}\\.)*")) ||
                (word.first().isUpperCase() && word.all { it.isLetter() || it in "'’.-" })
        }
    }
}

private val headingWords = setOf(
    "of", "and", "the", "for", "in", "on", "with", "to", "a", "an", "via", "using", "from", "by", "at", "or", "towards", "toward",
    "abstract", "introduction", "keywords", "review", "paper", "article", "research", "conclusion", "conclusions", "background",
)

/** A lone author name such as "Cecilia Panigutti", "Jerry M. Mendel" or "GRETA WARREN". */
private fun looksLikePersonName(line: String): Boolean {
    val cleaned = line.replace(Regex("[\\d*†‡§¶∗,]+$"), "").trim()
    if (cleaned.length > 60) return false
    val words = cleaned.split(Regex("\\s+"))
    if (words.size !in 2..5 || words.any { it.lowercase() in headingWords }) return false
    return words.all { word ->
        word.lowercase() in nameParticles ||
            word.matches(Regex("\\p{Lu}\\.(-?\\p{Lu}\\.)*")) ||
            (word.first().isUpperCase() && word.all { it.isLetter() || it in "'’-" })
    } && words.count { it.length > 2 } >= 2
}

/** "GRETA WARREN, School of Computer Science, University College Dublin" style author lines. */
private fun looksLikeNameWithAffiliation(line: String): Boolean {
    if (line.length > 400) return false
    val name = line.substringBefore(',', "")
    return name.isNotBlank() && looksLikePersonName(name) &&
        affiliationWord.containsMatchIn(line.substringAfter(',')) &&
        !Regex("(?i)\\b(is|are|was|were|we|this|that|these|our|has|have)\\b").containsMatchIn(line)
}

private val affiliationWord = Regex(
    "\\b(Department|Dept\\.|School|Faculty|Institute|Laborator(?:y|ies)|Centre|Center|University|College|Division|Commission|Research|Inc\\.|Corporation|Hospital)\\b",
)

private val affiliationStart = Regex(
    "^(?:[a-z\\d]{1,2}\\s*)?(?:The\\s+)?(?:Department|Dept\\.|School|Faculty|Institute|Institut|Laborator(?:y|ies)|Laboratoire|Lab|Centre|Center|University|Université|Universidad|Universität|College|Division|Graduate School|CNRS|Inria|IMT)\\b",
)

private fun looksLikeAffiliation(line: String): Boolean =
    line.length <= 700 &&
        (affiliationStart.containsMatchIn(line) || line.matches(Regex("^(University|Université|Universidad)\\b.*"))) &&
        line.contains(',') &&
        !Regex("(?i)\\b(is|are|was|were|we|this|that|these|our|has|have)\\b").containsMatchIn(line)

private fun String.endsWithAny(vararg values: Char): Boolean = values.any(::endsWith)
