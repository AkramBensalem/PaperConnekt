package com.paperconnekt.idea

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akram.bensalem.paper.connekt.feature.assistant.*
import com.akram.bensalem.paper.connekt.feature.assistant.component.ChatMarkdown
import com.mikepenz.markdown.coil3.Coil3ImageTransformerImpl
import com.akram.bensalem.paper.connekt.shared.zotero.model.zotero.ZoteroAnnotation
import org.jetbrains.jewel.ui.component.DefaultButton
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextArea
import org.jetbrains.jewel.ui.component.TextField
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import kotlin.math.roundToInt

private enum class ReaderTool { Select, Pan, Pen, Note, Text, Area, Idea, Move }
private val annotationColors = listOf("#ffd400", "#ff6666", "#5fb236", "#2ea8e5", "#a28ae5", "#e56eee")

@Composable
internal fun NativePdfWorkspace(features: NativeFeatures) {
    val vm = features.reader
    val state = vm.state
    var panel by remember { mutableStateOf("Reader") }
    var overlay by remember { mutableStateOf<ReaderOverlay?>(null) }
    val onAssistant: () -> Unit = { overlay = ReaderOverlay.Assistant }
    val switchPanel: (Boolean) -> Unit = { markdown ->
        panel = if (markdown) "Markdown" else "Reader"
        if (markdown) vm.showMarkdown() else vm.showPdf()
    }
    var bottomBarHeight by remember { mutableIntStateOf(0) }
    var tool by remember { mutableStateOf(ReaderTool.Select) }
    var continuous by remember { mutableStateOf(true) }
    var editInk by remember { mutableStateOf(false) }
    var headerCollapsed by remember { mutableStateOf(false) }
    var annotationKey by remember { mutableStateOf<String?>(null) }
    val pageNumber = rememberTextFieldState()
    var error by remember { mutableStateOf<String?>(null) }
    val clipboard = LocalClipboardManager.current
    val canAnnotate = (state.target != null || state.localStorageKey != null) && state.canWrite && state.draft == null
    val selectionActions = listOf(
        NativeMenuAction("Highlight text", NativeIcon.Highlight, enabled = canAnnotate && state.textSelection != null,
            swatch = annotationColor(state.annotationColor), action = vm::highlightSelectedText),
        NativeMenuAction("Underline text", NativeIcon.Underline, enabled = canAnnotate && state.textSelection != null, action = vm::underlineSelectedText),
        NativeMenuAction("Copy", NativeIcon.Copy, enabled = state.textSelection != null) {
            state.textSelection?.let { clipboard.setText(AnnotatedString(it.text)) }
        },
        NativeMenuAction("Ask AI about selection", NativeIcon.Assistant, enabled = (state.target != null || state.localStorageKey != null) && state.textSelection != null) {
            features.agent.askAbout(state.textSelection, state.pageIndex); onAssistant()
        },
        NativeMenuAction("Select all text", NativeIcon.Select, action = vm::selectAllText),
        NativeMenuAction("Clear selection", NativeIcon.Close, enabled = state.textSelection != null, action = vm::clearTextSelection),
    )
    var showSearch by remember { mutableStateOf(false) }
    val searchField = rememberTextFieldState()
    var searchQuery by remember { mutableStateOf("") }
    var matchCase by remember { mutableStateOf(false) }
    var wholeWord by remember { mutableStateOf(false) }
    var searchMatches by remember { mutableStateOf<List<PdfTextMatch>>(emptyList()) }
    var activeMatch by remember { mutableIntStateOf(-1) }
    var searching by remember { mutableStateOf(false) }
    var searchFocusRequest by remember { mutableIntStateOf(0) }
    var pageFocusRequest by remember { mutableIntStateOf(0) }
    var searchFieldFocused by remember { mutableStateOf(false) }
    var pageFieldFocused by remember { mutableStateOf(false) }
    val editingField = searchFieldFocused || pageFieldFocused
    val searchFocus = remember { FocusRequester() }
    val pageFocus = remember { FocusRequester() }
    LaunchedEffect(searchField) { snapshotFlow { searchField.text.toString() }.collect { searchQuery = it } }
    val revealMatch: (Int) -> Unit = { index ->
        searchMatches.getOrNull(index)?.let { match ->
            activeMatch = index
            val height = vm.state.pageGeometries[match.pageIndex]?.displayHeight
            val top = match.selection.rects.minOfOrNull { it.top }
            val fraction = if (height != null && top != null && height > 0f) (top / height - 0.12f).coerceIn(0f, 1f) else 0f
            vm.navigateToPosition(PdfReadingPosition(match.pageIndex, fraction))
        }
    }
    val stepMatch: (Int) -> Unit = { direction ->
        if (searchMatches.isNotEmpty()) revealMatch(((activeMatch + direction) % searchMatches.size + searchMatches.size) % searchMatches.size)
    }
    val openSearch: () -> Unit = {
        if (panel != "Reader") { panel = "Reader"; vm.showPdf() }
        showSearch = true
        searchFocusRequest++
    }
    val closeSearch: () -> Unit = {
        showSearch = false
        searchField.setTextAndPlaceCursorAtEnd("")
        searchMatches = emptyList()
        activeMatch = -1
    }
    val goToTypedPage: () -> Unit = {
        val index = pageNumber.text.toString().trim().toIntOrNull()
        if (index == null || index !in 1..state.readerPageCount) error = "Choose a page between 1 and ${state.readerPageCount}."
        else { error = null; vm.goToPage(index - 1) }
    }
    LaunchedEffect(searchQuery, matchCase, wholeWord, state.pageCount, state.target?.attachment?.key, state.localTitle) {
        searchMatches = emptyList()
        activeMatch = -1
        val query = searchQuery
        val pages = vm.state.pageCount
        if (query.isBlank() || pages == 0) { searching = false; return@LaunchedEffect }
        delay(250)
        searching = true
        try {
            val start = vm.state.pageIndex.coerceIn(0 until pages)
            val found = mutableListOf<PdfTextMatch>()
            // Scans from the current page and wraps around, so the first match is the nearest one ahead.
            for (offset in 0 until pages) {
                val index = (start + offset) % pages
                val text = vm.pageText(index) ?: continue
                val matches = withContext(Dispatchers.Default) { text.index().findExact(query, matchCase, wholeWord) }
                if (matches.isEmpty()) continue
                found += matches
                searchMatches = found.toList()
                if (activeMatch < 0) revealMatch(0)
            }
        } finally {
            searching = false
        }
    }
    LaunchedEffect(searchFocusRequest) {
        if (searchFocusRequest > 0) { withFrameNanos { }; runCatching { searchFocus.requestFocus() } }
    }
    LaunchedEffect(pageFocusRequest) {
        if (pageFocusRequest > 0) { withFrameNanos { }; runCatching { pageFocus.requestFocus() } }
    }
    val readerOpen = (state.target != null || state.localStorageKey != null) || state.localTitle != null
    val idle = state.draft == null && annotationKey == null
    NativeShortcutHandler(NativeShortcut.Find, readerOpen && idle, openSearch)
    NativeShortcutHandler(NativeShortcut.FindNext, showSearch && searchMatches.isNotEmpty()) { stepMatch(1) }
    NativeShortcutHandler(NativeShortcut.FindPrevious, showSearch && searchMatches.isNotEmpty()) { stepMatch(-1) }
    NativeShortcutHandler(NativeShortcut.GoToPage, readerOpen && idle) { pageFocusRequest++ }
    NativeShortcutHandler(NativeShortcut.ZoomIn, readerOpen) { vm.zoom(vm.state.zoom * 1.25f) }
    NativeShortcutHandler(NativeShortcut.ZoomOut, readerOpen) { vm.zoom(vm.state.zoom / 1.25f) }
    NativeShortcutHandler(NativeShortcut.ZoomReset, readerOpen) { vm.zoom(1f) }
    NativeShortcutHandler(NativeShortcut.Copy, !editingField && idle && state.textSelection != null) {
        vm.state.textSelection?.let { clipboard.setText(AnnotatedString(it.text)) }
    }
    NativeShortcutHandler(NativeShortcut.SelectAll, !editingField && panel == "Reader" && state.page != null && idle, vm::selectAllText)
    NativeShortcutHandler(NativeShortcut.PreviousPage, !editingField && readerOpen && idle && state.pageIndex > 0) {
        vm.goToPage(vm.state.pageIndex - 1)
    }
    NativeShortcutHandler(NativeShortcut.NextPage, !editingField && readerOpen && idle && state.pageIndex + 1 < state.readerPageCount) {
        vm.goToPage(vm.state.pageIndex + 1)
    }
    NativeShortcutHandler(NativeShortcut.FirstPage, !editingField && readerOpen && idle) { vm.goToPage(0) }
    NativeShortcutHandler(NativeShortcut.LastPage, !editingField && readerOpen && idle) { vm.goToPage(vm.state.readerPageCount - 1) }
    NativeShortcutHandler(NativeShortcut.Escape, idle && (showSearch || state.textSelection != null || state.adjustingTextRange || overlay != null)) {
        when {
            showSearch -> closeSearch()
            vm.state.adjustingTextRange -> vm.finishTextRange()
            vm.state.textSelection != null -> { vm.clearTextSelection(); vm.clearSelectedAnnotation() }
            else -> overlay = null
        }
    }
    val canCollapse by rememberUpdatedState(state.draft == null)
    val collapseThreshold = with(LocalDensity.current) { 32.dp.toPx() }
    val headerScroll = remember(collapseThreshold) {
        object : NestedScrollConnection {
            var distance = 0f
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (!canCollapse || source != NestedScrollSource.UserInput || available.y == 0f) return Offset.Zero
                if (distance * available.y < 0f) distance = 0f
                distance += available.y
                if (distance <= -collapseThreshold) {
                    headerCollapsed = true
                    distance = 0f
                } else if (distance >= collapseThreshold) {
                    headerCollapsed = false
                    distance = 0f
                }
                return Offset.Zero
            }
        }
    }
    val openAnnotation: (ZoteroAnnotation) -> Unit = { annotation ->
        vm.selectAnnotation(annotation, reveal = false)
        annotationKey = annotation.key
    }
    val annotationActions: (ZoteroAnnotation) -> List<NativeMenuAction> = { annotation ->
        val writable = state.canWrite && state.draft == null
        listOfNotNull(
            NativeMenuAction(if (annotation.type == "note") "Open note" else "Open annotation", NativeIcon.Note) { openAnnotation(annotation) },
            NativeMenuAction("Edit...", NativeIcon.Edit, enabled = writable) { vm.editAnnotation(annotation) },
            NativeMenuAction("Copy text", NativeIcon.Copy) {
                clipboard.setText(AnnotatedString(annotation.text.ifBlank { annotation.comment }))
            }.takeIf { annotation.text.isNotBlank() || annotation.comment.isNotBlank() },
            NativeMenuAction("Larger", NativeIcon.ZoomIn, enabled = writable) { vm.resizeAnnotation(annotation, true) }
                .takeIf { annotation.type in listOf("ink", "text") },
            NativeMenuAction("Smaller", NativeIcon.ZoomOut, enabled = writable) { vm.resizeAnnotation(annotation, false) }
                .takeIf { annotation.type in listOf("ink", "text") },
            NativeMenuAction("Adjust text range", NativeIcon.Select, enabled = writable) {
                vm.adjustAnnotationRange(annotation); tool = ReaderTool.Select; panel = "Reader"
            }.takeIf { annotation.type in listOf("highlight", "underline") },
            NativeMenuAction("Ask AI about this", NativeIcon.Assistant, enabled = (state.target != null || state.localStorageKey != null)) {
                vm.selectAnnotation(annotation, reveal = false); onAssistant()
            },
            NativeMenuAction("Delete annotation...", NativeIcon.Trash, enabled = writable) {
                if (confirmNative("Delete this annotation from this device?")) vm.deleteAnnotation(annotation)
            },
        )
    }
    val readerMenus = ReaderMenus(
        selection = selectionActions,
        annotation = annotationActions,
        colors = annotationColors,
        currentColor = state.annotationColor,
        onColor = { color -> vm.setAnnotationColor(color) },
    )
    LaunchedEffect(state.target?.attachment?.key, state.localTitle) {
        panel = "Reader"
        tool = ReaderTool.Select
        error = null
        editInk = false
        headerCollapsed = false
        annotationKey = null
    }
    LaunchedEffect(state.draft?.key) { editInk = false }
    LaunchedEffect(panel) { headerCollapsed = false }
    Box(Modifier.fillMaxSize()) {
      Column(Modifier.fillMaxSize().nestedScroll(headerScroll), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (headerCollapsed && state.draft == null) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            NativeHeading(state.target?.item?.data?.title ?: state.localTitle ?: "Reader", modifier = Modifier.weight(1f))
            NativeReaderModeSwitch(panel == "Markdown", enabled = state.draft == null,
                markdownEnabled = (state.target != null || state.localStorageKey != null), onSelect = switchPanel)
            NativeToolButton("Find in document (Ctrl+F)", NativeIcon.Search, selected = showSearch) {
                if (showSearch) closeSearch() else openSearch()
            }
            NativeToolButton("Show reader header", NativeIcon.Down) { headerCollapsed = false }
        }
        AnimatedVisibility(visible = !headerCollapsed || state.draft != null) {
          Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            NativeHeading(
                state.target?.item?.data?.title ?: state.target?.attachment?.data?.filename ?: state.localTitle ?: "Reader",
                null, Modifier.weight(1f),
            )
            NativeToolButton("Find in document (Ctrl+F)", NativeIcon.Search, enabled = state.page != null || state.pageCount > 0,
                selected = showSearch) { if (showSearch) closeSearch() else openSearch() }
            NativeToolButton("Ask the AI agent about this paper", NativeIcon.Assistant, enabled = (state.target != null || state.localStorageKey != null),
                selected = overlay == ReaderOverlay.Assistant) {
                overlay = if (overlay == ReaderOverlay.Assistant) null else ReaderOverlay.Assistant
            }
            NativeReaderModeSwitch(panel == "Markdown", enabled = state.draft == null,
                markdownEnabled = (state.target != null || state.localStorageKey != null), onSelect = switchPanel)
        }
          }
        }
        if (state.target == null && state.localTitle == null) {
            NativeEmptyState("Open a paper from Library or History to read it and use the research assistant.",
                Modifier.weight(1f).fillMaxWidth())
            return@Column
        }
        if (state.localTitle != null) NativeMeta(state.annotationSaveStatus)
        error?.let { Text(it) }
        state.error?.let { Text("PDF error: $it") }
        state.message?.let { Text(it) }
        if (state.loading || state.rendering) Text("Loading PDF...")
        if (panel == "Reader") {
          AnimatedVisibility(visible = !headerCollapsed || state.draft != null) {
            NativeActions {
                ReaderTool.entries.forEach { mode ->
                    NativeToolButton(mode.name, when (mode) {
                        ReaderTool.Select -> NativeIcon.Select
                        ReaderTool.Pan, ReaderTool.Move -> NativeIcon.Pan
                        ReaderTool.Pen -> NativeIcon.Pen
                        ReaderTool.Note -> NativeIcon.Note
                        ReaderTool.Text -> NativeIcon.Text
                        ReaderTool.Area -> NativeIcon.Area
                        ReaderTool.Idea -> NativeIcon.Idea
                    }, enabled = state.canWrite || mode in listOf(ReaderTool.Select, ReaderTool.Pan),
                        selected = mode == tool, onClick = { tool = mode })
                }
                if ((state.target != null || state.localStorageKey != null)) NativeAnnotationColors(state.annotationColor) { color ->
                    vm.setAnnotationColor(color)
                    vm.state.draft?.let { vm.changeDraft(it.comment, color) }
                }
                if (tool == ReaderTool.Pen && (state.target != null || state.localStorageKey != null)) {
                    NativeMenu("${state.inkWidth} pt", listOf(.5f, 1f, 2f, 4f, 8f, 12f).map { width ->
                        NativeMenuAction("$width pt", NativeIcon.Minus, selected = state.inkWidth == width) { vm.setInkWidth(width) }
                    }, icon = NativeIcon.Pen, showLabel = true)
                    Canvas(Modifier.width(40.dp).height(32.dp)) {
                        drawLine(annotationColor(state.annotationColor), Offset(4.dp.toPx(), size.height / 2),
                            Offset(size.width - 4.dp.toPx(), size.height / 2), state.inkWidth.dp.toPx())
                    }
                }
            }
          }
            if (showSearch) Row(
                Modifier.fillMaxWidth().background(nativeBackground, RoundedCornerShape(8.dp))
                    .border(1.dp, nativeForeground.copy(alpha = .15f), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                NativeGlyph(NativeIcon.Search)
                TextField(searchField, Modifier.weight(1f).focusRequester(searchFocus)
                    .onFocusChanged { searchFieldFocused = it.isFocused }
                    .onPreviewKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) false
                        else when (event.key) {
                            Key.Enter, Key.NumPadEnter -> { stepMatch(if (event.isShiftPressed) -1 else 1); true }
                            Key.Escape -> { closeSearch(); true }
                            else -> false
                        }
                    },
                    placeholder = { Text("Find in document") })
                Text(when {
                    searchQuery.isBlank() -> ""
                    searchMatches.isEmpty() -> if (searching) "Searching..." else "No matches"
                    else -> "${activeMatch + 1} / ${searchMatches.size}${if (searching) "+" else ""}"
                }, Modifier.widthIn(min = 72.dp), textAlign = TextAlign.Center, color = nativeForeground.copy(alpha = .7f))
                NativeToolButton("Match case", NativeIcon.Text, selected = matchCase) { matchCase = !matchCase }
                NativeToolButton("Whole words", NativeIcon.Select, selected = wholeWord) { wholeWord = !wholeWord }
                NativeToolButton("Previous match (Shift+F3)", NativeIcon.ArrowUp, enabled = searchMatches.isNotEmpty()) { stepMatch(-1) }
                NativeToolButton("Next match (F3)", NativeIcon.ArrowDown, enabled = searchMatches.isNotEmpty()) { stepMatch(1) }
                NativeToolButton("Close search (Escape)", NativeIcon.Close, onClick = closeSearch)
            }
            if (state.adjustingTextRange) NativeActions {
                Text("Drag across the new text range.")
                DefaultButton(onClick = vm::finishTextRange) { Text("Done selecting") }
                NativeToolButton("Cancel range adjustment", NativeIcon.Close, onClick = vm::cancelDraft)
            }
            state.draft?.takeIf { it.type == "ink" && it.editing == null && !editInk }?.let {
                NativeActions {
                    Text("Ink draft: keep drawing or save.")
                    DefaultButton(onClick = vm::saveDraft) { Text("Save ink") }
                    NativeToolButton("Edit ink note", NativeIcon.Edit) { editInk = true }
                    NativeToolButton("Discard ink draft", NativeIcon.Close, onClick = vm::cancelDraft)
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                val effectiveTool = if (!state.canWrite && tool !in listOf(ReaderTool.Select, ReaderTool.Pan)) ReaderTool.Select else tool
                Column(Modifier.fillMaxSize()) {
                    run {
                        Box(Modifier.fillMaxSize().pointerInput(Unit) {
                            awaitEachGesture {
                                awaitFirstDown(pass = PointerEventPass.Final)
                                if (currentEvent.buttons.isPrimaryPressed && vm.state.draft == null) {
                                    vm.clearTextSelection()
                                    vm.clearSelectedAnnotation()
                                }
                            }
                        }) {
                            val visibleMatches = if (showSearch) searchMatches else emptyList()
                            val currentMatch = visibleMatches.getOrNull(activeMatch)
                            if (continuous) NativeContinuousPages(vm, effectiveTool, markdown = false,
                                onAnnotationClick = openAnnotation, searchMatches = visibleMatches, activeMatch = currentMatch,
                                menus = readerMenus)
                            else state.page?.let { page ->
                                NativeAnnotatedPage(page, state, vm,
                                    effectiveTool,
                                    onAnnotationClick = openAnnotation,
                                    searchMatches = visibleMatches.filter { it.pageIndex == state.pageIndex }, activeMatch = currentMatch,
                                    menus = readerMenus)
                            }
                        }
                    }
                }
            }
        } else {
            Text("OCR Markdown (${state.markdown.pages.size} pages)")
            state.markdown.error?.let { Text(it) }
            state.markdown.syncError?.let { Text("OCR sync: $it") }
            if (state.markdown.busy) Text("Loading / converting OCR...")
            NativeActions {
                NativeToolButton("Load or retry OCR", NativeIcon.Refresh, enabled = !state.markdown.busy, showLabel = true, onClick = vm::retryMarkdown)
                NativeMenu("Markdown actions", listOf(
                    NativeMenuAction("Regenerate OCR...", NativeIcon.Refresh, enabled = !state.markdown.busy) {
                    if (confirmNative("Regenerate OCR using Mistral? This sends the PDF to Mistral and may incur charges.")) vm.regenerateMarkdown()
                    },
                    NativeMenuAction("Copy Markdown", NativeIcon.Copy) {
                    clipboard.setText(AnnotatedString(state.markdown.content.orEmpty()))
                    },
                ))
            }
            if (continuous) Box(Modifier.weight(1f).fillMaxWidth()) {
                NativeContinuousPages(vm, ReaderTool.Select, markdown = true)
            } else SelectionContainer(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                ChatMarkdown(state.markdown.pages.getOrNull(state.pageIndex).orEmpty(),
                    imageTransformer = Coil3ImageTransformerImpl)
            }
        }
        Row(Modifier.fillMaxWidth().onSizeChanged { bottomBarHeight = it.height }
            .background(nativeBackground, RoundedCornerShape(8.dp))
            .border(1.dp, nativeForeground.copy(alpha = .15f), RoundedCornerShape(8.dp)).padding(6.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Box(Modifier.weight(1f)) {
            NativeActions {
                NativeToolButton("Previous page", NativeIcon.Back, enabled = state.pageIndex > 0 && state.draft == null) {
                    vm.goToPage(state.pageIndex - 1)
                }
                Text("${state.pageIndex + 1} / ${state.readerPageCount}", Modifier.widthIn(min = 56.dp), textAlign = TextAlign.Center)
                NativeToolButton("Next page", NativeIcon.Next, enabled = state.pageIndex + 1 < state.readerPageCount && state.draft == null) {
                    vm.goToPage(state.pageIndex + 1)
                }
                TextField(pageNumber, Modifier.width(64.dp).focusRequester(pageFocus)
                    .onFocusChanged { pageFieldFocused = it.isFocused }
                    .onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown && (event.key == Key.Enter || event.key == Key.NumPadEnter)) {
                            if (state.draft == null) goToTypedPage()
                            true
                        } else false
                    }, placeholder = { Text("Page") })
                NativeToolButton("Zoom out (Ctrl+-)", NativeIcon.ZoomOut) { vm.zoom(state.zoom / 1.25f) }
                Text("${(state.zoom * 100).roundToInt()}%", Modifier.widthIn(min = 44.dp), textAlign = TextAlign.Center)
                NativeToolButton("Zoom in (Ctrl+=)", NativeIcon.ZoomIn) { vm.zoom(state.zoom * 1.25f) }
                NativeMenu("Page layout", listOf(
                    NativeMenuAction("Fit width", NativeIcon.Area) { vm.zoom(1f) },
                    NativeMenuAction("Continuous pages", NativeIcon.Layout, selected = continuous) { continuous = true },
                    NativeMenuAction("Single page", NativeIcon.File, selected = !continuous) { continuous = false },
                ), icon = NativeIcon.Layout)
            }
          }
            if ((state.target != null || state.localStorageKey != null)) {
                NativeToolButton("Annotations (${state.annotations.size})", NativeIcon.Note,
                    selected = overlay == ReaderOverlay.Annotations, showLabel = true) {
                    overlay = if (overlay == ReaderOverlay.Annotations) null else ReaderOverlay.Annotations
                }
                NativeToolButton("Idea board (${features.assistant.state.ideas.size})", NativeIcon.Idea,
                    selected = overlay == ReaderOverlay.Ideas, showLabel = true) {
                    overlay = if (overlay == ReaderOverlay.Ideas) null else ReaderOverlay.Ideas
                }
                NativeAgentButton(open = overlay == ReaderOverlay.Assistant, running = features.agent.running) {
                    overlay = if (overlay == ReaderOverlay.Assistant) null else ReaderOverlay.Assistant
                }
            }
        }
      }
      if (readerOpen && (state.target != null || state.localStorageKey != null)) overlay?.let { current ->
          val bottomInset = with(LocalDensity.current) { bottomBarHeight.toDp() }
          NativeFloatingPanel(
              Modifier.align(Alignment.BottomEnd).padding(bottom = bottomInset + 8.dp, end = 4.dp)
                  .widthIn(max = if (current == ReaderOverlay.Assistant) 460.dp else 400.dp).fillMaxWidth(.94f)
                  .fillMaxHeight(.78f),
          ) {
              when (current) {
                  ReaderOverlay.Assistant -> NativeAssistantPanel(features) { overlay = null }
                  ReaderOverlay.Ideas -> NativeIdeaBoardPanel(features) { overlay = null }
                  ReaderOverlay.Annotations -> NativeAnnotationsPanel(
                      state, vm, onOpen = openAnnotation, actions = annotationActions,
                      onShow = { annotation ->
                          if (panel != "Reader") { panel = "Reader"; vm.showPdf() }
                          vm.selectAnnotation(annotation)
                      },
                      onClose = { overlay = null },
                  )
              }
          }
      }
      val selectedAnnotation = state.annotations.firstOrNull { it.key == annotationKey }
      if (selectedAnnotation != null && state.draft == null) NativeDialog(
          "Annotation · page ${selectedAnnotation.pageLabel}",
          onClose = { annotationKey = null },
      ) {
          Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
              verticalArrangement = Arrangement.spacedBy(12.dp)) {
              NativeHeading(selectedAnnotation.type, if (selectedAnnotation.isAiGenerated) "AI-generated annotation" else null)
              if (selectedAnnotation.text.isNotBlank()) SelectionContainer { Text(selectedAnnotation.text) }
              SelectionContainer { Text(selectedAnnotation.comment.ifBlank { "No note added yet." }) }
          }
          NativeActions {
              DefaultButton(enabled = state.canWrite, onClick = {
                  annotationKey = null
                  vm.editAnnotation(selectedAnnotation)
              }) { Text("Edit note") }
              NativeToolButton("Move annotation", NativeIcon.Pan,
                  enabled = state.canWrite && selectedAnnotation.type in listOf("note", "text", "image", "ink"), showLabel = true) {
                  annotationKey = null; tool = ReaderTool.Move
              }
              NativeToolButton("Delete annotation", NativeIcon.Trash,
                  enabled = (state.target != null || state.localStorageKey != null) && state.canWrite, showLabel = true) {
                  if (confirmNative("Delete this annotation from this device?")) {
                      annotationKey = null
                      vm.deleteAnnotation(selectedAnnotation)
                  }
              }
          }
      }
      state.draft?.takeIf { !state.adjustingTextRange && (it.type != "ink" || it.editing != null || editInk) }?.let { draft ->
        NativeDialog("${if (draft.editing != null) "Edit" else "New"} ${draft.type}", onClose = {
            if (confirmNative("Discard this unsaved annotation draft?")) vm.cancelDraft()
        }) {
            key(draft.key) {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    NativeAnnotationDraft(features, draft)
                }
            }
        }
      }
    }
}

