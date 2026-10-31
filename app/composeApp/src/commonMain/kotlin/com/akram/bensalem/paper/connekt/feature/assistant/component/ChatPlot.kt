package com.akram.bensalem.paper.connekt.feature.assistant.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.letsPlot.Stat
import org.jetbrains.letsPlot.compose.PlotPanel
import org.jetbrains.letsPlot.geom.geomBar
import org.jetbrains.letsPlot.geom.geomLine
import org.jetbrains.letsPlot.geom.geomPoint
import org.jetbrains.letsPlot.intern.Plot
import org.jetbrains.letsPlot.label.labs
import org.jetbrains.letsPlot.letsPlot

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

private fun createFigure(model: PlotModel): Plot {
    var figure: Plot = letsPlot(model.data)
    model.layers.forEach { layer ->
        val type = layer.string("type")?.lowercase() ?: return@forEach
        val x = layer.string("x") ?: "x"
        val y = layer.string("y") ?: "y"
        val color = layer.string("color")
        val fill = layer.string("fill")
        val size = layer["size"]?.jsonPrimitive?.doubleOrNull
        when (type) {
            "line", "curve" -> {
                figure += geomLine(color = color, size = size) {
                    this.x = x
                    this.y = y
                }
            }
            "point", "scatter" -> {
                figure += geomPoint(color = color, fill = fill, size = size) {
                    this.x = x
                    this.y = y
                }
            }
            "bar" -> {
                figure += geomBar(stat = Stat.identity, fill = fill, color = color) {
                    this.x = x
                    this.y = y
                }
            }
        }
    }
    return figure + labs(title = model.title, x = model.xLabel, y = model.yLabel)
}

@Composable
internal fun PlotBlock(code: String, fallbackStyle: TextStyle) {
    val model = remember(code) { parsePlot(code) }
    val background = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f)
    Box(
        Modifier.fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .padding(10.dp),
    ) {
        if (model == null) {
            Text(code.trim(), style = fallbackStyle.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace))
        } else {
            PlotPanel(
                figure = remember(model) { createFigure(model) },
                modifier = Modifier.fillMaxWidth().height(model.height.dp),
                computationMessagesHandler = {},
            )
        }
    }
}
