package com.akram.bensalem.paper.connekt.feature.assistant.component

import com.paperconnekt.app.resources.Res
import com.paperconnekt.app.resources.filled_add
import com.paperconnekt.app.resources.filled_arrow_back
import com.paperconnekt.app.resources.filled_article
import com.paperconnekt.app.resources.filled_auto_awesome
import com.paperconnekt.app.resources.filled_auto_fix_high
import com.paperconnekt.app.resources.filled_auto_stories
import com.paperconnekt.app.resources.filled_chat_bubble_outline
import com.paperconnekt.app.resources.filled_check
import com.paperconnekt.app.resources.filled_check_circle
import com.paperconnekt.app.resources.filled_chevron_left
import com.paperconnekt.app.resources.filled_chevron_right
import com.paperconnekt.app.resources.filled_circle
import com.paperconnekt.app.resources.filled_close
import com.paperconnekt.app.resources.filled_cloud_done
import com.paperconnekt.app.resources.filled_code
import com.paperconnekt.app.resources.filled_content_copy
import com.paperconnekt.app.resources.filled_crop_square
import com.paperconnekt.app.resources.filled_delete_outline
import com.paperconnekt.app.resources.filled_description
import com.paperconnekt.app.resources.filled_edit
import com.paperconnekt.app.resources.filled_edit_note
import com.paperconnekt.app.resources.filled_error
import com.paperconnekt.app.resources.filled_error_outline
import com.paperconnekt.app.resources.filled_fit_screen
import com.paperconnekt.app.resources.filled_format_bold
import com.paperconnekt.app.resources.filled_format_italic
import com.paperconnekt.app.resources.filled_format_list_bulleted
import com.paperconnekt.app.resources.filled_format_list_numbered
import com.paperconnekt.app.resources.filled_format_quote
import com.paperconnekt.app.resources.filled_format_size
import com.paperconnekt.app.resources.filled_format_underlined
import com.paperconnekt.app.resources.filled_fullscreen
import com.paperconnekt.app.resources.filled_fullscreen_exit
import com.paperconnekt.app.resources.filled_gesture
import com.paperconnekt.app.resources.filled_highlight
import com.paperconnekt.app.resources.filled_keyboard_arrow_down
import com.paperconnekt.app.resources.filled_keyboard_arrow_up
import com.paperconnekt.app.resources.filled_lightbulb
import com.paperconnekt.app.resources.filled_line_weight
import com.paperconnekt.app.resources.filled_link
import com.paperconnekt.app.resources.filled_menu_book
import com.paperconnekt.app.resources.filled_more_vert
import com.paperconnekt.app.resources.filled_note_add
import com.paperconnekt.app.resources.filled_notes
import com.paperconnekt.app.resources.filled_pan_tool
import com.paperconnekt.app.resources.filled_picture_as_pdf
import com.paperconnekt.app.resources.filled_record_voice_over
import com.paperconnekt.app.resources.filled_refresh
import com.paperconnekt.app.resources.filled_remove
import com.paperconnekt.app.resources.filled_search
import com.paperconnekt.app.resources.filled_spellcheck
import com.paperconnekt.app.resources.filled_sync
import com.paperconnekt.app.resources.filled_text_fields
import com.paperconnekt.app.resources.filled_title
import com.paperconnekt.app.resources.filled_tune
import com.paperconnekt.app.resources.filled_view_day
import com.paperconnekt.app.resources.filled_zoom_in
import com.paperconnekt.app.resources.filled_zoom_out
import org.jetbrains.compose.resources.painterResource
import androidx.compose.runtime.withFrameNanos
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.PointerEventType
import com.akram.bensalem.bistosh.core.designsystem.component.observePointerEvent
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.ui.zIndex
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import com.akram.bensalem.paper.connekt.feature.assistant.*
import com.akram.bensalem.bistosh.core.designsystem.component.clipChipInteraction
import com.akram.bensalem.bistosh.core.designsystem.component.interactivePointer
import com.akram.bensalem.paper.connekt.shared.zotero.model.zotero.ZoteroAnnotation
import dev.nucleusframework.pdfium.PdfPage as NucleusPdfPage
import dev.nucleusframework.pdfium.PdfReaderState
import dev.nucleusframework.pdfium.rememberPdfReaderState
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

internal val annotationColors = listOf("#ffd400", "#ff6666", "#5fb236", "#2ea8e5", "#a28ae5")
internal val annotationColorNames = listOf("Yellow", "Red", "Green", "Blue", "Purple")

private val LocalNucleusPdfReaderState = compositionLocalOf<PdfReaderState?> { null }
internal fun annotationColor(hex: String): Color = Color(0xff000000L or hex.removePrefix("#").toLong(16))
private fun TextFieldValue.wrapSelection(prefix: String, suffix: String): TextFieldValue {
    val start = selection.min
    val end = selection.max
    val selected = text.substring(start, end)
    val replacement = "$prefix$selected$suffix"
    val updated = text.replaceRange(start, end, replacement)
    val updatedSelection = if (start == end) {
        TextRange(start + prefix.length)
    } else {
        TextRange(start + prefix.length, start + prefix.length + selected.length)
    }
    return copy(text = updated, selection = updatedSelection)
}
private fun TextFieldValue.prefixSelectedLines(prefix: String): TextFieldValue {
    val selectedStart = selection.min
    val selectedEnd = selection.max
    val lineStart = if (selectedStart == 0) 0 else text.lastIndexOf('\n', selectedStart - 1) + 1
    val endsAtNextLineStart = selectedEnd > lineStart && text.getOrNull(selectedEnd - 1) == '\n'
    val lineEnd = if (endsAtNextLineStart) {
        selectedEnd - 1
    } else {
        text.indexOf('\n', selectedEnd).let { if (it == -1) text.length else it }
    }
    val lines = text.substring(lineStart, lineEnd).split('\n')
    val replacement = lines.joinToString("\n") { "$prefix$it" }
    val updated = text.replaceRange(lineStart, lineEnd, replacement)
    val updatedSelection = if (selectedStart == selectedEnd) {
        TextRange(selectedStart + prefix.length)
    } else {
        val addedPrefixesBeforeEnd =
            text.substring(lineStart, minOf(selectedEnd, lineEnd)).count { it == '\n' } * prefix.length
        TextRange(selectedStart + prefix.length, selectedEnd + addedPrefixesBeforeEnd)
    }
    return copy(text = updated, selection = updatedSelection)
}
internal fun formatSize(value: Float) = (if (value % 1f == 0f) value.toInt().toString() else value.toString()) + " pt"
private data class AnnotationMove(val key: String, val from: Rect, val to: Rect)
private fun interpolateRect(from: Rect, to: Rect, fraction: Float): Rect = Rect(
    left = from.left + (to.left - from.left) * fraction,
    top = from.top + (to.top - from.top) * fraction,
    right = from.right + (to.right - from.right) * fraction,
    bottom = from.bottom + (to.bottom - from.bottom) * fraction,
)