@Composable
private fun NativeAnnotationDraft(features: NativeFeatures, draft: AnnotationDraft) {
    val vm = features.reader
    val agent = features.agent
    val comment = rememberTextFieldState(draft.comment)
    Text("${if (draft.editing != null) "Edit" else "New"} ${draft.type} on page ${draft.pageIndex + 1}")
    TextArea(comment, Modifier.fillMaxWidth().height(80.dp), placeholder = { Text("Comment / note / text") })
    NativeActions {
        DefaultButton(
            enabled = draft.type !in listOf("note", "text") || comment.text.isNotBlank(),
            onClick = { vm.changeDraft(comment.text.toString(), vm.state.draft?.color ?: draft.color); vm.saveDraft() },
        ) { Text("Save annotation") }
        OutlinedButton(onClick = vm::cancelDraft) { Text("Cancel draft") }
        NativeAnnotationColors(draft.color) { color -> vm.changeDraft(comment.text.toString(), color) }
        NativeMenu("AI comment", AnnotationAiAction.entries.map { action ->
            NativeMenuAction(action.label, NativeIcon.Assistant, enabled = !agent.annotationAiRunning && draft.text.isNotBlank()) {
                agent.generateAnnotationComment(draft.text, comment.text.toString(), draft.pageIndex,
                    includePage = true, includePaper = true, action = action, brief = true) {
                    comment.setTextAndPlaceCursorAtEnd(it)
                }
            }
        }, icon = NativeIcon.Assistant, showLabel = true)
    }
    agent.annotationAiError?.let { Text(it) }
}

