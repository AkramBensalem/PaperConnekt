package com.paperconnekt.app.legacygraph.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import com.paperconnekt.app.legacygraph.components.GraphCanvasTheme as MaterialTheme
import com.paperconnekt.app.legacygraph.components.GraphCanvasSurface as Surface
import com.paperconnekt.app.legacygraph.components.GraphCanvasText as Text
import androidx.compose.runtime.*
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import com.paperconnekt.app.legacygraph.storage.SavedGraphNodePosition
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paperconnekt.app.legacygraph.ConnectionType
import com.paperconnekt.app.legacygraph.PaperEdge
import com.paperconnekt.app.legacygraph.PaperGraph
import com.paperconnekt.app.legacygraph.PaperNode
import com.paperconnekt.app.legacygraph.seedNodes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.*
import kotlin.time.TimeSource

private const val GRAPH_GESTURE_DEBUG = false

// Level-of-detail budgets. They keep a frame bounded by what is on screen
// rather than by graph size.
private const val LABEL_MIN_SCALE = 0.42f
private const val MAX_LABELS_PER_FRAME = 220
private const val TEXT_LAYOUT_CACHE_LIMIT = 1_500
private const val DETAIL_MIN_SCREEN_RADIUS = 6f
private const val ARROW_MIN_SCREEN_SIZE = 3.5f
private const val DASH_MIN_SCALE = 0.3f
private const val MIN_SCREEN_STROKE = 0.6f
private const val CULL_MARGIN_PX = 24f

