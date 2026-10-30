package com.akram.bensalem.paper.connekt.feature.assistant

import kotlinx.serialization.Serializable

const val MISTRAL_OCR_MODEL = "mistral-ocr-4-1"
const val MISTRAL_OCR_MODEL_NAME = "Mistral OCR 4.1"

/** Markdown produced by OCR, with extracted figures keyed by the id referenced in the markdown. */
class OcrDocument(
    val markdown: String,
    val images: Map<String, ByteArray> = emptyMap(),
    /** Typed, positioned blocks (title, text, header, footer, references…) used to build the reading script. */
    val layout: OcrLayout? = null,
)

/** One OCR content block; [box] is `left, top, right, bottom` normalized to 0..1 of the page. */
@Serializable
data class OcrBlock(val type: String, val text: String, val box: List<Float>? = null)

@Serializable
data class OcrPageLayout(
    val index: Int,
    val header: String? = null,
    val footer: String? = null,
    val blocks: List<OcrBlock> = emptyList(),
)

@Serializable
data class OcrLayout(val model: String = MISTRAL_OCR_MODEL, val pages: List<OcrPageLayout> = emptyList())

/** Persists the hidden Markdown version of a PDF so it is converted only once. */
interface PdfMarkdownStore {
    val syncError: String? get() = null

    /** Returns render-ready markdown (figure links resolved), or null when nothing is saved. */
    suspend fun load(key: String): String?

    /** Saves [document] and returns render-ready markdown. */
    suspend fun save(key: String, document: OcrDocument): String

    /** Portable source, with relative figure links and their bytes, never device-local file URLs. */
    suspend fun loadDocument(key: String): OcrDocument? =
        load(key)?.let { OcrDocument(it, layout = loadLayout(key)) }

    suspend fun hasPendingSync(key: String): Boolean = false

    suspend fun setPendingSync(key: String, pending: Boolean) = Unit

    /** OCR blocks saved with the Markdown, or null for conversions made before blocks were stored. */
    suspend fun loadLayout(key: String): OcrLayout? = null

}

expect fun defaultPdfMarkdownStore(): PdfMarkdownStore

fun safePathSegment(value: String): String =
    value.map { if (it.isLetterOrDigit() || it == '.' || it == '-' || it == '_') it else '_' }
        .joinToString("").trim('.').ifEmpty { "_" }

fun ocrFigureIds(markdown: String): Set<String> =
    Regex("""\]\(figures/([^/)]+)\)""").findAll(markdown)
        .map { it.groupValues[1] }
        .onEach { require(it == safePathSegment(it)) { "Invalid OCR figure filename." } }
        .toSet()

/** Serializable form of [OcrDocument] used between the web client and the server (figures in base64). */
@Serializable
data class OcrDocumentPayload(
    val markdown: String,
    val images: Map<String, String> = emptyMap(),
    val layout: OcrLayout? = null,
) {
    fun toDocument(): OcrDocument = OcrDocument(markdown, images.mapValues { kotlin.io.encoding.Base64.decode(it.value) }, layout)

    companion object {
        fun of(document: OcrDocument): OcrDocumentPayload =
            OcrDocumentPayload(document.markdown, document.images.mapValues { kotlin.io.encoding.Base64.encode(it.value) }, document.layout)
    }
}
