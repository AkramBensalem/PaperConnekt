package com.paperconnekt.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.paperconnekt.app.resources.Res
import com.paperconnekt.app.resources.filled_close
import com.paperconnekt.app.resources.filled_description
import com.paperconnekt.app.resources.dashboard
import com.paperconnekt.app.resources.book
import com.paperconnekt.app.resources.lightbulb
import com.paperconnekt.domain.model.Paper
import com.akram.bensalem.paper.connekt.feature.assistant.AiUsageEvent
import com.akram.bensalem.paper.connekt.feature.assistant.AiUsageService
import com.akram.bensalem.paper.connekt.feature.assistant.AiUsageSummary
import com.akram.bensalem.paper.connekt.feature.assistant.ResearchHistory
import org.jetbrains.compose.resources.painterResource
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.math.round

@Composable
fun DashboardHeader(
    paperCount: Int,
    includeRemote: Boolean,
    onLibrary: () -> Unit,
    onAssistant: () -> Unit,
) {
    WorkspaceHeader(
        title = "Room for your next idea.",
        subtitle = "Welcome back. Pick up a paper, connect a thought, or ask a new question.",
        icon = Res.drawable.dashboard,
        summaries = listOf(
            "$paperCount saved paper${if (paperCount == 1) "" else "s"}",
            if (includeRemote) "OpenAlex discovery enabled" else "Local-first mode",
        ),
        actions = {
            HeaderAction("Open library", Res.drawable.book, onLibrary)
            HeaderAction("Ask Assistant", Res.drawable.lightbulb, onAssistant, primary = false)
        },
    )
}

@Composable
fun DashboardQuickActions(
    compact: Boolean,
    onDiscover: () -> Unit,
    onAssistant: () -> Unit,
    onLibrary: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Quick actions", style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Button(onClick = onDiscover, modifier = Modifier.weight(1f)) {
                Text("Discover papers")
            }
            if (!compact) {
                Button(onClick = onAssistant, modifier = Modifier.weight(1f)) {
                    Text("Open AI assistance")
                }
                Button(onClick = onLibrary, modifier = Modifier.weight(1f)) {
                    Text("Open library")
                }
            }
        }
        if (compact) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(onClick = onAssistant, modifier = Modifier.weight(1f)) {
                    Text("AI assistance")
                }
                Button(onClick = onLibrary, modifier = Modifier.weight(1f)) {
                    Text("Library")
                }
            }
        }
    }
}