private enum class ReaderOverlay { Assistant, Annotations, Ideas }

/** Segmented PDF / Markdown switch for the reader, like the app's reading-mode toggle. */
@Composable
private fun NativeReaderModeSwitch(markdown: Boolean, enabled: Boolean, markdownEnabled: Boolean, onSelect: (Boolean) -> Unit) {
    val shape = RoundedCornerShape(6.dp)
    Row(
        Modifier.clip(shape).background(nativeForeground.copy(alpha = .05f))
            .border(1.dp, nativeForeground.copy(alpha = .15f), shape).padding(2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        NativeToolButton("PDF", NativeIcon.File, enabled = enabled, selected = !markdown, showLabel = true, compact = true) {
            if (markdown) onSelect(false)
        }
        NativeToolButton("Markdown", NativeIcon.Markdown, enabled = enabled && markdownEnabled, selected = markdown,
            showLabel = true, compact = true) {
            if (!markdown) onSelect(true)
        }
    }
}

/** Bottom-bar entry point to the floating AI agent panel. */
@Composable
private fun NativeAgentButton(open: Boolean, running: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    val accent = Color(0xFF3574F0)
    Row(
        Modifier.clip(shape).background(if (open) accent.copy(alpha = .9f) else accent.copy(alpha = .16f))
            .border(1.dp, accent.copy(alpha = .6f), shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        NativeGlyph(NativeIcon.Assistant, Modifier.size(16.dp), tint = if (open) Color.White else nativeAccent)
        Text(if (running) "AI agent working..." else "AI agent", color = if (open) Color.White else nativeForeground)
        if (running) Box(Modifier.size(7.dp).background(Color(0xFF59A869), RoundedCornerShape(50)))
    }
}

@Composable
private fun ColumnScope.NativeAnnotationsPanel(
    state: PdfWorkspaceState,
    vm: PdfWorkspaceViewModel,
    onOpen: (ZoteroAnnotation) -> Unit,
    actions: (ZoteroAnnotation) -> List<NativeMenuAction>,
    onShow: (ZoteroAnnotation) -> Unit,
    onClose: () -> Unit,
) {
    val filterField = rememberTextFieldState()
    var filter by remember { mutableStateOf("") }
    LaunchedEffect(filterField) { snapshotFlow { filterField.text.toString() }.collect { filter = it } }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        NativeGlyph(NativeIcon.Note)
        NativeHeading("Annotations", "${state.annotations.size} on this paper", Modifier.weight(1f))
        NativeToolButton("Refresh annotations", NativeIcon.Refresh, enabled = !state.syncing, onClick = vm::refreshAnnotations)
        NativeMenu("Annotation maintenance", listOf(
            NativeMenuAction("Repair legacy ink positions", NativeIcon.Refresh, action = vm::repairInkAnnotations),
        ))
        NativeToolButton("Close annotations (Escape)", NativeIcon.Close, onClick = onClose)
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(nativeForeground.copy(alpha = .12f)))
    TextField(filterField, Modifier.fillMaxWidth(), placeholder = { Text("Filter annotations") })
    val visible = state.annotations.filter {
        filter.isBlank() || it.text.contains(filter, true) || it.comment.contains(filter, true) || it.type.contains(filter, true)
    }
    if (visible.isEmpty()) {
        NativeEmptyState(if (filter.isBlank()) "No annotations yet. Select text or use a tool to add one." else "No annotation matches \"$filter\".",
            Modifier.weight(1f).fillMaxWidth())
        return
    }
    LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(visible, key = { it.key }) { annotation ->
            NativeCard(onClick = { onOpen(annotation) }) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(12.dp).background(annotationColor(annotation.color), RoundedCornerShape(3.dp)))
                    Text("Page ${annotation.pageLabel} · ${annotation.type}", Modifier.weight(1f), color = nativeForeground.copy(alpha = .7f),
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (annotation.isAiGenerated) NativeChip("AI", NativeIcon.Assistant)
                    NativeToolButton("Show on page", NativeIcon.Locate) { onShow(annotation) }
                    NativeMenu("Annotation actions", actions(annotation).drop(1))
                }
                if (annotation.text.isNotBlank()) Text(annotation.text, maxLines = 3, overflow = TextOverflow.Ellipsis)
                Text(annotation.comment.ifBlank { "No note" }, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    color = nativeForeground.copy(alpha = if (annotation.comment.isBlank()) .5f else 1f))
            }
        }
    }
}

