package com.paperconnekt.app.legacygraph.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.paperconnekt.app.resources.Res
import org.jetbrains.compose.resources.painterResource

@Composable
fun PanControls(
    onPanLeft: () -> Unit,
    onPanRight: () -> Unit,
    onPanUp: () -> Unit,
    onPanDown: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val buttonModifier = if (compact) Modifier.size(32.dp) else Modifier
    val iconModifier = Modifier.size(if (compact) 18.dp else 24.dp)
    Column(
        modifier = modifier,
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        SmallFloatingActionButton(
            onClick = onPanUp,
            modifier = buttonModifier,
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(painterResource(Res.drawable.arrow_upward), contentDescription = "Move graph up", modifier = iconModifier)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            SmallFloatingActionButton(
                onClick = onPanLeft,
                modifier = buttonModifier,
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(painterResource(Res.drawable.arrow_back_material), contentDescription = "Move graph left", modifier = iconModifier)
            }
            Spacer(modifier = Modifier.size(if (compact) 32.dp else 40.dp))
            SmallFloatingActionButton(
                onClick = onPanRight,
                modifier = buttonModifier,
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(painterResource(Res.drawable.arrow_forward), contentDescription = "Move graph right", modifier = iconModifier)
            }
        }
        SmallFloatingActionButton(
            onClick = onPanDown,
            modifier = buttonModifier,
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(painterResource(Res.drawable.arrow_downward), contentDescription = "Move graph down", modifier = iconModifier)
        }
    }
}
