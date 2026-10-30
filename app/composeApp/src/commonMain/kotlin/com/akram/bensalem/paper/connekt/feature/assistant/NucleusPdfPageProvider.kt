package com.akram.bensalem.paper.connekt.feature.assistant

import dev.nucleusframework.pdfium.PdfReaderState

/**
 * Adapts Nucleus PDFium's point-based text layout to the legacy PaperConnekt page model.
 * The model remains in place so annotations, search, read-aloud, and AI anchoring keep their
 * existing coordinate contracts while PDFium owns document parsing and raster rendering.
 */
suspend fun PdfReaderState.loadPaperConnektPage(pageIndex: Int): PdfPage? {
    val size = pageSize(pageIndex) ?: return null
    val geometry = PdfGeometry(0f, 0f, size.widthPoints, size.heightPoints, 0)
    val layout = pageTextLayout(pageIndex)
    val glyphs = if (layout == null) {
        emptyList()
    } else {
        buildList(layout.charCount) {
            for (index in 0 until layout.charCount) {
                val text = layout.codepoint(index).toChar().toString()
                if (text.isBlank()) continue
                val rect = geometry.displayRect(
                    listOf(
                        layout.charLeft(index),
                        layout.charBottom(index),
                        layout.charRight(index),
                        layout.charTop(index),
                    )
                )
                if (rect.width > 0f && rect.height > 0f) add(PdfGlyph(text, rect))
            }
        }
    }
    return PdfPage(geometry = geometry, glyphs = glyphs)
}
