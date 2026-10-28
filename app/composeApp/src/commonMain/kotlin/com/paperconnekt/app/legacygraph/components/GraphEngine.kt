package com.paperconnekt.app.legacygraph.components

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize
import com.paperconnekt.app.legacygraph.ConnectionType
import com.paperconnekt.app.legacygraph.PaperGraph
import com.paperconnekt.app.legacygraph.PaperNode
import com.paperconnekt.app.legacygraph.seedNodes
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

internal const val HUB_MIN_DEGREE = 4
private const val SIMULATION_VELOCITY_EPSILON = 0.35f
private const val SIMULATION_FORCE_EPSILON = 2.5f
private const val SIMULATION_CELL_SIZE = 110f
private const val OVERLAP_SEPARATION_MAX_NODES = 5_000

/**
 * Immutable, index-based view of a [PaperGraph]. Every per-frame operation
 * (drawing, simulation, hit testing) works on primitive arrays instead of
 * looking nodes up by id, which used to make edge drawing O(edges × nodes).
 */
internal class GraphIndex(graph: PaperGraph) {
    val nodes: List<PaperNode> = graph.nodes
    val size: Int = nodes.size
    val ids: Array<String> = Array(size) { nodes[it].paper.id }
    val indexOf: HashMap<String, Int> = HashMap<String, Int>(size * 2).apply {
        ids.forEachIndexed { index, id -> if (id !in this) put(id, index) }
    }
    val radius: FloatArray = FloatArray(size) { nodeRadius(nodes[it]) }
    val depth: IntArray = IntArray(size) { nodes[it].depth }
    val isCentral: BooleanArray = BooleanArray(size) { nodes[it].connectionType == ConnectionType.CENTRAL }
    val isSeed: BooleanArray = BooleanArray(size).also { seeds ->
        graph.seedNodes.forEach { seed -> indexOf[seed.paper.id]?.let { seeds[it] = true } }
    }
    val maxRadius: Float = radius.maxOrNull() ?: 14f

    val edgeFrom: IntArray
    val edgeTo: IntArray
    val edgeCiting: BooleanArray

    /** Edge degree including duplicate edges, matching the force model's hub detection. */
    val degree: IntArray = IntArray(size)

    /** Distinct neighbors in edge order. */
    val neighbors: Array<IntArray>

    /** Nodes with at least [HUB_MIN_DEGREE] distinct neighbors define cluster structure. */
    val isClusterAnchor: BooleanArray

    init {
        val from = ArrayList<Int>(graph.edges.size)
        val to = ArrayList<Int>(graph.edges.size)
        graph.edges.forEach { edge ->
            val f = indexOf[edge.fromId] ?: return@forEach
            val t = indexOf[edge.toId] ?: return@forEach
            from += f
            to += t
        }
        edgeFrom = from.toIntArray()
        edgeTo = to.toIntArray()
        edgeCiting = BooleanArray(edgeFrom.size) { e ->
            nodes[edgeFrom[e]].connectionType == ConnectionType.CITES_CENTRAL ||
                nodes[edgeTo[e]].connectionType == ConnectionType.CITES_CENTRAL
        }
        val distinct = Array(size) { LinkedHashSet<Int>() }
        for (e in edgeFrom.indices) {
            val f = edgeFrom[e]
            val t = edgeTo[e]
            degree[f]++
            degree[t]++
            distinct[f] += t
            distinct[t] += f
        }
        neighbors = Array(size) { distinct[it].toIntArray() }
        isClusterAnchor = BooleanArray(size) { neighbors[it].size >= HUB_MIN_DEGREE }
    }

    val edgeCount: Int get() = edgeFrom.size

    companion object {
        val Empty = GraphIndex(PaperGraph())
    }
}

internal fun nodeRadius(node: PaperNode): Float {
    val citationScore = ln((node.citationCount.coerceAtLeast(0) + 1).toFloat())
    return if (node.connectionType == ConnectionType.CENTRAL) {
        (30f + citationScore * 1.8f).coerceIn(30f, 42f)
    } else {
        (10f + citationScore * 2.8f).coerceIn(10f, 32f)
    }
}

/**
 * Node positions stored in flat float arrays. Mutations bump a single
 * snapshot counter ([version]) instead of writing thousands of snapshot map
 * entries per frame, so only the draw phase is invalidated.
 */
@Stable
internal class GraphLayoutState {
    var index: GraphIndex = GraphIndex.Empty
        private set
    var xs: FloatArray = FloatArray(0)
        private set
    var ys: FloatArray = FloatArray(0)
        private set
    var levels: IntArray = IntArray(0)
        private set

