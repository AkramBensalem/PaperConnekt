package com.akram.bensalem.paper.connekt.feature.assistant.component

import androidx.compose.ui.graphics.painter.Painter

import com.paperconnekt.app.resources.Res
import com.paperconnekt.app.resources.filled_add
import com.paperconnekt.app.resources.filled_close
import com.paperconnekt.app.resources.filled_code
import com.paperconnekt.app.resources.filled_description
import com.paperconnekt.app.resources.filled_format_bold
import com.paperconnekt.app.resources.filled_format_italic
import com.paperconnekt.app.resources.filled_format_list_bulleted
import com.paperconnekt.app.resources.filled_format_list_numbered
import com.paperconnekt.app.resources.filled_format_quote
import com.paperconnekt.app.resources.filled_link
import com.paperconnekt.app.resources.filled_title
import org.jetbrains.compose.resources.painterResource
import com.akram.bensalem.bistosh.core.designsystem.component.interactivePointer

import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akram.bensalem.paper.connekt.feature.assistant.Idea

@Composable
fun IdeaOrganizer(
    ideas: List<Idea>,
    onAddIdea: (String, String) -> Unit,
    onUpdateIdea: (String, String, String, String) -> Unit,
    isLoading: Boolean = false,
    error: String? = null,
    headerCollapsed: Boolean = false,
    openIdeaId: String? = null,
    onIdeaOpened: () -> Unit = {}
) {
    var showDialog by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var editingIdea by remember { mutableStateOf<Idea?>(null) }
    LaunchedEffect(openIdeaId, ideas) {
        ideas.find { it.id == openIdeaId }?.let {
            editingIdea = it
            onIdeaOpened()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(visible = !headerCollapsed) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Idea Board", style = MaterialTheme.typography.titleLarge)

                    Button(
                        enabled = !isLoading,
                        onClick = { showDialog = true },
                        modifier = Modifier.interactivePointer(enabled = !isLoading)
                    ) {
                        Icon(painterResource(Res.drawable.filled_add), contentDescription = "Add")
                        Spacer(Modifier.width(4.dp))
                        Text("New")
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp))
        }

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (ideas.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Create an idea to start a Markdown note for this paper.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 190.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(ideas, key = { it.id }) { idea ->
                    IdeaCard(idea) { editingIdea = idea }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("New Research Idea") },
            text = {
                Column {
                    TextField(
                        value = title,
                        onValueChange = { title = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Idea Title") },
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    TextField(
                        value = description,
                        onValueChange = { description = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Write your idea in Markdown...") },
                        minLines = 4
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (title.isNotBlank()) {
                            onAddIdea(title.trim(), description)
                            title = ""
                            description = ""
                            showDialog = false
                        }
                    }, modifier = Modifier.interactivePointer()
                ) {
                    Text("Create note")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }, modifier = Modifier.interactivePointer()) {
                    Text("Cancel")
                }
            }
        )
    }

    editingIdea?.let { idea ->
        IdeaNoteEditorDialog(
            idea = idea,
            onDismiss = { editingIdea = null },
            onSave = { title, content, pageColor ->
                onUpdateIdea(idea.id, title, content, pageColor)
                editingIdea = null
            }
        )
    }
}

@Composable
fun IdeaCard(idea: Idea, onClick: () -> Unit = {}) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(142.dp)
            .clip(RoundedCornerShape(16.dp)).interactivePointer(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ideaPageColor(idea.pageColor)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.33f))
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(Res.drawable.filled_description), contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${idea.title}.md",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = idea.description.ifBlank { "Empty Markdown note — click to start writing." },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 4
            )
        }
    }
}

