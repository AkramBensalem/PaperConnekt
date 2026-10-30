package com.paperconnekt.knowledge

import kotlin.math.roundToInt

/** Compact, citation-first text renderings of retrieval results, sized to keep tool outputs cheap for the model. */
object KnowledgeFormat {
    fun header(chunk: PaperChunk): String = "[${chunk.id}] ${chunk.pageLabel} · ${chunk.sectionLabel} · ${chunk.kind.name.lowercase()}"

    fun results(query: String, results: List<RetrievedChunk>, maxChars: Int = 9_000, perChunkChars: Int = 1_400): String {
        if (results.isEmpty()) return "No passages matched \"$query\". Try other wording, synonyms, or mode=exact for a literal term."
        return budgeted(maxChars, "Results for \"$query\" (cite as (p. N, §Section) and keep the chunk IDs):") { builder ->
            results.map { result ->
                buildString {
                    append(header(result.chunk))
                    if (result.hybrid) {
                        append(" · relevance ").append(((result.score * 100).roundToInt() / 100.0))
                        val why = listOfNotNull(result.lexicalRank?.let { "keyword #$it" }, result.semanticRank?.let { "meaning #$it" })
                        append(" (").append(why.joinToString(", ")).append(')')
                    } else {
                        append(" · score ").append(((result.score * 10).roundToInt() / 10.0))
                    }
                    if (result.matchedTerms.isNotEmpty()) append(" · matched: ").append(result.matchedTerms.joinToString(", "))
                    if (RetrievalMethod.PHRASE in result.methods) append(" · exact phrase")
                    append('\n').append(truncate(result.chunk, perChunkChars))
                }
            }.forEach(builder)
        }
    }

    fun exact(phrase: String, matches: List<ExactMatch>): String {
        if (matches.isEmpty()) return "No literal occurrence of \"$phrase\". Try mode=relevant to search by meaning."
        return buildString {
            appendLine("${matches.size} occurrence(s) of \"$phrase\":")
            matches.forEach { appendLine("- [${it.chunk.id}] ${it.chunk.pageLabel} · ${it.chunk.sectionLabel}: ${it.snippet}") }
        }.trimEnd()
    }

    fun chunks(chunks: List<PaperChunk>, maxChars: Int = 12_000): String {
        if (chunks.isEmpty()) return "No chunk matched those IDs. Use search_paper or get_outline to find valid IDs."
        return budgeted(maxChars, "") { builder -> chunks.forEach { builder("${header(it)}\n${it.text}") } }
    }

    fun outline(index: PaperIndex, maxChars: Int = 6_000): String {
        val entries = index.outline()
        if (entries.isEmpty()) return "The paper has no readable text yet."
        return budgeted(maxChars, "Outline of ${index.pageCount} page(s), ${index.chunks.size} chunks:", separator = "\n") { builder ->
            entries.forEach { entry ->
                val depth = (entry.sectionPath.size - 1).coerceAtLeast(0)
                val title = entry.sectionPath.lastOrNull() ?: "Front matter"
                builder("${"  ".repeat(depth)}- $title — p. ${entry.firstPage}, starts at [${entry.firstChunkId}], ${entry.chunkCount} chunk(s)")
            }
        }
    }

    /** Small orientation map for the system prompt: outline plus the abstract, instead of the whole paper. */
    fun paperMap(index: PaperIndex, maxChars: Int = 2_400): String {
        val abstract = index.chunks.firstOrNull { it.kind == ChunkKind.ABSTRACT }
            ?: index.chunks.firstOrNull { it.kind == ChunkKind.TEXT }
        val abstractText = abstract?.let { "${header(it)}\n${truncate(it, maxChars / 2)}" }.orEmpty()
        val outline = outline(index, (maxChars - abstractText.length).coerceAtLeast(600))
        return listOf(outline, abstractText).filter(String::isNotEmpty).joinToString("\n\n")
    }

    private fun truncate(chunk: PaperChunk, maxChars: Int): String =
        if (chunk.text.length <= maxChars) chunk.text
        else chunk.text.take(maxChars).trimEnd() + "… [truncated; call read_chunks with ${chunk.id} for the full passage]"

    private fun budgeted(
        maxChars: Int,
        title: String,
        separator: String = "\n\n",
        items: ((String) -> Unit) -> Unit,
    ): String {
        val builder = StringBuilder(title)
        var count = 0
        var omitted = 0
        items { item ->
            val prefix = when {
                builder.isEmpty() -> ""
                count == 0 -> "\n"
                else -> separator
            }
            if (omitted > 0 || (count > 0 && builder.length + prefix.length + item.length > maxChars)) {
                omitted++
            } else {
                builder.append(prefix).append(item)
                count++
            }
        }
        if (omitted > 0) builder.append("\n\n[$omitted more item(s) omitted to save tokens; narrow the query or request specific IDs]")
        return builder.toString()
    }
}
