package com.akram.bensalem.paper.connekt.feature.assistant

import androidx.compose.ui.geometry.Rect

/** Text layer of one PDF page, used to anchor agent annotations without rendering at full resolution. */
data class PdfPageText(val pageIndex: Int, val geometry: PdfGeometry, val glyphs: List<PdfGlyph>)

data class PdfTextMatch(val pageIndex: Int, val selection: PdfTextSelection)

private val ligatures = mapOf('ﬀ' to "ff", 'ﬁ' to "fi", 'ﬂ' to "fl", 'ﬃ' to "ffi", 'ﬄ' to "ffl", 'ﬅ' to "st", 'ﬆ' to "st")

private fun normalizedChars(char: Char): String =
    (ligatures[char] ?: char.toString()).filter { it.isLetterOrDigit() }.lowercase()

fun normalizeForSearch(text: String): String = buildString { text.forEach { append(normalizedChars(it)) } }

fun PdfPageText.index(): PdfPageTextIndex = PdfPageTextIndex(pageIndex, geometry, glyphs)

class PdfPageTextIndex(
    val pageIndex: Int,
    val geometry: PdfGeometry,
    val glyphs: List<PdfGlyph>,
) {
    val raw = StringBuilder()
    val rawGlyph = mutableListOf<Int>()
    val normalized = StringBuilder()
    val normalizedToRaw = mutableListOf<Int>()
    val haystack: String

    init {
        val unrotated = geometry.copy(rotation = 0)
        var previousBounds: Rect? = null
        glyphs.forEachIndexed { index, glyph ->
            if (glyph.text.isEmpty()) return@forEachIndexed
            val bounds = unrotated.displayRect(geometry.pdfRect(glyph.bounds))
            val last = previousBounds
            if (last != null) {
                val height = maxOf(last.height, bounds.height)
                val newLine = kotlin.math.abs(last.bottom - bounds.bottom) > height * 0.5f
                if (newLine || bounds.left - last.right > height * 0.2f) { raw.append(' '); rawGlyph += -1 }
            }
            glyph.text.forEach { raw.append(it); rawGlyph += index }
            previousBounds = bounds
        }
        raw.forEachIndexed { position, char ->
            normalizedChars(char).forEach { normalized.append(it); normalizedToRaw += position }
        }
        haystack = normalized.toString()
    }

    private fun boundary(rawPosition: Int) = rawPosition !in raw.indices || !raw[rawPosition].isLetterOrDigit()

    fun findExact(query: String, matchCase: Boolean = false, wholeWord: Boolean = false): List<PdfTextMatch> {
        if (query.isBlank()) return emptyList()
        val text = raw.toString()
        val matches = mutableListOf<PdfTextMatch>()
        fun wordBoundary(position: Int) = position !in text.indices ||
            (!text[position].isLetterOrDigit() && text[position] != '_')
        var start = text.indexOf(query, ignoreCase = !matchCase)
        while (start >= 0) {
            val end = start + query.length
            if (!wholeWord || (wordBoundary(start - 1) && wordBoundary(end))) {
                val indices = rawGlyph.subList(start, end).filter { it >= 0 }.distinct()
                val selection = geometry.selectionFor(indices.map { glyphs[it] })
                if (selection.rects.isNotEmpty()) matches += PdfTextMatch(pageIndex, selection)
            }
            start = text.indexOf(query, end, ignoreCase = !matchCase)
        }
        return matches
    }

    fun match(start: Int, endExclusive: Int, wholeWord: Boolean = false): PdfTextMatch? {
        if (start < 0 || endExclusive > haystack.length || start >= endExclusive) return null
        val rawStart = normalizedToRaw[start]
        val rawEnd = normalizedToRaw[endExclusive - 1]
        if (wholeWord && (!boundary(rawStart - 1) || !boundary(rawEnd + 1))) return null
        val first = rawGlyph[rawStart]
        val last = rawGlyph[rawEnd]
        if (first < 0 || last < first) return null
        val selection = geometry.selectionFor(glyphs.subList(first, last + 1))
        return if (selection.rects.isEmpty()) null else PdfTextMatch(pageIndex, selection)
    }

    fun find(query: String, wholeWord: Boolean = false, limit: Int = Int.MAX_VALUE): List<PdfTextMatch> {
        val target = normalizeForSearch(query)
        if (target.isEmpty() || glyphs.isEmpty()) return emptyList()
        val matches = mutableListOf<PdfTextMatch>()
        var from = 0
        while (matches.size < limit) {
            val start = haystack.indexOf(target, from)
            if (start < 0) break
            match(start, start + target.length, wholeWord)?.let { matches += it }
            from = start + target.length
        }
        if (matches.isEmpty() && target.length >= 48) {
            val head = target.take(24)
            val tail = target.takeLast(24)
            var headStart = haystack.indexOf(head)
            while (headStart >= 0 && matches.size < limit) {
                val tailStart = haystack.indexOf(tail, headStart + head.length)
                if (tailStart >= 0 && tailStart + tail.length - headStart <= target.length * 1.4f) {
                    match(headStart, tailStart + tail.length, wholeWord)?.let { matches += it }
                }
                headStart = haystack.indexOf(head, headStart + head.length)
            }
        }
        return matches
    }

    fun findNextSequential(target: String, fromIndex: Int): Pair<Int, PdfTextMatch>? {
        if (target.isEmpty() || haystack.isEmpty() || fromIndex >= haystack.length) return null
        val maxLookahead = 250
        val limit = minOf(haystack.length, fromIndex + maxLookahead)

        // 1. Direct continuation at fromIndex with wholeWord = true
        if (haystack.startsWith(target, fromIndex)) {
            val m = match(fromIndex, fromIndex + target.length, wholeWord = true)
            if (m != null) return fromIndex to m
        }

        // 2. Search forward within lookahead window for wholeWord = true
        var idx = haystack.indexOf(target, fromIndex)
        while (idx in fromIndex until limit) {
            val m = match(idx, idx + target.length, wholeWord = true)
            if (m != null) return idx to m
            idx = haystack.indexOf(target, idx + 1)
        }

        // 3. Fallback: direct continuation at fromIndex with wholeWord = false
        if (haystack.startsWith(target, fromIndex)) {
            val m = match(fromIndex, fromIndex + target.length, wholeWord = false)
            if (m != null) return fromIndex to m
        }

        // 4. Fallback: search forward within lookahead window with wholeWord = false
        idx = haystack.indexOf(target, fromIndex)
        while (idx in fromIndex until limit) {
            val m = match(idx, idx + target.length, wholeWord = false)
            if (m != null) return idx to m
            idx = haystack.indexOf(target, idx + 1)
        }

        return null
    }

    fun locatePlanWords(segments: List<SpeechSegment>): List<SpeechSegment> {
        var cursor = 0
        return segments.map { segment ->
            if (segment.skipped) {
                val header = normalizeForSearch(segment.anchorText.take(40))
                if (header.length >= 4) {
                    val pos = haystack.indexOf(header, cursor)
                    if (pos in cursor until minOf(haystack.length, cursor + 500)) {
                        cursor = pos + header.length
                    }
                }
                segment.copy(pageIndex = pageIndex)
            } else {
                val header = normalizeForSearch(segment.anchorText.take(40))
                if (header.length >= 4) {
                    val pos = haystack.indexOf(header, cursor).takeIf { it >= 0 }
                        ?: header.takeIf { it.length >= 12 }?.let { haystack.indexOf(it) }
                        ?: -1
                    if (pos >= 0) {
                        cursor = pos
                    }
                } else if (segment.wordOffsets.isNotEmpty()) {
                    val firstWord = normalizeForSearch(
                        segment.wordOffsets.firstOrNull { it.anchorWord != null }?.anchorWord.orEmpty(),
                    )
                    if (firstWord.length >= 3) {
                        val pos = haystack.indexOf(firstWord, cursor)
                        if (pos in cursor until minOf(haystack.length, cursor + 300)) {
                            cursor = pos
                        }
                    }
                }

                val updatedWords = segment.wordOffsets.map { word ->
                    if (word.anchorWord == null) return@map word.copy(rects = emptyList())
                    val target = normalizeForSearch(word.anchorWord)
                    if (target.isEmpty()) {
                        word.copy(rects = emptyList())
                    } else {
                        val matchResult = findNextSequential(target, cursor)
                        if (matchResult != null) {
                            cursor = matchResult.first + target.length
                            word.copy(
                                rects = matchResult.second.selection.rects.map {
                                    listOf(it.left, it.top, it.right, it.bottom)
                                },
                            )
                        } else {
                            word.copy(rects = emptyList())
                        }
                    }
                }
                segment.copy(
                    pageIndex = pageIndex,
                    wordOffsets = updatedWords,
                )
            }
        }
    }
}

/**
 * Locates [query] in the page's glyph stream. Matching ignores case, whitespace, punctuation, line-break
 * hyphenation and ligatures because OCR Markdown and the PDF text layer rarely agree character-for-character.
 * Long phrases fall back to matching their beginning and end so small OCR differences in the middle still anchor.
 */
fun PdfPageText.find(query: String, wholeWord: Boolean = false, limit: Int = Int.MAX_VALUE): List<PdfTextMatch> =
    index().find(query, wholeWord, limit)
