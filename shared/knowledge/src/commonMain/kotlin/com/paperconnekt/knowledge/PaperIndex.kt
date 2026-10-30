package com.paperconnekt.knowledge

/**
 * Immutable, in-memory retrieval index for one paper. It is cheap to rebuild from the canonical Markdown, so it is
 * never persisted as a source of truth; callers cache it per paper and content version.
 */
class PaperIndex private constructor(
    val paperId: String,
    val pageCount: Int,
    val chunks: List<PaperChunk>,
) {
    private val byId = chunks.associateBy { it.id }
    private val normalizedTexts = chunks.map { TextAnalyzer.normalizePhrase(it.text) }
    private val bm25 = Bm25Index(chunks.map { chunk ->
        val sectionTerms = TextAnalyzer.terms(chunk.sectionPath.joinToString(" "))
        TextAnalyzer.terms(chunk.text) + sectionTerms + sectionTerms
    })

    val characterCount: Int = chunks.sumOf { it.text.length }

    /** Ranked, cited passages for [query]: BM25 relevance plus a bonus when the exact phrase occurs. */
    fun search(query: RetrievalQuery): List<RetrievedChunk> {
        val terms = TextAnalyzer.terms(query.text)
        val phrase = TextAnalyzer.normalizePhrase(query.text)
        if (terms.isEmpty() && phrase.length < 3) return emptyList()
        val phraseBonus = 2.0 + terms.distinct().size
        return bm25.score(terms).mapIndexedNotNull { index, (lexical, matched) ->
            val chunk = chunks[index]
            if (!query.accepts(chunk)) return@mapIndexedNotNull null
            val phraseHit = phrase.length >= 3 && normalizedTexts[index].contains(phrase)
            var score = lexical + if (phraseHit) phraseBonus else 0.0
            if (chunk.kind == ChunkKind.REFERENCES && !query.referencesRequested) score *= 0.5
            if (score <= 0.0) return@mapIndexedNotNull null
            val methods = buildSet {
                if (lexical > 0.0) add(RetrievalMethod.LEXICAL)
                if (phraseHit) add(RetrievalMethod.PHRASE)
            }
            RetrievedChunk(chunk, score, matched, methods)
        }.sortedByDescending { it.score }.take(query.limit.coerceAtLeast(1))
    }

    /** Case-insensitive literal occurrences of [phrase], each with a short snippet and its chunk for citation. */
    fun findExact(phrase: String, limit: Int = 12, pages: Set<Int>? = null): List<ExactMatch> {
        val needle = phrase.trim()
        if (needle.isEmpty()) return emptyList()
        val matches = ArrayList<ExactMatch>()
        for (chunk in chunks) {
            if (pages != null && (chunk.firstPage..chunk.lastPage).none { it in pages }) continue
            var from = 0
            while (matches.size < limit) {
                val at = chunk.text.indexOf(needle, from, ignoreCase = true)
                if (at < 0) break
                val start = (at - SNIPPET_RADIUS).coerceAtLeast(0)
                val end = (at + needle.length + SNIPPET_RADIUS).coerceAtMost(chunk.text.length)
                val snippet = buildString {
                    if (start > 0) append('…')
                    append(chunk.text.substring(start, end).replace('\n', ' '))
                    if (end < chunk.text.length) append('…')
                }
                matches += ExactMatch(chunk, snippet)
                from = at + needle.length
            }
            if (matches.size >= limit) break
        }
        return matches
    }

    fun chunk(id: String): PaperChunk? = byId[id.trim().lowercase()]

    /** The chunk [id] with up to [radius] chunks on each side, in reading order. */
    fun neighbors(id: String, radius: Int = 1): List<PaperChunk> {
        val center = chunk(id) ?: return emptyList()
        val first = (center.ordinal - radius).coerceAtLeast(0)
        val last = (center.ordinal + radius).coerceAtMost(chunks.lastIndex)
        return chunks.subList(first, last + 1)
    }

    /** One entry per distinct heading trail, in reading order, with where it starts and how many chunks it spans. */
    fun outline(): List<OutlineEntry> {
        val entries = ArrayList<OutlineEntry>()
        for (chunk in chunks) {
            val last = entries.lastOrNull()
            if (last != null && last.sectionPath == chunk.sectionPath) {
                entries[entries.lastIndex] = last.copy(chunkCount = last.chunkCount + 1)
            } else {
                entries += OutlineEntry(chunk.sectionPath, chunk.firstPage, chunk.id, 1)
            }
        }
        return entries
    }

    companion object {
        private const val SNIPPET_RADIUS = 80

        fun build(paperId: String, pages: List<String>, chunker: MarkdownChunker = MarkdownChunker()): PaperIndex =
            PaperIndex(paperId, pages.size, chunker.chunk(paperId, pages))
    }
}

/** Lexical-only retriever, used when semantic search is off or no embedding provider is configured. */
class LexicalPaperRetriever(private val index: PaperIndex) : PaperRetriever {
    override suspend fun retrieve(query: RetrievalQuery): List<RetrievedChunk> = index.search(query)
}
