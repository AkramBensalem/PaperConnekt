package com.paperconnekt.app.legacygraph.components

import androidx.compose.ui.graphics.painter.Painter

import com.paperconnekt.app.resources.Res
import com.paperconnekt.app.resources.filled_filter_list
import com.paperconnekt.app.resources.filled_info
import com.paperconnekt.app.resources.filled_keyboard_arrow_down
import com.paperconnekt.app.resources.filled_keyboard_arrow_up
import com.paperconnekt.app.resources.filled_layers
import org.jetbrains.compose.resources.painterResource
import com.paperconnekt.app.legacygraph.interactivePointer

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.*

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun GraphControls(
    filterCiting: Boolean,
    onToggleCiting: () -> Unit,
    filterCited: Boolean,
    onToggleCited: () -> Unit,
    minCitations: Int,
    onMinCitationsChanged: (Int) -> Unit,
    maxCitations: Int,
    onMaxCitationsChanged: (Int) -> Unit,
    minimumSimilarityPercent: Int = 0,
    onMinimumSimilarityChanged: (Int) -> Unit = {},
    showInterconnections: Boolean = true,
    onToggleInterconnections: () -> Unit = {},
    modifier: Modifier = Modifier,
    showDisqualified: Boolean = false,
    onToggleShowDisqualified: () -> Unit = {},
    initiallyExpanded: Boolean = false,
    embedded: Boolean = false,
) {
    var isCollapsed by remember { mutableStateOf(!initiallyExpanded) }
    var minCitationsText by remember { mutableStateOf(minCitations.toString()) }

    LaunchedEffect(minCitations) {
        if (minCitationsText.toIntOrNull() != minCitations) {
            minCitationsText = minCitations.toString()
        }
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = if (embedded) Color.Transparent else MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = if (embedded) 0.dp else 4.dp,
        border = if (embedded) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier
                .animateContentSize()
                .padding(14.dp),
        ) {



            if (!embedded) Row(
                modifier = Modifier
                    .fillMaxWidth().clickable { isCollapsed = !isCollapsed },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painterResource(Res.drawable.filled_filter_list),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Graph Filters", style = MaterialTheme.typography.titleSmall, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                }
                Icon(
                    if (isCollapsed) painterResource(Res.drawable.filled_keyboard_arrow_down) else painterResource(Res.drawable.filled_keyboard_arrow_up),
                    contentDescription = if (isCollapsed) "Expand" else "Collapse",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            AnimatedVisibility(
                visible = embedded || !isCollapsed,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))

                    ControlToggle(
                        label = "References",
                        checked = filterCiting,
                        onCheckedChange = { onToggleCiting() },
                        icon = painterResource(Res.drawable.filled_layers)
                    )

                    ControlToggle(
                        label = "Citations",
                        checked = filterCited,
                        onCheckedChange = { onToggleCited() },
                        icon = painterResource(Res.drawable.filled_info)
                    )

                    ControlToggle(
                        label = "Show Disqualified",
                        checked = showDisqualified,
                        onCheckedChange = { onToggleShowDisqualified() },
                        icon = painterResource(Res.drawable.filled_filter_list)
                    )

                    ControlToggle(
                        label = "Interconnections",
                        checked = showInterconnections,
                        onCheckedChange = { onToggleInterconnections() },
                        icon = painterResource(Res.drawable.filled_layers)
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = minCitationsText,
                        onValueChange = { value ->
                            val digits = value.filter(Char::isDigit).take(6)
                            minCitationsText = digits
                            onMinCitationsChanged(digits.toIntOrNull()?.coerceAtLeast(0) ?: 0)
                        },
                        label = { Text("Minimum Citations") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        "Minimum Similarity: $minimumSimilarityPercent%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                    )
                    Slider(
                        value = minimumSimilarityPercent.toFloat(),
                        onValueChange = { onMinimumSimilarityChanged(it.toInt()) },
                        valueRange = 0f..100f,
                        steps = 19
                    )
                }
            }
        }
    }
}

@Composable
private fun ControlToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: Painter
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            .background(
                if (checked) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                MaterialTheme.shapes.medium,
            )
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .interactivePointer().padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon, contentDescription = null, modifier = Modifier.size(18.dp),
                tint = if (checked) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                label, style = MaterialTheme.typography.bodyMedium,
                color = if (checked) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
        )
    }
}
