package com.akram.bensalem.paper.connekt.feature.assistant

import androidx.compose.ui.geometry.Rect
import kotlin.math.abs

/**
 * Reading-order reconstruction for one PDF page.
 *
 * PDF text layers only contain positioned glyphs, so multi-column papers, superscript markers and
 * letter-spaced labels must be rebuilt geometrically for accurate text selection:
 * glyphs → baseline rows → words → recursive XY-cut (columns first, then bands) → paragraph blocks.
 */
internal data class LayoutWord(val glyphs: List<PdfGlyph>, val text: String, val box: Rect, val row: Int)

internal data class LayoutLine(val words: List<LayoutWord>) {
    val box: Rect = words.map { it.box }.reduce(Rect::union)
    val text: String = words.joinToString(" ") { it.text }
    val size: Float = words.flatMap { it.glyphs }.map { it.bounds.height }.median()
}

internal data class LayoutBlock(val lines: List<LayoutLine>) {
    val box: Rect = lines.map { it.box }.reduce(Rect::union)
    val size: Float = lines.flatMap { line -> line.words.flatMap { it.glyphs } }.map { it.bounds.height }.median()
    val glyphs: List<PdfGlyph> get() = lines.flatMap { line -> line.words.flatMap { it.glyphs } }

    /** Lines joined into readable text; end-of-line hyphenation ("differ-" + "ent") is repaired. */
    val text: String
        get() = buildString {
            lines.forEach { line ->
                val next = line.text
                if (isEmpty()) {
                    append(next)
                } else if (length >= 2 && last() == '-' && this[length - 2].isLowerCase() && next.firstOrNull()?.isLowerCase() == true) {
                    setLength(length - 1)
                    append(next)
                } else {
                    append(' ').append(next)
                }
            }
        }
}

internal data class PdfPageLayout(
    val blocks: List<LayoutBlock>,
    val bodySize: Float,
    val pageHeight: Float,
) {
    /** Glyphs in reading order so sequential word lookup follows columns rather than content-stream order. */
    val orderedGlyphs: List<PdfGlyph> get() = blocks.flatMap { it.glyphs }

    fun edgeLines(band: Float = 0.09f): List<LayoutBlock> = blocks.filter { isInEdgeBand(it, band) }

    fun isInEdgeBand(block: LayoutBlock, band: Float = 0.09f): Boolean =
        block.box.bottom <= pageHeight * band || block.box.top >= pageHeight * (1f - band)
}

internal fun List<Float>.median(): Float = if (isEmpty()) 0f else sorted()[size / 2]

private fun Rect.union(other: Rect) = Rect(
    minOf(left, other.left), minOf(top, other.top), maxOf(right, other.right), maxOf(bottom, other.bottom),
)

private val superscriptMarker = Regex("[\\d,*†‡§¶∗⁎·a-e]+")

internal fun analyzePdfLayout(page: PdfPage): PdfPageLayout = analyzePdfLayout(page.geometry, page.glyphs)

internal fun analyzePdfLayout(geometry: PdfGeometry, pageGlyphs: List<PdfGlyph>): PdfPageLayout {
    val glyphs = pageGlyphs.filter { it.text.isNotBlank() && it.bounds.width > 0f && it.bounds.height > 0f }
    val pageHeight = geometry.displayHeight
    if (glyphs.isEmpty()) return PdfPageLayout(emptyList(), 0f, pageHeight)
    val bodySize = glyphs.map { it.bounds.height }.median()

    // 1. Baseline rows. Superscripts sit above the baseline with a smaller font, so they are kept in the
    // row they decorate (and later dropped) instead of becoming a stray "1 2 3" line.
    val rows = mutableListOf<MutableList<PdfGlyph>>()
    glyphs.sortedWith(compareByDescending<PdfGlyph> { it.bounds.height }.thenBy { it.bounds.bottom }).forEach { glyph ->
        val row = rows.firstOrNull { row ->
            val base = row.first().bounds
            abs(base.bottom - glyph.bounds.bottom) <= maxOf(base.height, glyph.bounds.height) * 0.3f ||
                (glyph.bounds.height < base.height * 0.85f &&
                    glyph.bounds.bottom < base.bottom &&
                    glyph.bounds.bottom > base.top + base.height * 0.1f &&
                    glyph.bounds.top >= base.top - base.height * 0.45f &&
                    row.any { abs(it.bounds.left - glyph.bounds.right) < base.height * 0.6f || abs(glyph.bounds.left - it.bounds.right) < base.height * 0.6f })
        }
        if (row == null) rows += mutableListOf(glyph) else row += glyph
    }

    // 2. Words per row, without superscript markers and with letter-spaced labels repaired.
    val words = mutableListOf<LayoutWord>()
    rows.forEachIndexed { rowIndex, row ->
        // Compare against the row's typical glyph, not its tallest: a row can span both columns and contain
        // tall math symbols, which would otherwise make ordinary letters look like raised footnote markers.
        val baseline = row.map { it.bounds.bottom }.median()
        val typicalHeight = row.map { it.bounds.height }.median()
        val content = row.sortedBy { it.bounds.left }.filterNot { glyph ->
            glyph.bounds.height < typicalHeight * 0.85f &&
                glyph.bounds.bottom < baseline - typicalHeight * 0.15f &&
                glyph.text.matches(superscriptMarker)
        }
        val pieces = mutableListOf<MutableList<PdfGlyph>>()
        content.forEach { glyph ->
            val previous = pieces.lastOrNull()?.last()
            val gap = previous?.let { glyph.bounds.left - it.bounds.right } ?: Float.MAX_VALUE
            if (previous == null || gap > maxOf(previous.bounds.height, glyph.bounds.height) * 0.2f) {
                pieces += mutableListOf(glyph)
            } else {
                pieces.last() += glyph
            }
        }
        mergeLetterSpacedRuns(pieces).forEach { piece ->
            words += LayoutWord(piece, piece.joinToString("") { it.text }.trim(), piece.map { it.bounds }.reduce(Rect::union), rowIndex)
        }
    }

    // 3. Recursive XY-cut into reading-ordered regions, then paragraph blocks.
    val blocks = cut(words.filter { it.text.isNotEmpty() }, bodySize).flatMap { leaf -> paragraphBlocks(leaf, bodySize) }
    return PdfPageLayout(blocks, bodySize, pageHeight)
}