@Composable
private fun NativeAnnotationColors(selected: String, onSelect: (String) -> Unit) {
    NativeMenu("Annotation color", annotationColors.zip(listOf("Yellow", "Red", "Green", "Blue", "Purple", "Magenta")).map { (color, name) ->
        NativeMenuAction(name, NativeIcon.Highlight, selected = selected == color, swatch = annotationColor(color)) { onSelect(color) }
    }, icon = NativeIcon.Pen, swatch = annotationColor(selected))
}

@Composable
private fun NativeContinuousPages(
    vm: PdfWorkspaceViewModel, tool: ReaderTool, markdown: Boolean,
    onAnnotationClick: (ZoteroAnnotation) -> Unit = {},
    searchMatches: List<PdfTextMatch> = emptyList(), activeMatch: PdfTextMatch? = null,
    menus: ReaderMenus? = null,
) {
    val state = vm.state
    val list = rememberLazyListState()
    var following by remember { mutableStateOf(false) }
    val current by rememberUpdatedState(state)
    val count = if (markdown) state.markdown.pages.size else state.pageCount
    LaunchedEffect(list, markdown) {
        snapshotFlow {
            list.layoutInfo.visibleItemsInfo.firstOrNull()?.let { Triple(it.index, it.offset, it.size) }
        }.collect { item ->
            if (!following && item != null && current.draft == null) {
                vm.trackReadingPosition(PdfReadingPosition(
                    item.first, (-item.second.toFloat() / item.third.coerceAtLeast(1)).coerceIn(0f, 1f),
                ), PdfScrollSource.Pdf)
            }
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val pageWidth = maxWidth * state.zoom
        val contentWidth = maxOf(maxWidth, pageWidth)
        val widthPixels = with(LocalDensity.current) { pageWidth.roundToPx() }
        val currentWidth by rememberUpdatedState(widthPixels)
        LaunchedEffect(state.pageNavigationId, markdown, count) {
            if (count > 0) {
                following = true
                try {
                    val index = current.pageIndex.coerceIn(0 until count)
                    val fraction = current.readingPosition.takeIf { it.pageIndex == index }?.fraction ?: 0f
                    val geometry = current.pageGeometries[index]
                    val offset = if (markdown || geometry == null || geometry.displayWidth <= 0f) 0
                    else (fraction * currentWidth * geometry.displayHeight / geometry.displayWidth).roundToInt()
                    list.scrollToItem(index, offset)
                } finally { following = false }
            }
        }
        Box(Modifier.fillMaxSize().horizontalScroll(rememberScrollState())) {
            LazyColumn(
                Modifier.width(contentWidth).fillMaxHeight(), state = list,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(count, key = { it }) { index ->
                    Box(Modifier.width(contentWidth), contentAlignment = Alignment.TopCenter) {
                        Column(Modifier.width(pageWidth)) {
                            Text("Page ${index + 1}")
                            if (markdown) {
                                SelectionContainer {
                                    ChatMarkdown(state.markdown.pages[index], imageTransformer = Coil3ImageTransformerImpl)
                                }
                            } else {
                                var page by remember(state.target, state.localTitle, index, widthPixels) { mutableStateOf<PdfPage?>(null) }
                                LaunchedEffect(state.target, state.localTitle, index, widthPixels, state.pageCount) {
                                    page = vm.loadContinuousPage(index, widthPixels)
                                }
                                val rendered = page
                                if (rendered == null) Box(Modifier.fillMaxWidth().height(360.dp), contentAlignment = Alignment.Center) { Text("Loading page ${index + 1}...") }
                                else NativeAnnotatedPage(
                                    rendered, state.copy(pageIndex = index, page = rendered, zoom = 1f,
                                        textSelection = state.textSelection.takeIf { state.pageIndex == index }),
                                    vm, tool, Modifier.width(pageWidth).height(pageWidth * (rendered.geometry.displayHeight / rendered.geometry.displayWidth)),
                                    scrollable = false,
                                    onAnnotationClick = onAnnotationClick,
                                    searchMatches = searchMatches.filter { it.pageIndex == index }, activeMatch = activeMatch,
                                    menus = menus,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NativeAnnotatedPage(
    page: PdfPage, state: PdfWorkspaceState, vm: PdfWorkspaceViewModel, tool: ReaderTool,
    modifier: Modifier = Modifier.fillMaxSize(), scrollable: Boolean = true,
    spokenRects: List<List<Float>> = emptyList(),
    onAnnotationClick: (ZoteroAnnotation) -> Unit,
    searchMatches: List<PdfTextMatch> = emptyList(), activeMatch: PdfTextMatch? = null,
    menus: ReaderMenus? = null,
) {
    val geometry = page.geometry
    val measurer = rememberTextMeasurer()
    val currentState by rememberUpdatedState(state)
    val currentMenus by rememberUpdatedState(menus)
    var drag by remember(page) { mutableStateOf<AnnotationDrag?>(null) }
    var hoverTarget by remember(page) { mutableStateOf(HoverTarget.None) }
    var contextMenu by remember(page) { mutableStateOf<PageContextMenu?>(null) }
    val dragging by remember(page) { derivedStateOf { drag != null } }
    /** Annotations on this page that can be dragged; areas only by their border or once selected so text under them stays selectable. */
    fun movableAt(point: Offset): ZoteroAnnotation? {
        val current = vm.state
        if (!current.canWrite || current.draft != null) return null
        return current.annotations.lastOrNull { annotation ->
            val bounds = annotationGrabBounds(annotation, state.pageIndex, geometry) ?: return@lastOrNull false
            val inside = bounds.inflate(4f).contains(point)
            inside && (annotation.type != "image" || annotation.key == current.selectedKey ||
                !bounds.deflate(6f).let { it.width > 0f && it.height > 0f && it.contains(point) })
        }
    }
    var preview by remember(state.pageIndex) { mutableStateOf<List<Offset>>(emptyList()) }
    val vertical = rememberScrollState()
    val horizontal = rememberScrollState()
    val openAnnotation by rememberUpdatedState(onAnnotationClick)
    var lastClick by remember { mutableStateOf<Pair<Long, Offset>?>(null) }
    BoxWithConstraints(modifier) {
        val width = maxWidth * state.zoom
        val height = width * (geometry.displayHeight / geometry.displayWidth)
        val density = LocalDensity.current
        val widthPx = with(density) { width.toPx().roundToInt() }
        LaunchedEffect(widthPx, state.pageIndex) { if (scrollable) vm.ensurePageResolution(widthPx) }
        val heightPx = with(density) { height.toPx() }
        LaunchedEffect(state.pageNavigationId, state.pageIndex) {
            if (!scrollable) return@LaunchedEffect
            withFrameNanos { }
            val fraction = state.readingPosition.takeIf { it.pageIndex == state.pageIndex }?.fraction ?: 0f
            vertical.scrollTo((fraction * heightPx).roundToInt())
            horizontal.scrollTo(0)
        }
        val contentWidth = maxOf(maxWidth, width)
        val contentHeight = maxOf(maxHeight, height)
        Box(
            Modifier.width(contentWidth).height(contentHeight)
                .let { if (scrollable) it.horizontalScroll(horizontal).verticalScroll(vertical) else it },
        ) {
            val gestures = Modifier.pointerInput(page, tool) {
                fun point(position: Offset): Offset {
                    val ratio = size.width / geometry.displayWidth
                    return Offset((position.x / ratio).coerceIn(0f, geometry.displayWidth),
                        (position.y / ratio).coerceIn(0f, geometry.displayHeight))
                }
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (currentEvent.isContextClick()) return@awaitEachGesture
                    if (!currentEvent.buttons.isPrimaryPressed) return@awaitEachGesture
                    // Marks the press as handled by a page so the reader's "click outside" handler ignores it.
                    down.consume()
                    contextMenu = null
                    if (vm.state.draft != null && vm.state.pageIndex != state.pageIndex) return@awaitEachGesture
                    vm.activateContinuousPage(state.pageIndex, page)
                    val start = point(down.position)
                    var end = start
                    val points = mutableListOf(start)
                    val grabbed = when (tool) {
                        ReaderTool.Select, ReaderTool.Pan -> movableAt(start)
                        ReaderTool.Move -> vm.state.annotations.find { it.key == vm.state.selectedKey }
                            ?.takeIf { vm.state.canWrite && annotationGrabBounds(it, state.pageIndex, geometry) != null }
                        else -> null
                    }
                    val grabFrom = grabbed?.let { annotationMoveBounds(it, geometry) }
                    var dragged = false
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        end = point(change.position)
                        if (!change.pressed) break
                        if (grabbed != null && grabFrom != null) {
                            if (dragged || (end - start).getDistance() >= 3f) {
                                dragged = true
                                drag = AnnotationDrag(grabbed.key, clampedDelta(grabFrom, end - start, geometry))
                            }
                            change.consume()
                            continue
                        }
                        if (tool == ReaderTool.Pen) { points += end; preview = points.toList() }
                        if (tool == ReaderTool.Select && (end - start).getDistance() >= 3f) vm.selectText(start, end)
                        if (tool != ReaderTool.Pan) change.consume()
                    }
                    if (grabbed != null && grabFrom != null && dragged) {
                        val delta = clampedDelta(grabFrom, end - start, geometry)
                        vm.selectAnnotation(grabbed, state.pageIndex, reveal = false)
                        vm.transformAnnotation(grabbed, state.pageIndex, grabFrom, grabFrom.translate(delta))
                        drag = null
                        return@awaitEachGesture
                    }
                    drag = null
                    val bounds = Rect(minOf(start.x, end.x), minOf(start.y, end.y), maxOf(start.x, end.x), maxOf(start.y, end.y))
                    when (tool) {
                        ReaderTool.Select, ReaderTool.Pan -> if ((end - start).getDistance() < 3f && vm.state.draft == null) {
                            val hit = annotationAt(vm.state.annotations, state.pageIndex, geometry, start)
                            val now = down.uptimeMillis
                            val previous = lastClick
                            val doubleClick = previous != null && now - previous.first <= viewConfiguration.doubleTapTimeoutMillis &&
                                (start - previous.second).getDistance() < 6f
                            lastClick = if (doubleClick) null else now to start
                            when {
                                hit != null -> openAnnotation(hit)
                                doubleClick && tool == ReaderTool.Select -> vm.selectWord(start)
                                else -> { vm.clearTextSelection(); vm.clearSelectedAnnotation() }
                            }
                        }
                        ReaderTool.Pen -> vm.addInkPath(points + end, vm.state.annotationColor)
                        ReaderTool.Note -> vm.beginNote(Rect(start, androidx.compose.ui.geometry.Size(22f, 22f)))
                        ReaderTool.Text -> vm.beginText(start)
                        ReaderTool.Area -> vm.beginRectangle(bounds)
                        ReaderTool.Idea -> vm.beginIdea(Rect(start, androidx.compose.ui.geometry.Size(22f, 22f)))
                        ReaderTool.Move -> Unit
                    }
                    preview = emptyList()
                }
            }
            val contextClicks = Modifier.pointerInput(page) {
                awaitPointerEventScope {
                    while (true) {
                        // Initial pass so nothing below (selection, scrolling, IDE handlers) swallows the right click.
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.type != PointerEventType.Press || !event.isContextClick()) continue
                        val change = event.changes.firstOrNull() ?: continue
                        change.consume()
                        if (vm.state.draft != null) continue
                        val ratio = size.width / geometry.displayWidth
                        val at = Offset((change.position.x / ratio).coerceIn(0f, geometry.displayWidth),
                            (change.position.y / ratio).coerceIn(0f, geometry.displayHeight))
                        val selection = vm.state.textSelection?.takeIf { vm.state.pageIndex == currentState.pageIndex }
                        val onSelection = selection?.rects?.any { it.inflate(6f).contains(at) } == true
                        val hit = if (onSelection) null else annotationAt(vm.state.annotations, currentState.pageIndex, geometry, at)
                        if (!onSelection) {
                            vm.activateContinuousPage(currentState.pageIndex, page)
                            if (hit != null) vm.selectAnnotation(hit, currentState.pageIndex, reveal = false)
                        }
                        contextMenu = PageContextMenu(change.position, at, hit, onSelection || hit == null && selection != null)
                    }
                }
            }
            val hovering = Modifier.pointerInput(page, tool) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type != PointerEventType.Move && event.type != PointerEventType.Enter &&
                            event.type != PointerEventType.Exit) continue
                        val ratio = size.width / geometry.displayWidth
                        val at = event.changes.firstOrNull()?.position?.takeIf { event.type != PointerEventType.Exit }
                            ?.let { Offset(it.x / ratio, it.y / ratio) }
                        val target = when {
                            at == null -> HoverTarget.None
                            (tool == ReaderTool.Select || tool == ReaderTool.Pan) && movableAt(at) != null -> HoverTarget.Movable
                            tool == ReaderTool.Select || tool == ReaderTool.Pan ->
                                if (annotationAt(vm.state.annotations, state.pageIndex, geometry, at) != null) HoverTarget.Annotation else HoverTarget.None
                            else -> HoverTarget.None
                        }
                        if (target != hoverTarget) hoverTarget = target
                    }
                }
            }
            val cursor = when {
                dragging || tool == ReaderTool.Move || hoverTarget == HoverTarget.Movable ->
                    PointerIcon(java.awt.Cursor(java.awt.Cursor.MOVE_CURSOR))
                hoverTarget == HoverTarget.Annotation -> PointerIcon.Hand
                tool == ReaderTool.Select -> PointerIcon.Text
                tool == ReaderTool.Pan -> PointerIcon.Hand
                else -> PointerIcon.Crosshair
            }
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
              Box(Modifier.width(width).height(height)) {
              Canvas(Modifier.fillMaxSize().background(Color.White).then(hovering).then(contextClicks).then(gestures).pointerHoverIcon(cursor)) {
                drawImage(page.image, dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()))
                val ratio = size.width / geometry.displayWidth
                withTransform({ scale(ratio, ratio, pivot = Offset.Zero) }) {
                    fun annotation(type: String, rects: List<List<Float>>, paths: List<List<Float>>, color: String,
                        stroke: Float, text: String = "", fontSize: Float = 12f, selected: Boolean = false, idea: Boolean = false) {
                        val tint = annotationColor(color)
                        if (type == "note") {
                            rects.lastOrNull()?.let { drawNoteMarker(geometry.displayRect(it), annotationMarkerColor(color), idea, selected) }
                            return
                        }
                        if (type == "ink") paths.forEach { values ->
                            val points = values.chunked(2).map { geometry.toDisplay(Offset(it[0], it[1])) }
                            drawNativeInk(points, tint, stroke)
                        } else rects.forEach { coordinates ->
                            val rect = geometry.displayRect(coordinates)
                            when (type) {
                                "highlight" -> drawRect(tint.copy(alpha = 0.3f), rect.topLeft, rect.size)
                                "underline" -> drawLine(tint, rect.bottomLeft, rect.bottomRight, 1.5f)
                                "text" -> drawText(measurer, text, topLeft = rect.topLeft, style = TextStyle(color = tint, fontSize = fontSize.sp))
                                else -> drawRect(tint.copy(alpha = 0.2f), rect.topLeft, rect.size)
                            }
                            if (selected || type == "image") drawRect(tint, rect.topLeft, rect.size, style = Stroke(1.5f))
                        }
                        if (text.isNotBlank() && type in listOf("highlight", "underline", "image") && (selected || type == "image")) {
                            rects.lastOrNull()?.let { coordinates ->
                                val rect = geometry.displayRect(coordinates)
                                drawCommentMarker(Rect(rect.right + 2f, rect.top - 12f, rect.right + 12f, rect.top - 2f), annotationMarkerColor(color), idea)
                            }
                        }
                    }
                    val moving = drag
                    state.annotations.forEach { item ->
                        val position = item.position
                        if (position.pageIndex == state.pageIndex) {
                            if (moving != null && moving.key == item.key) {
                                annotationMoveBounds(item, geometry)?.let { origin ->
                                    drawRect(annotationColor(item.color).copy(alpha = .6f), origin.topLeft, origin.size,
                                        style = Stroke(1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 3f))))
                                }
                                withTransform({ translate(moving.delta.x, moving.delta.y) }) {
                                    annotation(item.type, position.rects, position.paths, item.color, position.width,
                                        item.comment, position.fontSize, selected = true, idea = item.isIdea)
                                }
                            } else annotation(item.type, position.rects, position.paths,
                                item.color, position.width, item.comment, position.fontSize, item.key == state.selectedKey, item.isIdea)
                        }
                        if (position.pageIndex + 1 == state.pageIndex && position.nextPageRects.isNotEmpty())
                            annotation(item.type, position.nextPageRects, emptyList(), item.color, position.width)
                    }
                    searchMatches.forEach { match ->
                        val active = match == activeMatch
                        match.selection.rects.forEach { rect ->
                            drawRect(if (active) Color(0x99FF8C00) else Color(0x66FFD400), rect.topLeft, rect.size)
                            if (active) drawRect(Color(0xFFFF8C00), rect.topLeft, rect.size, style = Stroke(1f))
                        }
                    }
                    state.textSelection?.rects?.forEach { rect -> drawRect(Color(0x553B82F6), rect.topLeft, rect.size) }
                    spokenRects.filter { it.size == 4 }.forEach {
                        val rect = Rect(it[0], it[1], it[2], it[3])
                        drawRect(Color(0x665FB236), rect.topLeft, rect.size)
                    }
                    state.draft?.takeIf { it.pageIndex == state.pageIndex }?.let { draft ->
                        annotation(draft.type, draft.rects, draft.paths, draft.color, draft.width, draft.comment, draft.fontSize, selected = true, idea = draft.isIdea)
                    }
                    drawNativeInk(preview, annotationColor(state.annotationColor), state.inkWidth)
                }
              }
            contextMenu?.let { menu ->
                val readerMenus = currentMenus
                val dismiss = { contextMenu = null }
                val writable = (state.target != null || state.localStorageKey != null) && state.canWrite && state.draft == null
                val actions = when {
                    menu.selection && readerMenus != null -> readerMenus.selection
                    menu.annotation != null && readerMenus != null -> readerMenus.annotation(menu.annotation)
                    else -> listOf(
                        NativeMenuAction("Add note here", NativeIcon.Note, enabled = writable) {
                            vm.beginNote(Rect(menu.point, androidx.compose.ui.geometry.Size(22f, 22f)))
                        },
                        NativeMenuAction("Add idea here", NativeIcon.Idea, enabled = writable) {
                            vm.beginIdea(Rect(menu.point, androidx.compose.ui.geometry.Size(22f, 22f)))
                        },
                        NativeMenuAction("Add text here", NativeIcon.Text, enabled = writable) { vm.beginText(menu.point) },
                        NativeMenuAction("Select all text", NativeIcon.Select, enabled = state.draft == null, action = vm::selectAllText),
                    )
                }
                val colorRow: (@Composable () -> Unit)? =
                    if (readerMenus != null && menu.selection && (state.target != null || state.localStorageKey != null)) {
                        { NativeColorRow(readerMenus.colors, readerMenus.currentColor, readerMenus.onColor) }
                    } else null
                NativeMenuPopup(
                    actions, onDismiss = dismiss,
                    anchor = IntOffset(menu.position.x.roundToInt(), menu.position.y.roundToInt()),
                    header = colorRow,
                )
            }
            }
            }
        }
    }
}

