package com.akram.bensalem.paper.connekt.feature.assistant.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import com.akram.bensalem.paper.connekt.feature.assistant.component.MarkdownHostTheme as MaterialTheme
import com.akram.bensalem.paper.connekt.feature.assistant.component.MarkdownHostText as Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikepenz.markdown.compose.elements.MarkdownCodeFence
import com.mikepenz.markdown.compose.components.markdownComponents
import com.akram.bensalem.paper.connekt.feature.assistant.component.MarkdownHost as Markdown
import com.akram.bensalem.paper.connekt.feature.assistant.component.markdownHostColors as markdownColor
import com.akram.bensalem.paper.connekt.feature.assistant.component.markdownHostTypography as markdownTypography
import com.mikepenz.markdown.coil3.Coil3ImageTransformerImpl
import com.mikepenz.markdown.model.ImageTransformer
import com.mikepenz.markdown.model.markdownAnnotator
import com.mikepenz.markdown.model.markdownDimens
import com.mikepenz.markdown.model.markdownInlineContent
import com.mikepenz.markdown.model.markdownPadding
import com.mikepenz.markdown.model.rememberMarkdownState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.getTextInNode

/** A formula found in a chat message. Display formulas inside a sentence are rendered inline in display style. */
internal data class ChatMath(val latex: String, val display: Boolean)

/** Chat Markdown whose math has been replaced: block math by ```math fences, inline math by placeholder code spans. */
internal class PreparedChatMarkdown(val markdown: String, val formulas: List<ChatMath>)

internal const val MATH_FENCE_LANGUAGE = "math"
private const val MATH_OPEN = '\uE000'
private const val MATH_CLOSE = '\uE001'
private val fenceStart = Regex("^(```+|~~~+)")

internal fun mathPlaceholderId(codeSpan: String): Int? {
    val raw = codeSpan.trim('`', ' ')
    if (raw.length < 3 || raw.first() != MATH_OPEN || raw.last() != MATH_CLOSE) return null
    return raw.substring(1, raw.length - 1).toIntOrNull()
}

/** Turns LaTeX delimiters (`$…$`, `\(…\)`, `$$…$$`, `\[…\]`) into Markdown the renderer can display, leaving code untouched. */
internal fun prepareChatMarkdown(text: String): PreparedChatMarkdown {
    val formulas = mutableListOf<ChatMath>()
    val out = StringBuilder()
    val lines = text.replace("\r\n", "\n").split('\n')
    var fence: String? = null
    var i = 0
    while (i < lines.size) {
        val line = lines[i]
        val trimmed = line.trimStart()
        val indent = line.substring(0, line.length - trimmed.length)
        if (fence != null) {
            out.append(line).append('\n')
            if (trimmed.startsWith(fence)) fence = null
            i++
            continue
        }
        val fenceMatch = fenceStart.find(trimmed)
        if (fenceMatch != null) {
            fence = fenceMatch.value
            out.append(line).append('\n')
            i++
            continue
        }
        val opener = when {
            trimmed.startsWith("$$") -> "$$"
            trimmed.startsWith("\\[") -> "\\["
            else -> null
        }
        if (opener != null) {
            val closer = if (opener == "$$") "$$" else "\\]"
            val body = StringBuilder(trimmed.substring(opener.length))
            var last = i
            var close = body.indexOf(closer)
            while (close < 0 && last + 1 < lines.size) {
                last++
                body.append('\n').append(lines[last])
                close = body.indexOf(closer)
            }
            val latex = if (close >= 0) body.substring(0, close).trim() else ""
            if (latex.isNotEmpty() && body.substring(close + closer.length).isBlank()) {
                out.append(indent).append("```").append(MATH_FENCE_LANGUAGE).append('\n')
                latex.lines().forEach { out.append(indent).append(it.trim()).append('\n') }
                out.append(indent).append("```\n")
                i = last + 1
                continue
            }
        }
        out.append(replaceInlineMath(line, formulas)).append('\n')
        i++
    }
    return PreparedChatMarkdown(out.toString().removeSuffix("\n"), formulas)
}

