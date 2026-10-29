package com.paperconnekt.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.paperconnekt.app.resources.Res
import com.paperconnekt.app.resources.filled_close
import com.paperconnekt.app.resources.filled_description
import com.paperconnekt.domain.model.Paper
import org.jetbrains.compose.resources.painterResource

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
