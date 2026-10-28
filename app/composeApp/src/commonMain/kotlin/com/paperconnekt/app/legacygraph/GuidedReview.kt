package com.paperconnekt.app.legacygraph

import kotlinx.serialization.Serializable


enum class GuidedRelation { REFERENCE, CITATION }

data class GuidedCandidate(
    val node: PaperNode,
    val relation: GuidedRelation,
    val localScore: Float
)

/** AI proposal for one suggestion; the user still accepts or overrides it. */
enum class GuidedAiVerdict { KEEP, SKIP }

data class GuidedSuggestion(
    val candidate: GuidedCandidate,
    val reason: String,
    val aiVerdict: GuidedAiVerdict? = null,
    val aiRationale: String? = null,
    val isAiEvaluating: Boolean = false
) {
    val paperId: String get() = candidate.node.paper.id
}

const val GUIDED_DEFAULT_CRITERION =
    "Keep candidates that are directly relevant to the selected paper's research question, method or contribution."

/** Review progress of one paper's citations/references; each paper keeps its own queue. */
data class GuidedReviewState(
    val direction: GuidedReviewDirection = GuidedReviewDirection.BOTH,
    val batchSize: Int = 5,
    val useAi: Boolean = true,
    val focus: String = "",
    val isLoading: Boolean = false,
    val loadingMessage: String? = null,
    val batch: List<GuidedSuggestion> = emptyList(),
    val keptIds: Set<String> = emptySet(),
    val skippedIds: Set<String> = emptySet(),
    val isQueueLoaded: Boolean = false,
    val referenceCount: Int = 0,
    val citationCount: Int = 0,
    val remainingCount: Int = 0,
    val aiEvaluated: Int = 0,
    val aiTotal: Int = 0,
    val error: String? = null,
    /** Every loaded citation/reference of this paper, ranked by similarity; used by the insights plots. */
    val candidates: List<GuidedCandidate> = emptyList()
) {
    val availableCount: Int
        get() = when (direction) {
            GuidedReviewDirection.BOTH -> referenceCount + citationCount
            GuidedReviewDirection.REFERENCES -> referenceCount
            GuidedReviewDirection.CITATIONS -> citationCount
        }
    val reviewedCount: Int get() = keptIds.size + skippedIds.size
    val isAiEvaluating: Boolean get() = batch.any { it.isAiEvaluating }
    val undecided: List<GuidedSuggestion> get() = batch.filter { it.paperId !in keptIds && it.paperId !in skippedIds }
    /** Undecided suggestions whose AI proposal is ready to be validated. */
    val pendingAiProposals: List<GuidedSuggestion> get() = undecided.filter { it.aiVerdict != null }
    val needsAiRetry: Boolean get() = useAi && !isLoading && undecided.any { it.aiVerdict == null }
    val isExhausted: Boolean get() = isQueueLoaded && remainingCount == 0 && batch.none { it.paperId !in keptIds && it.paperId !in skippedIds }
}


internal fun GuidedReviewState.toSaved() = SavedGuidedReview(direction, batchSize, useAi, focus, keptIds, skippedIds)

internal fun SavedGuidedReview.toState() = GuidedReviewState(
    direction = direction,
    batchSize = batchSize,
    useAi = useAi,
    focus = focus,
    keptIds = keptIds,
    skippedIds = skippedIds
)

/** Seeds plus kept papers, with only the edges between them. */
internal fun guidedGraph(graph: PaperGraph, keptPaperIds: Set<String>): PaperGraph {
    val nodes = graph.nodes.filter { it.connectionType == ConnectionType.CENTRAL || it.paper.id in keptPaperIds }
    val ids = nodes.map { it.paper.id }.toSet()
    return graph.copy(nodes = nodes, edges = graph.edges.filter { it.fromId in ids && it.toId in ids })
}

/** All loaded connections in the chosen direction, including papers outside the current batch. */
internal fun guidedConnectionGraph(review: GuidedReviewState, seedIds: Set<String>): PaperGraph =
    PaperGraph(nodes = review.candidates.filter { review.direction.includes(it.relation) }
        .distinctBy { it.node.paper.id }.map { candidate ->
            candidate.node.copy(connectionType = when {
                candidate.node.paper.id in seedIds -> ConnectionType.CENTRAL
                candidate.relation == GuidedRelation.REFERENCE -> ConnectionType.CITED_BY_CENTRAL
                else -> ConnectionType.CITES_CENTRAL
            })
        })

/** Direct neighbors of [paperId], ranked by free local text similarity. */
internal fun rankGuidedCandidates(paperId: String, neighborhood: PaperGraph): List<GuidedCandidate> {
    val source = neighborhood.nodes.firstOrNull { it.paper.id == paperId }?.paper
    val relations = buildMap<String, GuidedRelation> {
        neighborhood.edges.forEach { edge ->
            val (otherId, relation) = when (paperId) {
                edge.fromId -> edge.toId to GuidedRelation.REFERENCE
                edge.toId -> edge.fromId to GuidedRelation.CITATION
                else -> return@forEach
            }
            if (otherId != paperId && otherId !in this) put(otherId, relation)
        }
    }
    return neighborhood.nodes
        .distinctBy { it.paper.id }
        .mapNotNull { node ->
            val relation = relations[node.paper.id] ?: return@mapNotNull null
            GuidedCandidate(node, relation, source?.let { paperContentSimilarity(it, node.paper) } ?: 0f)
        }
        .sortedWith(compareByDescending<GuidedCandidate> { it.localScore }.thenByDescending { it.node.citationCount })
}

internal fun GuidedReviewDirection.includes(relation: GuidedRelation) = when (this) {
    GuidedReviewDirection.BOTH -> true
    GuidedReviewDirection.REFERENCES -> relation == GuidedRelation.REFERENCE
    GuidedReviewDirection.CITATIONS -> relation == GuidedRelation.CITATION
}
