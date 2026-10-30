package com.paperconnekt.knowledge

import kotlinx.serialization.Serializable

/** What a chunk mostly contains; lets retrieval favour or skip structural parts such as the bibliography. */
@Serializable
enum class ChunkKind { TEXT, ABSTRACT, TABLE, EQUATION, CAPTION, REFERENCES }

/**
 * A retrievable passage of a paper's canonical OCR Markdown. Chunks never replace that Markdown: they are a derived,
 * rebuildable view that keeps provenance (pages and section) so every answer can cite where its evidence came from.
 */
@Serializable
data class PaperChunk(
    /** Short, stable ID within one index (for example `c12`), small enough for the model to cite cheaply. */
    val id: String,
    val paperId: String,
    val ordinal: Int,
    /** 1-based page where the chunk starts. */
    val firstPage: Int,
    /** 1-based page where the chunk ends; equal to [firstPage] unless a short paragraph continued on the next page. */
    val lastPage: Int,
    /** Heading trail from the outermost heading to the one containing this chunk; empty for front matter. */
    val sectionPath: List<String>,
    val kind: ChunkKind,
    val text: String,
) {
    val pageLabel: String get() = if (firstPage == lastPage) "p. $firstPage" else "pp. $firstPage–$lastPage"
    val sectionLabel: String get() = sectionPath.takeLast(2).joinToString(" › ").ifEmpty { "Front matter" }
}

data class RetrievalQuery(
    val text: String,
    val limit: Int = 6,
    /** Only chunks touching these 1-based pages; `null` searches the whole paper. */
    val pages: Set<Int>? = null,
    /** Only these kinds; `null` searches every kind but ranks the bibliography lower. */
    val kinds: Set<ChunkKind>? = null,
) {
    /** Whether [chunk] passes the page and kind filters. */
    fun accepts(chunk: PaperChunk): Boolean =
        (pages == null || (chunk.firstPage..chunk.lastPage).any { it in pages }) && (kinds == null || chunk.kind in kinds)

    /** The bibliography is ranked lower unless the query asks for it explicitly. */
    val referencesRequested: Boolean get() = kinds?.contains(ChunkKind.REFERENCES) == true
}

/** Why a chunk was retrieved, so the UI and the model can explain the evidence instead of trusting it blindly. */
enum class RetrievalMethod { LEXICAL, PHRASE, SEMANTIC }

data class RetrievedChunk(
    val chunk: PaperChunk,
    /** BM25 (plus phrase bonus) for lexical search; 0–1 fused relevance for hybrid search. */
    val score: Double,
    val matchedTerms: List<String>,
    val methods: Set<RetrievalMethod>,
    /** 1-based rank in the keyword (BM25) list, when hybrid search fused it. */
    val lexicalRank: Int? = null,
    /** 1-based rank in the embedding-similarity list, when hybrid search fused it. */
    val semanticRank: Int? = null,
    /** Cosine similarity between the query and the chunk embeddings. */
    val similarity: Double? = null,
) {
    val hybrid: Boolean get() = lexicalRank != null || semanticRank != null
}

data class ExactMatch(val chunk: PaperChunk, val snippet: String)

data class OutlineEntry(val sectionPath: List<String>, val firstPage: Int, val firstChunkId: String, val chunkCount: Int)

/** Retrieval contract shared by the in-app agent and future hosts (MCP server, plugins); implementations may be hybrid. */
interface PaperRetriever {
    suspend fun retrieve(query: RetrievalQuery): List<RetrievedChunk>
}
