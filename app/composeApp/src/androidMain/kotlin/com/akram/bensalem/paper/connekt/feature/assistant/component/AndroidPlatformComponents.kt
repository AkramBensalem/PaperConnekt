package com.akram.bensalem.paper.connekt.feature.assistant.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
internal actual fun NativeReaderTouchpad(
    enabled: () -> Boolean,
    onZoom: (Float) -> Unit,
    onHorizontalPan: (Float) -> Unit,
) = Unit

internal actual fun renderLatex(
    latex: String,
    sizePx: Float,
    color: Color,
    display: Boolean,
): LatexImage? = null
