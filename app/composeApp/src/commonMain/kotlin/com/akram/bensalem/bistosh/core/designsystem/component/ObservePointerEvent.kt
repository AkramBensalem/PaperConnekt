package com.akram.bensalem.bistosh.core.designsystem.component

import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput

fun Modifier.observePointerEvent(
    eventType: PointerEventType,
    onEvent: (PointerEvent) -> Unit,
): Modifier = composed {
    val currentOnEvent by rememberUpdatedState(onEvent)
    pointerInput(eventType) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                if (event.type == eventType) currentOnEvent(event)
            }
        }
    }
}