/** "A D V A N C E D  R E V I E W" → "ADVANCED", "REVIEW" using the wider inter-word gap. */
private fun mergeLetterSpacedRuns(pieces: List<MutableList<PdfGlyph>>): List<List<PdfGlyph>> {
    val result = mutableListOf<List<PdfGlyph>>()
    var index = 0
    while (index < pieces.size) {
        var end = index
        while (end < pieces.size && pieces[end].size == 1 && pieces[end][0].text.trim().let { it.length == 1 && it[0].isLetter() }) end++
        if (end - index >= 3) {
            val run = pieces.subList(index, end).map { it[0] }
            val gaps = run.zipWithNext { a, b -> b.bounds.left - a.bounds.right }
            val letterGap = gaps.median()
            var current = mutableListOf(run.first())
            run.drop(1).forEachIndexed { gapIndex, glyph ->
                if (gaps[gapIndex] > letterGap * 1.6f + glyph.bounds.height * 0.05f) {
                    result += current
                    current = mutableListOf(glyph)
                } else {
                    current += glyph
                }
            }
            result += current
            index = end
        } else {
            result += pieces[index]
            index++
        }
    }
    return result
}

private sealed interface LayoutNode {
    data class Leaf(val words: List<LayoutWord>) : LayoutNode
    data class Columns(val children: List<LayoutNode>, val gutters: List<ClosedFloatingPointRange<Float>>) : LayoutNode
    data class Rows(val children: List<LayoutNode>) : LayoutNode
}

private fun cut(words: List<LayoutWord>, bodySize: Float): List<List<LayoutWord>> = flatten(node(words, bodySize, 0))

private fun node(words: List<LayoutWord>, bodySize: Float, depth: Int): LayoutNode {
    if (words.size < 2 || depth > 24) return LayoutNode.Leaf(words)
    val distinctRows = words.map { it.row }.distinct().size
    val size = words.flatMap { it.glyphs }.map { it.bounds.height }.median().coerceAtLeast(bodySize * 0.6f)

    // Columns: a vertical gutter crossed by no word, only meaningful when several rows share it.
    if (distinctRows >= 2) {
        val gaps = gaps(words.map { it.box.left to it.box.right }, size * 1.1f)
        if (gaps.isNotEmpty()) {
            val parts = split(words, gaps) { it.box.center.x }
            // Narrow parts are section numbers, bullets or labels hanging beside text, not columns.
            val wideEnough = parts.all { part -> part.maxOf { it.box.right } - part.minOf { it.box.left } >= size * 5f }
            if (parts.size > 1 && wideEnough && parts.count { part -> part.map { it.row }.distinct().size >= 2 } >= 1) {
                return LayoutNode.Columns(parts.map { node(it, bodySize, depth + 1) }, gaps)
            }
        }
    }
    // Bands: horizontal whitespace crossed by no word.
    // Only gaps clearly wider than normal line spacing separate bands.
    val rawBandGaps = gaps(words.map { it.box.top to it.box.bottom }, size * 0.3f)
    val sortedGaps = rawBandGaps.map { it.endInclusive - it.start }.sorted()
    val lineGap = if (sortedGaps.size < 2) 0f else sortedGaps[(sortedGaps.size - 1) * 3 / 10]
    val bandGaps = rawBandGaps.filter { it.endInclusive - it.start >= maxOf(size * 1.1f, lineGap * 1.35f) }
    if (bandGaps.isNotEmpty()) {
        val parts = split(words, bandGaps) { it.box.center.y }
        if (parts.size > 1) return mergeAlignedColumns(LayoutNode.Rows(parts.map { node(it, bodySize, depth + 1) }))
    }
    return LayoutNode.Leaf(words)
}

