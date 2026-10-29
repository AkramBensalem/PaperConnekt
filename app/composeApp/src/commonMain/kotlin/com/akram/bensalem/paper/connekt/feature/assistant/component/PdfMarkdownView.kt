package com.akram.bensalem.paper.connekt.feature.assistant.component

import com.paperconnekt.app.resources.Res
import com.paperconnekt.app.resources.filled_article
import com.paperconnekt.app.resources.filled_error_outline
import com.paperconnekt.app.resources.filled_headphones
import com.paperconnekt.app.resources.filled_key
import com.paperconnekt.app.resources.filled_refresh
import org.jetbrains.compose.resources.painterResource
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.input.pointer.PointerEventType
import com.akram.bensalem.bistosh.core.designsystem.component.observePointerEvent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Constraints
import com.akram.bensalem.bistosh.core.designsystem.component.interactivePointer
import com.akram.bensalem.paper.connekt.feature.assistant.*
import com.mikepenz.markdown.coil3.Coil3ImageTransformerImpl
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.model.rememberMarkdownState
import kotlinx.coroutines.flow.drop
import kotlin.math.roundToInt

/** Reader body that shows the saved Markdown version of the open PDF. */
@Composable
internal fun PdfMarkdownView(
    viewModel: PdfWorkspaceViewModel,
    modifier: Modifier = Modifier,
    continuous: Boolean = false,
    spread: Boolean = false,
    fitPage: Boolean = false,
    readingActive: Boolean = false,
    readAloud: ReadAloudController? = null,
) {
    val markdown = viewModel.state.markdown
    Surface(modifier.testTag("pdf-markdown-view"), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(Modifier.fillMaxSize()) {
            val content = markdown.content
            if (content != null && markdown.syncError != null) {
                Text(
                    markdown.syncError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                )
                TextButton(onClick = viewModel::retryMarkdown, enabled = !markdown.busy,
                    modifier = Modifier.interactivePointer()) {
                    Text("Retry OCR sync")
                }
            }
            if (content != null && !markdown.hasPageBoundaries) {
                Text("This older saved copy has no page boundaries. Use Convert again to restore the PDF pages.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth().padding(12.dp))
            }
            when {
                content != null -> MarkdownPages(viewModel, continuous, spread, fitPage, readingActive, readAloud)
                markdown.busy -> MarkdownStatus(
                    title = if (markdown.converting) "Creating Markdown with $MISTRAL_OCR_MODEL_NAME…" else "Opening saved Markdown…",
                    body = if (markdown.converting) "Long papers can take a minute. The result is saved, so this only happens once." else null,
                ) { CircularProgressIndicator(Modifier.size(32.dp)) }
                markdown.error != null -> MarkdownStatus(
                    title = if (markdown.missingApiKey) "Mistral API key needed" else "Markdown version unavailable",
                    body = markdown.error,
                    icon = { Icon(if (markdown.missingApiKey) painterResource(Res.drawable.filled_key) else painterResource(Res.drawable.filled_error_outline), null,
                        Modifier.size(32.dp), tint = MaterialTheme.colorScheme.error) },
                ) {
                    FilledTonalButton(onClick = viewModel::retryMarkdown, modifier = Modifier.interactivePointer()) {
                        Icon(painterResource(Res.drawable.filled_refresh), null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Try again")
                    }
                }
                else -> MarkdownStatus(
                    title = "No Markdown version yet",
                    icon = { Icon(painterResource(Res.drawable.filled_article), null, Modifier.size(32.dp)) },
                ) {
                    FilledTonalButton(onClick = viewModel::retryMarkdown, modifier = Modifier.interactivePointer()) { Text("Create Markdown version") }
                }
            }
        }
    }
}

@Composable
private fun MarkdownPages(
    viewModel: PdfWorkspaceViewModel,
    continuous: Boolean,
    spread: Boolean,
    fitPage: Boolean,
    readingActive: Boolean,
    readAloud: ReadAloudController?,
) {
    val state = viewModel.state
    val pages = state.markdown.pages
    val columns = if (spread) 2 else 1
    val listState = rememberLazyListState()
    var following by remember { mutableStateOf(false) }
    val current by rememberUpdatedState(state)
    val density = LocalDensity.current

    LaunchedEffect(state.target, continuous, columns, pages, state.pageNavigationId) {
        if (continuous && pages.isNotEmpty()) {
            following = true
            try {
                val row = state.pageIndex.coerceIn(pages.indices) / columns
                listState.scrollToItem(row)
                withFrameNanos { }
                val height = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == row }?.size ?: 0
                listState.scrollToItem(row, (height * state.readingPosition.fraction).roundToInt())
            } finally { following = false }
        }
    }
    LaunchedEffect(listState, continuous, columns) {
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo.firstOrNull()?.let { Triple(it.index, it.offset, it.size) }
        }.collect { item ->
            if (continuous && !following && item != null) {
                val first = item.first * columns
                viewModel.trackReadingPosition(PdfReadingPosition(
                    if (current.pageIndex in first until first + columns) current.pageIndex else first,
                    (-item.second.toFloat() / item.third.coerceAtLeast(1)).coerceIn(0f, 1f),
                ), PdfScrollSource.Pdf)
            }
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val baseWidth = if (spread) ((maxWidth - 56.dp) / 2).coerceAtLeast(280.dp)
            else (maxWidth - 48.dp).coerceAtLeast(100.dp)
        val pageWidth = baseWidth * state.zoom
        val rowWidth = maxOf(maxWidth, pageWidth * columns + (if (spread) 8.dp else 0.dp) + 48.dp)
        val viewportHeight = with(density) { (maxHeight - 112.dp).coerceAtLeast(1.dp).roundToPx() }
        val horizontal = rememberScrollState()
        val vertical = rememberScrollState()
        val zoomTarget = rememberReaderZoomTarget(horizontal,
            contentWidth = pageWidth * columns + (if (spread) 8.dp else 0.dp),
            vertical = vertical.takeUnless { continuous }, list = listState.takeIf { continuous },
            verticalPadding = if (continuous) 6.dp else 24.dp)
        Box(Modifier.fillMaxSize().then(zoomTarget).horizontalScroll(horizontal)) {
            if (continuous) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.width(rowWidth).fillMaxHeight().testTag("markdown-continuous-pages"),
                    contentPadding = PaddingValues(top = 6.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items((pages.size + columns - 1) / columns, key = { it }) { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                            for (index in row * columns until minOf((row + 1) * columns, pages.size)) {
                                MarkdownPage(
                                    pages[index], index, baseWidth, state.zoom, aspect(state, index),
                                    readingActive = readingActive, readAloud = readAloud,
                                    rootId = state.target?.attachment?.key ?: "pdf",
                                )
                            }
                        }
                    }
                }
            } else {
                val first = state.pageIndex.coerceIn(pages.indices) / columns * columns
                LaunchedEffect(first, columns, state.pageNavigationId) {
                    following = true
                    try {
                        withFrameNanos { }
                        val height = vertical.maxValue + viewportHeight
                        vertical.scrollTo((height * state.readingPosition.fraction).roundToInt())
                    } finally { following = false }
                }
                LaunchedEffect(vertical, first, columns) {
                    snapshotFlow { vertical.value }.drop(1).collect { offset ->
                        if (!following) {
                            viewModel.trackReadingPosition(PdfReadingPosition(current.pageIndex,
                                offset.toFloat() / (vertical.maxValue + viewportHeight).coerceAtLeast(1)
                            ), PdfScrollSource.Pdf)
                        }
                    }
                }
                Row(
                    Modifier.width(rowWidth).fillMaxHeight().verticalScroll(vertical).padding(top = 24.dp, bottom = 80.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                ) {
                    for (index in first until minOf(first + columns, pages.size)) {
                        MarkdownPage(
                            pages[index], index, baseWidth, state.zoom, aspect(state, index),
                            fitHeightPixels = if (fitPage && !spread) viewportHeight else null,
                            readingActive = readingActive, readAloud = readAloud,
                            rootId = state.target?.attachment?.key ?: "pdf",
                        )
                    }
                }
            }
        }
    }
}