@Composable
fun IdeaNoteEditorDialog(
    idea: Idea,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var title by remember(idea.id) { mutableStateOf(idea.title) }
    var content by remember(idea.id) { mutableStateOf(TextFieldValue(idea.description)) }
    var showLivePreview by remember(idea.id) { mutableStateOf(true) }
    var pageColor by remember(idea.id) { mutableStateOf(idea.pageColor) }
    val isDirty = title != idea.title || content.text != idea.description
        || pageColor != idea.pageColor
    val pageBackground = ideaPageColor(pageColor)
    val dialogBackground = Color(0xFFF8F7F3)
    val headerBackground = Color(0xFFF1EFE9)
    val toolbarBackground = Color(0xFFEDEBE5)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.94f).widthIn(max = 1040.dp)
                .fillMaxHeight(0.92f).heightIn(max = 640.dp),
            shape = RoundedCornerShape(24.dp),
            shadowElevation = 24.dp,
            color = dialogBackground,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .background(headerBackground)
                        .padding(start = 28.dp, end = 22.dp, top = 18.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(painterResource(Res.drawable.filled_description), contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            modifier = Modifier.fillMaxWidth().padding(start = 8.dp),
                            placeholder = { Text("Untitled idea") },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = Color.Transparent,
                                focusedBorderColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent,
                                cursorColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(start = 20.dp, top = 2.dp)
                        ) {
                            Text("${title.ifBlank { "Untitled" }}.md",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1)
                            if (isDirty) {
                                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.tertiaryContainer) {
                                    Text("Unsaved changes", Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer)
                                }
                            } else {
                                Text("All changes saved", style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(40.dp).interactivePointer()
                    ) {
                        Icon(painterResource(Res.drawable.filled_close), contentDescription = "Close editor",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 12.dp)
                        .background(toolbarBackground, RoundedCornerShape(14.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MarkdownFormatButton("Bold", painterResource(Res.drawable.filled_format_bold)) {
                        content = content.wrapSelection("**", "**")
                    }
                    MarkdownFormatButton("Italic", painterResource(Res.drawable.filled_format_italic)) {
                        content = content.wrapSelection("*", "*")
                    }
                    MarkdownFormatButton("Inline code", painterResource(Res.drawable.filled_code)) {
                        content = content.wrapSelection("`", "`")
                    }
                    ToolbarDivider()
                    MarkdownFormatButton("Heading", painterResource(Res.drawable.filled_title)) {
                        content = content.prefixSelectedLines("## ")
                    }
                    MarkdownFormatButton("Bulleted list", painterResource(Res.drawable.filled_format_list_bulleted)) {
                        content = content.prefixSelectedLines("- ")
                    }
                    MarkdownFormatButton("Numbered list", painterResource(Res.drawable.filled_format_list_numbered)) {
                        content = content.prefixSelectedLines("1. ")
                    }
                    MarkdownFormatButton("Quote", painterResource(Res.drawable.filled_format_quote)) {
                        content = content.prefixSelectedLines("> ")
                    }
                    MarkdownFormatButton("Link", painterResource(Res.drawable.filled_link)) {
                        content = content.wrapSelection("[", "](url)")
                    }
                    ToolbarDivider()
                    PageColorMenu(
                        selectedColor = IDEA_PAGE_COLORS.firstOrNull { it.key == pageColor } ?: IDEA_PAGE_COLORS.first(),
                        onSelect = { pageColor = it.key }
                    )
                    ToolbarDivider()
                    TextButton(
                        onClick = { showLivePreview = !showLivePreview },
                        modifier = Modifier.focusProperties { canFocus = false }.interactivePointer()
                    ) {
                        Text(if (showLivePreview) "Hide preview" else "Show preview")
                    }
                }

                Box(
                    Modifier.fillMaxWidth().weight(1f).padding(horizontal = 28.dp, vertical = 12.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(18.dp))
                        .background(pageBackground, RoundedCornerShape(18.dp))
                ) {
                    if (showLivePreview) {
                        Row(
                            Modifier.fillMaxSize().padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MarkdownSourcePane(
                                value = content,
                                onValueChange = { content = it },
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                backgroundColor = pageBackground
                            )
                            Surface(
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                shape = RoundedCornerShape(12.dp),
                                color = pageBackground,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
                            ) {
                                Column(Modifier.fillMaxSize()) {
                                    Row(
                                        Modifier.fillMaxWidth().padding(start = 18.dp, end = 14.dp, top = 12.dp, bottom = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("PREVIEW", style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(Modifier.weight(1f))
                                        Icon(painterResource(Res.drawable.filled_description), contentDescription = null,
                                            modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    HorizontalDivider(
                                        Modifier.padding(horizontal = 14.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                                    )
                                    if (content.text.isBlank()) {
                                        Text(
                                            "Your formatted note will appear here.",
                                            Modifier.padding(20.dp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    } else {
                                        androidx.compose.foundation.lazy.LazyColumn(
                                            modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 10.dp)
                                        ) {
                                            item { ChatMarkdown(content.text, Modifier.fillMaxWidth()) }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        MarkdownSourcePane(
                            value = content,
                            onValueChange = { content = it },
                            modifier = Modifier.fillMaxSize().padding(10.dp),
                            backgroundColor = pageBackground
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth()
                        .background(headerBackground)
                        .padding(horizontal = 28.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Stored with this paper",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface)
                        Text("Your note stays available in this paper’s Idea Board.",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(
                        enabled = title.isNotBlank(),
                        onClick = { onSave(title.trim(), content.text, pageColor) },
                        modifier = Modifier.interactivePointer(enabled = title.isNotBlank()),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 12.dp)
                    ) {
                        Text("Save note")
                    }
                }
            }
        }
    }
}

@Composable
private fun MarkdownFormatButton(
    description: String,
    icon: Painter,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(38.dp).focusProperties { canFocus = false }.interactivePointer()
    ) {
        Icon(icon, contentDescription = description, modifier = Modifier.size(19.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ToolbarDivider() {
    VerticalDivider(Modifier.height(24.dp).padding(horizontal = 4.dp))
}

@Composable
private fun MarkdownSourcePane(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier,
    backgroundColor: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(start = 18.dp, end = 14.dp, top = 12.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("MARKDOWN SOURCE", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.weight(1f))
                Text("Editing", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary)
            }
            HorizontalDivider(
                Modifier.padding(horizontal = 14.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
            )
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth().weight(1f),
                placeholder = { Text("Start writing your idea…\n\nUse the toolbar to format text, create lists, add links, and more.") },
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = FontFamily.Serif,
                    lineHeight = 30.sp
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent,
                    unfocusedContainerColor = backgroundColor,
                    focusedContainerColor = backgroundColor
                )
            )
        }
    }
}

@Composable
private fun PageColorMenu(
    selectedColor: IdeaPageColor,
    onSelect: (IdeaPageColor) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(
            onClick = { expanded = true },
            modifier = Modifier.focusProperties { canFocus = false }.interactivePointer()
        ) {
            Surface(
                modifier = Modifier.size(18.dp).border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                color = selectedColor.color,
                shape = CircleShape
            ) {}
            Spacer(Modifier.width(8.dp))
            Text(selectedColor.label)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            IDEA_PAGE_COLORS.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    leadingIcon = {
                        Surface(
                            modifier = Modifier.size(20.dp).border(
                                1.dp,
                                if (option.key == selectedColor.key) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant,
                                CircleShape
                            ),
                            color = option.color,
                            shape = CircleShape
                        ) {}
                    },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

private data class IdeaPageColor(val key: String, val label: String, val color: Color)

private val IDEA_PAGE_COLORS = listOf(
    IdeaPageColor("cream", "Warm ivory", Color(0xFFFFFBF2)),
    IdeaPageColor("sage", "Soft sage", Color(0xFFF2F7F0)),
    IdeaPageColor("sky", "Cloud blue", Color(0xFFF1F7FD)),
    IdeaPageColor("lavender", "Misty lilac", Color(0xFFF7F4FC)),
    IdeaPageColor("rose", "Blush", Color(0xFFFDF3F1))
)

private fun ideaPageColor(key: String): Color =
    IDEA_PAGE_COLORS.firstOrNull { it.key == key }?.color ?: IDEA_PAGE_COLORS.first().color

private fun TextFieldValue.wrapSelection(prefix: String, suffix: String): TextFieldValue {
    val start = selection.min
    val end = selection.max
    val selected = text.substring(start, end)
    val replacement = "$prefix$selected$suffix"
    val updated = text.replaceRange(start, end, replacement)
    val updatedSelection = if (start == end) {
        TextRange(start + prefix.length)
    } else {
        TextRange(start + prefix.length, start + prefix.length + selected.length)
    }
    return copy(text = updated, selection = updatedSelection)
}

private fun TextFieldValue.prefixSelectedLines(prefix: String): TextFieldValue {
    val selectedStart = selection.min
    val selectedEnd = selection.max
    val lineStart = if (selectedStart == 0) 0 else text.lastIndexOf('\n', selectedStart - 1) + 1
    val endsAtNextLineStart = selectedEnd > lineStart && text.getOrNull(selectedEnd - 1) == '\n'
    val lineEnd = if (endsAtNextLineStart) {
        selectedEnd - 1
    } else {
        text.indexOf('\n', selectedEnd).let { if (it == -1) text.length else it }
    }
    val lines = text.substring(lineStart, lineEnd).split('\n')
    val replacement = lines.joinToString("\n") { "$prefix$it" }
    val updated = text.replaceRange(lineStart, lineEnd, replacement)
    val updatedSelection = if (selectedStart == selectedEnd) {
        TextRange(selectedStart + prefix.length)
    } else {
        val addedPrefixesBeforeEnd =
            text.substring(lineStart, minOf(selectedEnd, lineEnd)).count { it == '\n' } * prefix.length
        TextRange(selectedStart + prefix.length, selectedEnd + addedPrefixesBeforeEnd)
    }
    return copy(text = updated, selection = updatedSelection)
}