private fun DrawScope.drawNativeInk(points: List<Offset>, color: Color, width: Float) {
    if (points.size < 2) return
    val path = Path().apply { moveTo(points.first().x, points.first().y); points.drop(1).forEach { lineTo(it.x, it.y) } }
    drawPath(path, color, style = Stroke(width))
}

private fun annotationColor(value: String): Color = Color(0xFF000000 or value.removePrefix("#").toLong(16))

/** Actions the reader offers in the page's right-click menu. */
private class ReaderMenus(
    val selection: List<NativeMenuAction>,
    val annotation: (ZoteroAnnotation) -> List<NativeMenuAction>,
    val colors: List<String>,
    val currentColor: String,
    val onColor: (String) -> Unit,
)

private data class PageContextMenu(val position: Offset, val point: Offset, val annotation: ZoteroAnnotation?, val selection: Boolean)

/** Right click (or the platform popup trigger), read from the AWT event too because the IDE bridge may not report secondary buttons. */
@OptIn(ExperimentalComposeUiApi::class)
private fun PointerEvent.isContextClick(): Boolean {
    if (buttons.isSecondaryPressed || button == PointerButton.Secondary) return true
    val awt = nativeEvent as? java.awt.event.MouseEvent ?: return false
    return javax.swing.SwingUtilities.isRightMouseButton(awt) || awt.isPopupTrigger ||
        (awt.button == java.awt.event.MouseEvent.BUTTON1 && awt.isControlDown && System.getProperty("os.name").orEmpty().startsWith("Mac"))
}

