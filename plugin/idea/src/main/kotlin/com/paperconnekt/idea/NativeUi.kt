package com.paperconnekt.idea

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipPlacement
import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import com.intellij.util.ui.UIUtil
import androidx.compose.foundation.text.input.rememberTextFieldState
import org.jetbrains.jewel.ui.component.DefaultButton
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField

internal enum class NativeIcon {
    Home, Graph, Library, History, File, Assistant, Search, Folder, Add, More, Settings,
    Close, Back, Next, Up, Down, ZoomIn, ZoomOut, Filter, Refresh, Edit, Trash,
    Download, Upload, Check, Select, Pan, Pen, Note, Text, Area, Copy, Play, Pause, Stop,
    ArrowUp, ArrowDown, ArrowLeft, ArrowRight, Minus,
    Send, Chat, Highlight, Underline, Speaker, External, Chart, Layout, Logout, Locate, Pin, Book, Markdown, Context, Idea,
}

internal val nativeForeground: Color get() = Color(UIUtil.getLabelForeground().rgb)
internal val nativeBackground: Color get() = Color(UIUtil.getPanelBackground().rgb)
internal val nativeAccent = Color(0xFF3574F0)

/** Vector glyphs drawn on a 24x24 grid with rounded strokes, so they stay crisp at any size and follow the IDE theme. */
@Composable
internal fun NativeGlyph(icon: NativeIcon, modifier: Modifier = Modifier, tint: Color = nativeForeground) {
    val color = tint
    Canvas(modifier.size(18.dp)) {
        val scale = size.minDimension / 24f
        val width = 1.8f * scale
        val stroke = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round)
        fun line(x: Float, y: Float, x2: Float, y2: Float) =
            drawLine(color, Offset(x * scale, y * scale), Offset(x2 * scale, y2 * scale), width, cap = StrokeCap.Round)
        fun circle(x: Float, y: Float, r: Float) =
            drawCircle(color, r * scale, Offset(x * scale, y * scale), style = stroke)
        fun dot(x: Float, y: Float, r: Float) = drawCircle(color, r * scale, Offset(x * scale, y * scale))
        fun rect(x: Float, y: Float, w: Float, h: Float, r: Float = 2f) =
            drawRoundRect(color, Offset(x * scale, y * scale), Size(w * scale, h * scale), CornerRadius(r * scale), style = stroke)
        fun fillRect(x: Float, y: Float, w: Float, h: Float, r: Float = 1f) =
            drawRoundRect(color, Offset(x * scale, y * scale), Size(w * scale, h * scale), CornerRadius(r * scale))
        fun shape(points: Array<out Pair<Float, Float>>, closed: Boolean): Path {
            val result = Path()
            points.forEachIndexed { index, (x, y) -> if (index == 0) result.moveTo(x * scale, y * scale) else result.lineTo(x * scale, y * scale) }
            if (closed) result.close()
            return result
        }
        fun path(vararg points: Pair<Float, Float>, close: Boolean = false) = drawPath(shape(points, close), color, style = stroke)
        fun fill(vararg points: Pair<Float, Float>) = drawPath(shape(points, true), color)
        fun arc(x: Float, y: Float, r: Float, start: Float, sweep: Float) =
            drawArc(color, start, sweep, false, Offset((x - r) * scale, (y - r) * scale), Size(2 * r * scale, 2 * r * scale), style = stroke)
        fun sparkle(cx: Float, cy: Float, r: Float, filled: Boolean) {
            val k = r * .28f
            val points = arrayOf(cx to cy - r, cx + k to cy - k, cx + r to cy, cx + k to cy + k, cx to cy + r, cx - k to cy + k, cx - r to cy, cx - k to cy - k)
            if (filled) fill(*points) else path(*points, close = true)
        }
        when (icon) {
            NativeIcon.Home -> {
                path(4f to 11f, 12f to 4f, 20f to 11f, 20f to 20f, 15f to 20f, 15f to 14f, 9f to 14f, 9f to 20f, 4f to 20f, close = true)
            }
            NativeIcon.Graph -> {
                line(7f, 7f, 17f, 9f); line(7f, 7f, 9f, 17f); line(17f, 9f, 9f, 17f)
                dot(6f, 6f, 2.6f); dot(18f, 9f, 2.6f); dot(9f, 18f, 2.6f)
            }
            NativeIcon.Library -> { rect(3.5f, 4f, 4.5f, 16f, 1f); rect(10f, 4f, 4f, 16f, 1f); path(16.5f to 5f, 20.5f to 4f, 21f to 19f, 17f to 20f, close = true) }
            NativeIcon.History -> { arc(12f, 12f, 8.5f, 200f, 300f); path(3f to 6f, 4.2f to 10f, 8f to 9f); line(12f, 8f, 12f, 12f); line(12f, 12f, 15f, 14f) }
            NativeIcon.File -> { path(6f to 3f, 14f to 3f, 19f to 8f, 19f to 21f, 6f to 21f, close = true); path(14f to 3f, 14f to 8f, 19f to 8f); line(9f, 13f, 15f, 13f); line(9f, 17f, 15f, 17f) }
            NativeIcon.Book -> { path(12f to 6f, 12f to 20f); path(12f to 6f, 8f to 4.5f, 3f to 4.5f, 3f to 18.5f, 8f to 18.5f, 12f to 20f, 16f to 18.5f, 21f to 18.5f, 21f to 4.5f, 16f to 4.5f, close = true) }
            NativeIcon.Assistant -> { sparkle(10f, 13f, 8f, filled = false); sparkle(18.5f, 5.5f, 3.5f, filled = true) }
            NativeIcon.Search -> { circle(10.5f, 10.5f, 6.5f); line(15.5f, 15.5f, 20.5f, 20.5f) }
            NativeIcon.Folder -> path(3f to 6.5f, 3f to 19f, 21f to 19f, 21f to 8.5f, 11.5f to 8.5f, 9.5f to 5f, 3f to 5f, 3f to 6.5f)
            NativeIcon.Add -> { line(12f, 5f, 12f, 19f); line(5f, 12f, 19f, 12f) }
            NativeIcon.More -> { dot(5f, 12f, 1.7f); dot(12f, 12f, 1.7f); dot(19f, 12f, 1.7f) }
            NativeIcon.Settings -> {
                circle(12f, 12f, 3f)
                for (i in 0 until 8) {
                    val a = Math.toRadians(i * 45.0)
                    line(12f + 6f * kotlin.math.cos(a).toFloat(), 12f + 6f * kotlin.math.sin(a).toFloat(),
                        12f + 9f * kotlin.math.cos(a).toFloat(), 12f + 9f * kotlin.math.sin(a).toFloat())
                }
                circle(12f, 12f, 6.2f)
            }
            NativeIcon.Close -> { line(6f, 6f, 18f, 18f); line(18f, 6f, 6f, 18f) }
            NativeIcon.Back -> path(15f to 5f, 8f to 12f, 15f to 19f)
            NativeIcon.Next -> path(9f to 5f, 16f to 12f, 9f to 19f)
            NativeIcon.Up -> path(5f to 15f, 12f to 8f, 19f to 15f)
            NativeIcon.Down -> path(6f to 9f, 12f to 15f, 18f to 9f)
            NativeIcon.ZoomIn, NativeIcon.ZoomOut -> {
                circle(10.5f, 10.5f, 6.5f); line(15.5f, 15.5f, 20.5f, 20.5f); line(7.5f, 10.5f, 13.5f, 10.5f)
                if (icon == NativeIcon.ZoomIn) line(10.5f, 7.5f, 10.5f, 13.5f)
            }
            NativeIcon.Filter -> path(3f to 5f, 21f to 5f, 14f to 13f, 14f to 19f, 10f to 21f, 10f to 13f, close = true)
            NativeIcon.Refresh -> { arc(12f, 12f, 8f, -60f, 300f); path(16f to 3f, 20f to 5.2f, 17.5f to 9f) }
            NativeIcon.Edit, NativeIcon.Pen -> { path(4f to 20f, 5f to 15.5f, 15.5f to 5f, 19f to 8.5f, 8.5f to 19f, close = true); line(13f, 7.5f, 16.5f, 11f) }
            NativeIcon.Highlight -> { path(9f to 15f, 17f to 4f, 21f to 7.5f, 12f to 17f, close = true); path(9f to 15f, 6.5f to 18.5f, 9.5f to 20f, 12f to 17f); fillRect(3f, 20f, 18f, 2f) }
            NativeIcon.Underline -> { path(7f to 4f, 7f to 11f); arc(12f, 11f, 5f, 0f, 180f); path(17f to 4f, 17f to 11f); line(5f, 20f, 19f, 20f) }
            NativeIcon.Trash -> { path(5f to 7f, 6.5f to 20f, 17.5f to 20f, 19f to 7f); line(3.5f, 7f, 20.5f, 7f); path(9f to 7f, 9.5f to 4f, 14.5f to 4f, 15f to 7f); line(10f, 11f, 10f, 16f); line(14f, 11f, 14f, 16f) }
            NativeIcon.Download, NativeIcon.Upload -> {
                path(4f to 15f, 4f to 20f, 20f to 20f, 20f to 15f); line(12f, 4f, 12f, 15f)
                if (icon == NativeIcon.Download) path(7.5f to 10.5f, 12f to 15f, 16.5f to 10.5f)
                else path(7.5f to 8.5f, 12f to 4f, 16.5f to 8.5f)
            }
            NativeIcon.Check -> path(5f to 12.5f, 10f to 17.5f, 19f to 7f)
            NativeIcon.Select -> path(6f to 3.5f, 18.5f to 13f, 12.5f to 14f, 9.5f to 20f, close = true)
            NativeIcon.Pan -> { path(8f to 13f, 8f to 5.5f); path(11f to 12f, 11f to 4f); path(14f to 12f, 14f to 5f); path(17f to 13f, 17f to 8f); path(8f to 12f, 5.5f to 10.5f, 4.5f to 13f, 9f to 20f, 16f to 20f, 18f to 17f, 17f to 13f) }
            NativeIcon.Note -> { path(5f to 4f, 19f to 4f, 19f to 15f, 14f to 20f, 5f to 20f, close = true); path(14f to 20f, 14f to 15f, 19f to 15f); line(8.5f, 8.5f, 15.5f, 8.5f); line(8.5f, 12f, 13f, 12f) }
            NativeIcon.Text -> { line(5f, 5f, 19f, 5f); line(12f, 5f, 12f, 20f); line(9f, 20f, 15f, 20f) }
            NativeIcon.Area -> { path(4f to 8f, 4f to 4f, 8f to 4f); path(16f to 4f, 20f to 4f, 20f to 8f); path(20f to 16f, 20f to 20f, 16f to 20f); path(8f to 20f, 4f to 20f, 4f to 16f) }
            NativeIcon.Copy -> { rect(8.5f, 8.5f, 12f, 12f); path(15.5f to 8.5f, 15.5f to 3.5f, 3.5f to 3.5f, 3.5f to 15.5f, 8.5f to 15.5f) }
            NativeIcon.Play -> fill(7f to 4f, 20f to 12f, 7f to 20f)
            NativeIcon.Pause -> { fillRect(6f, 5f, 4f, 14f); fillRect(14f, 5f, 4f, 14f) }
            NativeIcon.Stop -> fillRect(6f, 6f, 12f, 12f, 2f)
            NativeIcon.Speaker -> { path(4f to 9f, 8f to 9f, 13f to 5f, 13f to 19f, 8f to 15f, 4f to 15f, close = true); arc(14f, 12f, 3.5f, -50f, 100f); arc(14f, 12f, 7f, -50f, 100f) }
            NativeIcon.ArrowUp -> { line(12f, 20f, 12f, 4f); path(6f to 10f, 12f to 4f, 18f to 10f) }
            NativeIcon.ArrowDown -> { line(12f, 4f, 12f, 20f); path(6f to 14f, 12f to 20f, 18f to 14f) }
            NativeIcon.ArrowLeft -> { line(20f, 12f, 4f, 12f); path(10f to 6f, 4f to 12f, 10f to 18f) }
            NativeIcon.ArrowRight -> { line(4f, 12f, 20f, 12f); path(14f to 6f, 20f to 12f, 14f to 18f) }
            NativeIcon.Minus -> line(5f, 12f, 19f, 12f)
            NativeIcon.Send -> { path(3f to 11f, 21f to 3f, 14f to 21f, 11f to 13f, close = true); line(11f, 13f, 21f, 3f) }
            NativeIcon.Chat -> { path(4f to 5f, 20f to 5f, 20f to 16f, 11f to 16f, 6f to 20f, 6f to 16f, 4f to 16f, close = true); dot(8.5f, 10.5f, 1.2f); dot(12f, 10.5f, 1.2f); dot(15.5f, 10.5f, 1.2f) }
            NativeIcon.External -> { path(13f to 4f, 20f to 4f, 20f to 11f); line(20f, 4f, 11f, 13f); path(17f to 14f, 17f to 20f, 4f to 20f, 4f to 7f, 10f to 7f) }
            NativeIcon.Chart -> { fillRect(4f, 13f, 4f, 7f); fillRect(10f, 8f, 4f, 12f); fillRect(16f, 4f, 4f, 16f) }
            NativeIcon.Layout -> { rect(4f, 3f, 16f, 18f); line(4f, 12f, 20f, 12f) }
            NativeIcon.Logout -> { path(10f to 4f, 4f to 4f, 4f to 20f, 10f to 20f); line(9f, 12f, 20f, 12f); path(16f to 8f, 20f to 12f, 16f to 16f) }
            NativeIcon.Locate -> { circle(12f, 12f, 7f); dot(12f, 12f, 2.2f); line(12f, 2f, 12f, 5f); line(12f, 19f, 12f, 22f); line(2f, 12f, 5f, 12f); line(19f, 12f, 22f, 12f) }
            NativeIcon.Pin -> { path(9f to 3f, 15f to 3f, 14f to 9f, 18f to 13f, 6f to 13f, 10f to 9f, close = true); line(12f, 13f, 12f, 21f) }
            NativeIcon.Markdown -> { rect(2.5f, 5f, 19f, 14f); path(6f to 15f, 6f to 9f, 8.5f to 12f, 11f to 9f, 11f to 15f); line(16f, 9f, 16f, 15f); path(14f to 13f, 16f to 15f, 18f to 13f) }
            NativeIcon.Context -> { rect(3f, 4f, 18f, 16f); line(7f, 9f, 17f, 9f); line(7f, 13f, 17f, 13f); line(7f, 17f, 12f, 17f) }
            NativeIcon.Idea -> {
                arc(12f, 9.5f, 6f, 140f, 260f)
                path(7.4f to 13.4f, 9.5f to 16f, 14.5f to 16f, 16.6f to 13.4f)
                line(9.5f, 18.5f, 14.5f, 18.5f); line(10.5f, 21f, 13.5f, 21f)
                line(12f, 16f, 12f, 11.5f)
            }
        }
    }
}

