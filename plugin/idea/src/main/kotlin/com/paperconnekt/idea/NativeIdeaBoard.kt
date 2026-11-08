package com.paperconnekt.idea

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.akram.bensalem.paper.connekt.feature.assistant.Idea
import com.akram.bensalem.paper.connekt.feature.assistant.component.ChatMarkdown
import com.mikepenz.markdown.coil3.Coil3ImageTransformerImpl
import org.jetbrains.jewel.ui.component.DefaultButton
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextArea
import org.jetbrains.jewel.ui.component.TextField

/** Same page colors as the app's Idea Board; the accent keeps them readable on dark IDE themes. */
private data class IdeaColor(val key: String, val label: String, val accent: Color)

private val ideaColors = listOf(
    IdeaColor("cream", "Warm ivory", Color(0xFFE0B252)),
    IdeaColor("sage", "Soft sage", Color(0xFF7FAE6E)),
    IdeaColor("sky", "Cloud blue", Color(0xFF5B9BD5)),
    IdeaColor("lavender", "Misty lilac", Color(0xFF9D86D6)),
    IdeaColor("rose", "Blush", Color(0xFFD9817A)),
)

private fun ideaColor(key: String) = ideaColors.firstOrNull { it.key == key } ?: ideaColors.first()

/** Floating Idea Board for the open paper: Markdown notes shared with the app and the AI agent. */
@Composable
internal fun ColumnScope.NativeIdeaBoardPanel(features: NativeFeatures, onClose: () -> Unit) {
    val board = features.assistant
    val state = board.state
    var editing by remember { mutableStateOf<Idea?>(null) }
    var creating by remember { mutableStateOf(false) }
    val filterField = rememberTextFieldState()
    var filter by remember { mutableStateOf("") }
    LaunchedEffect(filterField) { snapshotFlow { filterField.text.toString() }.collect { filter = it } }

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        NativeGlyph(NativeIcon.Idea, tint = ideaColors.first().accent)
        NativeHeading("Idea board", "${state.ideas.size} Markdown note${if (state.ideas.size == 1) "" else "s"} for this paper",
            Modifier.weight(1f))
        NativeToolButton("New idea", NativeIcon.Add, enabled = state.selectedPaper != null && !state.ideasLoading) { creating = true }
        NativeToolButton("Close idea board (Escape)", NativeIcon.Close, onClick = onClose)
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(nativeForeground.copy(alpha = .12f)))
    if (state.ideas.isNotEmpty()) TextField(filterField, Modifier.fillMaxWidth(), placeholder = { Text("Filter ideas") })
    state.ideaError?.let { Text(it, color = Color(0xFFE5534B)) }
    val visible = state.ideas.filter {
        filter.isBlank() || it.title.contains(filter, true) || it.description.contains(filter, true)
    }
    when {
        state.ideasLoading -> NativeEmptyState("Loading ideas...", Modifier.weight(1f).fillMaxWidth())
        state.ideas.isEmpty() -> NativeEmptyState(
            "No ideas yet. Capture research questions, hypotheses or follow-ups as Markdown notes — the AI agent can read and fill them too.",
            Modifier.weight(1f).fillMaxWidth(),
        ) { DefaultButton(onClick = { creating = true }, enabled = state.selectedPaper != null) { Text("New idea") } }
        visible.isEmpty() -> NativeEmptyState("No idea matches \"$filter\".", Modifier.weight(1f).fillMaxWidth())
        else -> LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(visible, key = { it.id }) { idea -> NativeIdeaCard(idea) { editing = idea } }
        }
    }

    if (creating || editing != null) NativeIdeaEditor(
        idea = editing,
        onSave = { title, body, color ->
            val current = editing
            if (current != null) board.updateIdea(current.id, title, body, color)
            else features.createIdea(title, body, color)
            creating = false; editing = null
        },
        onClose = { creating = false; editing = null },
    )
}

@Composable
private fun NativeIdeaCard(idea: Idea, onOpen: () -> Unit) {
    val color = ideaColor(idea.pageColor)
    NativeCard(onClick = onOpen) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.width(4.dp).height(18.dp).background(color.accent, RoundedCornerShape(2.dp)))
            NativeCardTitle(idea.title, Modifier.weight(1f), maxLines = 1)
            NativeGlyph(NativeIcon.Edit, Modifier.size(14.dp), tint = nativeForeground.copy(alpha = .55f))
        }
        val preview = idea.description.lineSequence().map { it.trim().trimStart('#', '>', '-', '*', ' ') }
            .filter(String::isNotBlank).joinToString(" ")
        Text(preview.ifBlank { "Empty note" }, maxLines = 3, overflow = TextOverflow.Ellipsis,
            color = nativeForeground.copy(alpha = if (preview.isBlank()) .5f else .8f))
    }
}

/** Creates or edits one idea with a Markdown editor and live preview, mirroring the app's idea page. */
@Composable
private fun NativeIdeaEditor(idea: Idea?, onSave: (String, String, String) -> Unit, onClose: () -> Unit) {
    val title = rememberTextFieldState(idea?.title.orEmpty())
    val body = rememberTextFieldState(idea?.description.orEmpty())
    var color by remember { mutableStateOf(idea?.pageColor ?: ideaColors.first().key) }
    var preview by remember { mutableStateOf(idea != null && idea.description.isNotBlank()) }
    val accent = ideaColor(color).accent
    NativeDialog(if (idea == null) "New idea" else "Idea", onClose = onClose, maxDialogWidth = 640.dp, maxDialogHeight = 620.dp) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.width(4.dp).height(28.dp).background(accent, RoundedCornerShape(2.dp)))
            TextField(title, Modifier.weight(1f), placeholder = { Text("Idea title") })
            NativeMenu("Page color", ideaColors.map { option ->
                NativeMenuAction(option.label, NativeIcon.Idea, selected = option.key == color, swatch = option.accent) { color = option.key }
            }, icon = NativeIcon.Idea, swatch = accent)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            NativeToolButton("Write", NativeIcon.Edit, selected = !preview, showLabel = true) { preview = false }
            NativeToolButton("Preview", NativeIcon.Markdown, selected = preview, showLabel = true) { preview = true }
            Spacer(Modifier.weight(1f))
            NativeMeta("Markdown supported")
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (preview) SelectionContainer(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                val text = body.text.toString()
                if (text.isBlank()) Text("Nothing to preview yet.", color = nativeForeground.copy(alpha = .5f))
                else ChatMarkdown(text, imageTransformer = Coil3ImageTransformerImpl)
            } else TextArea(body, Modifier.fillMaxSize(),
                placeholder = { Text("Write your idea in Markdown: context, hypothesis, next steps...") })
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
            OutlinedButton(onClick = onClose) { Text("Cancel") }
            DefaultButton(onClick = { onSave(title.text.toString().trim(), body.text.toString(), color) },
                enabled = title.text.isNotBlank()) { Text(if (idea == null) "Create idea" else "Save") }
        }
    }
}
