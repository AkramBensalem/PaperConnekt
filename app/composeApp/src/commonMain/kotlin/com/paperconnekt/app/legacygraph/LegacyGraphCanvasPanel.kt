package com.paperconnekt.app.legacygraph

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.paperconnekt.app.legacygraph.storage.SavedGraphNodePosition
import com.paperconnekt.app.legacygraph.components.GraphCanvas
import com.paperconnekt.domain.model.Paper
import com.paperconnekt.domain.model.GraphNodePosition
import com.paperconnekt.domain.model.ResearchGraph

@Composable
fun LegacyGraphCanvasPanel(
    modifier: Modifier = Modifier,
    graph: ResearchGraph,
    papers: List<Paper>,
    selectedPaperId: String?,
    onSelectPaper: (String?) -> Unit,
    initialNodePositions: Map<String, GraphNodePosition> = emptyMap(),
    onPositionsChanged: (Map<String, GraphNodePosition>) -> Unit = {},
) {
    val legacyGraph = graph.toLegacyGraph(papers)
    GraphCanvas(
        graph = legacyGraph,
        modifier = modifier,
        selectedNode = legacyGraph.nodes.firstOrNull { it.paper.id == selectedPaperId },
        // GraphCanvas reports null for a background click. Forward it so the
        // workspace can close the details panel with one click outside a node.
        onNodeClick = { node -> onSelectPaper(node?.paper?.id) },
        initialNodePositions = initialNodePositions.mapValues { (_, position) ->
            SavedGraphNodePosition(position.x, position.y)
        },
        onPositionsChanged = { positions ->
            onPositionsChanged(positions.mapValues { (_, offset) -> GraphNodePosition(offset.x, offset.y) })
        },
        showPaperGrid = true,
    )
}