/** Places tooltips above the component (or below when there is no room), never under the pointer. */
@OptIn(ExperimentalFoundationApi::class)
private object NativeTooltipPlacement : TooltipPlacement {
    @Composable
    override fun positionProvider(cursorPosition: Offset): PopupPositionProvider {
        val gap = with(LocalDensity.current) { 6.dp.roundToPx() }
        return remember(gap) {
            object : PopupPositionProvider {
                override fun calculatePosition(
                    anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize,
                ): IntOffset {
                    val x = (anchorBounds.left + (anchorBounds.width - popupContentSize.width) / 2)
                        .coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0))
                    val above = anchorBounds.top - gap - popupContentSize.height
                    val y = if (above >= 0) above else anchorBounds.bottom + gap
                    return IntOffset(x, y)
                }
            }
        }
    }
}

/** Hover tooltip shared by icon-only controls. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun NativeTooltip(label: String, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(5.dp)
    TooltipArea(tooltip = {
        Box(Modifier.shadow(6.dp, shape).background(nativeBackground, shape).border(1.dp, nativeForeground.copy(alpha = .25f), shape)
            .padding(horizontal = 8.dp, vertical = 5.dp)) {
            Text(label, fontSize = 12.sp)
        }
    }, delayMillis = 600, tooltipPlacement = NativeTooltipPlacement, content = content)
}

@Composable
internal fun NativeToolButton(
    label: String,
    icon: NativeIcon,
    enabled: Boolean = true,
    selected: Boolean? = null,
    showLabel: Boolean = false,
    swatch: Color? = null,
    compact: Boolean = false,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(5.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val button: @Composable () -> Unit = {
        val interaction = if (selected != null) Modifier.selectable(selected, enabled = enabled, role = Role.Tab, onClick = onClick)
        else Modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick)
        Row(
            Modifier.clip(shape)
                .alpha(if (enabled) 1f else .45f)
                .background(when {
                    selected == true -> nativeAccent.copy(alpha = .25f)
                    hovered && enabled -> nativeForeground.copy(alpha = .08f)
                    else -> Color.Transparent
                })
                .hoverable(interactionSource, enabled)
                .then(interaction)
                .semantics { contentDescription = label }
                .defaultMinSize(minWidth = if (compact) 22.dp else 32.dp, minHeight = if (compact) 22.dp else 32.dp)
                .padding(horizontal = if (compact) 4.dp else if (showLabel) 8.dp else 7.dp, vertical = if (compact) 4.dp else 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        ) {
            if (swatch != null) NativeColorSwatch(swatch)
            else Box(Modifier.size(if (compact) 12.dp else 18.dp), contentAlignment = Alignment.Center) {
                NativeGlyph(icon, Modifier.size(if (compact) 12.dp else 18.dp))
            }
            if (showLabel) Text(label, Modifier.widthIn(max = 180.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
    if (showLabel) button() else NativeTooltip(label, button)
}

@Composable
internal fun NativeColorSwatch(color: Color) {
    Box(Modifier.size(18.dp).background(color, RoundedCornerShape(4.dp))
        .border(1.dp, nativeForeground.copy(alpha = .35f), RoundedCornerShape(4.dp)))
}

internal data class NativeMenuAction(
    val label: String,
    val icon: NativeIcon = NativeIcon.Next,
    val enabled: Boolean = true,
    val selected: Boolean = false,
    val swatch: Color? = null,
    val action: () -> Unit,
)

@Composable
internal fun NativeMenu(
    label: String,
    actions: List<NativeMenuAction>,
    icon: NativeIcon = NativeIcon.More,
    showLabel: Boolean = false,
    swatch: Color? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    val offset = with(LocalDensity.current) { 34.dp.roundToPx() }
    Box {
        NativeToolButton(label, icon, showLabel = showLabel, swatch = swatch) { expanded = !expanded }
        if (expanded) NativeMenuPopup(actions, onDismiss = { expanded = false },
            alignment = Alignment.TopEnd, offset = IntOffset(0, offset))
    }
}

/** Menu surface shared by toolbar dropdowns and pointer-anchored context menus. */
@Composable
internal fun NativeMenuPopup(
    actions: List<NativeMenuAction>,
    onDismiss: () -> Unit,
    alignment: Alignment = Alignment.TopStart,
    offset: IntOffset = IntOffset.Zero,
    header: (@Composable () -> Unit)? = null,
    anchor: IntOffset? = null,
) {
    val focus = remember(actions.size) { List(actions.size) { FocusRequester() } }
    var focusedIndex by remember { mutableStateOf(-1) }
    val content: @Composable () -> Unit = {
        Column(
            Modifier.widthIn(min = 210.dp, max = 320.dp).heightIn(max = 420.dp)
                .background(nativeBackground, RoundedCornerShape(6.dp))
                .border(1.dp, nativeForeground.copy(alpha = .25f), RoundedCornerShape(6.dp))
                .padding(5.dp).verticalScroll(rememberScrollState())
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && event.key == Key.Escape) { onDismiss(); true } else false
                },
        ) {
            if (header != null) {
                header()
                Box(Modifier.padding(vertical = 4.dp).fillMaxWidth().height(1.dp).background(nativeForeground.copy(alpha = .12f)))
            }
            actions.forEachIndexed { index, item ->
                val interactionSource = remember { MutableInteractionSource() }
                val hovered by interactionSource.collectIsHoveredAsState()
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp))
                        .background(if (item.selected || index == focusedIndex || (hovered && item.enabled)) nativeAccent.copy(alpha = .2f) else Color.Transparent)
                        .hoverable(interactionSource, item.enabled)
                        .focusRequester(focus[index])
                        .onFocusChanged { if (it.isFocused) focusedIndex = index }
                        .onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown && event.key in listOf(Key.DirectionDown, Key.DirectionUp)) {
                                val enabled = actions.indices.filter { actions[it].enabled }
                                if (enabled.isNotEmpty()) {
                                    val next = enabled.indexOf(index) + if (event.key == Key.DirectionDown) 1 else -1
                                    focus[enabled[Math.floorMod(next, enabled.size)]].requestFocus()
                                }
                                true
                            } else false
                        }
                        .clickable(enabled = item.enabled, role = Role.Button) { onDismiss(); item.action() }
                        .padding(9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (item.swatch != null) {
                        NativeColorSwatch(item.swatch)
                        if (item.selected) NativeGlyph(NativeIcon.Check)
                    } else NativeGlyph(if (item.selected) NativeIcon.Check else item.icon)
                    Text(item.label, color = nativeForeground.copy(alpha = if (item.enabled) 1f else .45f))
                }
            }
            LaunchedEffect(Unit) {
                actions.indexOfFirst { it.enabled }.takeIf { it >= 0 }?.let { focus[it].requestFocus() }
            }
        }
    }
    val properties = PopupProperties(focusable = true)
    if (anchor != null) {
        val provider = remember(anchor) { PointerMenuPositionProvider(anchor) }
        Popup(popupPositionProvider = provider, onDismissRequest = onDismiss, properties = properties, content = content)
    } else {
        Popup(alignment = alignment, offset = offset, onDismissRequest = onDismiss, properties = properties, content = content)
    }
}

