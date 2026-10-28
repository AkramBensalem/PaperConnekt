package com.paperconnekt.app.legacygraph

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.paperconnekt.app.legacygraph.components.GraphCanvas
import com.paperconnekt.domain.model.Paper
import com.paperconnekt.domain.model.ResearchGraph

@Composable
fun LegacyGraphCanvasPanel(
    modifier: Modifier = Modifier,
    graph: ResearchGraph,
    papers: List<Paper>,
    selectedPaperId: String?,
    onSelectPaper: (String) -> Unit,
) {
    val legacyGraph = graph.toLegacyGraph(papers)
    GraphCanvas(
        graph = legacyGraph,
        modifier = modifier,
        selectedNode = legacyGraph.nodes.firstOrNull { it.paper.id == selectedPaperId },
        onNodeClick = { node -> node?.paper?.id?.let(onSelectPaper) },
        showPaperGrid = true,
    )
}
