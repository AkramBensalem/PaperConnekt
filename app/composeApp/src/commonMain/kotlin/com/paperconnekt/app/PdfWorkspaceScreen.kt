package com.paperconnekt.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.paperconnekt.app.resources.Res
import com.paperconnekt.app.resources.arrow_back
import com.paperconnekt.domain.model.Paper
import org.jetbrains.compose.resources.painterResource

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun PdfWorkspaceScreen(
    paper: Paper,
    paddingValues: PaddingValues,
    onBack: () -> Unit,
) {
    var page by remember { mutableStateOf(1) }
    var zoom by remember { mutableStateOf(1f) }
    var isAnnotating by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }
    var ideas by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
    ) {
        TopAppBar(
            title = {
                Column {
                    Text(paper.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "PDF workspace · local reading mode",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        painter = painterResource(Res.drawable.arrow_back),
                        contentDescription = "Back",
                    )
                }
            },
            actions = {
                FilterChip(
                    selected = isAnnotating,
                    onClick = { isAnnotating = !isAnnotating },
                    label = { Text(if (isAnnotating) "Annotating" else "Annotate") },
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = { if (page > 1) page-- }) { Text("Previous") }
                Text("Page $page", style = MaterialTheme.typography.labelLarge)
                TextButton(onClick = { page++ }) { Text("Next") }
                Spacer(Modifier.weight(1f))
                Text("Zoom", style = MaterialTheme.typography.labelMedium)
                Slider(
                    modifier = Modifier.width(140.dp),
                    value = zoom,
                    onValueChange = { zoom = it },
                    valueRange = 0.75f..1.5f,
                )
            }
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                PdfReaderPlaceholder(
                    modifier = Modifier.weight(1.7f),
                    paper = paper,
                    page = page,
                    zoom = zoom,
                    isAnnotating = isAnnotating,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    WorkspacePanel(
                        title = "Notes",
                        value = notes,
                        onValueChange = { notes = it },
                        placeholder = "Capture a quote, summary, or open question…",
                    )
                    WorkspacePanel(
                        title = "Ideas",
                        value = ideas,
                        onValueChange = { ideas = it },
                        placeholder = "Turn this reading into a next step…",
                    )
                }
            }
        }
    }
}

@Composable
private fun PdfReaderPlaceholder(
    modifier: Modifier,
    paper: Paper,
    page: Int,
    zoom: Float,
    isAnnotating: Boolean,
) {
    Card(
        modifier = modifier.fillMaxSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("PDF reader", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Page $page · ${(zoom * 100).toInt()}% · ${if (isAnnotating) "annotation mode" else "reading mode"}",
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                paper.pdfPath ?: "No local PDF path configured.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.padding(8.dp))
            Text(
                "The shared workspace reserves the reader surface for a native PDF implementation while keeping metadata, notes, ideas, and controls available on every platform.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun WorkspacePanel(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = value,
                onValueChange = onValueChange,
                placeholder = { Text(placeholder) },
                minLines = 5,
            )
        }
    }
}
