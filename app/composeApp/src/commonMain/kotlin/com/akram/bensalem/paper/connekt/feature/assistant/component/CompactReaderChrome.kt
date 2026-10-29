package com.akram.bensalem.paper.connekt.feature.assistant.component

import androidx.compose.ui.graphics.painter.Painter

import org.jetbrains.compose.resources.DrawableResource

import com.paperconnekt.app.resources.Res
import org.jetbrains.compose.resources.painterResource
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.akram.bensalem.bistosh.core.designsystem.component.interactivePointer
import com.akram.bensalem.paper.connekt.feature.assistant.PdfWorkspaceState
import com.akram.bensalem.paper.connekt.feature.assistant.inkWidthPresets
import com.akram.bensalem.paper.connekt.feature.assistant.textSizePresets
import com.paperconnekt.app.resources.filled_arrow_back
import com.paperconnekt.app.resources.filled_arrow_drop_down
import com.paperconnekt.app.resources.filled_article
import com.paperconnekt.app.resources.filled_auto_stories
import com.paperconnekt.app.resources.filled_border_color
import com.paperconnekt.app.resources.filled_check
import com.paperconnekt.app.resources.filled_chevron_left
import com.paperconnekt.app.resources.filled_chevron_right
import com.paperconnekt.app.resources.filled_close
import com.paperconnekt.app.resources.filled_content_copy
import com.paperconnekt.app.resources.filled_crop_square
import com.paperconnekt.app.resources.filled_description
import com.paperconnekt.app.resources.filled_edit
import com.paperconnekt.app.resources.filled_find_in_page
import com.paperconnekt.app.resources.filled_fit_screen
import com.paperconnekt.app.resources.filled_fullscreen
import com.paperconnekt.app.resources.filled_fullscreen_exit
import com.paperconnekt.app.resources.filled_headphones
import com.paperconnekt.app.resources.filled_keyboard_arrow_down
import com.paperconnekt.app.resources.filled_keyboard_arrow_up
import com.paperconnekt.app.resources.filled_lightbulb
import com.paperconnekt.app.resources.filled_more_vert
import com.paperconnekt.app.resources.filled_note_add
import com.paperconnekt.app.resources.filled_notes
import com.paperconnekt.app.resources.filled_pan_tool
import com.paperconnekt.app.resources.filled_picture_as_pdf
import com.paperconnekt.app.resources.filled_refresh
import com.paperconnekt.app.resources.filled_schedule
import com.paperconnekt.app.resources.filled_search
import com.paperconnekt.app.resources.filled_sync
import com.paperconnekt.app.resources.filled_sync_problem
import com.paperconnekt.app.resources.filled_text_fields
import com.paperconnekt.app.resources.filled_title
import com.paperconnekt.app.resources.filled_tune
import com.paperconnekt.app.resources.filled_view_day
import com.paperconnekt.app.resources.filled_zoom_in
import com.paperconnekt.app.resources.filled_zoom_out
import kotlinx.coroutines.launch

/** Width below which the reader switches to the compact, phone-friendly chrome. */
internal val CompactReaderMaxWidth = 600.dp

internal data class ReaderAnnotationTool(val id: String, val label: String, val icon: DrawableResource)

internal val readerAnnotationTools = listOf(
    ReaderAnnotationTool("note", "Note", Res.drawable.filled_note_add),
    ReaderAnnotationTool("idea", "Idea", Res.drawable.filled_lightbulb),
    ReaderAnnotationTool("ink", "Pen", Res.drawable.filled_edit),
    ReaderAnnotationTool("text", "Text", Res.drawable.filled_title),
    ReaderAnnotationTool("rect", "Box", Res.drawable.filled_crop_square),
)