/** Opens at the pointer and flips/clamps so the whole menu stays inside the tool window. */
private class PointerMenuPositionProvider(private val anchor: IntOffset) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize,
    ): IntOffset {
        val x = anchorBounds.left + anchor.x
        val y = anchorBounds.top + anchor.y
        val left = if (x + popupContentSize.width > windowSize.width) x - popupContentSize.width else x
        val top = if (y + popupContentSize.height > windowSize.height) y - popupContentSize.height else y
        return IntOffset(
            left.coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0)),
            top.coerceIn(0, (windowSize.height - popupContentSize.height).coerceAtLeast(0)),
        )
    }
}

@Composable
internal fun NativeItemContext(actions: List<NativeMenuAction>, content: @Composable () -> Unit) {
    ContextMenuArea(items = {
        actions.filter { it.enabled }.map { action -> ContextMenuItem(action.label, action.action) }
    }, content = content)
}

@Composable
internal fun NativeHeading(title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (!subtitle.isNullOrBlank()) Text(subtitle, color = nativeForeground.copy(alpha = .65f), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun NativeDialog(
    title: String,
    onClose: () -> Unit,
    maxDialogWidth: Dp = 560.dp,
    maxDialogHeight: Dp = 600.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val width = minOf(maxWidth * .92f, maxDialogWidth)
        val height = minOf(maxHeight * .9f, maxDialogHeight)
        Popup(alignment = Alignment.Center, onDismissRequest = onClose, properties = PopupProperties(focusable = true)) {
            Column(
                Modifier.width(width).height(height).background(nativeBackground, RoundedCornerShape(8.dp))
                    .border(1.dp, nativeForeground.copy(alpha = .3f), RoundedCornerShape(8.dp)).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NativeHeading(title, modifier = Modifier.weight(1f))
                    NativeToolButton("Close $title", NativeIcon.Close, onClick = onClose)
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(nativeForeground.copy(alpha = .12f)))
                Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
            }
        }
    }
}

