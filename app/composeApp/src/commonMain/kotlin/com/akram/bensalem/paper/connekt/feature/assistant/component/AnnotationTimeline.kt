package com.akram.bensalem.paper.connekt.feature.assistant.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.akram.bensalem.paper.connekt.feature.assistant.*
import com.akram.bensalem.paper.connekt.shared.zotero.model.zotero.ZoteroAnnotation
import kotlinx.coroutines.flow.first

@Composable
internal fun AnnotationTimeline(
    viewModel: PdfWorkspaceViewModel,
    annotations: List<ZoteroAnnotation>,
    currentPageOnly: Boolean,
    modifier: Modifier,
    card: @Composable (ZoteroAnnotation, Int) -> Unit
) {
    val state = viewModel.state
    val pages = if (currentPageOnly) listOf(state.pageIndex) else (0 until state.pageCount).toList()
    val listState = rememberLazyListState()
    val tracks = remember(state.target, annotations) { mutableStateMapOf<Int, AnnotationPageTrack>() }
    val orderedKeys = remember(state.target, annotations) { mutableStateMapOf<Int, List<String>>() }
    val density = LocalDensity.current
    var following by remember { mutableStateOf(false) }
    if (annotations.isEmpty()) return

    LaunchedEffect(state.readingPosition, state.selectedKey, state.pageNavigationId, pages, annotations) {
        if (state.scrollSource == PdfScrollSource.Sidebar) return@LaunchedEffect
        val position = state.readingPosition
        val index = pages.indexOf(position.pageIndex)
        if (index < 0 || listState.isScrollInProgress && !following) return@LaunchedEffect
        following = true
        try {
            withFrameNanos { }
            if (tracks[position.pageIndex] == null) listState.scrollToItem(index)
            val track = snapshotFlow { tracks[position.pageIndex] }.first { it != null }!!
            // Track geometry is published during layout; scrolling must not re-enter measurement.
            withFrameNanos { }
            val selectedIndex = if (state.scrollSource == PdfScrollSource.Navigation)
                orderedKeys[position.pageIndex].orEmpty().indexOf(state.selectedKey) else -1
            val offset = if (selectedIndex >= 0) track.offsets[selectedIndex] - with(density) { 12.dp.roundToPx() }
                else track.offsetAt(position.fraction)
            listState.scrollToItem(index, offset.coerceAtLeast(0))
        } finally { following = false }
    }
    val scrollConnection = remember(listState, pages, tracks) {
        object : NestedScrollConnection {
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (following || consumed.y == 0f) return Offset.Zero
                listState.layoutInfo.visibleItemsInfo.firstOrNull()?.let { item ->
                    pages.getOrNull(item.index)?.let { pageIndex ->
                        tracks[pageIndex]?.let { viewModel.navigateToPosition(PdfReadingPosition(pageIndex, it.fractionAt(-item.offset))) }
                    }
                }
                return Offset.Zero
            }
        }
    }
    BoxWithConstraints(modifier) {
        val viewportHeight = maxHeight
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().nestedScroll(scrollConnection).testTag("annotation-timeline"),
            userScrollEnabled = state.draft == null,
            contentPadding = PaddingValues(bottom = viewportHeight)
        ) {
            items(pages, key = { it }) { pageIndex ->
                val geometry = state.pageGeometries[pageIndex]
                LaunchedEffect(state.target, pageIndex, geometry) {
                    if (geometry == null) viewModel.loadContinuousPage(pageIndex, 300)
                }
                val positioned = annotations.mapNotNull { annotation ->
                    val rects = when (pageIndex) {
                        annotation.position.pageIndex -> annotation.position.boundingRects
                        annotation.position.pageIndex + 1 -> annotation.position.nextPageRects
                        else -> emptyList()
                    }
                    if (rects.isEmpty() || geometry == null) null else {
                        val fraction = rects.minOf { geometry.displayRect(it).top / geometry.displayHeight }.coerceIn(0f, 1f)
                        annotation to fraction
                    }
                }.sortedWith(compareBy({ it.second }, { it.first.key }))
                Layout(
                    content = {
                        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                                Text("Page ${pageIndex + 1}", style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                            }
                            HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                        }
                        positioned.forEach { (annotation, _) -> card(annotation, pageIndex) }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("annotation-page-$pageIndex")
                ) { measurables, constraints ->
                    val childConstraints = Constraints(maxWidth = constraints.maxWidth)
                    val children = measurables.map { it.measure(childConstraints) }
                    val header = children.first().height
                    val track = annotationPageTrack(
                        positioned.map { it.second }, children.drop(1).map { it.height },
                        (state.displayedPageWidth * (geometry?.let { it.displayHeight / it.displayWidth } ?: 1.414f)).dp.roundToPx(),
                        header, 8.dp.roundToPx()
                    )
                    if (geometry != null && tracks[pageIndex] != track) tracks[pageIndex] = track
                    val keys = positioned.map { it.first.key }
                    if (orderedKeys[pageIndex] != keys) orderedKeys[pageIndex] = keys
                    layout(constraints.maxWidth, track.height) {
                        children.first().placeRelative(0, 0)
                        children.drop(1).forEachIndexed { index, child -> child.placeRelative(0, track.offsets[index]) }
                    }
                }
            }
        }
    }
}
