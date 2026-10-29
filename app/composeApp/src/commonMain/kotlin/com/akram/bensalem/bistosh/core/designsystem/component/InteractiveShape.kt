package com.akram.bensalem.bistosh.core.designsystem.component

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/**
 * Clips interaction indications to the same rounded bounds used by compact
 * Material components. Desktop hover indications otherwise use the full node
 * rectangle.
 */
fun Modifier.clipChipInteraction(): Modifier =
    clip(RoundedCornerShape(8.dp))
