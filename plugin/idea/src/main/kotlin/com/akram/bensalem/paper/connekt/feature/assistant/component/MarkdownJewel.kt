package com.akram.bensalem.paper.connekt.feature.assistant.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.intellij.util.ui.UIUtil
import com.mikepenz.markdown.compose.Markdown
import com.mikepenz.markdown.compose.components.MarkdownComponents
import com.mikepenz.markdown.model.*
import org.jetbrains.jewel.ui.component.Text

internal data class MarkdownColors(val onSurface: Color, val onSurfaceVariant: Color, val primary: Color) {
    val outlineVariant get() = onSurface.copy(alpha = .2f)
    val secondary get() = Color(0xFF59A869)
    val tertiary get() = Color(0xFFA28AE5)
    val error get() = Color(0xFFE5534B)
    val surfaceVariant get() = onSurface.copy(alpha = .08f)
}
internal data class MarkdownTypography(
    val bodyMedium: TextStyle = TextStyle(fontSize = 14.sp),
    val bodySmall: TextStyle = TextStyle(fontSize = 12.sp),
    val titleMedium: TextStyle = TextStyle(fontSize = 17.sp),
    val titleSmall: TextStyle = TextStyle(fontSize = 15.sp),
    val labelSmall: TextStyle = TextStyle(fontSize = 11.sp),
)

internal object MarkdownHostTheme {
    val colorScheme: MarkdownColors
        @Composable get() {
            val foreground = Color(UIUtil.getLabelForeground().rgb)
            return MarkdownColors(foreground, foreground.copy(alpha = 0.75f), Color(0xFF589DF6))
        }
    val typography get() = MarkdownTypography()
}

@Composable
internal fun MarkdownHostText(text: String, style: TextStyle) = Text(text, style = style)

internal fun markdownHostColors(text: Color, codeBackground: Color, inlineCodeBackground: Color, tableBackground: Color) =
    DefaultMarkdownColors(text, codeBackground, inlineCodeBackground, text.copy(alpha = 0.2f), tableBackground)

internal fun markdownHostTypography(
    h1: TextStyle, h2: TextStyle, h3: TextStyle, h4: TextStyle, h5: TextStyle, h6: TextStyle,
    text: TextStyle, paragraph: TextStyle, ordered: TextStyle, bullet: TextStyle, list: TextStyle,
    code: TextStyle, inlineCode: TextStyle, quote: TextStyle, textLink: TextLinkStyles, table: TextStyle,
) = DefaultMarkdownTypography(
    h1 = h1, h2 = h2, h3 = h3, h4 = h4, h5 = h5, h6 = h6, text = text, paragraph = paragraph,
    ordered = ordered, bullet = bullet, list = list, code = code, inlineCode = inlineCode,
    quote = quote, textLink = textLink, table = table,
)

@Composable
internal fun MarkdownHost(
    markdownState: MarkdownState, modifier: Modifier,
    colors: com.mikepenz.markdown.model.MarkdownColors,
    typography: com.mikepenz.markdown.model.MarkdownTypography,
    padding: MarkdownPadding, dimens: MarkdownDimens, annotator: MarkdownAnnotator,
    inlineContent: MarkdownInlineContent, components: MarkdownComponents, imageTransformer: ImageTransformer,
) = Markdown(
    markdownState, modifier = modifier, colors = colors, typography = typography, padding = padding,
    dimens = dimens, annotator = annotator, inlineContent = inlineContent, components = components,
    imageTransformer = imageTransformer,
    loading = { Text("Rendering Markdown...") },
    error = { Text("Markdown could not be rendered.") },
)

internal fun renderLatex(latex: String, sizePx: Float, color: Color, display: Boolean): LatexImage? =
    renderJvmLatex(latex, sizePx, color, display)
