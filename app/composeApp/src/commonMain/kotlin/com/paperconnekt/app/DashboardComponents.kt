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
import org.jetbrains.compose.resources.stringResource
import com.paperconnekt.domain.model.Paper
import com.akram.bensalem.paper.connekt.feature.assistant.AiUsageEvent
import com.akram.bensalem.paper.connekt.feature.assistant.AiUsageService
import com.akram.bensalem.paper.connekt.feature.assistant.AiUsageSummary
import com.akram.bensalem.paper.connekt.feature.assistant.ResearchHistory
import org.jetbrains.compose.resources.painterResource
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.math.round
import com.paperconnekt.app.resources.add_paper
import com.paperconnekt.app.resources.ai_assistance
import com.paperconnekt.app.resources.ai_model_activity
import com.paperconnekt.app.resources.authors_example
import com.paperconnekt.app.resources.ai_cost_trend
import com.paperconnekt.app.resources.ai_operations_metric
import com.paperconnekt.app.resources.ai_usage_share
import com.paperconnekt.app.resources.ai_cost_in_range
import com.paperconnekt.app.resources.no_ai_cost_in_range
import com.paperconnekt.app.resources.add_papers_for_trends
import com.paperconnekt.app.resources.add_years_for_trend
import com.paperconnekt.app.resources.all_time
import com.paperconnekt.app.resources.cancel
import com.paperconnekt.app.resources.choose_a_day
import com.paperconnekt.app.resources.choose_day
import com.paperconnekt.app.resources.close_add_paper
import com.paperconnekt.app.resources.collection_ready
import com.paperconnekt.app.resources.collection_shape
import com.paperconnekt.app.resources.comma_separated_authors_tags
import com.paperconnekt.app.resources.connected_graphs_metric
import com.paperconnekt.app.resources.cost
import com.paperconnekt.app.resources.cost_by_service
import com.paperconnekt.app.resources.date_format_hint
import com.paperconnekt.app.resources.discover_online
import com.paperconnekt.app.resources.discover_papers
import com.paperconnekt.app.resources.doi_value
import com.paperconnekt.app.resources.doi
import com.paperconnekt.app.resources.doi_placeholder
import com.paperconnekt.app.resources.dashboard_description
import com.paperconnekt.app.resources.dashboard_empty_hint
import com.paperconnekt.app.resources.dashboard_search_empty_hint
import com.paperconnekt.app.resources.days_14
import com.paperconnekt.app.resources.days_30
import com.paperconnekt.app.resources.days_7
import com.paperconnekt.app.resources.days_90
import com.paperconnekt.app.resources.estimated_ai_cost_metric
import com.paperconnekt.app.resources.estimated_spend
import com.paperconnekt.app.resources.filed_entries_metric
import com.paperconnekt.app.resources.enter_full_title
import com.paperconnekt.app.resources.global_scope
import com.paperconnekt.app.resources.input_tokens
import com.paperconnekt.app.resources.input_tokens_units
import com.paperconnekt.app.resources.library
import com.paperconnekt.app.resources.loading_placeholder
import com.paperconnekt.app.resources.local_first_workspace
import com.paperconnekt.app.resources.local_first_mode
import com.paperconnekt.app.resources.managed_pdf_copy
import com.paperconnekt.app.resources.no_activity_period
import com.paperconnekt.app.resources.no_ai_usage
import com.paperconnekt.app.resources.no_cost_period
import com.paperconnekt.app.resources.no_data_period
import com.paperconnekt.app.resources.no_matching_papers
import com.paperconnekt.app.resources.no_publication_years_recorded
import com.paperconnekt.app.resources.no_service_usage
import com.paperconnekt.app.resources.no_token_usage
import com.paperconnekt.app.resources.nav_assistant
import com.paperconnekt.app.resources.open_ai_assistance
import com.paperconnekt.app.resources.openalex_discovery_enabled
import com.paperconnekt.app.resources.open_graph_action
import com.paperconnekt.app.resources.open_library
import com.paperconnekt.app.resources.open_paper
import com.paperconnekt.app.resources.open_source
import com.paperconnekt.app.resources.optional_pdf_attachment
import com.paperconnekt.app.resources.operations_count
import com.paperconnekt.app.resources.output_tokens
import com.paperconnekt.app.resources.output_tokens_units
import com.paperconnekt.app.resources.paper_abstract_placeholder
import com.paperconnekt.app.resources.paper_details_section
import com.paperconnekt.app.resources.paper_title_label
import com.paperconnekt.app.resources.papers_by_publication_year
import com.paperconnekt.app.resources.papers_connected_graphs
import com.paperconnekt.app.resources.papers_from_year
import com.paperconnekt.app.resources.quick_actions
import com.paperconnekt.app.resources.reading_copy_section
import com.paperconnekt.app.resources.remove
import com.paperconnekt.app.resources.research_collection_waiting
import com.paperconnekt.app.resources.research_rhythm_title
import com.paperconnekt.app.resources.research_insights
import com.paperconnekt.app.resources.researchers_metric
import com.paperconnekt.app.resources.range_dates
import com.paperconnekt.app.resources.range_last_days
import com.paperconnekt.app.resources.range_on_day
import com.paperconnekt.app.resources.room_for_next_idea
import com.paperconnekt.app.resources.save
import com.paperconnekt.app.resources.save_locally
import com.paperconnekt.app.resources.saved_papers_count
import com.paperconnekt.app.resources.saved_papers_metric
import com.paperconnekt.app.resources.tags_example
import com.paperconnekt.app.resources.tags
import com.paperconnekt.app.resources.title_required_edit_later
import com.paperconnekt.app.resources.today_label
import com.paperconnekt.app.resources.token_flow
import com.paperconnekt.app.resources.tracked_units
import com.paperconnekt.app.resources.token_usage_after_operation
import com.paperconnekt.app.resources.tokens
import com.paperconnekt.app.resources.top_sources
import com.paperconnekt.app.resources.unspecified_model
import com.paperconnekt.app.resources.usage_share_after_operation
import com.paperconnekt.app.resources.usage_input
import com.paperconnekt.app.resources.usage_pages
import com.paperconnekt.app.resources.workspace_opens_in_range
import com.paperconnekt.app.resources.no_workspace_opens_in_range
import com.paperconnekt.app.resources.service_costs_grouped
import com.paperconnekt.app.resources.year
import com.paperconnekt.app.resources.year_placeholder
import com.paperconnekt.app.resources.year_validation
import com.paperconnekt.app.resources.abstract_label
import com.paperconnekt.app.resources.authors
import com.paperconnekt.app.resources.build_collection_hint
import com.paperconnekt.app.resources.local_library_no_account
import com.paperconnekt.app.resources.welcome_back
import com.paperconnekt.app.resources.workspace