private fun replaceInlineMath(line: String, formulas: MutableList<ChatMath>): String {
    val out = StringBuilder()
    fun formula(latex: String, display: Boolean) {
        out.append('`').append(MATH_OPEN).append(formulas.size).append(MATH_CLOSE).append('`')
        formulas += ChatMath(latex.trim(), display)
    }
    var k = 0
    while (k < line.length) {
        val c = line[k]
        if (c == '\\' && k + 1 < line.length) {
            val next = line[k + 1]
            if (next == '(' || next == '[') {
                val end = line.indexOf(if (next == '(') "\\)" else "\\]", k + 2)
                if (end > k + 2) {
                    formula(line.substring(k + 2, end), display = next == '[')
                    k = end + 2
                    continue
                }
            }
            out.append(c).append(next)
            k += 2
            continue
        }
        if (c == '`') {
            var run = 1
            while (k + run < line.length && line[k + run] == '`') run++
            val ticks = "`".repeat(run)
            val end = line.indexOf(ticks, k + run)
            if (end >= 0) {
                out.append(line, k, end + run)
                k = end + run
            } else {
                out.append(ticks)
                k += run
            }
            continue
        }
        if (c == '$') {
            if (line.startsWith("$$", k)) {
                val end = line.indexOf("$$", k + 2)
                if (end > k + 2) {
                    formula(line.substring(k + 2, end), display = true)
                    k = end + 2
                    continue
                }
            } else if (k + 1 < line.length && !line[k + 1].isWhitespace()) {
                val end = closingDollar(line, k + 1)
                if (end > 0) {
                    formula(line.substring(k + 1, end), display = false)
                    k = end + 1
                    continue
                }
            }
        }
        out.append(c)
        k++
    }
    return out.toString()
}

/** Pandoc rules: the closing `$` follows a non-space and is not followed by a digit, so prices like "$5 and $10" stay text. */
private fun closingDollar(line: String, from: Int): Int {
    var j = from
    while (j < line.length) {
        when {
            line[j] == '\\' -> j++
            line[j] == '$' && !line[j - 1].isWhitespace() && (j + 1 >= line.length || !line[j + 1].isDigit()) -> return j
        }
        j++
    }
    return -1
}

private data class LatexKey(val latex: String, val sizePx: Float, val color: Color, val display: Boolean)

private val latexCache = LruCache<LatexKey, LatexImage?>(maxSize = 300)

/** Least-recently-used map; [LinkedHashMap] keeps insertion order, so reads re-insert the entry. */
private class LruCache<K, V>(private val maxSize: Int) {
    private val entries = LinkedHashMap<K, V>()

    operator fun contains(key: K): Boolean = key in entries

    operator fun get(key: K): V? {
        if (key !in entries) return null
        @Suppress("UNCHECKED_CAST")
        val value = entries.remove(key) as V
        entries[key] = value
        return value
    }

    operator fun set(key: K, value: V) {
        entries.remove(key)
        entries[key] = value
        if (entries.size > maxSize) entries.remove(entries.keys.first())
    }
}

/** Rendered formulas, or null while they are being rendered off the main thread for the first time. */
@Composable
private fun rememberLatex(keys: List<LatexKey>): List<LatexImage?>? {
    val cached = remember(keys) { if (keys.all { it in latexCache }) keys.map { latexCache[it] } else null }
    if (cached != null) return cached
    val rendered by produceState<List<LatexImage?>?>(null, keys) {
        val missing = keys.filterNot { it in latexCache }.distinct()
        val images = withContext(Dispatchers.Default) { missing.map { renderLatex(it.latex, it.sizePx, it.color, it.display) } }
        missing.forEachIndexed { index, key -> latexCache[key] = images[index] }
        value = keys.map { latexCache[it] }
    }
    return rendered
}

@Composable
private fun chatTypography(base: TextStyle) = markdownTypography(
    h1 = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
    h2 = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
    h3 = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
    h4 = base.copy(fontWeight = FontWeight.Bold),
    h5 = base.copy(fontWeight = FontWeight.SemiBold),
    h6 = base.copy(fontWeight = FontWeight.SemiBold),
    text = base,
    paragraph = base,
    ordered = base,
    bullet = base,
    list = base,
    code = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
    inlineCode = base.copy(fontFamily = FontFamily.Monospace, fontSize = base.fontSize * 0.92f),
    quote = base.copy(fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.onSurfaceVariant),
    textLink = TextLinkStyles(SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)),
    table = MaterialTheme.typography.bodySmall,
)

