package com.akram.bensalem.paper.connekt.feature.assistant

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.asImageBitmap
import com.akram.bensalem.paper.connekt.core.settings.requireAndroidApplicationContext
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

actual suspend fun openPdfDocument(bytes: ByteArray): PdfDocument = withContext(Dispatchers.IO) {
    require(bytes.size >= 5 && bytes.copyOfRange(0, 5).decodeToString() == "%PDF-") {
        "The selected file is not a valid PDF."
    }
    val file = File.createTempFile("paperconnekt-", ".pdf", cacheDir())
    try {
        file.writeBytes(bytes)
        PDFBoxResourceLoader.init(requireAndroidApplicationContext())
        val textDocument = PDDocument.load(file)
        try {
            AndroidPdfDocument(file, textDocument)
        } catch (exception: Exception) {
            textDocument.close()
            throw exception
        }
    } catch (exception: Exception) {
        file.delete()
        throw exception
    }
}

private fun cacheDir(): File =
    File(com.akram.bensalem.paper.connekt.core.settings.requireAndroidApplicationContext().cacheDir, "pdf").apply {
        check(isDirectory || mkdirs()) { "Could not create temporary PDF storage." }
    }

private class AndroidPdfDocument(
    private val file: File,
    private val textDocument: PDDocument,
) : PdfDocument {
    private val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    private val renderer = try {
        PdfRenderer(descriptor)
    } catch (exception: Exception) {
        descriptor.close()
        throw exception
    }
    override val pageCount: Int = renderer.pageCount
    private var closed = false
    private val pageGlyphs = mutableMapOf<Int, List<PdfGlyph>>()

    override suspend fun render(pageIndex: Int, scale: Float): PdfPage = withContext(Dispatchers.IO) {
        synchronized(renderer) {
            check(!closed) { "PDF is closed." }
            require(pageIndex in 0 until pageCount) { "PDF page is out of range." }
            renderer.openPage(pageIndex).use { page ->
                val source = textDocument.getPage(pageIndex)
                val crop = source.cropBox
                val geometry = PdfGeometry(
                    crop.lowerLeftX, crop.lowerLeftY, crop.width, crop.height,
                    ((source.rotation % 360) + 360) % 360,
                )
                val glyphs = pageGlyphs.getOrPut(pageIndex) {
                    if (pageGlyphs.size >= 64) pageGlyphs.clear()
                    extractGlyphs(pageIndex, geometry)
                }
                val width = geometry.displayWidth
                val height = geometry.displayHeight
                val safeScale = minOf(scale.coerceIn(0.1f, 8f), 4096f / maxOf(width, height))
                val bitmap = Bitmap.createBitmap(
                    (width * safeScale).toInt().coerceAtLeast(1),
                    (height * safeScale).toInt().coerceAtLeast(1),
                    Bitmap.Config.ARGB_8888,
                )
                bitmap.eraseColor(Color.WHITE)
                page.render(
                    bitmap,
                    null,
                    Matrix().apply {
                        setScale(width * safeScale / page.width, height * safeScale / page.height)
                    },
                    PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY,
                )
                PdfPage(
                    image = bitmap.asImageBitmap(),
                    geometry = geometry,
                    glyphs = glyphs,
                )
            }
        }
    }

    private fun extractGlyphs(pageIndex: Int, geometry: PdfGeometry): List<PdfGlyph> {
        val glyphs = mutableListOf<PdfGlyph>()
        val stripper = object : PDFTextStripper() {
            override fun writeString(text: String, positions: MutableList<TextPosition>) {
                positions.forEach { position ->
                    val textGeometry = geometry.copy(rotation = position.dir.toInt())
                    val textRect = Rect(
                        position.xDirAdj, position.yDirAdj - position.heightDir,
                        position.xDirAdj + position.widthDirAdj, position.yDirAdj,
                    )
                    glyphs += PdfGlyph(
                        position.unicode,
                        geometry.displayRect(textGeometry.pdfRect(textRect)),
                    )
                }
            }
        }
        stripper.sortByPosition = true
        stripper.startPage = pageIndex + 1
        stripper.endPage = pageIndex + 1
        stripper.getText(textDocument)
        return glyphs
    }

    override fun close() {
        synchronized(renderer) {
            if (closed) return
            closed = true
            pageGlyphs.clear()
            try {
                renderer.close()
            } finally {
                try {
                    descriptor.close()
                } finally {
                    try {
                        textDocument.close()
                    } finally {
                        file.delete()
                    }
                }
            }
        }
    }
}
