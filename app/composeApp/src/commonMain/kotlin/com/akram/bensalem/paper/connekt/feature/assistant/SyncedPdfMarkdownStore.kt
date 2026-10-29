package com.akram.bensalem.paper.connekt.feature.assistant

/** Keeps the copied OCR store API while persisting only through the local platform store. */
class SyncedPdfMarkdownStore(
    private val local: PdfMarkdownStore = defaultPdfMarkdownStore(),
) : PdfMarkdownStore by local {
    override var syncError: String?
        get() = null
        set(_) = Unit
}
