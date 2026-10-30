package com.akram.bensalem.paper.connekt.feature.mcp

import com.akram.bensalem.paper.connekt.core.aiassistant.AgentParameterType.INTEGER
import com.akram.bensalem.paper.connekt.core.aiassistant.AgentParameterType.STRING
import com.akram.bensalem.paper.connekt.core.aiassistant.AgentParameterType.STRING_LIST
import com.akram.bensalem.paper.connekt.core.aiassistant.AgentTool
import com.akram.bensalem.paper.connekt.core.aiassistant.AgentToolArguments
import com.akram.bensalem.paper.connekt.core.aiassistant.AgentToolParameter
import com.akram.bensalem.paper.connekt.feature.assistant.Idea
import com.akram.bensalem.paper.connekt.feature.assistant.IdeaNoteStore
import com.akram.bensalem.paper.connekt.feature.assistant.PaperKnowledgeCache
import com.akram.bensalem.paper.connekt.feature.assistant.PdfMarkdownStore
import com.akram.bensalem.paper.connekt.feature.assistant.agentMarkdown
import com.akram.bensalem.paper.connekt.feature.assistant.defaultPdfMarkdownStore
import com.akram.bensalem.paper.connekt.feature.assistant.markdownPages
import com.akram.bensalem.paper.connekt.shared.zotero.model.zotero.ZoteroItem
import com.paperconnekt.knowledge.ChunkKind
import com.paperconnekt.knowledge.EmbeddingProvider
import com.paperconnekt.knowledge.KnowledgeFormat
import com.paperconnekt.knowledge.LexicalPaperRetriever
import com.paperconnekt.knowledge.PaperChunk
import com.paperconnekt.knowledge.PaperIndex
import com.paperconnekt.knowledge.PaperRetriever
import com.paperconnekt.knowledge.RetrievalQuery
import com.paperconnekt.knowledge.RetrievedChunk
import com.paperconnekt.storage.LocalResearchStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.json.Json
import kotlin.math.roundToInt
import kotlin.random.Random

/** A library item as tools see it: one ID for metadata, retrieval and the Idea Board, whatever its source. */
data class LibraryPaper(
    /** Local paper ID, or the Idea Board ID of a Zotero attachment (`zotero:KEY` or `zotero:group:ID:KEY`). */
    val id: String,
    val title: String,
    val authors: List<String>,
    val year: String?,
    val doi: String?,
    val abstract: String?,
    val tags: List<String>,
    val source: String,
    /** Key of the converted Markdown in [PdfMarkdownStore]; null when the item has no PDF. */
    val markdownKey: String?,
    val archived: Boolean = false,
)

/**
 * Paper-ID-based access to the whole library and its converted papers, independent of any open reader.
 *
 * It is the data source of the MCP server (and of future multi-paper agent tools). Indexes and embeddings go through
 * [PaperKnowledgeCache] under the same keys the reader uses, so a paper embedded in the app is never embedded again
 * for an MCP client. Zotero items come from the library cache the app saved during its last sync, so only the
 * Zotero library selected in the app is visible.
 */