@Composable
internal fun CompactReaderTopBar(
    title: String,
    filename: String,
    state: PdfWorkspaceState,
    onExit: (() -> Unit)?,
    showSearchAction: Boolean,
    searchActive: Boolean,
    searchCount: Int?,
    onSearch: () -> Unit,
    onAnnotations: () -> Unit,
    focusMode: Boolean,
    onFocusMode: () -> Unit,
    toolsCollapsible: Boolean,
    toolsCollapsed: Boolean,
    onToggleTools: () -> Unit,
    onSync: () -> Unit,
    onGoToPage: (() -> Unit)?,
    studyTimeToday: Long,
    totalStudyTime: Long,
) {
    var menu by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    val syncProblem = state.needsRefresh || state.pendingWrites > 0
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        onExit?.let { exit ->
            IconButton(onClick = exit, modifier = Modifier.interactivePointer()) {
                Icon(painterResource(Res.drawable.filled_arrow_back), "Exit workspace")
            }
        }
        Column(Modifier.weight(1f).padding(start = if (onExit == null) 8.dp else 0.dp, end = 4.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(filename, Modifier.weight(1f, fill = false), style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (studyTimeToday > 0L) Text(" · ${studyDurationLabel(studyTimeToday)} today",
                    style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant, maxLines = 1)
            }
        }
        if (showSearchAction) {
            IconButton(onClick = onSearch, modifier = Modifier.interactivePointer(),
                colors = if (searchActive) IconButtonDefaults.iconButtonColors(containerColor = colors.primaryContainer,
                    contentColor = colors.onPrimaryContainer) else IconButtonDefaults.iconButtonColors()) {
                BadgedBox(badge = { if (searchCount != null) Badge { Text(searchCount.toString()) } }) {
                    Icon(painterResource(Res.drawable.filled_search), "Find text on this page")
                }
            }
        }
        IconButton(onClick = onAnnotations, modifier = Modifier.interactivePointer()) {
            BadgedBox(badge = {
                if (state.annotations.isNotEmpty()) Badge {
                    Text(if (state.annotations.size > 99) "99+" else state.annotations.size.toString())
                }
            }) {
                Icon(painterResource(Res.drawable.filled_notes),
                    "Annotations (${state.annotations.size})")
            }
        }
        Box {
            IconButton(onClick = { menu = true }, modifier = Modifier.interactivePointer()) {
                BadgedBox(badge = {
                    if (state.syncing) Badge(containerColor = colors.primary)
                    else if (syncProblem) Badge()
                }) { Icon(painterResource(Res.drawable.filled_more_vert), "More reader options") }
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, shape = RoundedCornerShape(16.dp)) {
                if (state.localTitle != null) {
                    DropdownMenuItem(
                        text = { Text(state.annotationSaveStatus) },
                        leadingIcon = { Icon(painterResource(Res.drawable.filled_description), null) },
                        enabled = !state.syncing && !state.loading,
                        onClick = { menu = false; onSync() },
                        modifier = Modifier.testTag("local-annotation-save-status"),
                    )
                    HorizontalDivider(Modifier.padding(vertical = 4.dp))
                } else {
                    val syncTint = when {
                        state.syncing -> colors.primary
                        syncProblem -> colors.error
                        else -> if (colors.surface.luminance() < 0.5f) Color(0xFF81C784) else Color(0xFF2E7D32)
                    }
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(when {
                                    state.syncing -> "Syncing with Zotero…"
                                    syncProblem -> "Not synced · Retry"
                                    else -> "Synced with Zotero"
                                })
                                if (!state.syncing && !syncProblem) Text("Tap to refresh annotations",
                                    style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                            }
                        },
                        leadingIcon = { Icon(if (syncProblem) painterResource(Res.drawable.filled_sync_problem) else painterResource(Res.drawable.filled_sync), null, tint = syncTint) },
                        enabled = !state.syncing && !state.loading,
                        onClick = { menu = false; onSync() },
                        modifier = Modifier.testTag("zotero-sync-status").interactivePointer(enabled = !state.syncing && !state.loading),
                    )
                    HorizontalDivider(Modifier.padding(vertical = 4.dp))
                }
                onGoToPage?.let { go ->
                    DropdownMenuItem(text = { Text("Go to page…") }, leadingIcon = { Icon(painterResource(Res.drawable.filled_find_in_page), null) },
                        onClick = { menu = false; go() }, modifier = Modifier.interactivePointer())
                }
                if (toolsCollapsible) DropdownMenuItem(
                    text = { Text(if (toolsCollapsed) "Show tools" else "Hide tools") },
                    leadingIcon = { Icon(if (toolsCollapsed) painterResource(Res.drawable.filled_keyboard_arrow_down) else painterResource(Res.drawable.filled_keyboard_arrow_up), null) },
                    onClick = { menu = false; onToggleTools() }, modifier = Modifier.interactivePointer())
                DropdownMenuItem(
                    text = { Text(if (focusMode) "Exit focus mode" else "Focus mode") },
                    leadingIcon = { Icon(if (focusMode) painterResource(Res.drawable.filled_fullscreen_exit) else painterResource(Res.drawable.filled_fullscreen), null) },
                    onClick = { menu = false; onFocusMode() }, modifier = Modifier.interactivePointer())
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                DropdownMenuItem(
                    text = {
                        Column {
                            Text("Study time")
                            Text("Today ${studyDurationLabel(studyTimeToday)} · Total ${studyDurationLabel(totalStudyTime)}",
                                style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                        }
                    },
                    leadingIcon = { Icon(painterResource(Res.drawable.filled_schedule), null) },
                    onClick = { menu = false }, modifier = Modifier.interactivePointer())
            }
        }
    }
}

