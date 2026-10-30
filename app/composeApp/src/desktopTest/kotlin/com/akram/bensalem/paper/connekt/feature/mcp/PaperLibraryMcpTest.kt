package com.akram.bensalem.paper.connekt.feature.mcp

import com.akram.bensalem.paper.connekt.feature.assistant.InMemoryIdeaNoteStore
import com.akram.bensalem.paper.connekt.feature.assistant.OcrDocument
import com.akram.bensalem.paper.connekt.feature.assistant.PaperKnowledgeCache
import com.akram.bensalem.paper.connekt.feature.assistant.PdfMarkdownStore
import com.paperconnekt.domain.model.Paper
import com.paperconnekt.knowledge.MemoryVectorStore
import com.paperconnekt.mcp.McpServer
import com.paperconnekt.storage.PersistentResearchStore
import com.paperconnekt.storage.WorkspaceStorage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PaperLibraryMcpTest {
    private val markdown = """
        <!-- paperconnekt:ocr-page 0 -->

        # Attention Is All You Need

        ## Abstract

        The Transformer relies entirely on self-attention and dispenses with recurrence.

        <!-- paperconnekt:ocr-page 1 -->

        ## Results

        Our model reaches 28.4 BLEU on the WMT 2014 English-to-German translation task.
    """.trimIndent()

    private val knowledge = PaperKnowledgeCache(vectorStore = MemoryVectorStore())
    private val ideas = InMemoryIdeaNoteStore()

    @AfterTest
    fun close() = knowledge.close()

    private suspend fun library(): PaperLibrary {
        val store = PersistentResearchStore(MemoryWorkspace())
        store.initialize()
        store.savePaper(Paper(id = "p1", title = "Attention Is All You Need", authors = listOf("Ashish Vaswani"), year = 2017, pdfPath = "attention.pdf"))
        store.savePaper(Paper(id = "p2", title = "Unconverted paper", pdfPath = "other.pdf"))
        store.savePaper(Paper(id = "p3", title = "Metadata only"))
        return PaperLibrary(store, FakeMarkdown(mapOf("local/p1/attention.pdf" to markdown)), knowledge)
    }

    private suspend fun McpServer.call(name: String, arguments: String): JsonObject {
        val request = """{"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"$name","arguments":$arguments}}"""
        return Json.parseToJsonElement(handle(request)!!).jsonObject["result"]!!.jsonObject
    }

    private fun JsonObject.text() = this["content"]!!.jsonArray[0].jsonObject["text"]!!.jsonPrimitive.content
    private fun JsonObject.isError() = this["isError"]?.jsonPrimitive?.content == "true"

    @Test
    fun listsPapersWithTheirTextStatus() = runTest {
        val server = paperConnektMcpServer(library(), ideas, allowIdeaWrites = { false })
        val text = server.call("list_papers", "{}").text()
        assertTrue("[p1] Attention Is All You Need" in text && "text ready" in text, text)
        assertTrue("[p2] Unconverted paper" in text && "not converted" in text, text)
        assertTrue("[p3] Metadata only" in text && "no PDF" in text, text)
        assertFalse("[p3]" in server.call("list_papers", """{"query":"attention"}""").text())
    }

    @Test
    fun searchesOnePaperAndTheWholeLibrary() = runTest {
        val server = paperConnektMcpServer(library(), ideas, allowIdeaWrites = { false })
        val paper = server.call("search_paper", """{"paper_id":"p1","query":"BLEU translation"}""")
        assertFalse(paper.isError(), paper.text())
        assertTrue("28.4 BLEU" in paper.text(), paper.text())
        val library = server.call("search_library", """{"query":"self-attention recurrence"}""").text()
        assertTrue("[p1]" in library && "Transformer" in library, library)
    }

    @Test
    fun reportsMissingTextAndUnknownPapersAsToolErrors() = runTest {
        val server = paperConnektMcpServer(library(), ideas, allowIdeaWrites = { false })
        val unconverted = server.call("get_outline", """{"paper_id":"p2"}""")
        assertTrue(unconverted.isError() && "not been converted" in unconverted.text(), unconverted.text())
        val unknown = server.call("get_paper", """{"paper_id":"nope"}""")
        assertTrue(unknown.isError() && "list_papers" in unknown.text(), unknown.text())
    }

    @Test
    fun ideaNotesAreOnlyServedWhenAllowedAndNotifyTheBoard() = runTest {
        var allowed = false
        val server = paperConnektMcpServer(library(), ideas, allowIdeaWrites = { allowed })
        val list = """{"jsonrpc":"2.0","id":1,"method":"tools/list"}"""
        assertFalse("add_idea_note" in server.handle(list)!!)
        allowed = true
        val tools = Json.parseToJsonElement(server.handle(list)!!).jsonObject["result"]!!.jsonObject["tools"]!!.jsonArray
        val add = tools.first { it.jsonObject["name"]!!.jsonPrimitive.content == "add_idea_note" }.jsonObject
        assertEquals("false", add["annotations"]!!.jsonObject["readOnlyHint"]!!.jsonPrimitive.content)

        val notified = backgroundScope.launch { assertEquals("p1", IdeaNoteChanges.changes.first()) }
        testScheduler.runCurrent()
        val result = server.call("add_idea_note", """{"paper_id":"p1","title":"Ablate heads","content":"Try 4 heads."}""")
        assertFalse(result.isError(), result.text())
        assertEquals(listOf("Ablate heads"), ideas.load("p1").map { it.title })
        notified.join()
    }

    @Test
    fun schemaListsTypesEnumsAndRequiredParameters() = runTest {
        val search = paperLibraryTools(library()).first { it.name == "search_paper" }.inputSchema()
        val properties = search["properties"]!!.jsonObject
        assertEquals("integer", properties["max_results"]!!.jsonObject["type"]!!.jsonPrimitive.content)
        assertEquals(listOf("relevant", "exact"), properties["mode"]!!.jsonObject["enum"]!!.jsonArray.map { it.jsonPrimitive.content })
        assertEquals(listOf("paper_id", "query"), search["required"]!!.jsonArray.map { it.jsonPrimitive.content })
    }
}

private class MemoryWorkspace : WorkspaceStorage {
    private var content: String? = null
    override suspend fun read() = content
    override suspend fun write(content: String) {
        this.content = content
    }
    override suspend fun quarantineCorruptData() = Unit
}

private class FakeMarkdown(private val documents: Map<String, String>) : PdfMarkdownStore {
    override suspend fun load(key: String) = documents[key]
    override suspend fun save(key: String, document: OcrDocument): String = error("read-only")
}
