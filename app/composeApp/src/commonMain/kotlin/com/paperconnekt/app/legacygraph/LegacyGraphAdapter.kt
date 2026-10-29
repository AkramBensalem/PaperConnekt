package com.paperconnekt.app.legacygraph

import com.paperconnekt.domain.model.Paper
import com.paperconnekt.domain.model.ResearchGraph

fun ResearchGraph.toLegacyGraph(papers: List<Paper>): PaperGraph {
    val papersById = papers.associateBy(Paper::id)
    val seedIds = (seedPaperIds.ifEmpty { paperIds.firstOrNull()?.let(::listOf).orEmpty() })
        .filter { it in paperIds }
        .toSet()
    val nodes = paperIds.mapNotNull { paperId ->
        val paper = papersById[paperId] ?: return@mapNotNull null
        val type = when {
            paperId in seedIds -> ConnectionType.CENTRAL
            connections.any { it.fromPaperId in seedIds && it.toPaperId == paperId } -> ConnectionType.CITED_BY_CENTRAL
            else -> ConnectionType.CITES_CENTRAL
        }
        PaperNode(
            paper = paper,
            citationCount = paper.citationCount ?: 0,
            connectionType = type,
            depth = if (paperId in seedIds) 0 else 1,
            referenceCount = paper.referenceCount ?: 0,
            citingCount = paper.citingCount ?: 0,
        )
    }
    return PaperGraph(
        nodes = nodes,
        edges = connections.map { PaperEdge(it.fromPaperId, it.toPaperId) }.distinct(),
        totalReferences = connections.count { it.fromPaperId in seedIds },
        totalCitations = connections.count { it.toPaperId in seedIds },
    )
}
