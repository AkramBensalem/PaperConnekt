package com.paperconnekt.knowledge

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SemanticSearchTest {
    private val pages = listOf(
        """
        # Abstract
        We study grounded question answering over scientific papers with cited passages.
        # 1 Introduction
        Large language models invent facts when evidence is missing.
        """.trimIndent(),
        """
        # 2 Method
        Our contrastive loss uses a temperature of 0.07 and hard negatives mined with BM25.
        # 3 Experiments
        On the SciFact benchmark the model reaches 72.4 F1, improving over the baseline.
        """.trimIndent(),
    )
    private val index = PaperIndex.build("paper", pages, MarkdownChunker(minChars = 10))

    /** Maps words to concepts so synonyms share a direction, like a real embedding model would. */
    private class ConceptEmbedder : EmbeddingProvider {
        val sent = mutableListOf<String>()
        override val modelId = "test/concepts"
        private val concepts = listOf(
            setOf("hallucinate", "hallucination", "invent", "fabricate", "facts"),
            setOf("temperature", "contrastive", "loss"),
            setOf("benchmark", "scifact", "f1", "results"),
            setOf("question", "answering", "grounded", "cited"),
        )

        override suspend fun embed(texts: List<String>): List<FloatArray> {
            sent += texts
            return texts.map { text ->
                val words = text.lowercase().split(Regex("[^a-z0-9]+"))
                FloatArray(concepts.size + 1) { dimension ->
                    if (dimension == concepts.size) 0.1f else words.count { it in concepts[dimension] }.toFloat()
                }
            }
        }
    }

    @Test
    fun codecRoundTripsAndRejectsCorruptData() {
        val vectors = mapOf(1L to floatArrayOf(0.5f, -1f, 3.25f), Long.MIN_VALUE to floatArrayOf(0f, 1e-6f, -7f))

        val decoded = assertNotNull(VectorCodec.decode(VectorCodec.encode(vectors)))

        assertEquals(vectors.keys, decoded.keys)
        vectors.forEach { (hash, vector) -> assertTrue(vector.contentEquals(decoded.getValue(hash))) }
        assertNull(VectorCodec.decode(VectorCodec.encode(vectors).copyOf(20)))
        assertNull(VectorCodec.decode(byteArrayOf(1, 2, 3)))
    }

    @Test
    fun contentHashIsStableAndDistinguishesTexts() {
        assertEquals(contentHash("same text"), contentHash("same text"))
        assertTrue(contentHash("same text") != contentHash("same texts"))
    }

    @Test
    fun buildEmbedsOnlyChangedChunks() = runTest {
        val store = MemoryVectorStore()
        val first = ConceptEmbedder()
        PaperVectors.build(index, first, store)
        assertEquals(index.chunks.size, first.sent.size)

        val edited = PaperIndex.build("paper", listOf(pages[0], pages[1].replace("72.4", "73.1")), MarkdownChunker(minChars = 10))
        val second = ConceptEmbedder()
        val vectors = PaperVectors.build(edited, second, store)

        assertEquals(1, second.sent.size)
        assertTrue("73.1" in second.sent.single())
        assertEquals(edited.chunks.size, vectors.coverage)
    }

    @Test
    fun hybridSearchFindsParaphrasesThatKeywordsMiss() = runTest {
        val embedder = ConceptEmbedder()
        val vectors = PaperVectors.build(index, embedder, MemoryVectorStore())
        val query = RetrievalQuery("do LLMs hallucinate", limit = 3)
        assertTrue(index.search(query).none { it.chunk.sectionPath == listOf("1 Introduction") })

        val retriever = HybridPaperRetriever(index, vectors) { embedder.embed(listOf(it)).single() }
        val top = retriever.retrieve(query).first()

        assertEquals(listOf("1 Introduction"), top.chunk.sectionPath)
        assertTrue(RetrievalMethod.SEMANTIC in top.methods)
        assertEquals(1, top.semanticRank)
        assertTrue(KnowledgeFormat.results(query.text, listOf(top)).contains("meaning #1"))
    }

    @Test
    fun hybridSearchFallsBackToKeywordsWhenTheQueryCannotBeEmbedded() = runTest {
        val vectors = PaperVectors.build(index, ConceptEmbedder(), MemoryVectorStore())
        val retriever = HybridPaperRetriever(index, vectors) { error("offline") }
        val query = RetrievalQuery("contrastive loss temperature")

        val results = retriever.retrieve(query)

        assertEquals(index.search(query).map { it.chunk.id }, results.map { it.chunk.id })
        assertTrue(results.none { it.hybrid })
    }

    @Test
    fun hybridSearchRespectsPageFilters() = runTest {
        val embedder = ConceptEmbedder()
        val vectors = PaperVectors.build(index, embedder, MemoryVectorStore())
        val retriever = HybridPaperRetriever(index, vectors) { embedder.embed(listOf(it)).single() }

        val results = retriever.retrieve(RetrievalQuery("invented facts", pages = setOf(2)))

        assertTrue(results.all { it.chunk.firstPage == 2 })
    }
}
