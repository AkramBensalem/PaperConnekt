package com.akram.bensalem.paper.connekt.feature.assistant

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import kotlin.io.encoding.Base64


class PdfMarkdownException(message: String, cause: Throwable? = null) : Exception(message, cause)

interface PdfMarkdownConverter {
    suspend fun convert(pdf: ByteArray, fileName: String, apiKey: String): OcrDocument
}


private val markdownPageMarker = Regex("(?m)^<!-- paperconnekt:ocr-page \\d+ -->\\r?$")

internal fun hasMarkdownPages(markdown: String): Boolean = markdownPageMarker.containsMatchIn(markdown)

internal fun markdownPages(markdown: String): List<String> {
    if (!hasMarkdownPages(markdown)) return listOf(markdown)
    return markdownPageMarker.split(markdown).drop(1).map { it.trim() }
}

internal fun pagedMarkdown(pages: List<String>): String =
    pages.mapIndexed { index, text -> "<!-- paperconnekt:ocr-page $index -->\n\n${text.trim()}" }
        .joinToString("\n\n")


/** Rewrites Mistral image references `](img-0.jpeg)` to `](figures/<safe id>)`. */
internal fun linkOcrFigures(markdown: String, imageIds: Collection<String>): String =
    imageIds.fold(markdown) { text, id -> text.replace("]($id)", "](figures/${safePathSegment(id)})") }

@Serializable
internal data class MistralUpload(val id: String)

@Serializable
internal data class MistralSignedUrl(val url: String)

@Serializable
internal data class MistralOcrResponse(val pages: List<Page> = emptyList()) {
    @Serializable
    data class Page(
        val index: Int = 0,
        val markdown: String = "",
        val images: List<Image> = emptyList(),
        val header: String? = null,
        val footer: String? = null,
        val blocks: List<Block> = emptyList(),
        val dimensions: Dimensions? = null,
    )

    @Serializable
    data class Block(
        val type: String = "text",
        val content: String? = null,
        @SerialName("top_left_x") val left: Float? = null,
        @SerialName("top_left_y") val top: Float? = null,
        @SerialName("bottom_right_x") val right: Float? = null,
        @SerialName("bottom_right_y") val bottom: Float? = null,
    )

    @Serializable
    data class Dimensions(val width: Float = 0f, val height: Float = 0f)

    @Serializable
    data class Image(val id: String, @SerialName("image_base64") val imageBase64: String? = null)
}

internal fun decodeOcrImage(value: String?): ByteArray? {
    if (value.isNullOrBlank()) return null
    val payload = if (value.startsWith("data:")) value.substringAfter(',', "") else value
    return try {
        Base64.Mime.decode(payload)
    } catch (_: IllegalArgumentException) {
        null
    }
}

internal fun ocrLayout(pages: List<MistralOcrResponse.Page>, model: String): OcrLayout = OcrLayout(
    model = model,
    pages = pages.map { page ->
        val width = page.dimensions?.width?.takeIf { it > 0f }
        val height = page.dimensions?.height?.takeIf { it > 0f }
        OcrPageLayout(
            index = page.index,
            header = page.header?.takeIf { it.isNotBlank() },
            footer = page.footer?.takeIf { it.isNotBlank() },
            blocks = page.blocks.mapNotNull { block ->
                val text = block.content?.trim().orEmpty()
                if (text.isEmpty() && !block.type.contains("image", ignoreCase = true)) return@mapNotNull null
                val box = if (width != null && height != null && block.left != null && block.top != null &&
                    block.right != null && block.bottom != null
                ) {
                    listOf(block.left / width, block.top / height, block.right / width, block.bottom / height)
                        .map { it.coerceIn(0f, 1f) }
                } else {
                    null
                }
                OcrBlock(type = block.type.lowercase(), text = text, box = box)
            },
        )
    },
)

private fun defaultMistralHttpClient() = HttpClient {
    expectSuccess = false
    install(HttpTimeout) {
        connectTimeoutMillis = 30_000
        socketTimeoutMillis = 10 * 60_000
        requestTimeoutMillis = 15 * 60_000
    }
}

