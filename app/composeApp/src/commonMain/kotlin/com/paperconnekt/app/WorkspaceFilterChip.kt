package com.paperconnekt.app

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

@Composable
internal fun WorkspaceFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    leadingIcon: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val pressed by interaction.collectIsPressedAsState()
    val focused by interaction.collectIsFocusedAsState()
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(12.dp)
    val containerColor by animateColorAsState(
        targetValue = when {
            selected && pressed -> lerp(colors.primaryContainer, colors.primary, 0.2f)
            selected && hovered -> lerp(colors.primaryContainer, colors.primary, 0.12f)
            selected -> colors.primaryContainer
            pressed -> lerp(colors.surface, colors.primary, 0.18f)
            hovered -> lerp(colors.surface, colors.primary, 0.08f)
            else -> colors.surface
        },
        animationSpec = tween(150),
    )
    val contentColor by animateColorAsState(
        targetValue = when {
            selected -> colors.onPrimaryContainer
            hovered || pressed -> colors.primary
            else -> colors.onSurfaceVariant
        },
        animationSpec = tween(150),
    )
    val borderColor = when {
        focused -> colors.primary
        hovered || selected -> colors.primary
        else -> colors.outlineVariant
    }
    Row(
        modifier = modifier
            .padding(vertical = 4.dp)
            .heightIn(min = 40.dp)
            .clip(shape)
            .background(containerColor)
            .border(
                BorderStroke(
                    if (focused) 2.dp else 1.dp,
                    borderColor,
                ),
                shape,
            )
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab,
                interactionSource = interaction,
                indication = null,
            )
            .pointerHoverIcon(PointerIcon.Hand)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(
            LocalContentColor provides contentColor,
        ) {
            leadingIcon?.invoke()
            ProvideTextStyle(MaterialTheme.typography.labelLarge) { label() }
        }
    }
}