@Composable
private fun CompactToolToggle(selected: Boolean, onClick: () -> Unit, icon: Painter, description: String, enabled: Boolean) {
    FilledTonalIconToggleButton(
        checked = selected, onCheckedChange = { onClick() }, enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = IconButtonDefaults.filledTonalIconToggleButtonColors(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        modifier = Modifier.size(40.dp).interactivePointer(enabled = enabled),
    ) { Icon(icon, description, Modifier.size(20.dp)) }
}

@Composable
internal fun CompactReaderToolRow(
    tool: String,
    onToolChange: (String) -> Unit,
    readMode: Boolean,
    onReadModeChange: () -> Unit,
    hasDraft: Boolean,
    inkDraft: Boolean,
    canSaveDrawing: Boolean,
    annotateEnabled: Boolean,
    readingLocked: Boolean,
    colorHex: String,
    onOpenAnnotate: () -> Unit,
    onSaveDrawing: () -> Unit,
    onDiscardDrawing: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val active = readerAnnotationTools.find { it.id == tool }
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        CompactToolToggle(tool == "select", { onToolChange("select") }, painterResource(Res.drawable.filled_text_fields), "Select text", !hasDraft)
        CompactToolToggle(tool == "pan", { onToolChange("pan") }, painterResource(Res.drawable.filled_pan_tool), "Pan", !hasDraft)
        CompactToolToggle(readMode, onReadModeChange, painterResource(Res.drawable.filled_headphones), "Read aloud", !hasDraft)
        VerticalDivider(Modifier.height(24.dp).padding(horizontal = 4.dp), color = colors.outlineVariant.copy(alpha = 0.6f))
        Surface(
            onClick = onOpenAnnotate,
            enabled = annotateEnabled,
            shape = RoundedCornerShape(50),
            color = if (active != null) colors.primaryContainer else colors.surfaceContainerHigh,
            contentColor = if (active != null) colors.onPrimaryContainer else colors.onSurface,
            border = if (active != null) BorderStroke(1.dp, colors.primary.copy(alpha = 0.3f)) else null,
            modifier = Modifier.height(40.dp).testTag("reader-annotate-tools").interactivePointer(enabled = annotateEnabled),
        ) {
            Row(Modifier.padding(start = 12.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painterResource(active?.icon ?: Res.drawable.filled_border_color),
                    null, Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(active?.label ?: "Annotate", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.width(8.dp))
                Box(Modifier.size(14.dp).clip(CircleShape).background(annotationColor(colorHex))
                    .border(1.dp, colors.outline.copy(alpha = 0.4f), CircleShape))
                Icon(painterResource(Res.drawable.filled_arrow_drop_down), "Annotation tools", Modifier.size(20.dp))
            }
        }
        if (inkDraft) {
            FilledIconButton(onClick = onSaveDrawing, enabled = canSaveDrawing && !readingLocked,
                modifier = Modifier.size(40.dp).interactivePointer(enabled = canSaveDrawing && !readingLocked)) {
                Icon(painterResource(Res.drawable.filled_check), "Save drawing", Modifier.size(20.dp))
            }
            IconButton(onClick = onDiscardDrawing, enabled = !readingLocked,
                modifier = Modifier.size(40.dp).interactivePointer(enabled = !readingLocked)) {
                Icon(painterResource(Res.drawable.filled_close), "Discard drawing", Modifier.size(20.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CompactAnnotationSheet(
    tool: String,
    onToolChange: (String) -> Unit,
    colorHex: String,
    onColorChange: (String) -> Unit,
    inkWidth: Float,
    textSize: Float,
    onInkWidthChange: (Float) -> Unit,
    onTextSizeChange: (Float) -> Unit,
    inkOnly: Boolean,
    enabled: Boolean,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val close: () -> Unit = { scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() } }
    val colors = MaterialTheme.colorScheme
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp).testTag("reader-annotate-sheet")) {
            Text("Annotate", style = MaterialTheme.typography.titleLarge)
            Text("Pick a tool, then tap or drag on the page.", style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                readerAnnotationTools.forEach { item ->
                    val selected = tool == item.id
                    val itemEnabled = enabled && (!inkOnly || item.id == "ink")
                    Surface(
                        onClick = { onToolChange(if (selected) "select" else item.id); close() },
                        enabled = itemEnabled,
                        shape = RoundedCornerShape(16.dp),
                        color = if (selected) colors.primaryContainer else colors.surfaceContainerHigh,
                        contentColor = if (selected) colors.onPrimaryContainer else colors.onSurface,
                        border = if (selected) BorderStroke(1.dp, colors.primary.copy(alpha = 0.4f)) else null,
                        modifier = Modifier.weight(1f).height(72.dp).semantics { this.selected = selected }
                            .interactivePointer(enabled = itemEnabled),
                    ) {
                        Column(Modifier.fillMaxSize().padding(4.dp), verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(painterResource(item.icon), null, Modifier.size(22.dp),
                                tint = if (item.id == "ink" && itemEnabled) annotationColor(colorHex) else LocalContentColor.current)
                            Spacer(Modifier.height(6.dp))
                            Text(item.label, style = MaterialTheme.typography.labelMedium, maxLines = 1, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Text("Color", style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                annotationColors.forEachIndexed { index, hex ->
                    val current = colorHex.equals(hex, true)
                    Box(Modifier.size(40.dp).clip(CircleShape).background(annotationColor(hex))
                        .border(if (current) 3.dp else 1.dp, if (current) colors.onSurface else colors.outlineVariant, CircleShape)
                        .semantics { selected = current; contentDescription = "${annotationColorNames[index]} annotation color" }
                        .clickable(enabled = enabled, role = Role.RadioButton) { onColorChange(hex) }
                        .interactivePointer(enabled = enabled),
                        contentAlignment = Alignment.Center) {
                        if (current) Icon(painterResource(Res.drawable.filled_check), null, Modifier.size(18.dp), tint = Color.Black.copy(alpha = 0.7f))
                    }
                }
            }
            if (tool == "ink" || tool == "text") {
                val ink = tool == "ink"
                val current = if (ink) inkWidth else textSize
                Spacer(Modifier.height(20.dp))
                Text(if (ink) "Line width" else "Text size", style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (if (ink) inkWidthPresets else textSizePresets).forEach { size ->
                        FilterChip(
                            selected = size == current,
                            onClick = { if (ink) onInkWidthChange(size) else onTextSizeChange(size) },
                            enabled = enabled,
                            label = { Text(formatSize(size)) },
                            leadingIcon = if (ink) { {
                                Box(Modifier.width(16.dp).height((size * 0.75f).coerceIn(1f, 8f).dp)
                                    .background(annotationColor(colorHex), RoundedCornerShape(50)))
                            } } else null,
                            modifier = Modifier.interactivePointer(enabled = enabled),
                        )
                    }
                }
            }
            if (readerAnnotationTools.any { it.id == tool }) {
                Spacer(Modifier.height(20.dp))
                OutlinedButton(onClick = { onToolChange("select"); close() }, enabled = !inkOnly,
                    modifier = Modifier.fillMaxWidth().interactivePointer(enabled = !inkOnly)) {
                    Icon(painterResource(Res.drawable.filled_text_fields), null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Back to text selection")
                }
            }
        }
    }
}

@Composable
internal fun CompactReaderBottomBar(
    pageLabel: String,
    canPrevious: Boolean,
    canNext: Boolean,
    pageEnabled: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onPageClick: () -> Unit,
    pdfMode: Boolean,
    zoomLabel: String,
    onViewOptions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        shadowElevation = 8.dp,
        tonalElevation = 4.dp,
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.96f),
    ) {
        Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(enabled = canPrevious, onClick = onPrevious, modifier = Modifier.interactivePointer(enabled = canPrevious)) {
                Icon(painterResource(Res.drawable.filled_chevron_left), "Previous page")
            }
            TextButton(enabled = pageEnabled, onClick = onPageClick, contentPadding = PaddingValues(horizontal = 8.dp),
                modifier = Modifier.interactivePointer(enabled = pageEnabled)) {
                Text(pageLabel, style = MaterialTheme.typography.titleSmall)
            }
            IconButton(enabled = canNext, onClick = onNext, modifier = Modifier.interactivePointer(enabled = canNext)) {
                Icon(painterResource(Res.drawable.filled_chevron_right), "Next page")
            }
            VerticalDivider(Modifier.height(20.dp).padding(horizontal = 4.dp))
            TextButton(onClick = onViewOptions, contentPadding = PaddingValues(horizontal = 10.dp),
                modifier = Modifier.testTag("reader-view-options").interactivePointer()) {
                Icon(if (pdfMode) painterResource(Res.drawable.filled_picture_as_pdf) else painterResource(Res.drawable.filled_article), null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(zoomLabel)
                Icon(painterResource(Res.drawable.filled_tune), "View options", Modifier.padding(start = 6.dp).size(18.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CompactViewOptionsSheet(
    pdfMode: Boolean,
    canShowPdf: Boolean,
    canShowMarkdown: Boolean,
    onShowPdf: () -> Unit,
    onShowMarkdown: () -> Unit,
    zoomLabel: String,
    canZoomOut: Boolean,
    canZoomIn: Boolean,
    onZoomOut: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomReset: () -> Unit,
    fitPage: Boolean,
    fitEnabled: Boolean,
    onToggleFit: () -> Unit,
    spread: Boolean,
    layoutEnabled: Boolean,
    onSpreadChange: (Boolean) -> Unit,
    continuous: Boolean,
    onContinuousChange: () -> Unit,
    canCopyMarkdown: Boolean,
    onCopyMarkdown: () -> Unit,
    canRegenerate: Boolean,
    regenerateLabel: String,
    onRegenerate: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Text("View", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))
            SheetLabel("Document")
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().testTag("reader-view-mode")) {
                SegmentedButton(selected = pdfMode, onClick = onShowPdf, enabled = canShowPdf,
                    shape = SegmentedButtonDefaults.itemShape(0, 2), modifier = Modifier.interactivePointer(enabled = canShowPdf),
                    icon = { SegmentedButtonDefaults.Icon(active = pdfMode) { Icon(painterResource(Res.drawable.filled_picture_as_pdf), null, Modifier.size(18.dp)) } }
                ) { Text("PDF") }
                SegmentedButton(selected = !pdfMode, onClick = onShowMarkdown, enabled = canShowMarkdown,
                    shape = SegmentedButtonDefaults.itemShape(1, 2), modifier = Modifier.interactivePointer(enabled = canShowMarkdown),
                    icon = { SegmentedButtonDefaults.Icon(active = !pdfMode) { Icon(painterResource(Res.drawable.filled_article), null, Modifier.size(18.dp)) } }
                ) { Text("Markdown") }
            }
            Spacer(Modifier.height(20.dp))
            SheetLabel("Zoom")
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                FilledTonalIconButton(onClick = onZoomOut, enabled = canZoomOut, modifier = Modifier.interactivePointer(enabled = canZoomOut)) {
                    Icon(painterResource(Res.drawable.filled_zoom_out), "Zoom out")
                }
                TextButton(onClick = onZoomReset, modifier = Modifier.weight(1f).interactivePointer()) {
                    Text(zoomLabel, style = MaterialTheme.typography.titleMedium)
                }
                FilledTonalIconButton(onClick = onZoomIn, enabled = canZoomIn, modifier = Modifier.interactivePointer(enabled = canZoomIn)) {
                    Icon(painterResource(Res.drawable.filled_zoom_in), "Zoom in")
                }
                Spacer(Modifier.width(8.dp))
                FilterChip(selected = fitPage, onClick = onToggleFit, enabled = fitEnabled, label = { Text("Fit") },
                    leadingIcon = { Icon(painterResource(Res.drawable.filled_fit_screen), "Toggle fit page", Modifier.size(18.dp)) },
                    modifier = Modifier.interactivePointer(enabled = fitEnabled))
            }
            Spacer(Modifier.height(20.dp))
            SheetLabel("Layout")
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(selected = !spread, onClick = { onSpreadChange(false) }, enabled = layoutEnabled,
                    shape = SegmentedButtonDefaults.itemShape(0, 2), modifier = Modifier.interactivePointer(enabled = layoutEnabled),
                    icon = { SegmentedButtonDefaults.Icon(active = !spread) { Icon(painterResource(Res.drawable.filled_description), null, Modifier.size(18.dp)) } }
                ) { Text("Single") }
                SegmentedButton(selected = spread, onClick = { onSpreadChange(true) }, enabled = layoutEnabled,
                    shape = SegmentedButtonDefaults.itemShape(1, 2), modifier = Modifier.interactivePointer(enabled = layoutEnabled),
                    icon = { SegmentedButtonDefaults.Icon(active = spread) { Icon(painterResource(Res.drawable.filled_auto_stories), null, Modifier.size(18.dp)) } }
                ) { Text("Two pages") }
            }
            ListItem(
                headlineContent = { Text("Continuous scrolling") },
                leadingContent = { Icon(painterResource(Res.drawable.filled_view_day), null) },
                trailingContent = { Switch(checked = continuous, onCheckedChange = { onContinuousChange() }, enabled = layoutEnabled) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clip(RoundedCornerShape(12.dp))
                    .clickable(enabled = layoutEnabled, role = Role.Switch) { onContinuousChange() }
                    .interactivePointer(enabled = layoutEnabled),
            )
            if (!pdfMode) {
                HorizontalDivider(Modifier.padding(vertical = 8.dp), color = colors.outlineVariant.copy(alpha = 0.5f))
                ListItem(
                    headlineContent = { Text("Copy Markdown") },
                    leadingContent = { Icon(painterResource(Res.drawable.filled_content_copy), null) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                        .clickable(enabled = canCopyMarkdown) { onCopyMarkdown(); onDismiss() }
                        .interactivePointer(enabled = canCopyMarkdown),
                )
                ListItem(
                    headlineContent = { Text("Convert again") },
                    supportingContent = { Text(regenerateLabel) },
                    leadingContent = { Icon(painterResource(Res.drawable.filled_refresh), null) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                        .clickable(enabled = canRegenerate) { onRegenerate(); onDismiss() }
                        .interactivePointer(enabled = canRegenerate),
                )
            }
        }
    }
}

@Composable
private fun SheetLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp))
}
