package com.akram.bensalem.paper.connekt.feature.assistant.component

import androidx.compose.ui.graphics.Color

/** Renders a formula at [sizePx], or returns null when the formula cannot be parsed. */
internal expect fun renderLatex(latex: String, sizePx: Float, color: Color, display: Boolean): LatexImage?