    /** Bumped when a new layout (new node set) is applied. */
    var generation by mutableIntStateOf(0)
        private set

    /** Bumped on every position change; read only from draw lambdas. */
    var version by mutableIntStateOf(0)
        private set

    var nodeCount by mutableIntStateOf(0)
        private set

    /** Bumped only when the set of node ids changes (used to refit the viewport). */
    var nodeSetVersion by mutableIntStateOf(0)
        private set

    private var mutations = 0
    private var boundsMutation = -1
    private var cachedBounds = Rect(1000f, 1000f, 1000f, 1000f)

    val isEmpty: Boolean get() = index.size == 0

    fun replace(newIndex: GraphIndex, positions: Map<String, NodePositionInfo>) {
        val n = newIndex.size
        val newXs = FloatArray(n)
        val newYs = FloatArray(n)
        val newLevels = IntArray(n)
        for (i in 0 until n) {
            val info = positions[newIndex.ids[i]] ?: continue
            newXs[i] = info.offset.x
            newYs[i] = info.offset.y
            newLevels[i] = info.level
        }
        val nodeSetChanged = !newIndex.ids.contentEquals(index.ids)
        index = newIndex
        xs = newXs
        ys = newYs
        levels = newLevels
        nodeCount = n
        if (nodeSetChanged) nodeSetVersion++
        generation++
        invalidate()
    }

    fun invalidate() {
        mutations++
        version++
    }

    fun position(i: Int): Offset = Offset(xs[i], ys[i])

    fun bounds(): Rect {
        if (boundsMutation == mutations) return cachedBounds
        boundsMutation = mutations
        cachedBounds = computeBounds(xs, ys, index.size)
        return cachedBounds
    }

    fun toNodePositionMap(): Map<String, NodePositionInfo> {
        val result = LinkedHashMap<String, NodePositionInfo>(index.size * 2)
        for (i in 0 until index.size) result[index.ids[i]] = NodePositionInfo(Offset(xs[i], ys[i]), levels[i])
        return result
    }

    fun toOffsetMap(): Map<String, Offset> {
        val result = LinkedHashMap<String, Offset>(index.size * 2)
        for (i in 0 until index.size) result[index.ids[i]] = Offset(xs[i], ys[i])
        return result
    }
}

internal fun computeBounds(xs: FloatArray, ys: FloatArray, count: Int): Rect {
    if (count == 0) return Rect(1000f, 1000f, 1000f, 1000f)
    var minX = Float.POSITIVE_INFINITY
    var minY = Float.POSITIVE_INFINITY
    var maxX = Float.NEGATIVE_INFINITY
    var maxY = Float.NEGATIVE_INFINITY
    for (i in 0 until count) {
        minX = min(minX, xs[i])
        minY = min(minY, ys[i])
        maxX = max(maxX, xs[i])
        maxY = max(maxY, ys[i])
    }
    return Rect(minX, minY, maxX, maxY)
}

/** Uniform-grid spatial hash using an intrusive linked list (no per-cell lists). */
internal class SpatialGrid(capacity: Int, private val cellSize: Float) {
    private val head = HashMap<Long, Int>()
    private val next = IntArray(capacity)

    fun rebuild(xs: FloatArray, ys: FloatArray, count: Int) {
        head.clear()
        for (i in 0 until count) {
            val key = key(cell(xs[i]), cell(ys[i]))
            next[i] = head[key] ?: -1
            head[key] = i
        }
    }

    fun cell(value: Float): Int = floor(value / cellSize).toInt()

    inline fun forEachNear(x: Float, y: Float, action: (Int) -> Unit) {
        val cx = cell(x)
        val cy = cell(y)
        for (dx in -1..1) for (dy in -1..1) {
            var j = first(cx + dx, cy + dy)
            while (j >= 0) {
                action(j)
                j = nextOf(j)
            }
        }
    }

    fun first(cx: Int, cy: Int): Int = head[key(cx, cy)] ?: -1
    fun nextOf(i: Int): Int = next[i]

    private fun key(cx: Int, cy: Int): Long = (cx.toLong() shl 32) or (cy.toLong() and 0xFFFFFFFFL)
}

/**
 * Array-based port of the force model (repulsion via spatial hash, springs
 * with hub spoke lengths, hub sector spreading, overlap separation).
 * Not thread-safe: one instance per simulation coroutine.
 */
