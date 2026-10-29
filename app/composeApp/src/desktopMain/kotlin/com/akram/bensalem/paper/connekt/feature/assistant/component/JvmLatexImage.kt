package com.akram.bensalem.paper.connekt.feature.assistant.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.scilab.forge.jlatexmath.TeXConstants
import org.scilab.forge.jlatexmath.TeXFormula
import java.awt.Insets
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import javax.swing.JLabel

/** JLaTeXMath keeps global parser and font tables that are not safe to initialize or use from several threads at once. */
private val latexLock = Any()

internal fun renderJvmLatex(latex: String, sizePx: Float, color: Color, display: Boolean): LatexImage? =
    synchronized(latexLock) { renderLatexUnsafe(latex, sizePx, color, display) }

private fun renderLatexUnsafe(latex: String, sizePx: Float, color: Color, display: Boolean): LatexImage? = runCatching {
    val icon = TeXFormula(latex).createTeXIcon(if (display) TeXConstants.STYLE_DISPLAY else TeXConstants.STYLE_TEXT, sizePx)
    val pad = (sizePx / 10f).toInt().coerceAtLeast(1)
    icon.insets = Insets(pad, pad, pad, pad)
    val width = icon.iconWidth.coerceAtLeast(1)
    val height = icon.iconHeight.coerceAtLeast(1)
    val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    val graphics = image.createGraphics()
    try {
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON)
        icon.paintIcon(JLabel().apply { foreground = java.awt.Color(color.toArgb(), true) }, graphics, 0, 0)
    } finally {
        graphics.dispose()
    }
    LatexImage(image.toComposeImageBitmap(), (height - pad - icon.iconDepth).coerceIn(0, height))
}.getOrNull()