class PaperLibrary(
    private val store: LocalResearchStore,
    private val markdown: PdfMarkdownStore = defaultPdfMarkdownStore(),
    private val knowledge: PaperKnowledgeCache = PaperKnowledgeCache(capacity = 24),
    /** Embedding provider for semantic search, or null for keyword search only. */
    private val embedder: suspend () -> EmbeddingProvider? = { null },
    /** Receives the texts sent to the embedding provider, for cost transparency. */
    private val onEmbedded: suspend (modelId: String, texts: List<String>) -> Unit = { _, _ -> },
) {
    private var zoteroSource: String? = null
    private var zoteroPapers: List<LibraryPaper> = emptyList()
    private val zoteroLock = Mutex()

    suspend fun papers(includeArchived: Boolean = false): List<LibraryPaper> {
        val local = store.papers.value.map { paper ->
            LibraryPaper(
                id = paper.id,
                title = paper.title,
                authors = paper.authors,
                year = paper.year?.toString(),
                doi = paper.doi,
                abstract = paper.abstractText,
                tags = paper.tags,
                source = paper.source.ifBlank { "Local" },
                markdownKey = paper.pdfPath?.takeIf(String::isNotBlank)?.let { "local/${paper.id}/$it" },
                archived = paper.archived,
            )
        }
        return (local + zotero()).filter { includeArchived || !it.archived }
    }

    suspend fun paper(id: String): LibraryPaper = papers(includeArchived = true).firstOrNull { it.id == id.trim() }
        ?: throw IllegalArgumentException("No paper with ID '$id'. Call list_papers to find paper IDs.")

    /** Markdown pages of [paper], or null when it has no PDF or the PDF was not converted yet. */
    suspend fun pages(paper: LibraryPaper): List<String>? {
        val key = paper.markdownKey ?: return null
        val pages = markdown.load(key)?.let(::markdownPages) ?: return null
        return pages.takeIf { it.any(String::isNotBlank) }
    }

    /** Retrieval index of [paper], or null when it has no PDF or the PDF was not converted yet. */
    suspend fun index(paper: LibraryPaper): PaperIndex? {
        val key = paper.markdownKey ?: return null
        return knowledge.index(key, pages(paper) ?: return null)
    }

    suspend fun requireIndex(paper: LibraryPaper): PaperIndex = index(paper) ?: throw unavailable(paper)

    suspend fun requirePages(paper: LibraryPaper): List<String> = pages(paper) ?: throw unavailable(paper)

    /** Hybrid keyword + meaning retriever when embeddings are enabled and ready, keyword retriever otherwise. */
    suspend fun retriever(index: PaperIndex): PaperRetriever {
        val provider = embedder() ?: return LexicalPaperRetriever(index)
        return knowledge.retriever(index, provider) { texts -> onEmbedded(provider.modelId, texts) }
    }

    fun close() = knowledge.close()

    private fun unavailable(paper: LibraryPaper) = IllegalArgumentException(
        if (paper.markdownKey == null) "\"${paper.title}\" has no attached PDF."
        else "\"${paper.title}\" has not been converted to text yet. Open it once in PaperConnekt's reader to convert it.",
    )

    private suspend fun zotero(): List<LibraryPaper> = zoteroLock.withLock {
        val cache = store.settings.value.zoteroLibraryCache
        if (cache.itemsJson == zoteroSource) return@withLock zoteroPapers
        zoteroPapers = try {
            decodeZotero(cache.accountKey, cache.itemsJson, cache.attachmentsJson)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            emptyList()
        }
        zoteroSource = cache.itemsJson
        zoteroPapers
    }

    private fun decodeZotero(accountKey: String, itemsJson: String, attachmentsJson: String): List<LibraryPaper> {
        if (itemsJson.isBlank()) return emptyList()
        // accountKey is "userId:libraryId:groupId|personal", as written by the Library screen.
        val parts = accountKey.split(':')
        val userId = parts.getOrNull(0).orEmpty()
        val groupId = parts.getOrNull(2)?.takeIf { it.isNotBlank() && it != "personal" }
        val items = zoteroJson.decodeFromString<List<ZoteroItem>>(itemsJson)
        val attachments = attachmentsJson.takeIf(String::isNotBlank)
            ?.let { zoteroJson.decodeFromString<List<ZoteroItem>>(it) }.orEmpty()
            .groupBy { it.data.parentItem }
        return items.mapNotNull { item ->
            val candidates = attachments[item.key].orEmpty()
            val attachment = candidates.firstOrNull { it.data.contentType == "application/pdf" } ?: candidates.firstOrNull()
                ?: return@mapNotNull null
            val data = item.data
            LibraryPaper(
                id = groupId?.let { "zotero:group:$it:${attachment.key}" } ?: "zotero:${attachment.key}",
                title = data.title ?: attachment.data.filename ?: "Zotero PDF",
                authors = data.creators.map { it.name ?: listOfNotNull(it.firstName, it.lastName).joinToString(" ") }
                    .filter(String::isNotBlank),
                year = data.date?.let { yearPattern.find(it)?.value },
                doi = data.doi,
                abstract = data.abstractNote,
                tags = data.tags.map { it.tag },
                source = "Zotero",
                markdownKey = userId.takeIf(String::isNotBlank)
                    ?.let { "$it/${groupId?.let { id -> "groups/$id/" }.orEmpty()}${attachment.key}" },
            )
        }
    }

    private companion object {
        val zoteroJson = Json { ignoreUnknownKeys = true; isLenient = true }
        val yearPattern = Regex("\\b(1[5-9]|20)\\d{2}\\b")
    }
}

