package com.paperconnekt.app.legacygraph.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.paperconnekt.app.legacygraph.clipChipInteraction
import com.paperconnekt.app.legacygraph.interactivePointer
import com.paperconnekt.app.legacygraph.RelevanceConnector
import com.paperconnekt.app.legacygraph.RelevanceCriterion
import com.paperconnekt.app.legacygraph.RelevanceField
import com.paperconnekt.app.legacygraph.RelevanceProfile
import com.paperconnekt.app.legacygraph.RelevanceAiState
import kotlin.random.Random

internal fun newRelevanceCriterion(
    field: RelevanceField = RelevanceField.KEYWORDS,
    connector: RelevanceConnector = RelevanceConnector.AND
) = RelevanceCriterion(Random.nextLong().toULong().toString(36), field, "", connector)

/** An empty target starts with one row so the structure is obvious. */
internal fun RelevanceProfile.withStarterCriterion(): RelevanceProfile =
    if (criteria.isEmpty()) copy(criteria = listOf(newRelevanceCriterion(RelevanceField.DOMAIN))) else this

internal fun RelevanceProfile.withoutBlankCriteria(): RelevanceProfile =
    copy(goal = goal.trim(), criteria = criteria.filter { it.value.isNotBlank() }.map { it.copy(value = it.value.trim()) })

