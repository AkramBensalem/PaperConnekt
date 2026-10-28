package com.paperconnekt.app.legacygraph.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paperconnekt.app.legacygraph.ConnectionType
import com.paperconnekt.app.legacygraph.GraphNodeSortOrder
import com.paperconnekt.app.legacygraph.PaperNode
import com.paperconnekt.app.legacygraph.PaperTriageResult
import com.paperconnekt.app.legacygraph.clipChipInteraction
import com.paperconnekt.app.legacygraph.interactivePointer
import com.paperconnekt.storage.GraphExpansionDirection

private const val REFERENCES_PAGE_SIZE = 5

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PaperSidebar(
    node: PaperNode,
    onClose: () -> Unit,
    onExpand: (GraphExpansionDirection) -> Unit,
    onRefresh: () -> Unit,
    isRefreshing: Boolean = false,
    onLoadConnections: () -> Unit = {},
    isLoadingConnections: Boolean = false,
    onHelp: () -> Unit = {},
    triageInstruction: String = "",
    onTriageInstructionChanged: (String) -> Unit = {},
    triageResults: List<PaperTriageResult> = emptyList(),
    isTriaging: Boolean = false,
    triageError: String? = null,
    onApplyTriage: () -> Unit = {},
    modifier: Modifier = Modifier,
    citedInThisPaper: Int = node.referenceCount,
    citingThisPaper: Int = node.citingCount,
    similarity: Float? = node.similarity,
    isDisqualified: Boolean = false,
    onToggleDisqualified: () -> Unit = {},
    citedPapers: List<PaperNode> = emptyList(),
    archivedPapers: List<PaperNode> = emptyList(),
    nodeSortOrder: GraphNodeSortOrder = GraphNodeSortOrder.DEFAULT,
    onNodeSortOrderChanged: (GraphNodeSortOrder) -> Unit = {},
    onAddCitedPaper: (PaperNode) -> Unit = {},
    onArchiveCitedPaper: (PaperNode) -> Unit = {},
    onRestorePaper: (PaperNode) -> Unit = {},
    onUseAsSeed: (() -> Unit)? = null,
    canUseAsSeed: Boolean = true
) {
    val paper = node.paper
    var referencesTab by remember(node.paper.id) { mutableStateOf(false) }
    var referencePage by remember(node.paper.id) { mutableIntStateOf(0) }
    var referenceSort by remember(node.paper.id) { mutableStateOf(ReferenceSortOrder.YEAR) }
    val visibleReferences = if (referencesTab) archivedPapers else citedPapers
    val sortedReferences = when (referenceSort) {
        ReferenceSortOrder.TITLE -> visibleReferences.sortedBy { it.paper.title.lowercase() }
        ReferenceSortOrder.YEAR -> visibleReferences.sortedByDescending { it.paper.year?.toString()?.toIntOrNull() ?: 0 }
        ReferenceSortOrder.SIMILARITY -> visibleReferences.sortedByDescending { it.similarity ?: 0f }
        ReferenceSortOrder.CITATIONS -> visibleReferences.sortedByDescending { it.citationCount }
    }
    val pageCount = ((sortedReferences.size + REFERENCES_PAGE_SIZE - 1) / REFERENCES_PAGE_SIZE).coerceAtLeast(1)
    val currentPage = referencePage.coerceIn(0, pageCount - 1)
    val pageReferences = sortedReferences.drop(currentPage * REFERENCES_PAGE_SIZE).take(REFERENCES_PAGE_SIZE)

    Surface(
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        )
        {
            Spacer(modifier = Modifier.height(12.dp))
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Connection role tag
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when {
                        isDisqualified -> MaterialTheme.colorScheme.errorContainer
                        node.connectionType == ConnectionType.CENTRAL -> MaterialTheme.colorScheme.primaryContainer
                        node.connectionType == ConnectionType.CITED_BY_CENTRAL -> Color(0xFFCCFBF1)
                        else -> Color(0xFFFEF3C7)
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when {
                                isDisqualified -> Icons.Default.Block
                                node.connectionType == ConnectionType.CENTRAL -> Icons.Default.Stars
                                node.connectionType == ConnectionType.CITED_BY_CENTRAL -> Icons.AutoMirrored.Filled.ArrowBack
                                else -> Icons.AutoMirrored.Filled.ArrowForward
                            },
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = when {
                                isDisqualified -> MaterialTheme.colorScheme.error
                                node.connectionType == ConnectionType.CENTRAL -> MaterialTheme.colorScheme.onPrimaryContainer
                                node.connectionType == ConnectionType.CITED_BY_CENTRAL -> Color(0xFF0F766E)
                                else -> Color(0xFFB45309)
                            }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when {
                                isDisqualified -> "Disqualified"
                                node.connectionType == ConnectionType.CENTRAL -> "Seed Paper"
                                node.connectionType == ConnectionType.CITED_BY_CENTRAL -> "Reference"
                                else -> "Citation"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isDisqualified -> MaterialTheme.colorScheme.onErrorContainer
                                node.connectionType == ConnectionType.CENTRAL -> MaterialTheme.colorScheme.onPrimaryContainer
                                node.connectionType == ConnectionType.CITED_BY_CENTRAL -> Color(0xFF0F766E)
                                else -> Color(0xFFB45309)
                            }
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onRefresh,
                        enabled = !isRefreshing,
                        modifier = Modifier.size(32.dp).interactivePointer(enabled = !isRefreshing)
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh paper information", modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(32.dp).interactivePointer()
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close details panel", modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Paper Title & Core Metadata
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = paper.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                        lineHeight = 22.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    CopyIconButton(textToCopy = paper.title, contentDescription = "Copy paper title")
                }

                if (paper.authors.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = paper.authors.joinToString(", "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        CopyIconButton(
                            textToCopy = paper.authors.joinToString(", "),
                            contentDescription = "Copy authors",
                            size = 28.dp,
                            iconSize = 14.dp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Badges Row
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    itemVerticalAlignment = Alignment.CenterVertically,
                ) {
                    paper.year?.toString()?.let { year ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "📅 $year",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    paper.source?.let { venue ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "🏛 $venue",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "⭐ ${node.citationCount} cit.",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    similarity?.let { sim ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = "🎯 ${(sim * 100).toInt()}% sim",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            // Graph Expansion & Connection Actions
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Graph Actions",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onExpand(GraphExpansionDirection.CITATIONS) },
                            modifier = Modifier.weight(1f).interactivePointer(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Citations ($citingThisPaper)", style = MaterialTheme.typography.labelSmall)
                        }
                        OutlinedButton(
                            onClick = { onExpand(GraphExpansionDirection.REFERENCES) },
                            modifier = Modifier.weight(1f).interactivePointer(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("References ($citedInThisPaper)", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    FilledTonalButton(
                        onClick = onLoadConnections,
                        enabled = !isLoadingConnections,
                        modifier = Modifier.fillMaxWidth().interactivePointer(enabled = !isLoadingConnections),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isLoadingConnections) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Discovering connections…", style = MaterialTheme.typography.labelSmall)
                        } else {
                            Icon(Icons.Default.AccountTree, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Find Connections with Graph", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    if (node.connectionType != ConnectionType.CENTRAL) {
                        Spacer(modifier = Modifier.height(8.dp))
                        onUseAsSeed?.let { useAsSeed ->
                            OutlinedButton(
                                onClick = useAsSeed,
                                enabled = canUseAsSeed,
                                modifier = Modifier.fillMaxWidth().interactivePointer(enabled = canUseAsSeed),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Stars, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Use as seed paper")
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                        if (isDisqualified) {
                            FilledTonalButton(
                                onClick = onToggleDisqualified,
                                modifier = Modifier.fillMaxWidth().interactivePointer(),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Color(0xFFCCFBF1),
                                    contentColor = Color(0xFF0F766E)
                                )
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Restore Paper to Graph", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            OutlinedButton(
                                onClick = onToggleDisqualified,
                                modifier = Modifier.fillMaxWidth().interactivePointer(),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Disqualify from Graph", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            // AI Triage Component (Clean statistics summary card with interactive dialog!)
            key(paper.id) {
                AiTriageSection(
                    instruction = triageInstruction,
                    onInstructionChanged = onTriageInstructionChanged,
                    onRunTriage = onHelp,
                    isTriaging = isTriaging,
                    triageResults = triageResults,
                    triageError = triageError,
                    onApplyDisqualifications = onApplyTriage
                )
            }

            // Abstract Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Abstract",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (!paper.abstractText.isNullOrBlank()) {
                            CopyIconButton(
                                textToCopy = paper.abstractText ?: "",
                                contentDescription = "Copy abstract",
                                size = 28.dp,
                                iconSize = 14.dp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = paper.abstractText?.takeIf { it.isNotBlank() } ?: "No abstract available for this publication.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 20.sp
                    )
                }
            }

            // References & Citations
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "References & Archive",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !referencesTab,
                            onClick = { referencesTab = false; referencePage = 0 },
                            label = { Text("Active (${citedPapers.size})") },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ), modifier = Modifier.clipChipInteraction().interactivePointer()
                        )
                        FilterChip(
                            selected = referencesTab,
                            onClick = { referencesTab = true; referencePage = 0 },
                            label = { Text("Archived (${archivedPapers.size})") },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ), modifier = Modifier.clipChipInteraction().interactivePointer()
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NodeSortMenu(nodeSortOrder, onNodeSortOrderChanged)
                        SortMenu(referenceSort) { referenceSort = it; referencePage = 0 }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (pageReferences.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (referencesTab) "No archived references." else "No references loaded in graph.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            pageReferences.forEach { reference ->
                                ReferenceRow(
                                    reference = reference,
                                    onAdd = { if (referencesTab) onRestorePaper(reference) else onAddCitedPaper(reference) },
                                    onArchive = { if (!referencesTab) onArchiveCitedPaper(reference) },
                                    restore = referencesTab
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Page ${currentPage + 1} of $pageCount", style = MaterialTheme.typography.labelSmall)
                                Row {
                                    TextButton(onClick = { referencePage-- }, enabled = currentPage > 0, modifier = Modifier.interactivePointer(enabled = currentPage > 0)) { Text("Prev") }
                                    TextButton(onClick = { referencePage++ }, enabled = currentPage < pageCount - 1, modifier = Modifier.interactivePointer(enabled = currentPage < pageCount - 1)) { Text("Next") }
                                }
                            }
                        }
                    }
                }
            }

            // Keywords
            if (paper.tags.isNotEmpty()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Keywords",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        paper.tags.forEach { keyword ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = keyword,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Citations Formatted Section
            CitationSection(title = "APA Citation", content = generateAPA(node))
            CitationSection(title = "BibTeX", content = generateBibTeX(node))

            // Metadata & Statistics Table
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Paper Statistics",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    StatRow("Direct References", citedInThisPaper.toString())
                    StatRow("Papers Citing This", citingThisPaper.toString())
                    similarity?.let { StatRow("Similarity to Seed", "${(it * 100).coerceIn(0f, 100f).toInt()}%") }
                    StatRow("Publication Venue", paper.source ?: "Unknown")
                    paper.doi?.let { StatRow("DOI", it) }
                    paper.sourceUrl?.let { StatRow("URL", it) }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

    }
}

@Composable
private fun CitationSection(title: String, content: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                CopyIconButton(textToCopy = content, contentDescription = "Copy $title")
            }
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(10.dp),
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ReferenceRow(
    reference: PaperNode,
    onAdd: () -> Unit,
    onArchive: () -> Unit = {},
    restore: Boolean = false
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(start = 10.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reference.paper.title,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${reference.paper.year?.toString() ?: "Year unknown"} · ${reference.citationCount} citations",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onAdd, modifier = Modifier.size(28.dp).interactivePointer()) {
                Icon(
                    imageVector = if (restore) Icons.Default.Unarchive else Icons.Default.Add,
                    contentDescription = if (restore) "Restore paper" else "Add paper to graph",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            if (!restore) {
                IconButton(onClick = onArchive, modifier = Modifier.size(28.dp).interactivePointer()) {
                    Icon(
                        imageVector = Icons.Default.Archive,
                        contentDescription = "Archive reference",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private enum class ReferenceSortOrder { TITLE, YEAR, SIMILARITY, CITATIONS }

@Composable
private fun NodeSortMenu(order: GraphNodeSortOrder, onOrderChanged: (GraphNodeSortOrder) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(
            onClick = { expanded = true },
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp), modifier = Modifier.interactivePointer()
        ) {
            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(order.label(), style = MaterialTheme.typography.labelSmall)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DisableSelection {
                GraphNodeSortOrder.entries.forEach { candidate ->
                    DropdownMenuItem(
                        text = { Text(candidate.label()) },
                        onClick = { onOrderChanged(candidate); expanded = false }, modifier = Modifier.interactivePointer()
                    )
                }
            }
        }
    }
}

private fun GraphNodeSortOrder.label(): String = when (this) {
    GraphNodeSortOrder.DEFAULT -> "Graph order"
    GraphNodeSortOrder.YEAR -> "Publication year"
    GraphNodeSortOrder.SIMILARITY -> "Similarity"
    GraphNodeSortOrder.CITATIONS -> "Citations"
}

@Composable
private fun SortMenu(order: ReferenceSortOrder, onOrderChanged: (ReferenceSortOrder) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(
            onClick = { expanded = true },
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp), modifier = Modifier.interactivePointer()
        ) {
            Text("Sort: ${order.name.lowercase().replaceFirstChar { it.uppercase() }}", style = MaterialTheme.typography.labelSmall)
            Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(14.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DisableSelection {
                ReferenceSortOrder.entries.forEach { candidate ->
                    DropdownMenuItem(
                        text = { Text(candidate.name.lowercase().replaceFirstChar { it.uppercase() }) },
                        onClick = { onOrderChanged(candidate); expanded = false }, modifier = Modifier.interactivePointer()
                    )
                }
            }
        }
    }
}

private fun generateAPA(node: PaperNode): String {
    val authors = if (node.paper.authors.isNotEmpty()) node.paper.authors.joinToString(", ") else "Unknown Author"
    val year = node.paper.year?.toString() ?: "n.d."
    val venue = node.paper.source ?: "Journal"
    return "$authors ($year). ${node.paper.title}. $venue."
}

private fun generateBibTeX(node: PaperNode): String {
    val id = node.paper.id.replace(Regex("[^a-zA-Z0-9]"), "").take(12)
    val year = node.paper.year?.toString() ?: "2024"
    val author = if (node.paper.authors.isNotEmpty()) node.paper.authors.joinToString(" and ") else "Unknown"
    val venue = node.paper.source ?: "Journal"
    return """
@article{$id$year,
  title={${node.paper.title}},
  author={$author},
  journal={$venue},
  year={$year}
}
    """.trimIndent()
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    maxItemsInEachRow: Int = Int.MAX_VALUE,
    content: @Composable RowScope.() -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement,
        maxItemsInEachRow = maxItemsInEachRow,
        content = content
    )
}
