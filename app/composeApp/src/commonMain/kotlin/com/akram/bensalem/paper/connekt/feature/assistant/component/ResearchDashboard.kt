package com.akram.bensalem.paper.connekt.feature.assistant.component

import com.paperconnekt.app.resources.Res
import com.paperconnekt.app.resources.filled_arrow_forward
import com.paperconnekt.app.resources.filled_history
import com.paperconnekt.app.resources.filled_search
import org.jetbrains.compose.resources.painterResource
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.akram.bensalem.bistosh.core.designsystem.component.clipChipInteraction
import com.akram.bensalem.bistosh.core.designsystem.component.interactivePointer
import com.akram.bensalem.paper.connekt.feature.assistant.AssistantUiState
import com.akram.bensalem.paper.connekt.feature.assistant.AiUsageEvent
import com.akram.bensalem.paper.connekt.feature.assistant.AiUsageService
import com.akram.bensalem.paper.connekt.feature.assistant.AiUsageSummary
import com.akram.bensalem.paper.connekt.feature.assistant.StudiedPaper
import com.akram.bensalem.paper.connekt.feature.assistant.ZoteroPdfTarget
import com.paperconnekt.app.resources.filled_schedule
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.math.round

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ResearchDashboard(
    state: AssistantUiState,
    onOpenPaper: (ZoteroPdfTarget) -> Unit,
    onOpenZotero: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    var selectedTag by remember { mutableStateOf<String?>(null) }
    var onlyThisWeek by remember { mutableStateOf(false) }
    var costPaperId by remember(state.historyAccountId) { mutableStateOf<String?>(null) }
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val papers = state.history.papers
    val latest = papers.firstOrNull()
    val tags = papers.flatMap { paper ->
        paper.target.item.data.tags.map { it.tag }.filter { it.isNotBlank() }.distinct()
    }.groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.take(6)
    val filtered = papers.filter { paper ->
        (query.isBlank() || "${paper.title} ${paper.authors} ${paper.target.item.data.tags.joinToString { it.tag }}"
            .contains(query.trim(), ignoreCase = true)) &&
            (selectedTag == null || paper.target.item.data.tags.any { it.tag == selectedTag }) &&
            (!onlyThisWeek || localDate(paper.lastOpenedAt) >= today - DatePeriod(days = 6))
    }
    LaunchedEffect(state.historyAccountId) {
        query = ""
        selectedTag = null
        onlyThisWeek = false
    }
    papers.firstOrNull { it.id == costPaperId }?.let { paper ->
        PaperCostDialog(paper = paper, onDismiss = { costPaperId = null })
    }
    BoxWithConstraints(modifier.fillMaxSize()) {
        val compact = maxWidth < 700.dp
        val chartsSideBySide = maxWidth >= 900.dp
        LazyVerticalGrid(
            columns = GridCells.Adaptive(320.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(if (compact) 16.dp else 28.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Your research, in motion", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "Pick up where you left off. This space follows your workspace activity, not your entire Zotero library.",
                        style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (state.historyLoading) item(span = { GridItemSpan(maxLineSpan) }) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            state.historyError?.let { error ->
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.medium) {
                        Column(Modifier.padding(16.dp)) {
                            Text(error, color = MaterialTheme.colorScheme.onErrorContainer)
                            TextButton(onClick = onRetry, modifier = Modifier.interactivePointer()) { Text("Reload history") }
                        }
                    }
                }
            }
            if (latest == null && state.history.graphUsage.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    AiCostChart(state.history.usage, today)
                }
            }
            if (latest == null && !state.historyLoading && state.historyError == null) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Card {
                        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(painterResource(Res.drawable.filled_history), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                            Text(
                                if (state.historyAvailable) "Your next study starts here" else "Connect your research library",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                if (state.historyAvailable)
                                    "Open a PDF from Zotero to start your research history. Future visits appear here with a direct route back to the workspace. Earlier visits were not recorded."
                                else "Configure Zotero to keep a personal research history and reopen the PDFs you study.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(onClick = onOpenZotero, modifier = Modifier.interactivePointer()) { Text("Go to Zotero") }
                        }
                    }
                }
            } else if (latest != null) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("CONTINUE RESEARCH", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(latest.title, style = MaterialTheme.typography.headlineSmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
                            Text(
                                "Last opened ${dateLabel(latest.lastOpenedAt, today)} · ${libraryLabel(latest)}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            StudyTimeLabel(latest)
                            UsageCostLabel(latest, onShowBreakdown = { costPaperId = latest.id })
                            Button(
                                onClick = { onOpenPaper(latest.target) },
                                enabled = state.historyAvailable,
                                modifier = Modifier.clipChipInteraction().interactivePointer()
                            ) {
                                Text("Resume in workspace")
                                Spacer(Modifier.width(8.dp))
                                Icon(painterResource(Res.drawable.filled_arrow_forward), null, Modifier.size(18.dp))
                            }
                        }
                    }
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        FlowRow(
                            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(28.dp, Alignment.Start),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ResearchMetric(papers.size.toString(), "PDFs explored")
                            ResearchMetric(papers.sumOf { it.openCount }.toString(), "Workspace opens")
                            ResearchMetric(state.history.dailyOpens.size.toString(), "Active days")
                            ResearchMetric(formatUsd(state.history.usageSummary.totalCostUsd), "Estimated AI cost · all time")
                        }
                    }
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    if (chartsSideBySide) {
                        Row(
                            Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            ActivityChart(state.history.dailyOpens, today, Modifier.weight(1f).fillMaxHeight())
                            AiCostChart(state.history.usage, today, Modifier.weight(1f).fillMaxHeight())
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            ActivityChart(state.history.dailyOpens, today)
                            AiCostChart(state.history.usage, today)
                        }
                    }
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    OutlinedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Research themes", style = MaterialTheme.typography.titleMedium)
                            Text("Zotero tags on PDFs you've opened. Select a theme to filter your history.", style = MaterialTheme.typography.bodySmall)
                            if (tags.isEmpty()) {
                                Text("No tags on your studied papers yet. Add tags in Zotero to reveal your research themes.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                tags.forEach { tag ->
                                    FilterChip(
                                        selected = selectedTag == tag.key,
                                        onClick = { selectedTag = tag.key.takeUnless { selectedTag == it } },
                                        label = { Text("${tag.key} · ${tag.value} PDF${if (tag.value == 1) "" else "s"}", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                        modifier = Modifier.clipChipInteraction().interactivePointer()
                                    )
                                }
                            }
                        }
                    }
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("Study history", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                            TextButton(onClick = onOpenZotero, modifier = Modifier.interactivePointer()) { Text("Browse Zotero") }
                        }
                        OutlinedTextField(
                            value = query, onValueChange = { query = it },
                            label = { Text("Find a studied paper") },
                            placeholder = { Text("Title, author, or tag") },
                            leadingIcon = { Icon(painterResource(Res.drawable.filled_search), null) },
                            singleLine = true, modifier = Modifier.fillMaxWidth()
                        )
                        CompositionLocalProvider(LocalRippleConfiguration provides null) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = !onlyThisWeek, onClick = { onlyThisWeek = false },
                                    label = { Text("All history") },
                                    modifier = Modifier.clipChipInteraction().interactivePointer(),
                                    shape = RoundedCornerShape(8.dp),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = !onlyThisWeek,
                                        borderColor = Color.Transparent,
                                        selectedBorderColor = Color.Transparent
                                    )
                                )
                                FilterChip(
                                    selected = onlyThisWeek, onClick = { onlyThisWeek = true },
                                    label = { Text("Last 7 days") },
                                    modifier = Modifier.clipChipInteraction().interactivePointer(),
                                    shape = RoundedCornerShape(8.dp),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = onlyThisWeek,
                                        borderColor = Color.Transparent,
                                        selectedBorderColor = Color.Transparent
                                    )
                                )
                                selectedTag?.let { tag ->
                                    InputChip(
                                        selected = true, onClick = { selectedTag = null },
                                        label = { Text("Clear theme: $tag") },
                                        modifier = Modifier.clipChipInteraction().interactivePointer(),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            "${filtered.size} of ${papers.size} PDFs · Most recently opened first",
                            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (filtered.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
                    Text("No studied papers match these filters.", modifier = Modifier.padding(vertical = 16.dp))
                }
                items(filtered, key = { it.id }) { paper ->
                    OutlinedCard(
                        onClick = { onOpenPaper(paper.target) },
                        enabled = state.historyAvailable,
                        modifier = Modifier.fillMaxWidth().interactivePointer()
                    ) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                dateLabel(paper.lastOpenedAt, today),
                                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary
                            )
                            Text(paper.title, style = MaterialTheme.typography.titleMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
                            if (paper.authors.isNotBlank()) Text(
                                paper.authors, style = MaterialTheme.typography.bodySmall,
                                maxLines = 2, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            StudyTimeLabel(paper)
                            UsageCostLabel(paper, onShowBreakdown = { costPaperId = paper.id })
                            Text(
                                "${libraryLabel(paper)} · ${paper.openCount} open${if (paper.openCount == 1) "" else "s"}",
                                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Open workspace", style = MaterialTheme.typography.labelLarge)
                                Spacer(Modifier.width(8.dp))
                                Icon(painterResource(Res.drawable.filled_arrow_forward), null, Modifier.size(16.dp))
                            }
                        }
                    }
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        "History is saved on this device for your Zotero account. AI costs are estimates: Mistral OCR \$4/1,000 pages (\$0.40/1,000 cached pages), GPT-6 Luna \$0.10/\$0.50 per million input/output tokens, GPT-5 Mini \$0.25/\$2.00, and Mistral Medium \$0.50/\$1.50. Study time measures how long each PDF workspace stays open; it does not indicate attention or completion.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ResearchMetric(value: String, label: String) {
    Column(Modifier.padding(vertical = 8.dp)) {
        Text(value, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StudyTimeLabel(paper: StudiedPaper) {
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
    StudyTimeIndicator(paper.dailyStudyMillis[today] ?: 0L, paper.totalStudyMillis)
}

@Composable
private fun UsageCostLabel(paper: StudiedPaper, onShowBreakdown: () -> Unit) {
    val summary = AiUsageSummary(paper.usage)
    Column {
        Text(
            "AI estimate ${formatUsd(summary.totalCostUsd)} · Today ${formatUsd(summary.todayCostUsd)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        TextButton(onClick = onShowBreakdown, modifier = Modifier.interactivePointer()) {
            Text("Cost breakdown")
        }
    }
}

@Composable
private fun PaperCostDialog(paper: StudiedPaper, onDismiss: () -> Unit) {
    val summary = AiUsageSummary(paper.usage)
    val costs = summary.byService
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("AI cost breakdown") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(paper.title, style = MaterialTheme.typography.titleMedium)
                Text(libraryLabel(paper), style = MaterialTheme.typography.labelSmall)
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Estimated total · USD", style = MaterialTheme.typography.labelMedium)
                        Text(
                            formatUsd(summary.totalCostUsd),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text("Today ${formatUsd(summary.todayCostUsd)}", style = MaterialTheme.typography.bodySmall)
                    }
                }
                AiUsageService.entries.forEach { service ->
                    val events = paper.usage.filter { it.service == service }
                    val cost = costs[service] ?: 0.0
                    val todayCost = events.filter { usageDate(it) == today }.sumOf { it.estimatedCostUsd }
                    val input = events.sumOf { it.inputUnits.coerceAtLeast(0L) }
                    val output = events.sumOf { it.outputUnits.coerceAtLeast(0L) }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(service.label, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                            Text(formatUsd(cost), style = MaterialTheme.typography.titleSmall)
                        }
                        Text(
                            if (events.isEmpty()) "No recorded usage" else when (service) {
                                AiUsageService.OCR -> "$input pages processed"
                                AiUsageService.CHAT, AiUsageService.LITERATURE_TRIAGE, AiUsageService.RELEVANCE_SCORING ->
                                    "$input input / $output output tokens"
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                        LinearProgressIndicator(
                            progress = { if (summary.totalCostUsd > 0.0) (cost / summary.totalCostUsd).toFloat() else 0f },
                            modifier = Modifier.fillMaxWidth(),
                            color = serviceColor(service)
                        )
                        Text(
                            "Today ${formatUsd(todayCost)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    "Estimates from recorded usage on this device, not provider invoices. Earlier or unrecorded usage is not included.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.interactivePointer()) { Text("Done") }
        }
    )
}

fun formatUsd(value: Double): String {
    if (value > 0.0 && value < 0.0001) return "<\$0.0001"
    val scaled = round(value.coerceAtLeast(0.0) * 10_000.0).toLong()
    return "\$${scaled / 10_000}.${(scaled % 10_000).toString().padStart(4, '0')}"
}

@Composable
fun StudyTimeIndicator(todayMillis: Long, totalMillis: Long) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painterResource(Res.drawable.filled_schedule),
            contentDescription = "Study time",
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(4.dp))
        Text(
            "Total ${studyDurationLabel(totalMillis)} · Today ${studyDurationLabel(todayMillis)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

fun studyDurationLabel(millis: Long): String {
    val totalSeconds = millis.coerceAtLeast(0L) / 1_000L
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        "${hours}h ${minutes.toString().padStart(2, '0')}m ${seconds.toString().padStart(2, '0')}s"
    } else if (minutes > 0L) {
        "${minutes}m ${seconds.toString().padStart(2, '0')}s"
    } else {
        "${seconds}s"
    }
}

@Composable
private fun ActivityChart(dailyOpens: Map<String, Int>, today: LocalDate, modifier: Modifier = Modifier) {
    val days = (13 downTo 0).map { today - DatePeriod(days = it) }
    val values = days.map { dailyOpens[it.toString()] ?: 0 }
    val peak = values.maxOrNull()?.coerceAtLeast(1) ?: 1
    val activeColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    OutlinedCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Your research rhythm", style = MaterialTheme.typography.titleMedium)
            Text("${values.sum()} workspace opens in the last 14 days", style = MaterialTheme.typography.bodySmall)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Opens / day", style = MaterialTheme.typography.labelSmall)
                Text("Peak ${values.maxOrNull() ?: 0}", style = MaterialTheme.typography.labelSmall)
            }
            Canvas(
                Modifier.fillMaxWidth().height(180.dp).semantics {
                    contentDescription = days.zip(values).joinToString { (day, count) -> "$day: $count opens" }
                }
            ) {
                val slot = size.width / days.size
                val width = slot * 0.65f
                values.forEachIndexed { index, count ->
                    val height = if (count == 0) 4.dp.toPx() else (size.height * count / peak).coerceAtLeast(4.dp.toPx())
                    drawRoundRect(
                        color = if (count == 0) trackColor else activeColor,
                        topLeft = Offset(index * slot + (slot - width) / 2, size.height - height),
                        size = Size(width, height), cornerRadius = CornerRadius(4.dp.toPx())
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(days.first().toString(), style = MaterialTheme.typography.labelSmall)
                Text("Today", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AiCostChart(events: List<AiUsageEvent>, today: LocalDate, modifier: Modifier = Modifier) {
    val days = (13 downTo 0).map { today - DatePeriod(days = it) }
    val periodEvents = events.filter { usageDate(it) in days.first()..today }
    val summary = AiUsageSummary(periodEvents)
    val services = AiUsageService.entries
    val serviceValues = services.associateWith { service ->
        days.map { day ->
            periodEvents.asSequence()
                .filter { it.service == service && usageDate(it) == day }
                .sumOf { it.estimatedCostUsd }
        }
    }
    val totalValues = days.indices.map { index ->
        services.sumOf { serviceValues.getValue(it)[index] }
    }
    val total = summary.totalCostUsd
    val peak = (totalValues + serviceValues.values.flatten()).maxOrNull()?.coerceAtLeast(0.0000001) ?: 0.0000001
    val lineColors = services.map { serviceColor(it) }
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    val totalColor = MaterialTheme.colorScheme.onSurface

    OutlinedCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("AI cost rhythm", style = MaterialTheme.typography.titleMedium)
            Text(
                "Daily estimated cost over the last 14 days · Total ${formatUsd(total)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("USD / day", style = MaterialTheme.typography.labelSmall)
                Text("Peak ${formatUsd(totalValues.maxOrNull() ?: 0.0)}", style = MaterialTheme.typography.labelSmall)
            }
            Canvas(
                Modifier.fillMaxWidth().height(180.dp).semantics {
                    contentDescription = days.indices.joinToString { index ->
                        "${days[index]}: ${formatUsd(totalValues[index])} total"
                    }
                }
            ) {
                val chartHeight = size.height - 8.dp.toPx()
                val xStep = size.width / days.lastIndex.coerceAtLeast(1)
                fun x(index: Int) = index * xStep
                fun y(value: Double) = chartHeight - (value / peak).toFloat() * chartHeight

                for (index in 0..3) {
                    val gridY = chartHeight * index / 3f
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, gridY),
                        end = Offset(size.width, gridY),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
                services.forEachIndexed { serviceIndex, service ->
                    serviceValues.getValue(service).zipWithNext().forEachIndexed { index, (from, to) ->
                        drawLine(
                            color = lineColors[serviceIndex],
                            start = Offset(x(index), y(from)),
                            end = Offset(x(index + 1), y(to)),
                            strokeWidth = 3.dp.toPx(),
                        )
                    }
                }
                totalValues.zipWithNext().forEachIndexed { index, (from, to) ->
                    drawLine(
                        color = totalColor,
                        start = Offset(x(index), y(from)),
                        end = Offset(x(index + 1), y(to)),
                        strokeWidth = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx())),
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(days.first().toString(), style = MaterialTheme.typography.labelSmall)
                Text("Today", style = MaterialTheme.typography.labelSmall)
            }
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                services.forEachIndexed { index, service ->
                    AiCostLegendItem(
                        label = service.label,
                        value = formatUsd(summary.byService[service] ?: 0.0),
                        color = lineColors[index],
                    )
                }
                AiCostLegendItem(label = "Total", value = formatUsd(total), color = totalColor, dashed = true)
            }
            if (periodEvents.isEmpty()) {
                Text(
                    "No AI usage recorded in the last 14 days.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun serviceColor(service: AiUsageService) = when (service) {
    AiUsageService.OCR -> MaterialTheme.colorScheme.primary
    AiUsageService.CHAT -> MaterialTheme.colorScheme.tertiary
    AiUsageService.LITERATURE_TRIAGE -> MaterialTheme.colorScheme.inversePrimary
    AiUsageService.RELEVANCE_SCORING -> MaterialTheme.colorScheme.secondaryContainer
}

@Composable
private fun AiCostLegendItem(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    dashed: Boolean = false,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(Modifier.width(18.dp).height(8.dp)) {
            drawLine(
                color = color,
                start = Offset(0f, size.height / 2),
                end = Offset(size.width, size.height / 2),
                strokeWidth = if (dashed) 2.dp.toPx() else 3.dp.toPx(),
                pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx())) else null,
            )
        }
        Text("$label $value", style = MaterialTheme.typography.labelSmall)
    }
}

private fun usageDate(event: AiUsageEvent): LocalDate =
    Instant.fromEpochMilliseconds(event.timestamp)
        .toLocalDateTime(TimeZone.currentSystemDefault()).date

private fun localDate(timestamp: Long): LocalDate =
    Instant.fromEpochMilliseconds(timestamp).toLocalDateTime(TimeZone.currentSystemDefault()).date

private fun dateLabel(timestamp: Long, today: LocalDate): String {
    val date = localDate(timestamp)
    val time = Instant.fromEpochMilliseconds(timestamp).toLocalDateTime(TimeZone.currentSystemDefault()).time
    val day = when (date) {
        today -> "Today"
        today - DatePeriod(days = 1) -> "Yesterday"
        else -> date.toString()
    }
    return "$day at ${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')}"
}

private fun libraryLabel(paper: StudiedPaper): String =
    paper.target.groupId?.let { "Group library $it" } ?: "Personal library"
