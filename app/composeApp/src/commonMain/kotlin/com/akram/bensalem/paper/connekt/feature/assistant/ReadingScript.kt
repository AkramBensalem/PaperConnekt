package com.akram.bensalem.paper.connekt.feature.assistant

import com.akram.bensalem.paper.connekt.core.aiassistant.AgentRequest
import com.akram.bensalem.paper.connekt.core.aiassistant.ResearchAgent
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * One positioned piece of a page in the reading script. [box] is `left, top, right, bottom` normalized to the
 * page (0..1). [read] = false keeps the block visible in `reading.md` but silent during read-aloud.
 */
data class ReadingBlock(
    val id: String,
    val pageIndex: Int,
    val type: String,
    val text: String,
    val box: List<Float>? = null,
    val read: Boolean = true,
    val role: String = "body",
    val level: Int? = null,
    val reason: String? = null,
) {
    val heading: Boolean get() = role == "title" || role == "heading"
}

@Serializable
internal data class ReadingDecision(
    val id: String,
    val read: Boolean = true,
    val role: String? = null,
    val level: Int? = null,
    val reason: String? = null,
)

@Serializable
internal data class ReadingDecisions(val blocks: List<ReadingDecision> = emptyList())

private val readingJson = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

/** OCR block types that are never part of the spoken article. */
private fun deterministicSkip(type: String): String? = when {
    "header" in type -> "page-header"
    "footer" in type -> "page-footer"
    "reference" in type -> "references"
    "image" in type || "figure" in type -> "figure"
    "table" in type -> "table"
    "equation" in type || "formula" in type -> "equation"
    "code" in type -> "code"
    "signature" in type -> "signature"
    else -> null
}

/**
 * Splits the OCR output into reading candidates. Mistral OCR 4.1 blocks (typed and positioned) are used when
 * they were saved; older conversions fall back to the paragraphs of each page's Markdown.
 */
fun readingCandidates(layout: OcrLayout?, pages: List<String>): List<ReadingBlock> {
    val layoutPages = layout?.pages.orEmpty().filter { it.blocks.isNotEmpty() }.associateBy { it.index }
    val count = maxOf(pages.size, (layoutPages.keys.maxOrNull() ?: -1) + 1)
    return (0 until count).flatMap { pageIndex ->
        val page = layoutPages[pageIndex]
        val blocks = mutableListOf<ReadingBlock>()
        fun add(type: String, text: String, box: List<Float>? = null) {
            val clean = text.trim()
            if (clean.isEmpty()) return
            val reason = deterministicSkip(type)
            val title = type == "title" || clean.startsWith("#")
            blocks += ReadingBlock(
                id = "p$pageIndex-b${blocks.size}",
                pageIndex = pageIndex,
                type = type,
                text = clean,
                box = box?.takeIf { it.size == 4 },
                read = reason == null,
                role = when {
                    reason != null -> reason
                    title -> "heading"
                    "list" in type -> "list"
                    "caption" in type -> "caption"
                    else -> "body"
                },
                level = if (title) clean.takeWhile { it == '#' }.length.coerceIn(1, 4) else null,
                reason = reason,
            )
        }
        if (page != null) {
            // Header/footer text is also returned as typed blocks; the page fields are only a fallback.
            if (page.blocks.none { "header" in it.type }) page.header?.let { add("header", it) }
            page.blocks.forEach { add(it.type, it.text, it.box) }
            if (page.blocks.none { "footer" in it.type }) page.footer?.let { add("footer", it) }
        } else {
            markdownParagraphs(pages.getOrNull(pageIndex).orEmpty()).forEach { (type, text) -> add(type, text) }
        }
        blocks
    }
}

