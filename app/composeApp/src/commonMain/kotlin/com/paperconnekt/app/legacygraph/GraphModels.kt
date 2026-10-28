package com.paperconnekt.app.legacygraph

import com.paperconnekt.domain.model.Paper
import kotlinx.serialization.Serializable

@Serializable
data class PaperNode(
    val paper: Paper,
    val citationCount: Int = 0,
    val connectionType: ConnectionType = ConnectionType.CENTRAL,
    val depth: Int = 0,
    val similarity: Float? = null,
    val referenceCount: Int = 0,
    val citingCount: Int = 0
)

@Serializable
enum class ConnectionType {
    CENTRAL,
    CITED_BY_CENTRAL, // This paper is in central's bibliography
    CITES_CENTRAL // This paper is newer and cites the central one
}

@Serializable
enum class GraphNodeSortOrder {
    DEFAULT,
    YEAR,
    SIMILARITY,
    CITATIONS
}

@Serializable
data class PaperEdge(val fromId: String, val toId: String)

@Serializable
data class PaperGraph(
    val nodes: List<PaperNode> = emptyList(),
    val edges: List<PaperEdge> = emptyList(),
    val totalReferences: Int = nodes.count { it.connectionType == ConnectionType.CITED_BY_CENTRAL },
    val totalCitations: Int = nodes.count { it.connectionType == ConnectionType.CITES_CENTRAL }
)

/** FULL shows every loaded neighbor; GUIDED shows only seeds and papers the user kept. */
@Serializable
enum class GraphViewMode { FULL, GUIDED }

@Serializable
enum class GuidedReviewDirection { BOTH, REFERENCES, CITATIONS }

@Serializable
data class SavedGuidedReview(
    val direction: GuidedReviewDirection = GuidedReviewDirection.BOTH,
    val batchSize: Int = 5,
    val useAi: Boolean = true,
    val focus: String = "",
    val keptIds: Set<String> = emptySet(),
    val skippedIds: Set<String> = emptySet()
)
