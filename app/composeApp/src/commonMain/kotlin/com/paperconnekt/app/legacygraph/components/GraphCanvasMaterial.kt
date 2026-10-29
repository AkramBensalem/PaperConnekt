package com.paperconnekt.app.legacygraph.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal object GraphCanvasTheme {
    val colorScheme @Composable get() = MaterialTheme.colorScheme
    val typography @Composable get() = MaterialTheme.typography
    val shapes @Composable get() = MaterialTheme.shapes
}

@Composable
internal fun GraphCanvasSurface(
    modifier: Modifier = Modifier,
    shape: Shape,
    color: Color,
    tonalElevation: Dp = 0.dp,
    border: BorderStroke? = null,
    content: @Composable () -> Unit,
) {
    Surface(modifier = modifier, shape = shape, color = color, tonalElevation = tonalElevation, border = border) {
        content()
    }
}

@Composable
internal fun GraphCanvasText(text: String, style: TextStyle) {
    Text(text, style = style)
}
