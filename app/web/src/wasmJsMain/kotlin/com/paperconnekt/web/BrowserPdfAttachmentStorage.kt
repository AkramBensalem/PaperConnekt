package com.paperconnekt.web

import com.paperconnekt.storage.PdfAttachmentStorage
import com.paperconnekt.storage.requirePdfHeader
import kotlin.io.encoding.Base64
import kotlin.random.Random

class BrowserPdfAttachmentStorage : PdfAttachmentStorage {
    override suspend fun importPdf(source: String): String {
        val encoded = source.substringAfter("base64,", missingDelimiterValue = "")
        require(encoded.isNotEmpty()) { "The browser file picker did not return PDF data." }
        val bytes = Base64.decode(encoded)
        requirePdfHeader(bytes)
        val key = "pdf:${Random.nextLong().toULong().toString(16)}"
        BrowserIndexedDb.write(key, Base64.encode(bytes))
        return key
    }

    override suspend fun readPdf(path: String): ByteArray {
        val encoded = BrowserIndexedDb.read(path)
            ?: error("The attached PDF is no longer available in browser storage.")
        return Base64.decode(encoded)
    }

    override suspend fun readCachedPdf(key: String): ByteArray? =
        BrowserIndexedDb.read("cache:$key")?.let(Base64::decode)

    override suspend fun writeCachedPdf(key: String, bytes: ByteArray) {
        BrowserIndexedDb.write("cache:$key", Base64.encode(bytes))
    }

    override suspend fun readAnnotations(key: String): String? =
        BrowserIndexedDb.read("annotations:$key")

    override suspend fun writeAnnotations(key: String, content: String) {
        BrowserIndexedDb.write("annotations:$key", content)
    }
}
