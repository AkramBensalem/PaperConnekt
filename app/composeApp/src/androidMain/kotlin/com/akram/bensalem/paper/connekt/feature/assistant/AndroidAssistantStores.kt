package com.akram.bensalem.paper.connekt.feature.assistant

import com.akram.bensalem.paper.connekt.core.settings.requireAndroidApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

private val assistantStoreJson = Json { encodeDefaults = true; ignoreUnknownKeys = true }
private val assistantStorePreferences by lazy {
    requireAndroidApplicationContext().getSharedPreferences("paperconnekt_contest_assistant", 0)
}

private fun preferenceKey(prefix: String, key: String): String =
    "$prefix:${key.encodeToByteArray().joinToString("") { "%02x".format(it) }}"

private class AndroidResearchHistoryStore : ResearchHistoryStore {
    override suspend fun load(userId: String): ResearchHistory = withContext(Dispatchers.IO) {
        require(userId.isNotBlank()) { "A Zotero user is required to store research history." }
        assistantStorePreferences.getString(preferenceKey("history", userId), null)
            ?.let { assistantStoreJson.decodeFromString<ResearchHistory>(it) }
            ?: ResearchHistory()
    }

    override suspend fun save(userId: String, history: ResearchHistory) = withContext(Dispatchers.IO) {
        require(userId.isNotBlank()) { "A Zotero user is required to store research history." }
        check(
            assistantStorePreferences.edit()
                .putString(preferenceKey("history", userId), assistantStoreJson.encodeToString(history))
                .commit(),
        ) { "Failed to save research history." }
    }
}

private class AndroidPaperAgentChatStore : PaperAgentChatStore {
    override suspend fun load(key: String): List<PaperAgentConversation> = withContext(Dispatchers.IO) {
        assistantStorePreferences.getString(preferenceKey("chats", key), null)
            ?.let { paperAgentChatJson.decodeFromString<StoredPaperAgentChats>(it).conversations }
            .orEmpty()
    }

    override suspend fun save(key: String, conversations: List<PaperAgentConversation>) = withContext(Dispatchers.IO) {
        val editor = assistantStorePreferences.edit()
        if (conversations.isEmpty()) {
            editor.remove(preferenceKey("chats", key))
        } else {
            editor.putString(
                preferenceKey("chats", key),
                paperAgentChatJson.encodeToString(StoredPaperAgentChats(conversations = conversations)),
            )
        }
        check(editor.commit()) { "Failed to save research agent chats." }
    }
}

private class AndroidPdfMarkdownStore : PdfMarkdownStore {
    private val root = File(requireAndroidApplicationContext().noBackupFilesDir, "paperconnekt-contest/markdown")

    private fun directory(key: String): File =
        key.split('/').filter(String::isNotBlank).fold(root) { parent, segment ->
            File(parent, safePathSegment(segment))
        }

    override suspend fun load(key: String): String? = withContext(Dispatchers.IO) {
        val directory = directory(key)
        val document = File(directory, "document.md")
        if (!document.isFile) null else resolveFigures(directory, document.readText())
    }

    override suspend fun save(key: String, document: OcrDocument): String = withContext(Dispatchers.IO) {
        val directory = directory(key)
        val figures = File(directory, "figures")
        check(figures.isDirectory || figures.mkdirs()) { "Could not create PDF figure storage." }
        document.images.forEach { (id, bytes) -> File(figures, safePathSegment(id)).writeBytes(bytes) }
        File(directory, "sync-pending").writeText("pending")
        val layoutFile = File(directory, "blocks.json")
        if (document.layout == null) {
            layoutFile.delete()
        } else {
            layoutFile.writeText(assistantStoreJson.encodeToString(OcrLayout.serializer(), document.layout))
        }
        File(directory, "document.md").writeText(document.markdown)
        resolveFigures(directory, document.markdown)
    }

    override suspend fun loadDocument(key: String): OcrDocument? = withContext(Dispatchers.IO) {
        val directory = directory(key)
        val file = File(directory, "document.md")
        if (!file.isFile) return@withContext null
        val markdown = file.readText()
        val images = ocrFigureIds(markdown).associateWith { id ->
            File(File(directory, "figures"), id).readBytes()
        }
        OcrDocument(markdown, images, loadLayout(key))
    }

    override suspend fun hasPendingSync(key: String): Boolean = withContext(Dispatchers.IO) {
        File(directory(key), "sync-pending").isFile
    }

    override suspend fun setPendingSync(key: String, pending: Boolean) = withContext(Dispatchers.IO) {
        val directory = directory(key)
        val marker = File(directory, "sync-pending")
        if (pending) {
            check(directory.isDirectory || directory.mkdirs()) { "Could not create PDF Markdown storage." }
            marker.writeText("pending")
        } else {
            check(!marker.exists() || marker.delete()) { "Could not update OCR synchronization status." }
        }
    }

    override suspend fun loadLayout(key: String): OcrLayout? = withContext(Dispatchers.IO) {
        val file = File(directory(key), "blocks.json")
        if (!file.isFile) null else assistantStoreJson.decodeFromString<OcrLayout>(file.readText())
    }

    private fun resolveFigures(directory: File, markdown: String): String =
        markdown.replace("](figures/", "](file://${File(directory, "figures").absolutePath}/")
}

@Serializable
private data class StoredIdea(
    val id: String,
    val title: String,
    val description: String,
    val x: Float,
    val y: Float,
    val pageColor: String,
) {
    fun toIdea() = Idea(id, title, description, x, y, pageColor)

    companion object {
        fun from(idea: Idea) = StoredIdea(
            idea.id,
            idea.title,
            idea.description,
            idea.x,
            idea.y,
            idea.pageColor,
        )
    }
}

private class AndroidIdeaNoteStore : IdeaNoteStore {
    override suspend fun load(paperId: String): List<Idea> = withContext(Dispatchers.IO) {
        assistantStorePreferences.getString(preferenceKey("ideas", paperId), null)
            ?.let { assistantStoreJson.decodeFromString<List<StoredIdea>>(it).map(StoredIdea::toIdea) }
            .orEmpty()
    }

    override suspend fun save(paperId: String, ideas: List<Idea>) = withContext(Dispatchers.IO) {
        val editor = assistantStorePreferences.edit()
        val key = preferenceKey("ideas", paperId)
        if (ideas.isEmpty()) {
            editor.remove(key)
        } else {
            editor.putString(key, assistantStoreJson.encodeToString(ideas.map(StoredIdea::from)))
        }
        check(editor.commit()) { "Failed to save paper ideas." }
    }
}

private val researchHistoryStore: ResearchHistoryStore by lazy(::AndroidResearchHistoryStore)
private val paperAgentChatStore: PaperAgentChatStore by lazy(::AndroidPaperAgentChatStore)
private val pdfMarkdownStore: PdfMarkdownStore by lazy(::AndroidPdfMarkdownStore)
private val ideaNoteStore: IdeaNoteStore by lazy(::AndroidIdeaNoteStore)

actual fun defaultResearchHistoryStore(): ResearchHistoryStore = researchHistoryStore
actual fun defaultPaperAgentChatStore(): PaperAgentChatStore = paperAgentChatStore
actual fun defaultPdfMarkdownStore(): PdfMarkdownStore = pdfMarkdownStore
actual fun defaultIdeaNoteStore(): IdeaNoteStore = ideaNoteStore