private fun annotationTypeLabel(annotation: ZoteroAnnotation) = when {
    annotation.isIdea -> "idea"
    annotation.type == "image" -> "rectangle"
    annotation.type == "ink" -> "drawing"
    else -> annotation.type
}
private fun annotationMarkerColor(hex: String): Color {
    val color = annotationColor(hex)
    return if (color.luminance() > 0.45f) lerp(color, Color.Black, 0.35f) else color
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ZoteroPdfReader(
    viewModel: PdfWorkspaceViewModel,
    focusMode: Boolean = false,
    onExit: (() -> Unit)? = null,
    onReadingPositionChanged: ((PdfReadingPosition, Float) -> Unit)? = null,
    supportingContent: (@Composable (allowEditing: Boolean) -> Unit)? = null,
    onInspectAnnotation: () -> Unit = {},
    onAskAi: (() -> Unit)? = null,
    paperAgent: PaperAgentViewModel? = null,
    onFocusModeChange: (Boolean) -> Unit = {},
    studyTimeToday: Long = 0L,
    totalStudyTime: Long = 0L
) {
    val state = viewModel.state
    val nucleusPdfState = rememberPdfReaderState()
    val loadedPdfBytes = viewModel.loadedPdfBytes()
    LaunchedEffect(loadedPdfBytes) {
        loadedPdfBytes?.let {
            nucleusPdfState.open(it)
            viewModel.attachPageProvider(nucleusPdfState.pageCount) { index ->
                nucleusPdfState.loadPaperConnektPage(index)
            }
        }
    }
    LaunchedEffect(state.target, state.localTitle, state.readingPosition, state.zoom) {
        onReadingPositionChanged?.invoke(state.readingPosition, state.zoom)
    }
    val pdfMode = state.viewMode == ReaderViewMode.Pdf
    val hasDocument = state.target != null || state.localTitle != null
    val hasMarkdownStorage = viewModel.paperStorageKey() != null
    var tool by remember(state.target) { mutableStateOf("select") }
    var showAnnotations by remember(state.target) { mutableStateOf(false) }
    var sidebarVisible by remember { mutableStateOf(true) }
    var sidebarWidth by remember(state.target) { mutableStateOf(340.dp) }
    var showSearch by remember(state.target) { mutableStateOf(false) }
    var showPagePicker by remember { mutableStateOf(false) }
    var fitPage by remember { mutableStateOf(false) }
    val zoomAnchor = remember { ReaderZoomAnchor() }
    var search by remember(state.target) { mutableStateOf("") }
    var matchCase by remember(state.target) { mutableStateOf(false) }
    var wholeWord by remember(state.target) { mutableStateOf(false) }
    val searchIndex = remember(state.page?.glyphs, state.page?.geometry, state.pageIndex) {
        state.page?.let { PdfPageText(state.pageIndex, it.geometry, it.glyphs).index() }
    }
    val searchMatches = remember(searchIndex, search, matchCase, wholeWord) {
        searchIndex?.findExact(search, matchCase, wholeWord).orEmpty()
    }
    var activeSearchIndex by remember(state.target, state.pageIndex, search, matchCase, wholeWord) { mutableIntStateOf(0) }
    var searchNavigationId by remember(state.target) { mutableIntStateOf(0) }
    var appliedSearchNavigationId by remember(state.target) { mutableIntStateOf(-1) }
    var searchPageNavigationId by remember(state.target) { mutableIntStateOf(-1) }
    val activeSearchMatch = searchMatches.getOrNull(activeSearchIndex)
    val navigateSearch: (Int) -> Unit = { direction ->
        if (searchMatches.isNotEmpty()) {
            activeSearchIndex = (activeSearchIndex + direction + searchMatches.size) % searchMatches.size
            searchNavigationId++
        }
    }
    LaunchedEffect(pdfMode) {
        if (!pdfMode) {
            search = ""
            showSearch = false
        }
    }
    LaunchedEffect(searchNavigationId, searchIndex, pdfMode) {
        if (!pdfMode || searchIndex == null || appliedSearchNavigationId == searchNavigationId) return@LaunchedEffect
        appliedSearchNavigationId = searchNavigationId
        activeSearchMatch?.let { match ->
            val height = state.page?.geometry?.displayHeight ?: return@let
            val top = match.selection.rects.minOf { it.top }
            viewModel.navigateToPosition(PdfReadingPosition(match.pageIndex, (top / height - 0.12f).coerceIn(0f, 1f)))
            searchPageNavigationId = viewModel.state.pageNavigationId
        }
    }
    val inkColor = state.annotationColor
    var showPenColors by remember { mutableStateOf(false) }
    var continuous by remember(state.target) { mutableStateOf(false) }
    var spread by remember(state.target) { mutableStateOf(false) }
    var showSizes by remember { mutableStateOf(false) }
    var showAnnotateSheet by remember { mutableStateOf(false) }
    var showViewSheet by remember { mutableStateOf(false) }
    var headerCollapsed by remember(state.target) { mutableStateOf(false) }
    val headerScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (state.draft == null) {
                    if (available.y < -15f) headerCollapsed = true
                    else if (available.y > 15f) headerCollapsed = false
                }
                return Offset.Zero
            }
        }
    }
    val clipboard = LocalClipboardManager.current
    val focusRequester = remember { FocusRequester() }
    val searchFocusRequester = remember { FocusRequester() }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val closeSearch: () -> Unit = {
        search = ""
        showSearch = false
        focusRequester.requestFocus()
    }
    val editingLocked = false
    val copySelection: () -> Unit = {
        state.textSelection?.let {
            clipboard.setText(AnnotatedString(it.text))
            scope.launch { snackbar.showSnackbar("Selected text copied") }
        }
    }
    LaunchedEffect(state.message) {
        state.message?.let { snackbar.showSnackbar(it); viewModel.dismissMessage() }
    }
    BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding().focusRequester(focusRequester).onPreviewKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown || state.draft != null) false
        else when {
            !showSearch && (event.isCtrlPressed || event.isMetaPressed) && event.key == Key.C && state.textSelection != null -> { copySelection(); true }
            pdfMode && !showSearch && (event.isCtrlPressed || event.isMetaPressed) && event.key == Key.A -> { viewModel.selectAllText(); true }
            pdfMode && (event.isCtrlPressed || event.isMetaPressed) && event.key == Key.F -> {
                if (showSearch) searchFocusRequester.requestFocus() else showSearch = true
                true
            }
            pdfMode && search.isNotBlank() && event.key == Key.F3 -> { navigateSearch(if (event.isShiftPressed) -1 else 1); true }
            showSearch && event.key == Key.Enter -> { navigateSearch(if (event.isShiftPressed) -1 else 1); true }
            showSearch && event.key == Key.Escape -> { closeSearch(); true }
            event.key == Key.Escape -> {
                if (state.adjustingTextRange) viewModel.finishTextRange() else viewModel.clearTextSelection()
                showSearch = false; true
            }
            event.key == Key.PageDown -> { viewModel.goToPage(state.pageIndex + 1); true }
            event.key == Key.PageUp -> { viewModel.goToPage(state.pageIndex - 1); true }
            else -> false
        }
    }.focusable()) {
        val wide = maxWidth >= 840.dp && maxHeight >= 480.dp
        val compact = maxWidth < CompactReaderMaxWidth
        val density = LocalDensity.current
        val readerWidth = maxWidth
        CompositionLocalProvider(LocalNucleusPdfReaderState provides nucleusPdfState) {
        Row(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).fillMaxHeight()) {
            if (!focusMode) Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 3.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(Modifier.fillMaxWidth().padding(horizontal = if (compact) 4.dp else 12.dp, vertical = if (compact) 2.dp else 8.dp)) {
                    if (compact) CompactReaderTopBar(
                        title = state.localTitle ?: state.target?.item?.data?.title ?: "PDF workspace",
                        state = state,
                        onExit = onExit,
                        showSearchAction = pdfMode,
                        searchActive = showSearch || search.isNotBlank(),
                        searchCount = searchMatches.size.takeIf { search.isNotBlank() },
                        onSearch = { if (showSearch) closeSearch() else showSearch = true },
                        onAnnotations = { showAnnotations = true },
                        focusMode = focusMode,
                        onFocusMode = { onFocusModeChange(!focusMode) },
                        toolsCollapsible = pdfMode && state.draft == null,
                        toolsCollapsed = headerCollapsed,
                        onToggleTools = { headerCollapsed = !headerCollapsed },
                        onSync = viewModel::refreshAnnotations,
                        onGoToPage = if (state.readerPageCount > 0 && state.draft == null) ({ showPagePicker = true }) else null,
                        studyTimeToday = studyTimeToday,
                        totalStudyTime = totalStudyTime,
                    ) else Row(verticalAlignment = Alignment.CenterVertically) {
                        onExit?.let { exit ->
                            FilledTonalIconButton(
                                onClick = exit,
                                shape = RoundedCornerShape(10.dp),
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier.padding(end = 6.dp).size(36.dp).interactivePointer()
                            ) {
                                Icon(painterResource(Res.drawable.filled_arrow_back), "Exit workspace", Modifier.size(18.dp))
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(painterResource(Res.drawable.filled_picture_as_pdf), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                        }
                        Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                            Text(state.localTitle ?: state.target?.item?.data?.title ?: "PDF workspace",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            StudyTimeIndicator(studyTimeToday, totalStudyTime)
                        }
                        if (pdfMode) {
                            FilledTonalIconButton(
                                onClick = { if (showSearch) closeSearch() else showSearch = true },
                                shape = RoundedCornerShape(10.dp),
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = if (showSearch || search.isNotBlank()) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
                                    contentColor = if (showSearch || search.isNotBlank()) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.padding(horizontal = 2.dp).size(36.dp).interactivePointer()
                            ) {
                                BadgedBox(badge = { if (search.isNotBlank()) Badge { Text(searchMatches.size.toString()) } }) {
                                    Icon(painterResource(Res.drawable.filled_search), "Find text on this page", Modifier.size(18.dp))
                                }
                            }
                        }
                        FilledTonalIconButton(
                            onClick = { onFocusModeChange(!focusMode) },
                            shape = RoundedCornerShape(10.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.padding(horizontal = 2.dp).size(36.dp).interactivePointer()
                        ) {
                            Icon(if (focusMode) painterResource(Res.drawable.filled_fullscreen_exit) else painterResource(Res.drawable.filled_fullscreen),
                                if (focusMode) "Exit focus mode" else "Focus mode", Modifier.size(18.dp))
                        }
                        FilledTonalIconButton(
                            onClick = {
                                if (wide) {
                                    if (focusMode) onFocusModeChange(false)
                                    sidebarVisible = if (focusMode) true else !sidebarVisible
                                } else showAnnotations = true
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = if (sidebarVisible && !focusMode && wide) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
                                contentColor = if (sidebarVisible && !focusMode && wide) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.padding(horizontal = 2.dp).size(36.dp).interactivePointer()
                        ) {
                            BadgedBox(badge = { if (state.annotations.isNotEmpty()) Badge { Text(state.annotations.size.toString()) } }) {
                                Icon(painterResource(Res.drawable.filled_notes),
                                    "Annotations (${state.annotations.size})", Modifier.size(18.dp))
                            }
                        }
                        if (pdfMode) {
                            FilledTonalIconButton(
                                onClick = { headerCollapsed = !headerCollapsed },
                                shape = RoundedCornerShape(10.dp),
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.45f),
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.padding(horizontal = 2.dp).size(36.dp).interactivePointer()
                            ) {
                                Icon(
                                    if (headerCollapsed && state.draft == null) painterResource(Res.drawable.filled_keyboard_arrow_down) else painterResource(Res.drawable.filled_keyboard_arrow_up),
                                    if (headerCollapsed && state.draft == null) "Expand tools" else "Collapse tools",
                                    Modifier.size(18.dp)
                                )
                            }
                        }
                        Box(Modifier.padding(start = 2.dp)) {
                            ZoteroSyncIndicator(state, viewModel::refreshAnnotations)
                        }
                    }
                    AnimatedVisibility(
                        visible = pdfMode && !focusMode && (!headerCollapsed || state.draft != null),
                        enter = expandVertically(animationSpec = tween(220, easing = FastOutSlowInEasing)) + fadeIn(tween(180)),
                        exit = shrinkVertically(animationSpec = tween(200, easing = FastOutSlowInEasing)) + fadeOut(tween(150))
                    ) {
                        Column {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = if (compact) 2.dp else 6.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                            )
                            if (compact) CompactReaderToolRow(
                                tool = tool,
                                onToolChange = { id -> tool = id; if (id != "select") viewModel.clearTextSelection() },
                                hasDraft = state.draft != null,
                                inkDraft = state.draft?.type == "ink",
                                canSaveDrawing = state.canWrite && state.draft?.paths?.isNotEmpty() == true,
                                annotateEnabled = state.canWrite && !editingLocked && (state.draft == null || state.draft.type == "ink"),
                                editingLocked = editingLocked,
                                colorHex = inkColor,
                                onOpenAnnotate = { showAnnotateSheet = true },
                                onSaveDrawing = viewModel::saveDraft,
                                onDiscardDrawing = viewModel::cancelDraft,
                            ) else Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SingleChoiceSegmentedButtonRow(Modifier.padding(end = 4.dp)) {
                            SegmentedButton(selected = tool == "select", onClick = { tool = "select" },
                                shape = SegmentedButtonDefaults.itemShape(0, 2), enabled = state.draft == null,
                                modifier = Modifier.interactivePointer(enabled = state.draft == null),
                                icon = {
                                    SegmentedButtonDefaults.Icon(active = tool == "select") {
                                        Icon(painterResource(Res.drawable.filled_text_fields), null, Modifier.size(18.dp))
                                    }
                                }) { Text("Select") }
                            SegmentedButton(selected = tool == "pan", onClick = { tool = "pan"; viewModel.clearTextSelection() },
                                shape = SegmentedButtonDefaults.itemShape(1, 2), enabled = state.draft == null,
                                modifier = Modifier.interactivePointer(enabled = state.draft == null),
                                icon = {
                                    SegmentedButtonDefaults.Icon(active = tool == "pan") {
                                        Icon(painterResource(Res.drawable.filled_pan_tool), null, Modifier.size(18.dp))
                                    }
                                }) { Text("Pan") }
                        }
                        ReaderToolChip(tool == "highlight", { tool = if (tool == "highlight") "select" else "highlight"; viewModel.clearTextSelection() },
                            enabled = state.canWrite && state.draft == null && !editingLocked, label = { Text("Highlight") },
                            leadingIcon = if (wide) { { Icon(painterResource(Res.drawable.filled_highlight), null, Modifier.size(18.dp)) } } else null)
                        ReaderToolChip(tool == "underline", { tool = if (tool == "underline") "select" else "underline"; viewModel.clearTextSelection() },
                            enabled = state.canWrite && state.draft == null && !editingLocked, label = { Text("Underline") },
                            leadingIcon = if (wide) { { Icon(painterResource(Res.drawable.filled_format_underlined), null, Modifier.size(18.dp)) } } else null)
                        ReaderToolChip(tool == "note", { tool = if (tool == "note") "select" else "note"; viewModel.clearTextSelection() }, enabled = state.canWrite && state.draft == null && !editingLocked, label = { Text("Note") },
                            leadingIcon = if (wide) { { Icon(painterResource(Res.drawable.filled_note_add), null, Modifier.size(18.dp)) } } else null)
                        ReaderToolChip(tool == "idea", { tool = if (tool == "idea") "select" else "idea"; viewModel.clearTextSelection() }, enabled = state.canWrite && state.draft == null && !editingLocked,
                            label = { Text("Idea") }, leadingIcon = if (wide) { { Icon(painterResource(Res.drawable.filled_lightbulb), null, Modifier.size(18.dp)) } } else null)
                        ReaderToolChip(tool == "ink", { tool = if (tool == "ink") "select" else "ink"; viewModel.clearTextSelection() },
                            enabled = state.canWrite && (state.draft == null || state.draft.type == "ink") && !editingLocked, label = { Text("Pen") },
                            leadingIcon = if (wide) { { Icon(painterResource(Res.drawable.filled_edit), null, Modifier.size(18.dp), tint = annotationColor(inkColor)) } } else null)
                        ReaderToolChip(tool == "text", { tool = if (tool == "text") "select" else "text"; viewModel.clearTextSelection() }, enabled = state.canWrite && state.draft == null && !editingLocked,
                            label = { Text("Text") }, leadingIcon = if (wide) { { Icon(painterResource(Res.drawable.filled_title), null, Modifier.size(18.dp)) } } else null)
                        ReaderToolChip(tool == "rect", { tool = if (tool == "rect") "select" else "rect"; viewModel.clearTextSelection() }, enabled = state.canWrite && state.draft == null && !editingLocked,
                            label = { Text("Rectangle") }, leadingIcon = if (wide) { { Icon(painterResource(Res.drawable.filled_crop_square), null, Modifier.size(18.dp)) } } else null)
                        if (tool == "ink" || tool == "text") Box {
                            val sizes = if (tool == "ink") inkWidthPresets else textSizePresets
                            val current = if (tool == "ink") state.inkWidth else state.textSize
                            TextButton(enabled = !editingLocked, onClick = { showSizes = true }, modifier = Modifier.interactivePointer(enabled = !editingLocked)) {
                                Icon(if (tool == "ink") painterResource(Res.drawable.filled_line_weight) else painterResource(Res.drawable.filled_format_size),
                                    if (tool == "ink") "Pen line width" else "Text size", Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp)); Text(formatSize(current))
                            }
                            DropdownMenu(expanded = showSizes, onDismissRequest = { showSizes = false }) {
                                sizes.forEach { size ->
                                    DropdownMenuItem(
                                        text = { Text(formatSize(size)) },
                                        leadingIcon = {
                                            if (tool == "ink") Box(Modifier.width(28.dp).height(18.dp), contentAlignment = Alignment.Center) {
                                                Box(Modifier.fillMaxWidth().height((size * 0.75f).coerceIn(1f, 10f).dp)
                                                    .background(annotationColor(inkColor), RoundedCornerShape(50)))
                                            } else Icon(painterResource(Res.drawable.filled_format_size), null, Modifier.size((10 + size / 2).coerceAtMost(26f).dp))
                                        },
                                        trailingIcon = { if (size == current) Icon(painterResource(Res.drawable.filled_check), null, Modifier.size(16.dp)) },
                                        onClick = {
                                            if (tool == "ink") viewModel.setInkWidth(size) else viewModel.setTextSize(size)
                                            showSizes = false
                                        }, enabled = !editingLocked, modifier = Modifier.interactivePointer(enabled = !editingLocked))
                                }
                            }
                        }
                        Box {
                            IconButton(enabled = state.canWrite && !editingLocked && (state.draft == null || state.draft.type == "ink"),
                                onClick = { showPenColors = true }, modifier = Modifier.interactivePointer(enabled = state.canWrite && !editingLocked && (state.draft == null || state.draft.type == "ink"))) {
                                Icon(painterResource(Res.drawable.filled_circle), "Annotation color", tint = annotationColor(inkColor))
                            }
                            DropdownMenu(expanded = showPenColors, onDismissRequest = { showPenColors = false }) {
                                annotationColors.forEachIndexed { index, color ->
                                    DropdownMenuItem(
                                        text = { Text(annotationColorNames[index]) },
                                        leadingIcon = { Icon(painterResource(Res.drawable.filled_circle), null, tint = annotationColor(color)) },
                                        onClick = {
                                            viewModel.setAnnotationColor(color)
                                            showPenColors = false
                                        }, enabled = !editingLocked, modifier = Modifier.interactivePointer(enabled = !editingLocked)
                                    )
                                }
                            }
                        }
                        if (state.draft?.type == "ink") {
                            TextButton(enabled = state.canWrite && !editingLocked && state.draft.paths.isNotEmpty(), onClick = viewModel::saveDraft, modifier = Modifier.interactivePointer(enabled = state.canWrite && !editingLocked && state.draft.paths.isNotEmpty())) {
                                Icon(painterResource(Res.drawable.filled_check), null, Modifier.size(18.dp)); Text("Save drawing")
                            }
                            IconButton(enabled = !editingLocked, onClick = viewModel::cancelDraft, modifier = Modifier.interactivePointer(enabled = !editingLocked)) {
                                Icon(painterResource(Res.drawable.filled_close), "Discard drawing")
                            }
                        }
                    }
                    }
                    }
                }
            }
            if (state.loading || state.rendering || (!pdfMode && state.markdown.busy)) LinearProgressIndicator(Modifier.fillMaxWidth())
            state.error?.takeIf { !state.needsRefresh }?.let { error ->
                Surface(color = MaterialTheme.colorScheme.errorContainer) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(painterResource(Res.drawable.filled_error_outline), null)
                        Text(error, Modifier.weight(1f).padding(horizontal = 8.dp), style = MaterialTheme.typography.bodySmall)
                        if (state.needsRefresh) TextButton(enabled = !state.syncing, onClick = viewModel::refreshAnnotations, modifier = Modifier.interactivePointer(enabled = !state.syncing)) {
                            Text(if (state.localTitle != null) "Retry local save" else "Retry sync")
                        }
                    }
                }
            }
            if (state.annotations.any { it.needsInkPositionRepair }) {
                Surface(color = MaterialTheme.colorScheme.secondaryContainer) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Earlier pen drawings use an incompatible Zotero position format. Repair them to restore annotations in Zotero's PDF reader.",
                            Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                        TextButton(enabled = state.canWrite && state.draft == null && !editingLocked, onClick = viewModel::repairInkAnnotations, modifier = Modifier.interactivePointer(enabled = state.canWrite && state.draft == null && !editingLocked)) {
                            Text("Repair Zotero drawings")
                        }
                    }
                }
            }
            if (showSearch && pdfMode) ReaderSearchBar(
                inputFocus = searchFocusRequester,
                query = search,
                onQueryChange = { search = it; searchNavigationId++ },
                matchCase = matchCase,
                onMatchCaseChange = { matchCase = !matchCase; searchNavigationId++ },
                wholeWord = wholeWord,
                onWholeWordChange = { wholeWord = !wholeWord; searchNavigationId++ },
                matchCount = searchMatches.size,
                activeIndex = activeSearchIndex,
                pageNumber = state.pageIndex + 1,
                loading = state.page == null && (state.loading || state.rendering),
                pageAvailable = state.page != null,
                hasText = state.page?.glyphs?.any { it.text.isNotBlank() } == true,
                onPrevious = { navigateSearch(-1) },
                onNext = { navigateSearch(1) },
                onClose = closeSearch,
            )
            Box(Modifier.weight(1f).fillMaxWidth()) {
                CompositionLocalProvider(LocalReaderZoomAnchor provides zoomAnchor) {
                Row(Modifier.fillMaxSize().nestedScroll(headerScrollConnection).testTag("reader-viewport").readerTouchpadGestures(zoomAnchor, state.zoom) { factor ->
                    fitPage = false
                    viewModel.zoom(viewModel.state.zoom * factor)
                }) {
                    if (!pdfMode) Box(Modifier.weight(1f).fillMaxHeight()) {
                        PdfMarkdownView(viewModel, Modifier.fillMaxSize(),
                            continuous = continuous, spread = spread, fitPage = fitPage)
                    }
                    else Box(Modifier.weight(1f).fillMaxHeight()) {
                        val inspect = {
                            onInspectAnnotation()
                            if (focusMode && wide) onFocusModeChange(false)
                            if (!wide) showAnnotations = true else sidebarVisible = true
                        }
                        if (continuous && state.pageCount > 0) {
                            ContinuousPdfPages(viewModel, tool, search, inkColor,
                                onFocus = { focusRequester.requestFocus() }, onInspect = inspect, columns = if (spread) 2 else 1,
                                matchCase = matchCase, wholeWord = wholeWord, activeSearchMatch = activeSearchMatch,
                                searchPageNavigationId = searchPageNavigationId,
                                onCopySelection = copySelection, onAskAi = onAskAi)
                        } else if (spread && state.pageCount > 0) {
                            PagedSpread(viewModel, tool, search, inkColor, onFocus = { focusRequester.requestFocus() }, onInspect = inspect,
                                matchCase = matchCase, wholeWord = wholeWord, activeSearchMatch = activeSearchMatch,
                                searchPageNavigationId = searchPageNavigationId,
                                onCopySelection = copySelection, onAskAi = onAskAi)
                        } else PdfPageCanvas(Modifier.fillMaxSize(), viewModel, tool, search, fitPage, inkColor,
                            onFocus = { focusRequester.requestFocus() }, onInspect = inspect,
                            matchCase = matchCase, wholeWord = wholeWord, activeSearchMatch = activeSearchMatch,
                            searchPageNavigationId = searchPageNavigationId,
                            onCopySelection = copySelection, onAskAi = onAskAi)
                        val selectedAnnotation = state.annotations.find { it.key == state.selectedKey }
                        if (selectedAnnotation != null && state.textSelection == null && state.draft == null && !editingLocked) {
                            Surface(Modifier.align(Alignment.BottomCenter).padding(bottom = 76.dp).testTag("pdf-annotation-actions"),
                                shape = RoundedCornerShape(20.dp), shadowElevation = 8.dp, tonalElevation = 6.dp) {
                                Row(Modifier.padding(horizontal = 8.dp).horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
                                    annotationColors.forEachIndexed { index, hex ->
                                        val current = selectedAnnotation.color.equals(hex, true)
                                        val enabled = state.canWrite
                                        Box(Modifier.padding(horizontal = 3.dp).size(22.dp).clip(CircleShape)
                                            .background(annotationColor(hex))
                                            .border(if (current) 2.dp else 1.dp,
                                                if (current) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                            .interactivePointer(enabled = enabled)
                                            .semantics { selected = current }
                                            .clickable(enabled = enabled, role = Role.RadioButton,
                                                onClickLabel = "${annotationColorNames[index]} annotation color") {
                                                viewModel.recolorAnnotation(selectedAnnotation, hex)
                                            }
                                            .semantics { contentDescription = "${annotationColorNames[index]} annotation color" },
                                            contentAlignment = Alignment.Center) {
                                            if (current) Icon(painterResource(Res.drawable.filled_check), null, Modifier.size(14.dp), tint = Color.Black.copy(alpha = 0.7f))
                                        }
                                    }
                                    VerticalDivider(Modifier.height(20.dp).padding(horizontal = 6.dp))
                                    if (selectedAnnotation.text.isNotBlank()) {
                                        TextButton(onClick = {
                                            clipboard.setText(AnnotatedString(selectedAnnotation.text))
                                            scope.launch { snackbar.showSnackbar("Annotation text copied") }
                                        }, modifier = Modifier.interactivePointer()) {
                                            Icon(painterResource(Res.drawable.filled_content_copy), null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Copy text")
                                        }
                                    }
                                    if (selectedAnnotation.type in listOf("ink", "text")) {
                                        val ink = selectedAnnotation.type == "ink"
                                        val value = if (ink) selectedAnnotation.position.width else selectedAnnotation.position.fontSize
                                        val presets = if (ink) inkWidthPresets else textSizePresets
                                        val enabled = state.canWrite
                                        IconButton(enabled = enabled && presets.any { it < value - 0.01f },
                                            onClick = { viewModel.resizeAnnotation(selectedAnnotation, larger = false) },
                                            modifier = Modifier.interactivePointer(enabled = enabled)) {
                                            Icon(painterResource(Res.drawable.filled_remove), if (ink) "Thinner line" else "Smaller text", Modifier.size(18.dp))
                                        }
                                        Text(formatSize(value), style = MaterialTheme.typography.labelMedium,
                                            modifier = Modifier.semantics { contentDescription = if (ink) "Line width" else "Text size" })
                                        IconButton(enabled = enabled && presets.any { it > value + 0.01f },
                                            onClick = { viewModel.resizeAnnotation(selectedAnnotation, larger = true) },
                                            modifier = Modifier.interactivePointer(enabled = enabled)) {
                                            Icon(painterResource(Res.drawable.filled_add), if (ink) "Thicker line" else "Larger text", Modifier.size(18.dp))
                                        }
                                        VerticalDivider(Modifier.height(20.dp).padding(horizontal = 6.dp))
                                    }
                                    if (selectedAnnotation.type in listOf("highlight", "underline")) {
                                        TextButton(enabled = state.canWrite, onClick = { viewModel.adjustAnnotationRange(selectedAnnotation) },
                                            modifier = Modifier.interactivePointer(enabled = state.canWrite)) {
                                            Icon(painterResource(Res.drawable.filled_tune), null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Adjust text range")
                                        }
                                    }
                                    TextButton(enabled = state.canWrite, onClick = { viewModel.editAnnotation(selectedAnnotation) },
                                        modifier = Modifier.interactivePointer(enabled = state.canWrite)) {
                                        Icon(painterResource(Res.drawable.filled_edit), null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp))
                                        Text(if (selectedAnnotation.type == "text") "Edit text" else "Edit note")
                                    }
                                    IconButton(onClick = viewModel::clearSelectedAnnotation, modifier = Modifier.interactivePointer()) {
                                        Icon(painterResource(Res.drawable.filled_close), "Deselect annotation")
                                    }
                                }
                            }
                        }
                    }
                }
                }

                val step = if (spread && !continuous) 2 else 1
                val pageStart = if (step == 2) state.pageIndex / 2 * 2 else state.pageIndex
                val zoomLabel = if (fitPage) "Fit page" else "${(state.zoom * 100).roundToInt()}%"
                if (compact) androidx.compose.animation.AnimatedVisibility(
                    visible = !headerCollapsed || state.draft != null || !pdfMode,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp),
                    enter = slideInVertically(tween(220)) { it } + fadeIn(tween(180)),
                    exit = slideOutVertically(tween(200)) { it } + fadeOut(tween(150)),
                ) {
                    CompactReaderBottomBar(
                        pageLabel = "${state.pageIndex + 1} / ${state.readerPageCount}",
                        canPrevious = state.pageIndex > 0 && state.draft == null,
                        canNext = pageStart + step < state.readerPageCount && state.draft == null,
                        pageEnabled = state.readerPageCount > 0 && state.draft == null,
                        onPrevious = { viewModel.goToPage((pageStart - step).coerceAtLeast(0)) },
                        onNext = { viewModel.goToPage(pageStart + step) },
                        onPageClick = { showPagePicker = true },
                        pdfMode = pdfMode,
                        zoomLabel = zoomLabel,
                        onViewOptions = { showViewSheet = true },
                        canShowMarkdown = hasMarkdownStorage && state.draft == null,
                        onShowPdf = viewModel::showPdf,
                        onShowMarkdown = viewModel::showMarkdown,
                        canCopyMarkdown = state.markdown.content != null,
                        onCopyMarkdown = {
                            state.markdown.content?.let {
                                clipboard.setText(AnnotatedString(it))
                                scope.launch { snackbar.showSnackbar("Markdown copied") }
                            }
                        },
                        canRegenerate = state.markdown.content != null && !state.markdown.busy,
                        onRegenerate = viewModel::regenerateMarkdown,
                    )
                } else Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(24.dp),
                    shadowElevation = 8.dp,
                    tonalElevation = 4.dp,
                    color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.94f)
                ) {
                    Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        IconButton(enabled = state.pageIndex > 0 && state.draft == null, onClick = { viewModel.goToPage((pageStart - step).coerceAtLeast(0)) }, modifier = Modifier.interactivePointer(enabled = state.pageIndex > 0 && state.draft == null)) { Icon(painterResource(Res.drawable.filled_chevron_left), "Previous page") }
                        TextButton(enabled = state.readerPageCount > 0 && state.draft == null, onClick = { showPagePicker = true }, modifier = Modifier.interactivePointer(enabled = state.readerPageCount > 0 && state.draft == null)) { Text("${state.pageIndex + 1} / ${state.readerPageCount}") }
                        IconButton(enabled = pageStart + step < state.readerPageCount && state.draft == null, onClick = { viewModel.goToPage(pageStart + step) }, modifier = Modifier.interactivePointer(enabled = pageStart + step < state.readerPageCount && state.draft == null)) { Icon(painterResource(Res.drawable.filled_chevron_right), "Next page") }
                        VerticalDivider(Modifier.height(20.dp).padding(horizontal = 4.dp))
                        IconButton(enabled = state.zoom > 0.5f, onClick = { zoomAnchor.capture(atPointer = false); fitPage = false; viewModel.zoom(state.zoom - 0.25f) }, modifier = Modifier.interactivePointer(enabled = state.zoom > 0.5f)) { Icon(painterResource(Res.drawable.filled_zoom_out), "Zoom out") }
                        TextButton(onClick = { zoomAnchor.capture(atPointer = false); fitPage = false; viewModel.zoom(1f) }, modifier = Modifier.interactivePointer()) { Text(zoomLabel) }
                        IconButton(enabled = state.zoom < 3f, onClick = { zoomAnchor.capture(atPointer = false); fitPage = false; viewModel.zoom(state.zoom + 0.25f) }, modifier = Modifier.interactivePointer(enabled = state.zoom < 3f)) { Icon(painterResource(Res.drawable.filled_zoom_in), "Zoom in") }
                        IconButton(enabled = !continuous && !spread, onClick = { fitPage = !fitPage; viewModel.zoom(1f) }, modifier = Modifier.interactivePointer(enabled = !continuous && !spread)) { Icon(painterResource(Res.drawable.filled_fit_screen), "Toggle fit page") }
                        VerticalDivider(Modifier.height(20.dp).padding(horizontal = 4.dp))
                        ReaderLayoutMenu(
                            spread = spread,
                            continuous = continuous,
                            enabled = state.draft == null,
                            onSpreadChange = { spread = it; if (it) fitPage = false },
                            onContinuousChange = { continuous = !continuous; fitPage = false },
                            modifier = Modifier.widthIn(min = 132.dp, max = 156.dp),
                        )
                        Box {
                            var menu by remember { mutableStateOf(false) }
                            IconButton(onClick = { menu = true }, modifier = Modifier.interactivePointer()) {
                                Icon(painterResource(Res.drawable.filled_more_vert), "More PDF viewer options")
                            }
                            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, shape = RoundedCornerShape(16.dp)) {
                                DropdownMenuItem(
                                    text = { Text(if (pdfMode) "Show Markdown" else "Show PDF") },
                                    leadingIcon = {
                                        Icon(
                                            painterResource(if (pdfMode) Res.drawable.filled_article else Res.drawable.filled_picture_as_pdf),
                                            null,
                                        )
                                    },
                                    enabled = if (pdfMode) hasMarkdownStorage && state.draft == null else hasDocument,
                                    onClick = { menu = false; if (pdfMode) viewModel.showMarkdown() else viewModel.showPdf() },
                                    modifier = Modifier.interactivePointer(enabled = if (pdfMode) hasMarkdownStorage && state.draft == null else hasDocument),
                                )
                                DropdownMenuItem(
                                    text = { Text("Copy Markdown") },
                                    leadingIcon = { Icon(painterResource(Res.drawable.filled_content_copy), null) },
                                    enabled = state.markdown.content != null,
                                    onClick = {
                                        menu = false
                                        state.markdown.content?.let {
                                            clipboard.setText(AnnotatedString(it))
                                            scope.launch { snackbar.showSnackbar("Markdown copied") }
                                        }
                                    },
                                    modifier = Modifier.interactivePointer(enabled = state.markdown.content != null),
                                )
                                val canRegenerate = state.markdown.content != null && !state.markdown.busy
                                DropdownMenuItem(
                                    text = { Text("Convert again") },
                                    leadingIcon = { Icon(painterResource(Res.drawable.filled_refresh), null) },
                                    enabled = canRegenerate,
                                    onClick = { menu = false; viewModel.regenerateMarkdown() },
                                    modifier = Modifier.interactivePointer(enabled = canRegenerate),
                                )
                            }
                        }
                    }
                }

                if (focusMode) {
                    Row(
                        modifier = Modifier.align(Alignment.TopEnd).padding(top = 16.dp, end = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ZoteroSyncIndicator(state, viewModel::refreshAnnotations)
                        FilledTonalIconButton(
                            onClick = { onFocusModeChange(false) },
                            modifier = Modifier.interactivePointer()
                        ) {
                            Icon(painterResource(Res.drawable.filled_fullscreen_exit), "Exit focus mode")
                        }
                    }
                }
            }
        }
        if (wide && sidebarVisible && !focusMode) {
            VerticalDivider()
            val minSidebarWidth = 280.dp
            val maxSidebarWidth = (readerWidth / 2f).coerceAtLeast(minSidebarWidth)
            val resolvedSidebarWidth = sidebarWidth.coerceIn(minSidebarWidth, maxSidebarWidth)
            Box(Modifier.width(resolvedSidebarWidth).fillMaxHeight()) {
                Surface(
                    Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Box(Modifier.fillMaxSize().padding(start = 24.dp, end = 16.dp, top = 16.dp, bottom = 16.dp)) {
                        if (supportingContent != null) supportingContent(!editingLocked)
                        else ReaderAnnotationPane(viewModel, allowEditing = !editingLocked)
                    }
                }
                Box(
                    Modifier
                        .align(Alignment.CenterStart)
                        .width(12.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        .pointerHoverIcon(PointerIcon.Hand)
                        .pointerInput(readerWidth) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val dragWidth = with(density) { dragAmount.x.toDp() }
                                sidebarWidth = (sidebarWidth - dragWidth)
                                    .coerceIn(minSidebarWidth, maxSidebarWidth)
                            }
                        }
                        .semantics { contentDescription = "Resize sidebar" }
                        .testTag("reader-sidebar-resize-handle")
                ) {
                    Box(
                        Modifier
                            .align(Alignment.Center)
                            .width(3.dp)
                            .height(56.dp)
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.outline)
                    )
                }
            }
        }
        }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = if (state.textSelection != null || state.selectedKey != null) 128.dp else 64.dp))
    }
    if (showPagePicker) {
        var pageInput by remember { mutableStateOf((state.pageIndex + 1).toString()) }
        val go: () -> Unit = { pageInput.toIntOrNull()?.takeIf { it in 1..state.readerPageCount }?.let { viewModel.goToPage(it - 1); showPagePicker = false } }
        AlertDialog(onDismissRequest = { showPagePicker = false }, title = { Text("Go to page") },
            text = { OutlinedTextField(pageInput, { pageInput = it.filter(Char::isDigit).take(5) },
                singleLine = true, label = { Text("Page (1-${state.readerPageCount})") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go), keyboardActions = KeyboardActions(onGo = { go() })) },
            confirmButton = { TextButton(enabled = pageInput.toIntOrNull()?.let { it in 1..state.readerPageCount } == true, onClick = go, modifier = Modifier.interactivePointer(enabled = pageInput.toIntOrNull()?.let { it in 1..state.readerPageCount } == true)) { Text("Go") } },
            dismissButton = { TextButton(onClick = { showPagePicker = false }, modifier = Modifier.interactivePointer()) { Text("Cancel") } })
    }
    if (showAnnotations) {
        ModalBottomSheet(onDismissRequest = { showAnnotations = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            Box(
                Modifier.fillMaxWidth()
                    .fillMaxHeight(0.9f)
                    .widthIn(max = 840.dp)
                    .align(Alignment.CenterHorizontally)
                    .padding(horizontal = 8.dp, vertical = 8.dp)
                    .imePadding()
            ) {
                if (supportingContent != null) supportingContent(!editingLocked)
                else ReaderAnnotationPane(viewModel, onSelected = { showAnnotations = false }, allowEditing = !editingLocked)
            }
        }
    }
    if (showAnnotateSheet) {
        CompactAnnotationSheet(
            tool = tool,
            onToolChange = { id -> tool = id; if (id != "select") viewModel.clearTextSelection() },
            colorHex = inkColor,
            onColorChange = viewModel::setAnnotationColor,
            inkWidth = state.inkWidth,
            textSize = state.textSize,
            onInkWidthChange = viewModel::setInkWidth,
            onTextSizeChange = viewModel::setTextSize,
            inkOnly = state.draft?.type == "ink",
            enabled = state.canWrite && !editingLocked,
            onDismiss = { showAnnotateSheet = false },
        )
    }
    if (showViewSheet) {
        val zoomTo: (Float) -> Unit = { value -> zoomAnchor.capture(atPointer = false); fitPage = false; viewModel.zoom(value) }
        CompactViewOptionsSheet(
            zoomLabel = if (fitPage) "Fit page" else "${(state.zoom * 100).roundToInt()}%",
            canZoomOut = state.zoom > 0.5f,
            canZoomIn = state.zoom < 3f,
            onZoomOut = { zoomTo(state.zoom - 0.25f) },
            onZoomIn = { zoomTo(state.zoom + 0.25f) },
            onZoomReset = { zoomTo(1f) },
            fitPage = fitPage,
            fitEnabled = !continuous && !spread,
            onToggleFit = { fitPage = !fitPage; viewModel.zoom(1f) },
            spread = spread,
            layoutEnabled = state.draft == null,
            onSpreadChange = { spread = it; if (it) fitPage = false },
            continuous = continuous,
            onContinuousChange = { continuous = !continuous; fitPage = false },
            onDismiss = { showViewSheet = false },
        )
    }
    if (!editingLocked && !state.adjustingTextRange && (state.draft?.type != "ink" || state.draft.editing != null)) {
        AnnotationEditor(viewModel, paperAgent)
    }
}

