package com.paperconnekt.app.legacygraph.components

import com.paperconnekt.app.resources.Res
import com.paperconnekt.app.resources.filled_check
import com.paperconnekt.app.resources.filled_content_copy
import org.jetbrains.compose.resources.painterResource
import com.paperconnekt.app.legacygraph.interactivePointer

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun CopyIconButton(
    textToCopy: String,
    contentDescription: String = "Copy to clipboard",
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    iconSize: Dp = 16.dp,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    val clipboardManager = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) {
            delay(1800)
            copied = false
        }
    }

    val iconTint by animateColorAsState(
        if (copied) MaterialTheme.colorScheme.primary else tint
    )

    IconButton(
        onClick = {
            if (textToCopy.isNotBlank()) {
                clipboardManager.setText(AnnotatedString(textToCopy))
                copied = true
            }
        },
        modifier = modifier.size(size).interactivePointer()
    ) {
        Icon(
            painter = if (copied) painterResource(Res.drawable.filled_check) else painterResource(Res.drawable.filled_content_copy),
            contentDescription = if (copied) "Copied!" else contentDescription,
            modifier = Modifier.size(iconSize),
            tint = iconTint
        )
    }
}

@Composable
fun CopyChipButton(
    textToCopy: String,
    label: String,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) {
            delay(1800)
            copied = false
        }
    }

    FilledTonalButton(
        onClick = {
            if (textToCopy.isNotBlank()) {
                clipboardManager.setText(AnnotatedString(textToCopy))
                copied = true
            }
        },
        modifier = modifier.interactivePointer(),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = if (copied) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (copied) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = if (copied) painterResource(Res.drawable.filled_check) else painterResource(Res.drawable.filled_content_copy),
                contentDescription = null,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(if (copied) "Copied!" else label, style = MaterialTheme.typography.labelSmall)
        }
    }
}
