package com.paperconnekt.app.legacygraph.components

import com.paperconnekt.app.resources.Res
import org.jetbrains.compose.resources.stringResource
import com.paperconnekt.app.resources.filled_account_tree
import com.paperconnekt.app.resources.filled_add
import com.paperconnekt.app.resources.filled_archive
import com.paperconnekt.app.resources.filled_arrow_back
import com.paperconnekt.app.resources.filled_arrow_drop_down
import com.paperconnekt.app.resources.filled_arrow_forward
import com.paperconnekt.app.resources.filled_block
import com.paperconnekt.app.resources.filled_check
import com.paperconnekt.app.resources.filled_close
import com.paperconnekt.app.resources.filled_expand_less
import com.paperconnekt.app.resources.filled_expand_more
import com.paperconnekt.app.resources.filled_refresh
import com.paperconnekt.app.resources.filled_sort
import com.paperconnekt.app.resources.filled_stars
import com.paperconnekt.app.resources.filled_tune
import com.paperconnekt.app.resources.filled_unarchive
import org.jetbrains.compose.resources.painterResource
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
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
import com.paperconnekt.app.resources.abstract_label
import com.paperconnekt.app.resources.active_count
import com.paperconnekt.app.resources.add_paper_to_graph
import com.paperconnekt.app.resources.apa_citation
import com.paperconnekt.app.resources.archive_reference
import com.paperconnekt.app.resources.archived_count
import com.paperconnekt.app.resources.bibtex
import com.paperconnekt.app.resources.citation
import com.paperconnekt.app.resources.citation_count
import com.paperconnekt.app.resources.citations
import com.paperconnekt.app.resources.close_details_panel
import com.paperconnekt.app.resources.collapse_references
import com.paperconnekt.app.resources.connections_discovered
import com.paperconnekt.app.resources.copy_abstract
import com.paperconnekt.app.resources.copy_authors
import com.paperconnekt.app.resources.copy_item
import com.paperconnekt.app.resources.copy_paper_title
import com.paperconnekt.app.resources.direct_references
import com.paperconnekt.app.resources.discovering_connections
import com.paperconnekt.app.resources.disqualified
import com.paperconnekt.app.resources.disqualify_from_graph
import com.paperconnekt.app.resources.expand_references
import com.paperconnekt.app.resources.find_connections_with_graph
import com.paperconnekt.app.resources.graph_order
import com.paperconnekt.app.resources.keywords
import com.paperconnekt.app.resources.next
import com.paperconnekt.app.resources.no_abstract_available
import com.paperconnekt.app.resources.no_archived_references
import com.paperconnekt.app.resources.no_references_loaded
import com.paperconnekt.app.resources.page_of
import com.paperconnekt.app.resources.paper_statistics
import com.paperconnekt.app.resources.papers_citing_this
import com.paperconnekt.app.resources.previous_page
import com.paperconnekt.app.resources.publication_venue
import com.paperconnekt.app.resources.publication_year
import com.paperconnekt.app.resources.reference
import com.paperconnekt.app.resources.reference_count
import com.paperconnekt.app.resources.references
import com.paperconnekt.app.resources.references_archive
import com.paperconnekt.app.resources.refresh_paper_information
import com.paperconnekt.app.resources.restore_paper
import com.paperconnekt.app.resources.restore_paper_to_graph
import com.paperconnekt.app.resources.review_connections
import com.paperconnekt.app.resources.save_to_library
import com.paperconnekt.app.resources.saved_in_library
import com.paperconnekt.app.resources.seed_paper
import com.paperconnekt.app.resources.show_less
import com.paperconnekt.app.resources.show_more
import com.paperconnekt.app.resources.similarity
import com.paperconnekt.app.resources.similarity_to_seed
import com.paperconnekt.app.resources.sort_label
import com.paperconnekt.app.resources.toggle_item
import com.paperconnekt.app.resources.toggle_paper_statistics
import com.paperconnekt.app.resources.unknown
import com.paperconnekt.app.resources.use_as_seed_paper
import com.paperconnekt.app.resources.year_unknown

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
    connectionProgress: Float = 0f,
    connectionDiscoveryComplete: Boolean = false,
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
    canUseAsSeed: Boolean = true,
    showReferences: Boolean = true,
    showCitations: Boolean = true,
    onToggleReferences: () -> Unit = {},
    onToggleCitations: () -> Unit = {},
    onOpenReview: () -> Unit = {},
    isSavedToLibrary: Boolean = false,
    onSaveToLibrary: (() -> Unit)? = null,
) {
    val paper = node.paper
    var referencesTab by remember(node.paper.id) { mutableStateOf(false) }
    var referencePage by remember(node.paper.id) { mutableIntStateOf(0) }
    var referenceSort by remember(node.paper.id) { mutableStateOf(ReferenceSortOrder.YEAR) }
    var abstractExpanded by remember(node.paper.id) { mutableStateOf(false) }
    var referencesExpanded by remember(node.paper.id) { mutableStateOf(true) }
    var citationsExpanded by remember(node.paper.id) { mutableStateOf(false) }
    var statisticsExpanded by remember(node.paper.id) { mutableStateOf(true) }
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
        SelectionContainer {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        )
        {
            Spacer(modifier = Modifier.height(12.dp))
            if (onSaveToLibrary != null) {
                OutlinedButton(
                    onClick = onSaveToLibrary,
                    enabled = !isSavedToLibrary,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(if (isSavedToLibrary) Res.string.saved_in_library else Res.string.save_to_library))
                }
            }
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
                            painter = when {
                                isDisqualified -> painterResource(Res.drawable.filled_block)
                                node.connectionType == ConnectionType.CENTRAL -> painterResource(Res.drawable.filled_stars)
                                node.connectionType == ConnectionType.CITED_BY_CENTRAL -> painterResource(Res.drawable.filled_arrow_back)
                                else -> painterResource(Res.drawable.filled_arrow_forward)
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
                                isDisqualified -> stringResource(Res.string.disqualified)
                                node.connectionType == ConnectionType.CENTRAL -> stringResource(Res.string.seed_paper)
                                node.connectionType == ConnectionType.CITED_BY_CENTRAL -> stringResource(Res.string.reference)
                                else -> stringResource(Res.string.citation)
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
                        onClick = onOpenReview,
                        modifier = Modifier.size(32.dp).interactivePointer(),
                    ) {
                        Icon(painterResource(Res.drawable.filled_tune), contentDescription = stringResource(Res.string.review_connections), modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onRefresh,
                        enabled = !isRefreshing,
                        modifier = Modifier.size(32.dp).interactivePointer(enabled = !isRefreshing)
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(painterResource(Res.drawable.filled_refresh), contentDescription = stringResource(Res.string.refresh_paper_information), modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(32.dp).interactivePointer()
                    ) {
                        Icon(painterResource(Res.drawable.filled_close), contentDescription = stringResource(Res.string.close_details_panel), modifier = Modifier.size(18.dp))
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
                    CopyIconButton(textToCopy = paper.title, contentDescription = stringResource(Res.string.copy_paper_title))
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
                            contentDescription = stringResource(Res.string.copy_authors),
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
                            Text(stringResource(Res.string.citation_count, citingThisPaper), style = MaterialTheme.typography.labelSmall)
                        }
                        OutlinedButton(
                            onClick = { onExpand(GraphExpansionDirection.REFERENCES) },
                            modifier = Modifier.weight(1f).interactivePointer(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(Res.string.reference_count, citedInThisPaper), style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    FilledTonalButton(
                        onClick = onLoadConnections,
                        enabled = !isLoadingConnections && !isRefreshing,
                        modifier = Modifier.fillMaxWidth().interactivePointer(enabled = !isLoadingConnections && !isRefreshing),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isLoadingConnections) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                stringResource(Res.string.discovering_connections, (connectionProgress * 100).toInt().coerceIn(0, 100)),
                                style = MaterialTheme.typography.labelSmall,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            LinearProgressIndicator(
                                progress = { connectionProgress.coerceIn(0f, 1f) },
                                modifier = Modifier.weight(1f).height(4.dp),
                            )
                        } else {
                            Icon(painterResource(Res.drawable.filled_account_tree), contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (connectionDiscoveryComplete) stringResource(Res.string.connections_discovered)
                                else stringResource(Res.string.find_connections_with_graph),
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = showReferences,
                            onClick = onToggleReferences,
                            label = { Text(stringResource(Res.string.references)) },
                            modifier = Modifier.weight(1f).interactivePointer(),
                        )
                        FilterChip(
                            selected = showCitations,
                            onClick = onToggleCitations,
                            label = { Text(stringResource(Res.string.citations)) },
                            modifier = Modifier.weight(1f).interactivePointer(),
                        )
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
                                Icon(painterResource(Res.drawable.filled_stars), contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(Res.string.use_as_seed_paper))
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
                                Icon(painterResource(Res.drawable.filled_check), contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(Res.string.restore_paper_to_graph), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            OutlinedButton(
                                onClick = onToggleDisqualified,
                                modifier = Modifier.fillMaxWidth().interactivePointer(),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(painterResource(Res.drawable.filled_block), contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(Res.string.disqualify_from_graph), style = MaterialTheme.typography.labelSmall)
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
                            text = stringResource(Res.string.abstract_label),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (!paper.abstractText.isNullOrBlank()) {
                            CopyIconButton(
                                textToCopy = paper.abstractText ?: "",
                                contentDescription = stringResource(Res.string.copy_abstract),
                                size = 28.dp,
                                iconSize = 14.dp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    val abstract = paper.abstractText?.takeIf { it.isNotBlank() }
                    Text(
                        text = abstract ?: stringResource(Res.string.no_abstract_available),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 20.sp,
                        maxLines = if (abstractExpanded) Int.MAX_VALUE else 5,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.animateContentSize(),
                    )
                    if (abstract != null && abstract.length > 420) {
                        TextButton(
                            onClick = { abstractExpanded = !abstractExpanded },
                            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 2.dp),
                        ) {
                            Text(stringResource(if (abstractExpanded) Res.string.show_less else Res.string.show_more))
                        }
                    }
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
                        TextButton(
                            onClick = { referencesExpanded = !referencesExpanded },
                            contentPadding = PaddingValues(0.dp),
                        ) {
                        Text(
                            text = stringResource(Res.string.references_archive),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        }
                        IconButton(onClick = { referencesExpanded = !referencesExpanded }, modifier = Modifier.size(28.dp)) {
                            Icon(
                                if (referencesExpanded) painterResource(Res.drawable.filled_expand_less) else painterResource(Res.drawable.filled_expand_more),
                                contentDescription = if (referencesExpanded) stringResource(Res.string.collapse_references) else stringResource(Res.string.expand_references),
                            )
                        }
                    }

                    AnimatedVisibility(visible = referencesExpanded) {
                    Column {
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !referencesTab,
                            onClick = { referencesTab = false; referencePage = 0 },
                            label = { Text(stringResource(Res.string.active_count, citedPapers.size)) },
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
                            label = { Text(stringResource(Res.string.archived_count, archivedPapers.size)) },
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
                                text = if (referencesTab) stringResource(Res.string.no_archived_references) else stringResource(Res.string.no_references_loaded),
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
                                Text(stringResource(Res.string.page_of, currentPage + 1, pageCount), style = MaterialTheme.typography.labelSmall)
                                Row {
                                    TextButton(onClick = { referencePage-- }, enabled = currentPage > 0, modifier = Modifier.interactivePointer(enabled = currentPage > 0)) { Text(stringResource(Res.string.previous_page)) }
                                    TextButton(onClick = { referencePage++ }, enabled = currentPage < pageCount - 1, modifier = Modifier.interactivePointer(enabled = currentPage < pageCount - 1)) { Text(stringResource(Res.string.next)) }
                                }
                            }
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
                        text = stringResource(Res.string.keywords),
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
            CollapsibleCitationSection(title = stringResource(Res.string.apa_citation), content = generateAPA(node), initiallyExpanded = false)
            CollapsibleCitationSection(title = stringResource(Res.string.bibtex), content = generateBibTeX(node), initiallyExpanded = false)

            // Metadata & Statistics Table
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { statisticsExpanded = !statisticsExpanded }, contentPadding = PaddingValues(0.dp)) {
                            Text(stringResource(Res.string.paper_statistics), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { statisticsExpanded = !statisticsExpanded }, modifier = Modifier.size(28.dp)) {
                            Icon(if (statisticsExpanded) painterResource(Res.drawable.filled_expand_less) else painterResource(Res.drawable.filled_expand_more), contentDescription = stringResource(Res.string.toggle_paper_statistics))
                        }
                    }
                    AnimatedVisibility(visible = statisticsExpanded) {
                        Column {
                            Spacer(modifier = Modifier.height(8.dp))
                            StatRow(stringResource(Res.string.direct_references), citedInThisPaper.toString())
                            StatRow(stringResource(Res.string.papers_citing_this), citingThisPaper.toString())
                            similarity?.let { StatRow(stringResource(Res.string.similarity_to_seed), "${(it * 100).coerceIn(0f, 100f).toInt()}%") }
                            StatRow(stringResource(Res.string.publication_venue), paper.source ?: stringResource(Res.string.unknown))
                            paper.doi?.let { StatRow("DOI", it, copyable = true) }
                            paper.sourceUrl?.let { StatRow("URL", it, copyable = true) }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

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
                CopyIconButton(textToCopy = content, contentDescription = stringResource(Res.string.copy_item, title))
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
private fun StatRow(label: String, value: String, copyable: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f, fill = false)) {
            Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (copyable) CopyIconButton(textToCopy = value, contentDescription = stringResource(Res.string.copy_item, label), size = 26.dp, iconSize = 13.dp)
        }
    }
}

@Composable
private fun CollapsibleCitationSection(title: String, content: String, initiallyExpanded: Boolean) {
    var expanded by remember(title) { mutableStateOf(initiallyExpanded) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { expanded = !expanded }, contentPadding = PaddingValues(0.dp)) {
                    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CopyIconButton(textToCopy = content, contentDescription = stringResource(Res.string.copy_item, title))
                    IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(28.dp)) {
                        Icon(if (expanded) painterResource(Res.drawable.filled_expand_less) else painterResource(Res.drawable.filled_expand_more), contentDescription = stringResource(Res.string.toggle_item, title))
                    }
                }
            }
            AnimatedVisibility(visible = expanded) {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(content, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(10.dp), fontFamily = FontFamily.Monospace, lineHeight = 18.sp)
                }
            }
        }
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
                    text = "${reference.paper.year?.toString() ?: stringResource(Res.string.year_unknown)} · ${reference.citationCount} ${stringResource(Res.string.citations)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onAdd, modifier = Modifier.size(28.dp).interactivePointer()) {
                Icon(
                    painter = if (restore) painterResource(Res.drawable.filled_unarchive) else painterResource(Res.drawable.filled_add),
                    contentDescription = if (restore) stringResource(Res.string.restore_paper)
                    else stringResource(Res.string.add_paper_to_graph),
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            if (!restore) {
                IconButton(onClick = onArchive, modifier = Modifier.size(28.dp).interactivePointer()) {
                    Icon(
                        painter = painterResource(Res.drawable.filled_archive),
                        contentDescription = stringResource(Res.string.archive_reference),
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
            Icon(painterResource(Res.drawable.filled_sort), contentDescription = null, modifier = Modifier.size(14.dp))
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

@Composable
private fun GraphNodeSortOrder.label(): String = when (this) {
    GraphNodeSortOrder.DEFAULT -> stringResource(Res.string.graph_order)
    GraphNodeSortOrder.YEAR -> stringResource(Res.string.publication_year)
    GraphNodeSortOrder.SIMILARITY -> stringResource(Res.string.similarity)
    GraphNodeSortOrder.CITATIONS -> stringResource(Res.string.citations)
}

@Composable
private fun SortMenu(order: ReferenceSortOrder, onOrderChanged: (ReferenceSortOrder) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(
            onClick = { expanded = true },
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp), modifier = Modifier.interactivePointer()
        ) {
            Text(stringResource(Res.string.sort_label, order.name.lowercase().replaceFirstChar { it.uppercase() }), style = MaterialTheme.typography.labelSmall)
            Icon(painterResource(Res.drawable.filled_arrow_drop_down), contentDescription = null, modifier = Modifier.size(14.dp))
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
