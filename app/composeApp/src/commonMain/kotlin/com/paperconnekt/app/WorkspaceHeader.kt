package com.paperconnekt.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.paperconnekt.app.resources.Res
import com.paperconnekt.app.resources.filled_close
import com.paperconnekt.app.resources.filled_search
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

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
            val wide = maxWidth >= 1040.dp
            val compact = maxWidth < 600.dp
            val inset = if (compact) 16.dp else 26.dp
            Column(
                modifier = Modifier.padding(inset),
                verticalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 18.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
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
                                "RESEARCH WORKSPACE",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.primary,
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(
                                title,
                                style = if (compact) MaterialTheme.typography.headlineMedium
                                    else MaterialTheme.typography.headlineLarge,
                            )
                            Text(
                                subtitle,
                                style = MaterialTheme.typography.bodyMedium,
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
    val content: @Composable () -> Unit = {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(18.dp))
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
    if (primary) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            shape = RoundedCornerShape(12.dp),
            interactionSource = interaction,
            colors = ButtonDefaults.buttonColors(containerColor = background),
        ) { content() }
    } else {
        FilledTonalButton(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            shape = RoundedCornerShape(12.dp),
            interactionSource = interaction,
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = background,
                contentColor = colors.onSecondaryContainer,
            ),
        ) { content() }
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
                        Icon(painterResource(Res.drawable.filled_close), contentDescription = "Clear search")
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
