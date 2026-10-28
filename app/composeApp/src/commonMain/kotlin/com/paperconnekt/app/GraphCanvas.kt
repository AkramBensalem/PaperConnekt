package com.paperconnekt.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.paperconnekt.domain.model.Paper
import com.paperconnekt.domain.model.ResearchGraph
import kotlin.math.ceil
import kotlin.math.sqrt

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
    var pan by remember(graph.id) { mutableStateOf(Offset.Zero) }
    val canvasModifier = modifier
        .clip(MaterialTheme.shapes.large)
        .pointerInput(graph.id) {
            detectTransformGestures { _, panChange, zoomChange, _ ->
                pan += panChange
                onZoomChange((zoom * zoomChange).coerceIn(0.65f, 2.4f))
            }
        }
        .pointerInput(graph.id, zoom, pan, papers, selectedPaperId) {
            detectTapGestures { tapPosition ->
                val index = papers.indices.firstOrNull { index ->
                    val position = transformedPosition(
                        basePosition(index, papers.size, size = Size(size.width.toFloat(), size.height.toFloat())),
                        Size(size.width.toFloat(), size.height.toFloat()),
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
            drawRect(MaterialTheme.colorScheme.surfaceContainerLowest)
            val canvasSize = size
            graph.connections.forEach { connection ->
                val fromIndex = papers.indexOfFirst { it.id == connection.fromPaperId }
                val toIndex = papers.indexOfFirst { it.id == connection.toPaperId }
                if (fromIndex >= 0 && toIndex >= 0) {
                    val from = transformedPosition(
                        basePosition(fromIndex, papers.size, canvasSize),
                        canvasSize,
                        zoom,
                        pan,
                    )
                    val to = transformedPosition(
                        basePosition(toIndex, papers.size, canvasSize),
                        canvasSize,
                        zoom,
                        pan,
                    )
                    drawLine(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        start = from,
                        end = to,
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                    drawCircle(
                        color = MaterialTheme.colorScheme.primary,
                        radius = 3.dp.toPx(),
                        center = Offset(
                            x = from.x + (to.x - from.x) * 0.62f,
                            y = from.y + (to.y - from.y) * 0.62f,
                        ),
                    )
                }
            }
            papers.forEachIndexed { index, paper ->
                val center = transformedPosition(
                    basePosition(index, papers.size, canvasSize),
                    canvasSize,
                    zoom,
                    pan,
                )
                val selected = paper.id == selectedPaperId
                val radius = nodeRadius(size.width)
                drawCircle(
                    color = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.secondary
                    },
                    radius = radius,
                    center = center,
                )
                drawCircle(
                    color = MaterialTheme.colorScheme.surface,
                    radius = radius - 5.dp.toPx(),
                    center = center,
                    style = Stroke(width = 2.dp.toPx()),
                )
                val label = paper.title.take(24)
                val measured = textMeasurer.measure(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                )
                drawText(
                    textLayoutResult = measured,
                    topLeft = Offset(
                        x = center.x - measured.size.width / 2f,
                        y = center.y + radius + 8.dp.toPx(),
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
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

private fun basePosition(index: Int, count: Int, size: Size): Offset {
    if (count == 0) return Offset(size.width / 2f, size.height / 2f)
    val columns = ceil(sqrt(count.toDouble())).toInt().coerceAtLeast(1)
    val rows = ceil(count.toDouble() / columns).toInt().coerceAtLeast(1)
    val column = index % columns
    val row = index / columns
    return Offset(
        x = size.width * ((column + 0.5f) / columns),
        y = size.height * ((row + 0.5f) / rows),
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
