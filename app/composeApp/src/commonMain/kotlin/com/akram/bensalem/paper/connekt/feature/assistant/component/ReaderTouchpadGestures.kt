package com.akram.bensalem.paper.connekt.feature.assistant.component

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isMetaPressed
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sign
import kotlin.time.TimeSource

private const val ZOOM_EVENT_SENSITIVITY = 0.42f

/**
 * Keeps the content under the pointer fixed while zooming, like native PDF viewers. The visible
 * layout registers itself with [rememberReaderZoomTarget]; the anchor captures the pointed-at content
 * before a zoom step and restores it once the pages are laid out at the new size.
 */
internal class ReaderZoomAnchor {
    internal var target: ReaderZoomTarget? = null
    private var viewport: LayoutCoordinates? = null
    private var pointer: Offset? = null

    val horizontal: ScrollState? get() = target?.horizontal

    internal fun track(coordinates: LayoutCoordinates) { viewport = coordinates }

    internal fun pointerAt(local: Offset) {
        pointer = viewport?.takeIf { it.isAttached }?.localToWindow(local)
    }

    /** Call right before changing the zoom; [atPointer] = false anchors at the viewport centre. */
    fun capture(atPointer: Boolean = true) { target?.capture(pointer.takeIf { atPointer }) }

    internal fun pan(delta: Offset) { target?.pan(delta) }
}

internal val LocalReaderZoomAnchor = staticCompositionLocalOf<ReaderZoomAnchor?> { null }

internal class ReaderZoomTarget(
    val horizontal: ScrollState,
    private val horizontalPadding: Float,
    private val verticalPadding: Float,
) {
    var vertical: ScrollState? = null
    var list: LazyListState? = null
    var bounds: Rect = Rect.Zero
    var contentWidth = 1f
    private var pending by mutableStateOf<Pending?>(null)
    val awaitingRestore: Boolean get() = pending != null

    private class Pending(var x: Float, var y: Float, val fractionX: Float, val contentY: Float,
        val item: Int, val itemOffset: Float, val width: Float)

    private fun lead(width: Float) = maxOf((bounds.width - width) / 2f, horizontalPadding)

    fun capture(pointer: Offset?) {
        // Several zoom steps can arrive before the next layout; keep the first, still-valid anchor.
        if (pending != null || bounds.isEmpty) return
        val point = pointer?.takeIf(bounds::contains) ?: bounds.center
        val x = point.x - bounds.left
        val y = point.y - bounds.top
        val width = contentWidth.coerceAtLeast(1f)
        pending = Pending(x, y,
            fractionX = (horizontal.value + x - lead(width)) / width,
            contentY = (vertical?.value ?: 0) + y - verticalPadding,
            item = list?.firstVisibleItemIndex ?: 0,
            itemOffset = (list?.firstVisibleItemScrollOffset ?: 0) + y,
            width = width)
    }

    suspend fun restore() {
        val anchor = pending ?: return
        apply(anchor, first = true)
        // Keep the original anchor if another zoom step cancels this effect before layout settles.
        repeat(SETTLE_FRAMES) {
            withFrameNanos { }
            if (apply(anchor, first = false)) {
                pending = null
                return
            }
        }
        pending = null
    }

    fun pan(delta: Offset) {
        pending?.let {
            it.x += delta.x
            it.y += delta.y
        } ?: run {
            horizontal.dispatchRawDelta(-delta.x)
            vertical?.dispatchRawDelta(-delta.y)
            list?.dispatchRawDelta(-delta.y)
        }
    }

    /** Scrolls to the anchor; returns true once the scroll positions match it. */
    private suspend fun apply(anchor: Pending, first: Boolean): Boolean {
        val width = contentWidth.coerceAtLeast(1f)
        val ratio = width / anchor.width
        val x = (lead(width) + anchor.fractionX * width - anchor.x).roundToInt().coerceAtLeast(0)
        val y = (verticalPadding + anchor.contentY * ratio - anchor.y).roundToInt().coerceAtLeast(0)
        horizontal.scrollTo(x)
        vertical?.scrollTo(y)
        list?.let { state ->
            val offset = (anchor.itemOffset * ratio - anchor.y).roundToInt()
            if (first) state.requestScrollToItem(anchor.item, offset.coerceAtLeast(0))
            else if (offset < 0 && state.firstVisibleItemIndex == anchor.item && state.firstVisibleItemScrollOffset == 0) {
                state.scrollBy(offset.toFloat())
            }
        }
        val settledX = horizontal.value == x || (x > horizontal.maxValue && horizontal.value == horizontal.maxValue && !first)
        val settledY = vertical?.let { it.value == y } ?: true
        return settledX && settledY
    }

    private companion object { const val SETTLE_FRAMES = 3 }
}

