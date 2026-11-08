package com.akram.bensalem.paper.connekt.feature.assistant

import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.PDFRenderer
import org.apache.pdfbox.text.PDFTextStripper
import org.apache.pdfbox.text.TextPosition
import java.awt.image.BufferedImage

class RasterizedPdfPage(val box: PdfPageBox, val image: BufferedImage, val glyphs: List<RawPdfGlyph>)

/** PDFBox page renderer shared by the desktop reader and the web server. Thread-safe. */
class PdfBoxRasterizer private constructor(private val document: PDDocument) : AutoCloseable {
    val pageCount: Int = document.numberOfPages
    private val renderer = PDFRenderer(document)
    private var closed = false

    fun render(pageIndex: Int, scale: Float): RasterizedPdfPage = synchronized(document) {
        check(!closed) { "PDF is closed." }
        require(pageIndex in 0 until pageCount)
        val page = document.getPage(pageIndex)
        val crop = page.cropBox
        val box = PdfPageBox(crop.lowerLeftX, crop.lowerLeftY, crop.width, crop.height, ((page.rotation % 360) + 360) % 360)
        val image = renderer.renderImage(pageIndex, box.safeRasterScale(scale))
        val glyphs = mutableListOf<RawPdfGlyph>()
        val stripper = object : PDFTextStripper() {
            override fun writeString(text: String, positions: MutableList<TextPosition>) {
                positions.forEach { p ->
                    glyphs += RawPdfGlyph(p.unicode, p.dir.toInt(), p.xDirAdj, p.yDirAdj, p.widthDirAdj, p.heightDir)
                }
            }
        }
        stripper.sortByPosition = true
        stripper.startPage = pageIndex + 1
        stripper.endPage = pageIndex + 1
        stripper.getText(document)
        RasterizedPdfPage(box, image, glyphs)
    }

    override fun close() = synchronized(document) {
        if (!closed) {
            closed = true
            document.close()
        }
    }

    companion object {
        fun open(bytes: ByteArray): PdfBoxRasterizer {
            val document = Loader.loadPDF(bytes)
            try {
                require(document.numberOfPages > 0) { "This PDF has no pages." }
                return PdfBoxRasterizer(document)
            } catch (exception: Exception) {
                document.close()
                throw exception
            }
        }
    }
}