@Composable
fun GraphCanvas(
    graph: PaperGraph,
    onNodeClick: (PaperNode?) -> Unit,
    modifier: Modifier = Modifier,
    selectedNode: PaperNode? = null,
    disqualifiedPaperIds: Set<String> = emptySet(),
    expansionFocusNodeId: String? = null,
    initialNodePositions: Map<String, SavedGraphNodePosition> = emptyMap(),
    onPositionsChanged: (Map<String, Offset>) -> Unit = {},
    showPaperGrid: Boolean = true,
    showPerformanceStats: Boolean = false,
) {
    val zoomEventSensitivity = 0.42f
    val transformZoomBoost = 1.35f
    val pinchZoomSmoothing = 0.42f
    val pinchZoomEpsilon = 0.0015f
    // Pan/zoom state is only read from draw lambdas and event handlers so a
    // gesture invalidates drawing, never composition.
    var scale by remember { mutableFloatStateOf(1f) }
    var fittedScale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var layoutSize by remember { mutableStateOf(IntSize.Zero) }
    val textMeasurer = rememberTextMeasurer()
    val touchSlop = LocalViewConfiguration.current.touchSlop

    val layout = remember { GraphLayoutState() }
    val stats = remember { GraphRenderStats() }
    var draggedIndex by remember { mutableIntStateOf(-1) }
    var simulationKick by remember { mutableIntStateOf(0) }
    val currentOnNodeClick by rememberUpdatedState(onNodeClick)
    val currentOnPositionsChanged by rememberUpdatedState(onPositionsChanged)

    LaunchedEffect(graph, expansionFocusNodeId, initialNodePositions) {
        val started = TimeSource.Monotonic.markNow()
        val previous = if (layout.isEmpty) emptyMap() else layout.toNodePositionMap()
        val (index, positions) = withContext(Dispatchers.Default) {
            val index = GraphIndex(graph)
            val positions = calculateNodePositions(graph, previous, expansionFocusNodeId)
            index to if (initialNodePositions.isEmpty()) positions else positions.mapValues { (id, info) ->
                initialNodePositions[id]?.let { saved -> info.copy(offset = Offset(saved.x, saved.y)) } ?: info
            }
        }
        layout.replace(index, positions, initialNodePositions.keys)
        // Capture the generated positions as well as drag updates. This gives
        // the host a stable ID-keyed snapshot before a filter or expansion can
        // rebuild the visible node list.
        currentOnPositionsChanged(layout.toOffsetMap())
        stats.layoutMillis = started.elapsedNow().inWholeMilliseconds
    }

    // The force solver runs on primitive-array copies off the Compose thread.
    // Only the per-node delta is applied back, so concurrent drags are kept.
    LaunchedEffect(layout.generation, simulationKick) {
        val index = layout.index
        val n = index.size
        if (n == 0) return@LaunchedEffect
        val simulation = ForceSimulation(index)
        val vx = FloatArray(n)
        val vy = FloatArray(n)
        // Seeds and cluster anchors define the structure; they only move by drag.
        val fixed = BooleanArray(n) {
            index.isSeed[it] ||
                index.isClusterAnchor[it] ||
                index.ids[it] in layout.stableNodeIds
        }
        var frameCount = 0
        var quietFrames = 0
        while (true) {
            withFrameNanos { }
            if (layout.index !== index) break
            frameCount++
            val startXs = layout.xs.copyOf()
            val startYs = layout.ys.copyOf()
            val xs = startXs.copyOf()
            val ys = startYs.copyOf()
            val dragged = draggedIndex
            val cooling = (1f - (frameCount / 360f)).coerceIn(0.16f, 1f)
            val started = TimeSource.Monotonic.markNow()
            val moving = withContext(Dispatchers.Default) {
                simulation.step(xs, ys, vx, vy, fixed, dragged, cooling)
            }
            stats.simulationNanos += started.elapsedNow().inWholeNanoseconds
            stats.simulationFrames++
            if (layout.index !== index) break
            val liveXs = layout.xs
            val liveYs = layout.ys
            val draggedNow = draggedIndex
            for (i in 0 until n) {
                // The pointer-controlled node must never be moved by the solver.
                if (i == draggedNow) continue
                liveXs[i] += xs[i] - startXs[i]
                liveYs[i] += ys[i] - startYs[i]
            }
            layout.invalidate()
            quietFrames = if (moving) 0 else quietFrames + 1
            // Do not leave an animation coroutine alive due to tiny,
            // contradictory corrections around dense hubs.
            if (quietFrames >= 10 || frameCount >= 300) break
        }
    }

    val transitionProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        transitionProgress.snapTo(0f)
        transitionProgress.animateTo(1f, animationSpec = tween(1000))
    }

    val centralColor = Color(0xFF4F46E5) // Electric Indigo for seed paper
    val citedColor = Color(0xFF0D9488) // Emerald/Teal for prior references
    val citingColor = Color(0xFFD97706) // Warm Amber for citing papers
    val bothColor = Color(0xFFA855F7) // Royal Purple
    val selectedColor = Color(0xFFE11D48) // Rose, distinct from the seed color
    val disqualifiedColor = Color(0xFF94A3B8) // Slate 400
    val unrelatedColor = Color(0xFF64748B) // Slate 500
    val onSurface = MaterialTheme.colorScheme.onSurface

    val clampOffset: (Offset, Float) -> Offset = remember(layout) {
        { newOffset, newScale ->
            if (layoutSize != IntSize.Zero) {
                val bounds = layout.bounds()
                val width = layoutSize.width.toFloat()
                val height = layoutSize.height.toFloat()
                val minX = width * 0.2f - bounds.right * newScale
                val maxX = width * 0.8f - bounds.left * newScale
                val minY = height * 0.2f - bounds.bottom * newScale
                val maxY = height * 0.8f - bounds.top * newScale
                Offset(newOffset.x.coerceIn(min(minX, maxX), max(minX, maxX)), newOffset.y.coerceIn(min(minY, maxY), max(minY, maxY)))
            } else {
                newOffset
            }
        }
    }

    val updateTransform: (Float, Offset?, Offset) -> Unit = remember(layout) {
        { zoomFactor, focusPoint, pan ->
            if (zoomFactor != 1f || pan != Offset.Zero) {
                val limits = zoomLimitsFor(fittedScale)
                val oldScale = scale
                val newScale = (oldScale * zoomFactor).coerceIn(limits.min, limits.max)
                if (newScale != oldScale || pan != Offset.Zero) {
                    val actualZoomFactor = newScale / oldScale
                    if (layoutSize != IntSize.Zero) {
                        val focusX = focusPoint?.x ?: (layoutSize.width / 2f)
                        val focusY = focusPoint?.y ?: (layoutSize.height / 2f)
                        // Pivot zoom: the point under the focus stays under the focus (moved by pan).
                        val newOffset = Offset(
                            focusX + pan.x - (focusX - offset.x) * actualZoomFactor,
                            focusY + pan.y - (focusY - offset.y) * actualZoomFactor
                        )
                        offset = clampOffset(newOffset, newScale)
                    }
                    scale = newScale
                    if (GRAPH_GESTURE_DEBUG) {
                        println("[GraphCanvas][Transform] zoom=$zoomFactor pan=$pan scale=$oldScale->$newScale offset=$offset")
                    }
                }
            }
        }
    }

    // Zoom steps scale with how far the overview is from readable nodes, so a
    // graph with thousands of papers zooms in large fractions and a small
    // graph in fine ones.
    val zoomBy: (Float, Offset?) -> Unit = remember(layout) {
        { factor, focus -> updateTransform(adaptZoomFactor(factor, adaptiveZoomSpeed(fittedScale)), focus, Offset.Zero) }
    }

    LaunchedEffect(layoutSize, layout.nodeSetVersion) {
        if (layoutSize.width > 0 && !layout.isEmpty) {
            val bounds = layout.bounds()
            val padding = 200f
            val fitScale = min(
                layoutSize.width / (bounds.width + padding * 2),
                layoutSize.height / (bounds.height + padding * 2)
            ).coerceIn(1e-4f, 25f)
            fittedScale = fitScale
            scale = fitScale
            offset = Offset(
                layoutSize.width / 2f - (bounds.center.x * fitScale),
                layoutSize.height / 2f - (bounds.center.y * fitScale)
            )
        }
    }

    val generation = layout.generation
    val selection = remember(selectedNode?.paper?.id, generation) {
        GraphSelection.build(layout.index, selectedNode?.paper?.id)
    }
    val disqualified = remember(disqualifiedPaperIds, generation) {
        val index = layout.index
        BooleanArray(index.size) { index.ids[it] in disqualifiedPaperIds }
    }
    val drawCache = remember(textMeasurer) { GraphDrawCache(textMeasurer) }
    val palette = GraphPalette(
        central = centralColor,
        cited = citedColor,
        citing = citingColor,
        both = bothColor,
        selected = selectedColor,
        disqualified = disqualifiedColor,
        unrelated = unrelatedColor,
        onSurface = onSurface,
    )

    Column(modifier) {
    BoxWithConstraints(
        modifier = Modifier.weight(1f).fillMaxWidth()
            .fillMaxSize()
            .clipToBounds()
            .onGloballyPositioned { layoutSize = it.size }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        // Observe the initial pass so scroll/pinch input is still
                        // available even when the drag detector consumes the same
                        // pointer changes later in the gesture pipeline.
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val firstChange = event.changes.firstOrNull()
                        val isScrollEvent = event.type == PointerEventType.Scroll
                        val isNativeZoomEvent = event.type.toString() == "Zoom"
                        val hasZoomModifier =
                            event.keyboardModifiers.isCtrlPressed ||
                                event.keyboardModifiers.isMetaPressed

                        if (isScrollEvent || isNativeZoomEvent) {
                            val delta = firstChange?.scrollDelta ?: Offset.Zero
                            // Desktop mouse wheels and trackpad two-finger gestures are
                            // delivered as scroll events. Treat both as zoom so they
                            // have the same behavior as pinch input.
                            val rawZoomDelta = if (abs(delta.y) >= abs(delta.x)) delta.y else delta.x
                            // Trackpad pinch is commonly reported as very small
                            // Ctrl/Cmd-scroll values. Normalize those while keeping
                            // ordinary wheel steps responsive.
                            val zoomDelta = if (hasZoomModifier) {
                                rawZoomDelta.coerceIn(-4f, 4f)
                            } else {
                                rawZoomDelta.coerceIn(-12f, 12f)
                            }
                            val factor = exp(-zoomDelta * zoomEventSensitivity).coerceIn(0.72f, 1.38f)
                            if (abs(zoomDelta) > 0.0001f && factor != 1f && factor > 0f) {
                                zoomBy(factor, firstChange?.position)
                            }
                            event.changes.forEach { it.consume() }
                        } else if (GRAPH_GESTURE_DEBUG && event.type != PointerEventType.Move) {
                            println("[GraphCanvas][PointerEvent][Other] type=${event.type} changes=${event.changes.size}")
                        }
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { tapOffset -> zoomBy(1.5f, tapOffset) },
                )
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val gestureIndex = layout.index
                    val downWorld = (down.position - offset) / scale
                    val minTouchRadius = (32f / scale.coerceAtLeast(1e-4f)).coerceAtMost(72f)
                    var activeIndex = hitTestNode(layout, downWorld, minTouchRadius)
                    var dragStarted = false
                    var gestureMoved = false
                    var multiTouch = false
                    var previousCentroid = down.position
                    var previousPointerCount = 1

                    while (true) {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.filter { it.pressed }
                        if (pressed.isEmpty()) {
                            if (dragStarted) {
                                currentOnPositionsChanged(layout.toOffsetMap())
                                simulationKick++
                            } else if (!gestureMoved && !multiTouch) {
                                currentOnNodeClick(
                                    activeIndex.takeIf { it >= 0 && layout.index === gestureIndex }
                                        ?.let { gestureIndex.nodes[it] }
                                )
                            }
                            draggedIndex = -1
                            break
                        }

                        val pointerCountChanged = pressed.size != previousPointerCount
                        val centroid = event.calculateCentroid(useCurrent = true)
                        val pan = if (pointerCountChanged) Offset.Zero else centroid - previousCentroid
                        if (pressed.size > 1) multiTouch = true
                        if (!gestureMoved && (centroid - down.position).getDistance() > touchSlop) {
                            gestureMoved = true
                        }

                        if (activeIndex >= 0 && pressed.size == 1 && layout.index === gestureIndex) {
                            val pointerPosition = pressed.single().position
                            if (!dragStarted && (pointerPosition - down.position).getDistance() > touchSlop) {
                                dragStarted = true
                            }
                            if (dragStarted) {
                                draggedIndex = activeIndex
                                val graphPosition = (pointerPosition - offset) / scale
                                val dx = graphPosition.x - layout.xs[activeIndex]
                                val dy = graphPosition.y - layout.ys[activeIndex]
                                if (dx != 0f || dy != 0f) {
                                    moveConnectedNeighborhood(layout, activeIndex, dx, dy)
                                    layout.invalidate()
                                }
                            }
                        } else {
                            activeIndex = -1
                            // A pointer being added or removed changes the centroid abruptly.
                            // Keep that frame as the new baseline so pinch zoom does not jump.
                            val zoom = if (pressed.size > 1 && !pointerCountChanged) event.calculateZoom() else 1f
                            val smoothedZoom = if (zoom > 0f) exp(ln(zoom) * pinchZoomSmoothing) else 1f
                            val adjustedZoom = (1f + (smoothedZoom - 1f) * transformZoomBoost).coerceIn(0.9f, 1.1f)
                            if (pan != Offset.Zero || abs(adjustedZoom - 1f) > pinchZoomEpsilon) {
                                val adaptiveZoom = adaptZoomFactor(adjustedZoom, adaptiveZoomSpeed(fittedScale))
                                updateTransform(adaptiveZoom, centroid, pan)
                            }
                        }

                        event.changes.forEach { it.consume() }
                        previousCentroid = centroid
                        previousPointerCount = pressed.size
                    }
                }
            }
    ) {
        NativeTrackpadPinchZoom { zoom -> zoomBy(zoom, null) }

        if (showPaperGrid) {
            val gridColor = MaterialTheme.colorScheme.outlineVariant
            Canvas(Modifier.fillMaxSize()) { drawPaperGrid(gridColor, scale, offset) }
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = transitionProgress.value }
        ) {
            // Subscribe to position changes; everything else is read below.
            layout.version
            val started = TimeSource.Monotonic.markNow()
            drawGraphScene(
                layout = layout,
                viewScale = scale,
                offset = offset,
                progress = transitionProgress.value,
                selection = selection,
                disqualified = disqualified,
                palette = palette,
                cache = drawCache,
                stats = stats,
            )
            stats.recordDraw(started.elapsedNow().inWholeNanoseconds)
        }

        if (showPerformanceStats) {
            GraphPerformanceOverlay(
                stats = stats,
                layout = layout,
                zoomPercent = { scale / fittedScale * 100f },
                modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
            )
        }

        val compactControls = maxWidth < 600.dp || maxHeight < 480.dp
        val controlPadding = if (compactControls) 4.dp else 16.dp
        // Pan buttons move a fixed share of the viewport, whatever the zoom.
        val panStep = { min(layoutSize.width, layoutSize.height).coerceAtLeast(1) * 0.2f }
        Row(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .padding(start = 4.dp, end = controlPadding, bottom = controlPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PanControls(
                onPanLeft = { updateTransform(1f, null, Offset(panStep(), 0f)) },
                onPanRight = { updateTransform(1f, null, Offset(-panStep(), 0f)) },
                onPanUp = { updateTransform(1f, null, Offset(0f, panStep())) },
                onPanDown = { updateTransform(1f, null, Offset(0f, -panStep())) },
                compact = compactControls,
            )
            Box(
                Modifier.weight(1f).padding(horizontal = controlPadding),
                contentAlignment = Alignment.Center,
            ) {
                if (selectedNode != null) {
                    RelationshipLegend(
                        referencesColor = citedColor,
                        citationsColor = citingColor,
                        bothColor = bothColor,
                        unrelatedColor = unrelatedColor,
                        twoRows = compactControls,
                    )
                } else {
                    GeneralGraphLegend(
                        centralColor = centralColor,
                        referencesColor = citedColor,
                        citationsColor = citingColor,
                        disqualifiedColor = disqualifiedColor,
                        twoRows = compactControls,
                    )
                }
            }
            ZoomControlsHost(
                zoomScale = { scale / fittedScale },
                onZoomIn = { zoomBy(1.25f, null) },
                onZoomOut = { zoomBy(1f / 1.25f, null) },
                compact = compactControls,
            )
        }
    }
    }
}

