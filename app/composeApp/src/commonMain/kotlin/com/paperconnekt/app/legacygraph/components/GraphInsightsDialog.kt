package com.paperconnekt.app.legacygraph.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.paperconnekt.app.legacygraph.clipChipInteraction
import com.paperconnekt.app.legacygraph.interactivePointer
import com.paperconnekt.app.legacygraph.ConnectionType
import com.paperconnekt.app.legacygraph.GuidedInsights
import com.paperconnekt.app.legacygraph.GuidedRelation
import com.paperconnekt.app.legacygraph.GuidedReviewDirection
import com.paperconnekt.app.legacygraph.GuidedReviewState
import com.paperconnekt.app.legacygraph.ImpactQuadrant
import com.paperconnekt.app.legacygraph.InsightPaper
import com.paperconnekt.app.legacygraph.InsightRankedLabel
import com.paperconnekt.app.legacygraph.InsightStatus
import com.paperconnekt.app.legacygraph.InsightInterval
import com.paperconnekt.app.legacygraph.PaperNode
import com.paperconnekt.app.legacygraph.RelevanceAiState
import com.paperconnekt.app.legacygraph.RelevanceQuadrant
import com.paperconnekt.app.legacygraph.RelevanceProfile
import com.paperconnekt.domain.model.Paper
import com.paperconnekt.app.legacygraph.buildGuidedInsights
import com.paperconnekt.app.legacygraph.currentInsightYear
import com.paperconnekt.app.legacygraph.includes
import com.paperconnekt.app.legacygraph.insightCitationScale
import com.paperconnekt.app.legacygraph.insightYear
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.pow
import kotlin.math.log10
import kotlinx.coroutines.launch

private enum class InsightTab(val label: String, val icon: ImageVector) {
    IMPACT("Impact map", Icons.Default.ScatterPlot),
    RELEVANCE("Relevance map", Icons.Default.BubbleChart),
    RISING("Fastest rising", Icons.Default.Whatshot),
    TIMELINE("Timeline", Icons.Default.BarChart),
    PEOPLE("Authors & venues", Icons.Default.Groups)
}

private fun Float.coerceInSafe(minimum: Float, maximum: Float): Float =
    if (maximum < minimum) minimum else coerceIn(minimum, maximum)

private enum class InsightColorMode(val label: String) { QUADRANT("Quadrant"), STATUS("Review status"), RELATION("Relation") }

private enum class RelevanceSource(val label: String) { AI("AI target match"), SIMILARITY("Seed similarity") }

private enum class RelevanceYAxis(val label: String) { CITATIONS("Citations"), YEAR("Publication year") }