private data class AnnotationDrag(val key: String, val delta: Offset)

private enum class HoverTarget { None, Annotation, Movable }

@Composable
private fun NativeColorRow(colors: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(Modifier.padding(horizontal = 6.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically) {
        colors.forEach { color ->
            val active = color.equals(selected, ignoreCase = true)
            Box(
                Modifier.size(22.dp).clip(RoundedCornerShape(11.dp)).background(annotationColor(color))
                    .border(if (active) 2.dp else 1.dp, if (active) nativeForeground else nativeForeground.copy(alpha = .25f), RoundedCornerShape(11.dp))
                    .clickable(role = Role.Button) { onSelect(color) },
            )
        }
    }
}

/** Display-space bounds used to move an annotation (union of its rectangles). */
private fun annotationMoveBounds(annotation: ZoteroAnnotation, geometry: PdfGeometry): Rect? =
    annotation.position.boundingRects.takeIf { it.isNotEmpty() }?.map(geometry::displayRect)?.reduce { a, b ->
        Rect(minOf(a.left, b.left), minOf(a.top, b.top), maxOf(a.right, b.right), maxOf(a.bottom, b.bottom))
    }

/** Area that grabs a movable annotation; notes use their drawn marker, which is at least 12 units wide. */
private fun annotationGrabBounds(annotation: ZoteroAnnotation, pageIndex: Int, geometry: PdfGeometry): Rect? {
    val position = annotation.position
    if (annotation.type !in movableAnnotationTypes || position.pageIndex != pageIndex || position.nextPageRects.isNotEmpty()) return null
    val bounds = annotationMoveBounds(annotation, geometry) ?: return null
    if (annotation.type != "note") return bounds
    val side = minOf(maxOf(minOf(bounds.width, bounds.height), 12f), 16f)
    return Rect(bounds.left, bounds.top, bounds.left + side, bounds.top + side)
}

private fun annotationAt(annotations: List<ZoteroAnnotation>, pageIndex: Int, geometry: PdfGeometry, point: Offset): ZoteroAnnotation? =
    annotations.lastOrNull { annotation ->
        val position = annotation.position
        val rects = when (pageIndex) {
            position.pageIndex -> position.boundingRects
            position.pageIndex + 1 -> position.nextPageRects
            else -> emptyList()
        }
        annotationGrabBounds(annotation, pageIndex, geometry)?.inflate(4f)?.contains(point) == true ||
            rects.any { geometry.displayRect(it).inflate(4f).contains(point) }
    }

/** Keeps a dragged annotation inside the page. */
private fun clampedDelta(from: Rect, delta: Offset, geometry: PdfGeometry): Offset = Offset(
    delta.x.coerceIn(-from.left, maxOf(geometry.displayWidth - from.right, -from.left)),
    delta.y.coerceIn(-from.top, maxOf(geometry.displayHeight - from.bottom, -from.top)),
)

/** Darkens light annotation colors so note markers stay readable on white pages, as in the app. */
private fun annotationMarkerColor(value: String): Color {
    val color = annotationColor(value)
    return if (color.luminance() > 0.45f) lerp(color, Color.Black, 0.35f) else color
}

/** Draws a sticky-note (or idea light bulb) marker like the app's EditNote/Lightbulb pin instead of a plain square. */
private fun DrawScope.drawNoteMarker(bounds: Rect, color: Color, idea: Boolean, selected: Boolean) {
    val side = minOf(maxOf(minOf(bounds.width, bounds.height), 12f), 16f)
    val box = Rect(bounds.left, bounds.top, bounds.left + side, bounds.top + side)
    if (selected) drawRoundRect(color.copy(alpha = 0.22f), box.topLeft - Offset(3f, 3f),
        Size(side + 6f, side + 6f), CornerRadius(5f))
    val unit = side / 24f
    fun at(x: Float, y: Float) = Offset(box.left + x * unit, box.top + y * unit)
    if (idea) {
        drawCircle(color, 7.5f * unit, at(12f, 9.5f))
        drawRect(color, at(8.5f, 14f), Size(7f * unit, 4f * unit))
        drawLine(color, at(9f, 20f), at(15f, 20f), 1.8f * unit)
        drawLine(color, at(10f, 22.5f), at(14f, 22.5f), 1.8f * unit)
        drawLine(Color.White, at(12f, 7f), at(12f, 14f), 1.4f * unit)
        drawLine(Color.White, at(10f, 9.5f), at(14f, 9.5f), 1.4f * unit)
        return
    }
    val sheet = Path().apply {
        moveTo(at(3f, 3f).x, at(3f, 3f).y); lineTo(at(21f, 3f).x, at(21f, 3f).y); lineTo(at(21f, 15f).x, at(21f, 15f).y)
        lineTo(at(15f, 21f).x, at(15f, 21f).y); lineTo(at(3f, 21f).x, at(3f, 21f).y); close()
    }
    drawPath(sheet, color)
    val fold = Path().apply {
        moveTo(at(21f, 15f).x, at(21f, 15f).y); lineTo(at(15f, 15f).x, at(15f, 15f).y); lineTo(at(15f, 21f).x, at(15f, 21f).y); close()
    }
    drawPath(fold, lerp(color, Color.White, 0.45f))
    listOf(8f, 12f).forEach { y -> drawLine(Color.White, at(7f, y), at(17f, y), 1.5f * unit) }
    drawLine(Color.White, at(7f, 16f), at(12f, 16f), 1.5f * unit)
}

/** Small speech-bubble marker shown next to commented highlights, underlines, and areas. */
private fun DrawScope.drawCommentMarker(bounds: Rect, color: Color, idea: Boolean) {
    if (idea) return drawNoteMarker(bounds, color, idea = true, selected = false)
    val unit = bounds.width / 24f
    fun at(x: Float, y: Float) = Offset(bounds.left + x * unit, bounds.top + y * unit)
    drawRoundRect(color, at(2f, 3f), Size(20f * unit, 14f * unit), CornerRadius(3f * unit))
    val tail = Path().apply { moveTo(at(7f, 16f).x, at(7f, 16f).y); lineTo(at(7f, 22f).x, at(7f, 22f).y); lineTo(at(13f, 16f).x, at(13f, 16f).y); close() }
    drawPath(tail, color)
}