/** Tells an open Idea Board that notes were added from outside the app's UI (an MCP client), so it reloads them. */
object IdeaNoteChanges {
    private val events = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val changes: SharedFlow<String> = events.asSharedFlow()

    fun notify(paperId: String) {
        events.tryEmit(paperId)
    }
}

/** Names of the library tools that change user data; every other tool only reads. */
val PaperLibraryWriteTools = setOf("add_idea_note")

/**
 * Library-level research tools: find papers, then read only the passages a task needs. They share the [AgentTool]
 * contract with the in-app agent, so the same definitions can be served over MCP or given to a Koog agent.
 * The write tool `add_idea_note` is only included when [ideas] is set.
 */
fun paperLibraryTools(library: PaperLibrary, ideas: IdeaNoteStore? = null): List<AgentTool> = buildList {
    add(AgentTool(
        "list_papers",
        "List the papers in the user's PaperConnekt library with their IDs, optionally filtered by words in the title, " +
            "authors, tags or abstract. Use the IDs with the other tools.",
        listOf(
            AgentToolParameter("query", "Words to filter by. Leave empty to list everything.", STRING, false),
            AgentToolParameter("max_results", "Number of papers, 1-50. Defaults to 20.", INTEGER, false),
        ),
    ) { args ->
        val terms = searchTerms(args.string("query").orEmpty())
        val limit = (args.int("max_results") ?: 20).coerceIn(1, 50)
        val papers = library.papers()
        val ranked = if (terms.isEmpty()) papers else papers
            .map { it to it.matchScore(terms) }.filter { it.second > 0 }.sortedByDescending { it.second }.map { it.first }
        if (ranked.isEmpty()) {
            return@AgentTool if (papers.isEmpty()) "The library is empty." else "No paper matches \"${args.string("query")}\"."
        }
        buildString {
            appendLine("${ranked.size} paper(s)${if (ranked.size > limit) ", showing $limit" else ""}:")
            ranked.take(limit).forEach { paper ->
                val text = when {
                    paper.markdownKey == null -> "no PDF"
                    library.pages(paper) != null -> "text ready"
                    else -> "not converted"
                }
                appendLine("- [${paper.id}] ${paper.title} — ${paper.byline()} · ${paper.source} · $text")
            }
        }.trimEnd()
    })
    add(library.paperTool("get_paper", "Get a paper's metadata, abstract and section overview.") { paper, _ ->
        buildString {
            appendLine(paper.title)
            appendLine("ID: ${paper.id} · ${paper.source}${if (paper.archived) " · archived" else ""}")
            appendLine("Authors: ${paper.authors.joinToString().ifBlank { "unknown" }}")
            paper.year?.let { appendLine("Year: $it") }
            paper.doi?.let { appendLine("DOI: $it") }
            if (paper.tags.isNotEmpty()) appendLine("Tags: ${paper.tags.joinToString()}")
            paper.abstract?.takeIf(String::isNotBlank)?.let { appendLine().appendLine("Abstract: ${it.trim()}") }
            appendLine()
            val index = library.index(paper)
            if (index == null) append("Full text: unavailable (${if (paper.markdownKey == null) "no PDF" else "not converted yet"}).")
            else append(KnowledgeFormat.paperMap(index))
        }.trimEnd()
    })
    add(library.paperTool("get_outline", "Get a paper's section outline with pages and the first chunk ID of each section.") { paper, _ ->
        KnowledgeFormat.outline(library.requireIndex(paper))
    })
    add(library.paperTool(
        "search_paper",
        "Retrieve the passages of one paper that answer a question, ranked, with chunk IDs, pages and sections. " +
            "mode=relevant ranks by keywords and, when enabled in PaperConnekt, by meaning; mode=exact lists literal occurrences.",
        AgentToolParameter("query", "Question, keywords or phrase to look for.", STRING),
        AgentToolParameter("mode", "relevant (default) or exact.", STRING, false, listOf("relevant", "exact")),
        AgentToolParameter("max_results", "Number of passages, 1-10. Defaults to 5.", INTEGER, false),
        AgentToolParameter("start_page", "Only search from this page (1-based).", INTEGER, false),
        AgentToolParameter("end_page", "Only search up to this page, inclusive.", INTEGER, false),
        AgentToolParameter("kind", "Only this kind of passage.", STRING, false, ChunkKind.entries.map { it.name.lowercase() }),
    ) { paper, args ->
        val index = library.requireIndex(paper)
        val query = args.requireString("query")
        val limit = (args.int("max_results") ?: 5).coerceIn(1, 10)
        val start = args.int("start_page")
        val end = args.int("end_page")
        val pages = if (start == null && end == null) null
        else ((start ?: 1).coerceAtLeast(1)..(end ?: start ?: index.pageCount).coerceAtLeast(start ?: 1)).toSet()
        val kind = args.string("kind")?.let { name -> ChunkKind.entries.firstOrNull { it.name.equals(name, ignoreCase = true) } }
        if (args.string("mode").equals("exact", ignoreCase = true)) {
            KnowledgeFormat.exact(query, index.findExact(query, limit = limit * 3, pages = pages))
        } else {
            KnowledgeFormat.results(query, library.retriever(index).retrieve(RetrievalQuery(query, limit, pages, kind?.let { setOf(it) })))
        }
    })
    add(library.paperTool(
        "read_chunks",
        "Read the full text of a paper's passages by chunk ID (from search_paper, search_library or get_outline), " +
            "optionally with neighbouring chunks.",
        AgentToolParameter("ids", "Chunk IDs such as c12, at most 8.", STRING_LIST),
        AgentToolParameter("neighbors", "Also include this many chunks before and after each one, 0-2. Defaults to 0.", INTEGER, false),
    ) { paper, args ->
        val index = library.requireIndex(paper)
        val radius = (args.int("neighbors") ?: 0).coerceIn(0, 2)
        val chunks = args.strings("ids").take(8)
            .flatMap { id -> if (radius == 0) listOfNotNull(index.chunk(id)) else index.neighbors(id, radius) }
            .distinctBy(PaperChunk::id).sortedBy(PaperChunk::ordinal)
        if (chunks.isEmpty()) "No chunk matches those IDs. Use search_paper or get_outline to find chunk IDs."
        else KnowledgeFormat.chunks(chunks)
    })
    add(library.paperTool(
        "read_pages",
        "Read the Markdown text of up to 5 consecutive pages of a paper. Prefer search_paper and read_chunks; read " +
            "pages only when you need the full context.",
        AgentToolParameter("start_page", "First page (1-based).", INTEGER),
        AgentToolParameter("end_page", "Last page, inclusive. Defaults to start_page.", INTEGER, false),
    ) { paper, args ->
        val pages = library.requirePages(paper)
        val start = args.requireInt("start_page").coerceIn(1, pages.size)
        val end = (args.int("end_page") ?: start).coerceIn(start, minOf(pages.size, start + 4))
        (start..end).joinToString("\n") { "<page number=\"$it\">\n${agentMarkdown(pages[it - 1])}\n</page>" }
    })
    add(AgentTool(
        "search_library",
        "Search the text of every converted paper in the library at once and return the best passages with their " +
            "paper IDs and chunk IDs. Use it to find which papers discuss a topic, then search_paper or read_chunks.",
        listOf(
            AgentToolParameter("query", "Question, keywords or phrase to look for.", STRING),
            AgentToolParameter("max_results", "Number of passages, 1-15. Defaults to 8.", INTEGER, false),
            AgentToolParameter("paper_ids", "Only these papers. Defaults to the whole library.", STRING_LIST, false),
        ),
    ) { args -> searchLibrary(library, args) })
    if (ideas != null) add(library.paperTool(
        "add_idea_note",
        "Add a note to a paper's Idea Board in PaperConnekt, for example a research idea, open question or summary.",
        AgentToolParameter("title", "Short title of the note.", STRING),
        AgentToolParameter("content", "Body of the note (Markdown allowed).", STRING),
    ) { paper, args ->
        val title = args.requireString("title").take(200)
        val content = args.string("content").orEmpty().take(20_000)
        val idea = ideaWriteLock.withLock {
            val existing = ideas.load(paper.id)
            val offset = (existing.size % 8) * 24f
            Idea(newIdeaId(), title, content, x = 100f + offset, y = 100f + offset)
                .also { ideas.save(paper.id, existing + it) }
        }
        IdeaNoteChanges.notify(paper.id)
        "Added idea note ${idea.id} \"$title\" to the Idea Board of \"${paper.title}\"."
    })
}

