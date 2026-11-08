package com.akram.bensalem.paper.connekt.feature.assistant

import kotlinx.serialization.Serializable

/** Visible page box in PDF points (crop box) and its clockwise rotation. */
@Serializable
data class PdfPageBox(val left: Float, val bottom: Float, val width: Float, val height: Float, val rotation: Int) {
    val displayWidth: Float get() = if (rotation % 180 == 0) width else height
    val displayHeight: Float get() = if (rotation % 180 == 0) height else width
}

/**
 * A glyph as reported by the PDF text engine, in its text-direction space: [x], [y] is the
 * baseline origin and [direction] the text rotation in degrees.
 */
@Serializable
data class RawPdfGlyph(
    val text: String,
    val direction: Int,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
)

/** Upper bound of the rendered bitmap side, keeping raster memory reasonable for very large pages. */
const val PDF_MAX_RASTER_SIDE = 4096f

fun PdfPageBox.safeRasterScale(scale: Float): Float =
    minOf(scale.coerceIn(0.1f, 8f), PDF_MAX_RASTER_SIDE / maxOf(displayWidth, displayHeight))

@Serializable
data class RemotePdfDocument(val id: String, val pageCount: Int)

/** A page rendered by the server: PNG bitmap (base64) plus the geometry and text layer. */
@Serializable
data class RemotePdfPage(val box: PdfPageBox, val glyphs: List<RawPdfGlyph>, val pngBase64: String)