/** Reads the zoom state in its own scope so zooming only recomposes this control. */
@Composable
private fun ZoomControlsHost(
    zoomScale: () -> Float,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    compact: Boolean,
) {
    ZoomControls(zoomScale = zoomScale(), onZoomIn = onZoomIn, onZoomOut = onZoomOut, compact = compact)
}

@Composable
private fun GraphPerformanceOverlay(
    stats: GraphRenderStats,
    layout: GraphLayoutState,
    zoomPercent: () -> Float,
    modifier: Modifier = Modifier,
) {
    var lines by remember { mutableStateOf(listOf("Collecting…")) }
    LaunchedEffect(stats, layout) {
        var last = TimeSource.Monotonic.markNow()
        while (true) {
            delay(1_000)
            val seconds = last.elapsedNow().inWholeMilliseconds.coerceAtLeast(1) / 1000f
            last = TimeSource.Monotonic.markNow()
            val avgDraw = if (stats.draws > 0) stats.totalDrawNanos / stats.draws / 1e6 else 0.0
            val avgSim = if (stats.simulationFrames > 0) stats.simulationNanos / stats.simulationFrames / 1e6 else 0.0
            lines = listOf(
                "Nodes ${layout.index.size} · Edges ${layout.index.edgeCount}",
                "Visible ${stats.visibleNodes} nodes · ${stats.visibleEdges} edges · ${stats.labels} labels",
                "Frames/s ${(stats.draws / seconds).roundToInt()} · draw avg ${avgDraw.format1()} ms · max ${(stats.maxDrawNanos / 1e6).format1()} ms",
                "Simulation ${avgSim.format1()} ms/step · ${stats.simulationFrames} steps/s",
                "Layout ${stats.layoutMillis} ms · zoom ${zoomPercent().roundToInt()}%",
            )
            stats.resetWindow()
        }
    }
    Surface(
        modifier = modifier,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
        tonalElevation = 4.dp,
    ) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
            lines.forEach { Text(it, style = MaterialTheme.typography.labelSmall) }
        }
    }
}

