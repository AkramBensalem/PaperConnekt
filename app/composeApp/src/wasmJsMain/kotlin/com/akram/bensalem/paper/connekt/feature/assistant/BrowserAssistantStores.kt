package com.akram.bensalem.paper.connekt.feature.assistant

import com.paperconnekt.knowledge.MemoryVectorStore
import com.paperconnekt.knowledge.VectorStore

actual fun defaultPaperVectorStore(): VectorStore = BrowserStores.vectorStore

actual fun defaultPaperAgentChatStore(): PaperAgentChatStore = BrowserStores.chatStore

actual fun defaultPdfMarkdownStore(): PdfMarkdownStore = BrowserStores.markdownStore

actual fun defaultResearchHistoryStore(): ResearchHistoryStore = BrowserStores.historyStore

actual fun defaultIdeaNoteStore(): IdeaNoteStore = BrowserStores.ideaStore

private object BrowserStores {
    val vectorStore = MemoryVectorStore()
    val chatStore = InMemoryPaperAgentChatStore()
    val markdownStore = BrowserMarkdownStore()
    val historyStore = InMemoryResearchHistoryStore()
    val ideaStore = InMemoryIdeaNoteStore()
}

private class BrowserMarkdownStore : PdfMarkdownStore {
    private val documents = mutableMapOf<String, OcrDocument>()

    override suspend fun load(key: String): String? = documents[key]?.markdown

    override suspend fun save(key: String, document: OcrDocument): String {
        documents[key] = document
        return document.markdown
    }

    override suspend fun loadDocument(key: String): OcrDocument? = documents[key]

    override suspend fun loadLayout(key: String): OcrLayout? = documents[key]?.layout
}
