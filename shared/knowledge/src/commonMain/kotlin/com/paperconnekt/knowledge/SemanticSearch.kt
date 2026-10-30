package com.paperconnekt.knowledge

import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.sqrt

/**
 * Unit-length embedding vectors aligned with the chunks of one [PaperIndex]. A chunk can lack a vector (for example
 * when it could not be embedded); semantic search then simply skips it and keyword search still finds it.
 */
class PaperVectors internal constructor(
    val modelId: String,
    private val vectors: List<FloatArray?>,
) {
    val dimensions: Int = vectors.firstNotNullOfOrNull { it }?.size ?: 0
    val coverage: Int = vectors.count { it != null }

    /** Chunk ordinals accepted by [accept], ranked by cosine similarity to [query]; at most [limit] of them. */
    fun nearest(query: FloatArray, limit: Int, accept: (Int) -> Boolean = { true }): List<Pair<Int, Double>> {
        if (query.size != dimensions || dimensions == 0) return emptyList()
        val unit = normalized(query) ?: return emptyList()
        return vectors.mapIndexedNotNull { ordinal, vector ->
            if (vector == null || !accept(ordinal)) null else ordinal to dot(unit, vector)
        }.sortedByDescending { it.second }.take(limit.coerceAtLeast(1))
    }

    companion object {
        /** Text sent to the embedding model: the section trail adds context that short passages lack. */
        fun embeddingText(chunk: PaperChunk, maxChars: Int = 6_000): String =
            "${chunk.sectionLabel}\n${chunk.text}".let { if (it.length > maxChars) it.take(maxChars) else it }

        /**
         * Vectors for every chunk of [index], embedding only texts missing from [store] and saving the result. Stale
         * vectors (texts no longer in the paper) are dropped. [onEmbedded] receives the texts that were sent.
         */
        suspend fun build(
            index: PaperIndex,
            embedder: EmbeddingProvider,
            store: VectorStore,
            batchSize: Int = 32,
            onEmbedded: suspend (List<String>) -> Unit = {},
        ): PaperVectors {
            val texts = index.chunks.map(::embeddingText)
            val hashes = texts.map(::contentHash)
            val cached = runCatchingNonCancellation { store.load(index.paperId, embedder.modelId) }.orEmpty()
            val known = HashMap<Long, FloatArray>()
            hashes.forEach { hash -> cached[hash]?.let { known[hash] = it } }
            val missing = hashes.indices.filter { hashes[it] !in known }.distinctBy { hashes[it] }
            fun consistent(): Map<Long, FloatArray> {
                val dimensions = known.values.groupingBy { it.size }.eachCount().maxByOrNull { it.value }?.key ?: 0
                return known.filterValues { it.size == dimensions && dimensions > 0 }
            }
            // Saved after every batch, so an interrupted run (paper closed, app quit) keeps what it already paid for.
            missing.chunked(batchSize.coerceAtLeast(1)).forEach { batch ->
                val batchTexts = batch.map { texts[it] }
                val embedded = embedder.embed(batchTexts)
                require(embedded.size == batch.size) { "The embedding provider returned ${embedded.size} vectors for ${batch.size} texts." }
                batch.forEachIndexed { position, ordinal -> known[hashes[ordinal]] = embedded[position] }
                onEmbedded(batchTexts)
                runCatchingNonCancellation { store.save(index.paperId, embedder.modelId, consistent()) }
            }
            val vectors = consistent()
            if (missing.isEmpty() && vectors.size != cached.size) {
                runCatchingNonCancellation { store.save(index.paperId, embedder.modelId, vectors) }
            }
            return PaperVectors(embedder.modelId, hashes.map { hash -> vectors[hash]?.let(::normalized) })
        }

        internal fun normalized(vector: FloatArray): FloatArray? {
            var sum = 0.0
            for (value in vector) sum += value.toDouble() * value
            val norm = sqrt(sum)
            if (norm == 0.0 || norm.isNaN()) return null
            return FloatArray(vector.size) { (vector[it] / norm).toFloat() }
        }

        private fun dot(a: FloatArray, b: FloatArray): Double {
            var sum = 0.0
            for (i in a.indices) sum += a[i].toDouble() * b[i]
            return sum
        }

        private inline fun <T> runCatchingNonCancellation(block: () -> T): T? = try {
            block()
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            null
        }
    }
}

/**
 * Hybrid retrieval: BM25 keyword search and embedding similarity, fused by reciprocal rank (RRF). Keywords catch exact
 * terms, numbers and symbols; embeddings catch paraphrases and synonyms. Each result keeps both ranks and the
 * similarity, so the agent and the reader can see why a passage was retrieved. Without vectors, or if embedding the
 * query fails, it falls back to keyword search alone.
 */
class HybridPaperRetriever(
    private val index: PaperIndex,
    private val vectors: PaperVectors?,
    private val rrfK: Int = 60,
    /** Embeds the search query with the same model as [vectors]; null searches by keywords only. */
    private val embedQuery: (suspend (String) -> FloatArray)? = null,
) : PaperRetriever {
    val semanticAvailable: Boolean get() = vectors != null && vectors.coverage > 0 && embedQuery != null

    override suspend fun retrieve(query: RetrievalQuery): List<RetrievedChunk> {
        val limit = query.limit.coerceAtLeast(1)
        val pool = (limit * 4).coerceAtLeast(20)
        val lexical = index.search(query.copy(limit = pool))
        if (!semanticAvailable || query.text.isBlank()) return lexical.take(limit)
        val queryVector = try {
            embedQuery!!(query.text)
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            return lexical.take(limit)
        }
        val semantic = vectors!!.nearest(queryVector, pool) { ordinal -> query.accepts(index.chunks[ordinal]) }
        return fuse(query, lexical, semantic).take(limit)
    }

    private fun fuse(query: RetrievalQuery, lexical: List<RetrievedChunk>, semantic: List<Pair<Int, Double>>): List<RetrievedChunk> {
        val lexicalRanks = lexical.withIndex().associate { (rank, result) -> result.chunk.ordinal to rank + 1 }
        val semanticRanks = semantic.withIndex().associate { (rank, hit) -> hit.first to rank + 1 }
        val similarities = semantic.toMap()
        val lexicalByOrdinal = lexical.associateBy { it.chunk.ordinal }
        val best = 2.0 / (rrfK + 1)
        return (lexicalRanks.keys + semanticRanks.keys).map { ordinal ->
            val chunk = index.chunks[ordinal]
            val lexicalRank = lexicalRanks[ordinal]
            val semanticRank = semanticRanks[ordinal]
            var fused = (lexicalRank?.let { 1.0 / (rrfK + it) } ?: 0.0) + (semanticRank?.let { 1.0 / (rrfK + it) } ?: 0.0)
            if (chunk.kind == ChunkKind.REFERENCES && !query.referencesRequested) fused *= 0.5
            val keyword = lexicalByOrdinal[ordinal]
            RetrievedChunk(
                chunk = chunk,
                score = fused / best,
                matchedTerms = keyword?.matchedTerms.orEmpty(),
                methods = keyword?.methods.orEmpty() + if (semanticRank != null) setOf(RetrievalMethod.SEMANTIC) else emptySet(),
                lexicalRank = lexicalRank,
                semanticRank = semanticRank,
                similarity = similarities[ordinal],
            )
        }.sortedByDescending { it.score }
    }
}
