package com.paperconnekt.knowledge

/**
 * Splits page-aware OCR Markdown into retrievable chunks that respect structure: headings start new chunks, tables
 * and display equations stay whole when possible, and every chunk remembers its pages and heading trail.
 */
class MarkdownChunker(
    private val targetChars: Int = 1_800,
    private val maxChars: Int = 3_200,
    private val minChars: Int = 300,
) {
    private enum class BlockType { HEADING, PARAGRAPH, TABLE, MATH, CODE }

    private data class Block(val type: BlockType, val text: String, val level: Int = 0)

    /** Chunks [pages] (index 0 is page 1) of the paper identified by [paperId]. */
    fun chunk(paperId: String, pages: List<String>): List<PaperChunk> {
        val chunks = ArrayList<PaperChunk>()
        val sections = ArrayList<Pair<Int, String>>()
        val buffer = StringBuilder()
        var firstPage = 1
        var lastPage = 1
        var sectionPath = emptyList<String>()
        var tableChars = 0
        var mathChars = 0

        fun flush() {
            val text = buffer.toString().trim()
            if (text.isNotEmpty()) {
                val ordinal = chunks.size
                chunks += PaperChunk(
                    id = "c${ordinal + 1}",
                    paperId = paperId,
                    ordinal = ordinal,
                    firstPage = firstPage,
                    lastPage = lastPage,
                    sectionPath = sectionPath,
                    kind = classify(sectionPath, text, tableChars, mathChars),
                    text = text,
                )
            }
            buffer.clear()
            tableChars = 0
            mathChars = 0
        }

        fun append(block: Block, page: Int) {
            if (buffer.isEmpty()) {
                firstPage = page
                sectionPath = sections.map { it.second }
            } else {
                buffer.append("\n\n")
            }
            lastPage = page
            buffer.append(block.text)
            when (block.type) {
                BlockType.TABLE -> tableChars += block.text.length
                BlockType.MATH -> mathChars += block.text.length
                else -> Unit
            }
        }

        pages.forEachIndexed { index, page ->
            val pageNumber = index + 1
            for (block in blocks(page)) {
                when (block.type) {
                    BlockType.HEADING -> {
                        flush()
                        while (sections.isNotEmpty() && sections.last().first >= block.level) sections.removeAt(sections.lastIndex)
                        sections += block.level to block.text
                    }
                    BlockType.TABLE -> {
                        if (buffer.length >= minChars) flush()
                        pieces(block).forEach { piece ->
                            if (buffer.isNotEmpty() && buffer.length + piece.text.length > maxChars) flush()
                            append(piece, pageNumber)
                        }
                        flush()
                    }
                    else -> pieces(block).forEach { piece ->
                        if (buffer.length >= minChars && buffer.length + piece.text.length > targetChars) flush()
                        append(piece, pageNumber)
                    }
                }
            }
            if (buffer.length >= minChars) flush()
        }
        flush()
        return chunks
    }

    private fun pieces(block: Block): List<Block> =
        if (block.text.length <= maxChars) listOf(block) else split(block.text).map { block.copy(text = it) }

    /** Splits an oversized block by lines, then sentences, then a hard cut, packing pieces up to the target size. */
    private fun split(text: String): List<String> {
        val units = text.lines().flatMap { line ->
            if (line.length <= maxChars) listOf(line)
            else line.split(sentenceEnd).flatMap { sentence ->
                if (sentence.length <= maxChars) listOf(sentence) else sentence.chunked(targetChars)
            }
        }
        val pieces = ArrayList<String>()
        val current = StringBuilder()
        for (unit in units) {
            if (current.isNotEmpty() && current.length + unit.length + 1 > targetChars) {
                pieces += current.toString().trim()
                current.clear()
            }
            if (current.isNotEmpty()) current.append('\n')
            current.append(unit)
        }
        if (current.isNotBlank()) pieces += current.toString().trim()
        return pieces.filter(String::isNotEmpty)
    }

    private fun blocks(page: String): List<Block> {
        val blocks = ArrayList<Block>()
        val lines = page.replace(image, "[figure]").lines()
        val paragraph = ArrayList<String>()
        fun endParagraph() {
            if (paragraph.isNotEmpty()) blocks += Block(BlockType.PARAGRAPH, paragraph.joinToString("\n").trim())
            paragraph.clear()
        }
        var index = 0
        while (index < lines.size) {
            val line = lines[index]
            val trimmed = line.trim()
            val heading = headingLine.matchEntire(trimmed)
            when {
                trimmed.isEmpty() -> endParagraph()
                heading != null -> {
                    endParagraph()
                    val title = heading.groupValues[2].replace("**", "").trimEnd('#', ' ').trim()
                    if (title.isNotEmpty()) blocks += Block(BlockType.HEADING, title, heading.groupValues[1].length)
                }
                trimmed.startsWith("```") -> {
                    endParagraph()
                    val start = index
                    index++
                    while (index < lines.size && !lines[index].trim().startsWith("```")) index++
                    blocks += Block(BlockType.CODE, lines.subList(start, minOf(index + 1, lines.size)).joinToString("\n"))
                }
                trimmed.startsWith("$$") -> {
                    endParagraph()
                    val start = index
                    val closedOnSameLine = trimmed.length > 2 && trimmed.substring(2).contains("$$")
                    if (!closedOnSameLine) {
                        index++
                        while (index < lines.size && !lines[index].contains("$$")) index++
                    }
                    blocks += Block(BlockType.MATH, lines.subList(start, minOf(index + 1, lines.size)).joinToString("\n").trim())
                }
                trimmed.startsWith("|") -> {
                    endParagraph()
                    val start = index
                    while (index + 1 < lines.size && lines[index + 1].trim().startsWith("|")) index++
                    blocks += Block(BlockType.TABLE, lines.subList(start, index + 1).joinToString("\n").trim())
                }
                else -> paragraph += line
            }
            index++
        }
        endParagraph()
        return blocks
    }

    private fun classify(sectionPath: List<String>, text: String, tableChars: Int, mathChars: Int): ChunkKind = when {
        sectionPath.any { referencesTitle.matches(it.trim()) } -> ChunkKind.REFERENCES
        sectionPath.lastOrNull()?.let { abstractTitle.matches(it.trim()) } == true -> ChunkKind.ABSTRACT
        sectionPath.isEmpty() && abstractStart.containsMatchIn(text) -> ChunkKind.ABSTRACT
        tableChars * 2 >= text.length -> ChunkKind.TABLE
        mathChars * 2 >= text.length -> ChunkKind.EQUATION
        caption.containsMatchIn(text) && text.length <= targetChars -> ChunkKind.CAPTION
        else -> ChunkKind.TEXT
    }

    private companion object {
        val image = Regex("!\\[[^\\]]*]\\([^)]*\\)")
        val headingLine = Regex("^(#{1,6})\\s+(.+)$")
        val sentenceEnd = Regex("(?<=[.!?])\\s+")
        val referencesTitle = Regex("^([0-9IVX]+(\\.[0-9]+)*\\.?\\s+)?(references|bibliography|works cited|literature cited)$", RegexOption.IGNORE_CASE)
        val abstractTitle = Regex("^(abstract|summary)$", RegexOption.IGNORE_CASE)
        val abstractStart = Regex("^(\\*\\*)?abstract\\b", RegexOption.IGNORE_CASE)
        val caption = Regex("^(\\*\\*)?(Figure|Fig\\.|Table)\\s*\\d+")
    }
}
