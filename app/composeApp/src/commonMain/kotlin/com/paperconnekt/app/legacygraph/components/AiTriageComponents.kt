package com.paperconnekt.app.legacygraph.components

import com.paperconnekt.app.resources.Res
import org.jetbrains.compose.resources.stringResource
import com.paperconnekt.app.resources.filled_auto_awesome
import com.paperconnekt.app.resources.filled_block
import com.paperconnekt.app.resources.filled_check
import com.paperconnekt.app.resources.filled_clear
import com.paperconnekt.app.resources.filled_close
import com.paperconnekt.app.resources.filled_error_outline
import com.paperconnekt.app.resources.filled_keyboard_arrow_down
import com.paperconnekt.app.resources.filled_keyboard_arrow_up
import com.paperconnekt.app.resources.filled_psychology
import com.paperconnekt.app.resources.filled_search
import com.paperconnekt.app.resources.filled_visibility
import org.jetbrains.compose.resources.painterResource
import com.paperconnekt.app.legacygraph.interactivePointer
import com.paperconnekt.app.legacygraph.clipChipInteraction

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.paperconnekt.app.legacygraph.PaperTriageResult
import com.paperconnekt.app.legacygraph.TriageDecision
import com.paperconnekt.app.resources.ai_literature_triage_report
import com.paperconnekt.app.resources.all_count
import com.paperconnekt.app.resources.analyzing_with_ai
import com.paperconnekt.app.resources.apply_disqualifications
import com.paperconnekt.app.resources.apply_disqualifications_count
import com.paperconnekt.app.resources.candidate_papers_evaluated
import com.paperconnekt.app.resources.clear
import com.paperconnekt.app.resources.close
import com.paperconnekt.app.resources.copy_paper_title
import com.paperconnekt.app.resources.criteria_prefix
import com.paperconnekt.app.resources.dismiss
import com.paperconnekt.app.resources.disqualify
import com.paperconnekt.app.resources.disqualify_count
import com.paperconnekt.app.resources.evaluate_relevance
import com.paperconnekt.app.resources.filter_papers_or_rationale
import com.paperconnekt.app.resources.inspect_report
import com.paperconnekt.app.resources.keep
import com.paperconnekt.app.resources.keep_count
import com.paperconnekt.app.resources.reevaluate_relevance
import com.paperconnekt.app.resources.total
import com.paperconnekt.app.resources.triage_criteria_instruction
import com.paperconnekt.app.resources.triage_description
import com.paperconnekt.app.resources.triage_instruction_hint
import com.paperconnekt.app.resources.triage_statistics

private val KeepColor = Color(0xFF0D9488) // Emerald
private val DisqualifyColor = Color(0xFFDC2626) // Crimson / Red
private val DisqualifyContainer = Color(0xFFFEE2E2) // Light red container
private val KeepContainer = Color(0xFFCCFBF1) // Light emerald container

