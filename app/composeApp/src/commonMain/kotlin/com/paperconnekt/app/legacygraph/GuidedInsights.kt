@file:OptIn(ExperimentalTime::class)

package com.paperconnekt.app.legacygraph

import com.paperconnekt.app.legacygraph.RelevanceScore
import kotlin.math.log10
import kotlin.time.ExperimentalTime

/** Where a candidate stands in the guided review of the selected paper. */
enum class InsightStatus(val label: String) {
    KEPT("Kept"),
    SKIPPED("Skipped"),
    AI_KEEP("AI suggests keep"),
    AI_SKIP("AI suggests skip"),
    IN_BATCH("In current batch"),
    UNREVIEWED("Not reviewed")
}

/** Year × citations quadrants, split at the median year and median citations. */
enum class ImpactQuadrant(val label: String, val hint: String) {
    RISING_STARS("Rising stars", "Recent and already highly cited"),
    CLASSICS("Classics", "Older and highly cited: foundational work"),
    EMERGING("Emerging", "Recent with few citations so far"),
    OVERLOOKED("Overlooked", "Older and rarely cited")
}

/** Similarity × citations quadrants, split at the median similarity and median citations. */
enum class RelevanceQuadrant(val label: String, val hint: String) {
    MUST_READ("Must read", "Close to this paper and highly cited"),
    HIDDEN_GEMS("Hidden gems", "Close to this paper but rarely cited"),
    POPULAR("Popular, less related", "Highly cited but further from this paper"),
    PERIPHERAL("Peripheral", "Further from this paper and rarely cited")
}

data class InsightPaper(
    val candidate: GuidedCandidate,
    val year: Int?,
    val citations: Int,
    val status: InsightStatus,
    /** Citations per year since publication; highlights fast-rising articles. */
    val citationsPerYear: Double?,
    /** AI match with the user's research target (0..1), null when not evaluated yet. */
    val aiRelevance: Float? = null,
    val aiRationale: String? = null
) {
    val id: String get() = candidate.node.paper.id
    val similarity: Float get() = candidate.localScore
}

data class InsightYearBucket(val year: Int, val references: Int, val citations: Int, val kept: Int) {
    val total: Int get() = references + citations
}

data class InsightRankedLabel(val label: String, val count: Int, val kept: Int)

data class GuidedInsights(
    val papers: List<InsightPaper>,
    val splitYear: Int?,
    val splitCitations: Int,
    val splitSimilarity: Float,
    val years: List<InsightYearBucket>,
    val topAuthors: List<InsightRankedLabel>,
    val topVenues: List<InsightRankedLabel>,
    val rising: List<InsightPaper>,
    val missingYear: Int,
    /** When true, relevance is the AI match with the research target instead of text similarity. */
    val usesAiRelevance: Boolean = false
) {
    /** Position on the relevance axis; null when the AI has not evaluated this paper yet. */
    fun relevanceValue(paper: InsightPaper): Float? = if (usesAiRelevance) paper.aiRelevance else paper.similarity

    fun impactOf(paper: InsightPaper): ImpactQuadrant? {
        val year = paper.year ?: return null
        val split = splitYear ?: return null
        val recent = year >= split
        val high = paper.citations > splitCitations
        return when {
            recent && high -> ImpactQuadrant.RISING_STARS
            high -> ImpactQuadrant.CLASSICS
            recent -> ImpactQuadrant.EMERGING
            else -> ImpactQuadrant.OVERLOOKED
        }
    }

    fun relevanceOf(paper: InsightPaper): RelevanceQuadrant? {
        val close = (relevanceValue(paper) ?: return null) >= splitSimilarity
        val high = paper.citations > splitCitations
        return when {
            close && high -> RelevanceQuadrant.MUST_READ
            close -> RelevanceQuadrant.HIDDEN_GEMS
            high -> RelevanceQuadrant.POPULAR
            else -> RelevanceQuadrant.PERIPHERAL
        }
    }
}

internal fun insightYear(node: PaperNode): Int? =
    node.paper.year?.takeIf { it in 1000..9999 }

internal fun insightCitationScale(citations: Int): Double = log10(1.0 + citations.coerceAtLeast(0))

internal fun currentInsightYear(): Int = (1970 + kotlin.time.Clock.System.now().toEpochMilliseconds() / 31_556_952_000L).toInt()

internal data class InsightInterval(
    val firstYear: Int? = null,
    val lastYear: Int? = null,
    val minCitations: Int? = null,
    val maxCitations: Int? = null
) {
    fun includes(candidate: GuidedCandidate): Boolean {
        val year = insightYear(candidate.node)
        val citations = candidate.node.citationCount.coerceAtLeast(0)
        return (firstYear == null || (year != null && year >= firstYear)) &&
            (lastYear == null || (year != null && year <= lastYear)) &&
            (minCitations == null || citations >= minCitations) &&
            (maxCitations == null || citations <= maxCitations)
    }
}

