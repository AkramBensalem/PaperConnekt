package com.paperconnekt.app.legacygraph

import com.paperconnekt.domain.model.ConnectionType
import com.paperconnekt.domain.model.Paper
import com.paperconnekt.domain.model.ResearchGraph

fun ResearchGraph.toLegacyGraph(papers: List<Paper>): PaperGraph {
    val papersById = papers.associateBy(Paper::id)
    val seedId = paperIds.firstOrNull()
    val nodes = paperIds.mapNotNull { paperId ->
        val paper = papersById[paperId] ?: return@mapNotNull null
        val type = when {
            paperId == seedId -> ConnectionType.CENTRAL
            connections.any { it.fromPaperId == seedId && it.toPaperId == paperId } -> ConnectionType.CITED_BY_CENTRAL
            else -> ConnectionType.CITES_CENTRAL
        }
        PaperNode(
            paper = paper,
            citationCount = paper.citationCount ?: 0,
            connectionType = type,
            depth = if (paperId == seedId) 0 else 1,
            referenceCount = paper.referenceCount ?: 0,
            citingCount = paper.citingCount ?: 0,
        )
    }
    return PaperGraph(
        nodes = nodes,
        edges = connections.map { PaperEdge(it.fromPaperId, it.toPaperId) }.distinct(),
        totalReferences = connections.count { it.fromPaperId == seedId },
        totalCitations = connections.count { it.toPaperId == seedId },
    )
}