@Composable
private fun MarkdownToolbarButton(
    description: String,
    icon: androidx.compose.ui.graphics.painter.Painter,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(34.dp).interactivePointer()
    ) {
        Icon(
            icon,
            contentDescription = description,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderToolChip(selected: Boolean, onClick: () -> Unit, enabled: Boolean = true,
    label: @Composable () -> Unit, leadingIcon: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier, shape: RoundedCornerShape = RoundedCornerShape(24.dp)) {
    val colors = MaterialTheme.colorScheme
    CompositionLocalProvider(LocalRippleConfiguration provides RippleConfiguration(color = colors.primary)) {
        FilterChip(selected, onClick, label,
            modifier = modifier.clip(shape).interactivePointer(enabled = enabled),
            enabled = enabled, leadingIcon = leadingIcon, shape = shape,
            colors = FilterChipDefaults.filterChipColors(
                containerColor = colors.surfaceContainerLow,
                labelColor = colors.onSurfaceVariant,
                iconColor = colors.onSurfaceVariant,
                selectedContainerColor = colors.primaryContainer,
                selectedLabelColor = colors.onPrimaryContainer,
                selectedLeadingIconColor = colors.onPrimaryContainer,
                selectedTrailingIconColor = colors.onPrimaryContainer),
            border = FilterChipDefaults.filterChipBorder(enabled, selected,
                borderColor = colors.outlineVariant,
                selectedBorderColor = colors.primary.copy(alpha = 0.3f)))
    }
}

@Composable
private fun ReaderAnnotationPane(viewModel: PdfWorkspaceViewModel, onSelected: () -> Unit = {}, allowEditing: Boolean = true) {
    var ideas by remember(viewModel.state.target) { mutableStateOf(false) }
    LaunchedEffect(viewModel.state.selectedKey) {
        viewModel.state.annotations.find { it.key == viewModel.state.selectedKey }?.let { ideas = it.isIdea }
    }
    Column(Modifier.fillMaxSize()) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ReaderToolChip(!ideas, { ideas = false }, label = { Text("Annotations") })
            ReaderToolChip(ideas, { ideas = true }, label = { Text("Ideas") })
        }
        AnnotationList(viewModel, Modifier.weight(1f), onSelected, ideasOnly = ideas, allowEditing = allowEditing)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AnnotationEditor(viewModel: PdfWorkspaceViewModel, paperAgent: PaperAgentViewModel? = null) {
    val state = viewModel.state
    state.draft?.let { draft ->
        var editMarkdown by remember(draft.key) { mutableStateOf(draft.comment.isBlank()) }
        var annotationAiAction by remember(draft.key) { mutableStateOf(AnnotationAiAction.Describe) }
        var annotationAiBrief by remember(draft.key) { mutableStateOf(true) }
        var annotationAiExpanded by remember(draft.key) { mutableStateOf(false) }
        var markdownValue by remember(draft.key) { mutableStateOf(TextFieldValue(draft.comment)) }
        val noteTint = annotationColor(draft.color)
        val noteBackground = noteTint.copy(alpha = 0.08f)
        val annotationTitle = when {
            draft.editing != null && draft.type == "text" -> "Edit text"
            draft.editing != null -> "Edit annotation"
            draft.isIdea -> "Page idea"
            draft.type == "note" -> "Page note"
            draft.type == "text" -> "Text box"
            draft.type == "image" -> "Rectangle"
            draft.type == "underline" -> "Text underline"
            else -> "Text highlight"
        }
        val annotationIcon = when {
            draft.isIdea -> Res.drawable.filled_lightbulb
            draft.type == "note" -> Res.drawable.filled_note_add
            draft.type == "text" -> Res.drawable.filled_text_fields
            draft.type == "image" -> Res.drawable.filled_crop_square
            draft.type == "underline" -> Res.drawable.filled_format_underlined
            draft.type == "ink" -> Res.drawable.filled_gesture
            else -> Res.drawable.filled_highlight
        }
        LaunchedEffect(draft.comment) {
            if (markdownValue.text != draft.comment) markdownValue = TextFieldValue(draft.comment)
        }
        val runAnnotationAi: (AnnotationAiAction) -> Unit = { action ->
            paperAgent?.generateAnnotationComment(
                selectedText = draft.text,
                existingComment = draft.comment,
                pageIndex = draft.pageIndex,
                includePage = paperAgent?.includePageContext == true,
                includePaper = paperAgent?.includePaperContext == true,
                action = action,
                brief = annotationAiBrief,
            ) { generated ->
                viewModel.changeDraft(generated, draft.color)
                editMarkdown = true
            }
        }
        AlertDialog(
            onDismissRequest = viewModel::cancelDraft,
            modifier = Modifier.widthIn(max = 620.dp),
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 6.dp,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = noteTint.copy(alpha = 0.16f),
                        contentColor = noteTint,
                    ) {
                        Icon(
                            painterResource(annotationIcon),
                            contentDescription = null,
                            modifier = Modifier.padding(10.dp).size(22.dp),
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(annotationTitle, style = MaterialTheme.typography.headlineSmall)
                        Text(
                            "Page ${draft.pageIndex + 1} · ${if (draft.editing == null) "New annotation" else "Saved annotation"}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
            text = {
                Column(
                    Modifier.heightIn(max = 560.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                painterResource(Res.drawable.filled_picture_as_pdf),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (state.localTitle != null) "Saved on this device" else "Synced with Zotero",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(Modifier.weight(1f))
                            Text("Page ${draft.pageIndex + 1}", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    if (draft.editing == null && draft.text.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f)),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Selected text", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                                    Text("From page ${draft.pageIndex + 1}", style = MaterialTheme.typography.labelSmall)
                                }
                                Text(
                                    draft.text,
                                    maxLines = 6,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (editMarkdown) "Edit Markdown" else "Markdown preview",
                            style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                        TextButton(onClick = { editMarkdown = !editMarkdown }, modifier = Modifier.interactivePointer()) {
                            Text(if (editMarkdown) "Preview" else "Edit")
                        }
                    }
                    if (editMarkdown) {
                        Column(
                            Modifier.fillMaxWidth()
                                .border(1.dp, noteTint.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .clip(RoundedCornerShape(12.dp))
                                .background(noteBackground)
                        ) {
                            Row(
                                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                MarkdownToolbarButton("Bold", painterResource(Res.drawable.filled_format_bold)) {
                                    val updated = markdownValue.wrapSelection("**", "**")
                                    markdownValue = updated
                                    viewModel.changeDraft(updated.text, draft.color)
                                }
                                MarkdownToolbarButton("Italic", painterResource(Res.drawable.filled_format_italic)) {
                                    val updated = markdownValue.wrapSelection("*", "*")
                                    markdownValue = updated
                                    viewModel.changeDraft(updated.text, draft.color)
                                }
                                MarkdownToolbarButton("Inline code", painterResource(Res.drawable.filled_code)) {
                                    val updated = markdownValue.wrapSelection("`", "`")
                                    markdownValue = updated
                                    viewModel.changeDraft(updated.text, draft.color)
                                }
                                VerticalDivider(Modifier.height(22.dp).padding(horizontal = 3.dp))
                                MarkdownToolbarButton("Heading", painterResource(Res.drawable.filled_title)) {
                                    val updated = markdownValue.prefixSelectedLines("## ")
                                    markdownValue = updated
                                    viewModel.changeDraft(updated.text, draft.color)
                                }
                                MarkdownToolbarButton("Bulleted list", painterResource(Res.drawable.filled_format_list_bulleted)) {
                                    val updated = markdownValue.prefixSelectedLines("- ")
                                    markdownValue = updated
                                    viewModel.changeDraft(updated.text, draft.color)
                                }
                                MarkdownToolbarButton("Numbered list", painterResource(Res.drawable.filled_format_list_numbered)) {
                                    val updated = markdownValue.prefixSelectedLines("1. ")
                                    markdownValue = updated
                                    viewModel.changeDraft(updated.text, draft.color)
                                }
                                MarkdownToolbarButton("Quote", painterResource(Res.drawable.filled_format_quote)) {
                                    val updated = markdownValue.prefixSelectedLines("> ")
                                    markdownValue = updated
                                    viewModel.changeDraft(updated.text, draft.color)
                                }
                                MarkdownToolbarButton("Link", painterResource(Res.drawable.filled_link)) {
                                    val updated = markdownValue.wrapSelection("[", "](url)")
                                    markdownValue = updated
                                    viewModel.changeDraft(updated.text, draft.color)
                                }
                                Spacer(Modifier.weight(1f))
                                if (paperAgent != null && draft.comment.isNotBlank() && draft.type != "text") {
                                    IconButton(
                                        onClick = { runAnnotationAi(AnnotationAiAction.Rephrase) },
                                        enabled = !paperAgent.annotationAiRunning,
                                        modifier = Modifier.size(34.dp).interactivePointer(enabled = !paperAgent.annotationAiRunning)
                                    ) {
                                        Icon(painterResource(Res.drawable.filled_auto_fix_high), "Rephrase with AI", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(
                                        onClick = { runAnnotationAi(AnnotationAiAction.Correct) },
                                        enabled = !paperAgent.annotationAiRunning,
                                        modifier = Modifier.size(34.dp).interactivePointer(enabled = !paperAgent.annotationAiRunning)
                                    ) {
                                        Icon(painterResource(Res.drawable.filled_spellcheck), "Correct with AI", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                            OutlinedTextField(
                                value = markdownValue,
                                onValueChange = {
                                    markdownValue = it
                                    viewModel.changeDraft(it.text, draft.color)
                                },
                                label = { Text(if (draft.type == "text") "Text" else "Note / comment") },
                                placeholder = { Text("Write a Markdown note or comment…") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 6,
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedBorderColor = Color.Transparent
                                )
                            )
                        }
                    } else if (draft.comment.isBlank()) {
                        Text("No note yet. Switch to Edit to write one or ask the AI agent.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Surface(
                            onClick = { editMarkdown = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, noteTint.copy(alpha = 0.5f)),
                            color = noteBackground
                        ) {
                            androidx.compose.foundation.lazy.LazyColumn(
                                modifier = Modifier.heightIn(min = 100.dp, max = 280.dp).padding(14.dp)
                            ) {
                                item { ChatMarkdown(draft.comment, Modifier.fillMaxWidth()) }
                            }
                        }
                    }
                    if (paperAgent != null && draft.text.isNotBlank() && draft.type != "text") {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
                        ) {
                            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(painterResource(Res.drawable.filled_auto_awesome), null, Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text("AI note assistant", style = MaterialTheme.typography.labelLarge)
                                        Text(
                                            if (annotationAiExpanded) "Choose how to draft a note from the selected text"
                                            else "Optional: generate a note from the selected text",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    IconButton(
                                        onClick = { annotationAiExpanded = !annotationAiExpanded },
                                        modifier = Modifier.size(36.dp).interactivePointer(),
                                    ) {
                                        Icon(
                                            painterResource(
                                                if (annotationAiExpanded) Res.drawable.filled_keyboard_arrow_up
                                                else Res.drawable.filled_keyboard_arrow_down,
                                            ),
                                            if (annotationAiExpanded) "Collapse AI note assistant" else "Expand AI note assistant",
                                            Modifier.size(20.dp),
                                        )
                                    }
                                }
                                if (annotationAiExpanded) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("AI action", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Checkbox(
                                                checked = annotationAiBrief,
                                                onCheckedChange = { annotationAiBrief = it },
                                                modifier = Modifier.size(28.dp).interactivePointer()
                                            )
                                            Text("Brief", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        AnnotationAiAction.entries.filterNot {
                                            it == AnnotationAiAction.Rephrase || it == AnnotationAiAction.Correct
                                        }.forEach { option ->
                                            FilterChip(
                                                selected = annotationAiAction == option,
                                                onClick = { annotationAiAction = option },
                                                label = { Text(option.label, style = MaterialTheme.typography.labelMedium) },
                                                leadingIcon = if (annotationAiAction == option) {
                                                    { Icon(painterResource(Res.drawable.filled_check), null, Modifier.size(15.dp)) }
                                                } else null,
                                                colors = FilterChipDefaults.filterChipColors(
                                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    selectedLeadingIconColor = MaterialTheme.colorScheme.primary
                                                ),
                                                modifier = Modifier.clipChipInteraction().interactivePointer()
                                            )
                                        }
                                    }
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Text(
                                            "Context range",
                                            style = MaterialTheme.typography.labelMedium,
                                            modifier = Modifier.weight(1f),
                                        )
                                        PageRangeSelector(
                                            agent = paperAgent,
                                            pageCount = state.readerPageCount.coerceAtLeast(1),
                                            currentPage = draft.pageIndex,
                                            targetLabel = "AI note",
                                        )
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    ) {
                                        Text(
                                            "Only updates the note draft; it does not save or close this dialog.",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.weight(1f),
                                        )
                                        FilledTonalButton(
                                            enabled = !paperAgent.annotationAiRunning,
                                            onClick = { runAnnotationAi(annotationAiAction) },
                                            modifier = Modifier.interactivePointer(enabled = !paperAgent.annotationAiRunning),
                                        ) {
                                            Text(if (paperAgent.annotationAiRunning) "Generating…" else "Generate & insert")
                                        }
                                    }
                                }
                                paperAgent.annotationAiError?.let {
                                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                    if (draft.type == "text") FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        textSizePresets.forEach { size ->
                            ReaderToolChip(selected = draft.fontSize == size, onClick = { viewModel.setTextSize(size) },
                                modifier = Modifier.interactivePointer(),
                                label = { Text(formatSize(size)) })
                        }
                    }
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                    ) {
                        Column(
                            Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text("Annotation color", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Choose a color that is easy to scan in your PDF.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Row(
                                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                annotationColors.forEachIndexed { index, hex ->
                                    AnnotationColorChoice(
                                        selected = draft.color.equals(hex, ignoreCase = true),
                                        color = annotationColor(hex),
                                        label = annotationColorNames[index],
                                        onClick = { viewModel.changeDraft(draft.comment, hex) },
                                    )
                                }
                            }
                        }
                    }
                    state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall) }
                    if (state.needsRefresh) Text("Unsaved changes are retained. Use the header save-status button to retry.",
                        style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                val canSave = state.canWrite && (draft.type !in listOf("note", "text") || draft.comment.isNotBlank())
                Button(
                    enabled = canSave,
                    onClick = viewModel::saveDraft,
                    modifier = Modifier.interactivePointer(enabled = canSave),
                ) { Text(if (state.localTitle != null) "Save locally & close" else "Save to Zotero & close") }
            },
            dismissButton = { TextButton(onClick = viewModel::cancelDraft, modifier = Modifier.interactivePointer()) { Text("Cancel") } }
        )
    }
}

@Composable
private fun AnnotationColorChoice(
    selected: Boolean,
    color: Color,
    label: String,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = Modifier.semantics {
            this.selected = selected
            contentDescription = "$label annotation color"
        }.interactivePointer(),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) color.copy(alpha = 0.16f) else colors.surfaceContainerHigh,
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) color else colors.outlineVariant,
        ),
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Box(
                Modifier.size(18.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(1.dp, colors.onSurface.copy(alpha = 0.14f), CircleShape),
            )
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) colors.onSurface else colors.onSurfaceVariant,
            )
            if (selected) {
                Icon(
                    painterResource(Res.drawable.filled_check),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = colors.onSurface,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ZoteroSyncIndicator(state: PdfWorkspaceState, onRetry: () -> Unit) {
    val local = state.localTitle != null
    val status = state.annotationSaveStatus
    val tint = when {
        state.syncing -> MaterialTheme.colorScheme.primary
        state.needsRefresh || state.pendingWrites > 0 -> MaterialTheme.colorScheme.error
        else -> if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) Color(0xFF81C784) else Color(0xFF2E7D32)
    }
    val rotation = if (state.syncing) {
        val transition = rememberInfiniteTransition(label = "Zotero sync")
        val angle by transition.animateFloat(0f, 360f,
            infiniteRepeatable(tween(1000, easing = LinearEasing)), label = "Sync rotation")
        angle
    } else 0f
    val enabled = !state.syncing && !state.loading
    TooltipBox(positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(state.error?.takeIf { state.needsRefresh } ?: status) } }, state = rememberTooltipState()) {
        IconButton(onClick = onRetry, enabled = enabled,
            modifier = Modifier.testTag(if (local) "local-annotation-save-status" else "zotero-sync-status").interactivePointer(enabled = enabled)) {
            Box {
                Icon(painterResource(if (local && !state.syncing) Res.drawable.filled_description else Res.drawable.filled_sync),
                    status, Modifier.graphicsLayer { rotationZ = rotation }, tint = tint)
                if (!state.syncing) Icon(
                    if (state.needsRefresh || state.pendingWrites > 0) painterResource(Res.drawable.filled_error) else painterResource(Res.drawable.filled_check_circle),
                    null, Modifier.size(12.dp).align(Alignment.BottomEnd)
                        .background(MaterialTheme.colorScheme.surface, CircleShape), tint = tint)
            }
        }
    }
}

@Composable
private fun ReaderSearchBar(
    inputFocus: FocusRequester,
    query: String,
    onQueryChange: (String) -> Unit,
    matchCase: Boolean,
    onMatchCaseChange: () -> Unit,
    wholeWord: Boolean,
    onWholeWordChange: () -> Unit,
    matchCount: Int,
    activeIndex: Int,
    pageNumber: Int,
    loading: Boolean,
    pageAvailable: Boolean,
    hasText: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit,
) {
    LaunchedEffect(Unit) { inputFocus.requestFocus() }
    val colors = MaterialTheme.colorScheme
    val noMatches = query.isNotBlank() && hasText && !loading && matchCount == 0
    val status = when {
        loading -> "Loading page..."
        !pageAvailable -> "Page unavailable"
        !hasText -> "No searchable text"
        query.isBlank() -> "Type to find"
        matchCount == 0 -> "No matches"
        else -> "${activeIndex + 1} of $matchCount"
    }
    val navigation: @Composable () -> Unit = {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            FilledTonalIconButton(onClick = onPrevious, enabled = matchCount > 0,
                shape = RoundedCornerShape(10.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = colors.surfaceContainerHigh,
                    contentColor = colors.onSurfaceVariant,
                    disabledContainerColor = Color.Transparent,
                ),
                modifier = Modifier.interactivePointer(enabled = matchCount > 0)) {
                Icon(painterResource(Res.drawable.filled_keyboard_arrow_up), "Previous match", Modifier.size(20.dp))
            }
            FilledTonalIconButton(onClick = onNext, enabled = matchCount > 0,
                shape = RoundedCornerShape(10.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = colors.surfaceContainerHigh,
                    contentColor = colors.onSurfaceVariant,
                    disabledContainerColor = Color.Transparent,
                ),
                modifier = Modifier.interactivePointer(enabled = matchCount > 0)) {
                Icon(painterResource(Res.drawable.filled_keyboard_arrow_down), "Next match", Modifier.size(20.dp))
            }
        }
    }
    Surface(
        color = colors.surface,
        modifier = Modifier.fillMaxWidth().testTag("pdf-search-bar"),
    ) {
        Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier.widthIn(max = 760.dp).fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = lerp(colors.surface, colors.surfaceContainerHigh, 0.4f),
            border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.6f)),
            shadowElevation = 1.dp,
        ) {
        BoxWithConstraints(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            val compact = maxWidth < 520.dp
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        modifier = Modifier.weight(1f).focusRequester(inputFocus),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        label = { Text("Find on this page") },
                        textStyle = MaterialTheme.typography.bodyMedium,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = colors.surface,
                            unfocusedContainerColor = colors.surface,
                            focusedBorderColor = colors.primary.copy(alpha = 0.55f),
                            unfocusedBorderColor = colors.outlineVariant,
                            focusedLabelColor = colors.onSurfaceVariant,
                            unfocusedLabelColor = colors.onSurfaceVariant,
                            focusedLeadingIconColor = colors.onSurfaceVariant,
                            unfocusedLeadingIconColor = colors.onSurfaceVariant,
                        ),
                        leadingIcon = { Icon(painterResource(Res.drawable.filled_search), null, Modifier.size(20.dp)) },
                        trailingIcon = {
                            if (query.isNotEmpty()) IconButton(
                                onClick = { onQueryChange(""); inputFocus.requestFocus() },
                                modifier = Modifier.interactivePointer(),
                            ) { Icon(painterResource(Res.drawable.filled_close), "Clear search", Modifier.size(18.dp)) }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { onNext() }),
                    )
                    if (!compact) Box(Modifier.padding(top = 8.dp)) { navigation() }
                    IconButton(onClick = onClose, modifier = Modifier.padding(top = 8.dp).interactivePointer()) {
                        Icon(painterResource(Res.drawable.filled_close), "Close search", Modifier.size(20.dp))
                    }
                }
                if (compact) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    navigation()
                    Text("Page $pageNumber", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = matchCase, onClick = onMatchCaseChange,
                        label = { Text("Match case", style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = { if (matchCase) Icon(painterResource(Res.drawable.filled_check), null, Modifier.size(14.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color.Transparent,
                            selectedContainerColor = colors.secondaryContainer,
                            selectedLabelColor = colors.onSecondaryContainer,
                            selectedLeadingIconColor = colors.onSecondaryContainer,
                        ),
                        modifier = Modifier.clip(RoundedCornerShape(10.dp)).interactivePointer(), shape = RoundedCornerShape(10.dp))
                    FilterChip(selected = wholeWord, onClick = onWholeWordChange,
                        label = { Text("Whole word", style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = { if (wholeWord) Icon(painterResource(Res.drawable.filled_check), null, Modifier.size(14.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color.Transparent,
                            selectedContainerColor = colors.secondaryContainer,
                            selectedLabelColor = colors.onSecondaryContainer,
                            selectedLeadingIconColor = colors.onSecondaryContainer,
                        ),
                        modifier = Modifier.clip(RoundedCornerShape(10.dp)).interactivePointer(), shape = RoundedCornerShape(10.dp))
                    Spacer(Modifier.weight(1f))
                    Surface(
                        color = if (matchCount > 0) colors.secondaryContainer.copy(alpha = 0.45f) else Color.Transparent,
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(
                            status,
                            style = MaterialTheme.typography.labelMedium,
                            color = when {
                                noMatches -> colors.error
                                matchCount > 0 -> colors.onSecondaryContainer
                                else -> colors.onSurfaceVariant
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = if (compact) 130.dp else 220.dp)
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                                .testTag("pdf-search-count")
                                .semantics {
                                    liveRegion = LiveRegionMode.Polite
                                    contentDescription = if (matchCount > 0) "Match ${activeIndex + 1} of $matchCount on page $pageNumber"
                                        else "$status on page $pageNumber"
                                },
                        )
                    }
                }
            }
        }
        }
        }
    }
}

private fun PdfTextMatch.horizontalFocusOffset(
    geometry: PdfGeometry, pageWidth: Dp, viewportWidth: Dp, pageOffset: Dp,
): Dp = (pageOffset + pageWidth * (selection.rects.first().center.x / geometry.displayWidth) - viewportWidth / 2)
    .coerceAtLeast(0.dp)

@Composable
private fun ContinuousPdfPages(viewModel: PdfWorkspaceViewModel, tool: String, query: String,
    color: String, onFocus: () -> Unit, onInspect: () -> Unit, columns: Int = 1,
    matchCase: Boolean = false, wholeWord: Boolean = false, activeSearchMatch: PdfTextMatch? = null,
    searchPageNavigationId: Int = -1,
    onCopySelection: () -> Unit = {}, onAskAi: (() -> Unit)? = null) {
    val state = viewModel.state
    val listState = remember(columns) { LazyListState(firstVisibleItemIndex = state.pageIndex / columns) }
    val density = LocalDensity.current
    var following by remember { mutableStateOf(false) }
    fun trackScroll() {
        listState.layoutInfo.visibleItemsInfo.firstOrNull()?.let { item ->
            val first = item.index * columns
            val active = viewModel.state.pageIndex
            viewModel.trackReadingPosition(PdfReadingPosition(if (active in first until first + columns) active else first,
                (-item.offset.toFloat() / item.size.coerceAtLeast(1)).coerceIn(0f, 1f)), PdfScrollSource.Pdf)
        }
    }
    val reportScroll by rememberUpdatedState(::trackScroll)
    val scrollConnection = remember(listState) {
        object : NestedScrollConnection {
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (!following && consumed.y != 0f) reportScroll()
                return Offset.Zero
            }
        }
    }
    LaunchedEffect(state.pageNavigationId) {
        val position = state.readingPosition
        if (position.pageIndex in 0 until state.pageCount) {
            following = true
            try {
                withFrameNanos { }
                val geometry = state.pageGeometries[position.pageIndex]
                val height = state.displayedPageWidth * (geometry?.let { it.displayHeight / it.displayWidth } ?: 1.414f)
                val offset = with(density) { (height.dp * position.fraction).roundToPx() }
                if (state.scrollSource == PdfScrollSource.Sidebar) listState.scrollToItem(position.pageIndex / columns, offset)
                else listState.animateScrollToItem(position.pageIndex / columns, offset)
            } finally { following = false }
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val width = (maxWidth - 48.dp).coerceAtLeast(100.dp) * state.zoom
        val widthPixels = with(density) { width.roundToPx() }
        val spreadPageWidth = spreadPageWidth(maxWidth) * state.zoom
        val rowWidth = if (columns > 1) maxOf(maxWidth, spreadPageWidth * 2 + spreadGap + 48.dp) else maxOf(maxWidth, width + 48.dp)
        val horizontal = rememberScrollState()
        LaunchedEffect(state.pageNavigationId) {
            if (state.pageNavigationId != searchPageNavigationId) return@LaunchedEffect
            val match = activeSearchMatch ?: return@LaunchedEffect
            val geometry = state.pageGeometries[match.pageIndex] ?: return@LaunchedEffect
            val matchWidth = if (columns > 1) spreadPageWidth else width
            val pageOffset = 24.dp + (matchWidth + spreadGap) * (match.pageIndex % columns)
            withFrameNanos { }
            horizontal.scrollTo(with(density) { match.horizontalFocusOffset(geometry, matchWidth, maxWidth, pageOffset).roundToPx() })
        }
        val zoomTarget = rememberReaderZoomTarget(horizontal,
            contentWidth = if (columns > 1) spreadPageWidth * 2 + spreadGap else width, list = listState,
            verticalPadding = 6.dp)
        Box(Modifier.fillMaxSize().then(zoomTarget).background(MaterialTheme.colorScheme.surfaceContainerHigh).horizontalScroll(horizontal)) {
        LazyColumn(state = listState, modifier = Modifier.width(rowWidth).fillMaxHeight()
            .pointerInput(Unit) { detectTapGestures { viewModel.clearSelectedAnnotation() } }
            .nestedScroll(scrollConnection).testTag("pdf-continuous-pages"),
            userScrollEnabled = state.draft == null,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(top = 6.dp, bottom = 80.dp)) {
            if (columns > 1) items((state.pageCount + columns - 1) / columns, key = { it }) { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spreadGap, Alignment.CenterHorizontally)) {
                    for (index in row * columns until minOf((row + 1) * columns, state.pageCount)) {
                        SpreadPage(viewModel, index, spreadPageWidth, tool, query, color, onFocus, onInspect,
                            matchCase = matchCase, wholeWord = wholeWord, activeSearchMatch = activeSearchMatch,
                            searchPageNavigationId = searchPageNavigationId,
                            onCopySelection = onCopySelection, onAskAi = onAskAi) {
                            listState.dispatchRawDelta(-it.y)
                            horizontal.dispatchRawDelta(-it.x)
                            trackScroll()
                        }
                    }
                }
            } else items(state.pageCount, key = { it }) { index ->
                var attempt by remember { mutableIntStateOf(0) }
                val rendered by produceState<PdfPage?>(null, state.target, index, widthPixels, attempt) {
                    value = viewModel.loadContinuousPage(index, widthPixels)
                }
                val page = rendered
                val aspect = page?.geometry?.let { it.displayHeight / it.displayWidth } ?: 1.414f
                if (page != null) PdfPageCanvas(
                    Modifier.fillMaxWidth().height(width * aspect), viewModel, tool, query,
                    fitPage = false, inkColor = color,
                    onFocus = { viewModel.activateContinuousPage(index, page); onFocus() },
                    onInspect = onInspect, suppliedPage = page, pageIndex = index, continuous = true,
                    fixedPageWidth = width, matchCase = matchCase, wholeWord = wholeWord,
                    activeSearchMatch = activeSearchMatch,
                    searchPageNavigationId = searchPageNavigationId,
                    onCopySelection = onCopySelection, onAskAi = onAskAi,
                    onPan = {
                        listState.dispatchRawDelta(-it.y)
                        horizontal.dispatchRawDelta(-it.x)
                        trackScroll()
                    })
                else Box(Modifier.fillMaxWidth().height(width * aspect), contentAlignment = Alignment.Center) {
                    if (state.error == null) CircularProgressIndicator()
                    else TextButton(onClick = { attempt++ }, modifier = Modifier.interactivePointer()) { Text("Retry page ${index + 1}") }
                }
            }
        }
        }
    }
}

private val spreadGap = 8.dp
private fun spreadPageWidth(available: Dp) = ((available - 48.dp - spreadGap) / 2).coerceAtLeast(80.dp)

@Composable
private fun SpreadPage(viewModel: PdfWorkspaceViewModel, index: Int, width: Dp, tool: String, query: String,
    color: String, onFocus: () -> Unit, onInspect: () -> Unit,
    matchCase: Boolean = false, wholeWord: Boolean = false, activeSearchMatch: PdfTextMatch? = null,
    searchPageNavigationId: Int = -1,
    onCopySelection: () -> Unit = {}, onAskAi: (() -> Unit)? = null,
    onPan: (Offset) -> Unit) {
    val state = viewModel.state
    val widthPixels = with(LocalDensity.current) { width.roundToPx() }
    var attempt by remember { mutableIntStateOf(0) }
    val rendered by produceState<PdfPage?>(null, state.target, index, widthPixels, attempt) {
        value = viewModel.loadContinuousPage(index, widthPixels)
    }
    val page = rendered
    val geometry = page?.geometry ?: state.pageGeometries[index]
    val height = width * (geometry?.let { it.displayHeight / it.displayWidth } ?: 1.414f)
    if (page != null) PdfPageCanvas(Modifier.requiredSize(width, height), viewModel, tool, query,
        fitPage = false, inkColor = color,
        onFocus = { viewModel.activateContinuousPage(index, page); onFocus() },
        onInspect = onInspect, suppliedPage = page, pageIndex = index, continuous = true,
        fixedPageWidth = width, matchCase = matchCase, wholeWord = wholeWord, activeSearchMatch = activeSearchMatch,
        searchPageNavigationId = searchPageNavigationId,
        onCopySelection = onCopySelection, onAskAi = onAskAi, onPan = onPan)
    else Box(Modifier.size(width, height), contentAlignment = Alignment.Center) {
        if (state.error == null) CircularProgressIndicator()
        else TextButton(onClick = { attempt++ }, modifier = Modifier.interactivePointer()) { Text("Retry page ${index + 1}") }
    }
}

@Composable
private fun PagedSpread(viewModel: PdfWorkspaceViewModel, tool: String, query: String,
    color: String, onFocus: () -> Unit, onInspect: () -> Unit,
    matchCase: Boolean = false, wholeWord: Boolean = false, activeSearchMatch: PdfTextMatch? = null,
    searchPageNavigationId: Int = -1,
    onCopySelection: () -> Unit = {}, onAskAi: (() -> Unit)? = null) {
    val state = viewModel.state
    val first = state.pageIndex / 2 * 2
    val density = LocalDensity.current
    BoxWithConstraints(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerHigh)
        .pointerInput(Unit) { detectTapGestures { viewModel.clearSelectedAnnotation() } }) {
        val pageWidth = spreadPageWidth(maxWidth) * state.zoom
        val pageHeight = pageWidth * (state.pageGeometries[first]?.let { it.displayHeight / it.displayWidth } ?: 1.414f)
        val vertical = remember(first) { ScrollState(0) }
        val horizontal = rememberScrollState()
        val viewportWidth = maxWidth
        val zoomTarget = rememberReaderZoomTarget(horizontal, contentWidth = pageWidth * 2 + spreadGap,
            vertical = vertical, horizontalPadding = 0.dp)
        var following by remember { mutableStateOf(false) }
        LaunchedEffect(state.pageNavigationId, first) {
            following = true
            try {
                withFrameNanos { }
                val searchMatch = activeSearchMatch?.takeIf { state.pageNavigationId == searchPageNavigationId }
                val matchHeight = searchMatch?.let { match ->
                    state.pageGeometries[match.pageIndex]?.let { pageWidth * (it.displayHeight / it.displayWidth) }
                } ?: pageHeight
                vertical.scrollTo(with(density) { (matchHeight * viewModel.state.readingPosition.fraction).roundToPx() })
                searchMatch?.takeIf { it.pageIndex in first..first + 1 }?.let { match ->
                    state.pageGeometries[match.pageIndex]?.let { geometry ->
                        val offset = (pageWidth + spreadGap) * (match.pageIndex - first)
                        horizontal.scrollTo(with(density) { match.horizontalFocusOffset(geometry, pageWidth, viewportWidth, offset).roundToPx() })
                    }
                }
            } finally { following = false }
        }
        LaunchedEffect(first, pageHeight) {
            snapshotFlow { vertical.value }.collect { value ->
                if (!following && vertical.isScrollInProgress) {
                    val active = viewModel.state.pageIndex.takeIf { it in first..first + 1 } ?: first
                    viewModel.trackReadingPosition(PdfReadingPosition(active,
                        value / with(density) { pageHeight.toPx() }.coerceAtLeast(1f)), PdfScrollSource.Pdf)
                }
            }
        }
        Box(Modifier.fillMaxSize().then(zoomTarget).verticalScroll(vertical).horizontalScroll(horizontal).padding(top = 24.dp, bottom = 80.dp)) {
            Row(Modifier.widthIn(min = viewportWidth).testTag("pdf-spread-pages"),
                horizontalArrangement = Arrangement.spacedBy(spreadGap, Alignment.CenterHorizontally)) {
                for (index in first until minOf(first + 2, state.pageCount)) {
                    SpreadPage(viewModel, index, pageWidth, tool, query, color, onFocus, onInspect,
                        matchCase = matchCase, wholeWord = wholeWord, activeSearchMatch = activeSearchMatch,
                        searchPageNavigationId = searchPageNavigationId,
                        onCopySelection = onCopySelection, onAskAi = onAskAi) {
                        vertical.dispatchRawDelta(-it.y)
                        horizontal.dispatchRawDelta(-it.x)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
private fun PdfPageCanvas(modifier: Modifier, viewModel: PdfWorkspaceViewModel, tool: String, query: String,
    fitPage: Boolean, inkColor: String, onFocus: () -> Unit, onInspect: () -> Unit,
    suppliedPage: PdfPage? = viewModel.state.page, pageIndex: Int = viewModel.state.pageIndex,
    continuous: Boolean = false, fixedPageWidth: Dp? = null,
    matchCase: Boolean = false, wholeWord: Boolean = false,
    activeSearchMatch: PdfTextMatch? = null,
    searchPageNavigationId: Int = -1,
    onCopySelection: () -> Unit = {}, onAskAi: (() -> Unit)? = null,
    onPan: ((Offset) -> Unit)? = null) {
    val state = viewModel.state
    val editingLocked = false
    val haptic = LocalHapticFeedback.current
    val page = suppliedPage
    BoxWithConstraints(modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)
        .pointerInput(Unit) { detectTapGestures { viewModel.clearSelectedAnnotation() } }) {
        if (page == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (state.loading || state.rendering) CircularProgressIndicator()
                else Column(
                    modifier = Modifier.padding(24.dp).widthIn(max = 560.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    Button(onClick = viewModel::retryOpen, modifier = Modifier.interactivePointer()) { Text("Retry opening PDF") }
                }
            }
        } else {
            val fitWidth = (maxWidth - 48.dp).coerceAtLeast(100.dp)
            val fitHeightWidth = (maxHeight - 48.dp).coerceAtLeast(100.dp) * (page.geometry.displayWidth / page.geometry.displayHeight)
            val pageWidth = fixedPageWidth ?: ((if (fitPage) minOf(fitWidth, fitHeightWidth) else fitWidth) * state.zoom)
            val pageHeight = pageWidth * (page.geometry.displayHeight / page.geometry.displayWidth)
            var canvasWidth by remember { mutableIntStateOf(0) }
            LaunchedEffect(page, canvasWidth) { if (!continuous) viewModel.ensurePageResolution(canvasWidth) }
            val annotations = state.annotations.filter { it.position.pageIndex == pageIndex ||
                (it.position.pageIndex + 1 == pageIndex && it.position.nextPageRects.isNotEmpty()) }
            val matches = remember(page.glyphs, page.geometry, pageIndex, query, matchCase, wholeWord) {
                PdfPageText(pageIndex, page.geometry, page.glyphs).index().findExact(query, matchCase, wholeWord)
            }
            val verticalScroll = remember(state.target, pageIndex) { ScrollState(0) }
            val horizontalScroll = remember(state.target, pageIndex) { ScrollState(0) }
            val zoomTarget = if (continuous) Modifier
                else rememberReaderZoomTarget(horizontalScroll, contentWidth = pageWidth, vertical = verticalScroll)
            val density = LocalDensity.current
            var following by remember { mutableStateOf(false) }
            LaunchedEffect(state.target, state.pageNavigationId, pageIndex) {
                if (!continuous && pageIndex == viewModel.state.readingPosition.pageIndex) {
                    following = true
                    try {
                        withFrameNanos { }
                        verticalScroll.scrollTo(with(density) { (pageHeight * viewModel.state.readingPosition.fraction).roundToPx() })
                        activeSearchMatch?.takeIf { it.pageIndex == pageIndex && state.pageNavigationId == searchPageNavigationId }?.let { match ->
                            horizontalScroll.scrollTo(with(density) { match.horizontalFocusOffset(page.geometry, pageWidth, maxWidth, 24.dp).roundToPx() })
                        }
                    } finally { following = false }
                }
            }
            val reportScroll by rememberUpdatedState {
                viewModel.trackReadingPosition(PdfReadingPosition(pageIndex,
                    verticalScroll.value / with(density) { pageHeight.toPx() }), PdfScrollSource.Pdf)
            }
            val scrollConnection = remember(verticalScroll) {
                object : NestedScrollConnection {
                    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                        if (!continuous && !following && consumed.y != 0f) reportScroll()
                        return Offset.Zero
                    }
                }
            }
            var liveInkStroke by remember(page, pageIndex) { mutableStateOf(emptyList<Offset>()) }
            var liveRect by remember(page, pageIndex) { mutableStateOf<Rect?>(null) }
            var canvasSize by remember { mutableStateOf(IntSize.Zero) }
            var hoveredAnnotationKey by remember { mutableStateOf<String?>(null) }
            val textMeasurer = rememberTextMeasurer()
            val currentAnnotations by rememberUpdatedState(annotations)
            val currentSelection by rememberUpdatedState(state.textSelection)
            val currentSelectedKey by rememberUpdatedState(state.selectedKey)
            var moving by remember(page, pageIndex) { mutableStateOf<AnnotationMove?>(null) }
            var draggingSelectionHandle by remember(pageIndex) { mutableStateOf(false) }
            val nucleusPdfState = LocalNucleusPdfReaderState.current
            fun movableBounds(annotation: ZoteroAnnotation): Rect? {
                val position = annotation.position
                if (annotation.type !in movableAnnotationTypes || position.pageIndex != pageIndex || position.nextPageRects.isNotEmpty()) return null
                val rects = position.boundingRects.map(page.geometry::displayRect).ifEmpty { return null }
                return Rect(rects.minOf { it.left }, rects.minOf { it.top }, rects.maxOf { it.right }, rects.maxOf { it.bottom })
            }
            fun moved(from: Rect, delta: Offset) = from.translate(
                delta.x.coerceIn(minOf(0f, -from.left), maxOf(0f, page.geometry.displayWidth - from.right)),
                delta.y.coerceIn(minOf(0f, -from.top), maxOf(0f, page.geometry.displayHeight - from.bottom)))
            fun resized(from: Rect, delta: Offset, uniform: Boolean): Rect {
                val minSize = 4f
                val maxWidth = maxOf(minSize, page.geometry.displayWidth - from.left)
                val maxHeight = maxOf(minSize, page.geometry.displayHeight - from.top)
                if (!uniform) return Rect(from.left, from.top, from.left + (from.width + delta.x).coerceIn(minSize, maxWidth),
                    from.top + (from.height + delta.y).coerceIn(minSize, maxHeight))
                val low = maxOf(minSize / from.width, minSize / from.height)
                val high = maxOf(low, minOf(maxWidth / from.width, maxHeight / from.height))
                val k = maxOf((from.width + delta.x) / from.width, (from.height + delta.y) / from.height).coerceIn(low, high)
                return Rect(from.left, from.top, from.left + from.width * k, from.top + from.height * k)
            }
            val sidePadding = if (fixedPageWidth != null) 0.dp else 24.dp
            val inset = ((maxWidth - pageWidth - sidePadding * 2) / 2).coerceAtLeast(0.dp)
            val topPad = if (continuous) 0.dp else 24.dp
            val bottomPad = if (continuous) 0.dp else 80.dp
            Box(Modifier.fillMaxSize().then(zoomTarget).then(if (continuous) Modifier else Modifier.nestedScroll(scrollConnection).verticalScroll(verticalScroll))
                .horizontalScroll(horizontalScroll).padding(start = sidePadding, end = sidePadding, top = topPad, bottom = bottomPad)) {
                Box(Modifier.requiredSize(pageWidth, pageHeight).offset(x = inset)) {
                nucleusPdfState?.takeIf { it.pageCount > pageIndex }?.let { readerState ->
                    NucleusPdfPage(
                        state = readerState,
                        pageIndex = pageIndex,
                        modifier = Modifier.fillMaxSize(),
                        selectableText = false,
                        linksEnabled = false,
                    )
                }
                Canvas(Modifier.fillMaxSize().testTag(if (continuous) "workspace-pdf-page-$pageIndex" else "workspace-pdf-page")
                    .pointerHoverIcon(when (tool) { "note", "idea", "ink", "text", "rect" -> PointerIcon.Crosshair; "pan" -> PointerIcon.Hand; else -> PointerIcon.Text })
                    .onSizeChanged {
                        canvasSize = it
                        canvasWidth = it.width
                        if (pageIndex == viewModel.state.pageIndex) viewModel.recordDisplayedPageWidth(with(density) { it.width.toDp().value })
                    }
                    .observePointerEvent(PointerEventType.Move) { event ->
                        val pos = event.changes.firstOrNull()?.position ?: return@observePointerEvent
                        if (canvasSize.width > 0 && canvasSize.height > 0) {
                            val target = Offset(
                                (pos.x / canvasSize.width * page.geometry.displayWidth).coerceIn(0f, page.geometry.displayWidth),
                                (pos.y / canvasSize.height * page.geometry.displayHeight).coerceIn(0f, page.geometry.displayHeight)
                            )
                            val hovered = currentAnnotations.lastOrNull { a ->
                                if (a.type in listOf("highlight", "underline")) {
                                    val posAnn = a.position
                                    val rList = if (posAnn.pageIndex == pageIndex) posAnn.boundingRects else posAnn.nextPageRects
                                    rList.any { page.geometry.displayRect(it).inflate(4f).contains(target) }
                                } else false
                            }
                            hoveredAnnotationKey = hovered?.key
                        }
                    }
                    .observePointerEvent(PointerEventType.Exit) {
                        hoveredAnnotationKey = null
                    }
                    .pointerInput(page, tool, state.canWrite, inkColor, state.adjustingTextRange, state.draft == null, editingLocked) {
                        fun point(offset: Offset) = Offset(
                            (offset.x / size.width * page.geometry.displayWidth).coerceIn(0f, page.geometry.displayWidth),
                            (offset.y / size.height * page.geometry.displayHeight).coerceIn(0f, page.geometry.displayHeight))
                        fun selectAnnotationAt(target: Offset) {
                            currentAnnotations.lastOrNull { annotation ->
                                val position = annotation.position
                                val rects = if (position.pageIndex == pageIndex) position.boundingRects else position.nextPageRects
                                rects.any { page.geometry.displayRect(it).contains(target) }
                            }?.let { annotation -> viewModel.selectAnnotation(annotation, pageIndex); onInspect() }
                                ?: viewModel.clearSelectedAnnotation()
                        }
                        suspend fun AwaitPointerEventScope.transformSelected(down: PointerInputChange): Boolean {
                            if (!state.canWrite || editingLocked || state.adjustingTextRange || state.draft != null) return false
                            val annotation = currentAnnotations.lastOrNull { it.key == currentSelectedKey } ?: return false
                            val from = movableBounds(annotation) ?: return false
                            val touch = point(down.position)
                            val unit = page.geometry.displayWidth / size.width
                            val resizing = annotation.type in scalableAnnotationTypes &&
                                (touch - from.bottomRight).getDistance() < 12.dp.toPx() * unit
                            if (!resizing && !from.inflate(4.dp.toPx() * unit).contains(touch)) return false
                            down.consume()
                            var dragged = false
                            var change = down
                            while (change.pressed) {
                                val event = awaitPointerEvent()
                                change = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (change.isConsumed || event.changes.count { it.pressed } > 1) {
                                    moving = null
                                    return true
                                }
                                if (!dragged && (change.position - down.position).getDistance() > viewConfiguration.touchSlop) dragged = true
                                if (dragged) {
                                    change.consume()
                                    val delta = point(change.position) - touch
                                    moving = AnnotationMove(annotation.key, from,
                                        if (resizing) resized(from, delta, annotation.type == "ink") else moved(from, delta))
                                }
                            }
                            val target = moving?.takeIf { it.key == annotation.key }
                            if (dragged && target != null) viewModel.transformAnnotation(annotation, pageIndex, target.from, target.to)
                            moving = null
                            if (!dragged) selectAnnotationAt(touch)
                            return true
                        }
                        if (editingLocked && (state.draft != null || state.adjustingTextRange)) return@pointerInput
                        if (state.draft != null && state.draft.pageIndex != pageIndex) return@pointerInput
                        if (tool in listOf("select", "highlight", "underline") || state.adjustingTextRange) {
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                onFocus()
                                if (currentEvent.buttons.isSecondaryPressed) {
                                    down.consume()
                                    return@awaitEachGesture
                                }
                                val touchSelection = down.type == PointerType.Touch && !state.adjustingTextRange
                                if (touchSelection) {
                                    // Ordinary finger drags belong to the scroll containers; hold to select text.
                                    val longPress = awaitLongPressOrCancellation(down.id)
                                    if (longPress == null) {
                                        val up = currentEvent.changes.firstOrNull { it.id == down.id }
                                        if (up != null && !up.pressed && !up.isConsumed &&
                                            (up.position - down.position).getDistance() <= viewConfiguration.touchSlop) {
                                            up.consume()
                                            viewModel.clearTextSelection()
                                            selectAnnotationAt(point(up.position))
                                        }
                                        return@awaitEachGesture
                                    }
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                                if (transformSelected(down)) return@awaitEachGesture
                                val selection = currentSelection
                                val startHandle = selection?.rects?.firstOrNull()?.let { Offset(it.left, it.bottom) }
                                val endHandle = selection?.rects?.lastOrNull()?.bottomRight
                                val touchPoint = point(down.position)
                                val handleRadius = 18.dp.toPx() / size.width * page.geometry.displayWidth
                                val startDistance = startHandle?.let { (touchPoint - it).getDistance() } ?: Float.MAX_VALUE
                                val endDistance = endHandle?.let { (touchPoint - it).getDistance() } ?: Float.MAX_VALUE
                                val draggingStart = state.adjustingTextRange && startDistance < handleRadius && startDistance < endDistance
                                val draggingEnd = state.adjustingTextRange && endDistance < handleRadius && !draggingStart
                                val start = if (draggingStart) endHandle!! else if (draggingEnd) startHandle!! else touchPoint
                                down.consume()
                                if (touchSelection) viewModel.selectWord(start)
                                var dragged = false
                                var change = down
                                while (change.pressed) {
                                    val event = awaitPointerEvent()
                                    change = event.changes.firstOrNull { it.id == down.id } ?: break
                                    if (change.isConsumed || event.changes.count { it.pressed } > 1) return@awaitEachGesture
                                    val end = point(change.position)
                                    if ((change.position - down.position).getDistance() > viewConfiguration.touchSlop) dragged = true
                                    if (dragged) {
                                        change.consume()
                                        viewModel.selectText(start, end)
                                    }
                                }
                                if (!change.pressed && dragged && !touchSelection && !state.adjustingTextRange && !editingLocked && tool == "highlight") viewModel.highlightSelectedText()
                                else if (!change.pressed && dragged && !touchSelection && !state.adjustingTextRange && !editingLocked && tool == "underline") viewModel.underlineSelectedText()
                                else if (!dragged && !state.adjustingTextRange && !touchSelection) {
                                    viewModel.clearTextSelection()
                                    selectAnnotationAt(start)
                                }
                            }
                        } else if (tool == "pan") {
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                onFocus()
                                if (currentEvent.buttons.isSecondaryPressed) return@awaitEachGesture
                                if (transformSelected(down)) return@awaitEachGesture
                                down.consume()
                                var dragged = false
                                var change = down
                                while (change.pressed) {
                                    val event = awaitPointerEvent()
                                    change = event.changes.firstOrNull { it.id == down.id } ?: break
                                    if (change.isConsumed || event.changes.count { it.pressed } > 1) return@awaitEachGesture
                                    if (!dragged && (change.position - down.position).getDistance() > viewConfiguration.touchSlop) dragged = true
                                    if (dragged) {
                                        val amount = change.position - change.previousPosition
                                        change.consume()
                                        horizontalScroll.dispatchRawDelta(-amount.x)
                                        if (onPan != null) onPan(amount) else {
                                            verticalScroll.dispatchRawDelta(-amount.y)
                                            viewModel.trackReadingPosition(PdfReadingPosition(pageIndex,
                                                verticalScroll.value / with(density) { pageHeight.toPx() }), PdfScrollSource.Pdf)
                                        }
                                    }
                                }
                                if (!dragged) selectAnnotationAt(point(down.position))
                            }
                        } else if (tool == "ink" && state.canWrite && !editingLocked) {
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                onFocus()
                                down.consume()
                                liveInkStroke = listOf(point(down.position))
                                var dragged = false
                                var change = down
                                while (change.pressed) {
                                    val event = awaitPointerEvent()
                                    change = event.changes.firstOrNull { it.id == down.id } ?: break
                                    if (change.isConsumed || event.changes.count { it.pressed } > 1) {
                                        liveInkStroke = emptyList()
                                        return@awaitEachGesture
                                    }
                                    if ((change.position - down.position).getDistance() > viewConfiguration.touchSlop) {
                                        dragged = true
                                        change.consume()
                                        liveInkStroke = liveInkStroke + point(change.position)
                                    }
                                }
                                if (dragged && liveInkStroke.size > 1) viewModel.addInkPath(liveInkStroke, inkColor)
                                liveInkStroke = emptyList()
                            }
                        } else if (tool == "rect" && state.canWrite && !editingLocked) {
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                onFocus()
                                down.consume()
                                val start = point(down.position)
                                var dragged = false
                                var change = down
                                while (change.pressed) {
                                    val event = awaitPointerEvent()
                                    change = event.changes.firstOrNull { it.id == down.id } ?: break
                                    if (change.isConsumed || event.changes.count { it.pressed } > 1) {
                                        liveRect = null
                                        return@awaitEachGesture
                                    }
                                    if ((change.position - down.position).getDistance() > viewConfiguration.touchSlop) dragged = true
                                    if (dragged) {
                                        change.consume()
                                        val end = point(change.position)
                                        liveRect = Rect(minOf(start.x, end.x), minOf(start.y, end.y), maxOf(start.x, end.x), maxOf(start.y, end.y))
                                    }
                                }
                                val drawn = liveRect
                                liveRect = null
                                if (dragged && drawn != null) {
                                    viewModel.activateContinuousPage(pageIndex, page)
                                    viewModel.beginRectangle(drawn)
                                } else selectAnnotationAt(start)
                            }
                        } else {
                            detectTapGestures { offset ->
                                onFocus()
                                val p = point(offset)
                                if (tool == "text" && state.canWrite && !editingLocked) viewModel.beginText(p)
                                else if (tool in listOf("note", "idea") && state.canWrite && !editingLocked) {
                                    val x = p.x.coerceAtMost((page.geometry.displayWidth - 22f).coerceAtLeast(0f))
                                    val y = p.y.coerceAtMost((page.geometry.displayHeight - 22f).coerceAtLeast(0f))
                                    val rect = Rect(x, y, x + 22f, y + 22f)
                                    if (tool == "idea") viewModel.beginIdea(rect) else viewModel.beginNote(rect)
                                } else selectAnnotationAt(p)
                            }
                        }
                    }) {
                    val sx = size.width / page.geometry.displayWidth
                    val sy = size.height / page.geometry.displayHeight
                    fun drawBounds(rect: Rect, color: Color, outline: Boolean = false) {
                        val offset = Offset(rect.left * sx, rect.top * sy)
                        val extent = Size(rect.width * sx, rect.height * sy)
                        if (outline) drawRect(color, offset, extent, style = Stroke(2.dp.toPx()))
                        else drawRect(color, offset, extent)
                    }
                    fun drawInkPath(values: List<Float>, color: Color, width: Float, pdfCoordinates: Boolean) {
                        if (values.size < 2 || values.size % 2 != 0 || values.any { !it.isFinite() }) return
                        val points = values.chunked(2).map { pair ->
                            val display = if (pdfCoordinates) page.geometry.toDisplay(Offset(pair[0], pair[1]))
                            else Offset(pair[0], pair[1])
                            Offset(display.x * sx, display.y * sy)
                        }
                        if (points.size == 1) {
                            drawCircle(color, radius = width * (sx + sy) / 4f, center = points.single())
                            return
                        }
                        val path = Path().apply {
                            moveTo(points.first().x, points.first().y)
                            points.drop(1).forEach { lineTo(it.x, it.y) }
                        }
                        drawPath(path, color, style = Stroke(
                            width = width * (sx + sy) / 2f,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        ))
                    }
                    fun drawAnnotation(annotation: ZoteroAnnotation) {
                        if (state.adjustingTextRange && annotation.key == state.draft?.editing?.key) return
                        val position = annotation.position
                        val rects = if (position.pageIndex == pageIndex) position.boundingRects else position.nextPageRects
                        val color = annotationColor(annotation.color)
                        if (annotation.type == "ink") {
                            position.paths.forEach { drawInkPath(it, color, position.width, pdfCoordinates = true) }
                        }
                        val displayRects = rects.map(page.geometry::displayRect)
                        displayRects.forEach { rect ->
                            when (annotation.type) {
                                "underline" -> {
                                    val underlineY = rect.bottom * sy + 2.dp.toPx()
                                    drawLine(color.copy(alpha = 0.82f), Offset(rect.left * sx, underlineY), Offset(rect.right * sx, underlineY), 2.dp.toPx())
                                }
                                "image" -> drawRect(color, Offset(rect.left * sx, rect.top * sy), Size(rect.width * sx, rect.height * sy),
                                    style = Stroke(maxOf(1.5f, 2f * (sx + sy) / 2f)))
                                "text" -> drawText(textMeasurer, annotation.comment, Offset(rect.left * sx, rect.top * sy),
                                    style = TextStyle(color = annotationMarkerColor(annotation.color), fontSize = (position.fontSize * sy).toSp()),
                                    size = Size(((page.geometry.displayWidth - rect.left) * sx).coerceAtLeast(1f),
                                        ((page.geometry.displayHeight - rect.top) * sy).coerceAtLeast(1f)))
                                "note", "ink" -> Unit
                                else -> drawBounds(rect, color.copy(alpha = 0.30f))
                            }
                        }
                        if (annotation.key == state.selectedKey && displayRects.isNotEmpty()) {
                            val pad = 3.dp.toPx()
                            val left = displayRects.minOf { it.left } * sx - pad
                            val top = displayRects.minOf { it.top } * sy - pad
                            val right = displayRects.maxOf { it.right } * sx + pad
                            val bottom = displayRects.maxOf { it.bottom } * sy + pad
                            drawRect(lerp(color, Color.Black, 0.35f), Offset(left, top), Size(right - left, bottom - top),
                                style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))))
                        }
                    }
                    annotations.forEach { annotation ->
                        val move = moving?.takeIf { it.key == annotation.key }
                        if (move == null) drawAnnotation(annotation) else {
                            val kx = move.to.width / move.from.width
                            val ky = move.to.height / move.from.height
                            withTransform({
                                translate(move.to.left * sx - move.from.left * sx * kx, move.to.top * sy - move.from.top * sy * ky)
                                scale(kx, ky, pivot = Offset.Zero)
                            }) { drawAnnotation(annotation) }
                        }
                    }
                    if (state.canWrite && state.draft == null && !editingLocked) annotations.firstOrNull {
                        it.key == state.selectedKey && it.type in scalableAnnotationTypes
                    }?.let { selected ->
                        val bounds = moving?.takeIf { it.key == selected.key }?.to ?: movableBounds(selected)
                        if (bounds != null) {
                            val handle = Offset(bounds.right * sx, bounds.bottom * sy)
                            val half = 5.dp.toPx()
                            drawRect(Color.White, handle - Offset(half + 1.dp.toPx(), half + 1.dp.toPx()),
                                Size((half + 1.dp.toPx()) * 2, (half + 1.dp.toPx()) * 2))
                            drawRect(lerp(annotationColor(selected.color), Color.Black, 0.35f), handle - Offset(half, half), Size(half * 2, half * 2))
                        }
                    }
                    state.draft?.takeIf { it.pageIndex == pageIndex && it.type == "image" && it.editing == null }?.let { draft ->
                        draft.rects.map(page.geometry::displayRect).forEach {
                            drawRect(annotationColor(draft.color), Offset(it.left * sx, it.top * sy), Size(it.width * sx, it.height * sy), style = Stroke(2.dp.toPx()))
                        }
                    }
                    liveRect?.let {
                        drawRect(annotationColor(inkColor), Offset(it.left * sx, it.top * sy), Size(it.width * sx, it.height * sy),
                            style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))))
                    }
                    state.draft?.takeIf { it.pageIndex == pageIndex && it.type == "ink" && it.editing == null }?.let { draft ->
                        draft.paths.forEach { drawInkPath(it, annotationColor(draft.color), draft.width, pdfCoordinates = true) }
                    }
                    if (liveInkStroke.size > 1) {
                        drawInkPath(liveInkStroke.flatMap { listOf(it.x, it.y) }, annotationColor(inkColor), state.draft?.width ?: state.inkWidth, pdfCoordinates = false)
                    }
                    matches.forEach { match ->
                        val active = match == activeSearchMatch
                        match.selection.rects.forEach { rect ->
                            drawBounds(rect, (if (active) Color(0xFFFF9800) else Color(0xFFFFD54F)).copy(alpha = if (active) 0.55f else 0.3f))
                            if (active) drawRect(Color(0xFFB45309), Offset(rect.left * sx, rect.top * sy),
                                Size(rect.width * sx, rect.height * sy), style = Stroke(1.5.dp.toPx()))
                        }
                    }
                    if (pageIndex == state.pageIndex) {
                        state.textSelection?.rects?.forEach {
                            val color = annotationColor(if (state.adjustingTextRange) state.draft!!.color else inkColor)
                            if (state.adjustingTextRange && state.draft?.type == "underline") {
                                val underlineY = it.bottom * sy + 2.dp.toPx()
                                drawLine(color.copy(alpha = 0.82f), Offset(it.left * sx, underlineY), Offset(it.right * sx, underlineY), 2.dp.toPx())
                            } else drawBounds(it, color.copy(alpha = 0.30f))
                        }
                    }
                }
                annotations.filter { it.type == "note" || it.comment.isNotBlank() && it.type !in listOf("ink", "text") }.forEach { annotation ->
                    val coordinates = (if (annotation.position.pageIndex == pageIndex) annotation.position.rects
                        else annotation.position.nextPageRects).lastOrNull()
                    if (coordinates != null) {
                        val rect = page.geometry.displayRect(coordinates)
                        val x = if (annotation.type == "note") rect.left else rect.right
                        val ghost = moving?.takeIf { it.key == annotation.key }?.to
                        val icon = if (annotation.isIdea) painterResource(Res.drawable.filled_lightbulb) else if (annotation.type == "note") painterResource(Res.drawable.filled_edit_note) else painterResource(Res.drawable.filled_chat_bubble_outline)
                        val isAttachedToText = annotation.type in listOf("highlight", "underline")
                        val isHoveredOrSelected = !isAttachedToText || annotation.key == hoveredAnnotationKey || annotation.key == state.selectedKey
                        val iconAlpha by animateFloatAsState(
                            targetValue = if (isHoveredOrSelected) (if (ghost != null) 0.35f else 1f) else 0f,
                            animationSpec = tween(160),
                            label = "noteIconAlpha"
                        )
                        if (ghost != null && isHoveredOrSelected) Icon(icon, null, Modifier.offset(
                            x = (pageWidth * (ghost.left / page.geometry.displayWidth)).coerceIn(0.dp, (pageWidth - 16.dp).coerceAtLeast(0.dp)),
                            y = (pageHeight * (ghost.top / page.geometry.displayHeight) - 18.dp).coerceIn(0.dp, (pageHeight - 16.dp).coerceAtLeast(0.dp)))
                            .size(16.dp), tint = annotationMarkerColor(annotation.color))
                        val draggable = annotation.type == "note" && state.canWrite && state.draft == null && !editingLocked
                        val pinBounds = if (draggable) movableBounds(annotation) else null
                        Box(
                            modifier = Modifier.offset(
                                x = (pageWidth * (x / page.geometry.displayWidth)).coerceIn(0.dp, (pageWidth - 16.dp).coerceAtLeast(0.dp)),
                                y = (pageHeight * (rect.top / page.geometry.displayHeight) - 18.dp).coerceIn(0.dp, (pageHeight - 16.dp).coerceAtLeast(0.dp)))
                                .size(16.dp).clip(RoundedCornerShape(3.dp)).interactivePointer()
                                .observePointerEvent(PointerEventType.Enter) { hoveredAnnotationKey = annotation.key }
                                .observePointerEvent(PointerEventType.Exit) { if (hoveredAnnotationKey == annotation.key) hoveredAnnotationKey = null }
                                .then(if (pinBounds == null) Modifier else Modifier.pointerInput(annotation.key, pinBounds) {
                                var delta = Offset.Zero
                                val unit = page.geometry.displayWidth / with(density) { pageWidth.toPx() }
                                detectDragGestures(
                                    onDragStart = { delta = Offset.Zero; onFocus(); viewModel.selectAnnotation(annotation, pageIndex, reveal = false) },
                                    onDragEnd = {
                                        moving?.takeIf { it.key == annotation.key }?.let {
                                            viewModel.transformAnnotation(annotation, pageIndex, it.from, it.to)
                                        }
                                        moving = null
                                    },
                                    onDragCancel = { moving = null }
                                ) { change, amount ->
                                    change.consume()
                                    delta += amount * unit
                                    moving = AnnotationMove(annotation.key, pinBounds, moved(pinBounds, delta))
                                }
                            })
                                .graphicsLayer { alpha = iconAlpha }
                                .semantics { selected = state.selectedKey == annotation.key }
                                .clickable(role = Role.Button) { onFocus(); viewModel.selectAnnotation(annotation, pageIndex); onInspect() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(icon, "Open annotation ${annotation.key}", Modifier.size(16.dp), tint = annotationMarkerColor(annotation.color))
                        }
                    }
                }
                if (!draggingSelectionHandle) PdfSelectionActions(
                    viewModel = viewModel,
                    page = page,
                    pageWidth = pageWidth,
                    pageHeight = pageHeight,
                    pageIndex = pageIndex,
                    onCopySelection = onCopySelection,
                    onAskAi = onAskAi,
                    onInspect = onInspect,
                )
                state.textSelection?.takeIf {
                    pageIndex == state.pageIndex && !editingLocked &&
                        (state.draft == null || state.adjustingTextRange)
                }?.let { selection ->
                    PdfSelectionHandles(
                        selection = selection,
                        geometry = page.geometry,
                        pageWidth = pageWidth,
                        pageHeight = pageHeight,
                        onSelect = viewModel::selectText,
                        onDraggingChange = { draggingSelectionHandle = it },
                    )
                }
                }
            }
        }
    }
}

