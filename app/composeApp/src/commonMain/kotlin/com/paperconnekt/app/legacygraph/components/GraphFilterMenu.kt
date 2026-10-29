package com.paperconnekt.app.legacygraph.components

import com.paperconnekt.app.resources.Res
import com.paperconnekt.app.resources.filled_close
import com.paperconnekt.app.resources.filled_filter_list
import org.jetbrains.compose.resources.painterResource
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.paperconnekt.app.legacygraph.components.GraphAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GraphFilterMenu(
    expanded: Boolean,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    val windowSize = LocalWindowInfo.current.containerSize
    val width = with(LocalDensity.current) { windowSize.width.toDp() }
    val height = with(LocalDensity.current) { windowSize.height.toDp() }
    Box {
        GraphAction(painterResource(Res.drawable.filled_filter_list), "Graph filters", onOpen)
        when {
            width < 600.dp -> if (expanded) {
                ModalBottomSheet(
                    onDismissRequest = onDismiss,
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ) {
                    Column(Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState())) {
                        GraphFilterHeader(onDismiss)
                        content()
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
            width < 1200.dp -> if (expanded) {
                Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
                    Surface(
                        modifier = Modifier.safeDrawingPadding().imePadding().padding(24.dp)
                            .widthIn(max = 480.dp).fillMaxWidth().heightIn(max = height * 0.85f),
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Column(Modifier.verticalScroll(rememberScrollState())) {
                            GraphFilterHeader(onDismiss)
                            content()
                        }
                    }
                }
            }
            else -> DropdownMenu(
                expanded = expanded,
                onDismissRequest = onDismiss,
                modifier = Modifier.width(360.dp).heightIn(max = height * 0.8f),
                shape = MaterialTheme.shapes.large,
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 4.dp,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                GraphFilterHeader(onDismiss)
                content()
            }
        }
    }
}

@Composable
private fun GraphFilterHeader(onDismiss: () -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(Res.drawable.filled_filter_list), null, tint = MaterialTheme.colorScheme.primary)
            Text("Graph filters", Modifier.weight(1f).padding(start = 12.dp), style = MaterialTheme.typography.titleMedium)
            GraphAction(painterResource(Res.drawable.filled_close), "Close graph filters", onDismiss)
        }
        Text(
            "Changes apply to the canvas immediately",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}
