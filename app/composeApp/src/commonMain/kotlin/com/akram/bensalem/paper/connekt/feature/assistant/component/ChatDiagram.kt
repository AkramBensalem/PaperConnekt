package com.akram.bensalem.paper.connekt.feature.assistant.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

internal const val DIAGRAM_FENCE_LANGUAGE = "diagram"

private val diagramJson = Json { ignoreUnknownKeys = true; isLenient = true }

private data class DiagramPoint(val x: Float, val y: Float)

private data class DiagramModel(
    val width: Float,
    val height: Float,
    val xLabel: String?,
    val yLabel: String?,
    val elements: JsonArray,
)

private fun JsonObject.number(name: String, fallback: Float): Float =
    this[name]?.jsonPrimitive?.doubleOrNull?.toFloat() ?: fallback

private fun JsonObject.string(name: String): String? =
    this[name]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }

private fun parseDiagram(text: String): DiagramModel? = runCatching {
    val root = diagramJson.parseToJsonElement(text.trim()).jsonObject
    val elements = root["elements"]?.jsonArray ?: return null
    DiagramModel(
        width = root.number("width", 640f).coerceIn(240f, 1600f),
        height = root.number("height", 360f).coerceIn(180f, 1000f),
        xLabel = root.string("xLabel"),
        yLabel = root.string("yLabel"),
        elements = elements,
    )
}.getOrNull()

private fun JsonObject.color(name: String, fallback: Color): Color {
    val value = string(name) ?: return fallback
    return runCatching {
        val hex = value.removePrefix("#")
        val argb = when (hex.length) {
            6 -> "FF$hex"
            8 -> hex
            else -> return@runCatching fallback
        }.toLong(16)
        Color(argb.toInt())
    }.getOrDefault(fallback)
}

private fun JsonObject.points(name: String = "points"): List<DiagramPoint> =
    (this[name] as? JsonArray).orEmpty().mapNotNull { item ->
        val point = item.jsonObject
        val x = point["x"]?.jsonPrimitive?.doubleOrNull?.toFloat()
        val y = point["y"]?.jsonPrimitive?.doubleOrNull?.toFloat()
        if (x != null && y != null) DiagramPoint(x, y) else null
    }

private fun drawArrow(
    draw: androidx.compose.ui.graphics.drawscope.DrawScope,
    start: Offset,
    end: Offset,
    color: Color,
    stroke: Float,
) {
    draw.drawLine(color, start, end, strokeWidth = stroke, cap = StrokeCap.Round)
    val angle = atan2(end.y - start.y, end.x - start.x)
    val size = stroke * 4f
    val left = Offset(end.x - size * cos(angle - 0.55f), end.y - size * sin(angle - 0.55f))
    val right = Offset(end.x - size * cos(angle + 0.55f), end.y - size * sin(angle + 0.55f))
    draw.drawLine(color, end, left, strokeWidth = stroke, cap = StrokeCap.Round)
    draw.drawLine(color, end, right, strokeWidth = stroke, cap = StrokeCap.Round)
}

