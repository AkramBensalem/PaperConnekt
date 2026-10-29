package com.akram.bensalem.paper.connekt.feature.assistant.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import com.mikepenz.markdown.compose.components.MarkdownComponents
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.model.*

internal object MarkdownHostTheme {
    val colorScheme @Composable get() = MaterialTheme.colorScheme
    val typography @Composable get() = MaterialTheme.typography
}

@Composable
internal fun MarkdownHostText(text: String, style: TextStyle) = Text(text, style = style)

@Composable
internal fun markdownHostColors(text: Color, codeBackground: Color, inlineCodeBackground: Color, tableBackground: Color) =
    markdownColor(text = text, codeBackground = codeBackground, inlineCodeBackground = inlineCodeBackground, tableBackground = tableBackground)

@Composable
internal fun markdownHostTypography(
    h1: TextStyle, h2: TextStyle, h3: TextStyle, h4: TextStyle, h5: TextStyle, h6: TextStyle,
    text: TextStyle, paragraph: TextStyle, ordered: TextStyle, bullet: TextStyle, list: TextStyle,
    code: TextStyle, inlineCode: TextStyle, quote: TextStyle, textLink: TextLinkStyles, table: TextStyle,
) = markdownTypography(
    h1 = h1, h2 = h2, h3 = h3, h4 = h4, h5 = h5, h6 = h6, text = text, paragraph = paragraph,
    ordered = ordered, bullet = bullet, list = list, code = code, inlineCode = inlineCode,
    quote = quote, textLink = textLink, table = table,
)

@Composable
internal fun MarkdownHost(
    markdownState: MarkdownState, modifier: Modifier, colors: MarkdownColors, typography: MarkdownTypography,
    padding: MarkdownPadding, dimens: MarkdownDimens, annotator: MarkdownAnnotator,
    inlineContent: MarkdownInlineContent, components: MarkdownComponents, imageTransformer: ImageTransformer,
) = Markdown(
    markdownState, modifier = modifier, colors = colors, typography = typography, padding = padding,
    dimens = dimens, annotator = annotator, inlineContent = inlineContent, components = components,
    imageTransformer = imageTransformer,
)