private fun Double.format1(): String {
    val tenths = (this * 10).roundToLong()
    return "${tenths / 10}.${abs(tenths % 10)}"
}

internal class GraphPalette(
    val central: Color,
    val cited: Color,
    val citing: Color,
    val both: Color,
    val selected: Color,
    val disqualified: Color,
    val unrelated: Color,
    val onSurface: Color,
)

/** Selected node and each node's direct relationship to it (by index). */
internal class GraphSelection(val selectedIndex: Int, val relationship: ByteArray) {
    fun relationshipOf(i: Int): DirectRelationship = when (relationship[i].toInt()) {
        1 -> DirectRelationship.INCOMING
        2 -> DirectRelationship.OUTGOING
        3 -> DirectRelationship.BIDIRECTIONAL
        else -> DirectRelationship.NONE
    }

    companion object {
        fun build(index: GraphIndex, selectedId: String?): GraphSelection {
            val selected = selectedId?.let { index.indexOf[it] } ?: -1
            val relationship = ByteArray(index.size)
            if (selected >= 0) {
                for (e in 0 until index.edgeCount) {
                    val f = index.edgeFrom[e]
                    val t = index.edgeTo[e]
                    when {
                        t == selected && f != selected -> relationship[f] = (relationship[f].toInt() or 1).toByte()
                        f == selected && t != selected -> relationship[t] = (relationship[t].toInt() or 2).toByte()
                    }
                }
            }
            return GraphSelection(selected, relationship)
        }
    }
}