/**
 * Non-modal floating panel (like a chat window) layered over the content beneath it.
 * It blocks pointer events from reaching the content underneath, but the rest of the content stays usable.
 */
@Composable
internal fun NativeFloatingPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier.shadow(16.dp, shape).clip(shape).background(nativeBackground, shape)
            .border(1.dp, nativeForeground.copy(alpha = .25f), shape)
            .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } }
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

@Composable
internal fun NativeEmptyState(message: String, modifier: Modifier = Modifier.fillMaxSize(), action: (@Composable () -> Unit)? = null) {
    Box(modifier.padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(message, color = nativeForeground.copy(alpha = .7f), textAlign = TextAlign.Center)
            action?.invoke()
        }
    }
}

/** Bordered, hoverable surface that keeps list entries visually separated. */
@Composable
internal fun NativeDivider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(nativeForeground.copy(alpha = .12f)))
}

@Composable
internal fun NativeCard(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    Column(
        modifier.fillMaxWidth().clip(shape)
            .background(when {
                selected -> nativeAccent.copy(alpha = .14f)
                hovered && onClick != null -> nativeForeground.copy(alpha = .07f)
                else -> nativeForeground.copy(alpha = .035f)
            })
            .border(1.dp, if (selected) nativeAccent.copy(alpha = .6f) else nativeForeground.copy(alpha = if (hovered) .22f else .12f), shape)
            .hoverable(interactionSource)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        content = content,
    )
}