/**
 * Plots of the selected paper's citations and references (year × citations, similarity × citations,
 * citation velocity, timeline, authors and venues) so the user can spot what matters and keep or
 * skip papers directly from a plot.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GuidedInsightsDialog(
    node: PaperNode?,
    review: GuidedReviewState,
    keptElsewhere: Set<String>,
    onLoad: () -> Unit,
    onKeep: (String) -> Unit,
    onSkip: (String) -> Unit,
    onUndo: (String) -> Unit,
    onDismiss: () -> Unit,
    graphTitle: String? = null,
    relevance: RelevanceAiState = RelevanceAiState(),
    onSaveRelevanceProfile: (RelevanceProfile, List<Paper>) -> Unit = { _, _ -> },
    onEvaluateRelevance: (List<Paper>) -> Unit = {},
    onStopRelevance: () -> Unit = {},
    onResetRelevance: () -> Unit = {},
    /** Removes a non-seed paper the user judged off-topic from the graph; null hides the action. */
    onDiscard: ((String) -> Unit)? = null,
    onRestoreDiscarded: (String) -> Unit = {}
) {
    val graphOverview = node == null
    val tabs = InsightTab.entries
    LaunchedEffect(node?.paper?.id) { if (!review.isQueueLoaded) onLoad() }
    var tab by remember { mutableStateOf(InsightTab.IMPACT) }
    var relation by remember(node?.paper?.id) { mutableStateOf(review.direction) }
    var onlyKept by remember { mutableStateOf(true) }
    var selectedColorMode by remember { mutableStateOf(InsightColorMode.QUADRANT) }
    val colorMode = if (graphOverview) InsightColorMode.QUADRANT else selectedColorMode
    var selectedId by remember(node?.paper?.id) { mutableStateOf<String?>(null) }
    var showControls by remember { mutableStateOf(false) }
    var compactDetails by remember { mutableStateOf(false) }
    var lastDiscarded by remember { mutableStateOf<Pair<String, String>?>(null) }
    val discard: ((String) -> Unit)? = onDiscard?.let { action ->
        { id: String ->
            val title = review.candidates.firstOrNull { it.node.paper.id == id }?.node?.paper?.title.orEmpty()
            action(id)
            lastDiscarded = id to title
            selectedId = null
        }
    }
    var interval by remember(node?.paper?.id) { mutableStateOf(InsightInterval()) }
    val isScatter = tab == InsightTab.IMPACT || tab == InsightTab.RELEVANCE
    val currentYear = remember { currentInsightYear() }
    var relevanceSource by remember {
        mutableStateOf(if (relevance.isAvailable) RelevanceSource.AI else RelevanceSource.SIMILARITY)
    }
    var relevanceDraft by remember(relevance.profile) { mutableStateOf(relevance.profile.withStarterCriterion()) }
    var relevanceYAxis by remember { mutableStateOf(RelevanceYAxis.CITATIONS) }
    val aiScores = if (relevanceSource == RelevanceSource.AI) relevance.scores else null
    val insights = remember(review, keptElsewhere, relation, onlyKept, currentYear, interval, isScatter, aiScores) {
        val candidates = review.candidates.filter { candidate ->
            (graphOverview || relation.includes(candidate.relation)) &&
                (graphOverview || !onlyKept || candidate.node.paper.id in review.keptIds || candidate.node.paper.id in keptElsewhere) &&
                (!isScatter || interval.includes(candidate))
        }
        buildGuidedInsights(review, candidates, keptElsewhere, currentYear, aiScores = aiScores)
    }
    val visible = insights.papers
    val selected = visible.firstOrNull { it.id == selectedId }
    val palette = insightPalette()

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
        val compact = maxWidth < 1000.dp || maxHeight < 600.dp
        val compactContentHeight = maxOf(maxHeight - 240.dp, if (isScatter || tab == InsightTab.TIMELINE) 640.dp else 500.dp)
        Surface(
            Modifier.align(Alignment.Center).widthIn(max = 1400.dp)
                .fillMaxWidth(if (compact) 1f else 0.95f).fillMaxHeight(if (compact) 1f else 0.92f)
                .safeDrawingPadding().testTag("guided-insights"),
            shape = if (compact) RoundedCornerShape(0.dp) else MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 6.dp
        ) {
            Column(
                Modifier.then(if (compact) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                    .padding(if (compact) 12.dp else 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.Insights, null, tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f)) {
                        Text(if (graphOverview) "Graph insights" else "Neighborhood insights", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text(
                            if (graphOverview) "${visible.size} articles in “${graphTitle.orEmpty()}”"
                            else "${review.referenceCount} references · ${review.citationCount} citations of “${node?.paper?.title}”",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.interactivePointer()) { Icon(Icons.Default.Close, "Close insights") }
                }

                val relationFilter: @Composable () -> Unit = {
                    GuidedReviewDirection.entries.forEach { direction ->
                        FilterChip(
                            selected = relation == direction,
                            onClick = { relation = direction },
                            label = {
                                Text(when (direction) {
                                    GuidedReviewDirection.BOTH -> "All"
                                    GuidedReviewDirection.REFERENCES -> "References"
                                    GuidedReviewDirection.CITATIONS -> "Citations"
                                })
                            },
                            colors = insightChipColors(),
                            border = insightChipBorder(relation == direction),
                            modifier = Modifier.clipChipInteraction().interactivePointer(),
                        )
                    }
                }
                val colorFilter: @Composable () -> Unit = {
                    InsightColorMode.entries.forEach { mode ->
                        FilterChip(
                            selected = colorMode == mode,
                            onClick = { selectedColorMode = mode },
                            label = { Text(mode.label) },
                            colors = insightChipColors(),
                            border = insightChipBorder(colorMode == mode),
                            modifier = Modifier.clipChipInteraction().interactivePointer(),
                        )
                    }
                }
                val showRelevanceSource = tab == InsightTab.RELEVANCE && relevance.isAvailable
                val relevanceSourceFilter: @Composable () -> Unit = {
                    RelevanceSourceSelector(relevanceSource, { relevanceSource = it })
                }
                val showRelevanceYAxis = tab == InsightTab.RELEVANCE
                val relevanceYAxisFilter: @Composable () -> Unit = {
                    RelevanceYAxis.entries.forEach { axis ->
                        FilterChip(
                            selected = relevanceYAxis == axis,
                            onClick = { relevanceYAxis = axis },
                            label = { Text(axis.label) },
                            leadingIcon = {
                                Icon(if (axis == RelevanceYAxis.YEAR) Icons.Default.CalendarMonth else Icons.Default.FormatQuote,
                                    null, Modifier.size(16.dp))
                            },
                            colors = insightChipColors(),
                            border = insightChipBorder(relevanceYAxis == axis),
                            modifier = Modifier.clipChipInteraction().interactivePointer(),
                        )
                    }
                }
                val filters: @Composable () -> Unit = {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        itemVerticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (!graphOverview) {
                            relationFilter()
                            OnlyKeptToggle(onlyKept, { onlyKept = it })
                        }
                        if (showRelevanceSource) relevanceSourceFilter()
                        if (showRelevanceYAxis) {
                            Text("Y axis", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            relevanceYAxisFilter()
                        }
                        if (isScatter && !graphOverview) {
                            Text("Color by", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            colorFilter()
                        }
                        if (isScatter && review.isQueueLoaded) {
                            InsightIntervalControls(interval, onApply = { interval = it })
                        }
                    }
                }
                if (!compact) filters()
                if (showControls) {
                    ModalBottomSheet(
                        onDismissRequest = { showControls = false },
                        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ) {
                        Column(
                            Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState())
                                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp)
                                .testTag("insight-filters-sheet"),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Tune, null, tint = MaterialTheme.colorScheme.primary)
                                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                    Text("Insight filters", style = MaterialTheme.typography.titleMedium)
                                    Text("Changes apply to the charts immediately",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                GraphAction(Icons.Default.Close, "Close insight filters", { showControls = false })
                            }
                            if (!graphOverview) {
                                InsightFilterSection("Relation") {
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { relationFilter() }
                                }
                                OnlyKeptToggle(onlyKept, { onlyKept = it }, Modifier.fillMaxWidth())
                            }
                            if (showRelevanceSource) InsightFilterSection("Relevance measure") { relevanceSourceFilter() }
                            if (showRelevanceYAxis) InsightFilterSection("Y axis") {
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { relevanceYAxisFilter() }
                            }
                            if (isScatter && !graphOverview) InsightFilterSection("Color points by") {
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { colorFilter() }
                            }
                            if (isScatter && review.isQueueLoaded) InsightFilterSection("Focus interval") {
                                InsightIntervalForm(interval, onApply = { interval = it })
                            }
                            if (!isScatter && graphOverview) Text(
                                "This view has no extra filters.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                /*Text(
                    if (graphOverview) "Seeds + kept articles across all reviews"
                    else if (onlyKept) "${visible.size} kept articles · Uncheck to explore all"
                    else "Showing all ${visible.size} articles in the selected relation.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )*/

                PrimaryScrollableTabRow(selectedTabIndex = tabs.indexOf(tab), containerColor = Color.Transparent, edgePadding = 0.dp) {
                    tabs.forEach { entry ->
                        Tab(
                            selected = tab == entry,
                            onClick = { tab = entry; compactDetails = false },
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(entry.icon, null, Modifier.size(16.dp))
                                    Text(entry.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            },
                            modifier = Modifier.clipChipInteraction().interactivePointer()
                        )
                    }
                }

                if (compact) FlowRow(
                    Modifier.fillMaxWidth(),
                    itemVerticalAlignment = Alignment.CenterVertically,
                ) {
                    GraphAction(Icons.Default.Tune, "Insight filters", { showControls = true })
                    TextButton(onClick = { compactDetails = !compactDetails }, modifier = Modifier.interactivePointer()) {
                        Icon(if (compactDetails) Icons.Default.ScatterPlot else Icons.Default.Info, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (compactDetails) "Back to chart" else if (selected != null) "Paper details"
                            else if (tab == InsightTab.RELEVANCE) "Research target" else "How to read it")
                    }
                    if (tab == InsightTab.RELEVANCE && selected != null) {
                        TextButton(onClick = { selectedId = null; compactDetails = true }, modifier = Modifier.interactivePointer()) {
                            Text("Research target")
                        }
                    }
                }

                lastDiscarded?.let { (id, title) ->
                    DiscardedBar(
                        title = title,
                        onUndo = { onRestoreDiscarded(id); lastDiscarded = null; selectedId = id; compactDetails = true },
                        onClose = { lastDiscarded = null }
                    )
                }

                Box(
                    Modifier.fillMaxWidth().then(
                        if (compact) Modifier.height(compactContentHeight) else Modifier.weight(1f)
                    )
                ) {
                when {
                    !review.isQueueLoaded && review.error != null -> InsightMessage(review.error, isError = true)
                    !review.isQueueLoaded -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            CircularProgressIndicator()
                            Text(review.loadingMessage ?: "Loading citations and references…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    insights.papers.isEmpty() -> InsightMessage(
                        if (isScatter && interval != InsightInterval()) "No articles in this interval. Widen the interval or select Reset."
                        else if (graphOverview) "No seeds or kept articles in this graph yet."
                        else if (onlyKept) "No kept articles in this selection yet. Uncheck “Only kept articles” to explore all articles."
                        else "No citations or references in this selection."
                    )
                    else -> BoxWithConstraints(Modifier.fillMaxSize()) {
                        val content: @Composable (Modifier) -> Unit = { m ->
                            InsightTabContent(
                                tab, insights, visible, node, colorMode, palette, selectedId,
                                { selectedId = it; if (it != null) compactDetails = true }, !onlyKept, interval, m,
                                relevance = relevance,
                                relevanceYAxis = relevanceYAxis,
                                onEvaluate = { onEvaluateRelevance(visible.map { it.candidate.node.paper }) }
                            )
                        }
                        val details: @Composable (Modifier) -> Unit = { m ->
                            if (selected == null && tab == InsightTab.RELEVANCE) RelevanceTargetPanel(
                                draft = relevanceDraft,
                                onDraftChange = { relevanceDraft = it },
                                relevance = relevance,
                                visibleIds = visible.map { it.id },
                                onSave = { evaluate ->
                                    relevanceSource = RelevanceSource.AI
                                    onSaveRelevanceProfile(relevanceDraft.withoutBlankCriteria(),
                                        if (evaluate) visible.map { it.candidate.node.paper } else emptyList())
                                },
                                onEvaluate = {
                                    relevanceSource = RelevanceSource.AI
                                    onEvaluateRelevance(visible.map { it.candidate.node.paper })
                                },
                                onStop = onStopRelevance,
                                onReset = onResetRelevance,
                                modifier = m
                            ) else InsightDetails(selected, insights, tab, palette, review, onKeep, onSkip, onUndo, m, graphOverview, discard)
                        }
                        if (!compact) Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            content(Modifier.weight(1f).fillMaxHeight())
                            SelectionContainer(Modifier.width(380.dp).fillMaxHeight()) {
                                details(Modifier.fillMaxSize())
                            }
                        } else if (compactDetails) {
                            SelectionContainer(Modifier.fillMaxSize()) {
                                details(Modifier.fillMaxSize())
                            }
                        } else content(Modifier.fillMaxSize())
                    }
                }
                }
            }
        }
        }
    }
}

