package com.akram.bensalem.paper.connekt.feature.assistant

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import kotlin.math.max
import kotlin.math.min

interface PdfDocument {
    val pageCount: Int
    suspend fun render(pageIndex: Int, scale: Float): PdfPage
    fun close()
}

data class PdfGlyph(val text: String, val bounds: Rect)

data class PdfPage(
    val image: ImageBitmap,
    val geometry: PdfGeometry,
    val glyphs: List<PdfGlyph>
)

data class PdfTextSelection(val text: String, val rects: List<Rect>)

fun List<PdfGlyph>.textSelection(): PdfTextSelection {
    val lines = mutableListOf<Rect>()
    val text = StringBuilder()
    var previous: PdfGlyph? = null
    for (glyph in this) {
        if (glyph.text.isEmpty()) continue
        val last = previous
        val newLine = last != null && kotlin.math.abs(last.bounds.bottom - glyph.bounds.bottom) > maxOf(last.bounds.height, glyph.bounds.height) * 0.5f
        if (newLine && text.isNotEmpty() && !text.last().isWhitespace()) text.append('\n')
        else if (last != null && !newLine && glyph.bounds.left - last.bounds.right > maxOf(last.bounds.height, glyph.bounds.height) * 0.2f &&
            text.isNotEmpty() && !text.last().isWhitespace() && !glyph.text.first().isWhitespace()) text.append(' ')
        text.append(glyph.text)
        val line = lines.lastOrNull()
        if (line != null && !newLine && glyph.bounds.left - line.right < maxOf(line.height, glyph.bounds.height) * 2f) {
            lines[lines.lastIndex] = Rect(minOf(line.left, glyph.bounds.left), minOf(line.top, glyph.bounds.top),
                maxOf(line.right, glyph.bounds.right), maxOf(line.bottom, glyph.bounds.bottom))
        } else lines += glyph.bounds
        previous = glyph
    }
    return PdfTextSelection(text.toString(), lines.filter { it.width > 0f && it.height > 0f })
}

fun PdfPage.selectText(start: Offset, end: Offset): PdfTextSelection? {
    val selectable = glyphs.filter { it.text.isNotEmpty() && it.bounds.width > 0f && it.bounds.height > 0f }
    if (selectable.isEmpty()) return null
    val a = selectable.nearestGlyphIndex(start)
    val b = selectable.nearestGlyphIndex(end)
    return selectionFor(selectable.subList(minOf(a, b), maxOf(a, b) + 1))
}

internal fun PdfPage.selectWord(point: Offset): PdfTextSelection? {
    val selectable = glyphs.filter { it.text.isNotEmpty() && it.bounds.width > 0f && it.bounds.height > 0f }
    if (selectable.isEmpty()) return null
    val index = selectable.nearestGlyphIndex(point)
    val unrotated = geometry.copy(rotation = 0)
    fun joinsWord(left: PdfGlyph, right: PdfGlyph): Boolean {
        val a = unrotated.displayRect(geometry.pdfRect(left.bounds))
        val b = unrotated.displayRect(geometry.pdfRect(right.bounds))
        val height = maxOf(a.height, b.height)
        return !left.text.last().isWhitespace() && !right.text.first().isWhitespace() &&
            kotlin.math.abs(a.bottom - b.bottom) <= height * 0.5f &&
            b.left - a.right <= height * 0.2f
    }
    var first = index
    var last = index
    while (first > 0 && joinsWord(selectable[first - 1], selectable[first])) first--
    while (last < selectable.lastIndex && joinsWord(selectable[last], selectable[last + 1])) last++
    return selectionFor(selectable.subList(first, last + 1))
}

private fun List<PdfGlyph>.nearestGlyphIndex(point: Offset): Int =
    indices.minBy { index ->
        val rect = this[index].bounds
        val dx = maxOf(rect.left - point.x, 0f, point.x - rect.right)
        val dy = maxOf(rect.top - point.y, 0f, point.y - rect.bottom)
        dx * dx + dy * dy
    }

fun PdfPage.selectionFor(selected: List<PdfGlyph>): PdfTextSelection = geometry.selectionFor(selected)

fun PdfGeometry.selectionFor(selected: List<PdfGlyph>): PdfTextSelection {
    val unrotated = copy(rotation = 0)
    val selection = selected.map { glyph ->
        glyph.copy(bounds = unrotated.displayRect(pdfRect(glyph.bounds)))
    }.textSelection()
    return selection.copy(rects = selection.rects.map { displayRect(unrotated.pdfRect(it)) })
}

data class PdfGeometry(val left: Float, val bottom: Float, val width: Float, val height: Float, val rotation: Int) {
    val displayWidth: Float get() = if (rotation % 180 == 0) width else height
    val displayHeight: Float get() = if (rotation % 180 == 0) height else width

    fun toPdf(point: Offset): Offset = when (rotation) {
        90 -> Offset(left + point.y, bottom + point.x)
        180 -> Offset(left + width - point.x, bottom + point.y)
        270 -> Offset(left + width - point.y, bottom + height - point.x)
        else -> Offset(left + point.x, bottom + height - point.y)
    }

    fun toDisplay(point: Offset): Offset = when (rotation) {
        90 -> Offset(point.y - bottom, point.x - left)
        180 -> Offset(width - point.x + left, point.y - bottom)
        270 -> Offset(height - point.y + bottom, width - point.x + left)
        else -> Offset(point.x - left, height - point.y + bottom)
    }

    fun pdfRect(rect: Rect): List<Float> {
        val a = toPdf(rect.topLeft)
        val b = toPdf(rect.bottomRight)
        return listOf(min(a.x, b.x), min(a.y, b.y), max(a.x, b.x), max(a.y, b.y))
    }

    fun displayRect(rect: List<Float>): Rect {
        require(rect.size == 4 && rect.all { it.isFinite() }) { "Invalid annotation rectangle." }
        val a = toDisplay(Offset(rect[0], rect[1]))
        val b = toDisplay(Offset(rect[2], rect[3]))
        return Rect(min(a.x, b.x), min(a.y, b.y), max(a.x, b.x), max(a.y, b.y))
    }
}

fun PdfPageBox.toGeometry(): PdfGeometry = PdfGeometry(left, bottom, width, height, rotation)

/** Maps a glyph from its text-direction space to display coordinates of the page. */
fun RawPdfGlyph.toPdfGlyph(geometry: PdfGeometry): PdfGlyph {
    val textGeometry = geometry.copy(rotation = direction)
    val textRect = Rect(x, y - height, x + width, y)
    return PdfGlyph(text, geometry.displayRect(textGeometry.pdfRect(textRect)))
}