/** Upload → signed URL → OCR flow, mirroring the DocumentConverter implementation. */
class MistralOcrConverter(
    private val http: HttpClient = defaultMistralHttpClient(),
    private val baseUrl: String = "https://api.mistral.ai",
    private val model: String = MISTRAL_OCR_MODEL,
    private val retryDelaysMillis: List<Long> = listOf(2_000, 5_000, 10_000),
) : PdfMarkdownConverter {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun convert(pdf: ByteArray, fileName: String, apiKey: String): OcrDocument {
        val key = apiKey.trim()
        if (key.isEmpty()) throw PdfMarkdownException("Add your Mistral API key in Settings → API keys.")
        val upload = try {
            val response = http.submitFormWithBinaryData("$baseUrl/v1/files", formData {
                append("purpose", "ocr")
                append("file", pdf, Headers.build {
                    append(HttpHeaders.ContentDisposition, "filename=\"${safePathSegment(fileName)}\"")
                    append(HttpHeaders.ContentType, ContentType.Application.Pdf.toString())
                })
            }) { bearerAuth(key) }
            decode<MistralUpload>(response, "upload the PDF")
        } catch (exception: Exception) {
            throw wrap(exception)
        }
        try {
            val signed = decode<MistralSignedUrl>(http.get("$baseUrl/v1/files/${upload.id}/url") {
                bearerAuth(key)
                parameter("expiry", 1)
            }, "prepare the PDF")
            val body = buildJsonObject {
                put("model", model)
                putJsonObject("document") {
                    put("type", "document_url")
                    put("document_url", signed.url)
                }
                put("include_image_base64", true)
                put("include_blocks", true)
                put("extract_header", true)
                put("extract_footer", true)
            }.toString()
            val result = ocrWithRetry(body, key)
            val pages = result.pages.sortedBy { it.index }
            val images = pages.flatMap { it.images }
                .mapNotNull { image -> decodeOcrImage(image.imageBase64)?.let { image.id to it } }
                .toMap()
            if (pages.none { it.markdown.isNotBlank() }) throw PdfMarkdownException("Mistral OCR returned no text for this PDF.")
            if (pages.any { it.index < 0 } || pages.map { it.index }.distinct().size != pages.size) {
                throw PdfMarkdownException("Mistral OCR returned invalid page numbers.")
            }
            val byIndex = pages.associateBy { it.index }
            val markdown = linkOcrFigures(pagedMarkdown(
                List(pages.last().index + 1) { byIndex[it]?.markdown.orEmpty() }
            ), images.keys)
            return OcrDocument(markdown, images.mapKeys { safePathSegment(it.key) }, ocrLayout(pages, model))
        } catch (exception: Exception) {
            throw wrap(exception)
        } finally {
            withContext(NonCancellable) {
                try {
                    http.delete("$baseUrl/v1/files/${upload.id}") { bearerAuth(key) }
                } catch (exception: Exception) {
                    if (exception is CancellationException) throw exception
                }
            }
        }
    }

    private suspend fun ocrWithRetry(body: String, key: String): MistralOcrResponse {
        var attempt = 0
        while (true) {
            val response = http.post("$baseUrl/v1/ocr") {
                bearerAuth(key)
                contentType(ContentType.Application.Json)
                setBody(body)
            }
            val status = response.status.value
            if (status in transientStatuses && attempt < retryDelaysMillis.size) {
                delay(retryDelaysMillis[attempt++])
                continue
            }
            return decode(response, "convert the PDF")
        }
    }

    private suspend inline fun <reified T> decode(response: HttpResponse, action: String): T {
        val status = response.status.value
        val text = response.bodyAsText()
        if (status !in 200..299) {
            throw PdfMarkdownException(when (status) {
                401 -> "Mistral rejected your API key. Update it in Settings → API keys."
                402 -> "Your Mistral account has no remaining credit for OCR."
                403 -> "Your Mistral API key cannot access $MISTRAL_OCR_MODEL_NAME."
                413 -> "This PDF is too large for Mistral OCR."
                429 -> "Mistral rate limit reached. Wait a moment and try again."
                in 500..599 -> "Mistral OCR is unavailable right now (HTTP $status). Try again later."
                else -> "Could not $action with Mistral OCR (HTTP $status)."
            })
        }
        return try {
            json.decodeFromString<T>(text)
        } catch (exception: SerializationException) {
            throw PdfMarkdownException("Mistral OCR returned an unexpected response.", exception)
        }
    }

    private fun wrap(exception: Exception): Exception = when (exception) {
        is CancellationException, is PdfMarkdownException -> exception
        else -> PdfMarkdownException("Could not reach Mistral OCR. Check your internet connection and try again.", exception)
    }

    private companion object {
        val transientStatuses = setOf(408, 429, 500, 502, 503, 504)
    }
}