/** Includes retained nodes from every review, regardless of depth or connection to a seed. */
internal fun graphInsightsReview(graph: PaperGraph, keptIds: Set<String>, excludedIds: Set<String>): GuidedReviewState {
    val nodes = graph.nodes.filter {
        (it.connectionType == ConnectionType.CENTRAL || it.paper.id in keptIds) &&
            it.paper.id !in excludedIds
    }.distinctBy { it.paper.id }
    val ids = nodes.mapTo(mutableSetOf()) { it.paper.id }
    val seeds = nodes.filter { it.connectionType == ConnectionType.CENTRAL }
    return GuidedReviewState(
        isQueueLoaded = true,
        keptIds = ids,
        candidates = nodes.map { node ->
            val similarity = seeds.maxOfOrNull { paperContentSimilarity(it.paper, node.paper) } ?: 0f
            GuidedCandidate(node, GuidedRelation.REFERENCE, similarity)
        }
    )
}

/**
 * Statistics about the selected paper's citations/references so the user can spot rising,
 * foundational or overlooked articles before deciding which ones to keep.
 */
internal fun buildGuidedInsights(
    review: GuidedReviewState,
    candidates: List<GuidedCandidate>,
    keptElsewhere: Set<String>,
    currentYear: Int,
    topCount: Int = 10,
    /** AI scores for the saved research target; null keeps the local text-similarity relevance. */
    aiScores: Map<String, RelevanceScore>? = null
): GuidedInsights {
    val batch = review.batch.associateBy { it.paperId }
    val papers = candidates.distinctBy { it.node.paper.id }.map { candidate ->
        val id = candidate.node.paper.id
        val year = insightYear(candidate.node)
        val citations = candidate.node.citationCount.coerceAtLeast(0)
        val status = when {
            id in review.keptIds || id in keptElsewhere -> InsightStatus.KEPT
            id in review.skippedIds -> InsightStatus.SKIPPED
            batch[id]?.aiVerdict == GuidedAiVerdict.KEEP -> InsightStatus.AI_KEEP
            batch[id]?.aiVerdict == GuidedAiVerdict.SKIP -> InsightStatus.AI_SKIP
            id in batch -> InsightStatus.IN_BATCH
            else -> InsightStatus.UNREVIEWED
        }
        val velocity = year?.takeIf { it <= currentYear }?.let { citations.toDouble() / (currentYear - it + 1) }
        val ai = aiScores?.get(id)
        InsightPaper(candidate, year, citations, status, velocity, ai?.score?.coerceIn(0, 100)?.div(100f), ai?.rationale)
    }
    val dated = papers.filter { it.year != null }
    val sortedYears = dated.mapNotNull { it.year }.sorted()
    val sortedCitations = papers.map { it.citations }.sorted()
    val sortedSimilarity = papers.map { it.similarity }.sorted()
    val years = dated.groupBy { it.year!! }.entries.sortedBy { it.key }.map { (year, group) ->
        InsightYearBucket(
            year = year,
            references = group.count { it.candidate.relation == GuidedRelation.REFERENCE },
            citations = group.count { it.candidate.relation == GuidedRelation.CITATION },
            kept = group.count { it.status == InsightStatus.KEPT }
        )
    }
    fun rank(labels: (InsightPaper) -> List<String>): List<InsightRankedLabel> {
        val counts = linkedMapOf<String, Pair<String, IntArray>>()
        papers.forEach { paper ->
            labels(paper).map { it.trim() }.filter { it.isNotEmpty() }.distinctBy { it.lowercase() }.forEach { label ->
                val entry = counts.getOrPut(label.lowercase()) { label to IntArray(2) }
                entry.second[0]++
                if (paper.status == InsightStatus.KEPT) entry.second[1]++
            }
        }
        return counts.values
            .map { (label, c) -> InsightRankedLabel(label, c[0], c[1]) }
            .filter { it.count > 1 }
            .sortedWith(compareByDescending<InsightRankedLabel> { it.count }.thenBy { it.label.lowercase() })
            .take(topCount)
    }
    return GuidedInsights(
        papers = papers,
        splitYear = sortedYears.takeIf { it.isNotEmpty() }?.let { it[it.size / 2] },
        splitCitations = sortedCitations.takeIf { it.isNotEmpty() }?.let { it[(it.size - 1) / 2] } ?: 0,
        // An AI score has an absolute meaning, so 50 % separates relevant from unrelated papers.
        splitSimilarity = if (aiScores != null) AI_RELEVANCE_SPLIT
        else sortedSimilarity.takeIf { it.isNotEmpty() }?.let { it[it.size / 2] } ?: 0f,
        years = years,
        topAuthors = rank { it.candidate.node.paper.authors },
        topVenues = rank { listOfNotNull(it.candidate.node.paper.source) },
        rising = papers.filter { (it.citationsPerYear ?: 0.0) > 0.0 }
            .sortedWith(compareByDescending<InsightPaper> { it.citationsPerYear }.thenByDescending { it.similarity })
            .take(topCount * 2),
        missingYear = papers.size - dated.size,
        usesAiRelevance = aiScores != null
    )
}

internal const val AI_RELEVANCE_SPLIT = 0.5f
