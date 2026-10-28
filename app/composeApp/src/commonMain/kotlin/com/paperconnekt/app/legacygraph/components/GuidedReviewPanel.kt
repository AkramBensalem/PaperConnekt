package com.paperconnekt.app.legacygraph.components

import com.paperconnekt.app.legacygraph.clipChipInteraction
import com.paperconnekt.app.legacygraph.interactivePointer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.paperconnekt.app.legacygraph.ConnectionType
import com.paperconnekt.app.legacygraph.GuidedAiVerdict
import com.paperconnekt.app.legacygraph.GuidedRelation
import com.paperconnekt.app.legacygraph.GuidedReviewDirection
import com.paperconnekt.app.legacygraph.GuidedReviewState
import com.paperconnekt.app.legacygraph.GuidedSuggestion
import com.paperconnekt.app.legacygraph.PaperNode
import kotlin.math.roundToInt

private val BATCH_SIZES = listOf(3, 5, 10, 100)

/**
 * Structured research-target scoring for a paper's connections, with optional manual batch browsing.
 */
@Composable
fun GuidedReviewPanel(
    node: PaperNode,
    review: GuidedReviewState,
    isEvaluating: Boolean,
    keptPapers: List<PaperNode>,
    onClose: () -> Unit,
    onShowDetails: () -> Unit,
    onShowInsights: () -> Unit,
    onBatchSizeChanged: (Int) -> Unit,
    onShowNext: () -> Unit,
    onKeep: (String) -> Unit,
    onSkip: (String) -> Unit,
    onUndo: (String) -> Unit,
    onRevisitSkipped: () -> Unit,
    onReviewPaper: (String) -> Unit,
    onDismissError: () -> Unit,
    researchTargetContent: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSettings by remember(node.paper.id) { mutableStateOf(true) }
    val undecided = review.undecided.size
    val isAiBusy = review.isAiEvaluating || isEvaluating

    Surface(
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "header") {
            Column(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(Modifier.weight(1f), shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.tertiaryContainer) {
                        Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Explore, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onTertiaryContainer)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                if (node.connectionType == ConnectionType.CENTRAL) "Guided review · Seed" else "Guided review",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                    GraphAction(Icons.Default.Insights, "Neighborhood insights", onShowInsights)
                    GraphAction(Icons.Default.Info, "Paper details", onShowDetails)
                    GraphAction(Icons.Default.Close, "Close guided review", onClose)
                }
                Text(
                    node.paper.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    paperMeta(node),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                GuidedProgress(review)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
                item(key = "settings") {
                    GuidedSettings(
                        review = review,
                        expanded = showSettings,
                        onToggle = { showSettings = !showSettings },
                        isBusy = review.isLoading || isAiBusy,
                        onBatchSizeChanged = onBatchSizeChanged,
                        researchTargetContent = researchTargetContent
                    )
                }

                review.error?.let { error ->
                    item(key = "error") {
                        Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.errorContainer) {
                            Row(Modifier.padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    error,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.weight(1f).padding(vertical = 10.dp)
                                )
                                IconButton(onClick = onDismissError, modifier = Modifier.interactivePointer()) {
                                    Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = MaterialTheme.colorScheme.onErrorContainer)
                                }
                            }
                        }
                    }
                }

                if (review.batch.isEmpty() && !review.isLoading) {
                    item(key = "empty") { GuidedEmptyState(review) }
                }

                itemsIndexed(review.batch, key = { _, suggestion -> "suggestion-${suggestion.paperId}" }) { index, suggestion ->
                    GuidedSuggestionCard(
                        rank = index + 1,
                        suggestion = suggestion,
                        isKept = suggestion.paperId in review.keptIds,
                        isSkipped = suggestion.paperId in review.skippedIds,
                        onKeep = { onKeep(suggestion.paperId) },
                        onSkip = { onSkip(suggestion.paperId) },
                        onUndo = { onUndo(suggestion.paperId) },
                        onReview = { onReviewPaper(suggestion.paperId) }
                    )
                }

                if (keptPapers.isNotEmpty()) {
                    item(key = "kept") { KeptPapersSection(keptPapers, onReviewPaper) }
                }
            item(key = "actions") {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            // Footer actions
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (review.isLoading) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text(
                        review.loadingMessage ?: "Working…",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text("Manual connection browsing", style = MaterialTheme.typography.titleSmall)
                val canShowNext = !review.isLoading && !isAiBusy && !review.isExhausted
                val nextLabel = when {
                    review.isExhausted -> "All connections reviewed"
                    review.batch.isEmpty() -> "Show top ${review.batchSize}"
                    else -> "Next ${review.batchSize}"
                }
                    Button(
                        onClick = onShowNext,
                        enabled = canShowNext,
                        modifier = Modifier.fillMaxWidth().interactivePointer(enabled = canShowNext)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(nextLabel)
                    }
                if (undecided > 0 && !review.isLoading && !isAiBusy) {
                    Text(
                        "$undecided undecided ${if (undecided == 1) "paper" else "papers"} will be skipped when you continue. You can revisit skipped papers later.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (review.skippedIds.isNotEmpty() && !review.isLoading && !isAiBusy) {
                    OutlinedButton(
                        onClick = onRevisitSkipped,
                        modifier = Modifier.fillMaxWidth().interactivePointer()
                    ) {
                        Icon(Icons.Default.Refresh, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Revisit skipped (${review.skippedIds.size})")
                    }
                }
            }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GuidedProgress(review: GuidedReviewState) {
    val available = review.availableCount
    if (review.isQueueLoaded && available > 0) {
        val reviewed = (available - review.remainingCount).coerceIn(0, available)
        LinearProgressIndicator(
            progress = { reviewed.toFloat() / available },
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
        )
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        StatPill("${review.keptIds.size} kept", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
        StatPill("${review.skippedIds.size} skipped", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
        if (review.isQueueLoaded) {
            StatPill("${review.remainingCount} left", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}

@Composable
private fun StatPill(text: String, container: Color, content: Color) {
    Surface(shape = RoundedCornerShape(50), color = container, contentColor = content) {
        Text(text, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun GuidedSettings(
    review: GuidedReviewState,
    expanded: Boolean,
    onToggle: () -> Unit,
    isBusy: Boolean,
    onBatchSizeChanged: (Int) -> Unit,
    researchTargetContent: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().animateContentSize()
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Tune, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text("Review settings", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${directionLabel(review)} · research target, AI scores and relevance threshold",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onToggle, modifier = Modifier.interactivePointer()) {
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Hide review settings" else "Show review settings"
                    )
                }
            }
            AnimatedVisibility(expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    researchTargetContent()
                    HorizontalDivider()
                    Text("Manual browsing options", style = MaterialTheme.typography.titleSmall)
                    Text("Papers per batch", style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BATCH_SIZES.forEach { size ->
                            FilterChip(
                                selected = review.batchSize == size,
                                onClick = { onBatchSizeChanged(size) },
                                enabled = !isBusy,
                                label = { Text("Top $size") },
                                modifier = Modifier.clipChipInteraction().interactivePointer(enabled = !isBusy)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GuidedEmptyState(review: GuidedReviewState) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(52.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    if (review.isExhausted) Icons.Default.TaskAlt else Icons.Default.AutoAwesome,
                    null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        Text(
            if (review.isExhausted) "Every connection has been reviewed" else "Review only what matters",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            if (review.isExhausted)
                "Revisit skipped papers, or select another paper on the graph to continue."
            else "Set your research target in Review settings, evaluate all connections, and accept suggestions using the relevance threshold. You can also browse batches manually below.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GuidedSuggestionCard(
    rank: Int,
    suggestion: GuidedSuggestion,
    isKept: Boolean,
    isSkipped: Boolean,
    onKeep: () -> Unit,
    onSkip: () -> Unit,
    onUndo: () -> Unit,
    onReview: () -> Unit
) {
    val node = suggestion.candidate.node
    var showAbstract by remember(suggestion.paperId) { mutableStateOf(false) }
    val containerColor = when {
        isKept -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        isSkipped -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.surface
    }
    OutlinedCard(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        colors = CardDefaults.outlinedCardColors(containerColor = containerColor),
        border = BorderStroke(
            1.dp,
            if (isKept) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "#$rank",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                RelationChip(suggestion.candidate.relation)
                Spacer(Modifier.weight(1f))
                SimilarityBadge(suggestion)
            }
            Text(
                node.paper.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                color = if (isSkipped) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
            )
            Text(
                paperMeta(node),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            AiProposal(suggestion, isDecided = isKept || isSkipped)
            val abstract = node.paper.abstractText
            if (!abstract.isNullOrBlank()) {
                TextButton(
                    onClick = { showAbstract = !showAbstract },
                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
                    modifier = Modifier.height(28.dp).interactivePointer()
                ) {
                    Text(if (showAbstract) "Hide abstract" else "Read abstract", style = MaterialTheme.typography.labelMedium)
                    Icon(if (showAbstract) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, Modifier.size(16.dp))
                }
                AnimatedVisibility(showAbstract) {
                    Text(abstract, style = MaterialTheme.typography.bodySmall)
                }
            }
            when {
                isKept -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(4.dp))
                    Text("Kept in graph", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onUndo, modifier = Modifier.interactivePointer()) {
                        Icon(Icons.AutoMirrored.Filled.Undo, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Undo")
                    }
                    FilledTonalButton(onClick = onReview, modifier = Modifier.interactivePointer()) {
                        Text("Review its links")
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(16.dp))
                    }
                }
                isSkipped -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Skipped", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onUndo, modifier = Modifier.interactivePointer()) {
                        Icon(Icons.AutoMirrored.Filled.Undo, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Undo")
                    }
                }
                else -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val verdict = suggestion.aiVerdict
                    val skipLabel = if (verdict == GuidedAiVerdict.SKIP) "Skip · AI" else "Skip"
                    val keepLabel = if (verdict == GuidedAiVerdict.KEEP) "Keep · AI" else "Keep"
                    if (verdict == GuidedAiVerdict.SKIP) {
                        FilledTonalButton(onClick = onSkip, modifier = Modifier.weight(1f).interactivePointer()) {
                            Icon(Icons.Default.Close, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(skipLabel)
                        }
                    } else {
                        OutlinedButton(onClick = onSkip, modifier = Modifier.weight(1f).interactivePointer()) {
                            Icon(Icons.Default.Close, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(skipLabel)
                        }
                    }
                    if (verdict == GuidedAiVerdict.SKIP) {
                        OutlinedButton(onClick = onKeep, modifier = Modifier.weight(1f).interactivePointer()) {
                            Icon(Icons.Default.Check, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(keepLabel)
                        }
                    } else {
                        Button(onClick = onKeep, modifier = Modifier.weight(1f).interactivePointer()) {
                            Icon(Icons.Default.Check, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(keepLabel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RelationChip(relation: GuidedRelation) {
    val isReference = relation == GuidedRelation.REFERENCE
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (isReference) Color(0xFFCCFBF1) else Color(0xFFFEF3C7)
    ) {
        Row(Modifier.padding(horizontal = 6.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (isReference) Icons.AutoMirrored.Filled.ArrowBack else Icons.AutoMirrored.Filled.ArrowForward,
                null,
                Modifier.size(12.dp),
                tint = if (isReference) Color(0xFF0F766E) else Color(0xFFB45309)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                if (isReference) "Reference" else "Cites this",
                style = MaterialTheme.typography.labelSmall,
                color = if (isReference) Color(0xFF0F766E) else Color(0xFFB45309)
            )
        }
    }
}

@Composable
private fun SimilarityBadge(suggestion: GuidedSuggestion) {
    val percent = (suggestion.candidate.localScore * 100).roundToInt()
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        Text(
            "$percent% similar",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

/** Shows the AI's Keep/Skip proposal and rationale, or the free similarity reason without AI. */
@Composable
private fun AiProposal(suggestion: GuidedSuggestion, isDecided: Boolean) {
    val verdict = suggestion.aiVerdict
    when {
        suggestion.isAiEvaluating -> Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
            Text(
                "Waiting for GPT-6 Luna's decision…",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        verdict != null -> {
            val keep = verdict == GuidedAiVerdict.KEEP
            val container = if (keep) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
            val content = if (keep) Color(0xFF166534) else Color(0xFF991B1B)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = container.copy(alpha = if (isDecided) 0.5f else 1f),
                contentColor = content,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, null, Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (keep) "AI proposes: Keep" else "AI proposes: Skip",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    suggestion.aiRationale?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic)
                    }
                }
            }
        }
        else -> Row(verticalAlignment = Alignment.Top) {
            Icon(
                Icons.Default.Description,
                null,
                Modifier.size(14.dp).padding(top = 2.dp),
                tint = MaterialTheme.colorScheme.tertiary
            )
            Spacer(Modifier.width(6.dp))
            Text(
                suggestion.reason,
                style = MaterialTheme.typography.bodySmall,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun KeptPapersSection(keptPapers: List<PaperNode>, onReviewPaper: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().animateContentSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        TextButton(onClick = { expanded = !expanded }, modifier = Modifier.interactivePointer()) {
            Text("Kept from this paper (${keptPapers.size})", style = MaterialTheme.typography.labelLarge)
            Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, Modifier.size(18.dp))
        }
        if (expanded) {
            keptPapers.forEach { kept ->
                Surface(
                    onClick = { onReviewPaper(kept.paper.id) },
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth().interactivePointer()
                ) {
                    Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            kept.paper.title,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Review this paper", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

private fun directionLabel(review: GuidedReviewState) = when (review.direction) {
    GuidedReviewDirection.BOTH -> "Citations & references"
    GuidedReviewDirection.REFERENCES -> "References"
    GuidedReviewDirection.CITATIONS -> "Citations"
}

private fun paperMeta(node: PaperNode): String = listOfNotNull(
    node.paper.authors.firstOrNull()?.let { if (node.paper.authors.size > 1) "$it et al." else it },
    node.paper.year?.toString(),
    "${node.citationCount} citations"
).joinToString(" · ")
