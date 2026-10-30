package com.akram.bensalem.paper.connekt.feature.assistant

import com.paperconnekt.knowledge.CitationReport
import com.paperconnekt.knowledge.PaperChunk
import com.paperconnekt.knowledge.PaperIndex
import com.paperconnekt.knowledge.RetrievalMethod
import com.paperconnekt.knowledge.RetrievedChunk

/** A tool the agent is running or ran during the current reply, shown live under the streaming text. */
data class PaperAgentLiveStep(val id: Int, val label: String, val done: Boolean = false, val ok: Boolean = true)

internal fun PaperChunk.toSource(via: String, score: Double? = null, snippet: String? = null, relevance: Double? = null) = PaperAgentSource(
    chunkId = id,
    firstPage = firstPage,
    lastPage = lastPage,
    section = sectionLabel,
    kind = kind.name.lowercase(),
    excerpt = (snippet ?: text).replace(Regex("\\s+"), " ").trim().let { if (it.length > 240) it.take(239) + "…" else it },
    via = via,
    score = score,
    relevance = relevance,
)

/** Source of a ranked search result, labelled with why it matched, e.g. `keyword #3 + meaning #1`. */
internal fun RetrievedChunk.toSource(): PaperAgentSource {
    if (!hybrid) return chunk.toSource(if (RetrievalMethod.PHRASE in methods) "keyword · phrase" else "keyword", score = score)
    val why = listOfNotNull(lexicalRank?.let { "keyword #$it" }, semanticRank?.let { "meaning #$it" }).joinToString(" + ")
    return chunk.toSource(if (RetrievalMethod.PHRASE in methods) "$why · phrase" else why, relevance = score)
}

/**
 * Sources of one reply: every passage the agent retrieved (deduplicated), plus cited chunks it saw only in the system
 * prompt. Cited passages come first, in citation order, so the evidence behind the answer is the first thing shown.
 */
internal fun replySources(
    retrieved: List<PaperAgentSource>,
    report: CitationReport?,
    index: PaperIndex?,
    max: Int = 24,
): List<PaperAgentSource> {
    val cited = report?.cited.orEmpty()
    val byId = LinkedHashMap<String, PaperAgentSource>()
    retrieved.forEach { source ->
        val previous = byId[source.chunkId]
        val ranked = source.score != null || source.relevance != null
        if (previous == null || (previous.score == null && previous.relevance == null && ranked)) byId[source.chunkId] = source
    }
    cited.forEach { id -> if (id !in byId) index?.chunk(id)?.let { byId[id] = it.toSource("cited") } }
    return byId.values
        .map { it.copy(cited = it.chunkId in cited) }
        .sortedBy { source -> cited.indexOf(source.chunkId).let { if (it < 0) Int.MAX_VALUE else it } }
        .take(max)
}

/** Short, human-readable label of a tool call, e.g. `search paper · "contrastive loss"`. */
internal fun toolStepLabel(name: String, arguments: Map<String, Any?>): String {
    val detail = stepDetailKeys.firstNotNullOfOrNull { key ->
        when (val value = arguments[key]) {
            null -> null
            is List<*> -> value.joinToString(", ")
            is Double -> if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
            else -> value.toString()
        }?.trim()?.takeIf(String::isNotEmpty)?.let { key to it }
    }
    val title = name.replace('_', ' ')
    return when {
        detail == null -> title
        detail.first.endsWith("page") -> "$title · p. ${detail.second}"
        else -> "$title · \"${detail.second.replace(Regex("\\s+"), " ").let { if (it.length > 60) it.take(59) + "…" else it }}\""
    }
}

private val stepDetailKeys = listOf("query", "ids", "phrase", "keywords", "text", "title", "id", "start_page", "page")
