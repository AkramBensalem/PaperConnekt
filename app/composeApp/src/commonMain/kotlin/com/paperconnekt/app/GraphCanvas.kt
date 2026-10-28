package com.paperconnekt.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.paperconnekt.domain.model.Paper
import com.paperconnekt.domain.model.ResearchGraph
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun GraphCanvasPanel(
    modifier: Modifier = Modifier,
    graph: ResearchGraph,
    papers: List<Paper>,
    selectedPaperId: String?,
    zoom: Float,
    onZoomChange: (Float) -> Unit,
    onSelectPaper: (String) -> Unit,
) {
    val textMeasurer = rememberTextMeasurer()
    val colorScheme = MaterialTheme.colorScheme
    val labelSmall = MaterialTheme.typography.labelSmall
    var pan by remember(graph.id) { mutableStateOf(Offset.Zero) }
    var nodePositions by remember(graph.id) { mutableStateOf<Map<String, Offset>>(emptyMap()) }
    val canvasModifier = modifier
        .clip(MaterialTheme.shapes.large)
        .pointerInput(graph.id, papers, zoom, pan, nodePositions) {
            var draggedPaperId: String? = null
            detectDragGestures(
                onDragStart = { start ->
                    val canvasSize = Size(size.width.toFloat(), size.height.toFloat())
                    draggedPaperId = papers.indices.firstOrNull { index ->
                        val position = transformedPosition(
                            positionFor(papers, index, canvasSize, nodePositions),
                            canvasSize,
                            zoom,
                            pan,
                        )
                        (start - position).getDistance() <= nodeRadius(size.width.toFloat())
                    }?.let(papers::get)?.id
                },
                onDragEnd = { draggedPaperId = null },
                onDragCancel = { draggedPaperId = null },
            ) { change, dragAmount ->
                val paperId = draggedPaperId
                if (paperId == null) {
                    pan += dragAmount
                } else {
                    val canvasSize = Size(size.width.toFloat(), size.height.toFloat())
                    val current = positionFor(
                        papers,
                        papers.indexOfFirst { it.id == paperId },
                        canvasSize,
                        nodePositions,
                    )
                    nodePositions = nodePositions + (paperId to (current + dragAmount / zoom))
                    change.consume()
                }
            }
        }
        .pointerInput(graph.id) {
            detectTransformGestures { _, panChange, zoomChange, _ ->
                pan += panChange
                onZoomChange((zoom * zoomChange).coerceIn(0.65f, 2.4f))
            }
        }
        .pointerInput(graph.id, zoom, pan, papers, selectedPaperId) {
            detectTapGestures { tapPosition ->
                val index = papers.indices.firstOrNull { index ->
                    val canvasSize = Size(size.width.toFloat(), size.height.toFloat())
                    val position = transformedPosition(
                        positionFor(papers, index, canvasSize, nodePositions),
                        canvasSize,
                        zoom,
                        pan,
                    )
                    (tapPosition - position).getDistance() <= nodeRadius(size.width.toFloat())
                }
                index?.let { onSelectPaper(papers[it].id) }
            }
        }

    Box(modifier = canvasModifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(colorScheme.surfaceContainerLowest)
            val canvasSize = size
            val positions = papers.mapIndexed { index, paper ->
                paper.id to positionFor(papers, index, canvasSize, nodePositions)
            }.toMap()
            graph.connections.forEach { connection ->
                val fromIndex = papers.indexOfFirst { it.id == connection.fromPaperId }
                val toIndex = papers.indexOfFirst { it.id == connection.toPaperId }
                if (fromIndex >= 0 && toIndex >= 0) {
                    val from = transformedPosition(
                        positions[papers[fromIndex].id] ?: Offset.Zero,
                        canvasSize,
                        zoom,
                        pan,
                    )
                    val to = transformedPosition(
                        positions[papers[toIndex].id] ?: Offset.Zero,
                        canvasSize,
                        zoom,
                        pan,
                    )
                    val edgeColor = when (connection.type) {
                            com.paperconnekt.domain.model.ConnectionType.CITATION -> colorScheme.primary
                            com.paperconnekt.domain.model.ConnectionType.SHARED_REFERENCE -> colorScheme.tertiary
                            com.paperconnekt.domain.model.ConnectionType.RELATED_TOPIC -> colorScheme.outlineVariant
                        }
                    drawLine(
                        color = edgeColor,
                        start = from,
                        end = to,
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                    drawArrowHead(
                        color = edgeColor,
                        from = from,
                        to = to,
                    )
                }
            }
            papers.forEachIndexed { index, paper ->
                val center = transformedPosition(
                    positions[paper.id] ?: Offset.Zero,
                    canvasSize,
                    zoom,
                    pan,
                )
                val selected = paper.id == selectedPaperId
                val connections = graph.connections.count {
                    it.fromPaperId == paper.id || it.toPaperId == paper.id
                }
                val radius = (nodeRadius(size.width) + connections * 1.5f).coerceAtMost(48f)
                drawCircle(
                    color = if (selected) {
                        colorScheme.primary
                    } else if (graph.paperIds.firstOrNull() == paper.id) {
                        colorScheme.primary
                    } else if (graph.connections.any {
                            it.fromPaperId == paper.id && it.toPaperId != paper.id
                        }) {
                        colorScheme.tertiary
                    } else {
                        colorScheme.secondary
                    },
                    radius = radius,
                    center = center,
                )
                drawCircle(
                    color = colorScheme.surface,
                    radius = radius - 5.dp.toPx(),
                    center = center,
                    style = Stroke(width = 2.dp.toPx()),
                )
                val label = paper.title.take(24)
                val measured = textMeasurer.measure(
                    label,
                    style = labelSmall,
                )
                drawText(
                    textLayoutResult = measured,
                    topLeft = Offset(
                        x = center.x - measured.size.width / 2f,
                        y = center.y + radius + 8.dp.toPx(),
                    ),
                    color = colorScheme.onSurface,
                )
            }
        }
        Card(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    "-",
                    modifier = Modifier
                    .semantics { contentDescription = "Zoom out" }
                    .clip(MaterialTheme.shapes.small)
                    .pointerInput(Unit) {
                            detectTapGestures {
                                onZoomChange((zoom - 0.15f).coerceIn(0.65f, 2.4f))
                            }
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    "${(zoom * 100).toInt()}%",
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 7.dp),
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(
                    "+",
                    modifier = Modifier
                    .semantics { contentDescription = "Zoom in" }
                    .clip(MaterialTheme.shapes.small)
                        .pointerInput(Unit) {
                            detectTapGestures {
                                onZoomChange((zoom + 0.15f).coerceIn(0.65f, 2.4f))
                            }
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
        if (papers.isEmpty()) {
            Card(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
            ) {
                Text(
                    "Add saved papers to see your research map.",
                    modifier = Modifier.padding(18.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Text(
                "Drag to pan • pinch or use +/- to zoom • select a node for details",
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(12.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun positionFor(
    papers: List<Paper>,
    index: Int,
    size: Size,
    saved: Map<String, Offset>,
): Offset {
    val paper = papers[index]
    saved[paper.id]?.let { return it }
    if (index == 0) return Offset(size.width / 2f, size.height / 2f)
    val neighbors = (papers.size - 1).coerceAtLeast(1)
    val angle = (index - 1).toFloat() / neighbors * (2f * PI.toFloat())
    val radius = (size.minDimension * 0.32f).coerceAtLeast(120f)
    return Offset(
        size.width / 2f + cos(angle) * radius,
        size.height / 2f + sin(angle) * radius,
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawArrowHead(
    color: Color,
    from: Offset,
    to: Offset,
) {
    val angle = atan2(to.y - from.y, to.x - from.x)
    val tip = from + (to - from) * 0.64f
    val length = 10.dp.toPx()
    val left = Offset(
        tip.x - cos(angle - 0.55f) * length,
        tip.y - sin(angle - 0.55f) * length,
    )
    val right = Offset(
        tip.x - cos(angle + 0.55f) * length,
        tip.y - sin(angle + 0.55f) * length,
    )
    drawPath(
        path = Path().apply {
            moveTo(tip.x, tip.y)
            lineTo(left.x, left.y)
            lineTo(right.x, right.y)
            close()
        },
        color = color,
    )
}

private fun transformedPosition(
    position: Offset,
    size: Size,
    zoom: Float,
    pan: Offset,
): Offset {
    val center = Offset(size.width / 2f, size.height / 2f)
    return center + (position - center) * zoom + pan
}

private fun nodeRadius(width: Float): Float = (width.coerceAtLeast(240f) / 18f).coerceIn(24f, 42f)