@Composable
private fun InsightIntervalControls(interval: InsightInterval, onApply: (InsightInterval) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val active = interval != InsightInterval()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box {
            FilterChip(
                selected = active,
                onClick = { expanded = true },
                label = { Text(if (active) interval.summary() else "Focus interval", maxLines = 1) },
                leadingIcon = { Icon(Icons.Default.Tune, null, Modifier.size(16.dp)) },
                trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, Modifier.size(16.dp)) },
                colors = insightChipColors(),
                border = insightChipBorder(active),
                modifier = Modifier.clipChipInteraction().interactivePointer()
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                shape = MaterialTheme.shapes.large,
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Column(
                    Modifier.width(320.dp).padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Focus your view", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    InsightIntervalForm(interval, onApply = { onApply(it); expanded = false }, onCancel = { expanded = false })
                }
            }
        }
        if (active) IconButton(
            onClick = { onApply(InsightInterval()) },
            modifier = Modifier.interactivePointer()
        ) { Icon(Icons.Default.RestartAlt, "Reset focus interval", Modifier.size(18.dp)) }
    }
}

private fun InsightInterval.summary(): String = listOfNotNull(
    if (firstYear != null || lastYear != null) "${firstYear ?: "Any"}–${lastYear ?: "Any"}" else null,
    if (minCitations != null || maxCitations != null) "${minCitations ?: 0}–${maxCitations ?: "Any"} citations" else null
).joinToString(" · ")

/** Year/citation interval fields; shown inline in the compact filter sheet and inside the desktop menu. */
@Composable
private fun InsightIntervalForm(
    interval: InsightInterval,
    onApply: (InsightInterval) -> Unit,
    onCancel: (() -> Unit)? = null,
) {
    var firstYear by remember(interval) { mutableStateOf(interval.firstYear?.toString().orEmpty()) }
    var lastYear by remember(interval) { mutableStateOf(interval.lastYear?.toString().orEmpty()) }
    var minCitations by remember(interval) { mutableStateOf(interval.minCitations?.toString().orEmpty()) }
    var maxCitations by remember(interval) { mutableStateOf(interval.maxCitations?.toString().orEmpty()) }
    val yearFrom = firstYear.toIntOrNull()
    val yearTo = lastYear.toIntOrNull()
    val citationFrom = minCitations.toIntOrNull()
    val citationTo = maxCitations.toIntOrNull()
    val invalid = listOf(firstYear, lastYear, minCitations, maxCitations)
        .any { it.isNotBlank() && (it.toIntOrNull() == null || it.toInt() < 0) } ||
        (yearFrom != null && yearFrom !in 1000..9999) ||
        (yearTo != null && yearTo !in 1000..9999) ||
        (yearFrom != null && yearTo != null && yearFrom > yearTo) ||
        (citationFrom != null && citationTo != null && citationFrom > citationTo)
    val draft = InsightInterval(yearFrom, yearTo, citationFrom, citationTo)
    val numeric = KeyboardOptions(keyboardType = KeyboardType.Number)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Hide outliers without changing your graph. Leave a field blank for no limit.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(firstYear, { firstYear = it }, label = { Text("Year from") },
                singleLine = true, isError = invalid, keyboardOptions = numeric, modifier = Modifier.weight(1f))
            OutlinedTextField(lastYear, { lastYear = it }, label = { Text("Year to") },
                singleLine = true, isError = invalid, keyboardOptions = numeric, modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(minCitations, { minCitations = it }, label = { Text("Min citations") },
                singleLine = true, isError = invalid, keyboardOptions = numeric, modifier = Modifier.weight(1f))
            OutlinedTextField(maxCitations, { maxCitations = it }, label = { Text("Max citations") },
                singleLine = true, isError = invalid, keyboardOptions = numeric, modifier = Modifier.weight(1f))
        }
        Text(
            if (invalid) "Use years 1000–9999 and non-negative citation counts. Minimum must not exceed maximum."
            else "Applies to both maps. Quadrants recalculate for the focused articles.",
            style = MaterialTheme.typography.labelSmall,
            color = if (invalid) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
            if (onCancel != null) TextButton(onClick = onCancel, modifier = Modifier.interactivePointer()) { Text("Cancel") }
            else if (interval != InsightInterval()) TextButton(
                onClick = { onApply(InsightInterval()) },
                modifier = Modifier.interactivePointer(),
            ) { Text("Reset") }
            Button(
                onClick = { onApply(draft) },
                enabled = !invalid && draft != interval,
                modifier = Modifier.interactivePointer(enabled = !invalid && draft != interval)
            ) { Text("Apply interval") }
        }
    }
}

@Composable
private fun insightChipColors() = FilterChipDefaults.filterChipColors(
    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
    selectedLeadingIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
    selectedTrailingIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
)

@Composable
private fun insightChipBorder(selected: Boolean) = FilterChipDefaults.filterChipBorder(
    enabled = true,
    selected = selected,
    borderColor = MaterialTheme.colorScheme.outlineVariant,
    selectedBorderColor = Color.Transparent,
)