private fun markdownParagraphs(markdown: String): List<Pair<String, String>> {
    val result = mutableListOf<Pair<String, String>>()
    val current = mutableListOf<String>()
    var inFence = false
    var inMath = false
    fun flush() {
        if (current.isEmpty()) return
        val text = current.joinToString("\n").trim()
        current.clear()
        if (text.isEmpty()) return
        val type = when {
            text.startsWith("```") -> "code"
            text.startsWith("$$") -> "equation"
            text.startsWith("|") -> "table"
            text.matches(Regex("(?s)^!\\[[^]]*]\\([^)]*\\)\\s*$")) -> "image"
            text.startsWith("#") -> "title"
            text.lines().all { it.trimStart().matches(Regex("^([-*+]|\\d+[.)])\\s+.*")) } -> "list"
            else -> "text"
        }
        result += type to text
    }
    markdown.replace(Regex("<!--[\\s\\S]*?-->"), "\n").lines().forEach { raw ->
        val line = raw.trimEnd()
        if (line.trimStart().startsWith("```")) {
            if (!inFence) flush()
            current += line
            inFence = !inFence
            if (!inFence) flush()
            return@forEach
        }
        if (inFence) { current += line; return@forEach }
        if (line.trim() == "$$") {
            if (!inMath) flush()
            current += line
            inMath = !inMath
            if (!inMath) flush()
            return@forEach
        }
        if (inMath) { current += line; return@forEach }
        when {
            line.isBlank() -> flush()
            line.trimStart().startsWith("#") -> { flush(); current += line.trim(); flush() }
            else -> current += line
        }
    }
    flush()
    return result
}

internal const val READING_SYSTEM_PROMPT = """You prepare research papers to be read aloud, like an audiobook narrator.
You receive OCR blocks of a paper (id, OCR type, text). Decide for every block whether a listener should hear it.

READ (read=true): the paper title, abstract, keywords, section and subsection headings, body paragraphs, lists that
belong to the argument, from the introduction through the conclusions, plus appendix content.

SKIP (read=false): running page headers and footers, page numbers, journal/publisher names and logos, volume/issue,
DOI, URLs, ISSN/ISBN, received/revised/accepted/published dates, copyright and licence notices, "cite this article"
or "ACM Reference Format" blocks, author names, affiliations, emails, ORCID, correspondence, funding and
acknowledgements, author contributions, conflict-of-interest statements, "This article is categorized under" and
CCS/subject classification lists, editor notes, figure or table content, footnotes, references or bibliography
entries and their heading.

Roles: title (paper title), heading (section heading; level 1 = section, 2 = subsection, 3 = deeper), abstract,
keywords, body, list, caption, metadata.
Reasons for skipped blocks: header, footer, page-number, journal, doi, dates, copyright, citation-format, authors,
affiliations, contact, funding, acknowledgements, classification, footnote, figure, table, references, metadata.

Answer with JSON only, no prose: {"blocks":[{"id":"p0-b3","read":true,"role":"heading","level":1},
{"id":"p0-b4","read":false,"role":"metadata","reason":"authors"}]}. Include every id you were given exactly once."""

/** Asks the language model to mark which blocks are spoken and to give their structural role. */
class ReadingScriptCleaner(
    private val agent: ResearchAgent,
    private val maxChunkCharacters: Int = 9_000,
    private val parallelism: Int = 3,
) {
    suspend fun clean(candidates: List<ReadingBlock>): List<ReadingBlock> {
        val pending = candidates.filter { it.read }
        if (pending.isEmpty()) return candidates
        val chunks = chunk(pending)
        val semaphore = Semaphore(parallelism)
        val decisions = coroutineScope {
            chunks.map { blocks -> async { semaphore.withPermit { classify(blocks) } } }.awaitAll()
        }.fold(emptyMap<String, ReadingDecision>()) { acc, map -> acc + map }
        return applyReadingDecisions(candidates, decisions)
    }

    private fun chunk(blocks: List<ReadingBlock>): List<List<ReadingBlock>> {
        val chunks = mutableListOf<MutableList<ReadingBlock>>()
        var size = 0
        blocks.forEach { block ->
            val cost = minOf(block.text.length, BLOCK_PREVIEW) + 40
            if (chunks.isEmpty() || size + cost > maxChunkCharacters) {
                chunks += mutableListOf<ReadingBlock>()
                size = 0
            }
            chunks.last() += block
            size += cost
        }
        return chunks
    }

    private suspend fun classify(blocks: List<ReadingBlock>): Map<String, ReadingDecision> {
        val message = buildString {
            appendLine("Classify these blocks (pages ${blocks.first().pageIndex + 1}-${blocks.last().pageIndex + 1}):")
            blocks.forEach { block ->
                val text = block.text.replace(Regex("\\s+"), " ").let {
                    if (it.length <= BLOCK_PREVIEW) it else it.take(BLOCK_PREVIEW - 120) + " … " + it.takeLast(100)
                }
                appendLine("""{"id":"${block.id}","type":"${block.type}","text":${readingJson.encodeToString(kotlinx.serialization.serializer<String>(), text)}}""")
            }
        }
        val answer = agent.respond(AgentRequest(READING_SYSTEM_PROMPT, emptyList(), message, emptyList()))
        return parseReadingDecisions(answer).associateBy { it.id }
    }

    private companion object {
        const val BLOCK_PREVIEW = 700
    }
}

