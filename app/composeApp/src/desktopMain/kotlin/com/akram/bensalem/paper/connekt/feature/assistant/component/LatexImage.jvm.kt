package com.akram.bensalem.paper.connekt.feature.assistant.component

import androidx.compose.ui.graphics.Color

internal actual fun renderLatex(latex: String, sizePx: Float, color: Color, display: Boolean): LatexImage? =
    renderJvmLatex(latex, sizePx, color, display)