/** Reusable drawing resources: batched paths and a bounded text layout cache. */
internal class GraphDrawCache(private val textMeasurer: TextMeasurer) {
    // Index = (citing ? 1 : 0) + (connected to selection ? 2 : 0)
    val edgePaths = Array(4) { Path() }
    val arrowPaths = Array(4) { Path() }
    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
    var labelCandidates = IntArray(256)
    private val textLayouts = HashMap<String, TextLayoutResult>()

    fun label(node: PaperNode, bold: Boolean): TextLayoutResult {
        val title = node.paper.title.take(26) + if (node.paper.title.length > 26) "…" else ""
        val key = (if (bold) "b:" else "r:") + title
        textLayouts[key]?.let { return it }
        if (textLayouts.size >= TEXT_LAYOUT_CACHE_LIMIT) textLayouts.clear()
        val result = textMeasurer.measure(
            title,
            style = TextStyle(fontSize = 11.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium),
        )
        textLayouts[key] = result
        return result
    }
}

internal fun DrawScope.drawGraphScene(
    layout: GraphLayoutState,
    viewScale: Float,
    offset: Offset,
    progress: Float,
    selection: GraphSelection,
    disqualified: BooleanArray,
    palette: GraphPalette,
    cache: GraphDrawCache,
    stats: GraphRenderStats,
) {
    val index = layout.index
    if (index.size == 0) {
        stats.visibleNodes = 0
        stats.visibleEdges = 0
        stats.labels = 0
        return
    }
    // A new layout can be drawn once before selection data is recomputed for it.
    val currentSelection = if (selection.relationship.size == index.size) selection else GraphSelection(-1, ByteArray(index.size))
    val currentDisqualified = if (disqualified.size == index.size) disqualified else BooleanArray(index.size)
    val viewport = visibleWorldRect(
        IntSize(size.width.toInt(), size.height.toInt()), viewScale, offset, CULL_MARGIN_PX
    ).inflate(index.maxRadius + 8f)
    withTransform({
        translate(offset.x, offset.y)
        scale(scaleX = viewScale, scaleY = viewScale, pivot = Offset.Zero)
    }) {
        stats.visibleEdges = drawEdges(layout, viewport, viewScale, progress, currentSelection, palette, cache)
        drawNodes(layout, viewport, viewScale, progress, currentSelection, currentDisqualified, palette, cache, stats)
    }
}

private fun DrawScope.drawEdges(
    layout: GraphLayoutState,
    viewport: Rect,
    scale: Float,
    progress: Float,
    selection: GraphSelection,
    palette: GraphPalette,
    cache: GraphDrawCache,
): Int {
    val index = layout.index
    val xs = layout.xs
    val ys = layout.ys
    val selected = selection.selectedIndex
    val drawArrows = scale * 9f >= ARROW_MIN_SCREEN_SIZE
    cache.edgePaths.forEach { it.reset() }
    cache.arrowPaths.forEach { it.reset() }
    val cos30 = 0.8660254f
    val sin30 = 0.5f
    var visible = 0
    for (e in 0 until index.edgeCount) {
        val f = index.edgeFrom[e]
        val t = index.edgeTo[e]
        val x1 = xs[f]
        val y1 = ys[f]
        val x2 = xs[t]
        val y2 = ys[t]
        if (max(x1, x2) < viewport.left || min(x1, x2) > viewport.right ||
            max(y1, y2) < viewport.top || min(y1, y2) > viewport.bottom
        ) continue
        val dx = x2 - x1
        val dy = y2 - y1
        val distance = sqrt(dx * dx + dy * dy)
        if (distance < 1f) continue
        val ux = dx / distance
        val uy = dy / distance
        val startX = x1 + ux * index.radius[f]
        val startY = y1 + uy * index.radius[f]
        val endX = x2 - ux * index.radius[t]
        val endY = y2 - uy * index.radius[t]
        val connected = selected >= 0 && (f == selected || t == selected)
        val group = (if (index.edgeCiting[e]) 1 else 0) + (if (connected) 2 else 0)
        cache.edgePaths[group].apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }
        if (drawArrows) {
            val arrowSize = if (connected) 12f else 9f
            cache.arrowPaths[group].apply {
                moveTo(endX, endY)
                lineTo(endX - arrowSize * (ux * cos30 + uy * sin30), endY - arrowSize * (uy * cos30 - ux * sin30))
                lineTo(endX - arrowSize * (ux * cos30 - uy * sin30), endY - arrowSize * (uy * cos30 + ux * sin30))
                close()
            }
        }
        visible++
    }
    val minStroke = MIN_SCREEN_STROKE / scale.coerceAtLeast(1e-4f)
    for (group in 0 until 4) {
        val citing = group and 1 != 0
        val connected = group and 2 != 0
        val color = if (citing) palette.citing else palette.cited
        val alpha = when {
            selected < 0 -> 0.55f
            connected -> 0.90f
            else -> 0.18f
        } * progress
        val width = when {
            connected -> 3.2f
            citing -> 2.2f
            else -> 1.8f
        }
        drawPath(
            path = cache.edgePaths[group],
            color = color.copy(alpha = alpha),
            style = Stroke(
                width = max(width, minStroke),
                pathEffect = if (citing && scale >= DASH_MIN_SCALE) cache.dashEffect else null,
            ),
        )
        if (drawArrows) drawPath(cache.arrowPaths[group], color = color.copy(alpha = alpha))
    }
    return visible
}

