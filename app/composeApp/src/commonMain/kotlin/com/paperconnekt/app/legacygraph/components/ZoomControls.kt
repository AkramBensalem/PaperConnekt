package com.paperconnekt.app.legacygraph.components


import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import com.paperconnekt.app.resources.Res
import com.paperconnekt.app.resources.add
import com.paperconnekt.app.resources.remove
import org.jetbrains.compose.resources.painterResource

@Composable
fun ZoomControls(
    zoomScale: Float,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val zoomPercent = (zoomScale * 100f).roundToInt()

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium, // Rounded rectangle
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
        tonalElevation = 4.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(if (compact) 2.dp else 4.dp)
        ) {
            IconButton(onClick = onZoomIn, modifier = Modifier.size(if (compact) 40.dp else 48.dp)) {
                Icon(
                    painterResource(Res.drawable.add),
                    contentDescription = "Zoom In",
                    modifier = Modifier.size(if (compact) 18.dp else 24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = "${zoomPercent}%",
                style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(vertical = 2.dp),
            )
            IconButton(onClick = onZoomOut, modifier = Modifier.size(if (compact) 40.dp else 48.dp)) {
                Icon(
                    painterResource(Res.drawable.remove),
                    contentDescription = "Zoom Out",
                    modifier = Modifier.size(if (compact) 18.dp else 24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