@Composable
internal fun NativeCardTitle(text: String, modifier: Modifier = Modifier, maxLines: Int = 2) {
    Text(text, modifier, fontWeight = FontWeight.SemiBold, maxLines = maxLines, overflow = TextOverflow.Ellipsis)
}

@Composable
internal fun NativeMeta(text: String, modifier: Modifier = Modifier, maxLines: Int = 1) {
    Text(text, modifier, color = nativeForeground.copy(alpha = .62f), fontSize = 12.sp, maxLines = maxLines, overflow = TextOverflow.Ellipsis)
}

/** Small rounded label used for statistics and metadata chips. */
@Composable
internal fun NativeChip(text: String, icon: NativeIcon? = null, color: Color? = null) {
    val shape = RoundedCornerShape(50)
    Row(
        Modifier.clip(shape).background(color?.copy(alpha = .18f) ?: nativeForeground.copy(alpha = .07f))
            .then(if (color != null) Modifier.border(1.dp, color.copy(alpha = .45f), shape) else Modifier)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (color != null && icon == null) Box(Modifier.size(8.dp).background(color, RoundedCornerShape(50)))
        if (icon != null) NativeGlyph(icon, Modifier.size(14.dp))
        Text(text, fontSize = 12.sp, maxLines = 1)
    }
}