@Composable
internal fun DiagramBlock(code: String, fallbackStyle: TextStyle) {
    val diagram = remember(code) { parseDiagram(code) }
    val background = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f)
    Box(
        Modifier.fillMaxWidth().padding(vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .horizontalScroll(rememberScrollState())
            .padding(10.dp),
    ) {
        if (diagram == null) {
            Text(code.trim(), style = fallbackStyle.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace))
        } else {
            val textMeasurer = rememberTextMeasurer()
            val colors = MaterialTheme.colorScheme
            Column {
                Canvas(
                    Modifier.width(diagram.width.dp)
                        .height(diagram.height.dp)
                        .padding(2.dp),
                ) {
                    val scale = minOf(size.width / diagram.width, size.height / diagram.height)
                    val content = Rect(42f, 18f, size.width - 18f, size.height - 30f)
                    fun point(x: Float, y: Float) = Offset(
                        content.left + x.coerceIn(0f, 1f) * content.width,
                        content.bottom - y.coerceIn(0f, 1f) * content.height,
                    )
                    val axisColor = colors.onSurfaceVariant.copy(alpha = 0.7f)
                    val gridColor = colors.onSurface.copy(alpha = 0.08f)
                    val defaultColor = colors.primary

                    for (step in 1..4) {
                        val x = content.left + content.width * step / 5f
                        val y = content.top + content.height * step / 5f
                        drawLine(gridColor, Offset(x, content.top), Offset(x, content.bottom), 1f)
                        drawLine(gridColor, Offset(content.left, y), Offset(content.right, y), 1f)
                    }
                    drawArrow(this, Offset(content.left, content.bottom), Offset(content.right, content.bottom), axisColor, 2f)
                    drawArrow(this, Offset(content.left, content.bottom), Offset(content.left, content.top), axisColor, 2f)

                    diagram.elements.forEach { raw ->
                        val element = raw.jsonObject
                        val type = element.string("type")?.lowercase() ?: return@forEach
                        val color = element.color("color", defaultColor)
                        val stroke = element.number("stroke", 2f).coerceIn(1f, 8f) / scale.coerceAtLeast(0.5f)
                        when (type) {
                            "axes" -> {
                                drawArrow(this, Offset(content.left, content.bottom), Offset(content.right, content.bottom), color, stroke)
                                drawArrow(this, Offset(content.left, content.bottom), Offset(content.left, content.top), color, stroke)
                            }
                            "line", "arrow" -> {
                                val start = point(element.number("x1", 0f), element.number("y1", 0f))
                                val end = point(element.number("x2", 1f), element.number("y2", 1f))
                                if (type == "arrow") drawArrow(this, start, end, color, stroke)
                                else drawLine(color, start, end, strokeWidth = stroke, cap = StrokeCap.Round)
                            }
                            "curve", "polyline" -> {
                                val points = element.points()
                                if (points.size >= 2) {
                                    val path = Path().apply {
                                        val first = point(points.first().x, points.first().y)
                                        moveTo(first.x, first.y)
                                        points.drop(1).forEach { next ->
                                            val mapped = point(next.x, next.y)
                                            lineTo(mapped.x, mapped.y)
                                        }
                                    }
                                    drawPath(path, color, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
                                }
                            }
                            "point" -> {
                                val center = point(element.number("x", 0f), element.number("y", 0f))
                                drawCircle(color, element.number("radius", 4f).coerceIn(2f, 12f) / scale.coerceAtLeast(0.5f), center)
                            }
                            "circle" -> {
                                val center = point(element.number("x", 0f), element.number("y", 0f))
                                drawCircle(color, element.number("radius", 0.1f) * content.width, center, style = Stroke(stroke))
                            }
                            "rect" -> {
                                val left = point(element.number("x", 0f), element.number("y", 1f))
                                val right = point(
                                    element.number("x", 0f) + element.number("width", 0.2f),
                                    element.number("y", 1f) - element.number("height", 0.2f),
                                )
                                drawRect(
                                    color = color,
                                    topLeft = left,
                                    size = Size(right.x - left.x, right.y - left.y),
                                    style = Stroke(stroke),
                                )
                            }
                            "text" -> {
                                val label = element.string("text") ?: return@forEach
                                val position = point(element.number("x", 0f), element.number("y", 0f))
                                val text = textMeasurer.measure(
                                    label,
                                    androidx.compose.ui.text.TextStyle(
                                        color = color,
                                        fontSize = element.number("size", 14f).sp,
                                    ),
                                )
                                drawText(text, topLeft = position)
                            }
                        }
                    }
                }
                val labels = listOfNotNull(diagram.xLabel, diagram.yLabel).joinToString(" · ")
                if (labels.isNotBlank()) {
                    Text(
                        labels,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}
