package com.paperconnekt.app.legacygraph.storage

import com.paperconnekt.app.legacygraph.PaperGraph
import com.paperconnekt.app.legacygraph.GraphNodeSortOrder
import com.paperconnekt.app.legacygraph.GraphViewMode
import com.paperconnekt.app.legacygraph.SavedGuidedReview
import com.paperconnekt.domain.model.Paper
import kotlinx.serialization.Serializable

@Serializable
data class SavedGraphSession(
    val id: String,
    val rootPaperId: String,
    val title: String,
    val authors: List<String> = emptyList(),
    val year: String? = null,
    val venue: String? = null,
    val centralPaper: Paper,
    val graph: PaperGraph,
    val nodePositions: Map<String, SavedGraphNodePosition> = emptyMap(),
    val expandedPaperIds: Set<String> = emptySet(),
    val disqualifiedPaperIds: Set<String> = emptySet(),
    val filterCiting: Boolean = true,
    val filterCited: Boolean = true,
    val minCitations: Int = 0,
    val maxCitations: Int = 10000,
    val minimumSimilarityPercent: Int = 0,
    val showInterconnections: Boolean = true,
    val nodeSortOrder: GraphNodeSortOrder = GraphNodeSortOrder.DEFAULT,
    val showDisqualified: Boolean = false,
    val createdAt: Long = 0L,
    val lastModifiedAt: Long = 0L,
    val tags: List<String> = emptyList(),
    val notes: String = "",
    val isTitleCustomized: Boolean = false,
    val viewMode: GraphViewMode = GraphViewMode.FULL,
    val guidedReviews: Map<String, SavedGuidedReview> = emptyMap()
) {
    companion object {
        const val DEFAULT_TITLE = "Untitled Graph"
    }

    // Older sessions used the seed paper's title as the workspace name.
    val displayTitle: String
        get() = title.takeUnless { it.isBlank() || (!isTitleCustomized && it == centralPaper.title) } ?: DEFAULT_TITLE

    val totalNodes: Int get() = graph.nodes.size
    val totalEdges: Int get() = graph.edges.size
    val totalReferences: Int get() = graph.totalReferences
    val totalCitations: Int get() = graph.totalCitations
}

@Serializable
data class SavedGraphNodePosition(
    val x: Float,
    val y: Float
)

@Serializable
data class ResearchHistoryData(
    val sessions: List<SavedGraphSession> = emptyList(),
    val recentQueries: List<String> = emptyList()
)

enum class SavedGraphSortOrder {
    RECENTLY_MODIFIED,
    TITLE,
    NODE_COUNT,
    YEAR
}