private fun aspect(state: PdfWorkspaceState, index: Int): Float =
    (state.pageGeometries[index] ?: state.page?.geometry)?.let { it.displayHeight / it.displayWidth } ?: 1.414f

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun MarkdownPage(
    content: String, index: Int, width: androidx.compose.ui.unit.Dp, zoom: Float, aspect: Float,
    fitHeightPixels: Int? = null,
    readingActive: Boolean = false,
    readAloud: ReadAloudController? = null,
    rootId: String = "pdf",
) {
    val blocks = remember(content) {
        content.split(Regex("""\n\s*\n"""))
            .map(String::trim)
            .filter(String::isNotBlank)
    }
    Layout(
        modifier = Modifier.testTag("markdown-page-$index"),
        content = {
            Box(
                Modifier.fillMaxWidth().heightIn(min = width * aspect)
            ) {
                Surface(Modifier.fillMaxSize(), shadowElevation = 1.dp) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 40.dp, vertical = 32.dp)) {
                        blocks.forEachIndexed { blockIndex, block ->
                            var hovered by remember(blockIndex, block) { mutableStateOf(false) }
                            val plan = remember(block) { createSpeechPlan(block) }
                            Box(
                                Modifier.fillMaxWidth()
                                    .observePointerEvent(PointerEventType.Enter) { hovered = true }
                                    .observePointerEvent(PointerEventType.Exit) { hovered = false },
                            ) {
                                if (readingActive) {
                                    Markdown(
                                        rememberMarkdownState(block),
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                        imageTransformer = Coil3ImageTransformerImpl,
                                    )
                                } else {
                                    SelectionContainer {
                                        Markdown(
                                            rememberMarkdownState(block),
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                            imageTransformer = Coil3ImageTransformerImpl,
                                        )
                                    }
                                }
                                if (hovered && readAloud != null && !readingActive && plan.segments.any { !it.skipped }) {
                                    IconButton(
                                        onClick = {
                                            readAloud.start(
                                                plan = plan,
                                                rootId = rootId,
                                                exampleId = "markdown-page-$index-block-$blockIndex",
                                            )
                                        },
                                        modifier = Modifier.align(Alignment.TopStart).size(28.dp)
                                            .interactivePointer(),
                                    ) {
                                        Icon(
                                            painterResource(Res.drawable.filled_headphones),
                                            "Read this paragraph aloud",
                                            tint = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
    ) { measurables, _ ->
        val nominalWidth = width.roundToPx()
        val page = measurables.single().measure(Constraints.fixedWidth(nominalWidth))
        val scale = if (fitHeightPixels != null) minOf(zoom, fitHeightPixels.toFloat() / page.height.coerceAtLeast(1)) else zoom
        layout((page.width * scale).roundToInt(), (page.height * scale).roundToInt()) {
            page.placeWithLayer(0, 0) {
                scaleX = scale
                scaleY = scale
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0f)
            }
        }
    }
}

@Composable
private fun MarkdownStatus(
    title: String,
    body: String? = null,
    icon: (@Composable () -> Unit)? = null,
    action: @Composable () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().padding(32.dp).semantics { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        icon?.invoke()
        if (icon == null) action()
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        body?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 420.dp))
        }
        if (icon != null) action()
    }
}