@Composable
private fun PdfSelectionActions(
    viewModel: PdfWorkspaceViewModel,
    page: PdfPage,
    pageWidth: Dp,
    pageHeight: Dp,
    pageIndex: Int,
    onCopySelection: () -> Unit,
    onAskAi: (() -> Unit)?,
    onInspect: () -> Unit,
) {
    val state = viewModel.state
    val selection = state.textSelection?.takeIf { it.rects.isNotEmpty() && pageIndex == state.pageIndex }
        ?: return
    val selectionLeft = selection.rects.minOf { it.left } * pageWidth.value / page.geometry.displayWidth
    val selectionRight = selection.rects.maxOf { it.right } * pageWidth.value / page.geometry.displayWidth
    val selectionBottom = selection.rects.maxOf { it.bottom } * pageHeight.value / page.geometry.displayHeight
    val toolbarWidth = 280.dp
    val x = ((selectionLeft + selectionRight) / 2f - toolbarWidth.value / 2f)
        .coerceIn(0f, (pageWidth - toolbarWidth).coerceAtLeast(0.dp).value).dp
    val y = (selectionBottom + 36f).coerceAtMost((pageHeight - 56.dp).coerceAtLeast(0.dp).value).dp

    Surface(
        Modifier.offset(x = x, y = y).widthIn(max = pageWidth).testTag("pdf-selection-actions"),
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 8.dp,
        tonalElevation = 6.dp,
    ) {
        Row(
            Modifier.padding(horizontal = 6.dp).horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (state.adjustingTextRange) {
                Text(
                    "Adjust selection",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
                TextButton(onClick = viewModel::cancelDraft, modifier = Modifier.interactivePointer()) { Text("Cancel") }
                TextButton(onClick = viewModel::finishTextRange, modifier = Modifier.interactivePointer()) { Text("Edit note") }
                TextButton(
                    enabled = state.canWrite,
                    onClick = viewModel::saveDraft,
                    modifier = Modifier.interactivePointer(enabled = state.canWrite),
                ) { Text("Save") }
            } else {
                TextButton(onClick = onCopySelection, modifier = Modifier.interactivePointer()) {
                    Icon(painterResource(Res.drawable.filled_content_copy), null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Copy")
                }
                TextButton(
                    enabled = state.canWrite,
                    onClick = viewModel::highlightSelectedText,
                    modifier = Modifier.interactivePointer(enabled = state.canWrite),
                ) {
                    Icon(painterResource(Res.drawable.filled_highlight), null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Highlight")
                }
                TextButton(
                    enabled = state.canWrite,
                    onClick = viewModel::underlineSelectedText,
                    modifier = Modifier.interactivePointer(enabled = state.canWrite),
                ) {
                    Icon(painterResource(Res.drawable.filled_format_underlined), null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Underline")
                }
                if (onAskAi != null) {
                    TextButton(
                        onClick = { onInspect(); onAskAi() },
                        modifier = Modifier.interactivePointer().testTag("pdf-ask-ai"),
                    ) {
                        Icon(painterResource(Res.drawable.filled_auto_awesome), null, Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Ask AI")
                    }
                }
                IconButton(onClick = viewModel::clearTextSelection, modifier = Modifier.interactivePointer()) {
                    Icon(painterResource(Res.drawable.filled_close), "Clear text selection")
                }
            }
        }
    }
}

@Composable
fun AnnotationList(viewModel: PdfWorkspaceViewModel, modifier: Modifier = Modifier, onSelected: () -> Unit = {}, ideasOnly: Boolean? = false,
    headerCollapsed: Boolean = false, allowEditing: Boolean = true) {
    val state = viewModel.state
    var deleting by remember { mutableStateOf<ZoteroAnnotation?>(null) }
    var query by remember(state.target) { mutableStateOf("") }
    var currentPageOnly by remember { mutableStateOf(false) }
    val category = state.annotations.filter { ideasOnly == null || it.isIdea == ideasOnly }
    val filtered = category.filter {
        (!currentPageOnly || it.position.pageIndex == state.pageIndex || it.position.pageIndex + 1 == state.pageIndex && it.position.nextPageRects.isNotEmpty()) &&
            (query.isBlank() || it.text.contains(query, true) || it.comment.contains(query, true))
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AnimatedVisibility(visible = !headerCollapsed) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (ideasOnly == true) "Page ideas" else "Annotations",
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f))
                    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceContainerHighest) {
                        Text("${category.size}", Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        painterResource(when {
                            state.syncing -> Res.drawable.filled_sync
                            state.needsRefresh || state.pendingWrites > 0 -> Res.drawable.filled_error_outline
                            state.localTitle != null -> Res.drawable.filled_description
                            else -> Res.drawable.filled_cloud_done
                        }),
                        null, Modifier.size(14.dp),
                        tint = if (state.needsRefresh) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        state.annotationSaveStatus,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (state.needsRefresh) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    placeholder = { Text(if (ideasOnly == true) "Search page ideas" else "Search annotations",
                        style = MaterialTheme.typography.bodyMedium) },
                    leadingIcon = { Icon(painterResource(Res.drawable.filled_search), null, Modifier.size(18.dp)) },
                    trailingIcon = if (query.isNotEmpty()) {
                        { IconButton(onClick = { query = "" }, modifier = Modifier.interactivePointer()) {
                            Icon(painterResource(Res.drawable.filled_close), "Clear annotation search", Modifier.size(18.dp))
                        } }
                    } else null,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    shape = RoundedCornerShape(10.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReaderToolChip(currentPageOnly, { currentPageOnly = !currentPageOnly },
                        label = { Text("This page") },
                        leadingIcon = if (currentPageOnly) { { Icon(painterResource(Res.drawable.filled_check), null, Modifier.size(16.dp)) } } else null,
                        shape = RoundedCornerShape(8.dp), modifier = Modifier.interactivePointer())
                    Spacer(Modifier.weight(1f))
                    FilledTonalButton(enabled = state.canWrite && allowEditing, onClick = {
                        if (ideasOnly == true) viewModel.beginIdea(Rect(24f, 24f, 46f, 46f)) else viewModel.beginNote()
                        onSelected()
                    }, shape = RoundedCornerShape(10.dp), contentPadding = PaddingValues(horizontal = 12.dp),
                        modifier = Modifier.interactivePointer(enabled = state.canWrite && allowEditing)) {
                        Icon(painterResource(Res.drawable.filled_add), null, Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (ideasOnly == true) "Idea" else "Note")
                    }
                }
            }
        }
        }
        if (filtered.isNotEmpty()) {
            Text("${filtered.size} ${if (ideasOnly == true) "ideas" else "annotations"}${if (currentPageOnly) " on this page" else " in this paper"}",
                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (filtered.isEmpty()) {
            Column(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(painterResource(Res.drawable.filled_edit_note), null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(if (category.isEmpty()) if (ideasOnly == true) "Capture a page idea" else "Make this paper your own"
                    else if (ideasOnly == true) "No matching page ideas" else "No matching annotations",
                    style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 12.dp))
                Text(if (category.isEmpty()) if (ideasOnly == true) "Use the Idea tool to place an idea on the PDF."
                    else "Select text to highlight, draw with the pen, or leave a note on the page." else "Try another search or turn off the page filter.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp))
            }
        }
        AnnotationTimeline(viewModel, filtered, currentPageOnly, Modifier.weight(1f)) { annotation, pageIndex ->
                val selected = state.selectedKey == annotation.key
                val noteColor = annotationColor(annotation.color)
                val accentColor = lerp(noteColor, MaterialTheme.colorScheme.onSurface, 0.3f)
                OutlinedCard(onClick = { viewModel.selectAnnotation(annotation, pageIndex); onSelected() },
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .testTag("annotation-card-${annotation.key}-$pageIndex")
                        .semantics { this.selected = selected }.interactivePointer(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                        contentColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface),
                    border = BorderStroke(if (selected) 2.dp else 1.dp,
                        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)) {
                    Box(Modifier.fillMaxWidth().height(3.dp).background(noteColor))
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                Modifier.size(28.dp).background(lerp(MaterialTheme.colorScheme.surface, noteColor, 0.16f), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(when {
                                    annotation.isIdea -> painterResource(Res.drawable.filled_lightbulb)
                                    annotation.type == "note" -> painterResource(Res.drawable.filled_edit_note)
                                    annotation.type == "underline" -> painterResource(Res.drawable.filled_format_underlined)
                                    annotation.type == "ink" -> painterResource(Res.drawable.filled_gesture)
                                    annotation.type == "text" -> painterResource(Res.drawable.filled_title)
                                    annotation.type == "image" -> painterResource(Res.drawable.filled_crop_square)
                                    else -> painterResource(Res.drawable.filled_highlight)
                                }, null, Modifier.size(16.dp), tint = accentColor)
                            }
                            Text("Page ${annotation.pageLabel.ifBlank { (annotation.position.pageIndex + 1).toString() }} - ${annotationTypeLabel(annotation)}",
                                style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (annotation.isAiGenerated) Icon(painterResource(Res.drawable.filled_auto_awesome), "Added by AI", Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.primary)
                            if (selected) Icon(painterResource(Res.drawable.filled_check_circle), "Selected annotation", Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary)
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (annotation.type == "ink") Text("Freehand drawing - ${formatSize(annotation.position.width)}", style = MaterialTheme.typography.bodySmall)
                            if (annotation.type == "image" && annotation.comment.isBlank()) Text("Rectangle area", style = MaterialTheme.typography.bodySmall)
                            if (annotation.comment.isNotBlank()) {
                                Box(
                                    Modifier.fillMaxWidth()
                                        .then(if (selected) Modifier else Modifier.heightIn(max = 150.dp))
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(noteColor.copy(alpha = 0.08f))
                                        .padding(8.dp)
                                ) {
                                    ChatMarkdown(annotation.comment, Modifier.fillMaxWidth())
                                }
                            }
                            else if (annotation.type !in listOf("ink", "image")) Text("No note", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (annotation.type !in listOf("highlight", "note", "underline", "ink", "text", "image")) {
                                Text("This annotation type is managed in Zotero; its content is preserved.", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    HorizontalDivider(Modifier.padding(horizontal = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.End) {
                        val actionsEnabled = state.canWrite && state.draft == null && allowEditing
                        IconButton(enabled = actionsEnabled, onClick = { viewModel.editAnnotation(annotation); onSelected() },
                            modifier = Modifier.interactivePointer(enabled = actionsEnabled)) {
                            Icon(painterResource(Res.drawable.filled_edit), "Edit annotation ${annotation.key}", Modifier.size(18.dp),
                                tint = if (actionsEnabled) MaterialTheme.colorScheme.onSurfaceVariant else LocalContentColor.current)
                        }
                        IconButton(enabled = actionsEnabled, onClick = { deleting = annotation },
                            modifier = Modifier.interactivePointer(enabled = actionsEnabled)) {
                            Icon(painterResource(Res.drawable.filled_delete_outline), "Delete annotation ${annotation.key}", Modifier.size(18.dp),
                                tint = if (actionsEnabled) MaterialTheme.colorScheme.onSurfaceVariant else LocalContentColor.current)
                        }
                    }
                }
            }
    }
    deleting?.let { annotation ->
        AlertDialog(onDismissRequest = { deleting = null },
            title = { Text(if (state.localTitle != null) "Delete local annotation?" else "Delete annotation from Zotero?") },
            text = { Text(if (state.localTitle != null) "This removes the annotation saved on this device. The PDF file is not changed. This cannot be undone here."
                else "This also removes it from Zotero after desktop sync. This cannot be undone here.") },
            confirmButton = { TextButton(enabled = state.canWrite && allowEditing, onClick = { viewModel.deleteAnnotation(annotation); deleting = null }, modifier = Modifier.interactivePointer(enabled = state.canWrite && allowEditing)) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { deleting = null }, modifier = Modifier.interactivePointer()) { Text("Cancel") } })
    }
}