internal class ForceSimulation(private val index: GraphIndex) {
    private val n = index.size
    private val fx = FloatArray(n)
    private val fy = FloatArray(n)
    private val grid = SpatialGrid(n, SIMULATION_CELL_SIZE)
    private val hubNeighbors: Array<IntArray?>

    init {
        val sets = arrayOfNulls<LinkedHashSet<Int>>(n)
        for (e in 0 until index.edgeCount) {
            val f = index.edgeFrom[e]
            val t = index.edgeTo[e]
            if (index.degree[f] >= HUB_MIN_DEGREE) (sets[f] ?: LinkedHashSet<Int>().also { sets[f] = it }) += t
            if (index.degree[t] >= HUB_MIN_DEGREE) (sets[t] ?: LinkedHashSet<Int>().also { sets[t] = it }) += f
        }
        hubNeighbors = Array(n) { sets[it]?.toIntArray() }
    }

    /**
     * Advances [xs]/[ys]/[vx]/[vy] in place by one frame.
     * @return whether any free node is still moving.
     */
    fun step(
        xs: FloatArray,
        ys: FloatArray,
        vx: FloatArray,
        vy: FloatArray,
        fixed: BooleanArray,
        draggedIndex: Int,
        cooling: Float,
    ): Boolean {
        if (n == 0) return false
        fx.fill(0f)
        fy.fill(0f)
        applyRepulsion(xs, ys)
        applySprings(xs, ys)
        applyHubSectorForces(xs, ys)

        var moving = false
        for (i in 0 until n) {
            if (fixed[i] || i == draggedIndex) {
                vx[i] = 0f
                vy[i] = 0f
                continue
            }
            var forceX = fx[i]
            var forceY = fy[i]
            val forceLength = sqrt(forceX * forceX + forceY * forceY)
            if (forceLength > 120f) {
                forceX *= 120f / forceLength
                forceY *= 120f / forceLength
            }
            forceX *= cooling
            forceY *= cooling
            var velX = (vx[i] + forceX * 0.25f) * 0.68f
            var velY = (vy[i] + forceY * 0.25f) * 0.68f
            val velocity = sqrt(velX * velX + velY * velY)
            val force = sqrt(forceX * forceX + forceY * forceY)
            if (velocity < SIMULATION_VELOCITY_EPSILON && force < SIMULATION_FORCE_EPSILON) {
                velX = 0f
                velY = 0f
            } else if (velocity > 72f) {
                velX *= 72f / velocity
                velY *= 72f / velocity
            }
            vx[i] = velX
            vy[i] = velY
            xs[i] += velX
            ys[i] += velY
            if (sqrt(velX * velX + velY * velY) >= SIMULATION_VELOCITY_EPSILON) moving = true
        }

        if (n <= OVERLAP_SEPARATION_MAX_NODES) {
            separateOverlaps(xs, ys, fixed, draggedIndex)
        }
        return moving
    }

    private fun applyRepulsion(xs: FloatArray, ys: FloatArray) {
        grid.rebuild(xs, ys, n)
        val radius = index.radius
        for (i in 0 until n) {
            val x = xs[i]
            val y = ys[i]
            grid.forEachNear(x, y) { j ->
                if (j <= i) return@forEachNear
                var dx = xs[j] - x
                var dy = ys[j] - y
                val distance = sqrt(dx * dx + dy * dy)
                if (distance < 0.01f) {
                    val angle = (index.ids[i].hashCode() xor index.ids[j].hashCode()) * 0.017f
                    dx = kotlin.math.cos(angle)
                    dy = kotlin.math.sin(angle)
                } else {
                    dx /= distance
                    dy /= distance
                }
                val minimum = radius[i] + radius[j] + 18f
                val safe = distance.coerceAtLeast(1f)
                val strength = (7200f / (safe * safe) + (minimum - safe).coerceAtLeast(0f) * 1.8f).coerceAtMost(55f)
                fx[i] -= dx * strength
                fy[i] -= dy * strength
                fx[j] += dx * strength
                fy[j] += dy * strength
            }
        }
    }

    private fun applySprings(xs: FloatArray, ys: FloatArray) {
        val degree = index.degree
        for (e in 0 until index.edgeCount) {
            val f = index.edgeFrom[e]
            val t = index.edgeTo[e]
            val dx = xs[t] - xs[f]
            val dy = ys[t] - ys[f]
            val distance = sqrt(dx * dx + dy * dy).coerceAtLeast(0.1f)
            val ux = dx / distance
            val uy = dy / distance
            val depthGap = abs(index.depth[f] - index.depth[t])
            val hub = when {
                degree[f] >= HUB_MIN_DEGREE -> f
                degree[t] >= HUB_MIN_DEGREE -> t
                else -> -1
            }
            val rest = if (hub >= 0) {
                val other = if (hub == f) t else f
                hubSpokeRestDistance(index.radius[hub], index.radius[other], degree[hub])
            } else {
                (150f + depthGap * 18f).coerceIn(135f, 220f)
            }
            val pull = ((distance - rest) * 0.062f).coerceIn(-30f, 40f)
            fx[f] += ux * pull
            fy[f] += uy * pull
            fx[t] -= ux * pull
            fy[t] -= uy * pull
        }
    }

