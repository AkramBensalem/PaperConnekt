package com.akram.bensalem.paper.connekt.feature.assistant

import com.paperconnekt.knowledge.EmbeddingProvider
import com.paperconnekt.knowledge.HybridPaperRetriever
import com.paperconnekt.knowledge.PaperIndex
import com.paperconnekt.knowledge.PaperVectors
import com.paperconnekt.knowledge.VectorStore
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/** Platform storage for chunk embeddings; vectors are derived data, kept outside synced and backed-up folders. */
expect fun defaultPaperVectorStore(): VectorStore

/**
 * Builds and keeps the retrieval index of recently opened papers, plus their chunk embeddings for semantic search.
 *
 * Indexes are derived from the canonical OCR Markdown, so they live in memory only and are rebuilt whenever that
 * Markdown changes. Embeddings cost money, so they are persisted in [vectorStore] by chunk-content hash: a paper is
 * embedded once, and an edited paper re-embeds only its changed passages. Embedding runs in the background on
 * [scope]; retrieval waits briefly for it and otherwise uses keyword search, so the agent never blocks on it.
 */
class PaperKnowledgeCache(
    private val capacity: Int = 4,
    private val compute: CoroutineContext = Dispatchers.Default,
    private val vectorStore: VectorStore = defaultPaperVectorStore(),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    private val retryFailedAfter: Duration = 5.minutes,
) {
    private data class Key(val paperId: String, val pageCount: Int, val contentHash: Int)

    private class Embedding(val index: PaperIndex, val modelId: String, val vectors: Deferred<PaperVectors?>) {
        val startedAt = TimeSource.Monotonic.markNow()
    }

    private val lock = Mutex()
    private val entries = LinkedHashMap<Key, PaperIndex>()
    private val embeddings = HashMap<String, Embedding>()
    private val queryVectors = LinkedHashMap<Pair<String, String>, FloatArray>()

    suspend fun index(paperId: String, pages: List<String>): PaperIndex = withContext(compute) {
        val key = Key(paperId, pages.size, pages.hashCode())
        lock.withLock {
            entries.remove(key)?.let { cached ->
                entries[key] = cached
                return@withLock cached
            }
            entries.keys.removeAll { it.paperId == paperId }
            PaperIndex.build(paperId, pages).also { built ->
                entries[key] = built
                while (entries.size > capacity) entries.remove(entries.keys.first())
            }
        }
    }

    /**
     * Starts (or reuses) the background embedding of [index] with [embedder]. [onEmbedded] receives the texts sent to
     * the provider, for usage accounting. A failed attempt is retried only after [retryFailedAfter].
     */
    suspend fun embed(
        index: PaperIndex,
        embedder: EmbeddingProvider,
        onEmbedded: suspend (List<String>) -> Unit = {},
    ): Deferred<PaperVectors?> = lock.withLock {
        embeddings[index.paperId]?.let { current ->
            val sameInput = current.index === index && current.modelId == embedder.modelId
            val failed = current.vectors.isCompleted && current.vectors.getCompletedOrNull() == null
            if (sameInput && (!failed || current.startedAt.elapsedNow() < retryFailedAfter)) return@withLock current.vectors
            if (!current.vectors.isCompleted) current.vectors.cancel()
        }
        val vectors = scope.async {
            try {
                PaperVectors.build(index, embedder, vectorStore, onEmbedded = onEmbedded)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Napier.w("Could not embed paper ${index.paperId}; semantic search falls back to keywords.", exception)
                null
            }
        }
        embeddings[index.paperId] = Embedding(index, embedder.modelId, vectors)
        while (embeddings.size > capacity) embeddings.remove(embeddings.keys.first())?.vectors?.cancel()
        vectors
    }

    /**
     * Hybrid retriever for [index]: keyword plus embedding search when [embedder] is set and the paper's vectors are
     * ready within [wait], keyword search otherwise. Query embeddings are cached, so repeated searches cost nothing.
     */
    suspend fun retriever(
        index: PaperIndex,
        embedder: EmbeddingProvider?,
        wait: Duration = 4.seconds,
        onEmbedded: suspend (List<String>) -> Unit = {},
    ): HybridPaperRetriever {
        if (embedder == null) return HybridPaperRetriever(index, null)
        val pending = embed(index, embedder, onEmbedded)
        val vectors = if (pending.isCompleted) pending.getCompletedOrNull() else try {
            withTimeoutOrNull(wait) { pending.await() }
        } catch (exception: CancellationException) {
            currentCoroutineContext().ensureActive()
            null // The embedding job was replaced or stopped, not this search.
        }
        return HybridPaperRetriever(index, vectors) { query -> embedQuery(embedder, query) }
    }

    private suspend fun embedQuery(embedder: EmbeddingProvider, query: String): FloatArray {
        val key = embedder.modelId to query.trim()
        lock.withLock { queryVectors[key] }?.let { return it }
        val vector = embedder.embed(listOf(query.trim().take(2_000))).single()
        lock.withLock {
            queryVectors[key] = vector
            while (queryVectors.size > 64) queryVectors.remove(queryVectors.keys.first())
        }
        return vector
    }

    /** Stops background embedding; vectors saved so far stay on disk and are reused next time. */
    fun close() = scope.cancel()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private fun <T> Deferred<T?>.getCompletedOrNull(): T? =
        if (isCancelled) null else runCatching { getCompleted() }.getOrNull()
}