@Composable
private fun RelevanceSourceSelector(source: RelevanceSource, onChange: (RelevanceSource) -> Unit) {
    SingleChoiceSegmentedButtonRow {
        RelevanceSource.entries.forEachIndexed { index, entry ->
            SegmentedButton(
                selected = source == entry,
                onClick = { onChange(entry) },
                shape = SegmentedButtonDefaults.itemShape(index, RelevanceSource.entries.size),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    activeContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    inactiveContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    activeBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    inactiveBorderColor = MaterialTheme.colorScheme.outlineVariant,
                ),
                icon = {},
                modifier = Modifier.interactivePointer()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(if (entry == RelevanceSource.AI) Icons.Default.AutoAwesome else Icons.Default.TextFields, null, Modifier.size(14.dp))
                    Text(entry.label, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun InsightFilterSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        content()
    }
}

/** Rounded, theme-coloured switch row; the whole surface is the touch target so no grey highlight box appears. */
@Composable
private fun OnlyKeptToggle(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Surface(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier.interactivePointer(),
        shape = RoundedCornerShape(12.dp),
        color = if (checked) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = if (checked) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Row(
            Modifier.padding(start = 12.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Only kept articles", Modifier.weight(1f, fill = false), style = MaterialTheme.typography.labelLarge)
            Switch(checked = checked, onCheckedChange = null, modifier = Modifier.scale(0.8f))
        }
    }
}

@Composable
private fun InsightMessage(text: String, isError: Boolean = false) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private class InsightPalette(
    val impact: Map<ImpactQuadrant, Color>,
    val relevance: Map<RelevanceQuadrant, Color>,
    val status: Map<InsightStatus, Color>,
    val relation: Map<GuidedRelation, Color>
)

@Composable
private fun insightPalette(): InsightPalette {
    val colors = MaterialTheme.colorScheme
    val dark = colors.surface.luminance() < 0.5f
    fun pick(light: Long, darkColor: Long) = Color(if (dark) darkColor else light)
    val orange = pick(0xFFE65100, 0xFFFFB74D)
    val blue = pick(0xFF1565C0, 0xFF90CAF9)
    val green = pick(0xFF2E7D32, 0xFFA5D6A7)
    val purple = pick(0xFF6A1B9A, 0xFFCE93D8)
    val teal = pick(0xFF00796B, 0xFF80CBC4)
    return InsightPalette(
        impact = mapOf(
            ImpactQuadrant.RISING_STARS to orange,
            ImpactQuadrant.CLASSICS to blue,
            ImpactQuadrant.EMERGING to green,
            ImpactQuadrant.OVERLOOKED to colors.outline
        ),
        relevance = mapOf(
            RelevanceQuadrant.MUST_READ to purple,
            RelevanceQuadrant.HIDDEN_GEMS to teal,
            RelevanceQuadrant.POPULAR to blue,
            RelevanceQuadrant.PERIPHERAL to colors.outline
        ),
        status = mapOf(
            InsightStatus.KEPT to green,
            InsightStatus.SKIPPED to colors.outline,
            InsightStatus.AI_KEEP to teal,
            InsightStatus.AI_SKIP to orange,
            InsightStatus.IN_BATCH to purple,
            InsightStatus.UNREVIEWED to blue
        ),
        relation = mapOf(GuidedRelation.REFERENCE to blue, GuidedRelation.CITATION to orange)
    )
}

private fun InsightPalette.colorOf(paper: InsightPaper, insights: GuidedInsights, tab: InsightTab, mode: InsightColorMode): Color =
    when (mode) {
        InsightColorMode.STATUS -> status.getValue(paper.status)
        InsightColorMode.RELATION -> relation.getValue(paper.candidate.relation)
        InsightColorMode.QUADRANT -> if (tab == InsightTab.RELEVANCE)
            insights.relevanceOf(paper)?.let { relevance.getValue(it) } ?: status.getValue(InsightStatus.SKIPPED)
        else insights.impactOf(paper)?.let { impact.getValue(it) } ?: status.getValue(InsightStatus.SKIPPED)
    }

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InsightTabContent(
    tab: InsightTab,
    insights: GuidedInsights,
    visible: List<InsightPaper>,
    node: PaperNode?,
    colorMode: InsightColorMode,
    palette: InsightPalette,
    selectedId: String?,
    onSelect: (String?) -> Unit,
    showKeptOverlay: Boolean,
    interval: InsightInterval,
    modifier: Modifier,
    relevance: RelevanceAiState = RelevanceAiState(),
    relevanceYAxis: RelevanceYAxis = RelevanceYAxis.CITATIONS,
    onEvaluate: () -> Unit = {}
) {
    when (tab) {
        InsightTab.IMPACT -> {
            val dated = visible.filter { it.year != null }
            val allYears = insights.papers.mapNotNull { it.year }
            if (allYears.isEmpty()) {
                Box(modifier, contentAlignment = Alignment.Center) { Text("No publication years are available for these papers.") }
                return
            }
            val first = interval.firstYear ?: allYears.min()
            val last = interval.lastYear ?: allYears.max()
            val span = (last - first).coerceAtLeast(1)
            val step = ((span + 11) / 12).coerceAtLeast(1)
            Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InsightLegend(colorMode, insights, dated, InsightTab.IMPACT, palette)
                InsightScatter(
                    points = dated.map { paper ->
                        val jitter = (abs(paper.id.hashCode()) % 1000) / 1000.0 - 0.5
                        ScatterPoint(paper, paper.year!! + jitter * 0.5, palette.colorOf(paper, insights, InsightTab.IMPACT, colorMode),
                            if (node == null) 5f else 3.5f + paper.similarity * 6f)
                    },
                    xMin = first - 0.5,
                    xMax = last + 0.5,
                    xTicks = (first..last step step).map { it.toDouble() to it.toString() },
                    xSplit = insights.splitYear?.let { it - 0.5 },
                    xSplitCaption = insights.splitYear?.let { "since $it" },
                    ySplitCitations = insights.splitCitations,
                    yMaxCitations = interval.maxCitations ?: insights.papers.maxOf { it.citations },
                    yMinCitations = interval.minCitations ?: 0,
                    corners = ImpactQuadrant.entries.map { quadrant ->
                        ScatterCorner(
                            "${quadrant.label} · ${dated.count { insights.impactOf(it) == quadrant }}",
                            palette.impact.getValue(quadrant),
                            right = quadrant == ImpactQuadrant.RISING_STARS || quadrant == ImpactQuadrant.EMERGING,
                            top = quadrant == ImpactQuadrant.RISING_STARS || quadrant == ImpactQuadrant.CLASSICS
                        )
                    },
                    marker = node?.let(::insightYear)?.let { it.toDouble() to "This paper · $it" },
                    tooltip = { "${it.year} · ${it.citations} citations" +
                        if (node == null) "" else " · ${(it.similarity * 100).roundToInt()}% similar" },
                    description = "Publication year versus citations of ${dated.size} papers.",
                    selectedId = selectedId,
                    onSelect = onSelect,
                    modifier = Modifier.weight(1f).fillMaxWidth()
                )
                val hidden = insights.missingYear
                Text(
                    (if (node == null) "Split at the median year and the median citation count."
                    else "Point size = text similarity to this paper. Split at the median year and the median citation count.") +
                        if (hidden > 0) " $hidden papers without a year are not shown." else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        InsightTab.RELEVANCE -> {
            val ai = insights.usesAiRelevance
            val evaluated = if (ai) visible.filter { it.aiRelevance != null } else visible
            val byYear = relevanceYAxis == RelevanceYAxis.YEAR
            val plotted = if (byYear) evaluated.filter { it.year != null } else evaluated
            val notEvaluated = visible.size - evaluated.size
            val yearAxis = if (!byYear) null else insights.papers.mapNotNull { it.year }.takeIf { it.isNotEmpty() }?.let { years ->
                val first = interval.firstYear ?: years.min()
                val last = maxOf(interval.lastYear ?: years.max(), first)
                val step = ((last - first + 5) / 5).coerceAtLeast(1)
                ScatterYAxis(
                    min = first - 0.5,
                    max = last + 0.5,
                    value = { paper ->
                        val jitter = (abs(paper.id.hashCode()) % 1000) / 1000.0 - 0.5
                        paper.year!! + jitter * 0.5
                    },
                    ticks = (first..last step step).map { it.toDouble() to it.toString() },
                    split = insights.splitYear?.let { it - 0.5 },
                    splitCaption = insights.splitYear?.let { "since $it" },
                )
            }
            val maxX = if (ai) 1.0 else (insights.papers.maxOf { it.similarity }.toDouble()).coerceAtLeast(0.05)
            val ticks = (0..4).map { maxX * it / 4 }.map { it to "${(it * 100).roundToInt()}%" }
            val closeTo = if (node == null) "a graph seed" else "this paper"
            fun percent(paper: InsightPaper) = ((insights.relevanceValue(paper) ?: 0f) * 100).roundToInt()
            Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InsightLegend(colorMode, insights, plotted, InsightTab.RELEVANCE, palette)
                if (ai && notEvaluated > 0) Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                    Row(
                        Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        Text(
                            when {
                                relevance.isEvaluating -> "Evaluating… ${relevance.evaluated}/${relevance.total} done"
                                relevance.profile.isEmpty -> "Open Research target to describe what you are looking for and place papers on this map."
                                else -> "$notEvaluated of ${visible.size} papers not evaluated for this target yet."
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        if (!relevance.isEvaluating && !relevance.profile.isEmpty) TextButton(onClick = onEvaluate, modifier = Modifier.interactivePointer()) {
                            Text("Evaluate $notEvaluated")
                        }
                    }
                }
                if (byYear && evaluated.isNotEmpty() && plotted.isEmpty()) Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No publication years are available for these papers.")
                } else if (plotted.isEmpty()) Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Psychology, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                        Text("No paper evaluated for this target yet", style = MaterialTheme.typography.titleSmall)
                        Text("Open Research target, then evaluate. Each paper gets a 0–100 % match.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else InsightScatter(
                    points = plotted.map { paper ->
                        ScatterPoint(paper, (insights.relevanceValue(paper) ?: 0f).toDouble(),
                            palette.colorOf(paper, insights, InsightTab.RELEVANCE, colorMode), 5f)
                    },
                    xMin = 0.0,
                    xMax = maxX * 1.04,
                    xTicks = ticks,
                    xSplit = insights.splitSimilarity.toDouble(),
                    xSplitCaption = if (ai) "≥ ${(insights.splitSimilarity * 100).roundToInt()}% match"
                    else "≥ ${(insights.splitSimilarity * 100).roundToInt()}% similar",
                    ySplitCitations = insights.splitCitations,
                    yMaxCitations = interval.maxCitations ?: insights.papers.maxOf { it.citations },
                    yMinCitations = interval.minCitations ?: 0,
                    yAxis = yearAxis,
                    corners = if (byYear) emptyList() else RelevanceQuadrant.entries.map { quadrant ->
                        ScatterCorner(
                            "${quadrant.label} · ${plotted.count { insights.relevanceOf(it) == quadrant }}",
                            palette.relevance.getValue(quadrant),
                            right = quadrant == RelevanceQuadrant.MUST_READ || quadrant == RelevanceQuadrant.HIDDEN_GEMS,
                            top = quadrant == RelevanceQuadrant.MUST_READ || quadrant == RelevanceQuadrant.POPULAR
                        )
                    },
                    marker = null,
                    tooltip = {
                        "${percent(it)}% ${if (ai) "match" else "similar"} · ${it.citations} citations${it.year?.let { y -> " · $y" } ?: ""}"
                    },
                    description = (if (ai) "AI match with your research target" else "Text similarity") +
                        " versus ${if (byYear) "publication year" else "citations"} of ${plotted.size} papers.",
                    selectedId = selectedId,
                    onSelect = onSelect,
                    modifier = Modifier.weight(1f).fillMaxWidth()
                )
                Text(
                    (if (ai) "Right = matches your research target (GPT-6 Luna, title, keywords and abstract). " +
                        "Read Must read first, then Hidden gems."
                    else "Right = closer to $closeTo (title, keywords and abstract). " +
                        "Start with Must read, then explore Hidden gems. Local text similarity is a reading aid, not an AI verdict."
                    ).let { text ->
                        if (!byYear) text
                        else text + " Up = more recent. Colors still show the citation-based quadrants." +
                            (plotted.size.let { evaluated.size - it }.takeIf { it > 0 }?.let { " $it papers without a year are not shown." } ?: "")
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        InsightTab.RISING -> {
            val visibleIds = remember(visible) { visible.mapTo(HashSet()) { it.id } }
            RisingList(insights.rising.filter { it.id in visibleIds }, palette, selectedId, onSelect, modifier)
        }
        InsightTab.TIMELINE -> InsightTimeline(insights, visible, node?.let(::insightYear), palette, showKeptOverlay, modifier, node == null)
        InsightTab.PEOPLE -> BoxWithConstraints(modifier) {
            if (maxWidth < 600.dp) Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                RankedBars("Most frequent authors", Icons.Default.Person, insights.topAuthors, palette, Modifier.weight(1f).fillMaxWidth())
                RankedBars("Most frequent venues", Icons.Default.AccountBalance, insights.topVenues, palette, Modifier.weight(1f).fillMaxWidth())
            } else Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                RankedBars("Most frequent authors", Icons.Default.Person, insights.topAuthors, palette, Modifier.weight(1f).fillMaxHeight())
                RankedBars("Most frequent venues", Icons.Default.AccountBalance, insights.topVenues, palette, Modifier.weight(1f).fillMaxHeight())
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InsightLegend(mode: InsightColorMode, insights: GuidedInsights, visible: List<InsightPaper>, tab: InsightTab, palette: InsightPalette) {
    val entries: List<Triple<String, Color, Int>> = when (mode) {
        InsightColorMode.QUADRANT -> if (tab == InsightTab.RELEVANCE) RelevanceQuadrant.entries.map { q ->
            Triple(q.label, palette.relevance.getValue(q), visible.count { insights.relevanceOf(it) == q })
        } else ImpactQuadrant.entries.map { q ->
            Triple(q.label, palette.impact.getValue(q), visible.count { insights.impactOf(it) == q })
        }
        InsightColorMode.STATUS -> InsightStatus.entries.map { s -> Triple(s.label, palette.status.getValue(s), visible.count { it.status == s }) }
            .filter { it.third > 0 }
        InsightColorMode.RELATION -> listOf(
            Triple("References", palette.relation.getValue(GuidedRelation.REFERENCE), visible.count { it.candidate.relation == GuidedRelation.REFERENCE }),
            Triple("Citations", palette.relation.getValue(GuidedRelation.CITATION), visible.count { it.candidate.relation == GuidedRelation.CITATION })
        )
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        entries.forEach { (label, color, count) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(color))
                Spacer(Modifier.width(6.dp))
                Text("$label · $count", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

private class ScatterPoint(val paper: InsightPaper, val x: Double, val color: Color, val radiusDp: Float)

private class ScatterCorner(val label: String, val color: Color, val right: Boolean, val top: Boolean)

/** Linear Y axis replacing the default log-citations axis. */
private class ScatterYAxis(
    val min: Double,
    val max: Double,
    val value: (InsightPaper) -> Double,
    val ticks: List<Pair<Double, String>>,
    val split: Double?,
    val splitCaption: String?,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InsightChartViewport(
    modifier: Modifier,
    minimumChartWidth: Dp = 720.dp,
    content: @Composable (Modifier, Boolean) -> Unit,
) {
    BoxWithConstraints(modifier) {
        val narrow = maxWidth < 600.dp
        var fitChart by remember { mutableStateOf(false) }
        val expanded = narrow && !fitChart
        val chartWidth = if (expanded) maxOf(maxWidth, minimumChartWidth) else maxWidth
        val scroll = rememberScrollState()
        val scope = rememberCoroutineScope()
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Card(
                Modifier.weight(1f).fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            ) {
                Box(
                    Modifier.fillMaxSize().horizontalScroll(scroll, enabled = expanded)
                        .testTag("insight-chart-viewport")
                ) {
                    content(Modifier.width(chartWidth).fillMaxHeight(), narrow)
                }
            }
            if (narrow) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        if (expanded) "Swipe chart or drag the bar" else "Showing the whole chart",
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(
                        onClick = { fitChart = !fitChart },
                        modifier = Modifier.interactivePointer(),
                    ) {
                        Text(if (expanded) "Fit chart" else "Expand chart")
                    }
                }
                if (expanded) Slider(
                    value = if (scroll.maxValue > 0) scroll.value.toFloat() / scroll.maxValue else 0f,
                    onValueChange = { fraction ->
                        scope.launch { scroll.scrollTo((fraction * scroll.maxValue).roundToInt()) }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("insight-chart-scroll")
                        .semantics { contentDescription = "Horizontal chart position" },
                    thumb = {
                        Box(
                            Modifier.size(18.dp).clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    },
                    track = {
                        Box(
                            Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        )
                    },
                )
            }
        }
    }
}

/** Generic scatter with a log citations axis, a quadrant split, hover tooltips and click selection. */
@Composable
private fun InsightScatter(
    points: List<ScatterPoint>,
    xMin: Double,
    xMax: Double,
    xTicks: List<Pair<Double, String>>,
    xSplit: Double?,
    xSplitCaption: String?,
    ySplitCitations: Int,
    yMaxCitations: Int,
    corners: List<ScatterCorner>,
    marker: Pair<Double, String>?,
    tooltip: (InsightPaper) -> String,
    description: String,
    selectedId: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier,
    yMinCitations: Int = 0,
    yAxis: ScatterYAxis? = null,
) {
    val measurer = rememberTextMeasurer()
    val colors = MaterialTheme.colorScheme
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = colors.onSurfaceVariant)
    val tooltipStyle = MaterialTheme.typography.labelMedium.copy(color = colors.inverseOnSurface)
    val cornerStyle = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
    val minScale = yAxis?.min ?: insightCitationScale(yMinCitations)
    val maxScale = maxOf(yAxis?.max ?: insightCitationScale(yMaxCitations), minScale + 0.01)
    fun yValue(paper: InsightPaper) = yAxis?.value?.invoke(paper) ?: insightCitationScale(paper.citations)
    var hoverId by remember { mutableStateOf<String?>(null) }
    var plot by remember { mutableStateOf<PlotArea?>(null) }

    fun PlotArea.px(point: ScatterPoint) = Offset(xOf(point.x, xMin, xMax), yOf(yValue(point.paper), maxScale, minScale))
    fun hit(position: Offset, radius: Float): String? = plot?.let { area ->
        points.minByOrNull { val p = area.px(it); hypot(p.x - position.x, p.y - position.y) }
            ?.takeIf { val p = area.px(it); hypot(p.x - position.x, p.y - position.y) <= radius }?.paper?.id
    }

    InsightChartViewport(modifier) { chartModifier, narrow ->
        val pointScale = if (!narrow) 1f else when {
            points.size >= 180 -> 0.5f
            points.size >= 80 -> 0.65f
            else -> 0.8f
        }
        Canvas(
            chartModifier.padding(16.dp).testTag("insight-scatter")
                .semantics { contentDescription = description }
                .pointerInput(points, plot) {
                    detectTapGestures { position -> onSelect(hit(position, 22.dp.toPx())) }
                }
                .pointerInput(points) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val position = event.changes.firstOrNull()?.position ?: continue
                            val radius = 10.dp.toPx()
                            when (event.type) {
                                PointerEventType.Move -> hoverId = hit(position, radius)
                                PointerEventType.Exit -> hoverId = null
                            }
                        }
                    }
                }
        ) {
            if (size.width < 96.dp.toPx() || size.height < 96.dp.toPx()) {
                if (plot != null) plot = null
                return@Canvas
            }
            val area = PlotArea(
                left = 48.dp.toPx(),
                top = 8.dp.toPx(),
                width = (size.width - 56.dp.toPx()).coerceAtLeast(0f),
                height = (size.height - 32.dp.toPx()).coerceAtLeast(0f),
                insetX = 10.dp.toPx(),
                insetTop = 10.dp.toPx(),
                insetBottom = 14.dp.toPx()
            )
            if (plot != area) plot = area

            val splitX = xSplit?.let { area.xOf(it, xMin, xMax).coerceInSafe(area.left, area.right) }
            val splitY = (if (yAxis != null) yAxis.split?.let { area.yOf(it, maxScale, minScale) }
            else area.yOf(
                (insightCitationScale(ySplitCitations) + log10(ySplitCitations.toDouble() + 2.0)) / 2, maxScale, minScale
            ))?.coerceInSafe(area.top, area.bottom)
            if (splitX != null && splitY != null) corners.forEach { corner ->
                val x0 = if (corner.right) splitX else area.left
                val x1 = if (corner.right) area.right else splitX
                val y0 = if (corner.top) area.top else splitY
                val y1 = if (corner.top) splitY else area.bottom
                if (x1 > x0 && y1 > y0) drawRect(corner.color.copy(alpha = 0.07f), Offset(x0, y0), Size(x1 - x0, y1 - y0))
            }

            val yTicks = yAxis?.ticks ?: (0..4).map { index ->
                (10.0.pow(minScale + (maxScale - minScale) * index / 4) - 1).roundToInt()
            }.distinct().map { insightCitationScale(it) to it.toString() }
            yTicks.forEach { (value, text) ->
                val y = area.yOf(value, maxScale, minScale)
                drawLine(colors.outlineVariant, Offset(area.left, y), Offset(area.right, y), 1.dp.toPx())
                val label = measurer.measure(text, labelStyle)
                drawText(label, topLeft = Offset(area.left - 6.dp.toPx() - label.size.width, y - label.size.height / 2f))
            }
            val tickStride = if (size.width < 400.dp.toPx()) 2 else 1
            xTicks.filterIndexed { index, _ -> index % tickStride == 0 }.forEach { (value, text) ->
                val label = measurer.measure(text, labelStyle)
                val x = area.xOf(value, xMin, xMax)
                drawText(label, topLeft = Offset((x - label.size.width / 2f).coerceInSafe(0f, size.width - label.size.width), area.bottom + 6.dp.toPx()))
            }

            marker?.let { (value, text) ->
                val x = area.xOf(value, xMin, xMax)
                if (x in area.left..area.right) {
                    drawLine(colors.primary, Offset(x, area.top), Offset(x, area.bottom), 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())))
                    val label = measurer.measure(text, labelStyle.copy(color = colors.primary))
                    drawText(label, topLeft = Offset((x + 4.dp.toPx()).coerceAtMost(area.right - label.size.width), area.top + 28.dp.toPx()))
                }
            }

            val axisColor = colors.onSurfaceVariant
            drawLine(axisColor.copy(alpha = 0.6f), Offset(area.left, area.bottom), Offset(area.right, area.bottom), 1.dp.toPx())
            drawLine(axisColor.copy(alpha = 0.6f), Offset(area.left, area.top), Offset(area.left, area.bottom), 1.dp.toPx())
            if (splitX != null) {
                drawLine(axisColor, Offset(splitX, area.top), Offset(splitX, area.bottom), 2.dp.toPx())
                xSplitCaption?.let {
                    val caption = measurer.measure(it, labelStyle)
                    drawText(caption, topLeft = Offset((splitX + 4.dp.toPx()).coerceAtMost(area.right - caption.size.width),
                        area.bottom - caption.size.height - 2.dp.toPx()))
                }
            }
            if (splitY != null) {
                drawLine(axisColor, Offset(area.left, splitY), Offset(area.right, splitY), 2.dp.toPx())
                val citationCaption = measurer.measure(
                    if (yAxis != null) yAxis.splitCaption.orEmpty() else "> $ySplitCitations citations", labelStyle)
                drawText(citationCaption, topLeft = Offset(area.left + 4.dp.toPx(), (splitY - citationCaption.size.height - 2.dp.toPx()).coerceAtLeast(area.top)))
            }

            val inset = 8.dp.toPx()
            if (!narrow && size.width >= 520.dp.toPx() && size.height >= 300.dp.toPx()) corners.forEach { corner ->
                val text = measurer.measure(corner.label, cornerStyle.copy(color = corner.color))
                val x = if (corner.right) area.right - inset - text.size.width else area.left + inset
                val y = if (corner.top) area.top + inset else area.bottom - inset - text.size.height * 2
                drawText(text, topLeft = Offset(x, y))
            }

            points.forEach { point ->
                if (point.paper.id == selectedId || point.paper.id == hoverId) return@forEach
                val center = area.px(point)
                val radius = point.radiusDp.dp.toPx() * pointScale
                val faded = point.paper.status == InsightStatus.SKIPPED
                drawCircle(point.color.copy(alpha = if (faded) 0.25f else 0.6f), radius, center)
                drawCircle(point.color.copy(alpha = if (faded) 0.5f else 1f), radius, center,
                    style = Stroke((if (narrow) 0.75.dp else 1.dp).toPx()))
                if (point.paper.status == InsightStatus.KEPT) {
                    drawCircle(colors.onSurface, radius + (if (narrow) 1.5.dp else 2.5.dp).toPx(), center,
                        style = Stroke((if (narrow) 0.75.dp else 1.5.dp).toPx()))
                }
            }
            listOfNotNull(hoverId, selectedId).distinct().forEach { id ->
                val point = points.firstOrNull { it.paper.id == id } ?: return@forEach
                val center = area.px(point)
                val radius = maxOf(point.radiusDp.dp.toPx() * pointScale * 1.5f, 5.dp.toPx())
                drawCircle(point.color, radius, center)
                drawCircle(colors.surface, radius, center, style = Stroke(2.dp.toPx()))
                if (id == selectedId) drawCircle(colors.onSurface, radius * 1.6f, center, style = Stroke(2.dp.toPx()))
            }

            hoverId?.let { id ->
                val point = points.firstOrNull { it.paper.id == id } ?: return@let
                val title = point.paper.candidate.node.paper.title
                val text = measurer.measure(
                    "${title.take(70)}${if (title.length > 70) "…" else ""}\n${tooltip(point.paper)} · ${point.paper.status.label}",
                    tooltipStyle,
                    constraints = Constraints(maxWidth = (size.width - 16.dp.toPx()).toInt().coerceAtLeast(1)),
                )
                val pad = 8.dp.toPx()
                val center = area.px(point)
                val boxWidth = text.size.width + pad * 2
                val boxHeight = text.size.height + pad * 2
                val bx = (center.x + 12.dp.toPx()).let { if (it + boxWidth > size.width) center.x - 12.dp.toPx() - boxWidth else it }.coerceAtLeast(0f)
                val by = (center.y - boxHeight - 8.dp.toPx()).coerceAtLeast(0f)
                drawRoundRect(colors.inverseSurface, Offset(bx, by), Size(boxWidth, boxHeight), CornerRadius(8.dp.toPx()))
                drawText(text, topLeft = Offset(bx + pad, by + pad))
            }
        }
    }
}

/**
 * Plot frame with an inner data inset so points on the minimum values (e.g. 0 citations, first year)
 * do not collide with the axes and their labels.
 */
private data class PlotArea(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
    val insetX: Float = 0f,
    val insetTop: Float = 0f,
    val insetBottom: Float = 0f
) {
    val right: Float get() = left + width.coerceAtLeast(0f)
    val bottom: Float get() = top + height.coerceAtLeast(0f)
    fun xOf(value: Double, min: Double, max: Double): Float =
        left + insetX + ((value - min) / (max - min).coerceAtLeast(1e-9) * (width - insetX * 2)).toFloat()
    fun yOf(scale: Double, maxScale: Double, minScale: Double = 0.0): Float =
        top + insetTop + ((height - insetTop - insetBottom) * (1 - (scale - minScale) / (maxScale - minScale))).toFloat()
}

@Composable
private fun RisingList(
    papers: List<InsightPaper>,
    palette: InsightPalette,
    selectedId: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier
) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        if (papers.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No dated, cited papers to rank.") }
            return@Card
        }
        val max = papers.maxOf { it.citationsPerYear ?: 0.0 }.coerceAtLeast(1e-9)
        LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            item {
                Text(
                    "Citations per year since publication: recent papers that gain citations quickly rank above old classics.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
            itemsIndexed(papers, key = { _, paper -> paper.id }) { index, paper ->
                val isSelected = paper.id == selectedId
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { onSelect(paper.id) }.interactivePointer()
                ) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${index + 1}", style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(28.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(paper.candidate.node.paper.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
                                    Box(Modifier.fillMaxHeight().fillMaxWidth(((paper.citationsPerYear ?: 0.0) / max).toFloat().coerceIn(0.02f, 1f))
                                        .background(palette.relation.getValue(paper.candidate.relation)))
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "${formatRate(paper.citationsPerYear ?: 0.0)}/yr · ${paper.year} · ${paper.citations} cit.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        StatusDot(paper.status, palette)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusDot(status: InsightStatus, palette: InsightPalette) {
    Surface(shape = RoundedCornerShape(50), color = palette.status.getValue(status).copy(alpha = 0.16f)) {
        Text(status.label, style = MaterialTheme.typography.labelSmall, color = palette.status.getValue(status),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
    }
}

/** Papers per year, references stacked under citations, with kept papers highlighted. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InsightTimeline(
    insights: GuidedInsights,
    visible: List<InsightPaper>,
    paperYear: Int?,
    palette: InsightPalette,
    showKeptOverlay: Boolean,
    modifier: Modifier,
    graphOverview: Boolean = false
) {
    val buckets = remember(visible) {
        visible.filter { it.year != null }.groupBy { it.year!! }.entries.sortedBy { it.key }.map { (year, group) ->
            Triple(year, group.count { it.candidate.relation == GuidedRelation.REFERENCE } to group.count { it.candidate.relation == GuidedRelation.CITATION },
                group.count { it.status == InsightStatus.KEPT })
        }
    }
    val measurer = rememberTextMeasurer()
    val colors = MaterialTheme.colorScheme
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = colors.onSurfaceVariant)
    val tooltipStyle = MaterialTheme.typography.labelMedium.copy(color = colors.inverseOnSurface)
    var hoverIndex by remember { mutableStateOf<Int?>(null) }
    var barArea by remember { mutableStateOf<Pair<Float, Float>?>(null) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            buildList {
                add((if (graphOverview) "Articles" else "References") to palette.relation.getValue(GuidedRelation.REFERENCE))
                if (!graphOverview) add("Citations" to palette.relation.getValue(GuidedRelation.CITATION))
                if (showKeptOverlay) add("Kept" to palette.status.getValue(InsightStatus.KEPT))
            }.forEach { (label, color) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(color))
                    Spacer(Modifier.width(6.dp))
                    Text(label, style = MaterialTheme.typography.labelMedium)
                }
            }
            val peak = buckets.maxByOrNull { it.second.first + it.second.second }
            peak?.let { Text("Peak: ${it.first} (${it.second.first + it.second.second} papers)", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant) }
        }
        if (buckets.isEmpty()) {
            InsightMessage("No publication years are available.")
            return
        }
        InsightChartViewport(
            Modifier.weight(1f).fillMaxWidth(),
            minimumChartWidth = maxOf(720.dp, ((buckets.last().first - buckets.first().first + 1) * 24).dp),
        ) { chartModifier, _ ->
            val first = buckets.first().first
            val last = buckets.last().first
            val byYear = buckets.associateBy { it.first }
            val maxCount = buckets.maxOf { it.second.first + it.second.second }.coerceAtLeast(1)
            Canvas(
                chartModifier.padding(16.dp).testTag("insight-timeline")
                    .semantics { contentDescription = "Number of ${if (graphOverview) "articles" else "references and citations"} per publication year, $first to $last." }
                    .pointerInput(buckets) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                val position = event.changes.firstOrNull()?.position ?: continue
                                val (left, slot) = barArea ?: continue
                                hoverIndex = when (event.type) {
                                    PointerEventType.Exit -> null
                                    else -> ((position.x - left) / slot).toInt().takeIf { it in 0..(last - first) }
                                }
                            }
                        }
                    }
            ) {
                if (size.width < 64.dp.toPx() || size.height < 64.dp.toPx()) {
                    if (barArea != null) barArea = null
                    return@Canvas
                }
                val left = 36.dp.toPx()
                val bottom = size.height - 22.dp.toPx()
                val top = 8.dp.toPx()
                val years = last - first + 1
                val slot = (size.width - left) / years
                if (barArea != left to slot) barArea = left to slot
                val height = bottom - top
                fun yOf(count: Int) = bottom - height * count / maxCount
                val step = listOf(1, 2, 5, 10, 20, 50, 100, 200, 500, 1000, 2000, 5000).firstOrNull { maxCount / it <= 5 } ?: (maxCount / 5)
                for (value in 0..maxCount step step) {
                    val y = yOf(value)
                    drawLine(colors.outlineVariant, Offset(left, y), Offset(size.width, y), 1.dp.toPx())
                    val label = measurer.measure(value.toString(), labelStyle)
                    drawText(label, topLeft = Offset(left - 6.dp.toPx() - label.size.width, y - label.size.height / 2f))
                }
                val barWidth = (slot * 0.72f).coerceAtLeast(1f)
                for (year in first..last) {
                    val bucket = byYear[year] ?: continue
                    val (references, citations) = bucket.second
                    val x = left + (year - first) * slot + (slot - barWidth) / 2
                    val refTop = yOf(references)
                    drawRect(palette.relation.getValue(GuidedRelation.REFERENCE), Offset(x, refTop), Size(barWidth, bottom - refTop))
                    val citeTop = yOf(references + citations)
                    drawRect(palette.relation.getValue(GuidedRelation.CITATION), Offset(x, citeTop), Size(barWidth, refTop - citeTop))
                    if (showKeptOverlay && bucket.third > 0) {
                        val keptTop = yOf(bucket.third)
                        drawRect(palette.status.getValue(InsightStatus.KEPT), Offset(x + barWidth * 0.3f, keptTop), Size(barWidth * 0.4f, bottom - keptTop))
                    }
                }
                val maxLabels = (size.width / 64.dp.toPx()).toInt().coerceAtLeast(1)
                val labelEvery = ((years + maxLabels - 1) / maxLabels).coerceAtLeast(1)
                for (year in first..last step labelEvery) {
                    val label = measurer.measure(year.toString(), labelStyle)
                    val x = left + (year - first + 0.5f) * slot
                    drawText(label, topLeft = Offset((x - label.size.width / 2f).coerceInSafe(0f, size.width - label.size.width), bottom + 4.dp.toPx()))
                }
                paperYear?.takeIf { it in first..last }?.let { year ->
                    val x = left + (year - first + 0.5f) * slot
                    drawLine(colors.primary, Offset(x, top), Offset(x, bottom), 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())))
                    val label = measurer.measure("This paper", labelStyle.copy(color = colors.primary))
                    drawText(label, topLeft = Offset((x + 4.dp.toPx()).coerceAtMost(size.width - label.size.width), top))
                }
                hoverIndex?.let { index ->
                    val year = first + index
                    val bucket = byYear[year] ?: return@let
                    val text = measurer.measure(
                        if (graphOverview) "$year\n${bucket.second.first + bucket.second.second} articles"
                        else "$year\n${bucket.second.first} references · ${bucket.second.second} citations · ${bucket.third} kept",
                        tooltipStyle
                    )
                    val pad = 8.dp.toPx()
                    val x = (left + index * slot + slot).let { if (it + text.size.width + pad * 2 > size.width) left + index * slot - text.size.width - pad * 2 else it }
                        .coerceAtLeast(0f)
                    drawRoundRect(colors.inverseSurface, Offset(x, top), Size(text.size.width + pad * 2, text.size.height + pad * 2), CornerRadius(8.dp.toPx()))
                    drawText(text, topLeft = Offset(x + pad, top + pad))
                }
            }
        }
        if (insights.missingYear > 0) {
            Text("${insights.missingYear} papers without a publication year are not shown.",
                style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
        }
    }
}

@Composable
private fun RankedBars(title: String, icon: ImageVector, items: List<InsightRankedLabel>, palette: InsightPalette, modifier: Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
            if (items.isEmpty()) {
                Text("No author or venue appears more than once.", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            val max = items.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
            items.forEach { item ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row {
                        Text(item.label, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Text(
                            "${item.count} papers${if (item.kept > 0) " · ${item.kept} kept" else ""}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
                        Box(Modifier.fillMaxHeight().fillMaxWidth(item.count.toFloat() / max).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)))
                        if (item.kept > 0) Box(Modifier.fillMaxHeight().fillMaxWidth(item.kept.toFloat() / max).background(palette.status.getValue(InsightStatus.KEPT)))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InsightDetails(
    paper: InsightPaper?,
    insights: GuidedInsights,
    tab: InsightTab,
    palette: InsightPalette,
    review: GuidedReviewState,
    onKeep: (String) -> Unit,
    onSkip: (String) -> Unit,
    onUndo: (String) -> Unit,
    modifier: Modifier,
    graphOverview: Boolean = false,
    onDiscard: ((String) -> Unit)? = null
) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (paper == null) {
                Text("How to read it", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                val hints = when (tab) {
                    InsightTab.IMPACT -> ImpactQuadrant.entries.map { Triple(it.label, it.hint, palette.impact.getValue(it)) }
                    InsightTab.RELEVANCE -> RelevanceQuadrant.entries.map { Triple(it.label,
                        if (graphOverview) it.hint.replace("this paper", "a graph seed") else it.hint,
                        palette.relevance.getValue(it)) }
                    InsightTab.RISING -> listOf(Triple("Citation velocity", "Citations divided by years since publication.", MaterialTheme.colorScheme.primary))
                    InsightTab.TIMELINE -> listOf(Triple("Timeline",
                        if (graphOverview) "Publication years of all kept nodes and seeds across your graph."
                        else "When the references were written and when the field cited this paper.", MaterialTheme.colorScheme.primary))
                    InsightTab.PEOPLE -> listOf(Triple("Recurring names", "Authors and venues that appear several times are often the core of the topic.", MaterialTheme.colorScheme.primary))
                }
                hints.forEach { (label, hint, color) ->
                    Row(verticalAlignment = Alignment.Top) {
                        Box(Modifier.padding(top = 4.dp).size(10.dp).clip(CircleShape).background(color))
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(label, style = MaterialTheme.typography.labelLarge, color = color)
                            Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                if (tab == InsightTab.IMPACT || tab == InsightTab.RELEVANCE || tab == InsightTab.RISING) {
                    Text(if (graphOverview) "Click a paper to see its details or discard it." else "Click a paper to see its details and keep or skip it.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                return@Column
            }
            val p = paper.candidate.node.paper
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                StatusDot(paper.status, palette)
                insights.impactOf(paper)?.let { InsightBadge(it.label, palette.impact.getValue(it)) }
                insights.relevanceOf(paper)?.let { InsightBadge(it.label, palette.relevance.getValue(it)) }
                if (!graphOverview) InsightBadge(if (paper.candidate.relation == GuidedRelation.REFERENCE) "Reference" else "Citation",
                    palette.relation.getValue(paper.candidate.relation))
            }
            Text(p.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (p.authors.isNotEmpty()) {
                Text(p.authors.take(6).joinToString(", ") + if (p.authors.size > 6) " et al." else "",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                listOfNotNull(
                    paper.year?.toString(),
                    p.source,
                    "${paper.citations} citations",
                    paper.citationsPerYear?.let { "${formatRate(it)}/yr" },
                    "${(paper.similarity * 100).roundToInt()}% ${if (graphOverview) "similar to seeds" else "similar"}"
                ).joinToString(" · "),
                style = MaterialTheme.typography.labelMedium
            )
            paper.aiRelevance?.let { score ->
                val percent = (score * 100).roundToInt()
                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                    Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.AutoAwesome, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                            Text("$percent% match with your target", style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                        LinearProgressIndicator(progress = { score }, modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)))
                        paper.aiRationale?.takeIf { it.isNotBlank() }?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                }
            }
            review.batch.firstOrNull { it.paperId == paper.id }?.aiRationale?.let { rationale ->
                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.tertiaryContainer) {
                    Text("AI: $rationale", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.padding(10.dp))
                }
            }
            val isSeed = paper.candidate.node.connectionType == ConnectionType.CENTRAL
            if (graphOverview) {
                if (isSeed) Text("Seed paper · remove it from the seed list to drop it.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                else onDiscard?.let { DiscardButton { it(paper.id) } }
            } else Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                when (paper.status) {
                    InsightStatus.KEPT, InsightStatus.SKIPPED -> if (paper.id in review.keptIds || paper.id in review.skippedIds) {
                        OutlinedButton(onClick = { onUndo(paper.id) }, modifier = Modifier.interactivePointer()) {
                            Icon(Icons.AutoMirrored.Filled.Undo, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Undo")
                        }
                    } else {
                        Text("Kept while reviewing another paper.", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                        if (!isSeed) onDiscard?.let { DiscardButton { it(paper.id) } }
                    }
                    else -> {
                        Button(onClick = { onKeep(paper.id) }, modifier = Modifier.interactivePointer()) {
                            Icon(Icons.Default.Check, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Keep")
                        }
                        OutlinedButton(onClick = { onSkip(paper.id) }, modifier = Modifier.interactivePointer()) {
                            Icon(Icons.Default.Close, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Skip")
                        }
                    }
                }
            }
            p.abstractText?.takeIf { it.isNotBlank() }?.let {
                Text("Abstract", style = MaterialTheme.typography.labelLarge)
                Text(it, style = MaterialTheme.typography.bodySmall)
            } ?: Text("No abstract available.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DiscardButton(onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
        modifier = Modifier.interactivePointer().testTag("insight-discard")
    ) {
        Icon(Icons.Default.RemoveCircleOutline, null, Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text("Discard · not what I'm looking for")
    }
}

@Composable
private fun DiscardedBar(title: String, onUndo: () -> Unit, onClose: () -> Unit) {
    Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.inverseSurface) {
        Row(
            Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.RemoveCircleOutline, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.inverseOnSurface)
            Text(
                "Discarded “${title.ifBlank { "paper" }}” from the graph.",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.inverseOnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onUndo, modifier = Modifier.interactivePointer()) {
                Text("Undo", color = MaterialTheme.colorScheme.inversePrimary)
            }
            IconButton(onClick = onClose, modifier = Modifier.size(32.dp).interactivePointer()) {
                Icon(Icons.Default.Close, "Dismiss", Modifier.size(16.dp), tint = MaterialTheme.colorScheme.inverseOnSurface)
            }
        }
    }
}

@Composable
private fun InsightBadge(text: String, color: Color) {
    Surface(shape = RoundedCornerShape(50), color = color.copy(alpha = 0.16f)) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = color, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
    }
}

private fun formatRate(value: Double): String =
    if (value >= 10) value.roundToInt().toString() else ((value * 10).roundToInt() / 10.0).toString()
