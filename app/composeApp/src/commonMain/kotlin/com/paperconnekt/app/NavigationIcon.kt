package com.paperconnekt.app

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

enum class NavigationGlyph {
    DASHBOARD,
    ASSISTANT,
    GRAPH,
    LIBRARY,
    SETTINGS,
}

@Composable
fun NavigationIcon(
    glyph: NavigationGlyph,
    contentDescription: String,
) {
    Canvas(
        modifier = Modifier
            .alpha(0.92f)
            .semantics {
                this.contentDescription = contentDescription
            }
            .size(24.dp),
    ) {
        val stroke = Stroke(width = 1.8.dp.toPx())
        val iconColor = MaterialTheme.colorScheme.onSurfaceVariant
        when (glyph) {
            NavigationGlyph.DASHBOARD -> {
                drawRoundRect(iconColor, style = stroke)
                drawLine(iconColor, Offset(center.x, 0f), Offset(center.x, size.height))
                drawLine(iconColor, Offset(0f, center.y), Offset(size.width, center.y))
            }
            NavigationGlyph.ASSISTANT -> {
                drawCircle(iconColor, radius = 4.dp.toPx(), style = stroke)
                drawLine(
                    iconColor,
                    Offset(center.x, 1.dp.toPx()),
                    Offset(center.x, size.height - 1.dp.toPx()),
                    strokeWidth = 1.6.dp.toPx(),
                )
                drawLine(
                    iconColor,
                    Offset(1.dp.toPx(), center.y),
                    Offset(size.width - 1.dp.toPx(), center.y),
                    strokeWidth = 1.6.dp.toPx(),
                )
            }
            NavigationGlyph.GRAPH -> {
                drawLine(
                    iconColor,
                    Offset(3.dp.toPx(), 17.dp.toPx()),
                    Offset(10.dp.toPx(), 7.dp.toPx()),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                )
                drawLine(
                    iconColor,
                    Offset(10.dp.toPx(), 7.dp.toPx()),
                    Offset(20.dp.toPx(), 14.dp.toPx()),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                )
                drawCircle(iconColor, center = Offset(3.dp.toPx(), 17.dp.toPx()), radius = 2.5.dp.toPx())
                drawCircle(iconColor, center = Offset(10.dp.toPx(), 7.dp.toPx()), radius = 2.5.dp.toPx())
                drawCircle(iconColor, center = Offset(20.dp.toPx(), 14.dp.toPx()), radius = 2.5.dp.toPx())
            }
            NavigationGlyph.LIBRARY -> {
                drawRoundRect(
                    iconColor,
                    topLeft = Offset(2.dp.toPx(), 3.dp.toPx()),
                    size = Size(20.dp.toPx(), 18.dp.toPx()),
                    style = stroke,
                )
                drawLine(iconColor, Offset(7.dp.toPx(), 3.dp.toPx()), Offset(7.dp.toPx(), 21.dp.toPx()), strokeWidth = 1.6.dp.toPx())
                drawLine(iconColor, Offset(11.dp.toPx(), 8.dp.toPx()), Offset(18.dp.toPx(), 8.dp.toPx()), strokeWidth = 1.6.dp.toPx())
                drawLine(iconColor, Offset(11.dp.toPx(), 12.dp.toPx()), Offset(18.dp.toPx(), 12.dp.toPx()), strokeWidth = 1.6.dp.toPx())
                drawLine(iconColor, Offset(11.dp.toPx(), 16.dp.toPx()), Offset(16.dp.toPx(), 16.dp.toPx()), strokeWidth = 1.6.dp.toPx())
            }
            NavigationGlyph.SETTINGS -> {
                drawCircle(iconColor, radius = 7.dp.toPx(), style = stroke)
                drawCircle(iconColor, radius = 2.5.dp.toPx())
                listOf(0f, 45f, 90f, 135f).forEach { angle ->
                    val radians = Math.toRadians(angle.toDouble())
                    val dx = kotlin.math.cos(radians).toFloat()
                    val dy = kotlin.math.sin(radians).toFloat()
                    drawLine(
                        iconColor,
                        start = center,
                        end = center + androidx.compose.ui.geometry.Offset(dx * 10.dp.toPx(), dy * 10.dp.toPx()),
                        strokeWidth = 1.6.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                    drawLine(
                        iconColor,
                        start = center,
                        end = center + androidx.compose.ui.geometry.Offset(-dx * 10.dp.toPx(), -dy * 10.dp.toPx()),
                        strokeWidth = 1.6.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
            }
        }
    )
}
