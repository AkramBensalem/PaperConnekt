package com.paperconnekt.app.legacygraph.components

import androidx.compose.runtime.Composable

// The old canvas also has platform-specific trackpad adapters. The contest
// application keeps platform launchers separate, so the shared fallback lets
// the canvas retain its regular pointer/pinch handling on every target.
@Composable
internal fun NativeTrackpadPinchZoom(onZoom: (Float) -> Unit) = Unit