/** Compact, chat-friendly Markdown with LaTeX math, used for research agent replies. */
@Composable
internal fun ChatMarkdown(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    imageTransformer: ImageTransformer = Coil3ImageTransformerImpl,
) {
    val prepared = remember(text) { prepareChatMarkdown(text) }
    val base = MaterialTheme.typography.bodyMedium.copy(lineHeight = 21.sp, color = color)
    val density = LocalDensity.current
    val fontPx = with(density) { base.fontSize.toPx() }
    val keys = remember(prepared, fontPx, color) {
        prepared.formulas.map { LatexKey(it.latex, fontPx * if (it.display) 1.15f else 1.05f, color, it.display) }
    }
    val images = rememberLatex(keys)
    val codeColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
    val inlineContent = remember(images, density) {
        images.orEmpty().mapIndexedNotNull { id, image -> image?.let { "math-$id" to mathInline(it, prepared.formulas[id].latex, density) } }.toMap()
    }
    val fallbackStyle = SpanStyle(fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic)
    val annotator = remember(prepared, images) {
        markdownAnnotator { content, child ->
            if (child.type != MarkdownElementTypes.CODE_SPAN) return@markdownAnnotator false
            val id = mathPlaceholderId(child.getTextInNode(content).toString()) ?: return@markdownAnnotator false
            val latex = prepared.formulas.getOrNull(id)?.latex ?: return@markdownAnnotator false
            if (images?.getOrNull(id) != null) appendInlineContent("math-$id", latex)
            else withStyle(fallbackStyle) { append(latex) }
            true
        }
    }
    Markdown(
        rememberMarkdownState(prepared.markdown, retainState = true),
        modifier = modifier,
        colors = markdownColor(text = color, codeBackground = codeColor, inlineCodeBackground = codeColor,
            tableBackground = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f)),
        typography = chatTypography(base),
        imageTransformer = imageTransformer,
        padding = markdownPadding(block = 4.dp, list = 2.dp, listItemTop = 2.dp, listItemBottom = 2.dp, listIndent = 6.dp,
            codeBlock = PaddingValues(10.dp), blockQuote = PaddingValues(horizontal = 10.dp)),
        dimens = markdownDimens(codeBackgroundCornerSize = 8.dp, blockQuoteThickness = 3.dp, tableCellWidth = 120.dp,
            tableCellPadding = 8.dp, tableCornerSize = 8.dp),
        annotator = annotator,
        inlineContent = markdownInlineContent(inlineContent),
        components = markdownComponents(codeFence = { model ->
            val language = model.node.children.firstOrNull { it.type == MarkdownTokenTypes.FENCE_LANG }
                ?.getTextInNode(model.content)?.toString()?.trim()
            if (language == MATH_FENCE_LANGUAGE) {
                MarkdownCodeFence(model.content, model.node, model.typography.code) { code, _, _ -> MathBlock(code, base, color) }
            } else MarkdownCodeFence(model.content, model.node, model.typography.code)
        }),
    )
}

private fun mathInline(image: LatexImage, latex: String, density: Density): InlineTextContent = with(density) {
    InlineTextContent(Placeholder(image.image.width.toSp(), image.image.height.toSp(), PlaceholderVerticalAlign.TextCenter)) {
        Image(BitmapPainter(image.image), latex, Modifier.fillMaxSize())
    }
}

@Composable
private fun MathBlock(latex: String, base: TextStyle, color: Color) {
    val fontPx = with(LocalDensity.current) { base.fontSize.toPx() }
    val image = rememberLatex(listOf(LatexKey(latex.trim(), fontPx * 1.15f, color, true)))?.single()
    Box(Modifier.fillMaxWidth().padding(vertical = 4.dp).horizontalScroll(rememberScrollState()).testTag("chat-math-block"),
        contentAlignment = Alignment.Center) {
        if (image != null) {
            val size = with(LocalDensity.current) { DpSize(image.image.width.toDp(), image.image.height.toDp()) }
            Image(BitmapPainter(image.image), latex, Modifier.size(size))
        } else Text(latex, style = base.copy(fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic))
    }
}
