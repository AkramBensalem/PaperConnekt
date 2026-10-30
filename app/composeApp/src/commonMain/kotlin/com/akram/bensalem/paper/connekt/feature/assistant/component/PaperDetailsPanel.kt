package com.akram.bensalem.paper.connekt.feature.assistant.component

import com.akram.bensalem.bistosh.core.designsystem.component.interactivePointer
import com.akram.bensalem.paper.connekt.models.Paper
import com.paperconnekt.app.legacygraph.components.CopyIconButton
import com.paperconnekt.app.resources.Res
import com.paperconnekt.app.resources.filled_article
import com.paperconnekt.app.resources.filled_calendar_month
import com.paperconnekt.app.resources.filled_description
import com.paperconnekt.app.resources.filled_info
import com.paperconnekt.app.resources.filled_link
import com.paperconnekt.app.resources.filled_title
import org.jetbrains.compose.resources.painterResource

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun PaperDetailsPanel(
    paper: Paper,
    modifier: Modifier = Modifier,
    scrollable: Boolean = true,
) {
    var abstractExpanded by remember(paper.id) { mutableStateOf(false) }
    val abstract = paper.abstract?.takeIf { it.isNotBlank() }
    val authors = paper.authors.joinToString(", ")
    val tags = (paper.tags + paper.keywords).distinct().filter { it.isNotBlank() }

    SelectionContainer {
        Column(
            modifier = modifier
                .fillMaxSize()
                .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                .padding(start = 0.dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        text = paper.title.ifBlank { "Untitled paper" },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        lineHeight = 25.sp,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(6.dp))
                    CopyIconButton(
                        textToCopy = paper.title,
                        contentDescription = "Copy paper title",
                        size = 30.dp,
                        iconSize = 15.dp,
                    )
                }
                if (authors.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                    ) {
                        Text(
                            text = authors,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        CopyIconButton(
                            textToCopy = authors,
                            contentDescription = "Copy authors",
                            size = 30.dp,
                            iconSize = 15.dp,
                        )
                    }
                }
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                paper.publicationDate?.takeIf { it.isNotBlank() }?.let {
                    DetailBadge(
                        icon = Res.drawable.filled_calendar_month,
                        text = it,
                    )
                }
                paper.publicationName?.takeIf { it.isNotBlank() }?.let {
                    DetailBadge(
                        icon = Res.drawable.filled_article,
                        text = it,
                    )
                }
                Surface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Text(
                        text = if (paper.filePath != null) "PDF available" else "PDF not attached",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }

            DetailCard(
                title = "Abstract",
                icon = Res.drawable.filled_description,
                copyText = abstract,
            ) {
                Text(
                    text = abstract ?: "No abstract available for this publication.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 20.sp,
                    maxLines = if (abstractExpanded) Int.MAX_VALUE else 6,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.animateContentSize(),
                )
                if (abstract != null && abstract.length > 420) {
                    TextButton(
                        onClick = { abstractExpanded = !abstractExpanded },
                        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 2.dp),
                        modifier = Modifier.interactivePointer(),
                    ) {
                        Text(if (abstractExpanded) "Show less" else "Show more")
                    }
                }
            }

            val publicationFields = listOfNotNull(
                paper.publicationName?.takeIf { it.isNotBlank() }?.let { "Publication" to it },
                paper.publicationDate?.takeIf { it.isNotBlank() }?.let { "Date" to it },
                paper.volume?.takeIf { it.isNotBlank() }?.let { "Volume" to it },
                paper.issue?.takeIf { it.isNotBlank() }?.let { "Issue" to it },
                paper.pages?.takeIf { it.isNotBlank() }?.let { "Pages" to it },
            )
            if (publicationFields.isNotEmpty()) {
                DetailCard(
                    title = "Publication details",
                    icon = Res.drawable.filled_article,
                ) {
                    publicationFields.forEach { (label, value) ->
                        DetailLine(label, value)
                    }
                }
            }

            val identifiers = listOfNotNull(
                paper.doi?.takeIf { it.isNotBlank() }?.let { "DOI" to it },
                paper.url?.takeIf { it.isNotBlank() }?.let { "Source" to it },
                paper.filePath?.takeIf { it.isNotBlank() }?.let { "File" to it },
            )
            if (identifiers.isNotEmpty()) {
                DetailCard(
                    title = "Identifiers & sources",
                    icon = Res.drawable.filled_link,
                ) {
                    identifiers.forEach { (label, value) ->
                        DetailLine(label, value, copyable = true)
                    }
                }
            }

            if (tags.isNotEmpty()) {
                DetailCard(
                    title = "Keywords & tags",
                    icon = Res.drawable.filled_title,
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        tags.forEach { tag ->
                            Surface(
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                            ) {
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                )
                            }
                        }
                    }
                }
            }

            DetailCard(
                title = "Record",
                icon = Res.drawable.filled_info,
            ) {
                DetailLine("Source", if (paper.id.startsWith("zotero:")) "Zotero" else "Local library")
                DetailLine("Paper ID", paper.id, copyable = true)
            }
        }
    }
}

@Composable
private fun DetailCard(
    title: String,
    icon: org.jetbrains.compose.resources.DrawableResource,
    copyText: String? = null,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(icon),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                copyText?.let {
                    CopyIconButton(
                        textToCopy = it,
                        contentDescription = "Copy $title",
                        size = 28.dp,
                        iconSize = 14.dp,
                    )
                }
            }
            Spacer(Modifier.height(7.dp))
            content()
        }
    }
}

@Composable
private fun DetailLine(
    label: String,
    value: String,
    copyable: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(78.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (copyable) {
            CopyIconButton(
                textToCopy = value,
                contentDescription = "Copy $label",
                size = 26.dp,
                iconSize = 13.dp,
            )
        }
    }
}

@Composable
private fun DetailBadge(
    icon: org.jetbrains.compose.resources.DrawableResource,
    text: String,
) {
    Surface(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