private suspend fun searchLibrary(library: PaperLibrary, args: AgentToolArguments): String {
    val query = args.requireString("query")
    val limit = (args.int("max_results") ?: 8).coerceIn(1, 15)
    val only = args.strings("paper_ids").map(String::trim).toSet()
    val candidates = library.papers()
        .filter { it.markdownKey != null && (only.isEmpty() || it.id in only) }
        .take(MAX_LIBRARY_SEARCH_PAPERS)
    val permits = Semaphore(4)
    val searched = coroutineScope {
        candidates.map { paper ->
            async {
                permits.withPermit {
                    val index = library.index(paper) ?: return@withPermit null
                    LexicalPaperRetriever(index).retrieve(RetrievalQuery(query, limit = 3)).map { paper to it }
                }
            }
        }.awaitAll()
    }
    val converted = searched.count { it != null }
    val hits = searched.filterNotNull().flatten()
    if (hits.isEmpty()) {
        return if (converted == 0) "No converted paper to search. Open papers once in PaperConnekt's reader to convert them."
        else "No passage in $converted paper(s) matched \"$query\". Try other keywords."
    }
    val best = hits.sortedByDescending { it.second.score }.take(limit)
    return buildString {
        appendLine("Best keyword matches for \"$query\" across $converted converted paper(s). Cite the paper ID with the chunk ID.")
        best.groupBy({ it.first }, { it.second }).forEach { (paper, results) ->
            appendLine()
            appendLine("## ${paper.title} [${paper.id}]")
            results.forEach { result -> appendLine(result.summary()) }
        }
    }.trimEnd()
}