private fun DrawScope.drawNodes(
    layout: GraphLayoutState,
    viewport: Rect,
    scale: Float,
    progress: Float,
    selection: GraphSelection,
    disqualified: BooleanArray,
    palette: GraphPalette,
    cache: GraphDrawCache,
    stats: GraphRenderStats,
) {
    val index = layout.index
    val xs = layout.xs
    val ys = layout.ys
    val selected = selection.selectedIndex
    val showLabels = scale > LABEL_MIN_SCALE
    val referencesOfSeed = BooleanArray(index.size)
    val citationsOfSeed = BooleanArray(index.size)
    for (edge in 0 until index.edgeCount) {
        val from = index.edgeFrom[edge]
        val to = index.edgeTo[edge]
        if (index.isSeed[from] && !index.isSeed[to]) referencesOfSeed[to] = true
        if (index.isSeed[to] && !index.isSeed[from]) citationsOfSeed[from] = true
    }
    var candidates = cache.labelCandidates
    var candidateCount = 0
    var visible = 0

    fun nodeColor(i: Int): Color = when {
        disqualified[i] -> palette.disqualified
        i == selected -> palette.selected
        index.isCentral[i] -> palette.central
        selected >= 0 -> relationshipColor(selection.relationshipOf(i), palette.cited, palette.citing, palette.unrelated)
        else -> when {
            referencesOfSeed[i] && citationsOfSeed[i] -> palette.both
            referencesOfSeed[i] -> palette.cited
            citationsOfSeed[i] -> palette.citing
            index.isCentral[i] -> palette.central
            else -> palette.unrelated
        }
    }

    fun nodeAlpha(i: Int): Float = when {
        disqualified[i] -> 0.45f
        i == selected -> 1.0f
        index.isCentral[i] -> 1.0f
        selected >= 0 -> if (selection.relationship[i].toInt() != 0) 0.95f else 0.38f
        else -> 0.95f
    } * progress

    fun nodeProgress(i: Int): Float = (progress * 1.5f - layout.levels[i] * 0.2f).coerceIn(0f, 1f)

    for (i in 0 until index.size) {
        val x = xs[i]
        val y = ys[i]
        if (x < viewport.left || x > viewport.right || y < viewport.top || y > viewport.bottom) continue
        val nodeProgress = nodeProgress(i)
        val baseSize = index.radius[i] * nodeProgress
        if (baseSize <= 0f) continue
        visible++
        val center = Offset(x, y)
        val isSelected = i == selected
        val baseColor = nodeColor(i)
        val alpha = nodeAlpha(i)
        // Far-away nodes are a few pixels wide: one flat circle is visually identical.
        val detailed = baseSize * scale >= DETAIL_MIN_SCREEN_RADIUS

        if (detailed) {
            drawCircle(
                color = Color.Black.copy(alpha = 0.22f * alpha),
                radius = baseSize + 2.5f,
                center = center + Offset(1.5f, 2f)
            )
        }
        if (index.isCentral[i]) {
            drawCircle(color = baseColor.copy(alpha = 0.28f * alpha), radius = baseSize + 8f, center = center)
            drawCircle(
                color = Color.White.copy(alpha = 0.85f * alpha),
                radius = baseSize + 3f,
                center = center,
                style = Stroke(width = 2f)
            )
        }
        if (isSelected) {
            drawCircle(
                color = Color.White.copy(alpha = 0.9f * alpha),
                radius = baseSize + 6f,
                center = center,
                style = Stroke(width = 3.5f)
            )
        }
        if (detailed) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(baseColor.copy(alpha = alpha), baseColor.copy(alpha = alpha * 0.82f)),
                    center = center - Offset(baseSize * 0.25f, baseSize * 0.25f),
                    radius = max(baseSize * 1.1f, 0.1f)
                ),
                radius = baseSize,
                center = center
            )
            // Crisp outline ring so overlapping nodes stay distinct
            drawCircle(
                color = Color.White.copy(alpha = if (isSelected) 1.0f else 0.75f * alpha),
                radius = baseSize,
                center = center,
                style = Stroke(width = if (isSelected) 3.5f else 1.8f)
            )
        } else {
            drawCircle(color = baseColor.copy(alpha = alpha), radius = baseSize, center = center)
        }

        if (showLabels && nodeProgress > 0.5f) {
            if (candidateCount == candidates.size) {
                candidates = candidates.copyOf(candidates.size * 2)
                cache.labelCandidates = candidates
            }
            candidates[candidateCount++] = i
        }
    }
    stats.visibleNodes = visible

    // Labels are the most expensive primitive; draw at most a budget of them,
    // preferring the selection, seeds and highly cited papers.
    val labelOrder: List<Int> = if (candidateCount > MAX_LABELS_PER_FRAME) {
        (0 until candidateCount).map { candidates[it] }
            .sortedWith(
                compareByDescending<Int> { it == selected }
                    .thenByDescending { index.isCentral[it] }
                    .thenByDescending { index.radius[it] }
            )
            .take(MAX_LABELS_PER_FRAME)
    } else {
        (0 until candidateCount).map { candidates[it] }
    }
    labelOrder.forEach { i ->
        val bold = i == selected || index.isCentral[i]
        val textLayout = cache.label(index.nodes[i], bold)
        val alpha = nodeAlpha(i)
        val baseSize = index.radius[i] * nodeProgress(i)
        val textTopLeft = Offset(xs[i] - textLayout.size.width / 2f, ys[i] + baseSize + 6f)
        drawRoundRect(
            color = Color.White.copy(alpha = 0.5f * alpha),
            topLeft = textTopLeft - Offset(4f, 2f),
            size = Size(textLayout.size.width + 8f, textLayout.size.height + 4f),
            cornerRadius = CornerRadius(4f, 4f)
        )
        drawText(textLayout, color = palette.onSurface.copy(alpha = alpha.coerceAtLeast(0.7f)), topLeft = textTopLeft)
    }
    stats.labels = labelOrder.size
}

