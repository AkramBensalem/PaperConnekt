package com.akram.bensalem.paper.connekt.feature.assistant.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.akram.bensalem.paper.connekt.feature.assistant.PdfGeometry
import com.akram.bensalem.paper.connekt.feature.assistant.PdfTextSelection

@Composable
internal fun PdfSelectionHandles(
    selection: PdfTextSelection,
    geometry: PdfGeometry,
    pageWidth: Dp,
    pageHeight: Dp,
    onSelect: (Offset, Offset) -> Unit,
    onDraggingChange: (Boolean) -> Unit,
) {
    val first = selection.rects.firstOrNull() ?: return
    val last = selection.rects.last()
    val density = LocalDensity.current
    val currentSelection by rememberUpdatedState(selection)
    val select by rememberUpdatedState(onSelect)
    val draggingChange by rememberUpdatedState(onDraggingChange)
    val scale by rememberUpdatedState(with(density) {
        Offset(geometry.displayWidth / pageWidth.toPx(), geometry.displayHeight / pageHeight.toPx())
    })
    val color = MaterialTheme.colorScheme.primary
    for (start in listOf(true, false)) {
        val rect = if (start) first else last
        val x = if (start) rect.left else rect.right
        val stemHeight = (pageHeight * (rect.height / geometry.displayHeight)).coerceAtMost(24.dp)
        Box(
            Modifier
                .offset(
                    x = pageWidth * (x / geometry.displayWidth) - 24.dp,
                    y = pageHeight * (rect.bottom / geometry.displayHeight) - 16.dp,
                )
                .size(48.dp)
                .zIndex(3f)
                .testTag(if (start) "pdf-selection-start-handle" else "pdf-selection-end-handle")
                .semantics { contentDescription = if (start) "Drag selection start" else "Drag selection end" }
                .pointerInput(start) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        down.consume()
                        val rects = currentSelection.rects
                        val startRect = rects.first()
                        val endRect = rects.last()
                        // Anchor inside the endpoint glyphs, not below their baseline near the knobs.
                        val firstPoint = Offset(startRect.left + 0.01f, startRect.center.y)
                        val lastPoint = Offset(endRect.right - 0.01f, endRect.center.y)
                        val fixed = if (start) lastPoint else firstPoint
                        var moving = if (start) firstPoint else lastPoint
                        draggingChange(true)
                        try {
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (change.isConsumed || event.changes.count { it.pressed } > 1) break
                                val delta = change.position - change.previousPosition
                                change.consume()
                                moving += Offset(delta.x * scale.x, delta.y * scale.y)
                                if (delta != Offset.Zero) select(fixed, moving)
                                if (!change.pressed) break
                            }
                        } finally {
                            draggingChange(false)
                        }
                    }
                },
        ) {
            Canvas(Modifier.size(48.dp)) {
                val baseline = Offset(24.dp.toPx(), 16.dp.toPx())
                drawLine(color, baseline - Offset(0f, stemHeight.toPx()), baseline, strokeWidth = 2.dp.toPx())
                drawCircle(color, radius = 7.dp.toPx(), center = baseline + Offset(0f, 7.dp.toPx()))
            }
        }
    }
}
