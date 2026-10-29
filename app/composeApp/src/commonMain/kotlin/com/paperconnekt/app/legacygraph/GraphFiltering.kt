package com.paperconnekt.app.legacygraph

internal fun filterGraph(
    graph: PaperGraph,
    includeCiting: Boolean,
    includeCited: Boolean,
    minCitations: Int,
    maxCitations: Int,
    minimumSimilarityPercent: Int,
    showInterconnections: Boolean,
    showDisqualified: Boolean,
    disqualifiedPaperIds: Set<String>,
    nodeSortOrder: GraphNodeSortOrder = GraphNodeSortOrder.DEFAULT
): PaperGraph {
    val seeds = graph.seedNodes
    val seedIds = seeds.map { it.paper.id }.toSet()
    val similarityById = mutableMapOf<String, Float>()
    fun similarity(node: PaperNode): Float = similarityById.getOrPut(node.paper.id) {
        seeds.maxOfOrNull { paperContentSimilarity(it.paper, node.paper) } ?: 0f
    }
    val directReferences = graph.edges.filter { it.fromId in seedIds }.map { it.toId }.toSet()
    val directCitations = graph.edges.filter { it.toId in seedIds }.map { it.fromId }.toSet()
    val hiddenSeeds = graph.nodes
        .filter {
            when {
                it.paper.id in seedIds -> false
                it.paper.id in directReferences || it.paper.id in directCitations ->
                    !((includeCited && it.paper.id in directReferences) || (includeCiting && it.paper.id in directCitations))
                it.connectionType == ConnectionType.CITES_CENTRAL -> !includeCiting
                else -> !includeCited
            }
        }
        .map { it.paper.id }
        .toSet()
    val disqualifiedSeeds = if (showDisqualified) emptySet() else disqualifiedPaperIds - seedIds

    val adjacency = buildMap<String, MutableList<String>> {
        graph.edges.forEach { edge ->
            getOrPut(edge.fromId) { mutableListOf() }.add(edge.toId)
            getOrPut(edge.toId) { mutableListOf() }.add(edge.fromId)
        }
    }
    val directlyHidden = hiddenSeeds + disqualifiedSeeds
    // A shared paper stays visible when it still has a path to any seed
    // through visible papers; a hidden branch cannot hide another seed.
    val reachable = seedIds.toMutableSet()
    val queue = ArrayDeque(seedIds)
    while (queue.isNotEmpty()) {
        adjacency[queue.removeFirst()].orEmpty().forEach { connectedId ->
            if (connectedId !in directlyHidden && reachable.add(connectedId)) {
                queue.addLast(connectedId)
            }
        }
    }

    val minimumSimilarity = minimumSimilarityPercent / 100f
    val filteredNodes = graph.nodes.filter {
        val citationsMatch = it.citationCount >= minCitations && it.citationCount <= maxCitations
        val similarityMatch = it.paper.id in seedIds || minimumSimilarity == 0f || similarity(it) >= minimumSimilarity
        (directlyHidden.isEmpty() || it.paper.id in reachable) &&
            (it.connectionType == ConnectionType.CENTRAL || citationsMatch) &&
            similarityMatch
    }
    val nodeIds = filteredNodes.map { it.paper.id }.toSet()
    val filteredEdges = graph.edges.filter { edge ->
        edge.fromId in nodeIds && edge.toId in nodeIds &&
            (showInterconnections || edge.fromId in seedIds || edge.toId in seedIds)
    }
    val sortedNodes = when (nodeSortOrder) {
        GraphNodeSortOrder.DEFAULT -> filteredNodes
        GraphNodeSortOrder.YEAR -> filteredNodes.sortedByDescending { it.paper.year ?: 0 }
        GraphNodeSortOrder.SIMILARITY -> filteredNodes.sortedByDescending { node ->
            if (node.connectionType == ConnectionType.CENTRAL) 1f else similarity(node)
        }
        GraphNodeSortOrder.CITATIONS -> filteredNodes.sortedByDescending { it.citationCount }
    }
    return graph.copy(nodes = sortedNodes, edges = filteredEdges)
}
