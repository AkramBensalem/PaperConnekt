package com.akram.bensalem.paper.connekt.feature.assistant.component

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import com.akram.bensalem.paper.connekt.feature.assistant.component.MarkdownHostTheme as MaterialTheme
import org.jetbrains.jewel.ui.component.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

internal const val PLOT_FENCE_LANGUAGE = "plot"

private val plotJson = Json { ignoreUnknownKeys = true; isLenient = true }

private data class PlotModel(
    val width: Float,
    val height: Float,
    val title: String?,
    val xLabel: String?,
    val yLabel: String?,
    val data: Map<String, List<Any>>,
    val layers: List<JsonObject>,
)

private fun JsonObject.string(name: String): String? =
    this[name]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }

private fun JsonObject.number(name: String, fallback: Float): Float =
    this[name]?.jsonPrimitive?.doubleOrNull?.toFloat() ?: fallback

private fun JsonArray.values(): List<Any> = map { item ->
    val primitive = item.jsonPrimitive
    primitive.doubleOrNull ?: primitive.content
}

private fun parsePlot(text: String): PlotModel? = runCatching {
    val root = plotJson.parseToJsonElement(text.trim()).jsonObject
    val data = root["data"]?.jsonObject
        ?.mapValues { (_, value) -> value.jsonArray.values() }
        ?.filterValues { it.isNotEmpty() }
        ?.takeIf { it.isNotEmpty() }
        ?: return null
    val layers = root["layers"]?.jsonArray?.map { it.jsonObject }.orEmpty()
    if (layers.isEmpty()) return null
    val rowCount = data.values.minOf { it.size }
    if (rowCount == 0 || rowCount > 10_000) return null
    if (data.values.any { it.size != rowCount }) return null
    if (layers.any { layer ->
            val x = layer.string("x") ?: "x"
            val y = layer.string("y") ?: "y"
            x !in data || y !in data
        }) return null
    PlotModel(
        width = root.number("width", 640f).coerceIn(280f, 1600f),
        height = root.number("height", 360f).coerceIn(220f, 900f),
        title = root.string("title"),
        xLabel = root.string("xLabel"),
        yLabel = root.string("yLabel"),
        data = data,
        layers = layers,
    )
}.getOrNull()

private data class NumericColumn(
    val values: List<Float>,
    val labels: List<String>?,
)

private fun PlotModel.column(name: String): NumericColumn? {
    val values = data[name] ?: return null
    val numbers = values.mapIndexed { index, value ->
        when (value) {
            is Number -> value.toFloat()
            else -> index.toFloat()
        }
    }
    val labels = if (values.all { it !is Number }) values.map { it.toString() } else null
    return NumericColumn(numbers, labels)
}

private val namedPlotColors = mapOf(
    "yellow" to "#ffd400",
    "red" to "#ff6666",
    "green" to "#5fb236",
    "blue" to "#2ea8e5",
    "purple" to "#a28ae5",
    "magenta" to "#e56eee",
    "pink" to "#e56eee",
    "orange" to "#f19837",
    "gray" to "#aaaaaa",
    "grey" to "#aaaaaa",
)

private fun parsePlotColor(value: String): Color? {
    val normalized = namedPlotColors[value.trim().lowercase()] ?: value.trim()
    if (!normalized.matches(Regex("#[0-9a-fA-F]{6}"))) return null
    val rgb = normalized.substring(1).toLongOrNull(16) ?: return null
    return Color(
        ((rgb shr 16) and 0xff).toFloat() / 255f,
        ((rgb shr 8) and 0xff).toFloat() / 255f,
        (rgb and 0xff).toFloat() / 255f,
    )
}

private fun colorFor(value: String?, palette: List<Color>, fallback: Color, layerIndex: Int): Color =
    value?.let(::parsePlotColor) ?: palette.getOrNull(layerIndex % palette.size) ?: fallback