@Composable
internal fun NativeSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), modifier.padding(top = 6.dp), color = nativeForeground.copy(alpha = .55f), fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold)
}

/** Small in-workspace prompt (rename, new name...) that submits on Enter and cancels on Escape. */
@Composable
internal fun NativeTextInputDialog(
    title: String,
    initial: String,
    confirmLabel: String = "Save",
    placeholder: String = "",
    onConfirm: (String) -> Unit,
    onClose: () -> Unit,
) {
    val field = rememberTextFieldState(initial)
    val focus = remember { FocusRequester() }
    val submit = {
        val value = field.text.toString().trim()
        if (value.isNotEmpty()) { onConfirm(value); onClose() }
    }
    NativeDialog(title, onClose = onClose, maxDialogWidth = 440.dp, maxDialogHeight = 190.dp) {
        TextField(field, Modifier.fillMaxWidth().focusRequester(focus).onPreviewKeyEvent { event ->
            when {
                event.type != KeyEventType.KeyDown -> false
                event.key == Key.Enter || event.key == Key.NumPadEnter -> { submit(); true }
                event.key == Key.Escape -> { onClose(); true }
                else -> false
            }
        }, placeholder = { Text(placeholder) })
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
            OutlinedButton(onClick = onClose) { Text("Cancel") }
            DefaultButton(onClick = submit, enabled = field.text.isNotBlank()) { Text(confirmLabel) }
        }
    }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        runCatching { focus.requestFocus() }
    }
}