    private fun applyHubSectorForces(xs: FloatArray, ys: FloatArray) {
        val twoPi = 2f * PI.toFloat()
        for (hub in 0 until n) {
            val neighborIds = hubNeighbors[hub] ?: continue
            val hx = xs[hub]
            val hy = ys[hub]
            val entries = ArrayList<SectorEntry>(neighborIds.size)
            for (id in neighborIds) {
                val rx = xs[id] - hx
                val ry = ys[id] - hy
                val length = sqrt(rx * rx + ry * ry)
                if (length < 1f) continue
                entries += SectorEntry(id, atan2(ry, rx), rx / length, ry / length)
            }
            if (entries.size < HUB_MIN_DEGREE) continue
            entries.sortWith(compareBy<SectorEntry> { it.angle }.thenBy { index.ids[it.index] })
            val desired = twoPi / entries.size
            for (k in entries.indices) {
                val current = entries[k]
                val next = entries[(k + 1) % entries.size]
                var sector = next.angle - current.angle
                if (sector <= 0f) sector += twoPi
                val shortage = ((desired - sector) / desired).coerceIn(0f, 1f)
                if (shortage == 0f) continue
                // Tangential pairwise correction preserves the radial topology.
                val strength = 10f * shortage
                fx[current.index] -= -current.uy * strength
                fy[current.index] -= current.ux * strength
                fx[next.index] += -next.uy * strength
                fy[next.index] += next.ux * strength
            }
        }
    }

    private fun separateOverlaps(xs: FloatArray, ys: FloatArray, fixed: BooleanArray, draggedIndex: Int) {
        val radius = index.radius
        repeat(2) {
            grid.rebuild(xs, ys, n)
            for (i in 0 until n) {
                grid.forEachNear(xs[i], ys[i]) { j ->
                    if (j <= i) return@forEachNear
                    val dx = xs[j] - xs[i]
                    val dy = ys[j] - ys[i]
                    val distance = sqrt(dx * dx + dy * dy)
                    val minimum = radius[i] + radius[j] + 14f
                    if (distance >= minimum) return@forEachNear
                    val ux: Float
                    val uy: Float
                    if (distance < 0.1f) {
                        val angle = (index.ids[i].hashCode() xor index.ids[j].hashCode()) * 0.017f
                        ux = kotlin.math.cos(angle)
                        uy = kotlin.math.sin(angle)
                    } else {
                        ux = dx / distance
                        uy = dy / distance
                    }
                    val penetration = minimum - distance.coerceAtLeast(0.1f)
                    val firstFixed = fixed[i] || i == draggedIndex
                    val secondFixed = fixed[j] || j == draggedIndex
                    when {
                        firstFixed && !secondFixed -> {
                            xs[j] += ux * penetration
                            ys[j] += uy * penetration
                        }
                        !firstFixed && secondFixed -> {
                            xs[i] -= ux * penetration
                            ys[i] -= uy * penetration
                        }
                        !firstFixed && !secondFixed -> {
                            val half = penetration / 2f
                            xs[i] -= ux * half
                            ys[i] -= uy * half
                            xs[j] += ux * half
                            ys[j] += uy * half
                        }
                    }
                }
            }
        }
    }

    private class SectorEntry(val index: Int, val angle: Float, val ux: Float, val uy: Float)
}

internal fun hubSpokeRestDistance(hubRadius: Float, neighborRadius: Float, degree: Int): Float {
    val nodeClearance = hubRadius + neighborRadius + 58f
    // Each additional spoke needs circumference for its neighboring labels and
    // for the straight edge to leave the hub without immediately crossing one.
    return max(nodeClearance, 170f + degree * 13f).coerceAtMost(460f)
}

/**
 * Finds the node under [world] (graph coordinates). Lower layout levels win,
 * then the closest node. Linear in node count with no allocation.
 */