@Composable
private fun PlotCanvas(model: PlotModel) {
    val textMeasurer = rememberTextMeasurer()
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val outline = MaterialTheme.colorScheme.outlineVariant
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = onSurfaceVariant)
    val titleStyle = MaterialTheme.typography.titleSmall.copy(color = onSurface)
    val palette = listOf(primary, secondary, tertiary, MaterialTheme.colorScheme.error)
    val layers = model.layers.mapNotNull { layer ->
        val type = layer.string("type")?.lowercase() ?: return@mapNotNull null
        val x = model.column(layer.string("x") ?: "x") ?: return@mapNotNull null
        val y = model.column(layer.string("y") ?: "y") ?: return@mapNotNull null
        Triple(layer, x, y).takeIf { type in setOf("line", "curve", "point", "scatter", "bar") }
    }

    Canvas(Modifier.fillMaxWidth().height(model.height.dp)) {
        val left = 54.dp.toPx()
        val right = 14.dp.toPx()
        val top = if (model.title != null) 32.dp.toPx() else 12.dp.toPx()
        val bottom = 34.dp.toPx()
        val plot = Size(
            (size.width - left - right).coerceAtLeast(1f),
            (size.height - top - bottom).coerceAtLeast(1f),
        )
        val origin = Offset(left, top)
        val allX = layers.flatMap { it.second.values }
        val allY = layers.flatMap { it.third.values }
        if (allX.isEmpty() || allY.isEmpty()) return@Canvas

        fun range(values: List<Float>): Pair<Float, Float> {
            val low = values.minOrNull() ?: 0f
            val high = values.maxOrNull() ?: 1f
            if (low == high) {
                val padding = max(abs(low) * 0.1f, 1f)
                return low - padding to high + padding
            }
            val padding = (high - low) * 0.06f
            return low - padding to high + padding
        }

        val (minX, maxX) = range(allX)
        val (minY, maxY) = range(allY)
        fun xPosition(value: Float): Float =
            origin.x + ((value - minX) / (maxX - minX)).coerceIn(0f, 1f) * plot.width
        fun yPosition(value: Float): Float =
            origin.y + plot.height - ((value - minY) / (maxY - minY)).coerceIn(0f, 1f) * plot.height
        fun drawLabel(text: String, x: Float, y: Float, style: TextStyle) {
            drawText(textMeasurer, text, topLeft = Offset(x, y), style = style)
        }
        fun format(value: Float): String =
            if (value == value.toInt().toFloat()) value.toInt().toString()
            else (value * 100f).toInt().toFloat().div(100f).toString()

        for (step in 0..4) {
            val fraction = step / 4f
            val y = origin.y + plot.height * (1f - fraction)
            drawLine(outline.copy(alpha = 0.45f), Offset(origin.x, y), Offset(origin.x + plot.width, y))
            drawLabel(format(minY + (maxY - minY) * fraction), 4.dp.toPx(), y - 8.dp.toPx(), labelStyle)
        }
        drawLine(outline, origin, Offset(origin.x, origin.y + plot.height), strokeWidth = 1.5f)
        drawLine(outline, Offset(origin.x, origin.y + plot.height),
            Offset(origin.x + plot.width, origin.y + plot.height), strokeWidth = 1.5f)

        layers.forEachIndexed { layerIndex, (layer, xColumn, yColumn) ->
            val type = layer.string("type")?.lowercase() ?: return@forEachIndexed
            val stroke = (layer["size"]?.jsonPrimitive?.doubleOrNull?.toFloat() ?: 2f).coerceIn(1f, 6f)
            val color = colorFor(layer.string("color") ?: layer.string("fill"), palette, primary, layerIndex)
            val count = min(xColumn.values.size, yColumn.values.size)
            if (count == 0) return@forEachIndexed
            when (type) {
                "line", "curve" -> {
                    for (index in 1 until count) {
                        drawLine(
                            color,
                            Offset(xPosition(xColumn.values[index - 1]), yPosition(yColumn.values[index - 1])),
                            Offset(xPosition(xColumn.values[index]), yPosition(yColumn.values[index])),
                            strokeWidth = stroke,
                            cap = StrokeCap.Round,
                        )
                    }
                }
                "point", "scatter" -> {
                    repeat(count) { index ->
                        drawCircle(color, radius = max(3f, stroke * 1.5f),
                            center = Offset(xPosition(xColumn.values[index]), yPosition(yColumn.values[index])))
                    }
                }
                "bar" -> {
                    val barWidth = (plot.width / count.coerceAtLeast(1) * 0.7f).coerceAtLeast(2f)
                    repeat(count) { index ->
                        val x = xPosition(xColumn.values[index])
                        val y = yPosition(yColumn.values[index])
                        val zero = yPosition(0f.coerceIn(minY, maxY))
                        drawRect(color.copy(alpha = 0.72f),
                            topLeft = Offset(x - barWidth / 2f, min(y, zero)),
                            size = Size(barWidth, abs(zero - y)))
                    }
                }
            }
            if (xColumn.labels != null && count <= 8) {
                xColumn.labels.take(count).forEachIndexed { index, label ->
                    val measured = textMeasurer.measure(label, labelStyle)
                    drawLabel(label, xPosition(xColumn.values[index]) - measured.size.width / 2f,
                        origin.y + plot.height + 5.dp.toPx(), labelStyle)
                }
            }
        }
        if (model.title != null) {
            drawLabel(model.title, origin.x, 6.dp.toPx(), titleStyle)
        }
        model.xLabel?.let { drawLabel(it, origin.x + plot.width / 2f, size.height - 16.dp.toPx(), labelStyle) }
        model.yLabel?.let { drawLabel(it, 4.dp.toPx(), 6.dp.toPx(), labelStyle) }
    }
}

@Composable
internal fun PlotBlock(code: String, fallbackStyle: TextStyle) {
    val plotCode = code.trim()
    val model = remember(plotCode) { parsePlot(plotCode) }
    val background = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f)
    Box(
        Modifier.fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .padding(10.dp),
    ) {
        if (model == null) {
            Text(plotCode, style = fallbackStyle.copy(fontFamily = FontFamily.Monospace))
        } else {
            PlotCanvas(model)
        }
    }
}