@Composable
fun AiTriageSection(
    instruction: String,
    onInstructionChanged: (String) -> Unit,
    onRunTriage: () -> Unit,
    isTriaging: Boolean,
    triageResults: List<PaperTriageResult>,
    triageError: String?,
    onApplyDisqualifications: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    var expandedInstructions by remember { mutableStateOf(false) }

    val keepCount = remember(triageResults) { triageResults.count { it.decision == TriageDecision.KEEP } }
    val disqualifyCount = remember(triageResults) { triageResults.count { it.decision == TriageDecision.DISQUALIFY } }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painterResource(Res.drawable.filled_auto_awesome),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AI Literature Triage",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                TextButton(
                    onClick = { expandedInstructions = !expandedInstructions },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp), modifier = Modifier.interactivePointer()
                ) {
                    Text(
                        text = if (expandedInstructions) "Hide prompt" else "Prompt",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        if (expandedInstructions) painterResource(Res.drawable.filled_keyboard_arrow_up) else painterResource(Res.drawable.filled_keyboard_arrow_down),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            AnimatedVisibility(visible = expandedInstructions) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    OutlinedTextField(
                        value = instruction,
                        onValueChange = onInstructionChanged,
                        label = { Text(stringResource(Res.string.triage_criteria_instruction), style = MaterialTheme.typography.labelSmall) },
                        placeholder = { Text(stringResource(Res.string.triage_instruction_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4,
                        shape = RoundedCornerShape(10.dp),
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                stringResource(Res.string.triage_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Action / Run Button
            Button(
                onClick = onRunTriage,
                enabled = !isTriaging,
                modifier = Modifier.fillMaxWidth().interactivePointer(enabled = !isTriaging),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (isTriaging) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(Res.string.analyzing_with_ai), style = MaterialTheme.typography.labelMedium)
                } else {
                    Icon(painterResource(Res.drawable.filled_psychology), contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (triageResults.isEmpty()) stringResource(Res.string.evaluate_relevance) else stringResource(Res.string.reevaluate_relevance),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            triageError?.let { err ->
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painterResource(Res.drawable.filled_error_outline),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = err,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // Results Statistics Box (Replaces the long ugly node list)
            if (triageResults.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = stringResource(Res.string.triage_statistics),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatBox(
                                label = stringResource(Res.string.total),
                                value = triageResults.size.toString(),
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            StatBox(
                                label = stringResource(Res.string.keep),
                                value = keepCount.toString(),
                                containerColor = KeepContainer,
                                contentColor = KeepColor,
                                modifier = Modifier.weight(1f)
                            )
                            StatBox(
                                label = stringResource(Res.string.disqualify),
                                value = disqualifyCount.toString(),
                                containerColor = DisqualifyContainer,
                                contentColor = DisqualifyColor,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showDialog = true },
                                modifier = Modifier.weight(1f).interactivePointer(),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Icon(painterResource(Res.drawable.filled_visibility), contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(Res.string.inspect_report), style = MaterialTheme.typography.labelSmall)
                            }

                            if (disqualifyCount > 0) {
                                Button(
                                    onClick = onApplyDisqualifications,
                                    modifier = Modifier.weight(1f).interactivePointer(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = DisqualifyColor
                                    ),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(painterResource(Res.drawable.filled_block), contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(stringResource(Res.string.apply_disqualifications_count, disqualifyCount), style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        AiTriageDialog(
            triageResults = triageResults,
            instruction = instruction,
            onDismiss = { showDialog = false },
            onApply = {
                onApplyDisqualifications()
                showDialog = false
            }
        )
    }
}

@Composable
private fun StatBox(
    label: String,
    value: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = containerColor,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = contentColor)
            Text(label, style = MaterialTheme.typography.labelSmall, color = contentColor.copy(alpha = 0.85f))
        }
    }
}

@Composable
fun AiTriageDialog(
    triageResults: List<PaperTriageResult>,
    instruction: String,
    onDismiss: () -> Unit,
    onApply: () -> Unit
) {
    var filterTab by remember { mutableStateOf(TriageFilterTab.ALL) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredResults = remember(triageResults, filterTab, searchQuery) {
        triageResults.filter { item ->
            val matchesTab = when (filterTab) {
                TriageFilterTab.ALL -> true
                TriageFilterTab.KEEP -> item.decision == TriageDecision.KEEP
                TriageFilterTab.DISQUALIFY -> item.decision == TriageDecision.DISQUALIFY
            }
            val matchesSearch = searchQuery.isBlank() ||
                item.title.contains(searchQuery, ignoreCase = true) ||
                item.rationale.contains(searchQuery, ignoreCase = true)
            matchesTab && matchesSearch
        }
    }

    val keepCount = remember(triageResults) { triageResults.count { it.decision == TriageDecision.KEEP } }
    val disqualifyCount = remember(triageResults) { triageResults.count { it.decision == TriageDecision.DISQUALIFY } }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        // Keep report selection local to the dialog's layout hierarchy.
        SelectionContainer {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
                .widthIn(max = 680.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                // Dialog Title Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painterResource(Res.drawable.filled_auto_awesome),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(Res.string.ai_literature_triage_report),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(Res.string.candidate_papers_evaluated, triageResults.size),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.interactivePointer()) {
                        Icon(painterResource(Res.drawable.filled_close), contentDescription = stringResource(Res.string.close))
                    }
                }

                if (instruction.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(Res.string.criteria_prefix, instruction),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search Box
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(stringResource(Res.string.filter_papers_or_rationale)) },
                    leadingIcon = { Icon(painterResource(Res.drawable.filled_search), contentDescription = null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }, modifier = Modifier.interactivePointer()) {
                                Icon(painterResource(Res.drawable.filled_clear), contentDescription = stringResource(Res.string.clear), modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Filter Tabs (All / Keep / Disqualify)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = filterTab == TriageFilterTab.ALL,
                        onClick = { filterTab = TriageFilterTab.ALL },
                        label = { Text(stringResource(Res.string.all_count, triageResults.size)) }, modifier = Modifier.clipChipInteraction().interactivePointer()
                    )
                    FilterChip(
                        selected = filterTab == TriageFilterTab.KEEP,
                        onClick = { filterTab = TriageFilterTab.KEEP },
                        label = { Text(stringResource(Res.string.keep_count, keepCount)) },
                        leadingIcon = { Icon(painterResource(Res.drawable.filled_check), null, modifier = Modifier.size(14.dp), tint = KeepColor) },
                        modifier = Modifier.clipChipInteraction().interactivePointer()
                    )
                    FilterChip(
                        selected = filterTab == TriageFilterTab.DISQUALIFY,
                        onClick = { filterTab = TriageFilterTab.DISQUALIFY },
                        label = { Text(stringResource(Res.string.disqualify_count, disqualifyCount)) },
                        leadingIcon = { Icon(painterResource(Res.drawable.filled_block), null, modifier = Modifier.size(14.dp), tint = DisqualifyColor) },
                        modifier = Modifier.clipChipInteraction().interactivePointer()
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Paper List
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (filteredResults.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No evaluated papers match this filter.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(filteredResults, key = { it.paperId }) { result ->
                            TriageResultCard(result = result)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$disqualifyCount suggested for removal",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onDismiss, modifier = Modifier.interactivePointer()) {
                            Text(stringResource(Res.string.dismiss))
                        }
                        if (disqualifyCount > 0) {
                            Button(
                                onClick = onApply,
                                colors = ButtonDefaults.buttonColors(containerColor = DisqualifyColor), modifier = Modifier.interactivePointer()
                            ) {
                                Icon(painterResource(Res.drawable.filled_block), contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(Res.string.apply_disqualifications))
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
private fun TriageResultCard(result: PaperTriageResult) {
    val isKeep = result.decision == TriageDecision.KEEP

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            1.dp,
            if (isKeep) KeepColor.copy(alpha = 0.4f) else DisqualifyColor.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isKeep) KeepContainer else DisqualifyContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (isKeep) painterResource(Res.drawable.filled_check) else painterResource(Res.drawable.filled_block),
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = if (isKeep) KeepColor else DisqualifyColor
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isKeep) "KEEP" else "DISQUALIFY",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isKeep) KeepColor else DisqualifyColor
                            )
                        }
                    }
                }

                CopyIconButton(textToCopy = result.title, contentDescription = stringResource(Res.string.copy_paper_title))
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = result.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = result.rationale,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
        }
    }
}

private enum class TriageFilterTab {
    ALL, KEEP, DISQUALIFY
}
