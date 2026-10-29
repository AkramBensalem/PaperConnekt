package com.paperconnekt.storage

interface PdfAttachmentStorage {
    /** Copies a native file reference into managed local storage and returns its path. */
    suspend fun importPdf(source: String): String
    suspend fun readPdf(path: String): ByteArray
}

fun requirePdfHeader(header: ByteArray) {
    require(header.size >= 5 && header.take(5).toByteArray().decodeToString() == "%PDF-") {
        "This file is not a PDF. Please choose a PDF document."
    }
}