internal fun hitTestNode(layout: GraphLayoutState, world: Offset, minTouchRadius: Float): Int {
    val index = layout.index
    var best = -1
    var bestLevel = Int.MAX_VALUE
    var bestDistance = Float.POSITIVE_INFINITY
    for (i in 0 until index.size) {
        val dx = layout.xs[i] - world.x
        val dy = layout.ys[i] - world.y
        val touch = max(index.radius[i], minTouchRadius)
        val distanceSquared = dx * dx + dy * dy
        if (distanceSquared >= touch * touch) continue
        val level = layout.levels[i]
        if (level < bestLevel || (level == bestLevel && distanceSquared < bestDistance)) {
            best = i
            bestLevel = level
            bestDistance = distanceSquared
        }
    }
    return best
}

/** Moves the dragged node and carries its connected branch with depth falloff. */
internal fun moveConnectedNeighborhood(
    layout: GraphLayoutState,
    movedIndex: Int,
    deltaX: Float,
    deltaY: Float,
) {
    val index = layout.index
    if (movedIndex !in 0 until index.size) return
    val xs = layout.xs
    val ys = layout.ys
    xs[movedIndex] += deltaX
    ys[movedIndex] += deltaY
    val visited = BooleanArray(index.size)
    visited[movedIndex] = true
    val queue = ArrayDeque<Long>()
    index.neighbors[movedIndex].forEach { queue.addLast(packNode(it, 1)) }
    while (queue.isNotEmpty()) {
        val packed = queue.removeFirst()
        val node = (packed ushr 32).toInt()
        val depth = packed.toInt()
        if (visited[node]) continue
        visited[node] = true
        if (index.isSeed[node] || index.isClusterAnchor[node]) continue
        val carry = 0.88f.pow((depth - 1).toFloat())
        xs[node] += deltaX * carry
        ys[node] += deltaY * carry
        index.neighbors[node].forEach { neighbor ->
            if (!visited[neighbor] && !index.isSeed[neighbor] && !index.isClusterAnchor[neighbor]) {
                queue.addLast(packNode(neighbor, depth + 1))
            }
        }
    }
}

private fun packNode(node: Int, depth: Int): Long = (node.toLong() shl 32) or (depth.toLong() and 0xFFFFFFFFL)

/** Graph-space rectangle visible in the viewport, inflated by [marginPx] screen pixels. */
internal fun visibleWorldRect(layoutSize: IntSize, scale: Float, offset: Offset, marginPx: Float): Rect {
    val safeScale = scale.coerceAtLeast(1e-6f)
    return Rect(
        left = (-offset.x - marginPx) / safeScale,
        top = (-offset.y - marginPx) / safeScale,
        right = (layoutSize.width - offset.x + marginPx) / safeScale,
        bottom = (layoutSize.height - offset.y + marginPx) / safeScale,
    )
}

internal data class ZoomLimits(val min: Float, val max: Float)

/**
 * Zoom limits relative to the "fit whole graph" scale: you can always zoom
 * out to a quarter of the overview and zoom in until nodes are 6× their
 * natural size, whatever the graph size.
 */
internal fun zoomLimitsFor(fittedScale: Float): ZoomLimits {
    val fit = fittedScale.coerceIn(1e-4f, 25f)
    return ZoomLimits(
        min = (fit * 0.25f).coerceIn(1e-5f, 0.25f),
        max = max(6f, fit * 6f),
    )
}

/**
 * Exponent applied to every zoom step. Large graphs have a long way to travel
 * between the overview and readable nodes, so each step covers more of it;
 * small graphs get finer steps.
 */
internal fun adaptiveZoomSpeed(fittedScale: Float): Float {
    val span = abs(ln(1f / fittedScale.coerceIn(1e-4f, 25f)))
    return (0.75f + span / 2.5f).coerceIn(0.75f, 3f)
}

internal fun adaptZoomFactor(factor: Float, speed: Float): Float =
    if (factor <= 0f) 1f else factor.pow(speed)

/** Mutable counters filled during drawing; copied to UI state periodically. */
internal class GraphRenderStats {
    var draws = 0
    var totalDrawNanos = 0L
    var maxDrawNanos = 0L
    var visibleNodes = 0
    var visibleEdges = 0
    var labels = 0
    var simulationNanos = 0L
    var simulationFrames = 0
    var layoutMillis = 0L
    var detail = ""

    fun recordDraw(nanos: Long) {
        draws++
        totalDrawNanos += nanos
        if (nanos > maxDrawNanos) maxDrawNanos = nanos
    }

    fun resetWindow() {
        draws = 0
        totalDrawNanos = 0L
        maxDrawNanos = 0L
        simulationNanos = 0L
        simulationFrames = 0
    }
}