private const val PAPER_GRID_CELL = 32f

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPaperGrid(
    color: Color,
    scale: Float,
    offset: Offset,
) {
    // Keep the drawn cell size readable at any zoom by doubling/halving the world step.
    var step = PAPER_GRID_CELL * scale
    while (step < 16f) step *= 2f
    while (step > 128f) step /= 2f
    val minor = color.copy(alpha = color.alpha * 0.45f)
    val major = color.copy(alpha = color.alpha * 0.9f)

    fun firstLine(origin: Float): Float = ((origin % step) + step) % step
    fun isMajor(position: Float, origin: Float): Boolean =
        kotlin.math.round((position - origin) / step).toLong() % 4L == 0L

    var x = firstLine(offset.x)
    while (x <= size.width) {
        drawLine(if (isMajor(x, offset.x)) major else minor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
        x += step
    }
    var y = firstLine(offset.y)
    while (y <= size.height) {
        drawLine(if (isMajor(y, offset.y)) major else minor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        y += step
    }
}

@Composable
private fun GeneralGraphLegend(
    centralColor: Color,
    referencesColor: Color,
    citationsColor: Color,
    disqualifiedColor: Color,
    twoRows: Boolean,
    modifier: Modifier = Modifier
) {
    GraphLegendSurface(
        items = listOf(
            centralColor to "Seed Papers",
            referencesColor to "Prior References",
            citationsColor to "Citing Papers",
            disqualifiedColor to "Disqualified",
        ),
        twoRows = twoRows,
        modifier = modifier,
    )
}

@Composable
private fun RelationshipLegend(
    referencesColor: Color,
    citationsColor: Color,
    bothColor: Color,
    unrelatedColor: Color,
    twoRows: Boolean,
    modifier: Modifier = Modifier
) {
    GraphLegendSurface(
        items = listOf(
            referencesColor to "References",
            citationsColor to "Citations",
            bothColor to "Both",
            unrelatedColor to "Other Papers",
        ),
        twoRows = twoRows,
        modifier = modifier,
    )
}

@Composable
private fun GraphLegendSurface(
    items: List<Pair<Color, String>>,
    twoRows: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        if (twoRows) {
            Column(
                modifier = Modifier.padding(start = 6.dp, end = 10.dp, top = 5.dp, bottom = 5.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                items.chunked(2).forEach { rowItems ->
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        rowItems.forEach { (color, label) -> LegendColor(color, label) }
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { (color, label) -> LegendColor(color, label) }
            }
        }
    }
}

@Composable
private fun LegendColor(color: Color, label: String) {
    Row(
        modifier = Modifier.padding(start = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .padding(end = 4.dp)
                .size(8.dp)
                .background(color, MaterialTheme.shapes.extraSmall)
        )
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

data class NodePositionInfo(val offset: Offset, val level: Int)

internal enum class DirectRelationship(
    private val incoming: Boolean,
    private val outgoing: Boolean
) {
    NONE(false, false),
    INCOMING(true, false),
    OUTGOING(false, true),
    BIDIRECTIONAL(true, true);

    fun withIncoming(): DirectRelationship = when {
        incoming -> this
        outgoing -> BIDIRECTIONAL
        else -> INCOMING
    }

    fun withOutgoing(): DirectRelationship = when {
        outgoing -> this
        incoming -> BIDIRECTIONAL
        else -> OUTGOING
    }
}

private fun relationshipColor(
    relationship: DirectRelationship,
    referencesColor: Color,
    citationsColor: Color,
    unrelatedColor: Color
): Color = when (relationship) {
    DirectRelationship.INCOMING -> citationsColor
    DirectRelationship.OUTGOING -> referencesColor
    DirectRelationship.BIDIRECTIONAL -> Color(0xFFA855F7)
    DirectRelationship.NONE -> unrelatedColor
}

private fun nodeRelationshipColor(
    node: PaperNode,
    citedColor: Color,
    citingColor: Color,
    centralColor: Color
): Color = when (node.connectionType) {
    ConnectionType.CENTRAL -> centralColor
    ConnectionType.CITED_BY_CENTRAL -> citedColor
    ConnectionType.CITES_CENTRAL -> citingColor
}


internal fun calculateNodePositions(
    graph: PaperGraph,
    previous: Map<String, NodePositionInfo>,
    expansionFocusNodeId: String?
): Map<String, NodePositionInfo> {
    if (graph.nodes.isEmpty()) return emptyMap()
    val nodesById = HashMap<String, PaperNode>(graph.nodes.size * 2)
    graph.nodes.forEach { node -> if (node.paper.id !in nodesById) nodesById[node.paper.id] = node }
    val nodeIds = nodesById.keys
    val edges = graph.edges.filter { edge ->
        edge.fromId in nodeIds && edge.toId in nodeIds
    }
    val positions = previous
        .filterKeys { it in nodeIds }
        .mapValuesTo(mutableMapOf()) { it.value.offset }
    if (expansionFocusNodeId != null && expansionFocusNodeId in positions) {
        reserveExpansionSpace(
            graph = graph,
            edges = edges,
            positions = positions,
            focusNodeId = expansionFocusNodeId,
            newNodeIds = nodeIds - previous.keys
        )
    }
    graph.seedNodes.forEachIndexed { index, seed ->
        if (seed.paper.id !in positions) {
            positions[seed.paper.id] = if (positions.isEmpty()) Offset.Zero else {
                val angle = index * 2.399963f
                val radius = 650f + index * 120f
                Offset(cos(angle) * radius, sin(angle) * radius)
            }
        }
    }
    // Neighbor lists in edge order (duplicates kept) replace a scan of every
    // edge for every new node, which made the initial layout cubic.
    val neighborIds = HashMap<String, MutableList<String>>()
    edges.forEach { edge ->
        neighborIds.getOrPut(edge.fromId) { mutableListOf() }.add(edge.toId)
        if (edge.toId != edge.fromId) {
            neighborIds.getOrPut(edge.toId) { mutableListOf() }.add(edge.fromId)
        }
    }
    graph.nodes.forEachIndexed { index, node ->
        if (node.paper.id !in positions) {
            positions[node.paper.id] = initialClusterPosition(
                node, index, graph.nodes.size, neighborIds[node.paper.id].orEmpty(), positions
            )
        }
    }
    return positions.mapValues { (id, offset) ->
        NodePositionInfo(offset, nodesById.getValue(id).depth)
    }
}

private fun reserveExpansionSpace(
    graph: PaperGraph,
    edges: List<PaperEdge>,
    positions: MutableMap<String, Offset>,
    focusNodeId: String,
    newNodeIds: Set<String>
) {
    if (newNodeIds.isEmpty()) return
    val focus = positions[focusNodeId] ?: return
    val directNewIds = edges.asSequence()
        .filter { it.fromId == focusNodeId || it.toId == focusNodeId }
        .map { if (it.fromId == focusNodeId) it.toId else it.fromId }
        .filter { it in newNodeIds }
        .toSet()
    if (directNewIds.isEmpty()) return

    val connectedIds = buildGraphAdjacency(graph)[focusNodeId].orEmpty().toSet() + focusNodeId
    val candidateAngles = (0 until 16).map { it * (2f * PI.toFloat() / 16f) }
    val angle = candidateAngles.maxBy { candidate ->
        positions.asSequence()
            .filter { (id, _) -> id !in connectedIds }
            .map { (_, position) ->
                val projected = (position - focus).x * cos(candidate) + (position - focus).y * sin(candidate)
                projected.coerceAtMost(0f)
            }
            .sum()
    }
    val direction = Offset(cos(angle), sin(angle))
    val reservedRadius = 230f + directNewIds.size.coerceAtMost(12) * 28f

    // Clear a directional pocket before adding the branch. Existing unrelated
    // nodes are displaced instead of becoming an overlap pile at the focus.
    positions.forEach { (id, position) ->
        if (id !in connectedIds) {
            val delta = position - focus
            val forward = delta.x * direction.x + delta.y * direction.y
            val lateral = abs(delta.x * direction.y - delta.y * direction.x)
            if (forward > -80f && forward < reservedRadius && lateral < reservedRadius * 0.72f) {
                val push = (reservedRadius - forward).coerceAtLeast(0f) + 80f
                positions[id] = position + direction * push
            }
        }
    }

    directNewIds.sorted().forEachIndexed { index, id ->
        val spread = (index - (directNewIds.size - 1) / 2f) * 0.34f
        val placementDirection = Offset(cos(angle + spread), sin(angle + spread))
        positions[id] = focus + placementDirection * (180f + (index % 3) * 38f)
    }
}


private fun initialClusterPosition(
    node: PaperNode,
    index: Int,
    size: Int,
    neighborIds: List<String>,
    positions: Map<String, Offset>
): Offset {
    var sumX = 0.0
    var sumY = 0.0
    var count = 0
    neighborIds.forEach { id ->
        val position = positions[id] ?: return@forEach
        sumX += position.x
        sumY += position.y
        count++
    }
    if (count > 0) {
        val anchor = Offset((sumX / count).toFloat(), (sumY / count).toFloat())
        val angle = (abs(node.paper.id.hashCode()) % 360) * (PI.toFloat() / 180f)
        val radius = 300f + node.depth.coerceAtLeast(1) * 100f
        return anchor + Offset(cos(angle) * radius, sin(angle) * radius)
    }

    val angle = index * 2.399963f
    val radius = 300f + sqrt((index + 1).toFloat()) * 18f + sqrt(size.toFloat()) * 8f
    return Offset(
        x = cos(angle) * radius,
        y = sin(angle) * radius
    )
}

private fun buildGraphAdjacency(graph: PaperGraph): Map<String, List<String>> {
    return buildMap<String, MutableList<String>> {
        graph.edges.forEach { edge ->
            getOrPut(edge.fromId) { mutableListOf() }.add(edge.toId)
            getOrPut(edge.toId) { mutableListOf() }.add(edge.fromId)
        }
    }.mapValues { it.value.distinct() }
}
