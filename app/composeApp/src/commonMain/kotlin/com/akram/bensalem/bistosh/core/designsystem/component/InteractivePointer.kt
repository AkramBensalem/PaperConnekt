package com.akram.bensalem.bistosh.core.designsystem.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon

fun Modifier.interactivePointer(enabled: Boolean = true): Modifier =
    pointerHoverIcon(if (enabled) PointerIcon.Hand else PointerIcon.Default)
