package com.paperconnekt.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.paperconnekt.app.resources.Res
import com.paperconnekt.app.resources.filled_close
import com.paperconnekt.app.resources.filled_search
import com.paperconnekt.app.resources.research_workspace
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import com.paperconnekt.app.resources.clear_search

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun WorkspaceHeader(
    title: String,
    subtitle: String,
    icon: DrawableResource,
    modifier: Modifier = Modifier,
    summaries: List<String> = emptyList(),
    actions: (@Composable () -> Unit)? = null,
    tools: (@Composable () -> Unit)? = null,
) {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val reveal by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(320),
    )
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.fillMaxWidth().graphicsLayer {
            alpha = reveal
            translationY = (1f - reveal) * 10.dp.toPx()
        },
        shape = RoundedCornerShape(24.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.6f)),
    ) {
        BoxWithConstraints(
            Modifier.background(
                Brush.linearGradient(
                    listOf(colors.surface, colors.surfaceContainerLow, colors.primaryContainer.copy(alpha = 0.35f)),
                ),
            ),
        ) {
            val wide = maxWidth >= 1120.dp
            val compact = maxWidth < 720.dp
            val inset = when {
                maxWidth < 420.dp -> 12.dp
                compact -> 16.dp
                else -> 26.dp
            }
            Column(
                modifier = Modifier.padding(inset),
                verticalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 18.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(if (compact) 14.dp else 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp),
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = colors.primaryContainer,
                                contentColor = colors.onPrimaryContainer,
                            ) {
                                Icon(
                                    painterResource(icon),
                                    contentDescription = null,
                                    modifier = Modifier.padding(8.dp).size(18.dp),
                                )
                            }
                            Text(
                                stringResource(Res.string.research_workspace),
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.primary,
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(
                                title,
                                style = if (compact) MaterialTheme.typography.headlineSmall
                                    else MaterialTheme.typography.headlineLarge,
                            )
                            Text(
                                subtitle,
                                style = if (compact) MaterialTheme.typography.bodySmall
                                    else MaterialTheme.typography.bodyMedium,
                                color = colors.onSurfaceVariant,
                            )
                        }
                    }
                    if (wide && actions != null) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) { actions() }
                    }
                }
                if (summaries.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        summaries.forEach { summary ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = colors.surfaceContainer,
                                contentColor = colors.onSurfaceVariant,
                            ) {
                                Text(
                                    summary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                        }
                    }
                }
                if (!wide && actions != null) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) { actions() }
                }
                tools?.invoke()
            }
        }
    }
}

@Composable
internal fun HeaderAction(
    label: String,
    icon: DrawableResource,
    onClick: () -> Unit,
    primary: Boolean = true,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.97f else 1f,
        animationSpec = tween(120),
    )
    val colors = MaterialTheme.colorScheme
    val background by animateColorAsState(
        targetValue = when {
            primary && hovered && enabled -> colors.primary.copy(alpha = 0.88f)
            primary -> colors.primary
            hovered && enabled -> colors.secondaryContainer
            else -> colors.surfaceContainerHigh
        },
        animationSpec = tween(160),
    )
    val modifier = Modifier.heightIn(min = 46.dp).graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
    val buttonBackground = if (enabled) {
        background
    } else {
        colors.onSurface.copy(alpha = 0.12f)
    }
    val buttonContentColor = if (enabled) {
        if (primary) colors.onPrimary else colors.onSecondaryContainer
    } else {
        colors.onSurface.copy(alpha = 0.38f)
    }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(buttonBackground)
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painterResource(icon),
            contentDescription = null,
            tint = buttonContentColor,
            modifier = Modifier.size(18.dp),
        )
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = buttonContentColor,
        )
    }
}

@Composable
internal fun HeaderSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    trailingAction: (@Composable () -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val background by animateColorAsState(
        if (focused) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainerLowest,
        animationSpec = tween(160),
    )
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        interactionSource = interaction,
        shape = RoundedCornerShape(14.dp),
        label = { Text(label) },
        leadingIcon = {
            Icon(painterResource(Res.drawable.filled_search), contentDescription = null)
        },
        trailingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AnimatedVisibility(visible = query.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(painterResource(Res.drawable.filled_close), contentDescription = stringResource(Res.string.clear_search))
                    }
                }
                trailingAction?.invoke()
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = background,
            unfocusedContainerColor = background,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        ),
        textStyle = MaterialTheme.typography.bodyMedium,
        singleLine = true,
    )
}
