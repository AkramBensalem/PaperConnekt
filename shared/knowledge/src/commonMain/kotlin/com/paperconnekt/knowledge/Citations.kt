package com.paperconnekt.knowledge

/**
 * Result of checking the chunk citations of an answer.
 *
 * @property text the answer with the machine-readable `[c12]` markers removed; human-readable page citations stay.
 * @property cited chunk IDs that exist in the paper, in order of first citation.
 * @property unknown cited IDs that do not exist in the paper (fabricated or stale), in order of first citation.
 */
data class CitationReport(val text: String, val cited: List<String>, val unknown: List<String>) {
    val verified: Boolean get() = unknown.isEmpty()
}

/**
 * Deterministic, model-free citation check. The agent tags evidence with chunk IDs such as `[c12]` or `[c3, c4]`;
 * this verifies every ID against the paper index so the UI can show which passages an answer really relies on.
 */
object Citations {
    private val marker = Regex("""[ \t]*\[\s*(c\d+(?:\s*[,;]\s*c\d+)*)\s*]""", RegexOption.IGNORE_CASE)
    private val fence = Regex("""```[\s\S]*?(```|$)""")

    fun hasMarkers(text: String): Boolean = marker.containsMatchIn(text)

    fun verify(text: String, exists: (String) -> Boolean): CitationReport {
        val cited = linkedSetOf<String>()
        val unknown = linkedSetOf<String>()
        val protected = fence.findAll(text).map { it.range }.toList()
        val cleaned = marker.replace(text) { match ->
            if (protected.any { match.range.first in it }) return@replace match.value
            match.groupValues[1].split(',', ';').map { it.trim().lowercase() }.filter(String::isNotEmpty).forEach { id ->
                if (exists(id)) cited += id else unknown += id
            }
            ""
        }
        return CitationReport(cleaned.trim(), cited.toList(), unknown.toList())
    }

    fun verify(text: String, index: PaperIndex): CitationReport = verify(text) { index.chunk(it) != null }
}