@Composable
fun DashboardHeader(
    paperCount: Int,
    includeRemote: Boolean,
    onLibrary: () -> Unit,
    onAssistant: () -> Unit,
) {
    WorkspaceHeader(
        title = stringResource(Res.string.room_for_next_idea),
        subtitle = stringResource(Res.string.welcome_back),
        icon = Res.drawable.dashboard,
        summaries = listOf(
            stringResource(Res.string.saved_papers_count, paperCount),
            stringResource(if (includeRemote) Res.string.openalex_discovery_enabled else Res.string.local_first_mode),
        ),
        actions = {
            HeaderAction(stringResource(Res.string.open_library), Res.drawable.book, onLibrary)
            HeaderAction(stringResource(Res.string.nav_assistant), Res.drawable.lightbulb, onAssistant, primary = false)
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
        Text(stringResource(Res.string.quick_actions), style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Button(onClick = onDiscover, modifier = Modifier.weight(1f)) {
                Text(stringResource(Res.string.discover_papers))
            }
            if (!compact) {
                Button(onClick = onAssistant, modifier = Modifier.weight(1f)) {
                    Text(stringResource(Res.string.open_ai_assistance))
                }
                Button(onClick = onLibrary, modifier = Modifier.weight(1f)) {
                    Text(stringResource(Res.string.open_library))
                }
            }
        }
        if (compact) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(onClick = onAssistant, modifier = Modifier.weight(1f)) {
                    Text(stringResource(Res.string.ai_assistance))
                }
                Button(onClick = onLibrary, modifier = Modifier.weight(1f)) {
                    Text(stringResource(Res.string.library))
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
        stringResource(Res.string.saved_papers_metric) to paperCount,
        stringResource(Res.string.researchers_metric) to authorCount,
        stringResource(Res.string.connected_graphs_metric) to graphCount,
        stringResource(Res.string.filed_entries_metric) to libraryCount,
        stringResource(Res.string.ai_operations_metric) to aiEventCount,
        stringResource(Res.string.estimated_ai_cost_metric) to formatDashboardUsd(aiCostUsd),
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
            Text(stringResource(Res.string.research_insights), style = MaterialTheme.typography.titleMedium)
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

private enum class DashboardRange(val dayCount: Int?) {
    TODAY(1),
    DAYS_7(7),
    DAYS_14(14),
    DAYS_30(30),
    DAYS_90(90),
    ALL(null),
    CUSTOM(1),
}

private enum class DashboardPieMetric {
    TOKENS,
    COST,
}

@Composable
private fun DashboardRange.localizedLabel(): String = stringResource(
    when (this) {
        DashboardRange.TODAY -> Res.string.today_label
        DashboardRange.DAYS_7 -> Res.string.days_7
        DashboardRange.DAYS_14 -> Res.string.days_14
        DashboardRange.DAYS_30 -> Res.string.days_30
        DashboardRange.DAYS_90 -> Res.string.days_90
        DashboardRange.ALL -> Res.string.all_time
        DashboardRange.CUSTOM -> Res.string.choose_day
    },
)

@Composable
private fun DashboardPieMetric.localizedLabel(): String = stringResource(
    when (this) {
        DashboardPieMetric.TOKENS -> Res.string.tokens
        DashboardPieMetric.COST -> Res.string.cost
    },
)

@Composable
fun DashboardAnalytics(
            history: ResearchHistory,
            papers: List<Paper>,
            graphs: List<com.paperconnekt.domain.model.ResearchGraph>,
            historyLoading: Boolean,
            historyError: String?,
        ) {
            val today = kotlinx.datetime.Clock.System.now()
                .toLocalDateTime(TimeZone.currentSystemDefault()).date
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
                        Text(stringResource(Res.string.workspace), style = MaterialTheme.typography.titleLarge)
                        Text(
                            stringResource(Res.string.dashboard_description),
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
                            WorkspaceFilterChip(
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
                                            range.localizedLabel()
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
                                Text(stringResource(Res.string.loading_placeholder), style = MaterialTheme.typography.titleMedium)
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
                                                    range.localizedLabel()
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
                            WorkspaceFilterChip(
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
                                            range.localizedLabel()
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
                        title = { Text(stringResource(Res.string.choose_a_day)) },
                        text = {
                            OutlinedTextField(
                                value = dayInput,
                                onValueChange = { dayInput = it },
                                label = { Text(stringResource(Res.string.date_format_hint)) },
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
                                Text(stringResource(Res.string.save))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDayDialog = false }) {
                                Text(stringResource(Res.string.cancel))
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
                        Modifier.fillMaxWidth().height(340.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        DashboardActivityChart(history.dailyOpens, chartDays, Modifier.weight(1f).fillMaxHeight())
                        DashboardCostChart(usage, chartDays, Modifier.weight(1f).fillMaxHeight())
                    }
                    Row(
                        Modifier.fillMaxWidth().height(278.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        DashboardTokenChart(visibleUsage, Modifier.weight(1f).fillMaxHeight())
                        DashboardServiceChart(summary.byService, visibleUsage, Modifier.weight(1f).fillMaxHeight())
                    }
                    Row(
                        Modifier.fillMaxWidth().height(418.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        DashboardPieChart(visibleUsage, Modifier.weight(1f).fillMaxHeight())
                        DashboardCollectionChart(papers, graphs, Modifier.weight(1f).fillMaxHeight())
                    }
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
                stringResource(Res.string.research_rhythm_title),
                if (total == 0) {
                    stringResource(Res.string.no_workspace_opens_in_range, rangeLabel(days))
                } else {
                    stringResource(Res.string.workspace_opens_in_range, total, rangeLabel(days))
                },
                modifier,
            ) {
                DashboardBarPlot(
                    values = values.map(Int::toFloat),
                    maxValue = peak.toFloat(),
                    color = MaterialTheme.colorScheme.primary,
                    description = days.zip(values).joinToString { "${it.first}: ${it.second} opens" },
                    emptyMessage = stringResource(Res.string.no_activity_period),
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
                label = stringResource(Res.string.global_scope),
                color = MaterialTheme.colorScheme.onSurface,
                values = globalValues,
                dashed = true,
            )
            DashboardChartCard(
                stringResource(Res.string.ai_cost_trend),
                if (total == 0.0) {
                    stringResource(Res.string.no_ai_cost_in_range, rangeLabel(days))
                } else {
                    stringResource(Res.string.ai_cost_in_range, rangeLabel(days), formatDashboardUsd(total))
                },
                modifier,
            ) {
                DashboardLinePlot(
                    series = series,
                    description = days.zip(globalValues).joinToString { "${it.first}: ${formatDashboardUsd(it.second)}" },
                    emptyMessage = stringResource(Res.string.no_cost_period),
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
            DashboardChartCard(
                stringResource(Res.string.token_flow),
                stringResource(Res.string.tracked_units, formatCompactNumber(total)),
                modifier,
            ) {
                if (total == 0L) {
                    DashboardPlotEmptyState(
                        message = stringResource(Res.string.no_token_usage),
                        detail = stringResource(Res.string.token_usage_after_operation),
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        TokenBar(stringResource(Res.string.input_tokens_units), input, total, MaterialTheme.colorScheme.primary)
                        TokenBar(stringResource(Res.string.output_tokens_units), output, total, MaterialTheme.colorScheme.secondary)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(Res.string.input_tokens, formatCompactNumber(input)), style = MaterialTheme.typography.labelSmall)
                            Text(stringResource(Res.string.output_tokens, formatCompactNumber(output)), style = MaterialTheme.typography.labelSmall)
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
            DashboardChartCard(stringResource(Res.string.cost_by_service), stringResource(Res.string.estimated_spend), modifier) {
                if (total == 0.0) {
                    DashboardPlotEmptyState(
                        message = stringResource(Res.string.no_service_usage),
                        detail = stringResource(Res.string.service_costs_grouped),
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        AiUsageService.entries.forEach { service ->
                            val value = values[service] ?: 0.0
                            val fraction = value / total
                            val serviceEvents = events.filter { it.service == service }
                            val units = serviceEvents.sumOf { it.inputUnits.coerceAtLeast(0L) }
                            val usageLabel = when (service) {
                                AiUsageService.OCR -> stringResource(Res.string.usage_pages, formatCompactNumber(units))
                                else -> stringResource(Res.string.usage_input, formatCompactNumber(units))
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
                stringResource(Res.string.ai_usage_share),
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
                        WorkspaceFilterChip(
                            selected = metric == option,
                            onClick = { metric = option },
                            label = { Text(option.localizedLabel()) },
                        )
                    }
                }
                if (values.isEmpty()) {
                    DashboardPlotEmptyState(
                        message = stringResource(Res.string.no_ai_usage),
                        detail = stringResource(Res.string.usage_share_after_operation),
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
            DashboardChartCard(
                stringResource(Res.string.collection_shape),
                stringResource(Res.string.papers_connected_graphs, papers.size, graphs.size),
                modifier,
            ) {
                if (papers.isEmpty()) {
                    DashboardPlotEmptyState(
                        message = stringResource(Res.string.collection_ready),
                        detail = stringResource(Res.string.add_papers_for_trends),
                    )
                } else {
                    if (years.isNotEmpty()) {
                        Text(stringResource(Res.string.papers_by_publication_year), style = MaterialTheme.typography.labelLarge)
                        if (years.size == 1) {
                            val (year, count) = years.single()
                            Text(
                                stringResource(Res.string.papers_from_year, count, year),
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
                            emptyMessage = stringResource(Res.string.no_publication_years_recorded),
                        )
                    } else {
                        Text(
                            stringResource(Res.string.add_years_for_trend),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Text(stringResource(Res.string.top_sources), style = MaterialTheme.typography.labelLarge)
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
            val unspecifiedModel = stringResource(Res.string.unspecified_model)
            val models = events.groupingBy { it.model.ifBlank { unspecifiedModel } }.eachCount()
            if (models.isEmpty()) return
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(Res.string.ai_model_activity), style = MaterialTheme.typography.titleMedium)
                    models.entries.sortedByDescending { it.value }.take(6).forEach { (model, count) ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(model, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(stringResource(Res.string.operations_count, count), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            emptyMessage: String? = null,
        ) {
            if (values.none { it > 0f }) {
                DashboardPlotEmptyState(emptyMessage ?: stringResource(Res.string.no_data_period))
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
            emptyMessage: String? = null,
        ) {
            val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
            val hasValues = series.any { line -> line.values.any { it > 0.0 } }
            if (!hasValues) {
                DashboardPlotEmptyState(emptyMessage ?: stringResource(Res.string.no_data_period))
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

@Composable
private fun rangeLabel(days: List<LocalDate>): String = when {
    days.size == 1 -> stringResource(Res.string.range_on_day, days.first())
    days.size <= 14 -> stringResource(Res.string.range_last_days, days.size)
    else -> stringResource(Res.string.range_dates, days.first(), days.last())
}

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
                paper.doi?.let { Text(stringResource(Res.string.doi_value, it), style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
            if (paper.tags.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    paper.tags.take(3).forEach { tag ->
                        WorkspaceFilterChip(selected = false, onClick = {}, label = { Text(tag) })
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
                    Text(stringResource(Res.string.open_graph_action))
                }
                if (paper.pdfPath != null || paper.sourceUrl != null) {
                    TextButton(onClick = onOpen) {
                        Text(stringResource(if (paper.pdfPath != null) Res.string.open_paper else Res.string.open_source))
                    }
                }
                if (isSaved) {
                    TextButton(onClick = onRemove) { Text(stringResource(Res.string.remove)) }
                } else {
                    TextButton(onClick = onSave) { Text(stringResource(Res.string.save_locally)) }
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
                if (hasQuery) stringResource(Res.string.no_matching_papers) else stringResource(Res.string.research_collection_waiting),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                if (hasQuery) {
                    stringResource(Res.string.dashboard_search_empty_hint)
                } else {
                    stringResource(Res.string.dashboard_empty_hint)
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onAddPaper) { Text(stringResource(Res.string.add_paper)) }
                TextButton(onClick = onDiscover) { Text(stringResource(Res.string.discover_online)) }
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
                                stringResource(Res.string.build_collection_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = onDismiss, enabled = !busy) {
                            Icon(painterResource(Res.drawable.filled_close), contentDescription = stringResource(Res.string.close_add_paper))
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Column(
                        Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(inset),
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                    ) {
                        val metadata: @Composable () -> Unit = {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                PaperFormSection(
                                    stringResource(Res.string.paper_details_section),
                                    stringResource(Res.string.title_required_edit_later),
                                )
                                PaperFormField(
                                    value = titleValue,
                                    onValueChange = onTitleChange,
                                    label = stringResource(Res.string.paper_title_label),
                                    placeholder = stringResource(Res.string.enter_full_title),
                                    modifier = Modifier.focusRequester(titleFocus),
                                    singleLine = false,
                                    enabled = !saving,
                                )
                                PaperFormField(
                                    value = authorsValue,
                                    onValueChange = onAuthorsChange,
                                    label = stringResource(Res.string.authors),
                                    placeholder = stringResource(Res.string.authors_example),
                                    enabled = !saving,
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    PaperFormField(
                                        value = yearValue,
                                        onValueChange = onYearChange,
                                        label = stringResource(Res.string.year),
                                        placeholder = stringResource(Res.string.year_placeholder),
                                        modifier = Modifier.weight(0.35f),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        isError = invalidYear,
                                        enabled = !saving,
                                    )
                                    PaperFormField(
                                        value = doiValue,
                                        onValueChange = onDoiChange,
                                        label = stringResource(Res.string.doi),
                                        placeholder = stringResource(Res.string.doi_placeholder),
                                        modifier = Modifier.weight(0.65f),
                                        enabled = !saving,
                                    )
                                }
                                if (invalidYear) {
                                    Text(
                                        stringResource(Res.string.year_validation),
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                                PaperFormField(
                                    value = abstractValue,
                                    onValueChange = onAbstractChange,
                                    label = stringResource(Res.string.abstract_label),
                                    placeholder = stringResource(Res.string.paper_abstract_placeholder),
                                    singleLine = false,
                                    minLines = 4,
                                    enabled = !saving,
                                )
                                PaperFormField(
                                    value = tagsValue,
                                    onValueChange = onTagsChange,
                                    label = stringResource(Res.string.tags),
                                    placeholder = stringResource(Res.string.tags_example),
                                    enabled = !saving,
                                )
                                Text(
                                    stringResource(Res.string.comma_separated_authors_tags),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        val attachment: @Composable () -> Unit = {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                PaperFormSection(
                                    stringResource(Res.string.reading_copy_section),
                                    stringResource(Res.string.optional_pdf_attachment),
                                )
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
                                        Text(stringResource(Res.string.local_first_workspace), style = MaterialTheme.typography.labelLarge)
                                        Text(
                                            stringResource(Res.string.managed_pdf_copy),
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
                                stringResource(Res.string.local_library_no_account),
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            Spacer(Modifier.weight(1f))
                        }
                        TextButton(onClick = onDismiss, enabled = !busy) { Text(stringResource(Res.string.cancel)) }
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