/**
 * Registers the visible layout for pointer-anchored zoom and touchpad swipes. [contentWidth] is the
 * width of the pages (without padding); content is assumed to be centred when narrower than the view.
 */
@Composable
internal fun rememberReaderZoomTarget(
    horizontal: ScrollState,
    contentWidth: Dp,
    vertical: ScrollState? = null,
    list: LazyListState? = null,
    horizontalPadding: Dp = 24.dp,
    verticalPadding: Dp = 24.dp,
): Modifier {
    val anchor = LocalReaderZoomAnchor.current ?: return Modifier
    val density = LocalDensity.current
    val target = remember(anchor, horizontal, density) {
        with(density) { ReaderZoomTarget(horizontal, horizontalPadding.toPx(), verticalPadding.toPx()) }
    }
    target.vertical = vertical
    target.list = list
    val width = with(density) { contentWidth.toPx() }
    target.contentWidth = width
    DisposableEffect(anchor, target) {
        anchor.target = target
        onDispose { if (anchor.target === target) anchor.target = null }
    }
    LaunchedEffect(target, width, target.awaitingRestore) { target.restore() }
    return Modifier.onGloballyPositioned { target.bounds = it.boundsInWindow() }
}

/**
 * Touch pinches zoom around the fingers' centroid and pan in both axes. Consume the entire
 * multi-touch gesture so the page cannot also select text or finish an annotation.
 * Desktop touchpads use Ctrl/Cmd + scroll, native zoom events, or [NativeReaderTouchpad].
 */
internal fun Modifier.readerTouchpadGestures(
    anchor: ReaderZoomAnchor,
    currentZoom: Float,
    onZoom: (factor: Float) -> Unit,
): Modifier = composed {
    val zoomValue by rememberUpdatedState(currentZoom)
    val zoom by rememberUpdatedState { factor: Float ->
        if ((zoomValue * factor).coerceIn(0.5f, 3f) != zoomValue) {
            anchor.capture()
            onZoom(factor)
        }
    }
    val hovered = remember { mutableStateOf(false) }
    NativeReaderTouchpad(
        enabled = { hovered.value },
        onZoom = { zoom(it) },
        onHorizontalPan = { dx -> anchor.horizontal?.dispatchRawDelta(if (TouchpadDirection.natural) -dx else dx) },
    )
    onGloballyPositioned(anchor::track).pointerInput(Unit) {
        awaitPointerEventScope {
            var transformingTouch = false
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val touches = event.changes.count { it.pressed && it.type == PointerType.Touch }
                if (touches >= 2) {
                    transformingTouch = true
                    anchor.pointerAt(event.calculateCentroid(useCurrent = false))
                    val factor = event.calculateZoom()
                    if (factor.isFinite() && factor > 0f && factor != 1f) zoom(factor)
                    anchor.pan(event.calculatePan())
                    event.changes.forEach { it.consume() }
                    continue
                }
                if (transformingTouch) {
                    event.changes.forEach { it.consume() }
                    if (event.changes.none { it.pressed }) transformingTouch = false
                    continue
                }
                event.changes.firstOrNull()?.position?.let(anchor::pointerAt)
                when (event.type) {
                    PointerEventType.Enter, PointerEventType.Move -> hovered.value = true
                    PointerEventType.Exit -> hovered.value = false
                }
                val factor = pinchZoomFactor(event)
                if (factor != null) {
                    if (factor != 1f) zoom(factor)
                    event.changes.forEach { it.consume() }
                } else if (event.type == PointerEventType.Scroll && !event.keyboardModifiers.isShiftPressed) {
                    event.changes.firstOrNull()?.scrollDelta?.y?.let(TouchpadDirection::observeScroll)
                }
            }
        }
    }
}

/**
 * Learns whether the system uses natural scrolling by comparing native finger motion with the
 * vertical scroll Compose receives, so horizontal swipes move the same way as vertical ones.
 */