/**
 * Structured research target (goal + facets joined by AND / OR / AND NOT). GPT-6 Luna scores each
 * paper from 0 % to 100 % against it; only papers without a cached score for this target are sent.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun RelevanceTargetPanel(
    draft: RelevanceProfile,
    onDraftChange: (RelevanceProfile) -> Unit,
    relevance: RelevanceAiState,
    visibleIds: List<String>,
    onSave: (evaluate: Boolean) -> Unit,
    onEvaluate: () -> Unit,
    onStop: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier,
    graphWide: Boolean = false,
    connectionsOnly: Boolean = false,
    evaluationEnabled: Boolean = true
) {
    val dirty = draft.fingerprint() != relevance.profile.fingerprint()
    val pending = relevance.pendingIds(visibleIds).size
    val scored = visibleIds.distinct().size - pending
    fun update(id: String, transform: (RelevanceCriterion) -> RelevanceCriterion) =
        onDraftChange(draft.copy(criteria = draft.criteria.map { if (it.id == id) transform(it) else it }))

    Card(modifier.testTag("relevance-target"), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.AutoAwesome, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                Text("Research target", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primaryContainer) {
                    Text("GPT-6 Luna", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                }
            }
            Text(
                "Describe what you are looking for. The AI rates every paper from 0 % (unrelated) to 100 % (fully compatible).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = draft.goal,
                onValueChange = { onDraftChange(draft.copy(goal = it)) },
                label = { Text("Goal (optional)") },
                placeholder = { Text("e.g. Short-term load forecasting for smart grids") },
                minLines = 2,
                maxLines = 4,
                textStyle = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth()
            )

            Text("Criteria", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            draft.criteria.forEachIndexed { index, criterion ->
                CriterionCard(
                    criterion = criterion,
                    isFirst = index == 0,
                    onChange = { changed -> update(criterion.id) { changed } },
                    onRemove = { onDraftChange(draft.copy(criteria = draft.criteria.filter { it.id != criterion.id })) }
                )
            }
            TextButton(
                onClick = { onDraftChange(draft.copy(criteria = draft.criteria + newRelevanceCriterion())) },
                modifier = Modifier.interactivePointer()
            ) {
                Icon(Icons.Default.Add, null, Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Add criterion")
            }

            draft.expression().takeIf { it.isNotEmpty() }?.let { expression ->
                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceContainerHighest) {
                    Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Logic", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(expression, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                    }
                }
            }

            HorizontalDivider()

            when {
                !relevance.isAvailable -> Text("AI scoring is unavailable. Configure an OpenAI API key in settings.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                relevance.isEvaluating -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Evaluating ${relevance.evaluated}/${relevance.total}" +
                                if (relevance.failed > 0) " · ${relevance.failed} failed" else "",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onStop, modifier = Modifier.interactivePointer()) {
                            Icon(Icons.Default.Stop, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Stop")
                        }
                    }
                    LinearProgressIndicator(
                        progress = { if (relevance.total == 0) 0f else relevance.evaluated.toFloat() / relevance.total },
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(2.dp))
                    )
                    Text(if (graphWide || connectionsOnly) "Up to 8 papers in parallel. Review AI suggestions when evaluation finishes."
                        else "Up to 8 papers in parallel. Scores appear on the map as they arrive.",
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                dirty -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = { onSave(true) },
                        enabled = !draft.isEmpty && evaluationEnabled,
                        modifier = Modifier.weight(1f).interactivePointer(enabled = !draft.isEmpty && evaluationEnabled)
                    ) {
                        Icon(Icons.Default.AutoAwesome, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Save & evaluate")
                    }
                    OutlinedButton(onClick = { onSave(false) }, modifier = Modifier.interactivePointer()) { Text("Save") }
                }
                else -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Button(
                        onClick = onEvaluate,
                        enabled = pending > 0 && !relevance.profile.isEmpty && evaluationEnabled,
                        modifier = Modifier.fillMaxWidth().interactivePointer(enabled = pending > 0 && !relevance.profile.isEmpty && evaluationEnabled)
                    ) {
                        Icon(if (pending > 0) Icons.Default.AutoAwesome else Icons.Default.Check, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(when {
                            relevance.profile.isEmpty -> "Add a criterion to evaluate"
                            pending > 0 -> "Evaluate $pending new paper${if (pending == 1) "" else "s"}"
                            connectionsOnly -> "All connections evaluated"
                            graphWide -> "All graph papers evaluated"
                            else -> "All papers in view evaluated"
                        })
                    }
                    if (scored > 0) Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("$scored of ${visibleIds.distinct().size} already scored for this target",
                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f))
                        TextButton(onClick = onReset, modifier = Modifier.interactivePointer()) {
                            Text("Re-score all", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
            relevance.error?.let {
                Text(
                    (if (relevance.failed > 0) "${relevance.failed} paper(s) failed: " else "") + it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Text(
                "Saved on this device. Only papers without a score for this exact target are sent to the AI; " +
                    "editing the target starts a new score set (the last 5 targets stay cached).",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CriterionCard(
    criterion: RelevanceCriterion,
    isFirst: Boolean,
    onChange: (RelevanceCriterion) -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ConnectorPill(criterion.connector, isFirst) { onChange(criterion.copy(connector = it)) }
                FieldPicker(criterion.field) { onChange(criterion.copy(field = it)) }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onRemove, modifier = Modifier.size(28.dp).interactivePointer()) {
                    Icon(Icons.Default.Close, "Remove criterion", Modifier.size(16.dp))
                }
            }
            OutlinedTextField(
                value = criterion.value,
                onValueChange = { onChange(criterion.copy(value = it)) },
                placeholder = { Text(criterion.field.placeholder, style = MaterialTheme.typography.bodySmall) },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth()
            )
            if (criterion.field.suggestions.isNotEmpty()) FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                criterion.field.suggestions.forEach { suggestion ->
                    val selected = criterion.value.trim().equals(suggestion, ignoreCase = true)
                    FilterChip(
                        selected = selected,
                        onClick = { onChange(criterion.copy(value = if (selected) "" else suggestion)) },
                        label = { Text(suggestion, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.height(26.dp).clipChipInteraction().interactivePointer()
                    )
                }
            }
        }
    }
}

/** The first criterion can only be included or negated; later ones join with AND, OR or AND NOT. */
@Composable
private fun ConnectorPill(connector: RelevanceConnector, isFirst: Boolean, onChange: (RelevanceConnector) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    fun labelOf(value: RelevanceConnector) = if (isFirst) {
        if (value == RelevanceConnector.AND_NOT) "NOT" else "WHERE"
    } else value.label
    val options = if (isFirst) listOf(RelevanceConnector.AND, RelevanceConnector.AND_NOT) else RelevanceConnector.entries
    val shown = if (isFirst && connector == RelevanceConnector.OR) RelevanceConnector.AND else connector
    val negated = shown == RelevanceConnector.AND_NOT
    Box {
        Surface(
            onClick = { expanded = true },
            shape = RoundedCornerShape(6.dp),
            color = if (negated) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.tertiaryContainer,
            modifier = Modifier.interactivePointer()
        ) {
            Row(Modifier.padding(start = 8.dp, end = 2.dp, top = 3.dp, bottom = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    labelOf(shown),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (negated) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onTertiaryContainer
                )
                Icon(Icons.Default.ArrowDropDown, null, Modifier.size(16.dp))
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(labelOf(option)) },
                    onClick = { onChange(option); expanded = false },
                    modifier = Modifier.interactivePointer()
                )
            }
        }
    }
}

@Composable
private fun FieldPicker(field: RelevanceField, onChange: (RelevanceField) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Surface(
            onClick = { expanded = true },
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.interactivePointer()
        ) {
            Row(Modifier.padding(start = 8.dp, end = 2.dp, top = 3.dp, bottom = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(field.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                Icon(Icons.Default.ArrowDropDown, null, Modifier.size(16.dp))
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            RelevanceField.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = { onChange(option); expanded = false },
                    modifier = Modifier.interactivePointer()
                )
            }
        }
    }
}