internal fun parseReadingDecisions(answer: String): List<ReadingDecision> {
    val start = answer.indexOf('{')
    val end = answer.lastIndexOf('}')
    if (start < 0 || end <= start) return emptyList()
    return runCatching { readingJson.decodeFromString(ReadingDecisions.serializer(), answer.substring(start, end + 1)).blocks }
        .getOrDefault(emptyList())
}

/** Merges model decisions into the candidates and silences everything after a references heading. */
internal fun applyReadingDecisions(candidates: List<ReadingBlock>, decisions: Map<String, ReadingDecision>): List<ReadingBlock> {
    var inReferences = false
    return candidates.map { block ->
        val decision = decisions[block.id]
        var result = if (decision == null || !block.read) {
            block
        } else {
            val role = decision.role?.lowercase()?.trim()?.takeIf { it.isNotEmpty() } ?: block.role
            block.copy(
                read = decision.read,
                role = role,
                level = decision.level ?: block.level ?: if (role == "heading") 1 else null,
                reason = if (decision.read) null else decision.reason?.trim()?.takeIf { it.isNotEmpty() } ?: role,
            )
        }
        val plain = result.text.trimStart('#', ' ').trim()
        if (block.type != "header" && block.type != "footer") {
            if (isReferenceHeading(plain) || result.reason == "references" && result.heading) {
                inReferences = true
            } else if (inReferences && result.read && result.heading) {
                inReferences = false
            }
        }
        if (inReferences && result.read) result = result.copy(read = false, reason = "references")
        result
    }
}

private const val READING_HEADER = "<!-- paperconnekt:reading v1 -->"
private const val READING_PAGE = "<!-- paperconnekt:reading-page "
private val blockMarker = Regex("^<!-- block (\\S+) (read|skip)((?: [a-z]+=\\S+)*) -->$")

/**
 * Renders the cleaned article as Markdown a person can read. Skipped blocks stay visible as quoted notes;
 * the invisible `<!-- block … -->` lines carry ids, read/skip flags and positions, and may be edited by hand.
 */
fun renderReadingScript(blocks: List<ReadingBlock>): String = buildString {
    appendLine(READING_HEADER)
    var page = -1
    blocks.forEach { block ->
        if (block.pageIndex != page) {
            page = block.pageIndex
            appendLine()
            appendLine("$READING_PAGE$page -->")
        }
        appendLine()
        val attributes = buildList {
            add("type=${block.type.attributeValue()}")
            add("role=${block.role.attributeValue()}")
            block.level?.let { add("level=$it") }
            block.reason?.let { add("reason=${it.attributeValue()}") }
            block.box?.let { box -> add("box=" + box.joinToString(",") { (kotlin.math.round(it * 10_000f) / 10_000f).toString() }) }
        }.joinToString(" ")
        appendLine("<!-- block ${block.id} ${if (block.read) "read" else "skip"} $attributes -->")
        val text = block.text.trim()
        if (!block.read) {
            val label = (block.reason ?: block.role).replace('-', ' ')
            appendLine("> *Skipped — $label:*")
            text.lines().forEach { appendLine(if (it.isBlank()) ">" else "> $it") }
        } else if (block.heading) {
            val hashes = if (block.role == "title") 1 else ((block.level ?: 1) + 1).coerceIn(2, 6)
            appendLine("#".repeat(hashes) + " " + text.trimStart('#', ' ').replace('\n', ' '))
        } else {
            appendLine(text)
        }
    }
}