private fun gaps(intervals: List<Pair<Float, Float>>, minimum: Float): List<ClosedFloatingPointRange<Float>> {
    val sorted = intervals.sortedBy { it.first }
    val result = mutableListOf<ClosedFloatingPointRange<Float>>()
    var reach = sorted.first().second
    sorted.drop(1).forEach { (start, end) ->
        if (start - reach >= minimum) result += reach..start
        reach = maxOf(reach, end)
    }
    return result
}

private fun split(
    words: List<LayoutWord>,
    gaps: List<ClosedFloatingPointRange<Float>>,
    position: (LayoutWord) -> Float,
): List<List<LayoutWord>> {
    val parts = List(gaps.size + 1) { mutableListOf<LayoutWord>() }
    words.forEach { word ->
        val value = position(word)
        parts[gaps.count { value > it.start }] += word
    }
    return parts.filter { it.isNotEmpty() }
}

/**
 * Paragraph gaps that happen to align in both columns create separate bands; consecutive bands with the
 * same gutter are joined back so the whole left column is read before the right one.
 */
private fun mergeAlignedColumns(rows: LayoutNode.Rows): LayoutNode {
    val merged = mutableListOf<LayoutNode>()
    rows.children.forEach { child ->
        val previous = merged.lastOrNull()
        if (previous is LayoutNode.Columns && child is LayoutNode.Columns &&
            previous.children.size == child.children.size &&
            previous.gutters.size == child.gutters.size &&
            previous.gutters.zip(child.gutters).all { (a, b) -> a.start <= b.endInclusive && b.start <= a.endInclusive }
        ) {
            merged[merged.lastIndex] = LayoutNode.Columns(
                previous.children.zip(child.children) { a, b -> LayoutNode.Rows(listOf(a, b)) },
                previous.gutters.zip(child.gutters) { a, b -> maxOf(a.start, b.start)..minOf(a.endInclusive, b.endInclusive) },
            )
        } else {
            merged += child
        }
    }
    return if (merged.size == 1) merged.single() else LayoutNode.Rows(merged)
}

private fun flatten(node: LayoutNode): List<List<LayoutWord>> = when (node) {
    is LayoutNode.Leaf -> listOf(node.words)
    is LayoutNode.Columns -> node.children.flatMap(::flatten)
    is LayoutNode.Rows -> node.children.flatMap(::flatten)
}

private fun paragraphBlocks(words: List<LayoutWord>, bodySize: Float): List<LayoutBlock> {
    val lines = words.groupBy { it.row }.values
        .map { LayoutLine(it.sortedBy { word -> word.box.left }) }
        .sortedBy { it.box.bottom }
    if (lines.isEmpty()) return emptyList()
    val left = lines.minOf { it.box.left }
    val right = lines.maxOf { it.box.right }
    val typicalGap = lines.zipWithNext { a, b -> b.box.top - a.box.bottom }.filter { it > 0f }.median()
    val blocks = mutableListOf<MutableList<LayoutLine>>()
    lines.forEach { line ->
        val current = blocks.lastOrNull()
        val previous = current?.last()
        val newBlock = previous == null || run {
            val size = maxOf(previous.size, line.size, bodySize * 0.6f)
            val gap = line.box.top - previous.box.bottom
            val sizeChanged = abs(line.size - previous.size) > minOf(line.size, previous.size) * 0.18f
            val indented = line.box.left - left > size * 0.9f && previous.box.left - left < size * 0.5f
            val previousEndsSentence = previous.text.trimEnd().lastOrNull()?.let { it in ".!?:" } == true
            val previousShort = right - previous.box.right > size * 3f
            gap > maxOf(typicalGap + size * 0.45f, size * 0.6f) || sizeChanged || (indented && previousEndsSentence) || (previousShort && previousEndsSentence && lines.size > 2)
        }
        if (newBlock) blocks += mutableListOf(line) else current!! += line
    }
    return blocks.map(::LayoutBlock)
}

/** Text signature of a header/footer line that survives changing page numbers. */
internal fun runningSignature(text: String): String = normalizeForSearch(text).filterNot(Char::isDigit)

/** Signatures of short top/bottom lines that appear on the given pages; used to drop running headers. */
internal fun runningSignatures(pages: List<PdfPageText>): Set<String> {
    val perPage = pages.map { pageText ->
        val layout = analyzePdfLayout(pageText.geometry, pageText.glyphs)
        layout.edgeLines().flatMap { block -> block.lines.map { runningSignature(it.text) } + runningSignature(block.text) }
            .filter { it.length >= 4 }
            .toSet()
    }
    return perPage.flatten().groupingBy { it }.eachCount().filterValues { it >= 2 }.keys
}

/** Markdown pages: repeated first/last lines (OCR running headers and footers). */
internal fun markdownRunningSignatures(pages: List<String>): Set<String> {
    if (pages.size < 2) return emptySet()
    return pages.flatMap { page ->
        val lines = page.lines().map { it.replace(Regex("[*_#>`]"), "").trim() }.filter { it.isNotBlank() }
        (lines.take(3) + lines.takeLast(3)).map(::runningSignature).filter { it.length >= 4 }.distinct()
    }.groupingBy { it }.eachCount().filterValues { it >= 2 }.keys
}
