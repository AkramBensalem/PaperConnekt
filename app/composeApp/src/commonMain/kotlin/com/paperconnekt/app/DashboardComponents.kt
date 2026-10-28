package com.paperconnekt.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.paperconnekt.domain.model.Paper

@Composable
fun DashboardHeader(
    paperCount: Int,
    includeRemote: Boolean,
    onRefresh: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Good to see you, researcher.", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Your local research workspace is ready for the next idea.",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    "$paperCount saved paper${if (paperCount == 1) "" else "s"} · " +
                        if (includeRemote) "online discovery enabled" else "local-first mode",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(16.dp))
            AssistChip(onClick = onRefresh, label = { Text("Refresh local view") })
        }
    }
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
    compact: Boolean,
) {
    val metrics = listOf(
        "Saved papers" to paperCount,
        "Researchers" to authorCount,
        "Connected graphs" to graphCount,
        "Filed entries" to libraryCount,
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Research insights", style = MaterialTheme.typography.titleMedium)
        if (compact) {
            metrics.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    row.forEach { (label, value) ->
                        MetricCard(label, value, Modifier.weight(1f))
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                metrics.forEach { (label, value) ->
                    MetricCard(label, value, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: Int,
    modifier: Modifier,
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(value.toString(), style = MaterialTheme.typography.headlineSmall)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

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
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(titleValue, onTitleChange, label = { Text("Title") }, singleLine = true)
                OutlinedTextField(authorsValue, onAuthorsChange, label = { Text("Authors, comma separated") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        modifier = Modifier.weight(1f),
                        value = yearValue,
                        onValueChange = onYearChange,
                        label = { Text("Year") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        modifier = Modifier.weight(2f),
                        value = doiValue,
                        onValueChange = onDoiChange,
                        label = { Text("DOI") },
                        singleLine = true,
                    )
                }
                OutlinedTextField(tagsValue, onTagsChange, label = { Text("Tags, comma separated") }, singleLine = true)
                OutlinedTextField(
                    abstractValue,
                    onAbstractChange,
                    label = { Text("Abstract (optional)") },
                    minLines = 3,
                )
                OutlinedTextField(pdfPathValue, onPdfPathChange, label = { Text("PDF path (optional)") }, singleLine = true)
            }
        },
        confirmButton = {
            Button(enabled = enabled, onClick = onConfirm) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