@Composable
fun DashboardMetrics(
    paperCount: Int,
    graphCount: Int,
    libraryCount: Int,
    authorCount: Int,
    aiEventCount: Int,
    aiCostUsd: Double,
) {
    val metrics = listOf(
        "Saved papers" to paperCount,
        "Researchers" to authorCount,
        "Connected graphs" to graphCount,
        "Filed entries" to libraryCount,
        "AI operations" to aiEventCount,
        "Estimated AI cost" to formatDashboardUsd(aiCostUsd),
    )
    BoxWithConstraints {
        val columns = when {
            maxWidth < 360.dp -> 1
            maxWidth < 820.dp -> 2
            maxWidth < 1180.dp -> 3
            else -> 6
        }
        val compact = maxWidth < 1120.dp
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Research insights", style = MaterialTheme.typography.titleMedium)
            metrics.chunked(columns).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    row.forEach { (label, value) ->
                        MetricCard(label, value.toString(), Modifier.weight(1f), compact)
                    }
                    repeat(columns - row.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    modifier: Modifier,
    compact: Boolean,
) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = colors.surfaceContainer,
        ),
        border = BorderStroke(1.dp, colors.outlineVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (compact) 14.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                value,
                style = if (compact) {
                    MaterialTheme.typography.titleLarge
                } else {
                    MaterialTheme.typography.headlineSmall
                },
            )
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private enum class DashboardRange(val label: String, val dayCount: Int?) {
    TODAY("Today", 1),
    DAYS_7("7 days", 7),
    DAYS_14("14 days", 14),
    DAYS_30("30 days", 30),
    DAYS_90("90 days", 90),
    ALL("All time", null),
    CUSTOM("Choose day", 1),
}

private enum class DashboardPieMetric(val label: String) {
    TOKENS("Tokens"),
    COST("Cost"),
}

@Composable
fun DashboardAnalytics(
            history: ResearchHistory,
            papers: List<Paper>,
            graphs: List<com.paperconnekt.domain.model.ResearchGraph>,
            historyLoading: Boolean,
            historyError: String?,
        ) {
            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            var selectedRange by rememberSaveable { mutableStateOf(DashboardRange.DAYS_14) }
            var selectedDay by rememberSaveable { mutableStateOf(today.toString()) }
            var showDayDialog by rememberSaveable { mutableStateOf(false) }
            var dayInput by rememberSaveable { mutableStateOf(today.toString()) }
            val customDay = runCatching { LocalDate.parse(selectedDay) }.getOrNull() ?: today
            val chartDays = dashboardDays(selectedRange, today, history, customDay)
            val usage = history.usage
            val visibleUsage = usage.filter { usageDate(it) in chartDays.first()..chartDays.last() }
            val summary = AiUsageSummary(visibleUsage)
            BoxWithConstraints {
                val stackCharts = maxWidth < 1120.dp
                val compactFilters = maxWidth < 620.dp
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("Workspace intelligence", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "A local view of your research activity and AI footprint.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (historyLoading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                }
                if (compactFilters) {
                    var moreExpanded by remember { mutableStateOf(false) }
                    val compactRanges = buildList {
                        addAll(DashboardRange.entries.take(3))
                        if (selectedRange !in this) {
                            removeAt(lastIndex)
                            add(selectedRange)
                        }
                    }
                    val hiddenRanges = DashboardRange.entries.filterNot { it in compactRanges }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        compactRanges.forEach { range ->
                            FilterChip(
                                selected = selectedRange == range,
                                onClick = {
                                    if (range == DashboardRange.CUSTOM) {
                                        dayInput = selectedDay
                                        showDayDialog = true
                                    } else {
                                        selectedRange = range
                                    }
                                },
                                label = {
                                    Text(
                                        if (range == DashboardRange.CUSTOM && selectedRange == range) {
                                            selectedDay
                                        } else {
                                            range.label
                                        },
                                        maxLines = 1,
                                    )
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Box {
                            IconButton(
                                onClick = { moreExpanded = true },
                                enabled = hiddenRanges.isNotEmpty(),
                            ) {
                                Text("...", style = MaterialTheme.typography.titleMedium)
                            }
                            DropdownMenu(
                                expanded = moreExpanded,
                                onDismissRequest = { moreExpanded = false },
                            ) {
                                hiddenRanges.forEach { range ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                if (range == DashboardRange.CUSTOM && selectedRange == range) {
                                                    selectedDay
                                                } else {
                                                    range.label
                                                },
                                            )
                                        },
                                        onClick = {
                                            moreExpanded = false
                                            if (range == DashboardRange.CUSTOM) {
                                                dayInput = selectedDay
                                                showDayDialog = true
                                            } else {
                                                selectedRange = range
                                            }
                                        },
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        DashboardRange.entries.forEach { range ->
                            FilterChip(
                                selected = selectedRange == range,
                                onClick = {
                                    if (range == DashboardRange.CUSTOM) {
                                        dayInput = selectedDay
                                        showDayDialog = true
                                    } else {
                                        selectedRange = range
                                    }
                                },
                                label = {
                                    Text(
                                        if (range == DashboardRange.CUSTOM && selectedRange == range) {
                                            selectedDay
                                        } else {
                                            range.label
                                        },
                                    )
                                },
                            )
                        }
                    }
                }
                if (showDayDialog) {
                    AlertDialog(
                        onDismissRequest = { showDayDialog = false },
                        title = { Text("Choose a day") },
                        text = {
                            OutlinedTextField(
                                value = dayInput,
                                onValueChange = { dayInput = it },
                                label = { Text("Date (YYYY-MM-DD)") },
                                singleLine = true,
                                isError = runCatching { LocalDate.parse(dayInput) }.isFailure,
                            )
                        },
                        confirmButton = {
                            TextButton(
                                enabled = runCatching { LocalDate.parse(dayInput) }.isSuccess,
                                onClick = {
                                    selectedDay = LocalDate.parse(dayInput).toString()
                                    selectedRange = DashboardRange.CUSTOM
                                    showDayDialog = false
                                },
                            ) {
                                Text("Apply")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDayDialog = false }) {
                                Text("Cancel")
                            }
                        },
                    )
                }
                historyError?.let { DashboardStatusCard(it, isError = true) }
                if (stackCharts) {
                    DashboardActivityChart(history.dailyOpens, chartDays)
                    DashboardCostChart(usage, chartDays)
                    DashboardTokenChart(visibleUsage)
                    DashboardServiceChart(summary.byService, visibleUsage)
                    DashboardPieChart(visibleUsage)
                    DashboardCollectionChart(papers, graphs)
                } else {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        DashboardActivityChart(history.dailyOpens, chartDays, Modifier.weight(1f))
                        DashboardCostChart(usage, chartDays, Modifier.weight(1f))
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        DashboardTokenChart(visibleUsage, Modifier.weight(1f))
                        DashboardServiceChart(summary.byService, visibleUsage, Modifier.weight(1f))
                    }
                    DashboardPieChart(visibleUsage)
                    DashboardCollectionChart(papers, graphs)
                }
                DashboardModelTable(visibleUsage)
                }
            }
        }

        @Composable
        private fun DashboardActivityChart(
            dailyOpens: Map<String, Int>,
            days: List<LocalDate>,
            modifier: Modifier = Modifier,
        ) {
            val values = days.map { dailyOpens[it.toString()] ?: 0 }
            val peak = values.maxOrNull()?.coerceAtLeast(1) ?: 1
            val total = values.sum()
            DashboardChartCard(
                "Research rhythm",
                if (total == 0) {
                    "No workspace opens in ${rangeLabel(days)} yet"
                } else {
                    "$total workspace opens in ${rangeLabel(days)}"
                },
                modifier,
            ) {
                DashboardBarPlot(
                    values = values.map(Int::toFloat),
                    maxValue = peak.toFloat(),
                    color = MaterialTheme.colorScheme.primary,
                    description = days.zip(values).joinToString { "${it.first}: ${it.second} opens" },
                    emptyMessage = "No activity in this period",
                )
                ChartRangeLabel(days.first().toString(), days.last().toString())
            }
        }

        @Composable
        private fun DashboardCostChart(
            events: List<AiUsageEvent>,
            days: List<LocalDate>,
            modifier: Modifier = Modifier,
        ) {
            val serviceSeries = AiUsageService.entries.map { service ->
                DashboardLineSeries(
                    label = service.label,
                    color = serviceColor(service),
                    values = days.map { day ->
                        events
                            .filter { usageDate(it) == day && it.service == service }
                            .sumOf { it.estimatedCostUsd }
                    },
                )
            }
            val globalValues = days.map { day ->
                events.filter { usageDate(it) == day }.sumOf { it.estimatedCostUsd }
            }
            val total = globalValues.sum()
            val series = serviceSeries + DashboardLineSeries(
                label = "Global",
                color = MaterialTheme.colorScheme.onSurface,
                values = globalValues,
                dashed = true,
            )
            DashboardChartCard(
                "AI cost trend",
                if (total == 0.0) {
                    "No AI cost recorded in ${rangeLabel(days)} yet"
                } else {
                    "Estimated USD over ${rangeLabel(days)} · ${formatDashboardUsd(total)} total"
                },
                modifier,
            ) {
                DashboardLinePlot(
                    series = series,
                    description = days.zip(globalValues).joinToString { "${it.first}: ${formatDashboardUsd(it.second)}" },
                    emptyMessage = "No cost data in this period",
                )
                if (total > 0.0) DashboardLineLegend(series)
                ChartRangeLabel(days.first().toString(), days.last().toString())
            }
        }

        @Composable
        private fun DashboardTokenChart(events: List<AiUsageEvent>, modifier: Modifier = Modifier) {
            val input = events.sumOf { it.inputUnits.coerceAtLeast(0L) }
            val output = events.sumOf { it.outputUnits.coerceAtLeast(0L) }
            val total = input + output
            DashboardChartCard("Token flow", "${formatCompactNumber(total)} tracked input and output units", modifier) {
                if (total == 0L) {
                    DashboardPlotEmptyState(
                        message = "No token usage recorded",
                        detail = "Token totals will appear after an AI operation.",
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        TokenBar("Input tokens / units", input, total, MaterialTheme.colorScheme.primary)
                        TokenBar("Output tokens / units", output, total, MaterialTheme.colorScheme.secondary)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Input ${formatCompactNumber(input)}", style = MaterialTheme.typography.labelSmall)
                            Text("Output ${formatCompactNumber(output)}", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        @Composable
        private fun DashboardServiceChart(
            values: Map<AiUsageService, Double>,
            events: List<AiUsageEvent>,
            modifier: Modifier = Modifier,
        ) {
            val total = values.values.sum()
            DashboardChartCard("Cost by AI service", "Where estimated spend is going", modifier) {
                if (total == 0.0) {
                    DashboardPlotEmptyState(
                        message = "No service usage recorded",
                        detail = "AI service costs will be grouped here once available.",
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        AiUsageService.entries.forEach { service ->
                            val value = values[service] ?: 0.0
                            val fraction = value / total
                            val serviceEvents = events.filter { it.service == service }
                            val units = serviceEvents.sumOf { it.inputUnits.coerceAtLeast(0L) }
                            val usageLabel = when (service) {
                                AiUsageService.OCR -> "${formatCompactNumber(units)} pages"
                                else -> "${formatCompactNumber(units)} input"
                            }
                            BoxWithConstraints {
                                if (maxWidth < 460.dp) {
                                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(service.label, style = MaterialTheme.typography.labelMedium)
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(formatDashboardUsd(value), style = MaterialTheme.typography.labelSmall)
                                                Text(usageLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                        LinearProgressIndicator(
                                            progress = { fraction.toFloat() },
                                            modifier = Modifier.fillMaxWidth(),
                                            color = serviceColor(service),
                                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                        )
                                    }
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text(
                                            service.label,
                                            Modifier.widthIn(min = 72.dp, max = 132.dp),
                                            style = MaterialTheme.typography.labelMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        LinearProgressIndicator(
                                            progress = { fraction.toFloat() },
                                            modifier = Modifier.weight(1f),
                                            color = serviceColor(service),
                                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                        )
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(formatDashboardUsd(value), style = MaterialTheme.typography.labelSmall)
                                            Text(usageLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        @Composable
        private fun DashboardPieChart(
            events: List<AiUsageEvent>,
            modifier: Modifier = Modifier,
        ) {
            var metric by rememberSaveable { mutableStateOf(DashboardPieMetric.TOKENS) }
            val values = AiUsageService.entries.map { service ->
                service to events
                    .filter { it.service == service }
                    .sumOf {
                        when (metric) {
                            DashboardPieMetric.TOKENS -> (it.inputUnits.coerceAtLeast(0L) + it.outputUnits.coerceAtLeast(0L)).toDouble()
                            DashboardPieMetric.COST -> it.estimatedCostUsd
                        }
                    }
            }.filter { it.second > 0.0 }
            val total = values.sumOf { it.second }
            val coloredValues = values.map { (service, value) ->
                Triple(service, value, serviceColor(service))
            }
            DashboardChartCard(
                "AI usage share",
                if (metric == DashboardPieMetric.TOKENS) {
                    "${formatCompactNumber(total.toLong())} tracked units"
                } else {
                    "${formatDashboardUsd(total)} estimated cost"
                },
                modifier,
            ) {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DashboardPieMetric.entries.forEach { option ->
                        FilterChip(
                            selected = metric == option,
                            onClick = { metric = option },
                            label = { Text(option.label) },
                        )
                    }
                }
                if (values.isEmpty()) {
                    DashboardPlotEmptyState(
                        message = "No AI usage recorded",
                        detail = "Usage share will appear after an AI operation.",
                    )
                } else {
                    BoxWithConstraints {
                        val stackContent = maxWidth < 520.dp
                        if (stackContent) {
                            Column(
                                Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                            ) {
                                DashboardPie(values, coloredValues, total)
                                DashboardPieLegend(coloredValues, total)
                            }
                        } else {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(18.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                DashboardPie(values, coloredValues, total)
                                DashboardPieLegend(coloredValues, total, Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

@Composable
private fun DashboardPie(
    values: List<Pair<AiUsageService, Double>>,
    coloredValues: List<Triple<AiUsageService, Double, androidx.compose.ui.graphics.Color>>,
    total: Double,
) {
    Canvas(Modifier.size(170.dp).semantics {
        contentDescription = values.joinToString { (service, value) ->
            "${service.label}: ${formatDashboardPercent(value / total)}"
        }
    }) {
        var startAngle = -90f
        coloredValues.forEach { (_, value, color) ->
            val sweep = (value / total * 360.0).toFloat()
            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = true,
            )
            startAngle += sweep
        }
    }
}

@Composable
private fun DashboardPieLegend(
    coloredValues: List<Triple<AiUsageService, Double, androidx.compose.ui.graphics.Color>>,
    total: Double,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier,
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        coloredValues.forEach { (service, value, color) ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Canvas(Modifier.size(10.dp)) {
                    drawCircle(color, radius = size.minDimension / 2f)
                }
                Text(
                    service.label,
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    formatDashboardPercent(value / total),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

        @Composable
        private fun DashboardCollectionChart(
            papers: List<Paper>,
            graphs: List<com.paperconnekt.domain.model.ResearchGraph>,
            modifier: Modifier = Modifier,
        ) {
            val years = papers.mapNotNull { it.year }.groupingBy { it }.eachCount().toList().sortedBy { it.first }.takeLast(8)
            val sources = papers.groupingBy { it.source.ifBlank { "Local" } }.eachCount().toList().sortedByDescending { it.second }.take(5)
            DashboardChartCard("Collection shape", "${papers.size} papers · ${graphs.size} connected graphs", modifier) {
                if (papers.isEmpty()) {
                    DashboardPlotEmptyState(
                        message = "Your collection is ready to grow",
                        detail = "Add papers to reveal publication and source trends.",
                    )
                } else {
                    if (years.isNotEmpty()) {
                        Text("Papers by publication year", style = MaterialTheme.typography.labelLarge)
                        if (years.size == 1) {
                            val (year, count) = years.single()
                            Text(
                                "$count paper${if (count == 1) "" else "s"} from $year",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        DashboardBarPlot(
                            values = years.map { it.second.toFloat() },
                            maxValue = years.maxOf { it.second }.toFloat().coerceAtLeast(1f),
                            labels = years.map { it.first.toString() },
                            color = MaterialTheme.colorScheme.primary,
                            description = years.joinToString { "${it.first}: ${it.second} papers" },
                            emptyMessage = "No publication years recorded",
                        )
                    } else {
                        Text(
                            "Add publication years to reveal the year trend.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Text("Top sources", style = MaterialTheme.typography.labelLarge)
                    sources.forEach { (source, count) ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(source, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("$count", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        @Composable
        private fun DashboardModelTable(events: List<AiUsageEvent>) {
            val models = events.groupingBy { it.model.ifBlank { "Unspecified model" } }.eachCount()
            if (models.isEmpty()) return
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("AI model activity", style = MaterialTheme.typography.titleMedium)
                    models.entries.sortedByDescending { it.value }.take(6).forEach { (model, count) ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(model, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("$count operation${if (count == 1) "" else "s"}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        @Composable
        private fun DashboardChartCard(
            title: String,
            subtitle: String,
            modifier: Modifier = Modifier,
            content: @Composable ColumnScope.() -> Unit,
        ) {
            OutlinedCard(
                modifier.fillMaxWidth(),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    content()
                })
            }
        }

        @Composable
        private fun DashboardBarPlot(
            values: List<Float>,
            maxValue: Float,
            color: androidx.compose.ui.graphics.Color,
            description: String,
            labels: List<String> = emptyList(),
            emptyMessage: String = "No data in this period",
        ) {
            if (values.none { it > 0f }) {
                DashboardPlotEmptyState(emptyMessage)
            } else {
                Canvas(Modifier.fillMaxWidth().height(150.dp).semantics { contentDescription = description }) {
                    val slot = size.width / values.size.coerceAtLeast(1)
                    val barWidth = (slot * 0.58f)
                        .coerceAtMost(if (values.size == 1) 72.dp.toPx() else Float.MAX_VALUE)
                        .coerceAtLeast(4.dp.toPx())
                    values.forEachIndexed { index, value ->
                        val height = (size.height * (value / maxValue.coerceAtLeast(1f))).coerceAtLeast(4.dp.toPx())
                        drawRoundRect(
                            color = if (value > 0f) color else color.copy(alpha = 0.18f),
                            topLeft = Offset(index * slot + (slot - barWidth) / 2f, size.height - height),
                            size = Size(barWidth, height),
                            cornerRadius = CornerRadius(5.dp.toPx()),
                        )
                    }
                }
            }
            if (labels.isNotEmpty()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(labels.first(), style = MaterialTheme.typography.labelSmall)
                    Text(labels.last(), style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        @Composable
        private fun DashboardLinePlot(
            series: List<DashboardLineSeries>,
            description: String,
            emptyMessage: String = "No data in this period",
        ) {
            val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
            val hasValues = series.any { line -> line.values.any { it > 0.0 } }
            if (!hasValues) {
                DashboardPlotEmptyState(emptyMessage)
            } else {
                Canvas(Modifier.fillMaxWidth().height(150.dp).semantics { contentDescription = description }) {
                    val peak = series.flatMap { it.values }.maxOrNull()?.coerceAtLeast(0.0000001) ?: 0.0000001
                    val pointCount = series.maxOfOrNull { it.values.size } ?: 0
                    val step = size.width / (pointCount - 1).coerceAtLeast(1)
                    repeat(4) { index ->
                        val y = size.height * index / 3f
                        drawLine(gridColor, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
                    }
                    series.forEach { line ->
                        val path = Path()
                        line.values.forEachIndexed { index, value ->
                            val point = Offset(index * step, size.height - (value / peak).toFloat() * size.height)
                            if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
                        }
                        drawPath(
                            path,
                            line.color,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = if (line.dashed) 2.dp.toPx() else 3.dp.toPx(),
                                pathEffect = if (line.dashed) {
                                    PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 7.dp.toPx()))
                                } else {
                                    null
                                },
                            )
                        )
                        if (!line.dashed) {
                            line.values.forEachIndexed { index, value ->
                                drawCircle(
                                    line.color,
                                    radius = 3.dp.toPx(),
                                    center = Offset(index * step, size.height - (value / peak).toFloat() * size.height),
                                )
                            }
                        }
                    }
                }
            }
        }

        @Composable
        private fun DashboardPlotEmptyState(
            message: String,
            detail: String? = null,
        ) {
            val dashColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            Column(
                Modifier.fillMaxWidth().height(150.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Canvas(Modifier.fillMaxWidth().height(28.dp)) {
                    val dashWidth = 12.dp.toPx()
                    val gap = 9.dp.toPx()
                    var x = 0f
                    while (x < size.width) {
                        drawRoundRect(
                            color = dashColor,
                            topLeft = Offset(x, size.height - 5.dp.toPx()),
                            size = Size(dashWidth, 5.dp.toPx()),
                            cornerRadius = CornerRadius(3.dp.toPx()),
                        )
                        x += dashWidth + gap
                    }
                }
                Text(message, style = MaterialTheme.typography.labelLarge)
                detail?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        @Composable
        private fun DashboardLineLegend(series: List<DashboardLineSeries>) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                series.chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        row.forEach { line ->
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Canvas(Modifier.width(20.dp).height(8.dp)) {
                                    drawLine(
                                        color = line.color,
                                        start = Offset(0f, size.height / 2f),
                                        end = Offset(size.width, size.height / 2f),
                                        strokeWidth = 2.dp.toPx(),
                                        pathEffect = if (line.dashed) {
                                            PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))
                                        } else {
                                            null
                                        },
                                    )
                                }
                                Text(line.label, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        private data class DashboardLineSeries(
            val label: String,
            val color: androidx.compose.ui.graphics.Color,
            val values: List<Double>,
            val dashed: Boolean = false,
        )

        @Composable
        private fun TokenBar(label: String, value: Long, total: Long, color: androidx.compose.ui.graphics.Color) {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(label, style = MaterialTheme.typography.labelMedium)
                    Text(formatCompactNumber(value), style = MaterialTheme.typography.labelMedium)
                }
                LinearProgressIndicator(
                    progress = { if (total > 0L) value.toFloat() / total else 0f },
                    modifier = Modifier.fillMaxWidth(),
                    color = color,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
        }

        @Composable
        private fun ChartRangeLabel(start: String, end: String) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(start, style = MaterialTheme.typography.labelSmall)
                Text(end, style = MaterialTheme.typography.labelSmall)
            }
        }

private fun usageDate(event: AiUsageEvent): LocalDate =
    Instant.fromEpochMilliseconds(event.timestamp).toLocalDateTime(TimeZone.currentSystemDefault()).date

@Composable
private fun serviceColor(service: AiUsageService) = when (service) {
    AiUsageService.OCR -> MaterialTheme.colorScheme.primary
    AiUsageService.CHAT -> MaterialTheme.colorScheme.tertiary
    AiUsageService.LITERATURE_TRIAGE -> MaterialTheme.colorScheme.secondary
    AiUsageService.RELEVANCE_SCORING -> MaterialTheme.colorScheme.error
    AiUsageService.EMBEDDINGS -> MaterialTheme.colorScheme.inversePrimary
}

private fun dashboardDays(
    range: DashboardRange,
    today: LocalDate,
    history: ResearchHistory,
    customDay: LocalDate,
): List<LocalDate> {
    if (range == DashboardRange.CUSTOM) return listOf(customDay)
    val count = range.dayCount
    if (count == null) {
        val historyDates = history.usage.map(::usageDate) +
            history.dailyOpens.keys.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }
        val earliest = historyDates.minOrNull() ?: today
        val cutoff = today - DatePeriod(days = 89)
        val start = if (earliest < cutoff) cutoff else earliest
        val days = mutableListOf<LocalDate>()
        var cursor = start
        while (cursor <= today) {
            days += cursor
            cursor = cursor + DatePeriod(days = 1)
        }
        return days
    }
    return (0 until count).map { offset -> today - DatePeriod(days = count - offset - 1) }
}

private fun rangeLabel(days: List<LocalDate>): String =
    if (days.size == 1) "on ${days.first()}"
    else if (days.size <= 14) "the last ${days.size} days"
    else "${days.first()} – ${days.last()}"

private fun formatCompactNumber(value: Long): String = when {
    value >= 1_000_000L -> "${round(value / 100_000.0) / 10.0}M"
    value >= 1_000L -> "${round(value / 100.0) / 10.0}K"
    else -> value.toString()
}

private fun formatDashboardUsd(value: Double): String {
    if (value > 0.0 && value < 0.0001) return "<\$0.0001"
    val scaled = round(value.coerceAtLeast(0.0) * 10_000.0).toLong()
    return "\$${scaled / 10_000}.${(scaled % 10_000).toString().padStart(4, '0')}"
}

private fun formatDashboardPercent(value: Double): String =
    "${round(value.coerceIn(0.0, 1.0) * 1000.0) / 10.0}%"

@Composable
fun DashboardPaperCard(
    modifier: Modifier = Modifier,
    paper: Paper,
    isSaved: Boolean,
    compact: Boolean,
    onOpen: () -> Unit,
    onOpenGraph: () -> Unit,
    onSave: () -> Unit,
    onRemove: () -> Unit,
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(
                        paper.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (paper.authors.isNotEmpty()) {
                        Text(
                            paper.authors.joinToString(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = if (compact) 2 else 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    paper.source,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                paper.year?.let { Text(it.toString(), style = MaterialTheme.typography.labelMedium) }
                paper.doi?.let { Text("DOI $it", style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
            if (paper.tags.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    paper.tags.take(3).forEach { tag ->
                        FilterChip(selected = false, onClick = {}, label = { Text(tag) })
                    }
                }
            }
            paper.abstractText?.takeIf { it.isNotBlank() }?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onOpenGraph) {
                    Text("Open graph")
                }
                if (paper.pdfPath != null || paper.sourceUrl != null) {
                    TextButton(onClick = onOpen) {
                        Text(if (paper.pdfPath != null) "Open paper" else "Open source")
                    }
                }
                if (isSaved) {
                    TextButton(onClick = onRemove) { Text("Remove") }
                } else {
                    TextButton(onClick = onSave) { Text("Save locally") }
                }
            }
        }
    }
}

@Composable
fun DashboardStatusCard(
    message: String,
    showProgress: Boolean = false,
    isError: Boolean = false,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isError) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showProgress) {
                CircularProgressIndicator(modifier = Modifier.width(20.dp).height(20.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(12.dp))
            }
            Text(message)
        }
    }
}

@Composable
fun DashboardEmptyState(
    hasQuery: Boolean,
    onAddPaper: () -> Unit,
    onDiscover: () -> Unit,
) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                if (hasQuery) "No matching papers" else "Your research collection is waiting",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                if (hasQuery) {
                    "Try a different title, author, tag, or DOI, or search optional online discovery."
                } else {
                    "Add a paper manually or discover a topic to start building your local-first workspace."
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onAddPaper) { Text("Add paper") }
                TextButton(onClick = onDiscover) { Text("Discover online") }
            }
        }
    }
}

@Composable
fun PaperMetadataDialog(
    title: String,
    confirmLabel: String,
    titleValue: String,
    onTitleChange: (String) -> Unit,
    authorsValue: String,
    onAuthorsChange: (String) -> Unit,
    yearValue: String,
    onYearChange: (String) -> Unit,
    doiValue: String,
    onDoiChange: (String) -> Unit,
    tagsValue: String,
    onTagsChange: (String) -> Unit,
    abstractValue: String,
    onAbstractChange: (String) -> Unit,
    pdfPathValue: String,
    onPdfPathChange: (String) -> Unit,
    onDismiss: () -> Unit,
    enabled: Boolean,
    onConfirm: () -> Unit,
    error: String? = null,
    saving: Boolean = false,
) {
    var attachingPdf by remember { mutableStateOf(false) }
    val titleFocus = remember { FocusRequester() }
    val busy = saving || attachingPdf
    val invalidYear = yearValue.isNotBlank() &&
        (yearValue.toIntOrNull()?.let { it in 1..9999 } != true)
    LaunchedEffect(Unit) { titleFocus.requestFocus() }

    Dialog(
        onDismissRequest = { if (!busy) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BoxWithConstraints(
            Modifier.fillMaxSize().safeDrawingPadding().imePadding(),
            contentAlignment = Alignment.Center,
        ) {
            val compact = maxWidth < 760.dp
            val inset = if (compact) 20.dp else 28.dp
            Surface(
                modifier = Modifier
                    .padding(if (compact) 8.dp else 24.dp)
                    .widthIn(max = 1000.dp)
                    .fillMaxWidth()
                    .fillMaxHeight(if (compact) 0.98f else 0.92f),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                tonalElevation = 0.dp,
            ) {
                Column {
                    Row(
                        Modifier.fillMaxWidth().padding(inset),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                        ) {
                            Icon(
                                painterResource(Res.drawable.filled_description),
                                contentDescription = null,
                                modifier = Modifier.padding(14.dp).size(24.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(title, style = MaterialTheme.typography.titleLarge)
                            Text(
                                "Build your collection, one paper at a time.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = onDismiss, enabled = !busy) {
                            Icon(painterResource(Res.drawable.filled_close), contentDescription = "Close add paper")
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Column(
                        Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(inset),
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                    ) {
                        val metadata: @Composable () -> Unit = {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                PaperFormSection("Paper details", "Only a title is required. You can edit everything later.")
                                PaperFormField(
                                    value = titleValue,
                                    onValueChange = onTitleChange,
                                    label = "Paper title *",
                                    placeholder = "Enter the full title",
                                    modifier = Modifier.focusRequester(titleFocus),
                                    singleLine = false,
                                    enabled = !saving,
                                )
                                PaperFormField(
                                    value = authorsValue,
                                    onValueChange = onAuthorsChange,
                                    label = "Authors",
                                    placeholder = "e.g. Ada Lovelace, Alan Turing",
                                    enabled = !saving,
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    PaperFormField(
                                        value = yearValue,
                                        onValueChange = onYearChange,
                                        label = "Year",
                                        placeholder = "2026",
                                        modifier = Modifier.weight(0.35f),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        isError = invalidYear,
                                        enabled = !saving,
                                    )
                                    PaperFormField(
                                        value = doiValue,
                                        onValueChange = onDoiChange,
                                        label = "DOI",
                                        placeholder = "10.1234/example",
                                        modifier = Modifier.weight(0.65f),
                                        enabled = !saving,
                                    )
                                }
                                if (invalidYear) {
                                    Text(
                                        "Enter a year between 1 and 9999, or leave it empty.",
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                                PaperFormField(
                                    value = abstractValue,
                                    onValueChange = onAbstractChange,
                                    label = "Abstract",
                                    placeholder = "What is this paper about?",
                                    singleLine = false,
                                    minLines = 4,
                                    enabled = !saving,
                                )
                                PaperFormField(
                                    value = tagsValue,
                                    onValueChange = onTagsChange,
                                    label = "Tags",
                                    placeholder = "e.g. machine learning, literature review",
                                    enabled = !saving,
                                )
                                Text(
                                    "Separate authors and tags with commas.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        val attachment: @Composable () -> Unit = {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                PaperFormSection("Reading copy", "Optional PDF attachment")
                                PdfAttachmentCard(
                                    pdfPath = pdfPathValue.ifBlank { null },
                                    onAttached = { onPdfPathChange(it) },
                                    onBusyChanged = { attachingPdf = it },
                                    enabled = !saving,
                                )
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                                ) {
                                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("Your research stays local", style = MaterialTheme.typography.labelLarge)
                                        Text(
                                            "A separate PDF copy is kept in app-managed storage. Your original file is never changed.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                        if (compact) {
                            metadata()
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            attachment()
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                                Column(Modifier.weight(1.5f)) { metadata() }
                                Column(Modifier.weight(1f)) { attachment() }
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    error?.let {
                        Text(
                            it,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = inset, vertical = 8.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = inset, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (!compact) {
                            Text(
                                "Local Library / No account required",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            Spacer(Modifier.weight(1f))
                        }
                        TextButton(onClick = onDismiss, enabled = !busy) { Text("Cancel") }
                        Button(
                            enabled = enabled && !busy && !invalidYear,
                            onClick = onConfirm,
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                        ) {
                            if (saving) {
                                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(confirmLabel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PaperFormSection(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PaperFormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    isError: Boolean = false,
    enabled: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        modifier = modifier.fillMaxWidth(),
        singleLine = singleLine,
        minLines = minLines,
        maxLines = if (singleLine) 1 else 6,
        keyboardOptions = keyboardOptions,
        isError = isError,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        ),
    )
}