internal object TouchpadDirection {
    @kotlin.concurrent.Volatile var natural = true
    @kotlin.concurrent.Volatile private var fingerDy = 0f
    @kotlin.concurrent.Volatile private var fingerAt = TimeSource.Monotonic.markNow()

    fun observeFingers(dy: Float) {
        if (abs(dy) < 1f) return
        fingerDy = dy
        fingerAt = TimeSource.Monotonic.markNow()
    }

    fun observeScroll(scrollDy: Float) {
        if (abs(scrollDy) < 0.01f || fingerDy == 0f || fingerAt.elapsedNow().inWholeMilliseconds > 150) return
        natural = sign(fingerDy) != sign(scrollDy)
    }
}

/** Returns the zoom factor for a pinch event, or `null` when the event is not a pinch. */
internal fun pinchZoomFactor(event: PointerEvent): Float? {
    val modifiers = event.keyboardModifiers
    val nativeZoom = event.type.toString() == "Zoom"
    if (nativeZoom || (event.type == PointerEventType.Scroll && (modifiers.isCtrlPressed || modifiers.isMetaPressed))) {
        val delta = event.changes.firstOrNull()?.scrollDelta ?: Offset.Zero
        val raw = if (abs(delta.y) >= abs(delta.x)) delta.y else delta.x
        val zoomDelta = raw.coerceIn(-4f, 4f)
        if (abs(zoomDelta) < 0.0001f) return 1f
        return exp(-zoomDelta * ZOOM_EVENT_SENSITIVITY).coerceIn(0.72f, 1.38f)
    }
    if (event.changes.count { it.pressed } >= 2) {
        val factor = event.calculateZoom()
        // Leave two-finger panning to the scroll containers.
        return if (factor.isFinite() && factor > 0f && abs(factor - 1f) > 0.001f) factor else null
    }
    return null
}

/** Classifies two-finger touchpad motion as a pinch (zoom) or a swipe (pan). */
internal class TouchpadGestureDetector {
    sealed interface Result {
        data class Zoom(val factor: Float) : Result
        data class Pan(val dx: Float, val dy: Float) : Result
    }

    private enum class Mode { Undecided, Pinch, Pan }

    private var mode = Mode.Undecided
    private var startDistance = 0f
    private var startX = 0f
    private var startY = 0f
    private var lastDistance = 0f
    private var lastX = 0f
    private var lastY = 0f

    fun reset() { mode = Mode.Undecided; startDistance = 0f }

    fun update(x1: Float, y1: Float, x2: Float, y2: Float): Result? {
        val distance = kotlin.math.hypot(x1 - x2, y1 - y2)
        val cx = (x1 + x2) / 2f
        val cy = (y1 + y2) / 2f
        if (distance < MIN_DISTANCE) { reset(); return null }
        if (startDistance == 0f) {
            startDistance = distance; startX = cx; startY = cy
            lastDistance = distance; lastX = cx; lastY = cy
            return null
        }
        val dx = cx - lastX
        val dy = cy - lastY
        val previousDistance = lastDistance
        lastDistance = distance; lastX = cx; lastY = cy
        if (mode == Mode.Undecided) {
            val spread = abs(distance - startDistance)
            val travel = kotlin.math.hypot(cx - startX, cy - startY)
            mode = when {
                spread / startDistance > PINCH_START_RATIO && spread > travel * 0.8f -> Mode.Pinch
                travel > PAN_START_TRAVEL && travel > spread -> Mode.Pan
                else -> return null
            }
        }
        return when (mode) {
            Mode.Pinch -> (distance / previousDistance).coerceIn(0.8f, 1.25f)
                .takeIf { abs(it - 1f) > 0.001f }?.let(Result::Zoom)
            Mode.Pan -> Result.Pan(dx, dy).takeIf { dx != 0f || dy != 0f }
            Mode.Undecided -> null
        }
    }

    private companion object {
        const val MIN_DISTANCE = 4f
        const val PINCH_START_RATIO = 0.06f
        const val PAN_START_TRAVEL = 12f
    }
}

@Composable
internal expect fun NativeReaderTouchpad(
    enabled: () -> Boolean,
    onZoom: (Float) -> Unit,
    onHorizontalPan: (Float) -> Unit,
)
