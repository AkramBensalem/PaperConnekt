package com.akram.bensalem.paper.connekt.feature.assistant.component

import androidx.compose.runtime.Composable

@Composable
internal actual fun NativeReaderTouchpad(
    enabled: () -> Boolean,
    onZoom: (Float) -> Unit,
    onHorizontalPan: (Float) -> Unit,
) = Unit
