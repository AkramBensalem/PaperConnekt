package com.akram.bensalem.paper.connekt.feature.assistant

import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal suspend fun openJvmPdfDocument(bytes: ByteArray): PdfDocument = withContext(Dispatchers.IO) {
    DesktopPdfDocument(PdfBoxRasterizer.open(bytes))
}

private class DesktopPdfDocument(private val rasterizer: PdfBoxRasterizer) : PdfDocument {
    override val pageCount = rasterizer.pageCount

    override suspend fun render(pageIndex: Int, scale: Float): PdfPage = withContext(Dispatchers.Default) {
        val page = rasterizer.render(pageIndex, scale)
        val geometry = page.box.toGeometry()
        PdfPage(page.image.toComposeImageBitmap(), geometry, page.glyphs.map { it.toPdfGlyph(geometry) })
    }

    override fun close() = rasterizer.close()
}
