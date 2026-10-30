package com.paperconnekt.knowledge

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PaperIndexTest {
    private val pages = listOf(
        """
        # Abstract
        We propose a retrieval model for scientific documents that grounds answers in cited passages.
        # 1 Introduction
        Large language models hallucinate when they lack evidence. Retrieval reduces hallucination.
        """.trimIndent(),
        """
        # 2 Method
        Our contrastive loss uses a temperature of 0.07 and hard negatives mined with BM25.
        # 3 Experiments
        On the SciFact benchmark the model reaches 72.4 F1, improving over the baseline.
        """.trimIndent(),
        """
        # References
        [1] Robertson. The probabilistic relevance framework: BM25 and beyond. 2009.
        """.trimIndent(),
    )
    private val index = PaperIndex.build("paper", pages, MarkdownChunker(minChars = 10))

    @Test
    fun searchRanksTheRelevantPassageFirstWithCitation() {
        val results = index.search(RetrievalQuery("what temperature does the contrastive loss use"))

        val top = results.first().chunk
        assertEquals(listOf("2 Method"), top.sectionPath)
        assertEquals(2, top.firstPage)
        assertTrue("temperature" in results.first().matchedTerms)
    }

    @Test
    fun referencesAreDownWeightedUnlessRequested() {
        val default = index.search(RetrievalQuery("BM25"))
        val references = index.search(RetrievalQuery("BM25", kinds = setOf(ChunkKind.REFERENCES)))

        assertEquals(ChunkKind.TEXT, default.first().chunk.kind)
        assertTrue(references.all { it.chunk.kind == ChunkKind.REFERENCES })
    }

    @Test
    fun pageFilterRestrictsResults() {
        val results = index.search(RetrievalQuery("model", pages = setOf(1)))

        assertTrue(results.isNotEmpty())
        assertTrue(results.all { it.chunk.firstPage == 1 })
    }

    @Test
    fun exactPhraseFindsLiteralOccurrences() {
        val matches = index.findExact("72.4 F1")

        assertEquals(1, matches.size)
        assertEquals(2, matches.single().chunk.firstPage)
        assertTrue(matches.single().snippet.contains("72.4 F1"))
    }

    @Test
    fun abstractIsClassifiedAndOutlineFollowsReadingOrder() {
        assertEquals(ChunkKind.ABSTRACT, index.chunks.first().kind)
        assertEquals(
            listOf("Abstract", "1 Introduction", "2 Method", "3 Experiments", "References"),
            index.outline().map { it.sectionPath.last() },
        )
    }

    @Test
    fun neighborsReturnSurroundingChunks() {
        val method = index.chunks.first { it.sectionPath == listOf("2 Method") }

        assertEquals(listOf(method.ordinal - 1, method.ordinal, method.ordinal + 1), index.neighbors(method.id).map { it.ordinal })
    }

    @Test
    fun formattedResultsCarryChunkIdsAndPages() {
        val text = KnowledgeFormat.results("temperature", index.search(RetrievalQuery("temperature")))

        assertTrue(text.contains("p. 2"))
        assertTrue(Regex("\\[c\\d+]").containsMatchIn(text))
    }
}