private fun String.attributeValue() = replace(Regex("[^A-Za-z0-9_.-]+"), "-").trim('-').ifEmpty { "x" }

/** Parses [renderReadingScript] output back into blocks, grouped by page. */
fun parseReadingScript(script: String): Map<Int, List<ReadingBlock>> {
    if (!script.trimStart().startsWith(READING_HEADER)) return emptyMap()
    val blocks = mutableListOf<ReadingBlock>()
    var page = 0
    var marker: MatchResult? = null
    val body = mutableListOf<String>()
    fun flush() {
        val match = marker ?: return
        marker = null
        val attributes = match.groupValues[3].trim().split(' ').filter { '=' in it }
            .associate { it.substringBefore('=') to it.substringAfter('=') }
        val read = match.groupValues[2] == "read"
        val text = body.joinToString("\n").trim().let { content ->
            if (read) {
                content.replace(Regex("^#{1,6}\\s+"), "")
            } else {
                content.lines().map { it.removePrefix(">").removePrefix(" ") }
                    .dropWhile { it.matches(Regex("^\\*Skipped — .*:\\*$")) }
                    .joinToString("\n").trim()
            }
        }
        body.clear()
        blocks += ReadingBlock(
            id = match.groupValues[1],
            pageIndex = page,
            type = attributes["type"] ?: "text",
            text = text,
            box = attributes["box"]?.split(',')?.mapNotNull { it.toFloatOrNull() }?.takeIf { it.size == 4 },
            read = read,
            role = attributes["role"] ?: "body",
            level = attributes["level"]?.toIntOrNull(),
            reason = attributes["reason"],
        )
    }
    script.lines().forEach { line ->
        val trimmed = line.trim()
        when {
            trimmed.startsWith(READING_PAGE) -> {
                flush()
                page = trimmed.removePrefix(READING_PAGE).removeSuffix("-->").trim().toIntOrNull() ?: page
            }
            blockMarker.matches(trimmed) -> {
                flush()
                marker = blockMarker.matchEntire(trimmed)
            }
            trimmed == READING_HEADER -> Unit
            marker != null -> body += line
        }
    }
    flush()
    return blocks.groupBy { it.pageIndex }
}

/** Builds speech sources from a cleaned page: explicit read/skip, roles and OCR regions; no guessing. */
internal fun readingSpeechSources(blocks: List<ReadingBlock>): List<SpeechSource> = blocks.flatMap { block ->
    if (!block.read) {
        return@flatMap listOf(SpeechSource(block.text, skipReason = block.reason ?: block.role, region = block.box), SpeechSource(""))
    }
    if (block.heading) {
        val text = block.text.lines().joinToString(" ") { it.trim() }
        val plain = markdownSpeechSources(text).joinToString(" ") { it.text }.trim()
        return@flatMap listOf(SpeechSource(plain, SpeechHint.Heading, region = block.box, trusted = true), SpeechSource(""))
    }
    val list = block.role == "list"
    markdownSpeechSources(block.text).flatMap { source ->
        val item = source.copy(
            text = if (list) source.text.replace(Regex("^([-+•]|\\d+[.)])\\s+"), "") else source.text,
            hint = if (source.hint == SpeechHint.Heading) SpeechHint.Heading else SpeechHint.Paragraph,
            region = block.box,
            trusted = true,
        )
        if (list) listOf(item, SpeechSource("")) else listOf(item)
    } + SpeechSource("")
}

/** Speech plan for one page of the AI-cleaned reading script. */
fun createReadingSpeechPlan(blocks: List<ReadingBlock>, maxCharacters: Int = 1800): SpeechPlan =
    createSpeechPlan(readingSpeechSources(blocks), maxCharacters).copy(source = "pdf-page-ai")
