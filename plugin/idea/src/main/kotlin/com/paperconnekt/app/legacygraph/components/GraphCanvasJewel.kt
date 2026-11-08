package com.paperconnekt.app.legacygraph.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.jewel.ui.component.Text
import com.intellij.util.ui.UIUtil
import com.paperconnekt.idea.NativeIcon
import com.paperconnekt.idea.NativeToolButton
import kotlin.math.roundToInt

internal data class CanvasColors(val onSurface: Color, val outlineVariant: Color, val surface: Color)
internal data class CanvasTypography(val labelSmall: TextStyle = TextStyle(fontSize = 11.sp))
internal data class CanvasShapes(val extraSmall: Shape = RoundedCornerShape(4.dp))

internal object GraphCanvasTheme {
    val colorScheme: CanvasColors
        @Composable get() {
            val foreground = Color(UIUtil.getLabelForeground().rgb)
            return CanvasColors(foreground, foreground.copy(alpha = 0.25f), Color(UIUtil.getPanelBackground().rgb))
        }
    val typography get() = CanvasTypography()
    val shapes get() = CanvasShapes()
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
    Box(modifier.clip(shape).background(color).then(if (border != null) Modifier.border(border, shape) else Modifier)) {
        content()
    }
}

@Composable
internal fun GraphCanvasText(text: String, style: TextStyle) {
    Text(text, style = style)
}

@Composable
// Compose handles zoom gestures in GraphCanvas; never install the upstream
// global Linux input-device listener inside the IDE.
internal fun NativeTrackpadPinchZoom(onZoom: (Float) -> Unit) = Unit

@Composable
private fun CanvasControlSurface(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    val colors = GraphCanvasTheme.colorScheme
    Box(
        modifier.clip(shape).background(colors.surface.copy(alpha = .94f))
            .border(1.dp, colors.outlineVariant, shape).padding(2.dp),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
internal fun ZoomControls(
    zoomScale: Float,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    CanvasControlSurface(modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            NativeToolButton("Zoom in", NativeIcon.Add, onClick = onZoomIn)
            Text(
                "${(zoomScale * 100).roundToInt()}%",
                Modifier.widthIn(min = 40.dp).padding(vertical = 2.dp),
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
            )
            NativeToolButton("Zoom out", NativeIcon.Minus, onClick = onZoomOut)
        }
    }
}

@Composable
internal fun PanControls(
    onPanLeft: () -> Unit,
    onPanRight: () -> Unit,
    onPanUp: () -> Unit,
    onPanDown: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    CanvasControlSurface(modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            NativeToolButton("Move graph up", NativeIcon.ArrowUp, onClick = onPanUp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                NativeToolButton("Move graph left", NativeIcon.ArrowLeft, onClick = onPanLeft)
                Spacer(Modifier.size(32.dp))
                NativeToolButton("Move graph right", NativeIcon.ArrowRight, onClick = onPanRight)
            }
            NativeToolButton("Move graph down", NativeIcon.ArrowDown, onClick = onPanDown)
        }
    }
}
