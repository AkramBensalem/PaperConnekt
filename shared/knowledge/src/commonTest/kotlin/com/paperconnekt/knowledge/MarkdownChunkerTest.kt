package com.paperconnekt.knowledge

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MarkdownChunkerTest {
    private val chunker = MarkdownChunker(targetChars = 400, maxChars = 700, minChars = 80)

    @Test
    fun headingsStartChunksAndKeepTheSectionTrail() {
        val pages = listOf(
            """
            # Method
            ${"We train the encoder with contrastive pairs. ".repeat(4)}
            ## Loss
            ${"The loss combines a margin term and a temperature. ".repeat(4)}
            """.trimIndent(),
        )

        val chunks = chunker.chunk("paper", pages)

        assertEquals(listOf(listOf("Method"), listOf("Method", "Loss")), chunks.map { it.sectionPath })
        assertTrue(chunks.all { it.firstPage == 1 && it.lastPage == 1 })
        assertEquals(listOf("c1", "c2"), chunks.map { it.id })
    }

    @Test
    fun shortTrailingParagraphContinuesOnTheNextPage() {
        val pages = listOf("# Intro\nShort start.", "continues here with ${"more words ".repeat(12)}")

        val chunks = chunker.chunk("paper", pages)

        assertEquals(1, chunks.size)
        assertEquals(1, chunks.single().firstPage)
        assertEquals(2, chunks.single().lastPage)
        assertEquals("pp. 1–2", chunks.single().pageLabel)
    }

    @Test
    fun tablesAndReferencesAreClassified() {
        val table = (1..6).joinToString("\n") { "| model $it | ${it * 10} |" }
        val pages = listOf(
            "# Results\n${"Accuracy improves on all benchmarks. ".repeat(4)}\n\n$table",
            "# References\n[1] A. Author. A cited work. 2020.",
        )

        val chunks = chunker.chunk("paper", pages)

        assertTrue(chunks.any { it.kind == ChunkKind.TABLE && it.text.contains("| model 3 |") })
        assertEquals(ChunkKind.REFERENCES, chunks.last().kind)
    }

    @Test
    fun oversizedParagraphIsSplitWithinLimits() {
        val pages = listOf("# Long\n" + "This sentence is part of a very long paragraph. ".repeat(60))

        val chunks = chunker.chunk("paper", pages)

        assertTrue(chunks.size > 1)
        assertTrue(chunks.all { it.text.length <= 700 })
    }

    @Test
    fun imagesBecomeFigurePlaceholders() {
        val chunks = chunker.chunk("paper", listOf("Intro ![plot](data:image/png;base64,AAAA) text."))

        assertEquals("Intro [figure] text.", chunks.single().text)
    }
}