private fun RetrievedChunk.summary(): String {
    val text = chunk.text.replace(whitespace, " ").trim()
    val excerpt = if (text.length > 600) text.take(600).trimEnd() + "…" else text
    val matched = if (matchedTerms.isEmpty()) "" else " · matched: ${matchedTerms.joinToString(", ")}"
    return "${KnowledgeFormat.header(chunk)} · score ${(score * 10).roundToInt() / 10.0}$matched\n$excerpt"
}

private fun PaperLibrary.paperTool(
    name: String,
    description: String,
    vararg parameters: AgentToolParameter,
    run: suspend (LibraryPaper, AgentToolArguments) -> String,
) = AgentTool(
    name,
    description,
    listOf(AgentToolParameter("paper_id", "Paper ID from list_papers or search_library.", STRING)) + parameters,
) { args -> run(paper(args.requireString("paper_id")), args) }

private val ideaWriteLock = Mutex()
private val whitespace = Regex("\\s+")

private fun newIdeaId(): String = "mcp-${Random.nextLong().toULong().toString(36)}"

private fun searchTerms(query: String): List<String> =
    query.lowercase().split(Regex("[^\\p{L}\\p{N}]+")).filter { it.length >= 2 }.distinct()

private fun LibraryPaper.matchScore(terms: List<String>): Int = terms.sumOf { term ->
    (if (term in title.lowercase()) 3 else 0) +
        (if (authors.any { term in it.lowercase() }) 2 else 0) +
        (if (tags.any { term in it.lowercase() } || doi?.lowercase()?.contains(term) == true) 2 else 0) +
        (if (abstract?.lowercase()?.contains(term) == true) 1 else 0)
}

private fun LibraryPaper.byline(): String {
    val names = when {
        authors.isEmpty() -> "Unknown authors"
        authors.size > 3 -> authors.take(3).joinToString() + " et al."
        else -> authors.joinToString()
    }
    return names + (year?.let { " ($it)" } ?: "")
}

private const val MAX_LIBRARY_SEARCH_PAPERS = 60
